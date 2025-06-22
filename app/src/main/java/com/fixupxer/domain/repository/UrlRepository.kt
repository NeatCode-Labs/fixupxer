package com.fixupxer.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for URL processing operations
 */
interface UrlRepository {
    /**
     * Process a URL according to user preferences
     * @param url The URL to process
     * @return The processed URL
     */
    suspend fun processUrl(url: String): String
    
    /**
     * Process a URL specifically for sharing (with all conversions enabled)
     * @param url The URL to process
     * @return The processed URL ready for sharing
     */
    suspend fun processUrlForSharing(url: String): String
    
    /**
     * Check if a URL is an Instagram URL
     * @param url The URL to check
     * @return True if it's an Instagram URL
     */
    fun isInstagramUrl(url: String): Boolean
    
    /**
     * Get the current state of Instagram conversion preference
     */
    fun isInstagramConversionEnabled(): Flow<Boolean>
    
    /**
     * Set Instagram conversion preference
     */
    suspend fun setInstagramConversionEnabled(enabled: Boolean)
    
    /**
     * Get the current state of tracking removal preference
     */
    fun isTrackingRemovalEnabled(): Flow<Boolean>
    
    /**
     * Set tracking removal preference
     */
    suspend fun setTrackingRemovalEnabled(enabled: Boolean)
    
    /**
     * Get the current state of Twitter conversion preference
     */
    fun isTwitterConversionEnabled(): Flow<Boolean>
    
    /**
     * Set Twitter conversion preference
     */
    suspend fun setTwitterConversionEnabled(enabled: Boolean)
} 