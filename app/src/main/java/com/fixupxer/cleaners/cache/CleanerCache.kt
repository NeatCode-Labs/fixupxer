package com.fixupxer.cleaners.cache

import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe LRU cache for cleaned URLs to improve performance
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
     * Get a cleaned URL from cache or compute it
     */
    fun getOrCompute(url: String, compute: () -> String): String {
        // Check cache first
        cache[url]?.let { entry ->
            if (!entry.isExpired()) {
                return entry.cleanedUrl
            }
        }
        
        // Compute and cache
        val cleanedUrl = compute()
        cache[url] = CacheEntry(cleanedUrl, System.currentTimeMillis())
        return cleanedUrl
    }
    
    /**
     * Clear the entire cache
     */
    fun clear() {
        cache.clear()
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
        val cleanedUrl: String,
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