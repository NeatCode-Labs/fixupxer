// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026 NeatCode Labs
 * This program is free software under the GNU General Public License,
 * version 3 or (at your option) any later version.
 */
package com.fixupxer.processing

import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import java.net.URI

enum class PipelineStatus { COMPLETE, FRONTEND_UNAVAILABLE, UNSUPPORTED_CONVERSION, INVALID_RESULT, CYCLE, HOP_LIMIT, STALE_CONFIGURATION }

data class BrowserFrontendSnapshot(
    val preferences: Map<ProxyPlatform, BrowserFrontendPreference>,
    val roster: ProxyRoster.Snapshot = ProxyRoster.snapshot(),
) {
    fun platformFor(url: String): ProxyPlatform? {
        val host = UrlNormalizer.extractAsciiHost(url) ?: return null
        if (UrlNormalizer.hostMatchesDomain(host, Constants.FARSIDE_DOMAIN) &&
            !PlatformDomainConverter.isFarsideNitterUrl(url)) return null
        return ProxyPlatform.entries.firstOrNull { platform ->
            roster.knownDomains[platform].orEmpty().any { UrlNormalizer.hostMatchesDomain(host, it) }
        }
    }

    fun convert(url: String): BrowserFrontendConversion {
        if (roster.revision != ProxyRoster.revision) {
            return BrowserFrontendConversion(url, PipelineStatus.STALE_CONFIGURATION)
        }
        val platform = platformFor(url) ?: return BrowserFrontendConversion(url)
        val preference = preferences[platform] ?: BrowserFrontendPreference(BrowserConversionMode.CLEAN_ONLY, null)
        if (preference.mode == BrowserConversionMode.CLEAN_ONLY) return BrowserFrontendConversion(url)
        val target = BrowserFrontendPolicy.resolve(platform, preference, roster.activeTargets[platform].orEmpty())
            ?: return BrowserFrontendConversion(url, if (preference.mode == BrowserConversionMode.READER) {
                PipelineStatus.COMPLETE // Preserve the documented legacy reader fallback.
            } else PipelineStatus.FRONTEND_UNAVAILABLE)
        val uri = runCatching { URI(url) }.getOrNull()
        if (!PlatformDomainConverter.supportsBrowserTarget(url, platform, target) || uri == null || uri.rawUserInfo != null ||
            uri.port != -1 && !(uri.scheme.equals("https", true) && uri.port == 443) &&
            !(uri.scheme.equals("http", true) && uri.port == 80)) {
            return BrowserFrontendConversion(url, PipelineStatus.UNSUPPORTED_CONVERSION)
        }
        val converted = PlatformDomainConverter.apply(url, true, ProxySelections(mapOf(platform to target.domain)))
        if (roster.revision != ProxyRoster.revision) return BrowserFrontendConversion(url, PipelineStatus.STALE_CONFIGURATION)
        val host = UrlNormalizer.extractAsciiHost(converted)
        val onTarget = UrlNormalizer.hostMatchesDomain(host, target.domain) &&
            (target.pathPrefix == null || URI(converted).rawPath.let { it == target.pathPrefix || it.startsWith("${target.pathPrefix}/") })
        return BrowserFrontendConversion(converted, if (onTarget) PipelineStatus.COMPLETE else PipelineStatus.UNSUPPORTED_CONVERSION)
    }
}

data class BrowserFrontendConversion(val url: String, val status: PipelineStatus = PipelineStatus.COMPLETE)
