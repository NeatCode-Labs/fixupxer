// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026 NeatCode Labs
 * This program is free software under the GNU General Public License,
 * version 3 or (at your option) any later version.
 */

package com.fixupxer.cleaners

import com.fixupxer.cleaners.impl.TwitterCleaner
import org.junit.Assert.assertEquals
import org.junit.Test

class TwitterCanonicalPermalinkTest {
    @Test
    fun `reported and future share parameters do not survive a first party permalink`() {
        val base = "https://x.com/Quinto_GQC/status/2115083116352327878"
        listOf(
            "?a=omnycujSk431o0Kde_8&s=19",
            "?future_random_key=opaque&another_key=2#opaque",
            "?a=first&a=second&enc%6Fded=value&A=upper",
            "#?random_key=opaque",
            "?",
            "#",
            ""
        ).forEach { suffix ->
            val result = TwitterCleaner.clean(base + suffix)
            assertEquals(suffix, base, result)
            assertEquals(suffix, result, TwitterCleaner.clean(result))
        }
    }

    @Test
    fun `trusted origin variants and complete status routes are canonicalized`() {
        listOf("x.com", "www.x.com", "mobile.x.com", "m.x.com",
            "twitter.com", "www.twitter.com", "mobile.twitter.com", "m.twitter.com", "WWW.X.COM")
            .forEach { host ->
                listOf("/alice_1/status/123", "/i/web/status/123", "/i/status/123").forEach { path ->
                    listOf("", "/", "/photo/2", "/video/1/").forEach { suffix ->
                        val base = "https://$host$path$suffix"
                        assertEquals(base, base, TwitterCleaner.clean("$base?random=share#opaque"))
                    }
                }
            }
        listOf("http://x.com", "http://x.com:80", "https://x.com:443").forEach { origin ->
            val base = "$origin/alice/status/1"
            assertEquals(base, TwitterCleaner.clean("$base?random=share"))
        }
    }

    @Test
    fun `existing functional options and media path selectors survive`() {
        val base = "https://x.com/alice/status/123/photo/2"
        val functional = "lang=hr&theme=dark&display=compact&tz=Europe%2FZagreb"
        assertEquals("$base?$functional", TwitterCleaner.clean("$base?a=opaque&$functional&s=19#share"))
    }

    @Test
    fun `special incomplete and ambiguous paths keep unknown parameters`() {
        listOf(
            "/search", "/intent/tweet", "/home", "/alice", "/alice/status",
            "/alice/status/0", "/alice/status/0123", "/alice/status/not-an-id",
            "/alice/status/%31%32%33", "/alice/status/123/likes", "/alice/status/123/embed",
            "/alice/status/123/photo/0", "/alice/status/123/video/not-an-index",
            "/alice/status/123/../456", "/alice/../status/123", "/alice%2Fbob/status/123",
            "/alice.with.dot/status/123", "/sixteen_charname/status/123",
            "/i/web/status/123/extra", "/alice/STATUS/123", "//alice/status/123"
        ).forEach { path ->
            val url = "https://x.com$path?future=keep&lang=en#state"
            assertEquals(path, url, TwitterCleaner.clean(url))
        }
    }

    @Test
    fun `proxies lookalikes and unexpected authorities keep their own options`() {
        listOf(
            "https://fixupx.com", "https://fxtwitter.com", "https://vxtwitter.com",
            "https://xcancel.com", "https://farside.link/nitter", "https://other.x.com",
            "https://x.com.example.org", "https://notx.com", "https://example.org",
            "https://user@x.com", "https://x.com:8443", "https://x.com:",
            "https://x.com.", "ftp://x.com"
        ).forEach { origin ->
            val url = "$origin/alice/status/123?future=keep&lang=en#state"
            assertEquals(origin, url, TwitterCleaner.clean(url))
        }
    }

    @Test
    fun `fallback still removes known tracking without removing unknown options`() {
        listOf("https://x.com/search", "https://fixupx.com/alice/status/123").forEach { base ->
            assertEquals("$base?future=keep&lang=en#state",
                TwitterCleaner.clean("$base?s=19&future=keep&lang=en#state"))
        }
    }

    @Test
    fun `malformed encoded permalinks are not canonicalized`() {
        listOf("https://x.com/alice/status/123?future=%ZZ", "https://x.com/alice/status/123#bad%ZZ")
            .forEach { url -> assertEquals(url, TwitterCleaner.clean(url)) }
    }
}
