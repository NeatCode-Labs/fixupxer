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
import com.fixupxer.cleaners.impl.InstagramCleaner
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InstagramProxyStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the user-defined custom Instagram proxy feature (v1.6.0):
 * InstagramProxyStore state, input normalization/validation and
 * UrlProcessor conversions involving custom proxy domains.
 */
class CustomInstagramProxyTest {

    private lateinit var processor: UrlProcessor

    private val customProxy = "myproxy.example.org"

    @Before
    fun setup() {
        // InstagramProxyStore is global state — start every test from a clean slate.
        InstagramProxyStore.reset()
        val registry = CleanerRegistry().apply {
            registerAll(CleanerCatalog.createBuiltInCleaners())
        }
        processor = UrlProcessor(CleanerService(registry, CleanerCache()))
    }

    @After
    fun tearDown() {
        InstagramProxyStore.reset()
    }

    private fun convert(url: String, target: String, convertOn: Boolean = true) =
        processor.processUrl(
            url,
            cleanTracking = false,
            convertTwitter = convertOn,
            instagramProxy = target
        ).first

    // ---------------------------------------------------------------------
    // Store state
    // ---------------------------------------------------------------------

    @Test
    fun `store starts empty and reset clears custom proxies`() {
        assertTrue(InstagramProxyStore.getCustomProxies().isEmpty())
        InstagramProxyStore.setCustomProxies(listOf(customProxy))
        assertEquals(listOf(customProxy), InstagramProxyStore.getCustomProxies())
        InstagramProxyStore.reset()
        assertTrue(InstagramProxyStore.getCustomProxies().isEmpty())
    }

    @Test
    fun `activeProxies is fixed roster plus custom entries`() {
        InstagramProxyStore.setCustomProxies(listOf(customProxy))
        val active = InstagramProxyStore.activeProxies()
        assertEquals(Constants.INSTAGRAM_PROXY_DOMAINS + customProxy, active)
    }

    @Test
    fun `allKnownProxies additionally includes legacy proxies`() {
        InstagramProxyStore.setCustomProxies(listOf(customProxy))
        val known = InstagramProxyStore.allKnownProxies()
        assertTrue(known.contains(customProxy))
        assertTrue(Constants.INSTAGRAM_LEGACY_PROXIES.all { known.contains(it) })
    }

    // ---------------------------------------------------------------------
    // Input normalization
    // ---------------------------------------------------------------------

    @Test
    fun `normalization strips protocol www path query and fragment`() {
        assertEquals("myproxy.org", InstagramProxyStore.normalizeCustomProxyInput("https://www.myproxy.org/p/abc?x=1#frag"))
        assertEquals("myproxy.org", InstagramProxyStore.normalizeCustomProxyInput("http://myproxy.org/"))
        assertEquals("myproxy.org", InstagramProxyStore.normalizeCustomProxyInput("  MyProxy.ORG  "))
        assertEquals("sub.myproxy.org", InstagramProxyStore.normalizeCustomProxyInput("sub.myproxy.org"))
    }

    // ---------------------------------------------------------------------
    // Format validation
    // ---------------------------------------------------------------------

    @Test
    fun `valid domain formats are accepted`() {
        assertTrue(InstagramProxyStore.isValidProxyDomainFormat("myproxy.org"))
        assertTrue(InstagramProxyStore.isValidProxyDomainFormat("sub.myproxy.org"))
        assertTrue(InstagramProxyStore.isValidProxyDomainFormat("my-proxy.co.uk"))
        assertTrue(InstagramProxyStore.isValidProxyDomainFormat("proxy123.io"))
    }

    @Test
    fun `invalid domain formats are rejected`() {
        assertFalse(InstagramProxyStore.isValidProxyDomainFormat(""))
        assertFalse(InstagramProxyStore.isValidProxyDomainFormat("no-tld"))
        assertFalse(InstagramProxyStore.isValidProxyDomainFormat("has space.com"))
        assertFalse(InstagramProxyStore.isValidProxyDomainFormat("-leadingdash.com"))
        assertFalse(InstagramProxyStore.isValidProxyDomainFormat("trailing.com-"))
        assertFalse(InstagramProxyStore.isValidProxyDomainFormat("double..dot.com"))
        assertFalse(InstagramProxyStore.isValidProxyDomainFormat("bad_char.com"))
        assertFalse(InstagramProxyStore.isValidProxyDomainFormat("regex*.com"))
        // Over the 253-char hostname limit
        val tooLong = "a".repeat(60) + "." + "b".repeat(60) + "." + "c".repeat(60) + "." +
            "d".repeat(60) + "." + "e".repeat(20) + ".com"
        assertFalse(InstagramProxyStore.isValidProxyDomainFormat(tooLong))
    }

    // ---------------------------------------------------------------------
    // Reserved-domain rejection
    // ---------------------------------------------------------------------

    @Test
    fun `domains the app already routes are reserved`() {
        assertTrue(InstagramProxyStore.isReservedDomain(Constants.INSTAGRAM_DOMAIN))
        assertTrue(InstagramProxyStore.isReservedDomain(Constants.TOINSTAGRAM_DOMAIN))
        assertTrue(InstagramProxyStore.isReservedDomain(Constants.KKINSTAGRAM_DOMAIN))
        assertTrue(InstagramProxyStore.isReservedDomain("eeinstagram.com"))
        assertTrue(InstagramProxyStore.isReservedDomain(Constants.FIXUPX_DOMAIN))
        assertTrue(InstagramProxyStore.isReservedDomain(Constants.VXTWITTER_DOMAIN))
        assertTrue(InstagramProxyStore.isReservedDomain(Constants.X_DOMAIN))
        assertTrue(InstagramProxyStore.isReservedDomain(Constants.FACEBOOK_DOMAIN))
        assertTrue(InstagramProxyStore.isReservedDomain(Constants.FB_SHORT_DOMAIN))
        assertTrue(InstagramProxyStore.isReservedDomain(Constants.FACEBOOKEZ_DOMAIN))
        // Substring collisions in either direction are also rejected because
        // all platform detection is substring-based.
        assertTrue(InstagramProxyStore.isReservedDomain("my.fixupx.com"))
        assertTrue(InstagramProxyStore.isReservedDomain("prefix-instagram.com"))
    }

    @Test
    fun `unrelated domains are not reserved`() {
        assertFalse(InstagramProxyStore.isReservedDomain("myproxy.example.org"))
        assertFalse(InstagramProxyStore.isReservedDomain("igproxy.net"))
    }

    // ---------------------------------------------------------------------
    // Duplicate detection
    // ---------------------------------------------------------------------

    @Test
    fun `duplicate detection matches existing custom proxies only`() {
        assertFalse(InstagramProxyStore.isDuplicate(customProxy))
        InstagramProxyStore.setCustomProxies(listOf(customProxy))
        assertTrue(InstagramProxyStore.isDuplicate(customProxy))
        assertFalse(InstagramProxyStore.isDuplicate("other.example.org"))
    }

    // ---------------------------------------------------------------------
    // Conversions with a custom proxy
    // ---------------------------------------------------------------------

    @Test
    fun `instagram_com converts to custom proxy`() {
        InstagramProxyStore.setCustomProxies(listOf(customProxy))
        assertEquals(
            "https://$customProxy/p/abc",
            convert("https://instagram.com/p/abc", customProxy)
        )
    }

    @Test
    fun `www_instagram_com converts to bare custom proxy`() {
        InstagramProxyStore.setCustomProxies(listOf(customProxy))
        assertEquals(
            "https://$customProxy/reel/xyz",
            convert("https://www.instagram.com/reel/xyz", customProxy)
        )
    }

    @Test
    fun `custom proxy url is detected and reverts to instagram_com when convertOff`() {
        InstagramProxyStore.setCustomProxies(listOf(customProxy))
        assertEquals(
            "https://instagram.com/p/abc",
            convert("https://$customProxy/p/abc", Constants.TOINSTAGRAM_DOMAIN, convertOn = false)
        )
    }

    @Test
    fun `custom proxy url swaps to selected fixed proxy`() {
        InstagramProxyStore.setCustomProxies(listOf(customProxy))
        assertEquals(
            "https://toinstagram.com/p/abc",
            convert("https://$customProxy/p/abc", Constants.TOINSTAGRAM_DOMAIN)
        )
    }

    @Test
    fun `fixed proxy url swaps to selected custom proxy`() {
        InstagramProxyStore.setCustomProxies(listOf(customProxy))
        assertEquals(
            "https://$customProxy/p/abc",
            convert("https://toinstagram.com/p/abc", customProxy)
        )
    }

    @Test
    fun `custom proxy url unchanged when it is the selected target`() {
        InstagramProxyStore.setCustomProxies(listOf(customProxy))
        val url = "https://$customProxy/p/abc"
        assertEquals(url, convert(url, customProxy))
    }

    // ---------------------------------------------------------------------
    // Detection
    // ---------------------------------------------------------------------

    @Test
    fun `isInstagramUrl recognizes custom proxy only while registered`() {
        assertFalse(processor.isInstagramUrl("https://$customProxy/p/abc"))
        InstagramProxyStore.setCustomProxies(listOf(customProxy))
        assertTrue(processor.isInstagramUrl("https://$customProxy/p/abc"))
        InstagramProxyStore.reset()
        assertFalse(processor.isInstagramUrl("https://$customProxy/p/abc"))
    }

    @Test
    fun `InstagramCleaner matches custom proxy and strips igshid`() {
        InstagramProxyStore.setCustomProxies(listOf(customProxy))
        assertTrue(InstagramCleaner.matches("https://$customProxy/p/abc"))
        assertEquals(
            "https://$customProxy/p/abc",
            InstagramCleaner.clean("https://$customProxy/p/abc?igshid=xyz")
        )
    }
}
