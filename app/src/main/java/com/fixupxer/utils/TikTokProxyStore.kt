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

/**
 * Thin facade over [ProxyRoster] for TikTok embed proxy domains — mirrors [InstagramProxyStore].
 *
 * The fixed roster lives in [AlternativeFrontendCatalog]; user-defined custom proxies
 * are persisted by `PreferencesManager` (`custom_tiktok_proxies` pref) which mirrors
 * them into [ProxyRoster] on construction and on every mutation.
 *
 * Thread safety: delegated to [ProxyRoster]'s immutable snapshot model.
 */
object TikTokProxyStore {

    private val platform = ProxyPlatform.TIKTOK

    /** Replace the custom proxy snapshot (called by PreferencesManager). */
    fun setCustomProxies(proxies: List<String>) {
        ProxyRoster.setCustomProxies(platform, proxies)
    }

    fun getCustomProxies(): List<String> = ProxyRoster.getCustomProxies(platform)

    /** Active selectable targets for TikTok embed proxies. */
    fun activeProxies(): List<String> =
        ProxyRoster.activeTargets(platform).map { it.domain }

    /** Every domain recognised as a TikTok proxy (all built-ins + custom + legacy). */
    fun allKnownProxies(): List<String> = ProxyRoster.allKnownDomains(platform)

    /** Test helper — clears TikTok platform state so tests don't leak into each other. */
    fun reset() {
        ProxyRoster.resetPlatform(platform)
    }

    fun normalizeCustomProxyInput(raw: String): String =
        ProxyRoster.normalizeCustomProxyInput(raw)

    fun isValidProxyDomainFormat(domain: String): Boolean =
        ProxyRoster.isValidProxyDomainFormat(domain)

    fun isReservedDomain(domain: String): Boolean =
        ProxyRoster.isReservedDomain(domain)

    fun isDuplicate(domain: String): Boolean =
        ProxyRoster.isDuplicate(platform, domain)
}
