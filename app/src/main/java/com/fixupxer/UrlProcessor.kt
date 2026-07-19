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
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.processing.PlatformDomainConverter
import com.fixupxer.processing.ProxySelections
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyPlatform
import timber.log.Timber
import java.net.IDN
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A utility class for processing URLs to:
 * 1. Remove tracking parameters (like ClearURLs)
 * 2. Convert social-media URLs to user-selected privacy frontends
 */
@Singleton
class UrlProcessor @Inject constructor(
    private val cleanerService: CleanerService
) {

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
        val validUrl = findFirstValidUrl(trimmedUrl) ?: throw IllegalArgumentException("Invalid URL format")

        return try {
            val preprocessedUrl = preprocessUrl(validUrl)
            var inputUrl = preprocessedUrl
            if (inputUrl.startsWith("@")) {
                inputUrl = inputUrl.substring(1)
                Timber.d("Removed leading @ from URL (length=${inputUrl.length})")
            }

            val decodedUrl = inputUrl
            var finalUrl = decodedUrl
            var wasAlreadyClean = true

            if (cleanTracking) {
                Timber.d(
                    "Using cleaner service (host=${UrlNormalizer.extractAsciiHost(decodedUrl) ?: "unknown"}, " +
                        "length=${decodedUrl.length})"
                )
                wasAlreadyClean = !cleanerService.wouldModifyUrl(decodedUrl)
                finalUrl = cleanerService.deepClean(decodedUrl)
            }

            val convertedUrl = applyDomainConversions(
                finalUrl,
                convertToAlternative = convertTwitter,
                instagramProxy = instagramProxy,
                tiktokProxy = tiktokProxy,
            )

            val wasModified = convertedUrl != decodedUrl
            Pair(convertedUrl, wasAlreadyClean && !wasModified)
        } catch (e: Exception) {
            Timber.e(e, "Error processing URL")
            throw IllegalArgumentException("Error processing URL: ${e.message}")
        }
    }

    private fun preprocessUrl(text: String): String {
        val cleaned = text.replace(Regex("[\\u200B\\u200C\\u200D\\uFEFF\\u2060]"), "")
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

    internal fun applyDomainConversions(
        url: String,
        convertToAlternative: Boolean,
        instagramProxy: String = Constants.INSTAGRAM_DEFAULT_PROXY,
        tiktokProxy: String = Constants.TIKTOK_DEFAULT_PROXY,
    ): String = applyDomainConversions(
        url,
        convertToAlternative,
        legacyProxySelections(instagramProxy, tiktokProxy),
    )

    internal fun applyDomainConversions(
        url: String,
        convertToAlternative: Boolean,
        selections: ProxySelections,
    ): String = PlatformDomainConverter.apply(url, convertToAlternative, selections)

    fun processUrlForSharing(
        url: String,
        instagramProxy: String = Constants.INSTAGRAM_DEFAULT_PROXY,
        tiktokProxy: String = Constants.TIKTOK_DEFAULT_PROXY,
        useCache: Boolean = true
    ): String = processUrlForSharing(
        url = url,
        selections = legacyProxySelections(instagramProxy, tiktokProxy),
        useCache = useCache,
    )

    fun processUrlForSharing(
        url: String,
        selections: ProxySelections,
        useCache: Boolean = true
    ): String {
        if (url.isEmpty()) return url

        return try {
            val decodedUrl = url.trim()
            val cleanedUrl = if (useCache) {
                cleanerService.deepClean(decodedUrl)
            } else {
                cleanerService.deepCleanWithoutCache(decodedUrl)
            }
            applyDomainConversions(
                cleanedUrl,
                convertToAlternative = true,
                selections = selections,
            )
        } catch (e: Exception) {
            Timber.e(e, "Error processing URL for sharing")
            url
        }
    }

    fun isValidUrl(url: String): Boolean {
        return try {
            if (isValidUrlSimple(url)) return true
            val uri = Uri.parse(url)
            uri.scheme != null && (uri.scheme == "http" || uri.scheme == "https")
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidUrlSimple(url: String): Boolean = Companion.isValidUrlSimple(url)

    fun isInstagramUrl(url: String): Boolean = PlatformDomainConverter.isInstagramUrl(url)

    fun isTwitterUrl(url: String): Boolean = PlatformDomainConverter.isTwitterUrl(url)

    fun isBlueskyUrl(url: String): Boolean = PlatformDomainConverter.isBlueskyUrl(url)

    fun isFacebookUrl(url: String): Boolean = PlatformDomainConverter.isFacebookUrl(url)

    fun isTikTokUrl(url: String): Boolean = PlatformDomainConverter.isTikTokUrl(url)

    fun isRedditUrl(url: String): Boolean = PlatformDomainConverter.isRedditUrl(url)

    fun isYouTubeUrl(url: String): Boolean = PlatformDomainConverter.isYouTubeUrl(url)

    fun isPinterestUrl(url: String): Boolean = PlatformDomainConverter.isPinterestUrl(url)

    fun isThreadsUrl(url: String): Boolean = PlatformDomainConverter.isThreadsUrl(url)

    fun hasTrackingParameters(url: String): Boolean = cleanerService.wouldModifyUrl(url)

    fun findFirstValidUrl(text: String): String? = Companion.findFirstValidUrl(text)

    fun cleanUrl(url: String): String {
        if (url.isEmpty()) return url
        return try {
            Timber.d(
                "Using cleaner service for cleanUrl " +
                    "(host=${UrlNormalizer.extractAsciiHost(url.trim()) ?: "unknown"}, " +
                    "length=${url.length})"
            )
            cleanerService.deepClean(url.trim())
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning URL")
            url
        }
    }

    private fun legacyProxySelections(instagramProxy: String, tiktokProxy: String): ProxySelections {
        val map = ProxySelections.DEFAULT.byPlatform.toMutableMap()
        map[ProxyPlatform.INSTAGRAM] = instagramProxy.takeIf { it.isNotBlank() }
        map[ProxyPlatform.TIKTOK] = tiktokProxy.takeIf { it.isNotBlank() }
        return ProxySelections(map)
    }

    companion object {
        private val URL_PATTERN = Regex(
            "(https?://[^\\s]+?)(?=https?://|\\s|$)",
            RegexOption.IGNORE_CASE
        )

        private val VALID_URL_PATTERN = Regex(
            "^(https?://)?" +
                "((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|" +
                "((\\d{1,3}\\.){3}\\d{1,3}))" +
                "(:\\d+)?(/[-a-z\\d%_.~+]*)*" +
                "(\\?[;&a-z\\d%_.~+=-]*)?" +
                "(#[-a-z\\d_]*)?$",
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

        private fun isValidUrlSimple(url: String): Boolean {
            if (url.isEmpty()) return false
            if (!VALID_URL_PATTERN.matches(url)) return false
            if (!url.startsWith("http://") && !url.startsWith("https://")) return false
            val domainStart = url.indexOf("://") + 3
            val domainEnd = url.indexOf("/", domainStart).let { if (it == -1) url.length else it }
            val domain = url.substring(domainStart, domainEnd)
            val domainWithoutPort = domain.split(":").first()
            if (!domainWithoutPort.contains(".")) return false
            val parts = domainWithoutPort.split(".")
            if (parts.size < 2) return false
            if (parts.any { it.isEmpty() }) return false
            return true
        }

        fun extractUrls(text: String): List<String> {
            val limitedText = if (text.length > Constants.MAX_INPUT_LENGTH) {
                text.substring(0, Constants.MAX_INPUT_LENGTH)
            } else {
                text
            }
            val urls = mutableListOf<String>()
            URL_PATTERN.findAll(limitedText).forEach { urls.add(it.value) }
            val wwwPattern = Regex("(www\\.[a-zA-Z0-9\\-_.]+?\\.[a-zA-Z]{2,}[^\\s]*?)(?=www\\.|https?://|\\s|$)")
            wwwPattern.findAll(limitedText).forEach { match ->
                val candidate = "https://" + match.value.trimEnd('.', ',')
                if (!urls.contains(candidate)) urls.add(candidate)
            }
            return urls
        }

        fun findFirstValidUrl(text: String): String? {
            val limitedText = if (text.length > Constants.MAX_INPUT_LENGTH) {
                text.substring(0, Constants.MAX_INPUT_LENGTH)
            } else {
                text
            }
            if (limitedText.length < 4) return null
            val trimmedText = limitedText.trim()
            if (trimmedText.isNotEmpty() && !trimmedText.contains("\n") && !trimmedText.contains("\r")) {
                if (trimmedText.startsWith("http://") || trimmedText.startsWith("https://")) {
                    if (isValidUrlSimple(trimmedText)) return trimmedText
                    return if (parseValidHttpUri(trimmedText) != null) trimmedText else null
                }
                if (!trimmedText.contains(" ") && trimmedText.contains(".")) {
                    val parts = trimmedText.split(".")
                    if (parts.size >= 2 && parts.all { it.isNotEmpty() }) {
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
            val searchText = if (limitedText.length > Constants.MAX_URL_SCAN_LENGTH) {
                limitedText.substring(0, Constants.MAX_URL_SCAN_LENGTH)
            } else {
                limitedText
            }
            return try {
                val matcher = URL_PATTERN.find(searchText)
                if (matcher != null) {
                    val foundUrl = matcher.value
                    if (isValidUrlSimple(foundUrl)) foundUrl else null
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
