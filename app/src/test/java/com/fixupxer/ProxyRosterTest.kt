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

import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.Constants
import com.fixupxer.utils.FrontendRole
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import com.fixupxer.utils.TikTokProxyStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProxyRosterTest {

    private val customX = "myreader.example.org"
    private val customTikTok = "mytt.example.org"

    @Before
    fun setup() {
        ProxyRoster.reset()
    }

    @After
    fun tearDown() {
        ProxyRoster.reset()
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
    }

    @Test
    fun `activeTargets respects disabled built-ins`() {
        ProxyRoster.setDisabledBuiltIns(ProxyPlatform.INSTAGRAM, setOf("ig_toinstagram"))
        val active = ProxyRoster.activeTargets(ProxyPlatform.INSTAGRAM)
        assertFalse(active.any { it.id == "ig_toinstagram" })
        assertTrue(active.any { it.id == "ig_adamlikes" })
    }

    @Test
    fun `allKnownDomains includes disabled built-ins and legacy`() {
        ProxyRoster.setDisabledBuiltIns(ProxyPlatform.INSTAGRAM, setOf("ig_toinstagram"))
        val known = ProxyRoster.allKnownDomains(ProxyPlatform.INSTAGRAM)
        assertTrue(known.contains(Constants.TOINSTAGRAM_DOMAIN))
        assertTrue(Constants.INSTAGRAM_LEGACY_PROXIES.all { known.contains(it) })
    }

    @Test
    fun `custom targets append in stored order with reader role`() {
        ProxyRoster.setCustomProxies(ProxyPlatform.X, listOf("a.example.org", "b.example.org"))
        val customs = ProxyRoster.activeTargets(ProxyPlatform.X).filter { it.id.startsWith("custom:") }
        assertEquals(listOf("a.example.org", "b.example.org"), customs.map { it.domain })
        assertTrue(customs.all { it.role == FrontendRole.READER })
        assertTrue(customs.all { !it.allowNativeApp })
    }

    @Test
    fun `reset clears all platform state`() {
        ProxyRoster.setCustomProxies(ProxyPlatform.X, listOf(customX))
        ProxyRoster.setDisabledBuiltIns(ProxyPlatform.INSTAGRAM, setOf("ig_toinstagram"))
        ProxyRoster.reset()
        assertTrue(ProxyRoster.getCustomProxies(ProxyPlatform.X).isEmpty())
        assertTrue(ProxyRoster.getDisabledBuiltIns(ProxyPlatform.INSTAGRAM).isEmpty())
    }

    @Test
    fun `normalization strips protocol www path query and fragment`() {
        assertEquals(
            "myproxy.org",
            ProxyRoster.normalizeCustomProxyInput("https://www.myproxy.org/p/abc?x=1#frag"),
        )
    }

    @Test
    fun `invalid domain formats are rejected`() {
        assertFalse(ProxyRoster.isValidProxyDomainFormat("no-tld"))
        assertFalse(ProxyRoster.isValidProxyDomainFormat("has space.com"))
    }

    @Test
    fun `host-boundary reserved rejects known built-ins subdomains and parents`() {
        assertTrue(ProxyRoster.isReservedDomain(Constants.FXBSKY_DOMAIN))
        assertTrue(ProxyRoster.isReservedDomain(Constants.FARSIDE_DOMAIN))
        assertTrue(ProxyRoster.isReservedDomain("sub.fixupx.com"))
        assertTrue(ProxyRoster.isReservedDomain("catsarch.com"))
        assertTrue(ProxyRoster.isReservedDomain("my.tnktok.com"))
        assertTrue(ProxyRoster.isReservedDomain(Constants.FACEBOOKEZ_DOMAIN))
        assertTrue(ProxyRoster.isReservedDomain(Constants.KKINSTAGRAM_DOMAIN))
        assertTrue(ProxyRoster.isReservedDomain("sub.facebookez.com"))
        assertTrue(ProxyRoster.isReservedDomain("sub.kkinstagram.com"))
    }

    @Test
    fun `retired unsafe domains are reserved but not known`() {
        assertFalse(ProxyRoster.allKnownDomains(ProxyPlatform.INSTAGRAM).contains(Constants.KKINSTAGRAM_DOMAIN))
        assertFalse(ProxyRoster.allKnownDomains(ProxyPlatform.FACEBOOK).contains(Constants.FACEBOOKEZ_DOMAIN))
    }

    @Test
    fun `unrelated domains are not reserved`() {
        assertFalse(ProxyRoster.isReservedDomain("myproxy.example.org"))
        assertFalse(ProxyRoster.isReservedDomain("example.org"))
        // Host-boundary no longer rejects unrelated lookalike strings (safer than substring).
        assertFalse(ProxyRoster.isReservedDomain("prefix-instagram.com"))
    }

    @Test
    fun `cross-platform custom collision is reserved`() {
        ProxyRoster.setCustomProxies(ProxyPlatform.X, listOf(customX))
        assertTrue(ProxyRoster.isReservedDomain(customX))
    }

    @Test
    fun `duplicate detection is per platform`() {
        ProxyRoster.setCustomProxies(ProxyPlatform.X, listOf(customX))
        ProxyRoster.setCustomProxies(ProxyPlatform.TIKTOK, listOf(customTikTok))
        assertTrue(ProxyRoster.isDuplicate(ProxyPlatform.X, customX))
        assertFalse(ProxyRoster.isDuplicate(ProxyPlatform.TIKTOK, customX))
        assertTrue(ProxyRoster.isDuplicate(ProxyPlatform.TIKTOK, Constants.TNKTOK_DOMAIN))
    }

    @Test
    fun `allKnownDomainsAllPlatforms is union of every platform`() {
        ProxyRoster.setCustomProxies(ProxyPlatform.X, listOf(customX))
        val all = ProxyRoster.allKnownDomainsAllPlatforms()
        assertTrue(all.contains(customX))
        assertTrue(all.contains(Constants.FIXUPX_DOMAIN))
        assertTrue(all.contains(Constants.INV_NADEKO_DOMAIN))
    }

    @Test
    fun `instagram facade activeProxies respects disabled embed targets`() {
        ProxyRoster.setDisabledBuiltIns(ProxyPlatform.INSTAGRAM, setOf("ig_toinstagram"))
        val active = InstagramProxyStore.activeProxies()
        assertFalse(active.contains(Constants.TOINSTAGRAM_DOMAIN))
        assertTrue(active.contains(Constants.ADAMLIKES_DOMAIN))
    }

    @Test
    fun `instagram facade activeProxies includes experimental readers`() {
        val active = InstagramProxyStore.activeProxies()
        assertTrue(active.contains(Constants.KITTYGRAM_DOMAIN))
    }
}
