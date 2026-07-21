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

import com.fixupxer.processing.UrlNormalizer

/**
 * Process-wide per-platform roster of custom domains and disabled built-in targets.
 *
 * Built-in catalog entries live in [AlternativeFrontendCatalog]; this object tracks
 * user overrides (custom additions and built-in removals) as immutable snapshots
 * swapped via @Volatile fields.
 *
 * Thread safety: each platform snapshot is replaced atomically on mutation.
 */
object ProxyRoster {

    private data class PlatformState(
        val customDomains: List<String> = emptyList(),
        val disabledBuiltInIds: Set<String> = emptySet(),
    )

    @Volatile
    private var states: Map<ProxyPlatform, PlatformState> = emptyMap()

    private fun stateFor(platform: ProxyPlatform): PlatformState =
        states[platform] ?: PlatformState()

    @Synchronized
    private fun updatePlatform(platform: ProxyPlatform, transform: (PlatformState) -> PlatformState) {
        val current = states.toMutableMap()
        current[platform] = transform(stateFor(platform))
        states = current
    }

    fun setCustomProxies(platform: ProxyPlatform, proxies: List<String>) {
        updatePlatform(platform) { it.copy(customDomains = proxies.toList()) }
    }

    fun getCustomProxies(platform: ProxyPlatform): List<String> =
        stateFor(platform).customDomains

    fun setDisabledBuiltIns(platform: ProxyPlatform, ids: Set<String>) {
        updatePlatform(platform) { it.copy(disabledBuiltInIds = ids.toSet()) }
    }

    fun getDisabledBuiltIns(platform: ProxyPlatform): Set<String> =
        stateFor(platform).disabledBuiltInIds

    /**
     * Active selectable targets: enabled built-ins (catalog order) then custom entries.
     */
    fun activeTargets(platform: ProxyPlatform): List<FrontendTarget> {
        val state = stateFor(platform)
        val builtIn = AlternativeFrontendCatalog.builtIn(platform)
            .filterNot { it.id in state.disabledBuiltInIds }
        val custom = state.customDomains.map { domain ->
            FrontendTarget(
                id = "custom:$domain",
                platform = platform,
                domain = domain,
                role = FrontendRole.READER,
                allowNativeApp = false,
            )
        }
        return builtIn + custom
    }

    /**
     * Every domain recognised for a platform (built-in including disabled, custom, legacy).
     * Used to detect old pasted links regardless of current user selection.
     */
    fun allKnownDomains(platform: ProxyPlatform): List<String> {
        val builtInDomains = AlternativeFrontendCatalog.builtIn(platform).map { it.domain }
        val customDomains = stateFor(platform).customDomains
        val legacy = AlternativeFrontendCatalog.legacyDomains(platform)
        return (builtInDomains + customDomains + legacy).distinct()
    }

    fun allKnownDomainsAllPlatforms(): Set<String> =
        ProxyPlatform.entries.flatMap { allKnownDomains(it) }.toSet()

    /**
     * Resolve a [FrontendTarget] by [domain] among all built-ins (including disabled)
     * and custom entries for [platform].
     */
    fun targetByDomain(platform: ProxyPlatform, domain: String): FrontendTarget? {
        AlternativeFrontendCatalog.byDomain(platform, domain)?.let { return it }
        if (domain in getCustomProxies(platform)) {
            return FrontendTarget(
                id = "custom:$domain",
                platform = platform,
                domain = domain,
                role = FrontendRole.READER,
                allowNativeApp = false,
            )
        }
        return null
    }

    /** Clears all platform state — test helper. */
    @Synchronized
    fun reset() {
        states = emptyMap()
    }

    /** Clears state for a single platform — used by legacy store facades. */
    @Synchronized
    fun resetPlatform(platform: ProxyPlatform) {
        val current = states.toMutableMap()
        current.remove(platform)
        states = current
    }

    // ---------------------------------------------------------------------
    // Shared custom proxy input validation
    // ---------------------------------------------------------------------

    private val DOMAIN_FORMAT = Regex("^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}$")

    /**
     * Normalize raw user input to a bare lowercase hostname:
     * strips protocol, `www.` prefix, path/query/fragment and whitespace.
     */
    fun normalizeCustomProxyInput(raw: String): String {
        return raw.trim()
            .lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
    }

    /** True when [domain] looks like a plain registrable hostname (subdomains allowed). */
    fun isValidProxyDomainFormat(domain: String): Boolean {
        return domain.length <= 253 && DOMAIN_FORMAT.matches(domain)
    }

    /**
     * Domains the app already routes specially — host-boundary check in both directions
     * so parent domains (e.g. catsarch.com) and subdomains (e.g. sub.fixupx.com) are
     * rejected without the old substring false positives.
     */
    fun isReservedDomain(domain: String): Boolean {
        val reserved = buildReservedDomainList()
        return reserved.any { reservedEntry ->
            UrlNormalizer.hostMatchesDomain(domain, reservedEntry) ||
                UrlNormalizer.hostMatchesDomain(reservedEntry, domain)
        }
    }

    /** True when [domain] is already present among built-ins, legacy, or customs. */
    fun isDuplicate(platform: ProxyPlatform, domain: String): Boolean {
        val known = AlternativeFrontendCatalog.builtIn(platform).map { it.domain } +
            AlternativeFrontendCatalog.legacyDomains(platform) +
            getCustomProxies(platform)
        return domain in known
    }

    private fun buildReservedDomainList(): List<String> {
        val entries = mutableListOf<String>()
        entries += Constants.FARSIDE_DOMAIN
        ProxyPlatform.entries.forEach { platform ->
            entries += AlternativeFrontendCatalog.sourceDomains(platform)
            entries += AlternativeFrontendCatalog.builtIn(platform).map { it.domain }
            entries += AlternativeFrontendCatalog.legacyDomains(platform)
            entries += getCustomProxies(platform)
        }
        entries += Constants.RETIRED_UNSAFE_FRONTEND_DOMAINS
        return entries.distinct()
    }
}
