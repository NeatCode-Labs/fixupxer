// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.fixupxer.processing

import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.Constants
import com.fixupxer.utils.FrontendRole
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import com.fixupxer.utils.TikTokProxyStore
import timber.log.Timber
import java.util.regex.Pattern

/**
 * Multi-platform host conversion logic driven by [ProxySelections].
 */
object PlatformDomainConverter {

    private val BLUESKY_POST_PATH = Regex("^/profile/[^/?#]+/post/[^/?#]+(?:/.*)?$")
    private val BLUESKY_PROFILE_PATH = Regex("^/profile/[^/?#]+/?$")
    private val PINTEREST_PIN_PATH = Regex("^/pin/[0-9]+(?:/.*)?$")
    private val THREADS_PATH = Regex("^/@[^/?#]+(/post/[^/?#]+)?(?:/.*)?$")
    private val YOUTUBE_ID_PATH = Regex("^/(?:shorts|live|channel)/[^/]+(?:/.*)?$")
    private val YOUTUBE_HANDLE_PATH = Regex("^/@[^/]+(?:/.*)?$")
    private val YOUTU_BE_VIDEO_PATH = Regex("^/[^/]+/?$")
    private val TWITTER_STATUS_PATH = Regex(
        "^https?://(?:www\\.)?(?:${Constants.TWITTER_DOMAIN}|${Constants.X_DOMAIN})/([A-Za-z0-9_]+)/status/(\\d+)",
        RegexOption.IGNORE_CASE,
    )

    fun apply(
        url: String,
        convertToAlternative: Boolean,
        selections: ProxySelections,
    ): String = try {
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
        val isKnownBlueskyHost = isBlueskyHost(url)
        val isKnownX = isKnownXUrl(url)
        val isFacebook = isFacebookUrl(url)
        val isReddit = isRedditUrl(url)
        val isYouTube = isYouTubeUrl(url)
        val isPinterest = isPinterestUrl(url)
        val isThreads = isThreadsUrl(url)

        when {
            isInstagram && convertToAlternative -> {
                selections.domainFor(ProxyPlatform.INSTAGRAM)?.let { target ->
                    if (target.isNotBlank()) convertToInstagramProxy(url, target) else url
                } ?: url
            }
            isInstagramProxy && !convertToAlternative -> convertFromInstagramProxy(url)

            isBluesky && convertToAlternative -> convertBlueskyForward(url, selections)
            isKnownBlueskyHost && !convertToAlternative -> convertBlueskyReverse(url)

            isKnownX && convertToAlternative -> convertXForward(url, selections.domainFor(ProxyPlatform.X))
            isKnownX && !convertToAlternative -> convertXReverse(url)

            isFacebook && convertToAlternative -> {
                selections.domainFor(ProxyPlatform.FACEBOOK)?.let { target ->
                    if (target.isNotBlank()) convertToFacebookTarget(url, target) else url
                } ?: url
            }
            isFacebookProxyHost(host) && !convertToAlternative -> convertFromFacebookTarget(url)

            isTikTok && convertToAlternative -> {
                selections.domainFor(ProxyPlatform.TIKTOK)?.let { target ->
                    if (target.isNotBlank()) convertToTikTokProxy(url, target) else url
                } ?: url
            }
            isTikTokProxy && !convertToAlternative -> convertFromTikTokProxy(url)

            isReddit && convertToAlternative -> {
                selections.domainFor(ProxyPlatform.REDDIT)?.let { target ->
                    if (target.isNotBlank()) convertToRedditTarget(url, target) else url
                } ?: url
            }
            isRedditProxyHost(host) && !convertToAlternative -> convertFromRedditTarget(url)

            isYouTube && convertToAlternative -> {
                selections.domainFor(ProxyPlatform.YOUTUBE)?.let { target ->
                    if (target.isNotBlank()) convertToYouTubeTarget(url, target) else url
                } ?: url
            }
            isYouTubeProxyHost(host) && !convertToAlternative -> convertFromYouTubeTarget(url)

            isPinterest && convertToAlternative -> {
                selections.domainFor(ProxyPlatform.PINTEREST)?.let { target ->
                    if (target.isNotBlank()) convertToPinterestTarget(url, target) else url
                } ?: url
            }
            isPinterestProxyHost(host) && !convertToAlternative -> convertFromPinterestTarget(url)

            isThreads && convertToAlternative -> {
                selections.domainFor(ProxyPlatform.THREADS)?.let { target ->
                    if (target.isNotBlank()) convertToThreadsTarget(url, target) else url
                } ?: url
            }
            isThreadsProxyHost(host) && !convertToAlternative -> convertFromThreadsTarget(url)

            else -> url
        }
    } catch (e: Exception) {
        Timber.e(e, "Error applying domain conversions")
        url
    }

    // ---------------------------------------------------------------------
    // Detection
    // ---------------------------------------------------------------------

    fun isInstagramUrl(url: String): Boolean =
        UrlNormalizer.urlMatchesAnyDomain(
            url,
            listOf(Constants.INSTAGRAM_DOMAIN) + InstagramProxyStore.allKnownProxies(),
        )

    fun isTwitterUrl(url: String): Boolean = isKnownXUrl(url)

    fun isKnownXUrl(url: String): Boolean {
        val host = UrlNormalizer.extractAsciiHost(url) ?: return false
        if (UrlNormalizer.hostMatchesDomain(host, Constants.FARSIDE_DOMAIN)) {
            return isFarsideNitterUrl(url)
        }
        if (UrlNormalizer.hostMatchesDomain(host, Constants.TWITTER_DOMAIN) ||
            UrlNormalizer.hostMatchesDomain(host, Constants.X_DOMAIN)
        ) {
            return true
        }
        return ProxyRoster.allKnownDomains(ProxyPlatform.X).any {
            UrlNormalizer.hostMatchesDomain(host, it)
        }
    }

    fun isBlueskyUrl(url: String): Boolean {
        if (!isBlueskyHost(url)) return false
        val path = rawPath(url)
        return BLUESKY_POST_PATH.matches(path) || BLUESKY_PROFILE_PATH.matches(path)
    }

    fun isBlueskyHost(url: String): Boolean {
        val host = UrlNormalizer.extractAsciiHost(url) ?: return false
        return isBaseOrWwwHost(host, Constants.BLUESKY_DOMAIN) ||
            isBaseOrWwwHost(host, Constants.FXBSKY_DOMAIN) ||
            ProxyRoster.allKnownDomains(ProxyPlatform.BLUESKY).any {
                UrlNormalizer.hostMatchesDomain(host, it)
            }
    }

    fun isFacebookUrl(url: String): Boolean {
        val host = UrlNormalizer.extractAsciiHost(url) ?: return false
        return UrlNormalizer.hostMatchesDomain(host, Constants.FACEBOOK_DOMAIN) ||
            UrlNormalizer.hostMatchesDomain(host, Constants.FB_SHORT_DOMAIN) ||
            ProxyRoster.allKnownDomains(ProxyPlatform.FACEBOOK).any {
                UrlNormalizer.hostMatchesDomain(host, it)
            }
    }

    fun isTikTokUrl(url: String): Boolean =
        UrlNormalizer.urlMatchesAnyDomain(
            url,
            listOf(Constants.TIKTOK_DOMAIN) + TikTokProxyStore.allKnownProxies(),
        )

    fun isRedditUrl(url: String): Boolean {
        val host = UrlNormalizer.extractAsciiHost(url) ?: return false
        if (UrlNormalizer.hostMatchesDomain(host, Constants.REDDIT_SHORT_DOMAIN)) return false
        if (isRedditSourceHost(host)) return true
        return ProxyRoster.allKnownDomains(ProxyPlatform.REDDIT).any {
            UrlNormalizer.hostMatchesDomain(host, it)
        }
    }

    fun isYouTubeUrl(url: String): Boolean {
        val host = UrlNormalizer.extractAsciiHost(url) ?: return false
        if (UrlNormalizer.hostMatchesDomain(host, "music.${Constants.YOUTUBE_DOMAIN}")) return false
        if (ProxyRoster.allKnownDomains(ProxyPlatform.YOUTUBE).any {
                UrlNormalizer.hostMatchesDomain(host, it)
            }
        ) {
            return true
        }
        if (UrlNormalizer.hostMatchesDomain(host, Constants.YOUTUBE_SHORT_DOMAIN)) {
            val path = rawPath(url)
            return YOUTU_BE_VIDEO_PATH.matches(path)
        }
        if (!isYouTubeSourceHost(host)) return false
        return isSupportedYouTubePath(rawPath(url))
    }

    fun isPinterestUrl(url: String): Boolean {
        val host = UrlNormalizer.extractAsciiHost(url) ?: return false
        if (!isPinterestSourceHost(host) && !isPinterestProxyHost(host)) return false
        return PINTEREST_PIN_PATH.matches(rawPath(url))
    }

    fun isThreadsUrl(url: String): Boolean {
        val host = UrlNormalizer.extractAsciiHost(url) ?: return false
        if (!isThreadsSourceHost(host) && !isThreadsProxyHost(host)) return false
        return THREADS_PATH.matches(rawPath(url))
    }

    // ---------------------------------------------------------------------
    // X / Twitter
    // ---------------------------------------------------------------------

    private fun convertXForward(url: String, selection: String?): String {
        val target = selection?.takeIf { it.isNotBlank() } ?: return url
        return if (target == Constants.FIXUPX_DOMAIN) {
            convertToFixupxForward(url)
        } else {
            convertToXReaderForward(url, target)
        }
    }

    private fun convertToFixupxForward(url: String): String {
        val host = UrlNormalizer.extractAsciiHost(url)
        when {
            UrlNormalizer.hostMatchesDomain(host, Constants.FXTWITTER_DOMAIN) ->
                return url.replace(Constants.FXTWITTER_DOMAIN, Constants.FIXUPX_DOMAIN, ignoreCase = true)
            UrlNormalizer.hostMatchesDomain(host, Constants.VXTWITTER_DOMAIN) ->
                return url.replace(Constants.VXTWITTER_DOMAIN, Constants.FIXUPX_DOMAIN, ignoreCase = true)
            UrlNormalizer.hostMatchesDomain(host, Constants.FIXUPX_DOMAIN) -> return url
        }
        val normalized = normalizeXInputToTwitterHost(url)
        val converted = convertTwitterStatusToFixupx(normalized)
        return if (converted != normalized) converted else url
    }

    private fun convertToXReaderForward(url: String, targetDomain: String): String {
        val target = AlternativeFrontendCatalog.byDomain(ProxyPlatform.X, targetDomain)
            ?: ProxyRoster.targetByDomain(ProxyPlatform.X, targetDomain)
        val pathPrefix = target?.pathPrefix
        if (isAlreadyOnXTarget(url, targetDomain, pathPrefix)) return url
        val suffix = extractUrlSuffix(url, stripFarsidePrefix = true)
        val prefixedPath = if (pathPrefix != null) {
            withPathPrefix(pathPrefix, suffix.path)
        } else {
            suffix.path
        }
        return "${suffix.scheme}://$targetDomain$prefixedPath${suffix.query ?: ""}${suffix.fragment ?: ""}"
    }

    private fun convertXReverse(url: String): String {
        val host = UrlNormalizer.extractAsciiHost(url)
        when {
            UrlNormalizer.hostMatchesDomain(host, Constants.FXTWITTER_DOMAIN) ->
                return replaceHostDomain(url, Constants.FXTWITTER_DOMAIN, Constants.X_DOMAIN)
            UrlNormalizer.hostMatchesDomain(host, Constants.VXTWITTER_DOMAIN) ->
                return replaceHostDomain(url, Constants.VXTWITTER_DOMAIN, Constants.X_DOMAIN)
            UrlNormalizer.hostMatchesDomain(host, Constants.FIXUPX_DOMAIN) ->
                return replaceHostDomain(url, Constants.FIXUPX_DOMAIN, Constants.X_DOMAIN)
        }
        if (!isKnownXProxyOrReaderHost(host)) return url
        val suffix = extractUrlSuffix(url, stripFarsidePrefix = true)
        return "${suffix.scheme}://${Constants.X_DOMAIN}${suffix.tail()}"
    }

    private fun convertTwitterStatusToFixupx(url: String): String {
        val fragmentIndex = url.indexOf('#')
        val urlWithoutFragment = if (fragmentIndex > -1) url.substring(0, fragmentIndex) else url
        val fragment = if (fragmentIndex > -1) url.substring(fragmentIndex) else ""
        val match = TWITTER_STATUS_PATH.find(urlWithoutFragment) ?: return url
        val username = match.groupValues[1]
        val statusId = match.groupValues[2]
        return "https://${Constants.FIXUPX_DOMAIN}/$username${Constants.TWITTER_STATUS_PATH}$statusId$fragment"
    }

    private fun normalizeXInputToTwitterHost(url: String): String {
        val suffix = extractUrlSuffix(url, stripFarsidePrefix = true)
        return "${suffix.scheme}://${Constants.X_DOMAIN}${suffix.tail()}"
    }

    private fun isKnownXProxyOrReaderHost(host: String?): Boolean {
        if (host == null) return false
        if (UrlNormalizer.hostMatchesDomain(host, Constants.FIXUPX_DOMAIN) ||
            UrlNormalizer.hostMatchesDomain(host, Constants.FXTWITTER_DOMAIN) ||
            UrlNormalizer.hostMatchesDomain(host, Constants.VXTWITTER_DOMAIN)
        ) {
            return true
        }
        return ProxyRoster.allKnownDomains(ProxyPlatform.X).any {
            UrlNormalizer.hostMatchesDomain(host, it)
        }
    }

    private fun isAlreadyOnXTarget(url: String, targetDomain: String, pathPrefix: String?): Boolean {
        val host = UrlNormalizer.extractAsciiHost(url) ?: return false
        if (!UrlNormalizer.hostMatchesDomain(host, targetDomain)) return false
        if (pathPrefix == null) return true
        val path = rawPath(url)
        return path == pathPrefix || path.startsWith("$pathPrefix/")
    }

    fun isFarsideNitterUrl(url: String): Boolean {
        val host = UrlNormalizer.extractAsciiHost(url) ?: return false
        if (!UrlNormalizer.hostMatchesDomain(host, Constants.FARSIDE_DOMAIN)) return false
        val path = rawPath(url)
        return path == "/nitter" || path.startsWith("/nitter/")
    }

    // ---------------------------------------------------------------------
    // Instagram
    // ---------------------------------------------------------------------

    private fun convertToInstagramProxy(url: String, targetProxy: String): String {
        val knownProxies = InstagramProxyStore.allKnownProxies()
        val knownAlternation = (listOf(Constants.INSTAGRAM_DOMAIN) + knownProxies)
            .joinToString("|") { it.replace(".", "\\.") }
        val pattern = Pattern.compile(
            "^(https?://)(?:www\\.)?(?:[a-z0-9]+\\.)?($knownAlternation)(?=[/:?#]|$)",
            Pattern.CASE_INSENSITIVE,
        )
        val matcher = pattern.matcher(url)
        return if (matcher.find()) {
            matcher.replaceAll("\$1$targetProxy")
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

    private fun convertFromInstagramProxy(url: String): String {
        val knownProxies = InstagramProxyStore.allKnownProxies()
        if (UrlNormalizer.urlMatchesDomain(url, Constants.INSTAGRAM_DOMAIN) &&
            knownProxies.none { UrlNormalizer.urlMatchesDomain(url, it) }
        ) {
            return url
        }
        val proxyAlternation = knownProxies.joinToString("|") { it.replace(".", "\\.") }
        val pattern = Pattern.compile(
            "^(https?://)((?:www\\.)?(?:[a-z0-9]+\\.)?)($proxyAlternation)(?=[/:?#]|$)",
            Pattern.CASE_INSENSITIVE,
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

    // ---------------------------------------------------------------------
    // Bluesky
    // ---------------------------------------------------------------------

    private fun convertBlueskyForward(url: String, selections: ProxySelections): String {
        val selection = selections.domainFor(ProxyPlatform.BLUESKY)?.takeIf { it.isNotBlank() }
            ?: return url
        val path = rawPath(url)
        val isPost = BLUESKY_POST_PATH.matches(path)
        val isProfile = BLUESKY_PROFILE_PATH.matches(path)
        val target = AlternativeFrontendCatalog.byDomain(ProxyPlatform.BLUESKY, selection)
        val isEmbed = target?.role == FrontendRole.EMBED
        if (isEmbed && !isPost) return url
        if (!isPost && !isProfile) return url
        return swapBareHost(url, selection)
    }

    private fun convertBlueskyReverse(url: String): String {
        val host = UrlNormalizer.extractAsciiHost(url) ?: return url
        if (isBaseOrWwwHost(host, Constants.BLUESKY_DOMAIN)) return url
        val path = rawPath(url)
        val isPost = BLUESKY_POST_PATH.matches(path)
        val isProfile = BLUESKY_PROFILE_PATH.matches(path)
        if (isBaseOrWwwHost(host, Constants.FXBSKY_DOMAIN) && isPost) {
            return swapBareHost(url, Constants.BLUESKY_DOMAIN)
        }
        if (ProxyRoster.allKnownDomains(ProxyPlatform.BLUESKY).any {
                UrlNormalizer.hostMatchesDomain(host, it)
            } && (isPost || isProfile)
        ) {
            return swapBareHost(url, Constants.BLUESKY_DOMAIN)
        }
        return url
    }

    // ---------------------------------------------------------------------
    // Facebook
    // ---------------------------------------------------------------------

    private fun convertToFacebookTarget(url: String, target: String): String {
        if (UrlNormalizer.urlMatchesDomain(url, target)) return url
        val regex = Regex(
            "^(https?://)(?:www\\.)?(?:[a-z]+\\.)?(?:facebook|fb)\\.com(?=[/:?#]|$)",
            RegexOption.IGNORE_CASE,
        )
        return if (regex.containsMatchIn(url)) {
            url.replace(regex, "$1$target")
        } else {
            replaceHostDomain(url, Constants.FACEBOOK_DOMAIN, target)
                .let { if (it != url) it else replaceHostDomain(url, Constants.FB_SHORT_DOMAIN, target) }
        }
    }

    private fun convertFromFacebookTarget(url: String): String {
        val known = ProxyRoster.allKnownDomains(ProxyPlatform.FACEBOOK)
        val host = UrlNormalizer.extractAsciiHost(url)
        if (known.none { UrlNormalizer.hostMatchesDomain(host, it) }) return url
        return swapBareHost(url, Constants.FACEBOOK_DOMAIN)
    }

    private fun isFacebookProxyHost(host: String?): Boolean =
        host != null && ProxyRoster.allKnownDomains(ProxyPlatform.FACEBOOK).any {
            UrlNormalizer.hostMatchesDomain(host, it)
        }

    // ---------------------------------------------------------------------
    // TikTok
    // ---------------------------------------------------------------------

    private fun convertToTikTokProxy(url: String, targetProxy: String): String {
        val knownDomains = listOf(Constants.TIKTOK_DOMAIN) + TikTokProxyStore.allKnownProxies()
        val alternation = knownDomains.joinToString("|") { it.replace(".", "\\.") }
        val pattern = Pattern.compile(
            "^(https?://)((?:[a-z0-9-]+\\.)*)($alternation)(?=[/:?#]|$)",
            Pattern.CASE_INSENSITIVE,
        )
        val matcher = pattern.matcher(url)
        return if (matcher.find()) {
            matcher.replaceAll("\$1\$2$targetProxy")
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

    private fun convertFromTikTokProxy(url: String): String {
        val knownProxies = TikTokProxyStore.allKnownProxies()
        if (UrlNormalizer.urlMatchesDomain(url, Constants.TIKTOK_DOMAIN) &&
            knownProxies.none { UrlNormalizer.urlMatchesDomain(url, it) }
        ) {
            return url
        }
        val proxyAlternation = knownProxies.joinToString("|") { it.replace(".", "\\.") }
        val pattern = Pattern.compile(
            "^(https?://)((?:[a-z0-9-]+\\.)*)($proxyAlternation)(?=[/:?#]|$)",
            Pattern.CASE_INSENSITIVE,
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

    // ---------------------------------------------------------------------
    // Reddit
    // ---------------------------------------------------------------------

    private fun convertToRedditTarget(url: String, target: String): String {
        if (UrlNormalizer.urlMatchesDomain(url, target)) return url
        return collapseRedditHost(url, target)
    }

    private fun convertFromRedditTarget(url: String): String =
        collapseRedditHost(url, Constants.REDDIT_DOMAIN)

    private fun collapseRedditHost(url: String, target: String): String {
        val suffix = extractUrlSuffix(url, stripFarsidePrefix = false)
        return "${suffix.scheme}://$target${suffix.tail()}"
    }

    private fun isRedditSourceHost(host: String): Boolean =
        host == Constants.REDDIT_DOMAIN ||
            host == "www.${Constants.REDDIT_DOMAIN}" ||
            host == "old.${Constants.REDDIT_DOMAIN}" ||
            host == "new.${Constants.REDDIT_DOMAIN}"

    private fun isRedditProxyHost(host: String?): Boolean =
        host != null && ProxyRoster.allKnownDomains(ProxyPlatform.REDDIT).any {
            UrlNormalizer.hostMatchesDomain(host, it)
        }

    // ---------------------------------------------------------------------
    // YouTube
    // ---------------------------------------------------------------------

    private fun convertToYouTubeTarget(url: String, target: String): String {
        val host = UrlNormalizer.extractAsciiHost(url) ?: return url
        if (UrlNormalizer.hostMatchesDomain(host, target)) return url
        if (UrlNormalizer.hostMatchesDomain(host, Constants.YOUTUBE_SHORT_DOMAIN)) {
            return convertYoutuBeToTarget(url, target)
        }
        if (!isYouTubeSourceHost(host) && !isYouTubeProxyHost(host)) return url
        if (isYouTubeSourceHost(host) && !isSupportedYouTubePath(rawPath(url))) return url
        return swapBareHost(url, target)
    }

    private fun convertFromYouTubeTarget(url: String): String {
        val host = UrlNormalizer.extractAsciiHost(url) ?: return url
        if (isYouTubeSourceHost(host)) return url
        return swapBareHost(url, Constants.YOUTUBE_DOMAIN)
    }

    private fun convertYoutuBeToTarget(url: String, target: String): String {
        val suffix = extractUrlSuffix(url, stripFarsidePrefix = false)
        if (!YOUTU_BE_VIDEO_PATH.matches(suffix.path)) return url
        val videoId = suffix.path.removePrefix("/").removeSuffix("/")
        val query = suffix.query?.removePrefix("?").orEmpty()
        val mergedQuery = buildString {
            append("?v=").append(videoId)
            if (query.isNotBlank()) {
                append('&').append(query)
            }
        }
        return "${suffix.scheme}://$target/watch$mergedQuery${suffix.fragment ?: ""}"
    }

    private fun isYouTubeSourceHost(host: String): Boolean =
        UrlNormalizer.hostMatchesDomain(host, Constants.YOUTUBE_DOMAIN) ||
            UrlNormalizer.hostMatchesDomain(host, "www.${Constants.YOUTUBE_DOMAIN}") ||
            UrlNormalizer.hostMatchesDomain(host, "m.${Constants.YOUTUBE_DOMAIN}")

    private fun isYouTubeProxyHost(host: String?): Boolean =
        host != null && ProxyRoster.allKnownDomains(ProxyPlatform.YOUTUBE).any {
            UrlNormalizer.hostMatchesDomain(host, it)
        }

    private fun isSupportedYouTubePath(path: String): Boolean =
        path == "/watch" ||
            path.startsWith("/watch/") ||
            path == "/playlist" ||
            path.startsWith("/playlist/") ||
            YOUTUBE_ID_PATH.matches(path) ||
            YOUTUBE_HANDLE_PATH.matches(path)

    // ---------------------------------------------------------------------
    // Pinterest
    // ---------------------------------------------------------------------

    private fun convertToPinterestTarget(url: String, target: String): String {
        if (!PINTEREST_PIN_PATH.matches(rawPath(url))) return url
        return swapBareHost(url, target)
    }

    private fun convertFromPinterestTarget(url: String): String {
        if (!PINTEREST_PIN_PATH.matches(rawPath(url))) return url
        return swapBareHost(url, Constants.PINTEREST_DOMAIN)
    }

    private fun isPinterestSourceHost(host: String): Boolean =
        UrlNormalizer.hostMatchesDomain(host, Constants.PINTEREST_DOMAIN) ||
            UrlNormalizer.hostMatchesDomain(host, "www.${Constants.PINTEREST_DOMAIN}")

    private fun isPinterestProxyHost(host: String?): Boolean =
        host != null && ProxyRoster.allKnownDomains(ProxyPlatform.PINTEREST).any {
            UrlNormalizer.hostMatchesDomain(host, it)
        }

    // ---------------------------------------------------------------------
    // Threads
    // ---------------------------------------------------------------------

    private fun convertToThreadsTarget(url: String, target: String): String {
        if (!THREADS_PATH.matches(rawPath(url))) return url
        return swapBareHost(url, target)
    }

    private fun convertFromThreadsTarget(url: String): String {
        if (!THREADS_PATH.matches(rawPath(url))) return url
        return swapBareHost(url, Constants.THREADS_COM_DOMAIN)
    }

    private fun isThreadsSourceHost(host: String): Boolean =
        UrlNormalizer.hostMatchesDomain(host, Constants.THREADS_NET_DOMAIN) ||
            UrlNormalizer.hostMatchesDomain(host, Constants.THREADS_COM_DOMAIN) ||
            UrlNormalizer.hostMatchesDomain(host, "www.${Constants.THREADS_NET_DOMAIN}") ||
            UrlNormalizer.hostMatchesDomain(host, "www.${Constants.THREADS_COM_DOMAIN}")

    private fun isThreadsProxyHost(host: String?): Boolean =
        host != null && ProxyRoster.allKnownDomains(ProxyPlatform.THREADS).any {
            UrlNormalizer.hostMatchesDomain(host, it)
        }

    // ---------------------------------------------------------------------
    // Shared URL helpers
    // ---------------------------------------------------------------------

    private data class UrlSuffix(
        val scheme: String,
        val path: String,
        val query: String?,
        val fragment: String?,
    ) {
        fun tail(): String = path + (query ?: "") + (fragment ?: "")
    }

    private fun extractUrlSuffix(url: String, stripFarsidePrefix: Boolean): UrlSuffix {
        val schemeEnd = url.indexOf("://")
        val scheme = if (schemeEnd >= 0) url.substring(0, schemeEnd) else "https"
        val authorityStart = if (schemeEnd >= 0) schemeEnd + 3 else 0
        val pathStart = url.indexOfAny(charArrayOf('/', '?', '#'), authorityStart)
            .let { if (it >= 0) it else url.length }
        var path = if (pathStart < url.length && url[pathStart] == '/') {
            val pathEnd = url.indexOfAny(charArrayOf('?', '#'), pathStart)
                .let { if (it >= 0) it else url.length }
            url.substring(pathStart, pathEnd)
        } else {
            ""
        }
        if (stripFarsidePrefix) {
            path = stripFarsidePathPrefix(path)
        }
        val queryStart = url.indexOf('?', pathStart).takeIf { it >= 0 }
        val fragmentStart = url.indexOf('#', pathStart).takeIf { it >= 0 }
        val query = queryStart?.let { start ->
            val end = fragmentStart?.takeIf { it > start } ?: url.length
            url.substring(start, end)
        }
        val fragment = fragmentStart?.let { url.substring(it) }
        return UrlSuffix(scheme, path, query, fragment)
    }

    private fun stripFarsidePathPrefix(path: String): String {
        if (path == "/nitter") return "/"
        if (path.startsWith("/nitter/")) return path.removePrefix("/nitter")
        return path
    }

    private fun withPathPrefix(prefix: String, path: String): String {
        val normalized = if (path.isEmpty() || path == "/") "" else path
        return if (normalized.startsWith("/")) "$prefix$normalized" else "$prefix/$normalized"
    }

    private fun swapBareHost(url: String, targetDomain: String): String {
        val suffix = extractUrlSuffix(url, stripFarsidePrefix = false)
        return "${suffix.scheme}://$targetDomain${suffix.tail()}"
    }

    private fun replaceHostDomain(url: String, domain: String, target: String): String {
        val pattern = Regex(
            "^(https?://(?:[a-z0-9-]+\\.)*)${Regex.escape(domain)}(?=[/:?#]|$)",
            RegexOption.IGNORE_CASE,
        )
        return pattern.replaceFirst(url, "\$1$target")
    }

    private fun isBaseOrWwwHost(host: String?, domain: String): Boolean {
        if (host == null) return false
        return listOf(domain, "www.$domain").any { allowedHost ->
            UrlNormalizer.hostMatchesDomain(host, allowedHost) &&
                UrlNormalizer.hostMatchesDomain(allowedHost, host)
        }
    }

    private fun rawPath(url: String): String {
        val authorityStart = url.indexOf("://").let { if (it >= 0) it + 3 else 0 }
        val pathStart = url.indexOfAny(charArrayOf('/', '?', '#'), authorityStart)
        if (pathStart < 0 || url[pathStart] != '/') return ""
        val pathEnd = url.indexOfAny(charArrayOf('?', '#'), pathStart)
            .let { if (it >= 0) it else url.length }
        return url.substring(pathStart, pathEnd)
    }
}
