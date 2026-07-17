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


package com.fixupxer.cleaners.cache

import com.fixupxer.processing.ChangeOperation
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

data class CachedCleanResult(
    val cleanedUrl: String,
    val operations: List<ChangeOperation>,
    val totalPasses: Int
)

/**
 * Thread-safe LRU cache for cleaned URLs and their non-sensitive operation trace.
 */
@Singleton
class CleanerCache @Inject constructor() {
    companion object {
        private const val MAX_CACHE_SIZE = 100
    }
    
    // Thread-safe LRU cache implementation
    private val cache: MutableMap<String, CacheEntry> = Collections.synchronizedMap(
        object : LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, CacheEntry>): Boolean {
                return size > MAX_CACHE_SIZE
            }
        }
    )
    
    /**
     * Get a cleaned URL result from cache or compute it.
     */
    fun getOrCompute(url: String, compute: () -> CachedCleanResult): CachedCleanResult {
        // Check cache first
        cache[url]?.let { entry ->
            if (!entry.isExpired()) {
                return entry.result
            }
        }
        
        // Compute and cache
        val result = compute()
        cache[url] = CacheEntry(result, System.currentTimeMillis())
        return result
    }
    
    /**
     * Clear the entire cache
     */
    fun clear() {
        cache.clear()
    }

    /**
     * Remove one input URL after later pipeline stages expose sensitive output.
     */
    fun remove(url: String) {
        cache.remove(url)
    }
    
    /**
     * Get cache statistics
     */
    fun getStats(): CacheStats {
        val entries = cache.values.toList()
        return CacheStats(
            size = entries.size,
            maxSize = MAX_CACHE_SIZE,
            hitRate = 0f // Would need to track hits/misses for accurate rate
        )
    }
    
    /**
     * Cache entry with timestamp
     */
    private data class CacheEntry(
        val result: CachedCleanResult,
        val timestamp: Long
    ) {
        companion object {
            // Cache entries expire after 1 hour
            private const val EXPIRY_TIME_MS = 60 * 60 * 1000L
        }
        
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > EXPIRY_TIME_MS
        }
    }
    
    /**
     * Cache statistics
     */
    data class CacheStats(
        val size: Int,
        val maxSize: Int,
        val hitRate: Float
    )
} 