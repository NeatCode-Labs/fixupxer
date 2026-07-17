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

import com.fixupxer.cleaners.cache.CachedCleanResult
import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.cleaners.model.ProcessingResult
import com.fixupxer.processing.ChangeOperation
import com.fixupxer.processing.ChangeOperationType
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.utils.Constants
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
        deepCleanWithDetailsWithoutCache(url, maxPasses).cleanedUrl
    
    /**
     * Clean a URL and return detailed, non-sensitive processing information.
     */
    fun deepCleanWithDetails(url: String, maxPasses: Int = 5): ProcessingResult {
        val cached = cache.getOrCompute(url) {
            val result = performDeepCleanWithDetails(url, maxPasses)
            CachedCleanResult(result.cleanedUrl, result.operations, result.totalPasses)
        }

        return ProcessingResult(
            originalUrl = url,
            cleanedUrl = cached.cleanedUrl,
            operations = cached.operations,
            totalPasses = cached.totalPasses
        )
    }

    /**
     * Clean a URL without using the cache and return non-sensitive details.
     */
    fun deepCleanWithDetailsWithoutCache(url: String, maxPasses: Int = 5): ProcessingResult =
        performDeepCleanWithDetails(url, maxPasses)
    
    private fun performDeepCleanWithDetails(url: String, maxPasses: Int): ProcessingResult {
        val detailedLogging = false
        
        if (detailedLogging) {
            Timber.d("===== URL Cleaning Start =====")
            Timber.d(
                "Original URL host=${UrlNormalizer.extractAsciiHost(url) ?: "unknown"}, length=${url.length}"
            )
            Timber.d("Max passes: $maxPasses")
        }
        
        var current = url
        var passCount = 0
        val operations = mutableListOf<ChangeOperation>()
        
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
                return processingResult(url, current, operations, passCount)
            }
            
            // Apply cleaners in sequence (extraction → conversion → parameter removal)
            val next = cleaners
                .sortedBy { it.priority }
                .asSequence()
                .fold(current) { urlToClean, cleaner ->
                    
                    if (detailedLogging) {
                        Timber.d("Applying cleaner: ${cleaner.id}")
                    }
                    
                    try {
                        val cleaned = cleaner.clean(urlToClean)
                        if (cleaned != urlToClean) {
                            operations.addBounded(
                                operationForCleanerChange(
                                    cleaner = cleaner,
                                    originalUrl = urlToClean,
                                    cleanedUrl = cleaned
                                )
                            )

                            Timber.d(
                                "Cleaner ${cleaner.id} modified URL " +
                                    "(host=${UrlNormalizer.extractAsciiHost(cleaned) ?: "unknown"})"
                            )
                        } else if (detailedLogging) {
                            Timber.d("Cleaner ${cleaner.id} made no changes")
                        }
                        cleaned
                    } catch (e: Exception) {
                        // Crash-safe: log error and continue with original URL
                        Timber.e(
                            e,
                            "Cleaner ${cleaner.id} failed " +
                                "(host=${UrlNormalizer.extractAsciiHost(urlToClean) ?: "unknown"})"
                        )
                        urlToClean
                    }
                }
            
            passCount++
            
            if (next == current) {
                // URL hasn't changed, we're done
                if (detailedLogging) {
                    Timber.d("URL stabilized after $passCount passes")
                    Timber.d("===== URL Cleaning Complete =====")
                    Timber.d(
                        "Final URL host=${UrlNormalizer.extractAsciiHost(current) ?: "unknown"}, " +
                            "length=${current.length}"
                    )
                } else {
                    Timber.d("URL stabilized after $passCount passes")
                }
                return processingResult(url, current, operations, passCount)
            }
            
            current = next
        }
        
        if (detailedLogging) {
            Timber.d("Reached max passes ($maxPasses)")
            Timber.d("===== URL Cleaning Complete =====")
            Timber.d(
                "Final URL host=${UrlNormalizer.extractAsciiHost(current) ?: "unknown"}, " +
                    "length=${current.length}"
            )
        }
        
        return processingResult(url, current, operations, passCount)
    }

    private fun processingResult(
        originalUrl: String,
        cleanedUrl: String,
        operations: List<ChangeOperation>,
        totalPasses: Int
    ) = ProcessingResult(
        originalUrl = originalUrl,
        cleanedUrl = cleanedUrl,
        operations = operations.toList(),
        totalPasses = totalPasses
    )

    private fun operationForCleanerChange(
        cleaner: UrlCleaner,
        originalUrl: String,
        cleanedUrl: String
    ): ChangeOperation {
        val fromHost = UrlNormalizer.extractAsciiHost(originalUrl)
        val toHost = UrlNormalizer.extractAsciiHost(cleanedUrl)
        val removedNames = extractRemovedParameterNames(originalUrl, cleanedUrl)

        return when {
            fromHost != toHost || cleaner.priority == UrlCleaner.PRIORITY_EXTRACTION ->
                ChangeOperation(
                    type = ChangeOperationType.REDIRECT_EXTRACTED,
                    source = cleaner.displayName,
                    fromHost = fromHost,
                    toHost = toHost
                )
            removedNames.isNotEmpty() ->
                ChangeOperation(
                    type = ChangeOperationType.PARAMETERS_REMOVED,
                    source = cleaner.displayName,
                    parameterNames = removedNames
                )
            else ->
                ChangeOperation(
                    type = ChangeOperationType.URL_CANONICALIZED,
                    source = cleaner.displayName
                )
        }
    }

    /**
     * Compares raw query keys only, preserving original order and never reading values.
     */
    private fun extractRemovedParameterNames(originalUrl: String, cleanedUrl: String): List<String> {
        if (!originalUrl.contains("?") || originalUrl == cleanedUrl) return emptyList()

        return runCatching {
            val cleanedNames = extractParameterNames(cleanedUrl).toSet()
            extractParameterNames(originalUrl).filterNot(cleanedNames::contains)
        }.getOrDefault(emptyList())
    }

    private fun extractParameterNames(url: String): List<String> {
        val queryStart = url.indexOf('?')
        if (queryStart == -1) return emptyList()
        
        val queryEnd = url.indexOf('#', queryStart)
        val query = if (queryEnd > -1) {
            url.substring(queryStart + 1, queryEnd)
        } else {
            url.substring(queryStart + 1)
        }
        
        return query.split('&').mapNotNull { token ->
            token.substringBefore('=').takeIf { it.isNotEmpty() }
        }
    }

    private fun MutableList<ChangeOperation>.addBounded(operation: ChangeOperation) {
        if (size < Constants.MAX_CHANGE_OPERATIONS) add(operation)
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

    /** Evict a single raw input URL from the cleaner cache. */
    fun evictFromCache(url: String) {
        cache.remove(url)
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats() = cache.getStats()
} 