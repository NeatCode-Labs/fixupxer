package com.fixupxer

import android.net.Uri
import androidx.core.net.toUri
import com.fixupxer.data.config.TrackingParameters
import com.fixupxer.utils.Constants
import timber.log.Timber
import java.net.URLDecoder
import java.util.regex.Pattern

/**
 * A utility class for processing URLs to:
 * 1. Remove tracking parameters (like ClearURLs)
 * 2. Convert Twitter/X URLs to FixupX format for better embedding
 * 3. Convert Instagram URLs to kkinstagram.com for better embedding
 */
class UrlProcessor {
    // Use tracking parameters from configuration
    private val trackingParams = TrackingParameters.allParameters
    
    /**
     * Process a URL by cleaning tracking parameters and optionally converting 
     * Twitter/X URLs to fixupx.com format
     */
    fun processUrl(url: String, cleanTracking: Boolean, convertTwitter: Boolean): String {
        if (url.isEmpty()) {
            return url
        }
        
        return try {
            // Handle URLs that might start with @ (common for Instagram shares)
            var inputUrl = url.trim()
            if (inputUrl.startsWith("@")) {
                inputUrl = inputUrl.substring(1)
                Timber.d("Removed @ from beginning of URL: $inputUrl")
            }
            
            // Decode URL if it's encoded
            val decodedUrl = try {
                URLDecoder.decode(inputUrl, "UTF-8")
            } catch (e: Exception) {
                inputUrl
            }
            
            // Validate URL format
            if (!isValidUrl(decodedUrl)) {
                Timber.w("Invalid URL format: $decodedUrl")
                return url
            }
            
            // First check if it's an Instagram URL to treat it specially
            if (isInstagramUrl(decodedUrl)) {
                Timber.d("Instagram URL detected: $decodedUrl")
                
                // First remove any tracking parameters
                val cleanedUrl = if (cleanTracking) removeTrackingParameters(decodedUrl) else decodedUrl
                Timber.d("Instagram URL after cleaning: $cleanedUrl")
                
                // If we're supposed to convert Twitter (which means we're also handling Instagram)
                if (convertTwitter) {
                    val converted = convertToKkInstagram(cleanedUrl)
                    Timber.d("Instagram URL converted: $converted")
                    return converted
                }
                
                return cleanedUrl
            }
            
            // For non-Instagram URLs
            var processedUrl = decodedUrl
            
            // Clean tracking parameters if enabled
            if (cleanTracking) {
                processedUrl = removeTrackingParameters(processedUrl)
            }
            
            // Convert Twitter/X URLs if enabled
            if (convertTwitter && isTwitterUrl(processedUrl)) {
                processedUrl = convertTwitterUrl(processedUrl)
            }
            
            processedUrl
        } catch (e: Exception) {
            Timber.e(e, "Error processing URL")
            url // Return original URL if there's an error
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
        
        return try {
            val decodedUrl = try {
                URLDecoder.decode(url.trim(), "UTF-8")
            } catch (e: Exception) {
                url.trim()
            }
            
            // Handle Instagram URLs as a special case
            when {
                isInstagramUrl(decodedUrl) -> {
                    Timber.d("Instagram URL for sharing: $decodedUrl")
                    
                    // First remove tracking parameters
                    val cleaned = removeTrackingParameters(decodedUrl)
                    Timber.d("Instagram URL cleaned: $cleaned")
                    
                    // Then convert to kkinstagram
                    val converted = convertToKkInstagram(cleaned)
                    Timber.d("Instagram URL converted for sharing: $converted")
                    
                    converted
                }
                decodedUrl.contains(Constants.KKINSTAGRAM_DOMAIN, ignoreCase = true) -> {
                    Timber.d("URL already contains kkinstagram: $decodedUrl")
                    decodedUrl
                }
                isTwitterUrl(decodedUrl) -> {
                    Timber.d("Twitter URL for sharing: $decodedUrl")
                    
                    // First remove tracking parameters
                    val cleaned = removeTrackingParameters(decodedUrl)
                    
                    // Then convert to fixupx
                    convertTwitterUrl(cleaned)
                }
                else -> {
                    // For other URLs, just clean tracking parameters
                    removeTrackingParameters(decodedUrl)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing URL for sharing")
            url // Return original URL if there's an error
        }
    }
    
    /**
     * Check if a URL is valid
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = url.toUri()
            uri.scheme != null && (uri.scheme == "http" || uri.scheme == "https")
        } catch (e: Exception) {
            false
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
        
        // Handle various Instagram subdomains
        val instagramPattern = Pattern.compile(
            "(https?://)((?:www\\.)?(?:[a-z]+\\.)?)(instagram\\.com)",
            Pattern.CASE_INSENSITIVE
        )
        
        val matcher = instagramPattern.matcher(url)
        return if (matcher.find()) {
            matcher.replaceAll("$1$2${Constants.KKINSTAGRAM_DOMAIN}")
        } else {
            url.replace(Constants.INSTAGRAM_DOMAIN, Constants.KKINSTAGRAM_DOMAIN, ignoreCase = true)
        }
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
            val statusIdMatch = Pattern.compile("^(\\d+)").matcher(parts[1])
            
            if (username.isNotEmpty() && statusIdMatch.find()) {
                val statusId = statusIdMatch.group(1)
                return "https://${Constants.FIXUPX_DOMAIN}/$username${Constants.TWITTER_STATUS_PATH}$statusId"
            }
        } catch (e: Exception) {
            Timber.e(e, "Error converting Twitter URL")
        }
        
        return url
    }
    
    /**
     * Remove tracking parameters from a URL
     * Uses Uri parsing for better safety and reliability
     */
    private fun removeTrackingParameters(url: String): String {
        try {
            // If no query parameters, return as is
            if (!url.contains("?")) {
                return url
            }
            
            // Parse the URL using Android's Uri class
            val uri = url.toUri()
            val builder = uri.buildUpon().clearQuery()
            
            // Get all query parameters
            uri.queryParameterNames?.forEach { paramName ->
                // Keep parameters not in our tracking list (case-insensitive check)
                if (!trackingParams.contains(paramName.lowercase())) {
                    uri.getQueryParameter(paramName)?.let { paramValue ->
                        builder.appendQueryParameter(paramName, paramValue)
                    }
                }
            }
            
            // Build the clean URL
            val cleanUrl = builder.build().toString()
            
            // Remove trailing ? if no parameters remain
            return if (cleanUrl.endsWith("?")) {
                cleanUrl.dropLast(1)
            } else {
                cleanUrl
            }
        } catch (e: Exception) {
            Timber.e(e, "Error removing tracking parameters")
            return url
        }
    }
    
    companion object {
        // Pre-compiled regex pattern for better performance
        private val URL_PATTERN = Pattern.compile(
            "(https?|ftp)://[^\\s/\$.?#].[^\\s]*",
            Pattern.CASE_INSENSITIVE
        )
        
        /**
         * Extract URLs from text
         */
        fun extractUrls(text: String): List<String> {
            val matcher = URL_PATTERN.matcher(text)
            val urls = mutableListOf<String>()
            while (matcher.find()) {
                urls.add(matcher.group())
            }
            return urls
        }
    }
} 