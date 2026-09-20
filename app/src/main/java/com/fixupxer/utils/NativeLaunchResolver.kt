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

import com.fixupxer.processing.NormalizedUrl
import com.fixupxer.processing.UrlNormalizer

/**
 * Resolves the URI and package candidates used only for a native-app handoff.
 * The final processed URL remains unchanged for every other destination.
 */
object NativeLaunchResolver {

    data class Resolution(
        val uri: String,
        val packageNames: List<String>,
    )

    private sealed interface Canonicalization {
        data class Resolved(val uri: String) : Canonicalization
        data object Unchanged : Canonicalization
        data object Unsupported : Canonicalization
    }

    private val xEmbedDomains = setOf(
        Constants.FIXUPX_DOMAIN,
        Constants.FXTWITTER_DOMAIN,
        Constants.VXTWITTER_DOMAIN,
    )

    private val instagramEmbedDomains = (
        AlternativeFrontendCatalog.builtIn(ProxyPlatform.INSTAGRAM)
            .filter { it.role == FrontendRole.EMBED && it.allowNativeApp }
            .map { it.domain } +
            AlternativeFrontendCatalog.legacyDomains(ProxyPlatform.INSTAGRAM)
        ).toSet()

    private val tikTokEmbedDomains = (
        AlternativeFrontendCatalog.builtIn(ProxyPlatform.TIKTOK)
            .filter { it.role == FrontendRole.EMBED && it.allowNativeApp }
            .map { it.domain } +
            AlternativeFrontendCatalog.legacyDomains(ProxyPlatform.TIKTOK)
        ).toSet()

    fun resolve(finalUrl: String): Resolution? {
        val normalized = runCatching { UrlNormalizer().normalize(finalUrl) }.getOrNull()
            ?: return null
        val host = normalized.asciiHost
        if (NativeAppMapping.isReaderOnlyUrl(finalUrl, host)) return null

        val nativeUrl = when (val result = canonicalizeEmbed(normalized)) {
            is Canonicalization.Resolved -> result.uri
            Canonicalization.Unchanged -> finalUrl
            Canonicalization.Unsupported -> return null
        }
        val packageNames = NativeAppMapping.packagesFor(finalUrl, host)
        return packageNames.takeIf { it.isNotEmpty() }?.let {
            Resolution(uri = nativeUrl, packageNames = it)
        }
    }

    private fun canonicalizeEmbed(url: NormalizedUrl): Canonicalization = when {
        xEmbedDomains.any { UrlNormalizer.hostMatchesDomain(url.asciiHost, it) } ->
            canonicalizeBaseOrWww(url, xEmbedDomains, Constants.X_DOMAIN)
        instagramEmbedDomains.any { UrlNormalizer.hostMatchesDomain(url.asciiHost, it) } ->
            canonicalizeBaseOrWww(
                url,
                instagramEmbedDomains,
                Constants.INSTAGRAM_DOMAIN,
            )
        tikTokEmbedDomains.any { UrlNormalizer.hostMatchesDomain(url.asciiHost, it) } ->
            canonicalizeTikTok(url)
        else -> Canonicalization.Unchanged
    }

    private fun canonicalizeBaseOrWww(
        url: NormalizedUrl,
        domains: Set<String>,
        destinationHost: String,
    ): Canonicalization {
        if (url.userInfo != null || url.port != null) return Canonicalization.Unsupported
        val supported = domains.any { domain ->
            url.asciiHost == domain || url.asciiHost == "www.$domain"
        }
        if (!supported) return Canonicalization.Unsupported
        return Canonicalization.Resolved(rebuild(url, destinationHost))
    }

    private fun canonicalizeTikTok(url: NormalizedUrl): Canonicalization {
        if (url.userInfo != null || url.port != null) return Canonicalization.Unsupported
        val prefix = tikTokEmbedDomains.firstNotNullOfOrNull { domain ->
            when (url.asciiHost) {
                domain -> ""
                "www.$domain" -> "www."
                "vm.$domain" -> "vm."
                "vt.$domain" -> "vt."
                else -> null
            }
        } ?: return Canonicalization.Unsupported
        return Canonicalization.Resolved(rebuild(url, "$prefix${Constants.TIKTOK_DOMAIN}"))
    }

    private fun rebuild(url: NormalizedUrl, host: String): String = buildString {
        append(url.scheme).append("://").append(host).append(url.rawPath)
        if (url.rawQuery != null) append('?').append(url.rawQuery)
        if (url.rawFragment != null) append('#').append(url.rawFragment)
    }
}
