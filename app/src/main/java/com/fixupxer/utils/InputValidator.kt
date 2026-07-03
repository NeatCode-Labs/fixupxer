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


package com.fixupxer.utils

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import timber.log.Timber

object InputValidator {
    
    private const val MAX_INPUT_LENGTH = 2048
    
    // Google redirect wrapper (Gmail links, Google Search results):
    // https://www.google.com/url?q=<destination>&source=gmail&...
    // The destination URL lives in the query string, so a second "https://"
    // in the SAME string is expected there — it is NOT a multi-URL paste.
    // Full-string match ("\S+$") so anything with whitespace (a real multi-URL
    // paste) falls through to the normal multiple-URL rejection.
    private val GOOGLE_REDIRECT_WRAPPER = Regex(
        "^https?://(www\\.)?google(\\.[a-z]{2,3}){1,2}/url\\?\\S+$",
        RegexOption.IGNORE_CASE
    )
    
    /**
     * Comprehensive input validation and sanitization
     * @param input Raw input string
     * @return Sanitized and validated input, or null if invalid
     */
    suspend fun validateAndSanitizeInput(input: String): String? {
        return try {
            withTimeout(100) { // 100ms timeout to prevent DoS
                // Check length first
                if (!validateInputLength(input)) {
                    Timber.w("Input too long: ${input.length} characters")
                    return@withTimeout null
                }
                
                // Sanitize and decode
                val sanitized = sanitizeInput(input)
                val decoded = decodeUrlSafely(sanitized)
                
                // Google redirect wrappers legitimately contain a nested URL,
                // so the multiple-URL check must not apply to them (regression
                // fix: Gmail links in browser mode). Checked on both the raw and
                // the decoded form — the destination may or may not be %-encoded.
                // Security note: ONLY the multiple-URL check is skipped (control
                // chars, combining accents, %2E and length checks still apply),
                // and downstream GoogleSearchCleaner deterministically extracts a
                // single URL from the url=/q= parameter — extra URLs smuggled
                // into the wrapper are never extracted (see UrlProcessorTest).
                val isGoogleRedirect = GOOGLE_REDIRECT_WRAPPER.matches(sanitized) ||
                        GOOGLE_REDIRECT_WRAPPER.matches(decoded)
                
                // Check for multiple URLs
                if (!isGoogleRedirect && hasMultipleUrls(decoded)) {
                    Timber.w("Multiple URLs detected in input")
                    return@withTimeout null
                }
                if (isGoogleRedirect) {
                    Timber.d("Google redirect wrapper detected — nested URL allowed")
                }
                
                // Additional safety checks
                if (decoded.contains(Regex("[\\u0000-\\u001F]"))) { // Control characters
                    Timber.w("Control characters detected in input")
                    return@withTimeout null
                }
                // Reject Unicode normalization attacks (combining accents)
                if (decoded.contains(Regex("\\p{M}"))) {
                    Timber.w("Unicode normalization (combining accent) detected in input")
                    return@withTimeout null
                }
                // Reject encoded dot attacks (e.g., %2E)
                if (sanitized.contains("%2E", ignoreCase = true)) {
                    Timber.w("Encoded dot attack detected in input")
                    return@withTimeout null
                }
                
                decoded
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w("Input validation timed out")
            null
        } catch (e: Exception) {
            Timber.e(e, "Error during input validation")
            null
        }
    }
    
    /**
     * Sanitize input by removing problematic characters and normalizing
     */
    private fun sanitizeInput(input: String): String {
        return input
            .trim() // Remove leading/trailing whitespace
            .replace(Regex("\\s+"), " ") // Normalize multiple spaces to single
            .replace(Regex("[\\u200B\\uFEFF\\u2060\\u200C\\u200D]"), "") // Remove zero-width characters
            .replace(Regex("[\\u0000-\\u001F\\u007F-\\u009F]"), "") // Remove control characters
    }
    
    /**
     * Safely decode URL-encoded strings
     */
    private fun decodeUrlSafely(input: String): String {
        return try {
            URLDecoder.decode(input, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            Timber.w("URL decoding failed, using original input")
            input // Return original if decoding fails
        }
    }
    
    /**
     * Check if input length is within acceptable limits
     */
    private fun validateInputLength(input: String): Boolean {
        return input.length <= MAX_INPUT_LENGTH
    }
    
    /**
     * Enhanced multiple URL detection with timeout protection
     */
    private suspend fun hasMultipleUrls(input: String): Boolean {
        return try {
            withTimeout(50) { // 50ms timeout for URL detection
                val protocolCount = Regex("https?://|ftp://|file://|mailto:", RegexOption.IGNORE_CASE)
                    .findAll(input).count()
                val wwwCount = Regex("www\\.", RegexOption.IGNORE_CASE)
                    .findAll(input).count()
                
                // More intelligent domain detection - look for domain patterns
                val domainPattern = Regex("([a-z0-9]([a-z0-9\\-]*[a-z0-9])?\\.)+[a-z]{2,}", RegexOption.IGNORE_CASE)
                val mainUrl = input.split("?", "#")[0]
                
                // Extract only the domain part (protocol + domain, without path)
                val domainPart = try {
                    val protocolEnd = mainUrl.indexOf("://")
                    if (protocolEnd > 0) {
                        val afterProtocol = mainUrl.substring(protocolEnd + 3)
                        val pathStart = afterProtocol.indexOfAny(charArrayOf('/', '?', '#'))
                        if (pathStart > 0) {
                            mainUrl.substring(0, protocolEnd + 3 + pathStart)
                        } else {
                            mainUrl
                        }
                    } else {
                        mainUrl
                    }
                } catch (e: Exception) {
                    mainUrl
                }
                
                val domainsMatches = domainPattern.findAll(domainPart).toList()
                val distinctDomains = domainsMatches.map { it.value.lowercase() }.distinct()
                
                // Count dots only in the actual domain part (not in the path)
                // Extract just the domain from the URL
                val domainOnly = try {
                    val urlWithoutProtocol = mainUrl.removePrefix("https://").removePrefix("http://")
                    val domainEnd = urlWithoutProtocol.indexOfAny(charArrayOf('/', ':', '?', '#'))
                    if (domainEnd > 0) urlWithoutProtocol.substring(0, domainEnd) else urlWithoutProtocol
                } catch (e: Exception) {
                    mainUrl
                }
                val domainDots = domainOnly.count { it == '.' }
                
                // Refined glue detection: look for patterns like
                // "google.cominstagram.com" → ".cominstagram.com"
                // We require:
                //   1. A known TLD (e.g., .com)
                //   2. Immediately followed by 1+ alnum/hyphen chars
                //   3. Followed by a dot and another TLD (second domain)
                // This avoids flagging regular domains like "x.com" or
                // "instagram.com" which end after the first TLD.
                val tldGluePattern = Regex("\\.(com|net|org|gov|edu|co|io|info)([a-z0-9-]+)\\.(com|net|org|gov|edu|co|io|info)", RegexOption.IGNORE_CASE)
                val hasTldGlue = tldGluePattern.containsMatchIn(mainUrl)
                
                // Enhanced glued URL detection
                val hasGluedUrls = detectGluedUrls(input)
                
                // Debug logging
                Timber.d("InputValidator: input='$input'")
                Timber.d("InputValidator: protocolCount=$protocolCount, wwwCount=$wwwCount")
                Timber.d("InputValidator: domains.size=${domainsMatches.size}, distinct=${distinctDomains.size}, domainDots=$domainDots")
                Timber.d("InputValidator: mainUrl='$mainUrl'")
                Timber.d("InputValidator: domainPart='$domainPart'")
                Timber.d("InputValidator: domainOnly='$domainOnly'")
                Timber.d("InputValidator: hasTldGlue=$hasTldGlue")
                Timber.d("InputValidator: hasGluedUrls=$hasGluedUrls")
                
                // Flag as multiple if any of these conditions are met.
                // To reduce false-positives (e.g. long but single URLs like
                // https://www.theblock.co/...), only treat a "glued" pattern
                // as multiple when we have already detected more than one
                // potential domain. This prevents a single, legitimate domain
                // from being rejected.
                val result = protocolCount > 1 ||
                        wwwCount > 1 ||
                        domainsMatches.size > 1 ||
                        domainDots > 5 ||
                        (hasGluedUrls && distinctDomains.size > 1) ||
                        hasTldGlue
                Timber.d("InputValidator: hasMultipleUrls result=$result")
                result
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w("URL detection timed out, assuming multiple URLs")
            true // Assume multiple URLs if timeout occurs
        } catch (e: Exception) {
            Timber.w("URL detection failed, assuming safe")
            false // Safe fallback
        }
    }
    
    /**
     * Detect glued URLs where two domains are concatenated without proper separation
     */
    private fun detectGluedUrls(input: String): Boolean {
        val lower = input.lowercase()
        
        // Common TLDs - comprehensive list
        val commonTlds = listOf(
            "com", "org", "net", "edu", "gov", "mil", "int", "io", "co", "uk", "de", "fr", "jp", "cn",
            "ru", "br", "au", "ca", "in", "it", "nl", "es", "se", "no", "dk", "fi", "pl", "ch", "at",
            "be", "pt", "gr", "cz", "hu", "ro", "bg", "hr", "si", "sk", "lt", "lv", "ee", "lu", "mt",
            "cy", "ie", "is", "li", "mc", "sm", "va", "ad", "al", "am", "az", "ba", "by", "ge", "kg",
            "kz", "md", "me", "mk", "rs", "tj", "tm", "ua", "uz", "tv", "ws", "info", "biz", "name",
            "pro", "aero", "coop", "museum", "mobi", "travel", "xxx", "asia", "cat", "jobs", "tel",
            "post", "geo", "nato", "mil", "gov", "edu", "ac", "ad", "ae", "af", "ag", "ai", "al", "am",
            "ao", "aq", "ar", "as", "at", "au", "aw", "ax", "az", "ba", "bb", "bd", "be", "bf", "bg",
            "bh", "bi", "bj", "bm", "bn", "bo", "br", "bs", "bt", "bw", "by", "bz", "ca", "cc", "cd",
            "cf", "cg", "ch", "ci", "ck", "cl", "cm", "cn", "co", "cr", "cu", "cv", "cw", "cx", "cy",
            "cz", "de", "dj", "dk", "dm", "do", "dz", "ec", "ee", "eg", "er", "es", "et", "eu", "fi",
            "fj", "fk", "fm", "fo", "fr", "ga", "gb", "gd", "ge", "gf", "gg", "gh", "gi", "gl", "gm",
            "gn", "gp", "gq", "gr", "gs", "gt", "gu", "gw", "gy", "hk", "hm", "hn", "hr", "ht", "hu",
            "id", "ie", "il", "im", "in", "io", "iq", "ir", "is", "it", "je", "jm", "jo", "jp", "ke",
            "kg", "kh", "ki", "km", "kn", "kp", "kr", "kw", "ky", "kz", "la", "lb", "lc", "li", "lk",
            "lr", "ls", "lt", "lu", "lv", "ly", "ma", "mc", "md", "me", "mg", "mh", "mk", "ml", "mm",
            "mn", "mo", "mp", "mq", "mr", "ms", "mt", "mu", "mv", "mw", "mx", "my", "mz", "na", "nc",
            "ne", "nf", "ng", "ni", "nl", "no", "np", "nr", "nu", "nz", "om", "pa", "pe", "pf", "pg",
            "ph", "pk", "pl", "pm", "pn", "pr", "ps", "pt", "pw", "py", "qa", "re", "ro", "rs", "ru",
            "rw", "sa", "sb", "sc", "sd", "se", "sg", "sh", "si", "sk", "sl", "sm", "sn", "so", "sr",
            "ss", "st", "su", "sv", "sx", "sy", "sz", "tc", "td", "tf", "tg", "th", "tj", "tk", "tl",
            "tm", "tn", "to", "tr", "tt", "tv", "tw", "tz", "ua", "ug", "uk", "us", "uy", "uz", "va",
            "vc", "ve", "vg", "vi", "vn", "vu", "wf", "ws", "ye", "yt", "za", "zm", "zw"
        )
        
        // Look for the pattern: complete_domain.tld + another_domain.tld
        // The key is to ensure we're matching complete domains, not partial ones
        
        // First, let's find all valid domain boundaries in the input
        // A domain boundary is: start of string, space, /, :, or other non-domain character
        val domainBoundaryPattern = Regex("(?:^|[^a-z0-9.-])([a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)*\\.(?:" + commonTlds.joinToString("|") + "))(?=[a-z0-9])", RegexOption.IGNORE_CASE)
        
        domainBoundaryPattern.findAll(" $lower ").forEach { match ->
            val domain = match.groups[1]?.value ?: ""
            val afterDomainPos = match.range.last
            
            if (domain.isNotEmpty() && afterDomainPos < lower.length + 1) {
                // Check what comes after this domain
                val remaining = lower.substring(afterDomainPos - 1) // Adjust for the prepended space
                
                // If the next character is a letter/number and forms another domain, it's glued
                if (remaining.isNotEmpty() && remaining[0].isLetterOrDigit()) {
                    // Check if what follows is another domain
                    val nextDomainPattern = Regex("^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.(?:" + commonTlds.joinToString("|") + ")(?:[^a-z0-9.-]|$)", RegexOption.IGNORE_CASE)
                    if (nextDomainPattern.containsMatchIn(remaining)) {
                        Timber.d("InputValidator: Detected glued domains: '$domain' followed immediately by another domain in '$input'")
                        return true
                    }
                }
            }
        }
        
        // Additional check: Look for pattern like "domain.tld[letter]" where letter starts another domain
        // But exclude cases where it's a subdomain (e.g., www.instagram.com should not match www.in + stagram.com)
        val tldBoundaryPattern = Regex("([a-z0-9](?:[a-z0-9-]*[a-z0-9])?)\\.(${commonTlds.joinToString("|")})([a-z0-9])", RegexOption.IGNORE_CASE)
        
        tldBoundaryPattern.findAll(lower).forEach { match ->
            val domainPart = match.groups[1]?.value ?: ""
            val tld = match.groups[2]?.value ?: ""
            val charAfterTld = match.groups[3]?.value ?: ""
            
            if (domainPart.isNotEmpty() && tld.isNotEmpty() && charAfterTld.isNotEmpty()) {
                // Check if this is a legitimate subdomain or a glued URL
                // If the domain part is very short (like "www"), it's likely a subdomain
                if (domainPart.length <= 3 && (domainPart == "www" || domainPart == "ftp" || domainPart == "api" || domainPart == "cdn")) {
                    // This is likely a subdomain, not a glued URL
                    return@forEach
                }
                
                // Check what comes after the TLD
                val position = match.range.last
                val afterMatch = lower.substring(position - charAfterTld.length)
                
                // If what follows forms a complete domain, it's glued
                val completeDomainPattern = Regex("^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.(?:" + commonTlds.joinToString("|") + ")(?:[^a-z0-9.-]|$)", RegexOption.IGNORE_CASE)
                if (completeDomainPattern.containsMatchIn(afterMatch)) {
                    Timber.d("InputValidator: Detected glued pattern: domain '$domainPart.$tld' followed by another domain")
                    return true
                }
            }
        }
        
        return false
    }
} 