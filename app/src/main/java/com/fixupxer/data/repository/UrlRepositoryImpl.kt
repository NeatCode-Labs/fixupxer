// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2025  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


package com.fixupxer.data.repository

import com.fixupxer.PreferencesManager
import com.fixupxer.UrlProcessor
import com.fixupxer.domain.model.ProcessedUrlResult
import com.fixupxer.domain.repository.UrlRepository
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of UrlRepository
 */
class UrlRepositoryImpl @Inject constructor(
    private val urlProcessor: UrlProcessor,
    private val preferencesManager: PreferencesManager,
    private val historyRepository: HistoryRepository
) : UrlRepository {
    
    override suspend fun processUrl(url: String): ProcessedUrlResult = processUrl(url, false)
    
    override suspend fun processUrl(url: String, forceCleanTracking: Boolean): ProcessedUrlResult = 
        processUrl(url, forceCleanTracking, null)
    
    override suspend fun processUrl(url: String, forceCleanTracking: Boolean, previousProcessedUrl: String?): ProcessedUrlResult = withContext(Dispatchers.IO) {
        
        if (url.isEmpty()) return@withContext ProcessedUrlResult(url, true)
        
        val isInstagram = urlProcessor.isInstagramUrl(url)
        val isTwitter = urlProcessor.isTwitterUrl(url)
        val isFacebook = urlProcessor.isFacebookUrl(url)
        
        val platform = when {
            isInstagram -> "Instagram"
            isTwitter -> "Twitter/X"
            isFacebook -> "Facebook"
            else -> "Other"
        }
        
        val instagramProxy = preferencesManager.getInstagramProxy()

        val (processedUrl, wasAlreadyClean) = if (isInstagram) {
            // For Instagram URLs, use the Instagram conversion preference
            urlProcessor.processUrl(
                url,
                cleanTracking = true, // Always clean tracking
                convertTwitter = preferencesManager.isConvertInstagramEnabled(),
                instagramProxy = instagramProxy
            )
        } else if (isFacebook) {
            // For Facebook URLs, use the Instagram conversion preference (same toggle)
            urlProcessor.processUrl(
                url,
                cleanTracking = forceCleanTracking || preferencesManager.isCleanTrackingEnabled(),
                convertTwitter = preferencesManager.isConvertInstagramEnabled(),
                instagramProxy = instagramProxy
            )
        } else {
            // For other URLs, use the standard preferences
            urlProcessor.processUrl(
                url,
                cleanTracking = forceCleanTracking || preferencesManager.isCleanTrackingEnabled(),
                convertTwitter = preferencesManager.isConvertTwitterEnabled(),
                instagramProxy = instagramProxy
            )
        }
        
        // Save to history if enabled and URL was modified
        // If previousProcessedUrl is provided, compare against that instead of the original URL
        val shouldSaveHistory = if (previousProcessedUrl != null) {
            // For toggle changes: only save if the new result differs from previous result
            preferencesManager.isHistoryEnabled() && processedUrl != previousProcessedUrl
        } else {
            // For initial processing: save if the result differs from input
            preferencesManager.isHistoryEnabled() && url != processedUrl
        }
        
        if (shouldSaveHistory) {
            
            val trackingRemoved = !wasAlreadyClean
            
            val conversionType = when {
                url.contains(Constants.INSTAGRAM_DOMAIN) &&
                    Constants.INSTAGRAM_PROXY_DOMAINS.any { processedUrl.contains(it) } -> "Domain converted"
                Constants.INSTAGRAM_PROXY_DOMAINS.any { url.contains(it) } &&
                    processedUrl.contains(Constants.INSTAGRAM_DOMAIN) &&
                    Constants.INSTAGRAM_PROXY_DOMAINS.none { processedUrl.contains(it) } -> "Domain converted"
                Constants.INSTAGRAM_PROXY_DOMAINS.any { url.contains(it) } &&
                    Constants.INSTAGRAM_PROXY_DOMAINS.any { processedUrl.contains(it) } &&
                    Constants.INSTAGRAM_PROXY_DOMAINS.none { p ->
                        url.contains(p) && processedUrl.contains(p)
                    } -> "Domain converted"
                url.contains("facebook.com") && processedUrl.contains("facebookez.com") -> "Domain converted"
                url.contains("facebookez.com") && processedUrl.contains("facebook.com") -> "Domain converted"
                url.contains("twitter.com") && processedUrl.contains("fixupx.com") -> "Domain converted"
                url.contains("x.com") && processedUrl.contains("fixupx.com") -> "Domain converted"
                url.contains("fixupx.com") && processedUrl.contains("x.com") -> "Domain converted"
                url.contains("fixupx.com") && processedUrl.contains("twitter.com") -> "Domain converted"
                url.contains("fxtwitter.com") && processedUrl.contains("fixupx.com") -> "Domain converted"
                url.contains("fxtwitter.com") && processedUrl.contains("x.com") -> "Domain converted"
                trackingRemoved -> "Tracking removed"
                else -> "URL cleaned"
            }
            
            try {
                historyRepository.insertHistory(
                    originalUrl = url,
                    cleanedUrl = processedUrl,
                    platform = platform,
                    conversionType = conversionType
                )
                
                // Trim history to max entries
                val maxEntries = preferencesManager.getMaxHistoryEntries()
                historyRepository.trimHistory(maxEntries)
                
            } catch (e: Exception) {
                // Silently ignore errors
            }
        }
        
        ProcessedUrlResult(processedUrl, wasAlreadyClean)
    }
    
    override suspend fun processUrlWithoutHistory(url: String): ProcessedUrlResult = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext ProcessedUrlResult(url, true)
        
        val isInstagram = urlProcessor.isInstagramUrl(url)
        val isFacebook = urlProcessor.isFacebookUrl(url)
        val instagramProxy = preferencesManager.getInstagramProxy()
        
        val (processedUrl, wasAlreadyClean) = if (isInstagram) {
            // For Instagram URLs, use the Instagram conversion preference
            urlProcessor.processUrl(
                url,
                cleanTracking = true, // Always clean tracking
                convertTwitter = preferencesManager.isConvertInstagramEnabled(),
                instagramProxy = instagramProxy
            )
        } else if (isFacebook) {
            // For Facebook URLs, use the Instagram conversion preference (same toggle)
            urlProcessor.processUrl(
                url,
                cleanTracking = preferencesManager.isCleanTrackingEnabled(),
                convertTwitter = preferencesManager.isConvertInstagramEnabled(),
                instagramProxy = instagramProxy
            )
        } else {
            // For other URLs, use the standard preferences
            urlProcessor.processUrl(
                url,
                cleanTracking = preferencesManager.isCleanTrackingEnabled(),
                convertTwitter = preferencesManager.isConvertTwitterEnabled(),
                instagramProxy = instagramProxy
            )
        }
        
        ProcessedUrlResult(processedUrl, wasAlreadyClean)
    }
    
    override suspend fun processUrlForSharing(url: String): String = withContext(Dispatchers.IO) {
        urlProcessor.processUrlForSharing(url, preferencesManager.getInstagramProxy())
    }
    
    override suspend fun cleanUrl(url: String): String = withContext(Dispatchers.IO) {
        // Clean URL with history tracking
        val result = processUrl(url, true)
        result.url
    }
    
    override fun isInstagramUrl(url: String): Boolean = urlProcessor.isInstagramUrl(url)
    
    override fun isTwitterUrl(url: String): Boolean = urlProcessor.isTwitterUrl(url)
    
    override fun hasTrackingParameters(url: String): Boolean = urlProcessor.hasTrackingParameters(url)
    
    override fun isInstagramConversionEnabled(): Flow<Boolean> = flowOf(preferencesManager.isConvertInstagramEnabled())
    
    override suspend fun setInstagramConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesManager.setConvertInstagramEnabled(enabled)
        }
    }
    
    override fun isTrackingRemovalEnabled(): Flow<Boolean> = flowOf(preferencesManager.isCleanTrackingEnabled())
    
    override suspend fun setTrackingRemovalEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesManager.setCleanTrackingEnabled(enabled)
        }
    }
    
    override fun isTwitterConversionEnabled(): Flow<Boolean> = flowOf(preferencesManager.isConvertTwitterEnabled())
    
    override suspend fun setTwitterConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesManager.setConvertTwitterEnabled(enabled)
        }
    }
    
    override suspend fun processUrlForBrowser(url: String): ProcessedUrlResult = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext ProcessedUrlResult(url, true)
        
        val isInstagram = urlProcessor.isInstagramUrl(url)
        val isTwitter = urlProcessor.isTwitterUrl(url)
        val isFacebook = urlProcessor.isFacebookUrl(url)
        
        val platform = when {
            isInstagram -> "Instagram"
            isTwitter -> "Twitter/X"
            isFacebook -> "Facebook"
            else -> "Other"
        }
        
        // Use browser-specific conversion preferences; Instagram proxy is shared with main app
        val instagramProxy = preferencesManager.getInstagramProxy()
        val (processedUrl, wasAlreadyClean) = when {
            isInstagram -> {
                urlProcessor.processUrl(
                    url,
                    cleanTracking = true, // Always clean tracking
                    convertTwitter = preferencesManager.isBrowserConvertInstagramEnabled(),
                    instagramProxy = instagramProxy
                )
            }
            isFacebook -> {
                urlProcessor.processUrl(
                    url,
                    cleanTracking = preferencesManager.isCleanTrackingEnabled(),
                    convertTwitter = preferencesManager.isBrowserConvertFacebookEnabled(),
                    instagramProxy = instagramProxy
                )
            }
            isTwitter -> {
                urlProcessor.processUrl(
                    url,
                    cleanTracking = preferencesManager.isCleanTrackingEnabled(),
                    convertTwitter = preferencesManager.isBrowserConvertTwitterEnabled(),
                    instagramProxy = instagramProxy
                )
            }
            else -> {
                // For other URLs, use the standard preferences
                urlProcessor.processUrl(
                    url,
                    cleanTracking = preferencesManager.isCleanTrackingEnabled(),
                    convertTwitter = false, // Don't convert non-social media URLs
                    instagramProxy = instagramProxy
                )
            }
        }
        
        // Save to history if enabled and URL was modified
        if (preferencesManager.isHistoryEnabled() && url != processedUrl) {
            val trackingRemoved = !wasAlreadyClean
            
            val conversionType = when {
                url.contains(Constants.INSTAGRAM_DOMAIN) &&
                    Constants.INSTAGRAM_PROXY_DOMAINS.any { processedUrl.contains(it) } -> "Domain converted"
                Constants.INSTAGRAM_PROXY_DOMAINS.any { url.contains(it) } &&
                    processedUrl.contains(Constants.INSTAGRAM_DOMAIN) &&
                    Constants.INSTAGRAM_PROXY_DOMAINS.none { processedUrl.contains(it) } -> "Domain converted"
                Constants.INSTAGRAM_PROXY_DOMAINS.any { url.contains(it) } &&
                    Constants.INSTAGRAM_PROXY_DOMAINS.any { processedUrl.contains(it) } &&
                    Constants.INSTAGRAM_PROXY_DOMAINS.none { p ->
                        url.contains(p) && processedUrl.contains(p)
                    } -> "Domain converted"
                url.contains("facebook.com") && processedUrl.contains("facebookez.com") -> "Domain converted"
                url.contains("facebookez.com") && processedUrl.contains("facebook.com") -> "Domain converted"
                url.contains("twitter.com") && processedUrl.contains("fixupx.com") -> "Domain converted"
                url.contains("x.com") && processedUrl.contains("fixupx.com") -> "Domain converted"
                url.contains("fixupx.com") && processedUrl.contains("x.com") -> "Domain converted"
                url.contains("fixupx.com") && processedUrl.contains("twitter.com") -> "Domain converted"
                url.contains("fxtwitter.com") && processedUrl.contains("fixupx.com") -> "Domain converted"
                url.contains("fxtwitter.com") && processedUrl.contains("x.com") -> "Domain converted"
                trackingRemoved -> "Tracking removed"
                else -> "URL cleaned"
            }
            
            try {
                historyRepository.insertHistory(
                    originalUrl = url,
                    cleanedUrl = processedUrl,
                    platform = platform,
                    conversionType = conversionType
                )
                
                // Trim history to max entries
                val maxEntries = preferencesManager.getMaxHistoryEntries()
                historyRepository.trimHistory(maxEntries)
                
            } catch (e: Exception) {
                // Silently ignore errors
            }
        }
        
        ProcessedUrlResult(processedUrl, wasAlreadyClean)
    }

} 