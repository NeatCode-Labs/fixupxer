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
import java.net.URI
import java.net.URISyntaxException
import timber.log.Timber

object InputValidator {
    
    // A single, whitespace-free http(s) URL. Redirect wrappers legitimately
    // carry a nested destination URL in their query string — Gmail/Google
    // (google.com/url?q=), Reddit (out.reddit.com/…?url=), Facebook
    // (l.facebook.com/l.php?u=), LinkedIn, YouTube, newsletters, … — so a second
    // "https://" inside the query is expected and does NOT mean the user pasted
    // multiple URLs. A per-service allow-list was the root cause of Gmail-only
    // coverage; this pattern is host-agnostic. Genuine multi-URL pastes are
    // whitespace-separated (this fails on them); glued host names still live in
    // the authority+path, which the multiple-URL check keeps probing.
    private val SINGLE_URL_TOKEN = Regex("^https?://\\S+$", RegexOption.IGNORE_CASE)
    private val URL_TOKEN = Regex("(?:https?://|www\\.)\\S+", RegexOption.IGNORE_CASE)
    
    // Compile shared patterns once; validation also runs on cold, slow devices.
    private val PROTOCOL_PATTERN = Regex("https?://|ftp://|file://|mailto:", RegexOption.IGNORE_CASE)
    private val WWW_PATTERN = Regex("www\\.", RegexOption.IGNORE_CASE)
    private val DOMAIN_PATTERN = Regex("([a-z0-9]([a-z0-9\\-]*[a-z0-9])?\\.)+[a-z]{2,}", RegexOption.IGNORE_CASE)
    private val TLD_GLUE_PATTERN = Regex("\\.(com|net|org|gov|edu|co|io|info)([a-z0-9-]+)\\.(com|net|org|gov|edu|co|io|info)", RegexOption.IGNORE_CASE)
    private val CONTROL_CHARS_PATTERN = Regex("[\\u0000-\\u001F]")
    private val COMBINING_MARKS_PATTERN = Regex("\\p{M}")
    private val DOT_SEGMENT_PATTERN = Regex("(?:\\.|%2e){1,2}", RegexOption.IGNORE_CASE)
    
    /** Why [validate] rejected the input — lets the UI show an accurate message. */
    enum class InvalidReason { MULTIPLE_URLS, OTHER }

    sealed class ValidationResult {
        data class Valid(val value: String) : ValidationResult()
        data class Invalid(val reason: InvalidReason) : ValidationResult()
    }

    /**
     * Comprehensive input validation and sanitization
     * @param input Raw input string
     * @return Sanitized and validated input, or null if invalid
     */
    suspend fun validateAndSanitizeInput(input: String): String? =
        (validate(input) as? ValidationResult.Valid)?.value

    /** Returns whether [value] is a single, whitespace-free HTTP(S) URL token. */
    fun isSingleUrlToken(value: String): Boolean = SINGLE_URL_TOKEN.matches(value)

    /**
     * Same checks as [validateAndSanitizeInput], but reports WHY the input was
     * rejected so callers can distinguish a genuine multi-URL paste from other
     * failures (too long, unsafe URL components, control characters, timeout).
     */
    suspend fun validate(input: String): ValidationResult {
        return try {
            withTimeout(100) { // 100ms timeout to prevent DoS
                // Check length first
                if (!validateInputLength(input)) {
                    Timber.w("Input too long: ${input.length} characters")
                    return@withTimeout ValidationResult.Invalid(InvalidReason.OTHER)
                }
                
                // Keep URL components and percent escapes intact. Decoding before
                // finding their boundaries would turn encoded data into delimiters.
                val sanitized = sanitizeInput(input)
                
                // A single URL legitimately carries a nested destination in its
                // query/fragment (redirect wrappers: Gmail's google.com/url?q=,
                // Reddit's out.reddit.com/…?url=, Facebook's l.facebook.com/l.php?u=,
                // …). Run the multiple-URL heuristic on the authority+path only, so
                // a second "https://" in the query is not mistaken for a paste of
                // two URLs. Whitespace-separated pastes fail SINGLE_URL_TOKEN and
                // are probed whole; glued host names are still caught because the
                // probe keeps the authority+path (where such an attack lives).
                // Every other check below still applies, and downstream only ever a
                // single URL is extracted/opened, so a second URL smuggled into the
                // wrapper is never navigated (see UrlProcessorTest
                // `google url wrapper cannot smuggle extra urls`).
                val multiUrlProbe = if (SINGLE_URL_TOKEN.matches(sanitized)) {
                    sanitized.substringBefore('?').substringBefore('#')
                } else {
                    sanitized
                }
                if (hasMultipleUrls(multiUrlProbe)) {
                    Timber.w("Multiple URLs detected in input")
                    return@withTimeout ValidationResult.Invalid(InvalidReason.MULTIPLE_URLS)
                }
                
                if (hasUnsafeUrlComponents(sanitized)) {
                    Timber.w("Unsafe or malformed URL component detected in input")
                    return@withTimeout ValidationResult.Invalid(InvalidReason.OTHER)
                }
                
                ValidationResult.Valid(sanitized)
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w("Input validation timed out")
            ValidationResult.Invalid(InvalidReason.OTHER)
        } catch (e: Exception) {
            Timber.e(e, "Error during input validation")
            ValidationResult.Invalid(InvalidReason.OTHER)
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
     * Inspect each URL without decoding delimiters or normalizing the returned
     * value. Prose around a URL is allowed; downstream extraction owns it.
     */
    private fun hasUnsafeUrlComponents(input: String): Boolean {
        val tokens = URL_TOKEN.findAll(input).map { it.value }.toList()
        // Plain domains without a scheme are also accepted by UrlProcessor.
        val candidates = if (tokens.isEmpty() && input.isNotBlank() && input.none { it.isWhitespace() }) {
            listOf(input)
        } else {
            tokens
        }
        return candidates.any { token ->
            val url = if (token.startsWith("http://", true) || token.startsWith("https://", true)) {
                token
            } else {
                "https://$token"
            }
            val uri = try {
                URI(url)
            } catch (_: URISyntaxException) {
                return@any true // Includes incomplete/non-hex percent escapes.
            }
            val authority = uri.rawAuthority ?: return@any true
            val hostAndPort = authority.substringAfterLast('@')
            // Keep host spoofing checks in the authority. Encoded host characters
            // must not hide separators; ordinary Unicode/IDN labels are preserved.
            if ('%' in hostAndPort || COMBINING_MARKS_PATTERN.containsMatchIn(hostAndPort)) {
                return@any true
            }
            // Decode components only for control-character inspection, never for
            // URL boundaries, multi-URL detection or the value returned to callers.
            if (listOf(uri.authority, uri.path, uri.query, uri.fragment).any {
                    it != null && CONTROL_CHARS_PATTERN.containsMatchIn(it)
                }) {
                return@any true
            }
            // An encoded filename dot is data. Preserve the existing rejection of
            // encoded dot segments, matching only real '/' path boundaries. Query,
            // fragment and encoded slashes are not filesystem/path separators.
            uri.rawPath.orEmpty().split('/').any { segment ->
                '%' in segment && DOT_SEGMENT_PATTERN.matches(segment)
            }
        }
    }
    
    /**
     * Check if input length is within acceptable limits
     */
    private fun validateInputLength(input: String): Boolean {
        return input.length <= Constants.MAX_INPUT_LENGTH
    }
    
    /**
     * Enhanced multiple URL detection with timeout protection
     */
    private suspend fun hasMultipleUrls(input: String): Boolean {
        return try {
                withTimeout(50) { // 50ms timeout for URL detection
                val protocolCount = PROTOCOL_PATTERN.findAll(input).count()
                val wwwCount = WWW_PATTERN.findAll(input).count()
                
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
                
                val domainsMatches = DOMAIN_PATTERN.findAll(domainPart).toList()
                
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
                val hasTldGlue = TLD_GLUE_PATTERN.containsMatchIn(mainUrl)
                
                // A previous extra glue scan was gated on >1 distinct domains.
                // That condition already implies domainsMatches.size > 1 below,
                // so the expensive scan could never change the decision.
                // DebugTree resolves stack traces; keep that overhead outside
                // this short validation deadline, especially during cold starts.
                protocolCount > 1 ||
                        wwwCount > 1 ||
                        domainsMatches.size > 1 ||
                        domainDots > 5 ||
                        hasTldGlue
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w("URL detection timed out, assuming multiple URLs")
            true // Assume multiple URLs if timeout occurs
        } catch (e: Exception) {
            Timber.w("URL detection failed, assuming safe")
            false // Safe fallback
        }
    }
    
}
