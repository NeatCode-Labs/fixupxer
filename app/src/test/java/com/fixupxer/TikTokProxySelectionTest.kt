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
import com.fixupxer.utils.Constants
import com.fixupxer.utils.TikTokProxyStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the TikTok proxy selection feature (v1.7.0).
 *
 * Active proxies: tnktok.com + tfxktok.com (primary), tiktokez.com + kktiktok.com (backup).
 * Legacy proxies (vxtiktok.com, tiktxk.com — both dead services) are still recognized
 * so old pasted/shared links keep working, but always convert to an active proxy.
 *
 * Verifies:
 *   1. Forward conversion tiktok.com -> any active proxy
 *   2. Host prefix (www., vm., vt., m.) is PRESERVED on conversion — TikTok short
 *      links live on subdomains and the proxies mirror them (unlike Instagram,
 *      where the prefix is stripped)
 *   3. Cross-proxy swaps between active and legacy domains
 *   4. Backward conversion: any known proxy -> tiktok.com
 *   5. isTikTokUrl recognizes the full known set
 */
class TikTokProxySelectionTest {

    private lateinit var processor: UrlProcessor

    @Before
    fun setup() {
        // TikTokProxyStore is global state — start every test from a clean slate.
        TikTokProxyStore.reset()
        val registry = CleanerRegistry().apply {
            registerAll(CleanerCatalog.createBuiltInCleaners())
        }
        val cache = CleanerCache()
        processor = UrlProcessor(CleanerService(registry, cache))
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
    // Forward: tiktok.com -> any active proxy
    // ---------------------------------------------------------------------

    @Test
    fun `tiktok_com converts to tnktok_com`() {
        assertEquals(
            "https://tnktok.com/@user/video/123",
            convert("https://tiktok.com/@user/video/123", Constants.TNKTOK_DOMAIN)
        )
    }

    @Test
    fun `tiktok_com converts to tfxktok_com`() {
        assertEquals(
            "https://tfxktok.com/@user/video/123",
            convert("https://tiktok.com/@user/video/123", Constants.TFXKTOK_DOMAIN)
        )
    }

    @Test
    fun `tiktok_com converts to tiktokez_com`() {
        assertEquals(
            "https://tiktokez.com/@user/video/123",
            convert("https://tiktok.com/@user/video/123", Constants.TIKTOKEZ_DOMAIN)
        )
    }

    @Test
    fun `tiktok_com converts to kktiktok_com`() {
        assertEquals(
            "https://kktiktok.com/@user/video/123",
            convert("https://tiktok.com/@user/video/123", Constants.KKTIKTOK_DOMAIN)
        )
    }

    // ---------------------------------------------------------------------
    // Host prefix preservation (vm., vt., m., www. short/mobile links)
    // ---------------------------------------------------------------------

    @Test
    fun `www_tiktok_com keeps www on conversion`() {
        assertEquals(
            "https://www.tnktok.com/@user/video/123",
            convert("https://www.tiktok.com/@user/video/123", Constants.TNKTOK_DOMAIN)
        )
    }

    @Test
    fun `vm_tiktok_com short link keeps vm prefix`() {
        assertEquals(
            "https://vm.tnktok.com/ZMabcdef/",
            convert("https://vm.tiktok.com/ZMabcdef/", Constants.TNKTOK_DOMAIN)
        )
    }

    @Test
    fun `vt_tiktok_com short link keeps vt prefix`() {
        assertEquals(
            "https://vt.tiktokez.com/ZSabcdef/",
            convert("https://vt.tiktok.com/ZSabcdef/", Constants.TIKTOKEZ_DOMAIN)
        )
    }

    @Test
    fun `m_tiktok_com mobile link keeps m prefix`() {
        assertEquals(
            "https://m.kktiktok.com/v/123.html",
            convert("https://m.tiktok.com/v/123.html", Constants.KKTIKTOK_DOMAIN)
        )
    }

    // ---------------------------------------------------------------------
    // Cross-proxy swaps among active proxies
    // ---------------------------------------------------------------------

    @Test
    fun `tnktok_com converts to tfxktok_com`() {
        assertEquals(
            "https://tfxktok.com/@user/video/123",
            convert("https://tnktok.com/@user/video/123", Constants.TFXKTOK_DOMAIN)
        )
    }

    @Test
    fun `kktiktok_com converts to tnktok_com`() {
        assertEquals(
            "https://tnktok.com/@user/video/123",
            convert("https://kktiktok.com/@user/video/123", Constants.TNKTOK_DOMAIN)
        )
    }

    @Test
    fun `tiktokez_com converts to kktiktok_com`() {
        assertEquals(
            "https://kktiktok.com/@user/video/123",
            convert("https://tiktokez.com/@user/video/123", Constants.KKTIKTOK_DOMAIN)
        )
    }

    // ---------------------------------------------------------------------
    // Legacy proxies (vxtiktok.com, tiktxk.com) auto-migrate
    // ---------------------------------------------------------------------

    @Test
    fun `legacy vxtiktok converts to tnktok_com`() {
        assertEquals(
            "https://tnktok.com/@user/video/123",
            convert("https://vxtiktok.com/@user/video/123", Constants.TNKTOK_DOMAIN)
        )
    }

    @Test
    fun `legacy vm_vxtiktok keeps vm prefix and converts`() {
        assertEquals(
            "https://vm.tnktok.com/ZMabcdef/",
            convert("https://vm.vxtiktok.com/ZMabcdef/", Constants.TNKTOK_DOMAIN)
        )
    }

    @Test
    fun `legacy tiktxk converts to tfxktok_com`() {
        assertEquals(
            "https://tfxktok.com/@user/video/123",
            convert("https://tiktxk.com/@user/video/123", Constants.TFXKTOK_DOMAIN)
        )
    }

    // ---------------------------------------------------------------------
    // No-op: proxy already matches target
    // ---------------------------------------------------------------------

    @Test
    fun `tnktok_com unchanged when target=tnktok`() {
        val url = "https://tnktok.com/@user/video/123"
        assertEquals(url, convert(url, Constants.TNKTOK_DOMAIN))
    }

    @Test
    fun `vm_tnktok_com unchanged when target=tnktok`() {
        val url = "https://vm.tnktok.com/ZMabcdef/"
        assertEquals(url, convert(url, Constants.TNKTOK_DOMAIN))
    }

    @Test
    fun `kktiktok_com unchanged when target=kktiktok`() {
        val url = "https://kktiktok.com/@user/video/123"
        assertEquals(url, convert(url, Constants.KKTIKTOK_DOMAIN))
    }

    // ---------------------------------------------------------------------
    // Backward: any proxy -> tiktok.com (Embed toggle OFF)
    // ---------------------------------------------------------------------

    @Test
    fun `tnktok_com reverts to tiktok_com when convertOff`() {
        assertEquals(
            "https://tiktok.com/@user/video/123",
            convert("https://tnktok.com/@user/video/123", Constants.TNKTOK_DOMAIN, convertOn = false)
        )
    }

    @Test
    fun `vm_tnktok_com reverts to vm_tiktok_com when convertOff`() {
        assertEquals(
            "https://vm.tiktok.com/ZMabcdef/",
            convert("https://vm.tnktok.com/ZMabcdef/", Constants.TNKTOK_DOMAIN, convertOn = false)
        )
    }

    @Test
    fun `kktiktok_com reverts to tiktok_com when convertOff`() {
        assertEquals(
            "https://tiktok.com/@user/video/123",
            convert("https://kktiktok.com/@user/video/123", Constants.TNKTOK_DOMAIN, convertOn = false)
        )
    }

    @Test
    fun `legacy vxtiktok reverts to tiktok_com when convertOff`() {
        assertEquals(
            "https://tiktok.com/@user/video/123",
            convert("https://vxtiktok.com/@user/video/123", Constants.TNKTOK_DOMAIN, convertOn = false)
        )
    }

    @Test
    fun `plain tiktok_com unchanged when convertOff`() {
        val url = "https://www.tiktok.com/@user/video/123"
        assertEquals(url, convert(url, Constants.TNKTOK_DOMAIN, convertOn = false))
    }

    // ---------------------------------------------------------------------
    // Dirty URL cleaning + proxy conversion
    // ---------------------------------------------------------------------

    @Test
    fun `dirty tiktok url cleans tracking and converts to tnktok`() {
        val result = processor.processUrl(
            "https://www.tiktok.com/@user/video/123?is_from_webapp=1&sender_device=pc&_r=1&_t=abc",
            cleanTracking = true,
            convertTwitter = true,
            tiktokProxy = Constants.TNKTOK_DOMAIN
        ).first
        assertEquals(
            "https://www.tnktok.com/@user/video/123?is_from_webapp=1&sender_device=pc",
            result
        )
    }

    // ---------------------------------------------------------------------
    // isTikTokUrl covers active + legacy
    // ---------------------------------------------------------------------

    @Test
    fun `isTikTokUrl recognizes active and legacy proxies and tiktok_com`() {
        assertTrue(processor.isTikTokUrl("https://tiktok.com/@user/video/123"))
        assertTrue(processor.isTikTokUrl("https://www.tiktok.com/@user/video/123"))
        assertTrue(processor.isTikTokUrl("https://vm.tiktok.com/ZMabcdef/"))
        assertTrue(processor.isTikTokUrl("https://tnktok.com/@user/video/123"))
        assertTrue(processor.isTikTokUrl("https://tfxktok.com/@user/video/123"))
        assertTrue(processor.isTikTokUrl("https://tiktokez.com/@user/video/123"))
        assertTrue(processor.isTikTokUrl("https://kktiktok.com/@user/video/123"))
        // legacy still detected so the toggle still triggers
        assertTrue(processor.isTikTokUrl("https://vxtiktok.com/@user/video/123"))
        assertTrue(processor.isTikTokUrl("https://tiktxk.com/@user/video/123"))
        assertFalse(processor.isTikTokUrl("https://example.com/@user/video/123"))
    }
}
