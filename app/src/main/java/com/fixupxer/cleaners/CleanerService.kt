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


package com.fixupxer.cleaners

import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.cleaners.model.AppliedCleaner
import com.fixupxer.cleaners.model.ProcessingResult
import com.fixupxer.cleaners.model.RemovedParameter
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InstagramProxyStore
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that manages the URL cleaning process with iterative deep cleaning
 */
@Singleton
class CleanerService @Inject constructor(
    private val registry: CleanerRegistry,
    private val cache: CleanerCache
) {
    
    /**
     * Clean a URL using all applicable cleaners with iterative processing
     * 
     * @param url The URL to clean
     * @param maxPasses Maximum number of cleaning passes (default 5)
     * @return The cleaned URL
     */
    fun deepClean(url: String, maxPasses: Int = 5): String {
        val result = deepCleanWithDetails(url, maxPasses)
        return result.cleanedUrl
    }

    fun deepCleanWithoutCache(url: String, maxPasses: Int = 5): String =
        performDeepClean(url, maxPasses)
    
    /**
     * Clean a URL and return detailed processing information
     */
    fun deepCleanWithDetails(url: String, maxPasses: Int = 5): ProcessingResult {
        // Try cache first for the cleaned URL
        val cleanedUrl = cache.getOrCompute(url) {
            performDeepClean(url, maxPasses)
        }
        
        // If cached, we need to reconstruct the result
        if (cleanedUrl != url) {
            // For cached results, we can't provide detailed info
            return ProcessingResult(
                originalUrl = url,
                cleanedUrl = cleanedUrl,
                removedParameters = emptyList(),
                appliedCleaners = emptyList(),
                totalPasses = 1,
                wasModified = true
            )
        }
        
        // Otherwise, do a full processing run with details
        return performDeepCleanWithDetails(url, maxPasses)
    }
    
    private fun performDeepClean(url: String, maxPasses: Int): String {
        val result = performDeepCleanWithDetails(url, maxPasses)
        return result.cleanedUrl
    }
    
    private fun performDeepCleanWithDetails(url: String, maxPasses: Int): ProcessingResult {
        val detailedLogging = false
        
        if (detailedLogging) {
            Timber.d("===== URL Cleaning Start =====")
            Timber.d("Original URL: $url")
            Timber.d("Max passes: $maxPasses")
        }
        
        var current = url
        var passCount = 0
        val removedParameters = mutableListOf<RemovedParameter>()
        val appliedCleaners = mutableListOf<AppliedCleaner>()
        
        repeat(maxPasses) { pass ->
            if (detailedLogging) {
                Timber.d("--- Pass ${pass + 1} ---")
            }
            
            val cleaners = registry.getCleanersFor(current)
            
            if (detailedLogging) {
                Timber.d("Matching cleaners: ${cleaners.map { it.id }.joinToString()}")
            }
            
            if (cleaners.isEmpty()) {
                // No cleaners match, we're done
                if (detailedLogging) {
                    Timber.d("No cleaners match URL, stopping")
                }
                return ProcessingResult(
                    originalUrl = url,
                    cleanedUrl = current,
                    removedParameters = removedParameters,
                    appliedCleaners = appliedCleaners,
                    totalPasses = passCount
                )
            }
            
            // Apply cleaners in sequence (extraction → conversion → parameter removal)
            val next = cleaners
                .sortedBy { getCleanerPriority(it) }
                .asSequence()
                .fold(current) { urlToClean, cleaner ->
                    
                    if (detailedLogging) {
                        Timber.d("Applying cleaner: ${cleaner.id}")
                    }
                    
                    try {
                        val cleaned = cleaner.clean(urlToClean)
                        if (cleaned != urlToClean) {
                            
                            appliedCleaners.add(
                                AppliedCleaner(
                                    id = cleaner.id,
                                    name = getCleanerName(cleaner),
                                    action = describeAction(urlToClean, cleaned)
                                )
                            )
                            
                            // Extract removed parameters if applicable
                            extractRemovedParameters(urlToClean, cleaned)?.let { params ->
                                removedParameters.addAll(params)
                                if (detailedLogging) {
                                    Timber.d("Removed parameters: ${params.map { "${it.key}=${it.value}" }.joinToString()}")
                                }
                            }
                            
                            Timber.d("Cleaner ${cleaner.id} modified URL: $urlToClean → $cleaned")
                        } else if (detailedLogging) {
                            Timber.d("Cleaner ${cleaner.id} made no changes")
                        }
                        cleaned
                    } catch (e: Exception) {
                        // Crash-safe: log error and continue with original URL
                        Timber.e(e, "Cleaner ${cleaner.id} failed on URL: $urlToClean")
                        urlToClean
                    }
                }
            
            passCount++
            
            if (next == current) {
                // URL hasn't changed, we're done
                if (detailedLogging) {
                    Timber.d("URL stabilized after $passCount passes")
                    Timber.d("===== URL Cleaning Complete =====")
                    Timber.d("Final URL: $current")
                    if (removedParameters.isNotEmpty()) {
                        Timber.d("Total parameters removed: ${removedParameters.size}")
                    }
                } else {
                    Timber.d("URL stabilized after $passCount passes")
                }
                return ProcessingResult(
                    originalUrl = url,
                    cleanedUrl = current,
                    removedParameters = removedParameters,
                    appliedCleaners = appliedCleaners,
                    totalPasses = passCount
                )
            }
            
            current = next
        }
        
        if (detailedLogging) {
            Timber.d("Reached max passes ($maxPasses)")
            Timber.d("===== URL Cleaning Complete =====")
            Timber.d("Final URL: $current")
        }
        
        return ProcessingResult(
            originalUrl = url,
            cleanedUrl = current,
            removedParameters = removedParameters,
            appliedCleaners = appliedCleaners,
            totalPasses = passCount
        )
    }
    
    /**
     * Get priority for cleaner execution order
     * Lower number = higher priority (executes first)
     */
    private fun getCleanerPriority(cleaner: UrlCleaner): Int {
        return when {
            // URL extraction cleaners (e.g., Google redirect) should run first
            cleaner.id.contains("search") || 
            cleaner.id.contains("redirect") ||
            cleaner.id.contains("short") -> 1
            
            // Domain conversion cleaners (e.g., Twitter→FixupX) run second
            cleaner.id.contains("twitter") ||
            cleaner.id.contains("instagram") ||
            cleaner.id.contains("facebook") -> 2
            
            // Domain-specific parameter cleaners run third
            cleaner.id != "general" && cleaner.id != "general_tracking" -> 3
            
            // General parameter removal runs last
            cleaner.id == "general" || cleaner.id == "general_tracking" -> 4
            
            // Unknown cleaners
            else -> 5
        }
    }
    
    /**
     * Get human-readable name for a cleaner
     */
    private fun getCleanerName(cleaner: UrlCleaner): String {
        return when (cleaner.id) {
            "amazon" -> "Amazon"
            "youtube" -> "YouTube"
            "google_search" -> "Google Search"
            "twitter" -> "Twitter/X"
            "instagram" -> "Instagram"
            "facebook" -> "Facebook"
            "reddit" -> "Reddit"
            "tiktok" -> "TikTok"
            "linkedin" -> "LinkedIn"
            "substack" -> "Substack"
            "general_tracking" -> "General Tracking"
            else -> cleaner.id.replaceFirstChar { it.uppercaseChar() }
        }
    }
    
    /**
     * Describe what action the cleaner performed
     */
    private fun describeAction(originalUrl: String, cleanedUrl: String): String {
        return when {
            // URL extraction (e.g., Google redirect)
            originalUrl.contains("/url?") && !cleanedUrl.contains("/url?") -> "Extracted actual URL"
            
            // Domain conversion
            originalUrl.contains(Constants.TWITTER_DOMAIN) &&
                cleanedUrl.contains(Constants.FIXUPX_DOMAIN) -> "Converted to FixupX"
            originalUrl.contains(Constants.INSTAGRAM_DOMAIN) &&
                InstagramProxyStore.activeProxies().any { cleanedUrl.contains(it) } -> "Converted to Instagram proxy"
            originalUrl.contains(Constants.FACEBOOK_DOMAIN) &&
                cleanedUrl.contains(Constants.FACEBOOKEZ_DOMAIN) -> "Converted to FacebookEZ"
            
            // Product ID extraction
            originalUrl.contains("amazon") && cleanedUrl.contains("/dp/") && 
                originalUrl.length > cleanedUrl.length + 20 -> "Extracted product ID"
            
            // Parameter removal
            originalUrl.contains("?") && (!cleanedUrl.contains("?") || 
                cleanedUrl.substringAfter("?").length < originalUrl.substringAfter("?").length) -> 
                    "Removed tracking parameters"
            
            else -> "Cleaned URL"
        }
    }
    
    /**
     * Extract removed parameters by comparing URLs
     */
    private fun extractRemovedParameters(originalUrl: String, cleanedUrl: String): List<RemovedParameter>? {
        if (!originalUrl.contains("?") || originalUrl == cleanedUrl) return null
        
        try {
            val originalParams = extractParameters(originalUrl)
            val cleanedParams = extractParameters(cleanedUrl)
            
            return originalParams
                .filterNot { param -> cleanedParams.any { it.first == param.first } }
                .map { RemovedParameter(it.first, it.second) }
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Extract parameters from a URL
     */
    private fun extractParameters(url: String): List<Pair<String, String>> {
        val queryStart = url.indexOf('?')
        if (queryStart == -1) return emptyList()
        
        val queryEnd = url.indexOf('#', queryStart)
        val query = if (queryEnd > -1) {
            url.substring(queryStart + 1, queryEnd)
        } else {
            url.substring(queryStart + 1)
        }
        
        return query.split('&').mapNotNull { pair ->
            val parts = pair.split('=', limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }
    }
    
    /**
     * Clean a URL with a single pass (for backwards compatibility)
     */
    fun cleanUrl(url: String): String {
        return deepClean(url, maxPasses = 1)
    }
    
    /**
     * Check if any cleaners would modify the given URL
     */
    fun wouldModifyUrl(url: String): Boolean {
        val cleaned = cleanUrl(url)
        return cleaned != url
    }
    
    /**
     * Clear the cache
     */
    fun clearCache() {
        cache.clear()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats() = cache.getStats()
} 