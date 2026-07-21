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

package com.fixupxer.utils

import com.fixupxer.processing.PlatformDomainConverter
import com.fixupxer.processing.UrlNormalizer

/**
 * Pure JVM-safe host → native Android package mapping for cleaned URLs.
 */
object NativeAppMapping {

    private val YOUTUBE_PACKAGES = listOf(
        "app.revanced.android.youtube",
        "app.morphe.android.youtube",
        "com.google.android.youtube",
    )

    data class NativeAppDecision(
        val packageName: String?,
        val reason: String? = null,
    )

    fun resolvePackage(url: String, host: String?): NativeAppDecision {
        if (host.isNullOrBlank()) return NativeAppDecision(null)

        if (isReaderOnlyUrl(url, host)) {
            return NativeAppDecision(null, "privacy_reader")
        }

        if (UrlNormalizer.hostMatchesDomain(host, Constants.YOUTUBE_DOMAIN) ||
            UrlNormalizer.hostMatchesDomain(host, Constants.YOUTUBE_SHORT_DOMAIN) ||
            ProxyRoster.allKnownDomains(ProxyPlatform.YOUTUBE).any {
                UrlNormalizer.hostMatchesDomain(host, it)
            }
        ) {
            return NativeAppDecision(null, "youtube_handled_separately")
        }

        return when {
            isInstagramNativeHost(host) ->
                NativeAppDecision("com.instagram.android")
            isTwitterNativeHost(host) ->
                NativeAppDecision("com.twitter.android")
            UrlNormalizer.hostMatchesDomain(host, Constants.FACEBOOK_DOMAIN) ->
                NativeAppDecision("com.facebook.katana")
            isRedditNativeHost(host) ->
                NativeAppDecision("com.reddit.frontpage")
            UrlNormalizer.hostMatchesDomain(host, Constants.LINKEDIN_DOMAIN) ->
                NativeAppDecision("com.linkedin.android")
            UrlNormalizer.hostMatchesDomain(host, Constants.AMAZON_DOMAIN) ||
                UrlNormalizer.hostMatchesDomain(host, Constants.AMAZON_SHORT_DOMAIN) ->
                NativeAppDecision("com.amazon.mShop.android.shopping")
            isGoogleAppUrl(url, host) ->
                NativeAppDecision("com.google.android.googlequicksearchbox")
            isTikTokNativeHost(host) ->
                NativeAppDecision("com.zhiliaoapp.musically")
            UrlNormalizer.hostMatchesDomain(host, Constants.SUBSTACK_DOMAIN) ->
                NativeAppDecision("com.substack.app")
            else -> NativeAppDecision(null)
        }
    }

    fun packagesFor(url: String, host: String?): List<String> {
        val decision = resolvePackage(url, host)
        return if (decision.reason == "youtube_handled_separately") {
            YOUTUBE_PACKAGES
        } else {
            listOfNotNull(decision.packageName)
        }
    }

    fun isReaderOnlyHost(host: String): Boolean {
        if (UrlNormalizer.hostMatchesDomain(host, Constants.FARSIDE_DOMAIN)) return false
        ProxyPlatform.entries.forEach { platform ->
            AlternativeFrontendCatalog.builtIn(platform).forEach { target ->
                if (!target.allowNativeApp &&
                    target.pathPrefix == null &&
                    UrlNormalizer.hostMatchesDomain(host, target.domain)
                ) {
                    return true
                }
            }
            ProxyRoster.getCustomProxies(platform).forEach { custom ->
                if (UrlNormalizer.hostMatchesDomain(host, custom)) return true
            }
        }
        return false
    }

    /**
     * True when the FINAL url points at a reader-only destination, including the
     * Farside/nitter path form that [isReaderOnlyHost] alone cannot detect.
     */
    fun isReaderOnlyUrl(url: String, host: String): Boolean {
        if (UrlNormalizer.hostMatchesDomain(host, Constants.FARSIDE_DOMAIN)) {
            return PlatformDomainConverter.isFarsideNitterUrl(url)
        }
        if (isReaderOnlyHost(host)) return true
        return false
    }

    private fun isGoogleAppUrl(url: String, host: String): Boolean {
        if (!UrlNormalizer.hostMatchesDomain(host, Constants.GOOGLE_DOMAIN)) return false
        val authorityStart = url.indexOf("://").let { if (it >= 0) it + 3 else 0 }
        val pathStart = url.indexOfAny(charArrayOf('/', '?', '#'), authorityStart)
        if (pathStart < 0 || url[pathStart] != '/') return false
        val pathEnd = url.indexOfAny(charArrayOf('?', '#'), pathStart)
            .let { if (it >= 0) it else url.length }
        val path = url.substring(pathStart, pathEnd).lowercase()
        return path == "/search" ||
            path.startsWith("/search/") ||
            path == "/url" ||
            path.startsWith("/url/")
    }

    private fun isInstagramNativeHost(host: String): Boolean {
        if (UrlNormalizer.hostMatchesDomain(host, Constants.INSTAGRAM_DOMAIN)) return true
        return InstagramProxyStore.allKnownProxies().any { proxy ->
            UrlNormalizer.hostMatchesDomain(host, proxy) &&
                ProxyRoster.targetByDomain(ProxyPlatform.INSTAGRAM, proxy)?.allowNativeApp != false
        }
    }

    private fun isTwitterNativeHost(host: String): Boolean {
        if (UrlNormalizer.hostMatchesDomain(host, Constants.X_DOMAIN) ||
            UrlNormalizer.hostMatchesDomain(host, Constants.TWITTER_DOMAIN) ||
            UrlNormalizer.hostMatchesDomain(host, Constants.FIXUPX_DOMAIN)
        ) {
            return true
        }
        return false
    }

    private fun isRedditNativeHost(host: String): Boolean =
        host == Constants.REDDIT_DOMAIN ||
            host == "www.${Constants.REDDIT_DOMAIN}" ||
            host == "old.${Constants.REDDIT_DOMAIN}" ||
            host == "new.${Constants.REDDIT_DOMAIN}"

    private fun isTikTokNativeHost(host: String): Boolean {
        if (UrlNormalizer.hostMatchesDomain(host, Constants.TIKTOK_DOMAIN)) return true
        return TikTokProxyStore.allKnownProxies().any { proxy ->
            UrlNormalizer.hostMatchesDomain(host, proxy) &&
                ProxyRoster.targetByDomain(ProxyPlatform.TIKTOK, proxy)?.allowNativeApp != false
        }
    }

}
