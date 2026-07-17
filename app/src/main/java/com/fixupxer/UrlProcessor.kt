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
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.TikTokProxyStore
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
 * 4. Convert TikTok URLs to a user-selected proxy for better embedding
 */
@Singleton
class UrlProcessor @Inject constructor(
    private val cleanerService: CleanerService
) {
    
    /**
     * Process a URL by cleaning tracking parameters and optionally converting 
     * Twitter/X URLs to fixupx.com format and Instagram/TikTok URLs to a selected proxy
     * @param instagramProxy which Instagram proxy to convert to (default [Constants.INSTAGRAM_DEFAULT_PROXY])
     * @param tiktokProxy which TikTok proxy to convert to (default [Constants.TIKTOK_DEFAULT_PROXY])
     * @return Pair of (processedUrl, wasAlreadyClean)
     */
    fun processUrl(
        url: String,
        cleanTracking: Boolean,
        convertTwitter: Boolean,
        instagramProxy: String = Constants.INSTAGRAM_DEFAULT_PROXY,
        tiktokProxy: String = Constants.TIKTOK_DEFAULT_PROXY
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
                Timber.d("Removed leading @ from URL (length=${inputUrl.length})")
            }
            
            // Keep the URL raw. Whole-string URLDecoder corrupts '+' and turns
            // encoded query delimiters into structural delimiters.
            val decodedUrl = inputUrl
            
            var finalUrl = decodedUrl
            var wasAlreadyClean = true
            
            // Clean tracking parameters if requested
            if (cleanTracking) {
                Timber.d(
                    "Using cleaner service (host=${UrlNormalizer.extractAsciiHost(decodedUrl) ?: "unknown"}, " +
                        "length=${decodedUrl.length})"
                )
                wasAlreadyClean = !cleanerService.wouldModifyUrl(decodedUrl)
                finalUrl = cleanerService.deepClean(decodedUrl)
            }
            
            // Apply domain conversions based on settings
            val convertedUrl = applyDomainConversions(
                finalUrl,
                convertToAlternative = convertTwitter,
                instagramProxy = instagramProxy,
                tiktokProxy = tiktokProxy
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
    internal fun applyDomainConversions(
        url: String,
        convertToAlternative: Boolean,
        instagramProxy: String = Constants.INSTAGRAM_DEFAULT_PROXY,
        tiktokProxy: String = Constants.TIKTOK_DEFAULT_PROXY
    ): String {
        val host = UrlNormalizer.extractAsciiHost(url)
        val isInstagram = isInstagramUrl(url)
        val isInstagramProxy = InstagramProxyStore.allKnownProxies().any {
            UrlNormalizer.hostMatchesDomain(host, it)
        }
        val isTikTok = isTikTokUrl(url)
        val isTikTokProxy = TikTokProxyStore.allKnownProxies().any {
            UrlNormalizer.hostMatchesDomain(host, it)
        }
        val isBluesky = isBlueskyUrl(url)

        return when {
            // Instagram conversions — covers instagram.com + any known proxy (fixed/custom/legacy)
            isInstagram && convertToAlternative -> convertToInstagramProxy(url, instagramProxy)
            isInstagramProxy && !convertToAlternative -> convertFromInstagramProxy(url)

            // Bluesky post conversions
            isBluesky && isBaseOrWwwHost(host, Constants.BLUESKY_DOMAIN) && convertToAlternative ->
                convertBlueskyHost(url, Constants.BLUESKY_DOMAIN, Constants.FXBSKY_DOMAIN)
            isBluesky && isBaseOrWwwHost(host, Constants.FXBSKY_DOMAIN) && !convertToAlternative ->
                convertBlueskyHost(url, Constants.FXBSKY_DOMAIN, Constants.BLUESKY_DOMAIN)

            // Twitter/X conversions
            // 1) fxtwitter/vxtwitter domain rewrites need to happen BEFORE generic Twitter conversion
            UrlNormalizer.hostMatchesDomain(host, Constants.FXTWITTER_DOMAIN) ->
                if (convertToAlternative) {
                    url.replace(Constants.FXTWITTER_DOMAIN, Constants.FIXUPX_DOMAIN, ignoreCase = true)
                } else {
                    convertFromFxTwitter(url)
                }
            UrlNormalizer.hostMatchesDomain(host, Constants.VXTWITTER_DOMAIN) ->
                if (convertToAlternative) {
                    url.replace(Constants.VXTWITTER_DOMAIN, Constants.FIXUPX_DOMAIN, ignoreCase = true)
                } else {
                    url.replace(Constants.VXTWITTER_DOMAIN, Constants.X_DOMAIN, ignoreCase = true)
                }
            // 2) Standard Twitter/X → FixupX conversion for status URLs
            isTwitterUrl(url) && convertToAlternative -> convertTwitterUrl(url)
            UrlNormalizer.hostMatchesDomain(host, Constants.FIXUPX_DOMAIN) && !convertToAlternative ->
                convertFromFixupx(url)
            
            // Facebook conversions
            isFacebookUrl(url) && convertToAlternative -> convertToFacebookez(url)
            UrlNormalizer.hostMatchesDomain(host, Constants.FACEBOOKEZ_DOMAIN) && !convertToAlternative ->
                convertFromFacebookez(url)
            
            // TikTok conversions — covers tiktok.com + any known proxy (fixed/custom/legacy)
            isTikTok && convertToAlternative -> convertToTikTokProxy(url, tiktokProxy)
            isTikTokProxy && !convertToAlternative -> convertFromTikTokProxy(url)
            
            else -> url
        }
    }
    
    /**
     * Process URL for sharing - this will convert to alternative
     * domain formats like fixupx.com and the selected Instagram proxy
     */
    fun processUrlForSharing(
        url: String,
        instagramProxy: String = Constants.INSTAGRAM_DEFAULT_PROXY,
        tiktokProxy: String = Constants.TIKTOK_DEFAULT_PROXY,
        useCache: Boolean = true
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
            val cleanedUrl = if (useCache) {
                cleanerService.deepClean(decodedUrl)
            } else {
                cleanerService.deepCleanWithoutCache(decodedUrl)
            }
            
            // Apply domain conversions for sharing (always convert to alternative domains)
            applyDomainConversions(
                cleanedUrl,
                convertToAlternative = true,
                instagramProxy = instagramProxy,
                tiktokProxy = tiktokProxy
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
     * Check if a URL is an Instagram URL (instagram.com or any of the supported / custom / legacy proxies).
     * Legacy proxies are detected so existing pasted links still trigger the conversion flow.
     */
    fun isInstagramUrl(url: String): Boolean {
        return UrlNormalizer.urlMatchesAnyDomain(
            url,
            listOf(Constants.INSTAGRAM_DOMAIN) + InstagramProxyStore.allKnownProxies()
        )
    }

    /**
     * Convert instagram.com / any proxy in [url] to the [targetProxy] domain.
     * - If [url] already uses [targetProxy] AND has no `www.`/sub prefix, it is returned unchanged.
     * - Any `www.` prefix or sub-prefix on the host is stripped (active proxies prefer bare hostnames).
     * - Custom and legacy proxy hostnames are also rewritten.
     */
    private fun convertToInstagramProxy(url: String, targetProxy: String): String {
        val knownProxies = InstagramProxyStore.allKnownProxies()
        val knownAlternation = (listOf(Constants.INSTAGRAM_DOMAIN) + knownProxies)
            .joinToString("|") { it.replace(".", "\\.") }
        val pattern = Pattern.compile(
            "^(https?://)(?:www\\.)?(?:[a-z0-9]+\\.)?($knownAlternation)(?=[/:?#]|$)",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(url)
        return if (matcher.find()) {
            // Replace whole prefix+hostname with `https://${targetProxy}` — drops `www.` and any sub.
            matcher.replaceAll("\$1${targetProxy}")
        } else {
            var result = url
            for (domain in listOf(Constants.INSTAGRAM_DOMAIN) + knownProxies) {
                val replaced = replaceHostDomain(result, domain, targetProxy)
                if (replaced != result) {
                    result = replaced
                    break
                }
            }
            result
        }
    }

    /**
     * Replace [domain] with [target] only when it terminates the URL's hostname —
     * never inside the path, query or fragment. Any host prefix is preserved.
     */
    private fun replaceHostDomain(url: String, domain: String, target: String): String {
        val pattern = Regex(
            "^(https?://(?:[a-z0-9-]+\\.)*)${Regex.escape(domain)}(?=[/:?#]|$)",
            RegexOption.IGNORE_CASE
        )
        return pattern.replaceFirst(url, "\$1$target")
    }

    /**
     * Convert any Instagram proxy in [url] (fixed, custom or legacy) back to instagram.com.
     */
    private fun convertFromInstagramProxy(url: String): String {
        val knownProxies = InstagramProxyStore.allKnownProxies()
        // Already on instagram.com and no proxy is present
        if (UrlNormalizer.urlMatchesDomain(url, Constants.INSTAGRAM_DOMAIN) &&
            knownProxies.none { UrlNormalizer.urlMatchesDomain(url, it) }
        ) {
            return url
        }

        val proxyAlternation = knownProxies.joinToString("|") {
            it.replace(".", "\\.")
        }
        val pattern = Pattern.compile(
            "^(https?://)((?:www\\.)?(?:[a-z0-9]+\\.)?)($proxyAlternation)(?=[/:?#]|$)",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(url)
        return if (matcher.find()) {
            matcher.replaceAll("\$1\$2${Constants.INSTAGRAM_DOMAIN}")
        } else {
            var result = url
            for (proxy in knownProxies) {
                val replaced = replaceHostDomain(result, proxy, Constants.INSTAGRAM_DOMAIN)
                if (replaced != result) {
                    result = replaced
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
        return UrlNormalizer.urlMatchesAnyDomain(
            url,
            listOf(
                Constants.TWITTER_DOMAIN,
                Constants.X_DOMAIN,
                Constants.FXTWITTER_DOMAIN,
                Constants.VXTWITTER_DOMAIN,
                Constants.FIXUPX_DOMAIN
            )
        )
    }

    /**
     * Check whether a URL is a Bluesky post that supports embed conversion.
     */
    fun isBlueskyUrl(url: String): Boolean {
        val host = UrlNormalizer.extractAsciiHost(url)
        return (isBaseOrWwwHost(host, Constants.BLUESKY_DOMAIN) ||
            isBaseOrWwwHost(host, Constants.FXBSKY_DOMAIN)) &&
            BLUESKY_POST_PATH.matches(rawPath(url))
    }

    private fun isBaseOrWwwHost(host: String?, domain: String): Boolean {
        if (host == null) return false
        return listOf(domain, "www.$domain").any { allowedHost ->
            UrlNormalizer.hostMatchesDomain(host, allowedHost) &&
                UrlNormalizer.hostMatchesDomain(allowedHost, host)
        }
    }

    private fun convertBlueskyHost(url: String, sourceDomain: String, targetDomain: String): String {
        val source = sourceDomain.replace(".", "\\.")
        val pattern = Regex("^(https?://)(?:www\\.)?$source(?=[/:?#]|$)", RegexOption.IGNORE_CASE)
        return pattern.replaceFirst(url, "\$1$targetDomain")
    }

    private fun rawPath(url: String): String {
        val authorityStart = url.indexOf("://").let { if (it >= 0) it + 3 else 0 }
        val pathStart = url.indexOfAny(charArrayOf('/', '?', '#'), authorityStart)
        if (pathStart < 0 || url[pathStart] != '/') return ""
        val pathEnd = url.indexOfAny(charArrayOf('?', '#'), pathStart)
            .let { if (it >= 0) it else url.length }
        return url.substring(pathStart, pathEnd)
    }

    /**
     * Check if a URL is a Facebook URL
     */
    fun isFacebookUrl(url: String): Boolean {
        return UrlNormalizer.urlMatchesAnyDomain(
            url,
            listOf(
                Constants.FACEBOOK_DOMAIN,
                Constants.FB_SHORT_DOMAIN,
                Constants.FACEBOOKEZ_DOMAIN
            )
        )
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

            // Use regex to robustly extract username and status id (host-anchored)
            val regex = Regex("^https?://(?:www\\.)?(?:${Constants.TWITTER_DOMAIN}|${Constants.X_DOMAIN})/([A-Za-z0-9_]+)/status/(\\d+)", RegexOption.IGNORE_CASE)
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
            // Only convert when fixupx.com is the URL's actual host
            if (!UrlNormalizer.urlMatchesDomain(url, Constants.FIXUPX_DOMAIN)) {
                return url
            }

            return replaceHostDomain(url, Constants.FIXUPX_DOMAIN, Constants.X_DOMAIN)
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
            // Only convert when fxtwitter.com is the URL's actual host
            if (!UrlNormalizer.urlMatchesDomain(url, Constants.FXTWITTER_DOMAIN)) {
                return url
            }

            return replaceHostDomain(url, Constants.FXTWITTER_DOMAIN, Constants.X_DOMAIN)
        } catch (e: Exception) {
            Timber.e(e, "Error converting from fxtwitter URL")
        }
        
        return url
    }
    
    /**
     * Convert Facebook URLs (facebook.com / fb.com, any subdomain prefix) to facebookez.com
     */
    private fun convertToFacebookez(url: String): String {
        if (UrlNormalizer.urlMatchesDomain(url, Constants.FACEBOOKEZ_DOMAIN)) return url
        
        // Pattern to match Facebook URLs with any prefix (m., web., www., etc.)
        // This regex captures the protocol and removes any subdomain prefixes
        val regex = Regex("^(https?://)(?:www\\.)?(?:[a-z]+\\.)?(?:facebook|fb)\\.com(?=[/:?#]|$)", RegexOption.IGNORE_CASE)
        
        return if (regex.containsMatchIn(url)) {
            // Remove all prefixes and convert to facebookez.com
            url.replace(regex, "$1${Constants.FACEBOOKEZ_DOMAIN}")
        } else {
            // Fallback for edge cases
            replaceHostDomain(url, Constants.FACEBOOK_DOMAIN, Constants.FACEBOOKEZ_DOMAIN)
        }
    }

    private fun convertFromFacebookez(url: String): String {
        if (UrlNormalizer.urlMatchesDomain(url, Constants.FACEBOOK_DOMAIN) &&
            !UrlNormalizer.urlMatchesDomain(url, Constants.FACEBOOKEZ_DOMAIN)
        ) {
            return url
        }
        
        // Pattern to match facebookez URLs with any prefix (though they shouldn't have any after conversion)
        val regex = Regex("^(https?://)(?:www\\.)?(?:[a-z]+\\.)?facebookez\\.com(?=[/:?#]|$)", RegexOption.IGNORE_CASE)
        
        return if (regex.containsMatchIn(url)) {
            // Convert back to plain facebook.com without prefixes
            url.replace(regex, "$1${Constants.FACEBOOK_DOMAIN}")
        } else {
            replaceHostDomain(url, Constants.FACEBOOKEZ_DOMAIN, Constants.FACEBOOK_DOMAIN)
        }
    }
    
    /**
     * Check if a URL is a TikTok URL (tiktok.com or any of the supported / custom / legacy proxies).
     * Legacy proxies are detected so existing pasted links still trigger the conversion flow.
     * Note: kktiktok.com and vxtiktok.com contain "tiktok.com", so the first check
     * already covers them — the proxy scan is for the non-substring proxies (tnktok.com, …).
     */
    fun isTikTokUrl(url: String): Boolean {
        return UrlNormalizer.urlMatchesAnyDomain(
            url,
            listOf(Constants.TIKTOK_DOMAIN) + TikTokProxyStore.allKnownProxies()
        )
    }

    /**
     * Convert tiktok.com / any proxy in [url] to the [targetProxy] domain.
     *
     * Unlike Instagram conversions, any host prefix (`www.`, `vm.`, `vt.`, `m.`, …) is
     * PRESERVED: TikTok share links live on subdomains and the proxies mirror them
     * (vm.tiktok.com → vm.tnktok.com), so stripping the prefix would break short links.
     */
    private fun convertToTikTokProxy(url: String, targetProxy: String): String {
        val knownDomains = listOf(Constants.TIKTOK_DOMAIN) + TikTokProxyStore.allKnownProxies()
        val alternation = knownDomains.joinToString("|") { it.replace(".", "\\.") }
        val pattern = Pattern.compile(
            "^(https?://)((?:[a-z0-9-]+\\.)*)($alternation)(?=[/:?#]|$)",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(url)
        return if (matcher.find()) {
            matcher.replaceAll("\$1\$2${targetProxy}")
        } else {
            var result = url
            for (domain in knownDomains) {
                val replaced = replaceHostDomain(result, domain, targetProxy)
                if (replaced != result) {
                    result = replaced
                    break
                }
            }
            result
        }
    }

    /**
     * Convert any TikTok proxy in [url] (fixed, custom or legacy) back to tiktok.com,
     * preserving any host prefix (vm.tnktok.com → vm.tiktok.com).
     */
    private fun convertFromTikTokProxy(url: String): String {
        val knownProxies = TikTokProxyStore.allKnownProxies()
        // Already on tiktok.com and no proxy is present (kktiktok.com/vxtiktok.com
        // contain "tiktok.com" as substring, hence the host-boundary check)
        if (UrlNormalizer.urlMatchesDomain(url, Constants.TIKTOK_DOMAIN) &&
            knownProxies.none { UrlNormalizer.urlMatchesDomain(url, it) }
        ) {
            return url
        }

        val proxyAlternation = knownProxies.joinToString("|") { it.replace(".", "\\.") }
        val pattern = Pattern.compile(
            "^(https?://)((?:[a-z0-9-]+\\.)*)($proxyAlternation)(?=[/:?#]|$)",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(url)
        return if (matcher.find()) {
            matcher.replaceAll("\$1\$2${Constants.TIKTOK_DOMAIN}")
        } else {
            var result = url
            for (proxy in knownProxies) {
                val replaced = replaceHostDomain(result, proxy, Constants.TIKTOK_DOMAIN)
                if (replaced != result) {
                    result = replaced
                    break
                }
            }
            result
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
        val limitedText = if (text.length > Constants.MAX_INPUT_LENGTH) {
            text.substring(0, Constants.MAX_INPUT_LENGTH)
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
        val searchText = if (limitedText.length > Constants.MAX_URL_SCAN_LENGTH) {
            limitedText.substring(0, Constants.MAX_URL_SCAN_LENGTH)
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
            val decodedUrl = url.trim()
            
            // Use new cleaner service for superior cleaning
            Timber.d(
                "Using cleaner service for cleanUrl " +
                    "(host=${UrlNormalizer.extractAsciiHost(decodedUrl) ?: "unknown"}, " +
                    "length=${decodedUrl.length})"
            )
            cleanerService.deepClean(decodedUrl)
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning URL")
            url
        }
    }
    
    companion object {
        private val BLUESKY_POST_PATH = Regex(
            "^/profile/[^/?#]+/post/[^/?#]+(?:/.*)?$"
        )

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
            val limitedText = if (text.length > Constants.MAX_INPUT_LENGTH) {
                text.substring(0, Constants.MAX_INPUT_LENGTH)
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
            val limitedText = if (text.length > Constants.MAX_INPUT_LENGTH) {
                text.substring(0, Constants.MAX_INPUT_LENGTH)
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
            val searchText = if (limitedText.length > Constants.MAX_URL_SCAN_LENGTH) {
                limitedText.substring(0, Constants.MAX_URL_SCAN_LENGTH)
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