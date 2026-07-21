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

package com.fixupxer

import com.fixupxer.cleaners.CleanerCatalog
import com.fixupxer.cleaners.CleanerRegistry
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.processing.PlatformDomainConverter
import com.fixupxer.processing.ProxySelections
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.NativeAppMapping
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MultiPlatformConversionTest {

    private lateinit var processor: UrlProcessor

    @Before
    fun setup() {
        InstagramProxyStore.reset()
        ProxyRoster.reset()
        val registry = CleanerRegistry().apply { registerAll(CleanerCatalog.createBuiltInCleaners()) }
        processor = UrlProcessor(CleanerService(registry, CleanerCache()))
    }

    @After
    fun tearDown() {
        InstagramProxyStore.reset()
        ProxyRoster.reset()
    }

    private fun convert(
        url: String,
        enabled: Boolean,
        selections: ProxySelections = ProxySelections.DEFAULT,
    ) = processor.applyDomainConversions(url, enabled, selections)

    private fun selections(vararg pairs: Pair<ProxyPlatform, String?>): ProxySelections {
        val map = ProxySelections.DEFAULT.byPlatform.toMutableMap()
        pairs.forEach { (platform, domain) -> map[platform] = domain }
        return ProxySelections(map)
    }

    @Test
    fun `fragment pseudo query is not promoted to real query on x reader forward`() {
        val input = "https://x.com/user/status/1#client?access_token=abcdefghijkl"
        val readerSel = selections(ProxyPlatform.X to Constants.XCANCEL_DOMAIN)
        assertEquals(
            "https://xcancel.com/user/status/1#client?access_token=abcdefghijkl",
            convert(input, true, readerSel),
        )
    }

    @Test
    fun `fragment pseudo query is not promoted to real query on x reverse`() {
        val input = "https://xcancel.com/user/status/1#client?access_token=abcdefghijkl"
        val readerSel = selections(ProxyPlatform.X to Constants.XCANCEL_DOMAIN)
        assertEquals(
            "https://x.com/user/status/1#client?access_token=abcdefghijkl",
            convert(input, false, readerSel),
        )
    }

    @Test
    fun `fixupx conversion preserves fragment pseudo query from reader input`() {
        val input = "https://xcancel.com/user/status/123#frag?token=abc"
        assertEquals(
            "https://fixupx.com/user/status/123#frag?token=abc",
            convert(input, true),
        )
    }

    @Test
    fun `x reader conversion preserves real query before fragment pseudo query`() {
        val input = "https://x.com/user/status/1?s=20#client?access_token=abc"
        val readerSel = selections(ProxyPlatform.X to Constants.XCANCEL_DOMAIN)
        assertEquals(
            "https://xcancel.com/user/status/1?s=20#client?access_token=abc",
            convert(input, true, readerSel),
        )
    }

    @Test
    fun `x reader conversion preserves trailing fragment marker`() {
        val input = "https://x.com/user/status/1#"
        val readerSel = selections(ProxyPlatform.X to Constants.XCANCEL_DOMAIN)
        assertEquals(
            "https://xcancel.com/user/status/1#",
            convert(input, true, readerSel),
        )
    }

    @Test
    fun `fragment pseudo query is not promoted to real query on reddit host swap`() {
        val input = "https://www.reddit.com/r/test/comments/1#client?access_token=abcdefghijkl"
        val redditSel = selections(ProxyPlatform.REDDIT to Constants.REDLIB_CATSARCH_DOMAIN)
        assertEquals(
            "https://redlib.catsarch.com/r/test/comments/1#client?access_token=abcdefghijkl",
            convert(input, true, redditSel),
        )
    }

    @Test
    fun `fragment pseudo query is not promoted on youtu be conversion`() {
        val input = "https://youtu.be/dQw4w9WgXcQ#t?access_token=abcdefghijkl"
        val ytSel = selections(ProxyPlatform.YOUTUBE to Constants.INV_NADEKO_DOMAIN)
        val result = convert(input, true, ytSel)
        assertEquals(
            "https://inv.nadeko.net/watch?v=dQw4w9WgXcQ#t?access_token=abcdefghijkl",
            result,
        )
        assertFalse(result.substringBefore('#').contains("?access_token"))
    }

    @Test
    fun `facebook custom target converts facebook_com source`() {
        val customDomain = "custom-facebook.example"
        ProxyRoster.setCustomProxies(ProxyPlatform.FACEBOOK, listOf(customDomain))
        val customSel = selections(ProxyPlatform.FACEBOOK to customDomain)
        assertEquals(
            "https://custom-facebook.example/post/1",
            convert("https://facebook.com/post/1", true, customSel),
        )
    }

    @Test
    fun `facebookez_com is no longer detected or reverse-converted`() {
        val customDomain = "custom-facebook.example"
        ProxyRoster.setCustomProxies(ProxyPlatform.FACEBOOK, listOf(customDomain))
        val customSel = selections(ProxyPlatform.FACEBOOK to customDomain)
        val facebookez = "https://facebookez.com/post/1"
        assertFalse(processor.isFacebookUrl(facebookez))
        assertEquals(facebookez, convert(facebookez, true, customSel))
        assertEquals(facebookez, convert(facebookez, false, customSel))
    }

    @Test
    fun `facebook with no configured target leaves facebook_com unchanged`() {
        val url = "https://facebook.com/post/1"
        assertEquals(url, convert(url, true))
    }

    @Test
    fun `facebook retarget preserves query and fragment pseudo query`() {
        val customDomain = "custom-facebook.example"
        ProxyRoster.setCustomProxies(ProxyPlatform.FACEBOOK, listOf(customDomain))
        val customSel = selections(ProxyPlatform.FACEBOOK to customDomain)
        assertEquals(
            "https://custom-facebook.example/post/1?a=1#frag?tok=x",
            convert("https://facebook.com/post/1?a=1#frag?tok=x", true, customSel),
        )
    }

    @Test
    fun `facebook custom retargets to another custom frontend`() {
        val customDomain = "custom-facebook.example"
        val otherCustom = "other-facebook.example"
        ProxyRoster.setCustomProxies(ProxyPlatform.FACEBOOK, listOf(customDomain, otherCustom))
        val otherSel = selections(ProxyPlatform.FACEBOOK to otherCustom)
        assertEquals(
            "https://other-facebook.example/post/1",
            convert("https://custom-facebook.example/post/1", true, otherSel),
        )
    }

    @Test
    fun `facebook already on selected target is unchanged`() {
        val customDomain = "custom-facebook.example"
        ProxyRoster.setCustomProxies(ProxyPlatform.FACEBOOK, listOf(customDomain))
        val customSel = selections(ProxyPlatform.FACEBOOK to customDomain)
        val url = "https://custom-facebook.example/post/1"
        assertEquals(url, convert(url, true, customSel))
    }

    @Test
    fun `facebook lookalike host is not retargeted`() {
        val customDomain = "custom-facebook.example"
        ProxyRoster.setCustomProxies(ProxyPlatform.FACEBOOK, listOf(customDomain))
        val customSel = selections(ProxyPlatform.FACEBOOK to customDomain)
        val lookalike = "https://myfacebookez.com/page"
        assertEquals(lookalike, convert(lookalike, true, customSel))
    }

    @Test
    fun `facebook custom reverse works with conversion off`() {
        val customDomain = "custom-facebook.example"
        ProxyRoster.setCustomProxies(ProxyPlatform.FACEBOOK, listOf(customDomain))
        val customSel = selections(ProxyPlatform.FACEBOOK to customDomain)
        assertEquals(
            "https://facebook.com/post/1",
            convert("https://custom-facebook.example/post/1", false, customSel),
        )
    }

    @Test
    fun `farside forward and reverse preserve path query fragment`() {
        val input = "https://x.com/user/status/1?s=20#frag"
        val farsideSel = selections(ProxyPlatform.X to Constants.FARSIDE_DOMAIN)
        assertEquals(
            "https://farside.link/nitter/user/status/1?s=20#frag",
            convert(input, true, farsideSel),
        )
        assertEquals(
            "https://x.com/user/status/1?s=20#frag",
            convert("https://farside.link/nitter/user/status/1?s=20#frag", false, farsideSel),
        )
    }

    @Test
    fun `farside reverse preserves fragment pseudo query`() {
        val farsideSel = selections(ProxyPlatform.X to Constants.FARSIDE_DOMAIN)
        assertEquals(
            "https://x.com/user/status/1#f?token=abc",
            convert("https://farside.link/nitter/user/status/1#f?token=abc", false, farsideSel),
        )
    }

    @Test
    fun `farside without nitter prefix is untouched`() {
        val url = "https://farside.link/other/page"
        assertFalse(processor.isTwitterUrl(url))
        assertEquals(url, convert(url, true))
        assertEquals(url, convert(url, false))
    }

    @Test
    fun `xcancel reader converts profile paths fixupx does not`() {
        val profile = "https://x.com/elonmusk"
        val readerSel = selections(ProxyPlatform.X to Constants.XCANCEL_DOMAIN)
        assertEquals(profile, convert(profile, true))
        assertEquals("https://xcancel.com/elonmusk", convert(profile, true, readerSel))
    }

    @Test
    fun `fixupx selected converts reader status input`() {
        assertEquals(
            "https://fixupx.com/user/status/123",
            convert("https://xcancel.com/user/status/123", true),
        )
    }

    @Test
    fun `reader selected converts fxtwitter input`() {
        val readerSel = selections(ProxyPlatform.X to Constants.XCANCEL_DOMAIN)
        assertEquals(
            "https://xcancel.com/user/status/123",
            convert("https://fxtwitter.com/user/status/123", true, readerSel),
        )
    }

    @Test
    fun `xcancel selection converts twitter status and fixupx proxy swap`() {
        val readerSel = selections(ProxyPlatform.X to Constants.XCANCEL_DOMAIN)
        assertEquals(
            "https://xcancel.com/user/status/123",
            convert("https://twitter.com/user/status/123", true, readerSel),
        )
        assertEquals(
            "https://xcancel.com/user/status/123",
            convert("https://fixupx.com/user/status/123", true, readerSel),
        )
    }

    @Test
    fun `kittygram forward reverse and detection`() {
        val kittySel = selections(ProxyPlatform.INSTAGRAM to Constants.KITTYGRAM_DOMAIN)
        assertEquals(
            "https://kittygr.am/p/abc",
            convert("https://instagram.com/p/abc", true, kittySel),
        )
        assertEquals(
            "https://instagram.com/p/abc",
            convert("https://kittygr.am/p/abc", false, kittySel),
        )
        assertTrue(processor.isInstagramUrl("https://kittygr.am/p/abc"))
    }

    @Test
    fun `empty instagram selection is forward no-op`() {
        val noIg = selections(ProxyPlatform.INSTAGRAM to "")
        assertEquals(
            "https://instagram.com/p/abc",
            convert("https://instagram.com/p/abc", true, noIg),
        )
    }

    @Test
    fun `skylib converts bluesky profile paths`() {
        val profile = "https://bsky.app/profile/alice.bsky.social"
        val skylibSel = selections(ProxyPlatform.BLUESKY to Constants.SKYLIB_COFFEE_DOMAIN)
        assertEquals(
            "https://skylib.coffee/profile/alice.bsky.social",
            convert(profile, true, skylibSel),
        )
        assertEquals(profile, convert(profile, true))
    }

    @Test
    fun `reddit forward reverse and old prefix collapse`() {
        val redditSel = selections(ProxyPlatform.REDDIT to Constants.REDLIB_CATSARCH_DOMAIN)
        assertEquals(
            "https://redlib.catsarch.com/r/test/comments/abc",
            convert("https://old.reddit.com/r/test/comments/abc", true, redditSel),
        )
        assertEquals(
            "https://reddit.com/r/test/comments/abc",
            convert("https://redlib.catsarch.com/r/test/comments/abc", false, redditSel),
        )
    }

    @Test
    fun `redd it is not detected for conversion routing`() {
        val url = "https://redd.it/abc123"
        assertFalse(processor.isRedditUrl(url))
    }

    @Test
    fun `reddit redirect subdomain is not converted or classified as reddit`() {
        val url = "https://out.reddit.com/t3_abc?url=https%3A%2F%2Fexample.com"
        val redditSel = selections(ProxyPlatform.REDDIT to Constants.REDLIB_CATSARCH_DOMAIN)

        assertFalse(processor.isRedditUrl(url))
        assertEquals(url, convert(url, true, redditSel))
    }

    @Test
    fun `youtube watch forward youtu be canonicalization and music untouched`() {
        val ytSel = selections(ProxyPlatform.YOUTUBE to Constants.INV_NADEKO_DOMAIN)
        assertEquals(
            "https://inv.nadeko.net/watch?v=dQw4w9WgXcQ&t=30",
            convert("https://youtu.be/dQw4w9WgXcQ?t=30", true, ytSel),
        )
        val music = "https://music.youtube.com/watch?v=abc"
        assertFalse(processor.isYouTubeUrl(music))
        assertEquals(music, convert(music, true, ytSel))
    }

    @Test
    fun `youtube conversion rejects false path prefixes and multi-segment short links`() {
        val ytSel = selections(ProxyPlatform.YOUTUBE to Constants.INV_NADEKO_DOMAIN)
        val unsupported = listOf(
            "https://youtube.com/watchanything?v=abc",
            "https://youtube.com/playlistfoo?list=abc",
            "https://youtube.com/shorts/",
            "https://youtube.com/live/",
            "https://youtube.com/channel/",
            "https://youtube.com/@",
            "https://youtu.be/abc/extra",
        )

        unsupported.forEach { url ->
            assertFalse(url, processor.isYouTubeUrl(url))
            assertEquals(url, convert(url, true, ytSel))
        }
    }

    @Test
    fun `pinterest pin path only`() {
        val pinSel = selections(ProxyPlatform.PINTEREST to Constants.PINTEREST_BUNK_DOMAIN)
        val pin = "https://pinterest.com/pin/12345?utm=1"
        val home = "https://pinterest.com/user/board/"
        assertEquals("https://pinterest.bunk.im/pin/12345?utm=1", convert(pin, true, pinSel))
        assertEquals(home, convert(home, true, pinSel))
        assertFalse(processor.isPinterestUrl("https://pin.it/abc"))
    }

    @Test
    fun `threads net and com convert`() {
        val thSel = selections(ProxyPlatform.THREADS to Constants.SHOELACE_MINT_DOMAIN)
        assertEquals(
            "https://shoelace.mint.lgbt/@user/post/abc",
            convert("https://threads.net/@user/post/abc", true, thSel),
        )
        assertEquals(
            "https://threads.com/@user",
            convert("https://shoelace.mint.lgbt/@user", false, thSel),
        )
    }

    @Test
    fun `lookalike hosts do not false convert`() {
        val lookalike = "https://xcancel.com.evil.com/user/status/1"
        assertFalse(processor.isTwitterUrl(lookalike))
        assertEquals(lookalike, convert(lookalike, true))
    }

    @Test
    fun `reader frontends are excluded while source hosts map to native apps`() {
        assertTrue(NativeAppMapping.isReaderOnlyHost(Constants.SAFEREDDIT_DOMAIN))
        assertNull(
            NativeAppMapping.resolvePackage(
                "https://safereddit.com/r/test",
                Constants.SAFEREDDIT_DOMAIN,
            ).packageName,
        )
        assertNull(
            NativeAppMapping.resolvePackage(
                "https://kittygr.am/p/abc",
                Constants.KITTYGRAM_DOMAIN,
            ).packageName,
        )
        assertTrue(
            NativeAppMapping.packagesFor(
                "https://safereddit.com/r/test",
                Constants.SAFEREDDIT_DOMAIN,
            ).isEmpty(),
        )
        assertTrue(
            NativeAppMapping.packagesFor(
                "https://kittygr.am/p/abc",
                Constants.KITTYGRAM_DOMAIN,
            ).isEmpty(),
        )
        assertTrue(
            NativeAppMapping.packagesFor(
                "https://out.reddit.com/t3_abc",
                "out.${Constants.REDDIT_DOMAIN}",
            ).isEmpty(),
        )
        assertEquals(
            listOf("com.reddit.frontpage"),
            NativeAppMapping.packagesFor(
                "https://reddit.com/r/test",
                Constants.REDDIT_DOMAIN,
            ),
        )
        assertEquals(
            listOf("com.instagram.android"),
            NativeAppMapping.packagesFor(
                "https://instagram.com/p/abc",
                Constants.INSTAGRAM_DOMAIN,
            ),
        )
    }
}
