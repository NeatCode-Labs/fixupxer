package com.fixupxer

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.fixupxer.utils.Constants

/**
 * A utility class for processing URLs to:
 * 1. Remove tracking parameters (like ClearURLs)
 * 2. Convert Twitter/X URLs to FixupX format for better embedding
 * 3. Convert Instagram URLs to kkinstagram.com for better embedding
 */
class UrlProcessor {
    // List of common tracking parameters to remove
    private val trackingParams = listOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid", "gclid", "ocid", "ncid", "ref", "referrer", "source", "source_platform",
        "share_id", "igshid", "mc_cid", "mc_eid", "yclid", "ml_subscriber", "ml_subscriber_hash",
        "oly_enc_id", "oly_anon_id", "_openstat", "marketo_tracking", "s", "t", 
        "__a", "__d", "_rdr", "hl", "fbadid", "vt", "campaign_id", "ad_id", "ad_set_id",
        "ig_cache_key", "ig_mid", "ig_share_sheet", "igsh", "igshid", "__cft__", "__tn__",
        "_branch_match_id", "epa", "_gl", "from_ad", "from_tiktok", "from_twitter",
        "from_web", "context", "contextual_post", "correlator", "timestamp",
        // New Facebook/Meta tracking parameters
        "mibextid", "fb_action_ids", "fb_action_types", "fb_source", "fb_ref",
        "fb_comment_id", "fb_story_location", "fb_dtsg_ag"
    )
    
    /**
     * Process a URL by cleaning tracking parameters and optionally converting 
     * Twitter/X URLs to fixupx.com format
     */
    fun processUrl(url: String, cleanTracking: Boolean, convertTwitter: Boolean): String {
        if (url.isEmpty()) {
            return url
        }
        
        try {
            // First check if it's an Instagram URL to treat it specially
            if (isInstagramUrl(url)) {
                Log.d(Constants.LOG_TAG, "Instagram URL detected: $url")
                
                // First remove any tracking parameters
                val cleanedUrl = if (cleanTracking) removeTrackingParameters(url) else url
                Log.d(Constants.LOG_TAG, "Instagram URL after cleaning: $cleanedUrl")
                
                // If we're supposed to convert Twitter (which means we're also handling Instagram)
                if (convertTwitter) {
                    val converted = convertToKkInstagram(cleanedUrl)
                    Log.d(Constants.LOG_TAG, "Instagram URL converted: $converted")
                    return converted
                }
                
                return cleanedUrl
            }
            
            // For non-Instagram URLs
            var processedUrl = url
            
            // Clean tracking parameters if enabled
            if (cleanTracking) {
                processedUrl = removeTrackingParameters(processedUrl)
            }
            
            // Convert Twitter/X URLs if enabled
            if (convertTwitter && isTwitterUrl(processedUrl)) {
                processedUrl = convertTwitterUrl(processedUrl)
            }
            
            return processedUrl
        } catch (e: Exception) {
            Log.e(Constants.LOG_TAG, "Error processing URL: ${e.message}")
            return url // Return original URL if there's an error
        }
    }
    
    /**
     * Process URL for sharing - this will convert to alternative
     * domain formats like fixupx.com and kkinstagram.com
     */
    fun processUrlForSharing(url: String): String {
        if (url.isEmpty()) {
            return url
        }
        
        try {
            // Handle Instagram URLs as a special case
            if (isInstagramUrl(url)) {
                Log.d(Constants.LOG_TAG, "Instagram URL for sharing: $url")
                
                // First remove tracking parameters
                val cleaned = removeTrackingParameters(url)
                Log.d(Constants.LOG_TAG, "Instagram URL cleaned: $cleaned")
                
                // Then convert to kkinstagram
                val converted = convertToKkInstagram(cleaned)
                Log.d(Constants.LOG_TAG, "Instagram URL converted for sharing: $converted")
                
                return converted
            } else if (url.contains(Constants.KKINSTAGRAM_DOMAIN, ignoreCase = true)) {
                Log.d(Constants.LOG_TAG, "URL already contains kkinstagram, no need to convert: $url")
                return url
            }
            
            // Handle Twitter URLs
            if (isTwitterUrl(url)) {
                Log.d(Constants.LOG_TAG, "Twitter URL for sharing: $url")
                
                // First remove tracking parameters
                val cleaned = removeTrackingParameters(url)
                
                // Then convert to fixupx
                return convertTwitterUrl(cleaned)
            }
            
            // For other URLs, just clean tracking parameters
            return removeTrackingParameters(url)
        } catch (e: Exception) {
            Log.e(Constants.LOG_TAG, "Error processing URL for sharing: ${e.message}")
            return url // Return original URL if there's an error
        }
    }
    
    /**
     * Check if a URL is an Instagram URL
     */
    fun isInstagramUrl(url: String): Boolean {
        return url.contains(Constants.INSTAGRAM_DOMAIN, ignoreCase = true)
    }
    
    /**
     * Convert Instagram URLs to kkinstagram.com
     */
    private fun convertToKkInstagram(url: String): String {
        // If already contains kkinstagram, return as is
        if (url.contains(Constants.KKINSTAGRAM_DOMAIN, ignoreCase = true)) {
            return url
        }
        return url.replace(Constants.INSTAGRAM_DOMAIN, Constants.KKINSTAGRAM_DOMAIN, ignoreCase = true)
    }
    
    /**
     * Check if a URL is a Twitter/X URL
     */
    private fun isTwitterUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return ((lowerUrl.contains(Constants.TWITTER_DOMAIN) || 
                lowerUrl.contains(Constants.X_DOMAIN)) && 
                lowerUrl.contains(Constants.TWITTER_STATUS_PATH))
    }
    
    /**
     * Convert Twitter URLs to fixupx.com format
     */
    private fun convertTwitterUrl(url: String): String {
        try {
            // If it's not a Twitter URL, return unchanged
            if (!isTwitterUrl(url)) {
                return url
            }
            
            // Parse the URL for better safety
            val uri = url.toUri()
            val path = uri.path ?: return url
            
            // Check if this is a status URL
            if (!path.contains(Constants.TWITTER_STATUS_PATH)) {
                return url
            }
            
            // Split on /status/ to get username and status ID
            val parts = path.split(Constants.TWITTER_STATUS_PATH)
            if (parts.size != 2) return url
            
            val username = parts[0].removePrefix("/")
            val statusId = parts[1].takeWhile { it.isDigit() }
            
            if (username.isNotEmpty() && statusId.isNotEmpty()) {
                return "https://${Constants.FIXUPX_DOMAIN}/$username${Constants.TWITTER_STATUS_PATH}$statusId"
            }
        } catch (e: Exception) {
            Log.e(Constants.LOG_TAG, "Error converting Twitter URL: ${e.message}")
        }
        
        return url
    }
    
    /**
     * Remove tracking parameters from a URL
     * Uses Uri parsing for better safety and reliability
     */
    private fun removeTrackingParameters(url: String): String {
        try {
            // Handle URLs that might start with @ (common for Instagram shares)
            var processedUrl = url
            if (processedUrl.startsWith("@")) {
                processedUrl = processedUrl.substring(1)
                Log.d(Constants.LOG_TAG, "Removed @ from beginning of URL: $processedUrl")
            }
            
            // If no query parameters, return as is
            if (!processedUrl.contains("?")) {
                return processedUrl
            }
            
            // Parse the URL using Android's Uri class
            val uri = processedUrl.toUri()
            val builder = uri.buildUpon().clearQuery()
            
            // Get all query parameters
            uri.queryParameterNames?.forEach { paramName ->
                // Keep parameters not in our tracking list
                if (!trackingParams.contains(paramName.lowercase())) {
                    uri.getQueryParameter(paramName)?.let { paramValue ->
                        builder.appendQueryParameter(paramName, paramValue)
                    }
                }
            }
            
            // Build the clean URL
            val cleanUrl = builder.build().toString()
            return cleanUrl
        } catch (e: Exception) {
            Log.e(Constants.LOG_TAG, "Error removing tracking parameters: ${e.message}")
            return url
        }
    }
    
    companion object {
        // Pre-compiled regex pattern for better performance
        private val URL_PATTERN = "(https?|ftp)://[^\\s/$.?#].[^\\s]*".toRegex()
        
        /**
         * Extract URLs from text
         */
        fun extractUrls(text: String): List<String> {
            return URL_PATTERN.findAll(text).map { it.value }.toList()
        }
    }
} 