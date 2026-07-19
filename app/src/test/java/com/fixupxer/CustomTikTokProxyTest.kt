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

import com.fixupxer.cleaners.CleanerCatalog
import com.fixupxer.cleaners.CleanerRegistry
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.cleaners.impl.TikTokCleaner
import com.fixupxer.utils.Constants
import com.fixupxer.utils.TikTokProxyStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the user-defined custom TikTok proxy feature (v1.7.0):
 * TikTokProxyStore state, input normalization/validation and
 * UrlProcessor conversions involving custom proxy domains.
 */
class CustomTikTokProxyTest {

    private lateinit var processor: UrlProcessor

    private val customProxy = "myttproxy.example.org"

    @Before
    fun setup() {
        // TikTokProxyStore is global state — start every test from a clean slate.
        TikTokProxyStore.reset()
        val registry = CleanerRegistry().apply {
            registerAll(CleanerCatalog.createBuiltInCleaners())
        }
        processor = UrlProcessor(CleanerService(registry, CleanerCache()))
    }

    @After
    fun tearDown() {
        TikTokProxyStore.reset()
    }

    private fun convert(url: String, target: String, convertOn: Boolean = true) =
        processor.processUrl(
            url,
            cleanTracking = false,
            convertTwitter = convertOn,
            tiktokProxy = target
        ).first

    // ---------------------------------------------------------------------
    // Store state
    // ---------------------------------------------------------------------

    @Test
    fun `store starts empty and reset clears custom proxies`() {
        assertTrue(TikTokProxyStore.getCustomProxies().isEmpty())
        TikTokProxyStore.setCustomProxies(listOf(customProxy))
        assertEquals(listOf(customProxy), TikTokProxyStore.getCustomProxies())
        TikTokProxyStore.reset()
        assertTrue(TikTokProxyStore.getCustomProxies().isEmpty())
    }

    @Test
    fun `activeProxies is fixed roster plus custom entries`() {
        TikTokProxyStore.setCustomProxies(listOf(customProxy))
        val active = TikTokProxyStore.activeProxies()
        assertEquals(Constants.TIKTOK_PROXY_DOMAINS + customProxy, active)
    }

    @Test
    fun `allKnownProxies additionally includes legacy proxies`() {
        TikTokProxyStore.setCustomProxies(listOf(customProxy))
        val known = TikTokProxyStore.allKnownProxies()
        assertTrue(known.contains(customProxy))
        assertTrue(Constants.TIKTOK_LEGACY_PROXIES.all { known.contains(it) })
    }

    // ---------------------------------------------------------------------
    // Reserved-domain rejection
    // ---------------------------------------------------------------------

    @Test
    fun `domains the app already routes are reserved`() {
        assertTrue(TikTokProxyStore.isReservedDomain(Constants.TIKTOK_DOMAIN))
        assertTrue(TikTokProxyStore.isReservedDomain(Constants.TNKTOK_DOMAIN))
        assertTrue(TikTokProxyStore.isReservedDomain(Constants.TFXKTOK_DOMAIN))
        assertTrue(TikTokProxyStore.isReservedDomain(Constants.TIKTOKEZ_DOMAIN))
        assertTrue(TikTokProxyStore.isReservedDomain(Constants.KKTIKTOK_DOMAIN))
        assertTrue(TikTokProxyStore.isReservedDomain("vxtiktok.com"))
        assertTrue(TikTokProxyStore.isReservedDomain(Constants.INSTAGRAM_DOMAIN))
        assertTrue(TikTokProxyStore.isReservedDomain(Constants.TOINSTAGRAM_DOMAIN))
        assertTrue(TikTokProxyStore.isReservedDomain(Constants.FIXUPX_DOMAIN))
        assertTrue(TikTokProxyStore.isReservedDomain(Constants.X_DOMAIN))
        assertTrue(TikTokProxyStore.isReservedDomain(Constants.FACEBOOK_DOMAIN))
        // Subdomains of reserved hosts are rejected via host-boundary matching.
        assertTrue(TikTokProxyStore.isReservedDomain("my.tnktok.com"))
    }

    @Test
    fun `lookalike domains without host relationship are not reserved`() {
        assertFalse(TikTokProxyStore.isReservedDomain("prefix-tiktok.com"))
    }

    @Test
    fun `unrelated domains are not reserved`() {
        assertFalse(TikTokProxyStore.isReservedDomain("myttproxy.example.org"))
        assertFalse(TikTokProxyStore.isReservedDomain("ttproxy.net"))
    }

    // ---------------------------------------------------------------------
    // Duplicate detection
    // ---------------------------------------------------------------------

    @Test
    fun `duplicate detection matches existing custom proxies only`() {
        assertFalse(TikTokProxyStore.isDuplicate(customProxy))
        TikTokProxyStore.setCustomProxies(listOf(customProxy))
        assertTrue(TikTokProxyStore.isDuplicate(customProxy))
        assertFalse(TikTokProxyStore.isDuplicate("other.example.org"))
    }

    // ---------------------------------------------------------------------
    // The Instagram store must also treat TikTok domains as reserved (and vice
    // versa) so the two custom rosters can't hijack each other's detection.
    // ---------------------------------------------------------------------

    @Test
    fun `InstagramProxyStore treats TikTok domains as reserved`() {
        assertTrue(com.fixupxer.utils.InstagramProxyStore.isReservedDomain(Constants.TIKTOK_DOMAIN))
        assertTrue(com.fixupxer.utils.InstagramProxyStore.isReservedDomain(Constants.TNKTOK_DOMAIN))
        assertTrue(com.fixupxer.utils.InstagramProxyStore.isReservedDomain(Constants.KKTIKTOK_DOMAIN))
    }

    // ---------------------------------------------------------------------
    // Conversions with a custom proxy
    // ---------------------------------------------------------------------

    @Test
    fun `tiktok_com converts to custom proxy`() {
        TikTokProxyStore.setCustomProxies(listOf(customProxy))
        assertEquals(
            "https://$customProxy/@user/video/123",
            convert("https://tiktok.com/@user/video/123", customProxy)
        )
    }

    @Test
    fun `www_tiktok_com converts to custom proxy keeping www`() {
        TikTokProxyStore.setCustomProxies(listOf(customProxy))
        assertEquals(
            "https://www.$customProxy/@user/video/123",
            convert("https://www.tiktok.com/@user/video/123", customProxy)
        )
    }

    @Test
    fun `custom proxy url is detected and reverts to tiktok_com when convertOff`() {
        TikTokProxyStore.setCustomProxies(listOf(customProxy))
        assertEquals(
            "https://tiktok.com/@user/video/123",
            convert("https://$customProxy/@user/video/123", Constants.TNKTOK_DOMAIN, convertOn = false)
        )
    }

    @Test
    fun `custom proxy url swaps to selected fixed proxy`() {
        TikTokProxyStore.setCustomProxies(listOf(customProxy))
        assertEquals(
            "https://tnktok.com/@user/video/123",
            convert("https://$customProxy/@user/video/123", Constants.TNKTOK_DOMAIN)
        )
    }

    @Test
    fun `fixed proxy url swaps to selected custom proxy`() {
        TikTokProxyStore.setCustomProxies(listOf(customProxy))
        assertEquals(
            "https://$customProxy/@user/video/123",
            convert("https://tnktok.com/@user/video/123", customProxy)
        )
    }

    @Test
    fun `custom proxy url unchanged when it is the selected target`() {
        TikTokProxyStore.setCustomProxies(listOf(customProxy))
        val url = "https://$customProxy/@user/video/123"
        assertEquals(url, convert(url, customProxy))
    }

    // ---------------------------------------------------------------------
    // Detection
    // ---------------------------------------------------------------------

    @Test
    fun `isTikTokUrl recognizes custom proxy only while registered`() {
        assertFalse(processor.isTikTokUrl("https://$customProxy/@user/video/123"))
        TikTokProxyStore.setCustomProxies(listOf(customProxy))
        assertTrue(processor.isTikTokUrl("https://$customProxy/@user/video/123"))
        TikTokProxyStore.reset()
        assertFalse(processor.isTikTokUrl("https://$customProxy/@user/video/123"))
    }

    @Test
    fun `TikTokCleaner matches custom proxy and strips tracking params`() {
        TikTokProxyStore.setCustomProxies(listOf(customProxy))
        assertTrue(TikTokCleaner.matches("https://$customProxy/@user/video/123"))
        assertEquals(
            "https://$customProxy/@user/video/123",
            TikTokCleaner.clean("https://$customProxy/@user/video/123?_r=1&tt_from=share")
        )
    }
}
