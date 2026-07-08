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
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.TikTokProxyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementation of UrlRepository
 */
class UrlRepositoryImpl @Inject constructor(
    private val urlProcessor: UrlProcessor,
    private val preferencesManager: PreferencesManager,
    private val historyRepository: HistoryRepository
) : UrlRepository {

    companion object {
        // History data values persisted in Room — intentionally NOT localized;
        // changing them would break classification of existing history entries.
        private const val CONVERSION_DOMAIN_CONVERTED = "Domain converted"
        private const val CONVERSION_TRACKING_REMOVED = "Tracking removed"
        private const val CONVERSION_URL_CLEANED = "URL cleaned"
        private const val PLATFORM_INSTAGRAM = "Instagram"
        private const val PLATFORM_TWITTER = "Twitter/X"
        private const val PLATFORM_FACEBOOK = "Facebook"
        private const val PLATFORM_TIKTOK = "TikTok"
        private const val PLATFORM_OTHER = "Other"
    }

    private fun detectPlatform(url: String): String = when {
        urlProcessor.isInstagramUrl(url) -> PLATFORM_INSTAGRAM
        urlProcessor.isTwitterUrl(url) -> PLATFORM_TWITTER
        urlProcessor.isFacebookUrl(url) -> PLATFORM_FACEBOOK
        urlProcessor.isTikTokUrl(url) -> PLATFORM_TIKTOK
        else -> PLATFORM_OTHER
    }

    /**
     * Classify how [url] → [processedUrl] should appear in history.
     * Shared by [processUrl] and [processUrlForBrowser].
     */
    private fun classifyConversion(url: String, processedUrl: String, trackingRemoved: Boolean): String {
        val knownProxies = InstagramProxyStore.allKnownProxies()
        val urlHasProxy = knownProxies.any { url.contains(it, ignoreCase = true) }
        val resultHasProxy = knownProxies.any { processedUrl.contains(it, ignoreCase = true) }
        // NOTE: contains(INSTAGRAM_DOMAIN) is also true for proxy hosts like
        // toinstagram.com (substring), so proxy checks must come first / be combined.
        val isInstagramConversion =
            (url.contains(Constants.INSTAGRAM_DOMAIN, ignoreCase = true) && !urlHasProxy && resultHasProxy) ||
                (urlHasProxy && processedUrl.contains(Constants.INSTAGRAM_DOMAIN, ignoreCase = true) && !resultHasProxy) ||
                (urlHasProxy && resultHasProxy && knownProxies.none { p ->
                    url.contains(p, ignoreCase = true) && processedUrl.contains(p, ignoreCase = true)
                })

        val isFacebookConversion =
            ((url.contains(Constants.FACEBOOK_DOMAIN, ignoreCase = true) ||
                url.contains(Constants.FB_SHORT_DOMAIN, ignoreCase = true)) &&
                processedUrl.contains(Constants.FACEBOOKEZ_DOMAIN, ignoreCase = true)) ||
                (url.contains(Constants.FACEBOOKEZ_DOMAIN, ignoreCase = true) &&
                    processedUrl.contains(Constants.FACEBOOK_DOMAIN, ignoreCase = true) &&
                    !processedUrl.contains(Constants.FACEBOOKEZ_DOMAIN, ignoreCase = true))

        val toFixupx = processedUrl.contains(Constants.FIXUPX_DOMAIN, ignoreCase = true)
        val isTwitterConversion =
            ((url.contains(Constants.TWITTER_DOMAIN, ignoreCase = true) ||
                url.contains(Constants.X_DOMAIN, ignoreCase = true)) &&
                !url.contains(Constants.FIXUPX_DOMAIN, ignoreCase = true) && toFixupx) ||
                (url.contains(Constants.FIXUPX_DOMAIN, ignoreCase = true) && !toFixupx &&
                    (processedUrl.contains(Constants.X_DOMAIN, ignoreCase = true) ||
                        processedUrl.contains(Constants.TWITTER_DOMAIN, ignoreCase = true))) ||
                (url.contains(Constants.FXTWITTER_DOMAIN, ignoreCase = true) &&
                    (toFixupx || processedUrl.contains(Constants.X_DOMAIN, ignoreCase = true))) ||
                (url.contains(Constants.VXTWITTER_DOMAIN, ignoreCase = true) &&
                    (toFixupx || processedUrl.contains(Constants.X_DOMAIN, ignoreCase = true)))

        // TikTok mirrors the Instagram logic: contains(TIKTOK_DOMAIN) is also true for
        // proxy hosts like kktiktok.com (substring), so proxy checks come first / combined.
        val tiktokProxies = TikTokProxyStore.allKnownProxies()
        val urlHasTikTokProxy = tiktokProxies.any { url.contains(it, ignoreCase = true) }
        val resultHasTikTokProxy = tiktokProxies.any { processedUrl.contains(it, ignoreCase = true) }
        val isTikTokConversion =
            (url.contains(Constants.TIKTOK_DOMAIN, ignoreCase = true) && !urlHasTikTokProxy && resultHasTikTokProxy) ||
                (urlHasTikTokProxy && processedUrl.contains(Constants.TIKTOK_DOMAIN, ignoreCase = true) && !resultHasTikTokProxy) ||
                (urlHasTikTokProxy && resultHasTikTokProxy && tiktokProxies.none { p ->
                    url.contains(p, ignoreCase = true) && processedUrl.contains(p, ignoreCase = true)
                })

        return when {
            isInstagramConversion || isFacebookConversion || isTwitterConversion || isTikTokConversion -> CONVERSION_DOMAIN_CONVERTED
            trackingRemoved -> CONVERSION_TRACKING_REMOVED
            else -> CONVERSION_URL_CLEANED
        }
    }

    private suspend fun saveHistoryEntry(url: String, processedUrl: String, platform: String, wasAlreadyClean: Boolean) {
        val conversionType = classifyConversion(url, processedUrl, trackingRemoved = !wasAlreadyClean)
        try {
            historyRepository.insertHistory(
                originalUrl = url,
                cleanedUrl = processedUrl,
                platform = platform,
                conversionType = conversionType
            )
            historyRepository.trimHistory(preferencesManager.getMaxHistoryEntries())
        } catch (e: Exception) {
            Timber.e(e, "Failed to save history entry")
        }
    }
    
    override suspend fun processUrl(url: String): ProcessedUrlResult = processUrl(url, false)
    
    override suspend fun processUrl(url: String, forceCleanTracking: Boolean): ProcessedUrlResult = 
        processUrl(url, forceCleanTracking, null)
    
    override suspend fun processUrl(url: String, forceCleanTracking: Boolean, previousProcessedUrl: String?): ProcessedUrlResult = withContext(Dispatchers.IO) {
        
        if (url.isEmpty()) return@withContext ProcessedUrlResult(url, true)
        
        val isInstagram = urlProcessor.isInstagramUrl(url)
        val isFacebook = urlProcessor.isFacebookUrl(url)
        val isTikTok = urlProcessor.isTikTokUrl(url)
        
        val platform = detectPlatform(url)
        
        val instagramProxy = preferencesManager.getInstagramProxy()
        val tiktokProxy = preferencesManager.getTikTokProxy()

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
        } else if (isTikTok) {
            // For TikTok URLs, use the TikTok conversion preference
            urlProcessor.processUrl(
                url,
                cleanTracking = forceCleanTracking || preferencesManager.isCleanTrackingEnabled(),
                convertTwitter = preferencesManager.isConvertTikTokEnabled(),
                tiktokProxy = tiktokProxy
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
            saveHistoryEntry(url, processedUrl, platform, wasAlreadyClean)
        }
        
        ProcessedUrlResult(processedUrl, wasAlreadyClean)
    }
    
    override suspend fun processUrlWithoutHistory(url: String): ProcessedUrlResult = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext ProcessedUrlResult(url, true)
        
        val isInstagram = urlProcessor.isInstagramUrl(url)
        val isFacebook = urlProcessor.isFacebookUrl(url)
        val isTikTok = urlProcessor.isTikTokUrl(url)
        val instagramProxy = preferencesManager.getInstagramProxy()
        val tiktokProxy = preferencesManager.getTikTokProxy()
        
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
        } else if (isTikTok) {
            // For TikTok URLs, use the TikTok conversion preference
            urlProcessor.processUrl(
                url,
                cleanTracking = preferencesManager.isCleanTrackingEnabled(),
                convertTwitter = preferencesManager.isConvertTikTokEnabled(),
                tiktokProxy = tiktokProxy
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
        urlProcessor.processUrlForSharing(
            url,
            preferencesManager.getInstagramProxy(),
            preferencesManager.getTikTokProxy()
        )
    }
    
    override suspend fun cleanUrl(url: String): String = withContext(Dispatchers.IO) {
        // Clean URL with history tracking
        val result = processUrl(url, true)
        result.url
    }
    
    override fun isInstagramUrl(url: String): Boolean = urlProcessor.isInstagramUrl(url)

    override fun isFacebookUrl(url: String): Boolean = urlProcessor.isFacebookUrl(url)
    
    override fun isTwitterUrl(url: String): Boolean = urlProcessor.isTwitterUrl(url)
    
    override fun isTikTokUrl(url: String): Boolean = urlProcessor.isTikTokUrl(url)
    
    override fun hasTrackingParameters(url: String): Boolean = urlProcessor.hasTrackingParameters(url)
    
    override fun isInstagramConversionEnabled(): Flow<Boolean> =
        preferencesManager.booleanFlow(PreferencesManager.KEY_CONVERT_INSTAGRAM, default = true)
    
    override suspend fun setInstagramConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (preferencesManager.isConvertInstagramEnabled() != enabled) {
                preferencesManager.setConvertInstagramEnabled(enabled)
            }
        }
    }
    
    override fun isTrackingRemovalEnabled(): Flow<Boolean> =
        preferencesManager.booleanFlow(PreferencesManager.KEY_CLEAN_TRACKING, default = true)
    
    override suspend fun setTrackingRemovalEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (preferencesManager.isCleanTrackingEnabled() != enabled) {
                preferencesManager.setCleanTrackingEnabled(enabled)
            }
        }
    }
    
    override fun isTwitterConversionEnabled(): Flow<Boolean> =
        preferencesManager.booleanFlow(PreferencesManager.KEY_CONVERT_TWITTER, default = true)
    
    override suspend fun setTwitterConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (preferencesManager.isConvertTwitterEnabled() != enabled) {
                preferencesManager.setConvertTwitterEnabled(enabled)
            }
        }
    }
    
    override fun isTikTokConversionEnabled(): Flow<Boolean> =
        preferencesManager.booleanFlow(PreferencesManager.KEY_CONVERT_TIKTOK, default = true)
    
    override suspend fun setTikTokConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (preferencesManager.isConvertTikTokEnabled() != enabled) {
                preferencesManager.setConvertTikTokEnabled(enabled)
            }
        }
    }
    
    override suspend fun processUrlForBrowser(url: String): ProcessedUrlResult = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext ProcessedUrlResult(url, true)
        
        val isInstagram = urlProcessor.isInstagramUrl(url)
        val isTwitter = urlProcessor.isTwitterUrl(url)
        val isFacebook = urlProcessor.isFacebookUrl(url)
        val isTikTok = urlProcessor.isTikTokUrl(url)
        
        val platform = detectPlatform(url)
        
        // Use browser-specific conversion preferences; Instagram/TikTok proxies are shared with main app
        val instagramProxy = preferencesManager.getInstagramProxy()
        val tiktokProxy = preferencesManager.getTikTokProxy()
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
            isTikTok -> {
                urlProcessor.processUrl(
                    url,
                    cleanTracking = preferencesManager.isCleanTrackingEnabled(),
                    convertTwitter = preferencesManager.isBrowserConvertTikTokEnabled(),
                    tiktokProxy = tiktokProxy
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
            saveHistoryEntry(url, processedUrl, platform, wasAlreadyClean)
        }
        
        ProcessedUrlResult(processedUrl, wasAlreadyClean)
    }

} 