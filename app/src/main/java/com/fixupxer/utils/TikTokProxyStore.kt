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
 * Process-wide registry of TikTok embed proxy domains — mirrors [InstagramProxyStore].
 *
 * The fixed roster lives in [Constants]; user-defined custom proxies are persisted by
 * `PreferencesManager` (`custom_tiktok_proxies` pref) which mirrors them into this
 * store on construction and on every mutation. Stateless consumers that cannot receive
 * injected dependencies (cleaner `object`s, [Constants]-style static checks) read the
 * combined lists from here.
 *
 * Thread safety: the custom list is an immutable snapshot swapped via a @Volatile field.
 */
object TikTokProxyStore {

    @Volatile
    private var customProxies: List<String> = emptyList()

    /** Replace the custom proxy snapshot (called by PreferencesManager). */
    fun setCustomProxies(proxies: List<String>) {
        customProxies = proxies.toList()
    }

    fun getCustomProxies(): List<String> = customProxies

    /** Proxies the user can actively pick: fixed roster + custom entries. */
    fun activeProxies(): List<String> = Constants.TIKTOK_PROXY_DOMAINS + customProxies

    /** Every domain recognised as a TikTok proxy (active + retired legacy). */
    fun allKnownProxies(): List<String> = activeProxies() + Constants.TIKTOK_LEGACY_PROXIES

    /** Test helper — clears custom proxies so tests don't leak state into each other. */
    fun reset() {
        customProxies = emptyList()
    }

    // ---------------------------------------------------------------------
    // Custom proxy input validation (same rules as InstagramProxyStore)
    // ---------------------------------------------------------------------

    /**
     * Normalize raw user input to a bare lowercase hostname:
     * strips protocol, `www.` prefix, path/query/fragment and whitespace.
     */
    fun normalizeCustomProxyInput(raw: String): String =
        InstagramProxyStore.normalizeCustomProxyInput(raw)

    /** True when [domain] looks like a plain registrable hostname (subdomains allowed). */
    fun isValidProxyDomainFormat(domain: String): Boolean =
        InstagramProxyStore.isValidProxyDomainFormat(domain)

    /**
     * Domains the app already routes specially — allowing them as a custom TikTok
     * proxy would corrupt platform detection. Because all detection is
     * substring-based, both containment directions are rejected.
     */
    fun isReservedDomain(domain: String): Boolean {
        val reserved = listOf(
            Constants.TIKTOK_DOMAIN,
            Constants.INSTAGRAM_DOMAIN,
            Constants.TWITTER_DOMAIN,
            Constants.X_DOMAIN,
            Constants.FACEBOOK_DOMAIN,
            Constants.FB_SHORT_DOMAIN,
            Constants.FACEBOOKEZ_DOMAIN
        ) + Constants.TWITTER_PROXY_DOMAINS +
            Constants.INSTAGRAM_PROXY_DOMAINS +
            Constants.INSTAGRAM_LEGACY_PROXIES +
            Constants.TIKTOK_PROXY_DOMAINS +
            Constants.TIKTOK_LEGACY_PROXIES
        return reserved.any { domain.contains(it) || it.contains(domain) }
    }

    /** True when [domain] is already present among custom proxies. */
    fun isDuplicate(domain: String): Boolean = domain in customProxies
}
