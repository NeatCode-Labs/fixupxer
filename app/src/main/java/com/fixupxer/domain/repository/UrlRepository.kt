package com.fixupxer.domain.repository

import com.fixupxer.domain.model.ProcessedUrlResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for URL processing operations
 */
interface UrlRepository {
    /**
     * Process a URL by cleaning tracking parameters and optionally converting
     * Twitter/Instagram URLs based on user preferences
     */
    suspend fun processUrl(url: String): ProcessedUrlResult
    
    /**
     * Process a URL with option to force tracking removal
     */
    suspend fun processUrl(url: String, forceCleanTracking: Boolean): ProcessedUrlResult
    
    /**
     * Process URL for sharing - always converts to alternative domains
     */
    suspend fun processUrlForSharing(url: String): String
    
    /**
     * Check if a URL is an Instagram URL
     * @param url The URL to check
     * @return True if it's an Instagram URL
     */
    fun isInstagramUrl(url: String): Boolean
    
    /**
     * Check if a URL is a Twitter/X URL
     * @param url The URL to check
     * @return True if it's a Twitter/X URL
     */
    fun isTwitterUrl(url: String): Boolean
    
    /**
     * Check if a URL contains tracking parameters
     */
    fun hasTrackingParameters(url: String): Boolean
    
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