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

    enum class CustomProxyValidationError {
        INVALID_DOMAIN,
        RESERVED_DOMAIN,
        DUPLICATE,
    }

    data class Snapshot(
        val revision: Long,
        val activeTargets: Map<ProxyPlatform, List<FrontendTarget>>,
        val knownDomains: Map<ProxyPlatform, List<String>>,
    )

    @Volatile
    var revision: Long = 0L
        private set

    @Synchronized
    fun snapshot(): Snapshot = Snapshot(
        revision,
        ProxyPlatform.entries.associateWith(::activeTargets),
        ProxyPlatform.entries.associateWith { platform ->
            (AlternativeFrontendCatalog.sourceDomains(platform) + allKnownDomains(platform)).distinct()
        },
    )

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
        revision++
        BrowserViewGate.invalidate()
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
        revision++
        BrowserViewGate.invalidate()
    }

    /** Clears state for a single platform — used by legacy store facades. */
    @Synchronized
    fun resetPlatform(platform: ProxyPlatform) {
        val current = states.toMutableMap()
        current.remove(platform)
        states = current
        revision++
        BrowserViewGate.invalidate()
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
    fun isReservedDomain(
        domain: String,
        customProxies: Map<ProxyPlatform, List<String>> = currentCustomProxies(),
        includeCustomDomains: Boolean = true,
    ): Boolean {
        val reserved = buildReservedDomainList(customProxies, includeCustomDomains)
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

    /**
     * Validates a custom domain against an optional staged roster. Browser settings
     * uses this before Save so nested picker edits do not touch the live roster.
     */
    fun validateCustomProxy(
        platform: ProxyPlatform,
        raw: String,
        customProxies: Map<ProxyPlatform, List<String>> = currentCustomProxies(),
        excludingDomain: String? = null,
    ): CustomProxyValidationError? {
        val domain = normalizeCustomProxyInput(raw)
        if (!isValidProxyDomainFormat(domain)) {
            return CustomProxyValidationError.INVALID_DOMAIN
        }

        val staged = customProxies.mapValues { (_, values) ->
            values
        }
            .toMutableMap()
            .apply {
                if (excludingDomain != null) {
                    this[platform] = this[platform].orEmpty().filterNot { it == excludingDomain }
                }
            }
        if (isReservedDomain(domain, staged, includeCustomDomains = false)) {
            return CustomProxyValidationError.RESERVED_DOMAIN
        }
        val overlaps = staged.values.flatten().any { existing ->
            UrlNormalizer.hostMatchesDomain(domain, existing) ||
                UrlNormalizer.hostMatchesDomain(existing, domain)
        }
        if (overlaps) return CustomProxyValidationError.DUPLICATE
        val duplicate = AlternativeFrontendCatalog.builtIn(platform).any { it.domain == domain } ||
            AlternativeFrontendCatalog.legacyDomains(platform).contains(domain) ||
            staged.values.flatten().any { it == domain }
        if (duplicate) return CustomProxyValidationError.DUPLICATE
        return null
    }

    /** Validates a complete staged custom roster before it is committed atomically. */
    fun validateCustomProxyMap(customProxies: Map<ProxyPlatform, List<String>>) {
        ProxyPlatform.entries.forEach { platform ->
            val values = customProxies[platform].orEmpty()
            require(values == values.map(::normalizeCustomProxyInput)) {
                "Custom proxy domain is not normalized"
            }
            require(values.size == values.toSet().size) {
                "Duplicate custom proxy for $platform"
            }
            values.forEach { domain ->
                require(isValidProxyDomainFormat(domain)) {
                    "Invalid custom proxy domain format"
                }
                require(!isReservedDomain(domain, customProxies, includeCustomDomains = false)) {
                    "Custom proxy collides with a reserved domain"
                }
            }
        }
        ProxyPlatform.entries.forEach { platform ->
            val values = customProxies[platform].orEmpty()
            val otherValues = ProxyPlatform.entries
                .filter { it != platform }
                .flatMap { customProxies[it].orEmpty() }
            values.forEach { domain ->
                require(otherValues.none { other ->
                    UrlNormalizer.hostMatchesDomain(domain, other) ||
                        UrlNormalizer.hostMatchesDomain(other, domain)
                }) { "Custom proxy collides with another platform's custom proxy" }
            }
        }
    }

    private fun currentCustomProxies(): Map<ProxyPlatform, List<String>> =
        ProxyPlatform.entries.associateWith(::getCustomProxies)

    private fun buildReservedDomainList(
        customProxies: Map<ProxyPlatform, List<String>> = currentCustomProxies(),
        includeCustomDomains: Boolean = true,
    ): List<String> {
        val entries = mutableListOf<String>()
        entries += Constants.FARSIDE_DOMAIN
        ProxyPlatform.entries.forEach { platform ->
            entries += AlternativeFrontendCatalog.sourceDomains(platform)
            entries += AlternativeFrontendCatalog.builtIn(platform).map { it.domain }
            entries += AlternativeFrontendCatalog.legacyDomains(platform)
            if (includeCustomDomains) entries += customProxies[platform].orEmpty()
        }
        entries += Constants.RETIRED_UNSAFE_FRONTEND_DOMAINS
        return entries.distinct()
    }
}
