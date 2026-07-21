// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
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

package com.fixupxer.processing

import com.fixupxer.cleaners.CleanerRegistry
import com.fixupxer.cleaners.impl.FacebookCleaner
import com.fixupxer.cleaners.impl.InstagramCleaner
import com.fixupxer.cleaners.impl.TikTokCleaner
import com.fixupxer.cleaners.impl.TwitterCleaner
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.TikTokProxyStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.IDN

class HostBoundaryRegressionTest {

    @Before
    fun setUp() {
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
    }

    @After
    fun tearDown() {
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
    }

    @Test
    fun `instagram matching requires a host-label boundary`() {
        assertFalse(InstagramCleaner.matches("https://notinstagram.com/p/x"))
        assertFalse(UrlNormalizer.urlMatchesDomain("https://notinstagram.com/p/x", Constants.INSTAGRAM_DOMAIN))
        assertTrue(InstagramCleaner.matches("https://instagram.com/p/x"))
        assertTrue(InstagramCleaner.matches("https://www.instagram.com/p/x"))
        assertTrue(InstagramCleaner.matches("https://sub.instagram.com/p/x"))
    }

    @Test
    fun `twitter matching does not treat x com as a substring`() {
        assertFalse(TwitterCleaner.matches("https://examplex.com/post"))
        assertTrue(TwitterCleaner.matches("https://x.com/post"))
        assertTrue(TwitterCleaner.matches("https://www.x.com/post"))
    }

    @Test
    fun `farside matches twitter only on nitter path`() {
        val registry = CleanerRegistry().apply { register(TwitterCleaner) }

        assertFalse(TwitterCleaner.matches("https://farside.link/other/page?s=20"))
        assertTrue(TwitterCleaner.matches("https://farside.link/nitter/user/status/1?s=20"))
        assertFalse(registry.getCleanersFor("https://farside.link/other/page?s=20").contains(TwitterCleaner))
        assertEquals(
            "https://farside.link/other/page?s=20",
            TwitterCleaner.clean("https://farside.link/other/page?s=20"),
        )
    }

    @Test
    fun `tiktok proxy hosts remain distinct from tiktok host`() {
        assertFalse(UrlNormalizer.urlMatchesDomain("https://kktiktok.com/video/1", Constants.TIKTOK_DOMAIN))
        assertTrue(TikTokCleaner.matches("https://kktiktok.com/video/1"))
        assertTrue(TikTokCleaner.matches("https://vm.tiktok.com/ZMabc"))
    }

    @Test
    fun `facebook proxy matching requires a host-label boundary and excludes retired facebookez`() {
        assertFalse(FacebookCleaner.matches("https://myfacebookez.com/page"))
        assertFalse(FacebookCleaner.matches("https://facebookez.com/page"))
        assertFalse(UrlNormalizer.urlMatchesDomain("https://myfacebookez.com/page", Constants.FACEBOOKEZ_DOMAIN))
        assertTrue(FacebookCleaner.matches("https://facebook.com/page"))
    }

    @Test
    fun `host extraction accepts ports userinfo and uppercase schemes`() {
        assertTrue(UrlNormalizer.urlMatchesDomain("https://instagram.com:8443/p/x", Constants.INSTAGRAM_DOMAIN))
        assertTrue(UrlNormalizer.urlMatchesDomain("HTTPS://WWW.INSTAGRAM.COM/p/x", Constants.INSTAGRAM_DOMAIN))
        assertTrue(
            UrlNormalizer.urlMatchesDomain(
                "https://user:pass@instagram.com/p/x",
                Constants.INSTAGRAM_DOMAIN
            )
        )
    }

    @Test
    fun `idn host extraction is safe and normalized`() {
        val host = UrlNormalizer.extractAsciiHost("https://пример.com/p/x")

        assertNotNull(host)
        assertEquals(IDN.toASCII("пример.com").lowercase(), host)
        assertFalse(UrlNormalizer.hostMatchesDomain(host, Constants.INSTAGRAM_DOMAIN))
    }

    @Test
    fun `domains in paths and queries are not treated as hosts`() {
        assertFalse(
            InstagramCleaner.matches(
                "https://example.com/?u=https%3A%2F%2Finstagram.com%2Fp%2Fx"
            )
        )
        assertFalse(InstagramCleaner.matches("https://example.com/instagram.com/foo"))
    }
}
