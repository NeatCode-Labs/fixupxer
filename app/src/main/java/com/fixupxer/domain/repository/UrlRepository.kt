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
     * Process a URL with force clean tracking option
     */
    suspend fun processUrl(url: String, forceCleanTracking: Boolean): ProcessedUrlResult
    
    /**
     * Process a URL with optional previous result for comparison
     */
    suspend fun processUrl(url: String, forceCleanTracking: Boolean, previousProcessedUrl: String?): ProcessedUrlResult
    
    /**
     * Process a URL without saving to history - for UI updates only
     */
    suspend fun processUrlWithoutHistory(url: String): ProcessedUrlResult
    
    /**
     * Process URL for sharing (always converts to alternative domains)
     */
    suspend fun processUrlForSharing(url: String): String
    
    /**
     * Clean a URL by removing tracking parameters
     */
    suspend fun cleanUrl(url: String): String
    
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
     * Check if a URL is a TikTok URL
     * @param url The URL to check
     * @return True if it's a TikTok URL
     */
    fun isTikTokUrl(url: String): Boolean
    
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
    
    /**
     * Process URL for browser mode with browser-specific conversion settings
     */
    suspend fun processUrlForBrowser(url: String): ProcessedUrlResult

} 