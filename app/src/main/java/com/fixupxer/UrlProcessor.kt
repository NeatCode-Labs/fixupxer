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
     * @return Pair of (processedUrl, wasAlreadyClean)
     */
    fun processUrl(url: String, cleanTracking: Boolean, convertTwitter: Boolean): Pair<String, Boolean> {
        if (url.isEmpty()) {
            throw IllegalArgumentException("Please enter a URL")
        }
        
        val trimmedUrl = url.trim()
        
        // First try to find a valid URL in the input
        val validUrl = findFirstValidUrl(trimmedUrl) ?: throw IllegalArgumentException("Invalid URL format")
        
        return try {
            // Handle URLs that might start with @ (common for Instagram shares)
            var inputUrl = validUrl
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
            
            // Always check if URL has tracking parameters for cleanliness detection
            val urlHasTrackingParams = hasTrackingParameters(decodedUrl)
            
            // Check if it's an Instagram-related URL (including kkinstagram)
            val isInstagram = isInstagramUrl(decodedUrl)
            val isKkInstagram = decodedUrl.contains(Constants.KKINSTAGRAM_DOMAIN, ignoreCase = true)
            
            if (isInstagram || isKkInstagram) {
                Timber.d("Instagram/KKInstagram URL detected: $decodedUrl")
                
                // Clean tracking parameters if enabled
                val cleanedUrl = if (cleanTracking) {
                    removeTrackingParameters(decodedUrl)
                } else {
                    decodedUrl
                }
                
                // Determine what needs to be done based on current state and desired state
                val needsConversion = when {
                    convertTwitter && isInstagram && !isKkInstagram -> true // Need to convert to kkinstagram
                    !convertTwitter && isKkInstagram -> true // Need to convert back to instagram
                    else -> false
                }
                
                val finalUrl = when {
                    convertTwitter && !isKkInstagram -> convertToKkInstagram(cleanedUrl)
                    !convertTwitter && isKkInstagram -> convertFromKkInstagram(cleanedUrl)
                    else -> cleanedUrl
                }
                
                // URL is already clean if no tracking params exist AND no conversion was needed
                val wasAlreadyClean = !urlHasTrackingParams && !needsConversion
                
                Timber.d("Instagram URL processing - hadTracking: $urlHasTrackingParams, needsConversion: $needsConversion, wasAlreadyClean: $wasAlreadyClean")
                
                return Pair(finalUrl, wasAlreadyClean)
            }
            
            // Check if it's a Twitter/X-related URL (including fixupx and fxtwitter)
            val isTwitter = isTwitterUrl(decodedUrl)
            val isFixupx = decodedUrl.contains(Constants.FIXUPX_DOMAIN, ignoreCase = true)
            val isFxTwitter = decodedUrl.contains(Constants.FXTWITTER_DOMAIN, ignoreCase = true)
            
            if (isTwitter || isFixupx || isFxTwitter) {
                Timber.d("Twitter/Fixupx/FxTwitter URL detected: $decodedUrl")
                
                // Clean tracking parameters if enabled
                val cleanedUrl = if (cleanTracking) {
                    removeTrackingParameters(decodedUrl)
                } else {
                    decodedUrl
                }
                
                // Determine what needs to be done based on current state and desired state
                val needsConversion = when {
                    convertTwitter && (isTwitter || isFxTwitter) && !isFixupx -> true // Need to convert to fixupx
                    !convertTwitter && (isFixupx || isFxTwitter) -> true // Need to convert back to x.com
                    else -> false
                }
                
                val finalUrl = when {
                    convertTwitter && !isFixupx -> {
                        if (isFxTwitter) {
                            // Convert fxtwitter to fixupx
                            cleanedUrl.replace(Constants.FXTWITTER_DOMAIN, Constants.FIXUPX_DOMAIN, ignoreCase = true)
                        } else {
                            // Convert x.com/twitter.com to fixupx
                            // Use direct domain replacement for robustness
                            if (cleanedUrl.contains(Constants.X_DOMAIN, ignoreCase = true)) {
                                cleanedUrl.replace(Constants.X_DOMAIN, Constants.FIXUPX_DOMAIN, ignoreCase = true)
                            } else if (cleanedUrl.contains(Constants.TWITTER_DOMAIN, ignoreCase = true)) {
                                cleanedUrl.replace(Constants.TWITTER_DOMAIN, Constants.FIXUPX_DOMAIN, ignoreCase = true)
                            } else {
                                cleanedUrl
                            }
                        }
                    }
                    !convertTwitter && isFixupx -> convertFromFixupx(cleanedUrl)
                    !convertTwitter && isFxTwitter -> convertFromFxTwitter(cleanedUrl)
                    else -> cleanedUrl
                }
                
                // URL is already clean if no tracking params exist AND no conversion was needed
                val wasAlreadyClean = !urlHasTrackingParams && !needsConversion
                
                Timber.d("Twitter URL processing - hadTracking: $urlHasTrackingParams, needsConversion: $needsConversion, wasAlreadyClean: $wasAlreadyClean")
                
                return Pair(finalUrl, wasAlreadyClean)
            }
            
            // For other URLs (non-Instagram, non-Twitter)
            var processedUrl = decodedUrl
            val originallyHadTracking = hasTrackingParameters(decodedUrl)
            // Clean tracking parameters if enabled
            if (cleanTracking) {
                processedUrl = removeTrackingParameters(processedUrl)
            }
            // URL is already clean if it had no tracking parameters
            val wasAlreadyClean = !originallyHadTracking
            Timber.d("Other URL processing - hadTracking: $originallyHadTracking, wasAlreadyClean: $wasAlreadyClean")
            Pair(processedUrl, wasAlreadyClean)
        } catch (e: Exception) {
            Timber.e(e, "Error processing URL")
            throw IllegalArgumentException("Error processing URL: ${e.message}")
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
                    // Still clean tracking parameters from kkinstagram URLs
                    removeTrackingParameters(decodedUrl)
                }
                isTwitterUrl(decodedUrl) -> {
                    Timber.d("Twitter URL for sharing: $decodedUrl")
                    
                    // First remove tracking parameters
                    val cleaned = removeTrackingParameters(decodedUrl)
                    
                    // Then convert to fixupx
                    convertTwitterUrl(cleaned)
                }
                decodedUrl.contains(Constants.FXTWITTER_DOMAIN, ignoreCase = true) -> {
                    Timber.d("FxTwitter URL for sharing: $decodedUrl")
                    
                    // First remove tracking parameters
                    val cleaned = removeTrackingParameters(decodedUrl)
                    
                    // Convert fxtwitter to fixupx
                    cleaned.replace(Constants.FXTWITTER_DOMAIN, Constants.FIXUPX_DOMAIN, ignoreCase = true)
                }
                decodedUrl.contains(Constants.FIXUPX_DOMAIN, ignoreCase = true) -> {
                    Timber.d("URL already contains fixupx: $decodedUrl")
                    // Still clean tracking parameters from fixupx URLs
                    removeTrackingParameters(decodedUrl)
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
    fun isValidUrl(url: String): Boolean {
        return try {
            // Try the simple validation first (works in tests)
            if (isValidUrlSimple(url)) return true
            
            // Fallback to Uri.parse for production (more robust)
            val uri = Uri.parse(url)
            uri.scheme != null && (uri.scheme == "http" || uri.scheme == "https")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Simple URL validation that works in both production and test environments
     */
    private fun isValidUrlSimple(url: String): Boolean {
        if (url.isEmpty()) return false
        
        // Basic pattern check
        if (!VALID_URL_PATTERN.matcher(url).matches()) return false
        
        // Must start with http:// or https://
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        
        // Must contain a domain with at least one dot
        val domainStart = url.indexOf("://") + 3
        val domainEnd = url.indexOf("/", domainStart).let { if (it == -1) url.length else it }
        val domain = url.substring(domainStart, domainEnd)
        
        // Remove port if present
        val domainWithoutPort = domain.split(":").first()
        
        // Must contain at least one dot and have valid domain structure
        if (!domainWithoutPort.contains(".")) return false
        
        val parts = domainWithoutPort.split(".")
        if (parts.size < 2) return false
        
        // Each part must be non-empty
        if (parts.any { it.isEmpty() }) return false
        
        return true
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
     * Convert kkinstagram URLs back to instagram.com
     */
    private fun convertFromKkInstagram(url: String): String {
        // If already contains instagram.com, return as is
        if (url.contains(Constants.INSTAGRAM_DOMAIN, ignoreCase = true) && 
            !url.contains(Constants.KKINSTAGRAM_DOMAIN, ignoreCase = true)) {
            return url
        }
        
        // Handle various kkinstagram subdomains
        val kkinstagramPattern = Pattern.compile(
            "(https?://)((?:www\\.)?(?:[a-z]+\\.)?)(kkinstagram\\.com)",
            Pattern.CASE_INSENSITIVE
        )
        
        val matcher = kkinstagramPattern.matcher(url)
        return if (matcher.find()) {
            matcher.replaceAll("$1$2${Constants.INSTAGRAM_DOMAIN}")
        } else {
            url.replace(Constants.KKINSTAGRAM_DOMAIN, Constants.INSTAGRAM_DOMAIN, ignoreCase = true)
        }
    }
    
    /**
     * Check if a URL is a Twitter/X URL
     */
    fun isTwitterUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        if (!(lowerUrl.contains(Constants.TWITTER_DOMAIN) ||
              lowerUrl.contains(Constants.X_DOMAIN) ||
              lowerUrl.contains(Constants.FXTWITTER_DOMAIN))) return false

        // Treat as Twitter/X link even if it doesn't contain /status/ so we can still clean tracking
        return true
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
            val uri = Uri.parse(url)
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
     * Convert fixupx URLs back to x.com
     */
    private fun convertFromFixupx(url: String): String {
        try {
            // If it doesn't contain fixupx, return unchanged
            if (!url.contains(Constants.FIXUPX_DOMAIN, ignoreCase = true)) {
                return url
            }
            
            // Replace fixupx.com with x.com
            return url.replace(Constants.FIXUPX_DOMAIN, Constants.X_DOMAIN, ignoreCase = true)
        } catch (e: Exception) {
            Timber.e(e, "Error converting from fixupx URL")
        }
        
        return url
    }
    
    /**
     * Convert fxtwitter URLs back to x.com
     */
    private fun convertFromFxTwitter(url: String): String {
        try {
            // If it doesn't contain fxtwitter, return unchanged
            if (!url.contains(Constants.FXTWITTER_DOMAIN, ignoreCase = true)) {
                return url
            }
            
            // Replace fxtwitter.com with x.com
            return url.replace(Constants.FXTWITTER_DOMAIN, Constants.X_DOMAIN, ignoreCase = true)
        } catch (e: Exception) {
            Timber.e(e, "Error converting from fxtwitter URL")
        }
        
        return url
    }
    
    /**
     * Check if a URL contains any tracking parameters (robust manual implementation)
     */
    fun hasTrackingParameters(url: String): Boolean {
        val idx = url.indexOf('?')
        if (idx == -1) return false
        val query = url.substring(idx + 1)
        return query.split('&').any { pair ->
            val eqIdx = pair.indexOf('=')
            if (eqIdx == -1) return@any false
            val key = pair.substring(0, eqIdx).lowercase()
            trackingParams.contains(key)
        }
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
            // Always use manual fallback for robust removal
            val idx = url.indexOf('?')
            if (idx == -1) return url
            val base = url.substring(0, idx)
            val query = url.substring(idx + 1)
            val kept = query.split('&').mapNotNull { pair ->
                val eqIdx = pair.indexOf('=')
                if (eqIdx == -1) return@mapNotNull null
                val key = pair.substring(0, eqIdx).lowercase()
                val value = pair.substring(eqIdx + 1)
                if (!trackingParams.contains(key)) "$key=$value" else null
            }.filter { it.isNotEmpty() }
            return if (kept.isEmpty()) base else base + "?" + kept.joinToString("&")
        } catch (e: Exception) {
            Timber.e(e, "Error removing tracking parameters")
            return url
        }
    }
    
    /**
     * Check if a text contains a valid URL
     * @param text The text to check
     * @return The first valid URL found, or null if no valid URL
     */
    fun findFirstValidUrl(text: String): String? {
        // Limit input size to prevent performance issues
        val limitedText = if (text.length > MAX_INPUT_LENGTH) {
            text.substring(0, MAX_INPUT_LENGTH)
        } else {
            text
        }
        
        // Quick check: if text is too short to be a URL, return null
        if (limitedText.length < 4) { // minimum would be "a.co"
            return null
        }
        
        // First, check if the entire text might be a simple URL
        val trimmedText = limitedText.trim()
        if (trimmedText.isNotEmpty() && !trimmedText.contains("\n") && !trimmedText.contains("\r")) {
            // If it starts with http:// or https://, validate it directly
            if (trimmedText.startsWith("http://") || trimmedText.startsWith("https://")) {
                return if (isValidUrlSimple(trimmedText)) trimmedText else null
            }
            
            // If it looks like a domain without protocol
            // Must contain at least one dot and have valid domain structure
            if (!trimmedText.contains(" ") && trimmedText.contains(".")) {
                // Basic domain validation - must have at least 2 parts separated by dot
                val parts = trimmedText.split(".")
                if (parts.size >= 2 && parts.all { it.isNotEmpty() }) {
                    // Check if it matches a basic domain pattern
                    val domainPattern = Pattern.compile(
                        "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$"
                    )
                    if (domainPattern.matcher(trimmedText).matches()) {
                        val withProtocol = "https://$trimmedText"
                        return if (isValidUrlSimple(withProtocol)) withProtocol else null
                    }
                }
            }
        }
        
        // If the above quick checks fail, try to extract URLs using the pattern
        // But limit the search to avoid performance issues
        val searchText = if (limitedText.length > 1000) {
            limitedText.substring(0, 1000)
        } else {
            limitedText
        }
        
        return try {
            val matcher = URL_PATTERN.matcher(searchText)
            if (matcher.find()) {
                val foundUrl = matcher.group()
                // Additional validation for found URLs
                if (isValidUrlSimple(foundUrl)) {
                    foundUrl
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error finding URL in text")
            null
        }
    }
    
    /**
     * Clean a URL by removing tracking parameters only
     */
    fun cleanUrl(url: String): String {
        if (url.isEmpty()) {
            return url
        }
        
        return try {
            val decodedUrl = try {
                URLDecoder.decode(url.trim(), "UTF-8")
            } catch (e: Exception) {
                url.trim()
            }
            
            // Remove tracking parameters
            removeTrackingParameters(decodedUrl)
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning URL")
            url
        }
    }
    
    companion object {
        // Maximum input size to prevent performance issues
        private const val MAX_INPUT_LENGTH = 10000
        
        // Pre-compiled regex pattern for better performance
        private val URL_PATTERN = Pattern.compile(
            "(https?|ftp)://[^\\s/\$.?#].[^\\s]*",
            Pattern.CASE_INSENSITIVE
        )
        
        // More comprehensive URL validation pattern
        private val VALID_URL_PATTERN = Pattern.compile(
            "^(https?://)?" + // Optional protocol
            "((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|" + // Domain name
            "((\\d{1,3}\\.){3}\\d{1,3}))" + // OR IP address
            "(:\\d+)?(/[-a-z\\d%_.~+]*)*" + // Port and path
            "(\\?[;&a-z\\d%_.~+=-]*)?" + // Query string
            "(#[-a-z\\d_]*)?$", // Fragment
            Pattern.CASE_INSENSITIVE
        )
        
        /**
         * Simple URL validation that works in both production and test environments (static version)
         */
        private fun isValidUrlSimple(url: String): Boolean {
            if (url.isEmpty()) return false
            
            // Basic pattern check
            if (!VALID_URL_PATTERN.matcher(url).matches()) return false
            
            // Must start with http:// or https://
            if (!url.startsWith("http://") && !url.startsWith("https://")) return false
            
            // Must contain a domain with at least one dot
            val domainStart = url.indexOf("://") + 3
            val domainEnd = url.indexOf("/", domainStart).let { if (it == -1) url.length else it }
            val domain = url.substring(domainStart, domainEnd)
            
            // Remove port if present
            val domainWithoutPort = domain.split(":").first()
            
            // Must contain at least one dot and have valid domain structure
            if (!domainWithoutPort.contains(".")) return false
            
            val parts = domainWithoutPort.split(".")
            if (parts.size < 2) return false
            
            // Each part must be non-empty
            if (parts.any { it.isEmpty() }) return false
            
            return true
        }
        
        /**
         * Extract URLs from text
         */
        fun extractUrls(text: String): List<String> {
            // Limit input size to prevent performance issues
            val limitedText = if (text.length > MAX_INPUT_LENGTH) {
                text.substring(0, MAX_INPUT_LENGTH)
            } else {
                text
            }
            
            val matcher = URL_PATTERN.matcher(limitedText)
            val urls = mutableListOf<String>()
            while (matcher.find()) {
                urls.add(matcher.group())
            }
            return urls
        }
        
        /**
         * Check if a text contains a valid URL
         * @param text The text to check
         * @return The first valid URL found, or null if no valid URL
         */
        fun findFirstValidUrl(text: String): String? {
            // Limit input size to prevent performance issues
            val limitedText = if (text.length > MAX_INPUT_LENGTH) {
                text.substring(0, MAX_INPUT_LENGTH)
            } else {
                text
            }
            
            // Quick check: if text is too short to be a URL, return null
            if (limitedText.length < 4) { // minimum would be "a.co"
                return null
            }
            
            // First, check if the entire text might be a simple URL
            val trimmedText = limitedText.trim()
            if (trimmedText.isNotEmpty() && !trimmedText.contains("\n") && !trimmedText.contains("\r")) {
                // If it starts with http:// or https://, validate it directly
                if (trimmedText.startsWith("http://") || trimmedText.startsWith("https://")) {
                    return if (isValidUrlSimple(trimmedText)) trimmedText else null
                }
                
                // If it looks like a domain without protocol
                // Must contain at least one dot and have valid domain structure
                if (!trimmedText.contains(" ") && trimmedText.contains(".")) {
                    // Basic domain validation - must have at least 2 parts separated by dot
                    val parts = trimmedText.split(".")
                    if (parts.size >= 2 && parts.all { it.isNotEmpty() }) {
                        // Check if it matches a basic domain pattern
                        val domainPattern = Pattern.compile(
                            "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$"
                        )
                        if (domainPattern.matcher(trimmedText).matches()) {
                            val withProtocol = "https://$trimmedText"
                            return if (isValidUrlSimple(withProtocol)) withProtocol else null
                        }
                    }
                }
            }
            
            // If the above quick checks fail, try to extract URLs using the pattern
            // But limit the search to avoid performance issues
            val searchText = if (limitedText.length > 1000) {
                limitedText.substring(0, 1000)
            } else {
                limitedText
            }
            
            return try {
                val matcher = URL_PATTERN.matcher(searchText)
                if (matcher.find()) {
                    val foundUrl = matcher.group()
                    // Additional validation for found URLs
                    if (isValidUrlSimple(foundUrl)) {
                        foundUrl
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "Error finding URL in text")
                null
            }
        }
    }
} 