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


package com.fixupxer

import android.net.Uri
import androidx.core.net.toUri
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.utils.Constants
import timber.log.Timber
import java.net.IDN
import java.net.URI
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton
import java.util.regex.Pattern

/**
 * A utility class for processing URLs to:
 * 1. Remove tracking parameters (like ClearURLs)
 * 2. Convert Twitter/X URLs to FixupX format for better embedding
 * 3. Convert Instagram URLs to a user-selected proxy for better embedding
 */
@Singleton
class UrlProcessor @Inject constructor(
    private val cleanerService: CleanerService
) {
    
    /**
     * Process a URL by cleaning tracking parameters and optionally converting 
     * Twitter/X URLs to fixupx.com format and Instagram URLs to a selected proxy
     * @param instagramProxy which Instagram proxy to convert to (default [Constants.INSTAGRAM_DEFAULT_PROXY])
     * @return Pair of (processedUrl, wasAlreadyClean)
     */
    fun processUrl(
        url: String,
        cleanTracking: Boolean,
        convertTwitter: Boolean,
        instagramProxy: String = Constants.INSTAGRAM_DEFAULT_PROXY
    ): Pair<String, Boolean> {
        if (url.isEmpty()) {
            throw IllegalArgumentException("Please enter a URL")
        }
        
        val trimmedUrl = url.trim()
        
        // First try to find a valid URL in the input
        val validUrl = findFirstValidUrl(trimmedUrl) ?: throw IllegalArgumentException("Invalid URL format")
        
        return try {
            // Preprocess URL to handle IDN and zero-width characters
            val preprocessedUrl = preprocessUrl(validUrl)
            
            // Handle URLs that might start with @ (common for Instagram shares)
            var inputUrl = preprocessedUrl
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
            
            var finalUrl = decodedUrl
            var wasAlreadyClean = true
            
            // Clean tracking parameters if requested
            if (cleanTracking) {
                Timber.d("Using new cleaner service for URL: $decodedUrl")
                wasAlreadyClean = !cleanerService.wouldModifyUrl(decodedUrl)
                finalUrl = cleanerService.deepClean(decodedUrl)
            }
            
            // Apply domain conversions based on settings
            val convertedUrl = applyDomainConversions(
                finalUrl,
                convertToAlternative = convertTwitter,
                instagramProxy = instagramProxy
            )
            
            // Check if any changes were made
            val wasModified = convertedUrl != decodedUrl
            
            Pair(convertedUrl, wasAlreadyClean && !wasModified)
        } catch (e: Exception) {
            Timber.e(e, "Error processing URL")
            throw IllegalArgumentException("Error processing URL: ${e.message}")
        }
    }
    
    /**
     * Preprocess URL to handle IDN domains and remove problematic characters
     */
    private fun preprocessUrl(text: String): String {
        // Remove zero-width characters
        val cleaned = text.replace(Regex("[\\u200B\\u200C\\u200D\\uFEFF\\u2060]"), "")
        
        // Convert international domains to ASCII
        return try {
            val parts = cleaned.split("://", limit = 2)
            if (parts.size == 2) {
                val domainEnd = parts[1].indexOfAny(charArrayOf('/', '?', '#', ':'))
                val domain = if (domainEnd > 0) parts[1].substring(0, domainEnd) else parts[1]
                val asciiDomain = IDN.toASCII(domain)
                parts[0] + "://" + parts[1].replaceFirst(domain, asciiDomain)
            } else {
                cleaned
            }
        } catch (e: Exception) {
            Timber.e(e, "Error converting IDN domain")
            cleaned
        }
    }
    
    /**
     * Apply domain conversions based on settings
     */
    private fun applyDomainConversions(
        url: String,
        convertToAlternative: Boolean,
        instagramProxy: String = Constants.INSTAGRAM_DEFAULT_PROXY
    ): String {
        val isInstagram = isInstagramUrl(url)
        val isInstagramProxy = Constants.INSTAGRAM_ALL_KNOWN_PROXIES.any {
            url.contains(it, ignoreCase = true)
        }

        return when {
            // Instagram conversions — covers instagram.com + any of the 3 proxies
            isInstagram && convertToAlternative -> convertToInstagramProxy(url, instagramProxy)
            isInstagramProxy && !convertToAlternative -> convertFromInstagramProxy(url)
            
            // Twitter/X conversions
            // 1) fxtwitter domain rewrite needs to happen BEFORE generic Twitter conversion
            url.contains(Constants.FXTWITTER_DOMAIN, ignoreCase = true) ->
                if (convertToAlternative) {
                    url.replace(Constants.FXTWITTER_DOMAIN, Constants.FIXUPX_DOMAIN, ignoreCase = true)
                } else {
                    convertFromFxTwitter(url)
                }
            // 2) Standard Twitter/X → FixupX conversion for status URLs
            isTwitterUrl(url) && convertToAlternative -> convertTwitterUrl(url)
            url.contains(Constants.FIXUPX_DOMAIN, ignoreCase = true) && !convertToAlternative ->
                convertFromFixupx(url)
            
            // Facebook conversions
            isFacebookUrl(url) && convertToAlternative -> convertToFacebookez(url)
            url.contains(Constants.FACEBOOKEZ_DOMAIN, ignoreCase = true) && !convertToAlternative ->
                convertFromFacebookez(url)
            
            else -> url
        }
    }
    
    /**
     * Process URL for sharing - this will convert to alternative
     * domain formats like fixupx.com and the selected Instagram proxy
     */
    fun processUrlForSharing(
        url: String,
        instagramProxy: String = Constants.INSTAGRAM_DEFAULT_PROXY
    ): String {
        if (url.isEmpty()) {
            return url
        }
        
        return try {
            val decodedUrl = try {
                URLDecoder.decode(url.trim(), "UTF-8")
            } catch (e: Exception) {
                url.trim()
            }
            
            // Use cleaner service for better cleaning
            val cleanedUrl = cleanerService.deepClean(decodedUrl)
            
            // Apply domain conversions for sharing (always convert to alternative domains)
            applyDomainConversions(
                cleanedUrl,
                convertToAlternative = true,
                instagramProxy = instagramProxy
            )
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
        if (!VALID_URL_PATTERN.matches(url)) return false
        
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
     * Check if a URL is an Instagram URL (instagram.com or any of the supported / legacy proxies).
     * Legacy proxies are detected so existing pasted links still trigger the conversion flow.
     */
    fun isInstagramUrl(url: String): Boolean {
        if (url.contains(Constants.INSTAGRAM_DOMAIN, ignoreCase = true)) return true
        return Constants.INSTAGRAM_ALL_KNOWN_PROXIES.any { url.contains(it, ignoreCase = true) }
    }

    /**
     * Convert instagram.com / any proxy in [url] to the [targetProxy] domain.
     * - If [url] already uses [targetProxy] AND has no `www.`/sub prefix, it is returned unchanged.
     * - Any `www.` prefix or sub-prefix on the host is stripped (active proxies prefer bare hostnames).
     * - Legacy proxy hostnames (kkinstagram.com, eeinstagram.com) are also rewritten.
     */
    private fun convertToInstagramProxy(url: String, targetProxy: String): String {
        val knownAlternation = (listOf(Constants.INSTAGRAM_DOMAIN) + Constants.INSTAGRAM_ALL_KNOWN_PROXIES)
            .joinToString("|") { it.replace(".", "\\.") }
        val pattern = Pattern.compile(
            "(https?://)(?:www\\.)?(?:[a-z0-9]+\\.)?($knownAlternation)",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(url)
        return if (matcher.find()) {
            // Replace whole prefix+hostname with `https://${targetProxy}` — drops `www.` and any sub.
            matcher.replaceAll("\$1${targetProxy}")
        } else {
            var result = url
            for (domain in listOf(Constants.INSTAGRAM_DOMAIN) + Constants.INSTAGRAM_ALL_KNOWN_PROXIES) {
                if (result.contains(domain, ignoreCase = true)) {
                    result = result.replace(domain, targetProxy, ignoreCase = true)
                    break
                }
            }
            result
        }
    }

    /**
     * Convert any Instagram proxy in [url] (current or legacy) back to instagram.com.
     */
    private fun convertFromInstagramProxy(url: String): String {
        // Already on instagram.com and no proxy is present
        if (url.contains(Constants.INSTAGRAM_DOMAIN, ignoreCase = true) &&
            Constants.INSTAGRAM_ALL_KNOWN_PROXIES.none { url.contains(it, ignoreCase = true) }
        ) {
            return url
        }

        val proxyAlternation = Constants.INSTAGRAM_ALL_KNOWN_PROXIES.joinToString("|") {
            it.replace(".", "\\.")
        }
        val pattern = Pattern.compile(
            "(https?://)((?:www\\.)?(?:[a-z0-9]+\\.)?)($proxyAlternation)",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(url)
        return if (matcher.find()) {
            matcher.replaceAll("\$1\$2${Constants.INSTAGRAM_DOMAIN}")
        } else {
            var result = url
            for (proxy in Constants.INSTAGRAM_ALL_KNOWN_PROXIES) {
                if (result.contains(proxy, ignoreCase = true)) {
                    result = result.replace(proxy, Constants.INSTAGRAM_DOMAIN, ignoreCase = true)
                    break
                }
            }
            result
        }
    }
    
    /**
     * Check if a URL is a Twitter/X URL
     */
    fun isTwitterUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains(Constants.TWITTER_DOMAIN) ||
               lowerUrl.contains(Constants.X_DOMAIN) ||
               lowerUrl.contains(Constants.FXTWITTER_DOMAIN) ||
               lowerUrl.contains(Constants.FIXUPX_DOMAIN)
    }
    
    /**
     * Check if a URL is a Facebook URL
     */
    fun isFacebookUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains(Constants.FACEBOOK_DOMAIN) || 
               lowerUrl.contains(Constants.FACEBOOKEZ_DOMAIN)
    }
    
    /**
     * Convert Twitter URLs to fixupx.com format
     */
    private fun convertTwitterUrl(url: String): String {
        return try {
            // If it's not a Twitter/X URL, return unchanged
            if (!isTwitterUrl(url)) return url

            // Extract fragment if present
            val fragmentIndex = url.indexOf('#')
            val urlWithoutFragment = if (fragmentIndex > -1) url.substring(0, fragmentIndex) else url
            val fragment = if (fragmentIndex > -1) url.substring(fragmentIndex) else ""

            // Use regex to robustly extract username and status id
            val regex = Regex("https?://(?:www\\.)?(?:${Constants.TWITTER_DOMAIN}|${Constants.X_DOMAIN})/([A-Za-z0-9_]+)/status/(\\d+)", RegexOption.IGNORE_CASE)
            val match = regex.find(urlWithoutFragment)
            if (match != null) {
                val username = match.groupValues[1]
                val statusId = match.groupValues[2]
                "https://${Constants.FIXUPX_DOMAIN}/$username${Constants.TWITTER_STATUS_PATH}$statusId$fragment"
            } else {
                url // Not a standard status URL
            }
        } catch (e: Exception) {
            Timber.e(e, "Error converting Twitter URL")
            url
        }
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
     * Convert Facebook URLs to facebookez.com
     */
    private fun convertToFacebookez(url: String): String {
        if (url.contains(Constants.FACEBOOKEZ_DOMAIN, ignoreCase = true)) return url
        
        // Pattern to match Facebook URLs with any prefix (m., web., www., etc.)
        // This regex captures the protocol and removes any subdomain prefixes
        val regex = Regex("(https?://)(?:www\\.)?(?:[a-z]+\\.)?facebook\\.com", RegexOption.IGNORE_CASE)
        
        return if (regex.containsMatchIn(url)) {
            // Remove all prefixes and convert to facebookez.com
            url.replace(regex, "$1${Constants.FACEBOOKEZ_DOMAIN}")
        } else {
            // Fallback for edge cases
            url.replace(Constants.FACEBOOK_DOMAIN, Constants.FACEBOOKEZ_DOMAIN, ignoreCase = true)
        }
    }

    private fun convertFromFacebookez(url: String): String {
        if (url.contains(Constants.FACEBOOK_DOMAIN, ignoreCase = true) && !url.contains(Constants.FACEBOOKEZ_DOMAIN, ignoreCase = true)) return url
        
        // Pattern to match facebookez URLs with any prefix (though they shouldn't have any after conversion)
        val regex = Regex("(https?://)(?:www\\.)?(?:[a-z]+\\.)?facebookez\\.com", RegexOption.IGNORE_CASE)
        
        return if (regex.containsMatchIn(url)) {
            // Convert back to plain facebook.com without prefixes
            url.replace(regex, "$1${Constants.FACEBOOK_DOMAIN}")
        } else {
            url.replace(Constants.FACEBOOKEZ_DOMAIN, Constants.FACEBOOK_DOMAIN, ignoreCase = true)
        }
    }
    
    /**
     * Check if a URL contains any tracking parameters
     */
    fun hasTrackingParameters(url: String): Boolean {
        // Use the new cleaner service to check if URL would be modified
        return cleanerService.wouldModifyUrl(url)
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
                val simpleValid = isValidUrlSimple(trimmedText)
                if (simpleValid) return trimmedText
                val uri = parseValidHttpUri(trimmedText)
                return if (uri != null) trimmedText else null
            }
            
            // If it looks like a domain without protocol
            // Must contain at least one dot and have valid domain structure
            if (!trimmedText.contains(" ") && trimmedText.contains(".")) {
                // Basic domain validation - must have at least 2 parts separated by dot
                val parts = trimmedText.split(".")
                if (parts.size >= 2 && parts.all { it.isNotEmpty() }) {
                    // Check if it matches a basic domain pattern
                    val domainPattern = Regex(
                        "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$"
                    )
                    if (domainPattern.matches(trimmedText)) {
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
            val matcher = URL_PATTERN.find(searchText)
            if (matcher != null) {
                val foundUrl = matcher.value
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
            
            // Use new cleaner service for superior cleaning
            Timber.d("Using new cleaner service for cleanUrl: $decodedUrl")
            cleanerService.deepClean(decodedUrl)
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning URL")
            url
        }
    }
    
    companion object {
        // Maximum input size to prevent performance issues
        private const val MAX_INPUT_LENGTH = 10000
        
        // Regex that captures each http/https URL up to the next whitespace OR the next http/https occurrence.
        // This prevents blobs like "https://a.comfoohttps://b.com" from being treated as one URL.
        private val URL_PATTERN = Regex(
            "(https?://[^\\s]+?)(?=https?://|\\s|$)",
            RegexOption.IGNORE_CASE
        )
        
        // More comprehensive URL validation pattern
        private val VALID_URL_PATTERN = Regex(
            "^(https?://)?" + // Optional protocol
            "((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|" + // Domain name
            "((\\d{1,3}\\.){3}\\d{1,3}))" + // OR IP address
            "(:\\d+)?(/[-a-z\\d%_.~+]*)*" + // Port and path
            "(\\?[;&a-z\\d%_.~+=-]*)?" + // Query string
            "(#[-a-z\\d_]*)?$", // Fragment
            RegexOption.IGNORE_CASE
        )
        
        private fun parseValidHttpUri(url: String): URI? {
            return try {
                val uri = URI(url)
                if ((uri.scheme == "http" || uri.scheme == "https") &&
                    !uri.host.isNullOrBlank() &&
                    uri.host.contains(".")
                ) {
                    uri
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
        
        /**
         * Simple URL validation that works in both production and test environments (static version)
         */
        private fun isValidUrlSimple(url: String): Boolean {
            if (url.isEmpty()) return false
            
            // Basic pattern check
            if (!VALID_URL_PATTERN.matches(url)) return false
            
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
            
            val urls = mutableListOf<String>()
            
            // 1) Extract protocol-based URLs with the improved pattern
            val protoMatcher = URL_PATTERN.findAll(limitedText)
            for (match in protoMatcher) {
                urls.add(match.value)
            }
            
            // 2) Scan for "www." domains that were not preceded by a protocol.
            //    We cut at the next occurrence of "www." / whitespace.
            val wwwPattern = Regex("(www\\.[a-zA-Z0-9\\-_.]+?\\.[a-zA-Z]{2,}[^\\s]*?)(?=www\\.|https?://|\\s|$)")
            val wwwMatcher = wwwPattern.findAll(limitedText)
            for (match in wwwMatcher) {
                val candidate = "https://" + match.value.trimEnd('.', ',')
                // De-duplicate
                if (!urls.contains(candidate)) {
                    urls.add(candidate)
                }
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
                    if (isValidUrlSimple(trimmedText)) return trimmedText
                    return if (parseValidHttpUri(trimmedText) != null) trimmedText else null
                }
                
                // If it looks like a domain without protocol
                // Must contain at least one dot and have valid domain structure
                if (!trimmedText.contains(" ") && trimmedText.contains(".")) {
                    // Basic domain validation - must have at least 2 parts separated by dot
                    val parts = trimmedText.split(".")
                    if (parts.size >= 2 && parts.all { it.isNotEmpty() }) {
                        // Check if it matches a basic domain pattern
                        val domainPattern = Regex(
                            "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$"
                        )
                        if (domainPattern.matches(trimmedText)) {
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
                val matcher = URL_PATTERN.find(searchText)
                if (matcher != null) {
                    val foundUrl = matcher.value
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