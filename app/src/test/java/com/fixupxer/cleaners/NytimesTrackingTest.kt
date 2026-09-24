// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026 NeatCode Labs
 * Licensed under the GNU General Public License, version 3 or later.
 * See <https://www.gnu.org/licenses/>.
 */

package com.fixupxer.cleaners

import com.fixupxer.UrlProcessor
import com.fixupxer.cleaners.cache.CleanerCache
import org.junit.Assert.assertEquals
import org.junit.Test

class NytimesTrackingTest {
    private val registry = CleanerRegistry().apply { registerAll(CleanerCatalog.createBuiltInCleaners()) }
    private val service = CleanerService(registry, CleanerCache())
    private val article = "https://www.nytimes.com/2026/02/15/nyregion/" +
        "underground-railroad-manhattan-merchant-house.html"

    @Test
    fun `publisher share fixture removes only smid through the full pipeline`() {
        val input = "$article?smtyp=cur&smid=bsky-nytimes"
        val expected = "$article?smtyp=cur"
        assertEquals(expected, service.deepCleanWithoutCache(input))
        assertEquals(expected, UrlProcessor(service).processUrl(input, cleanTracking = true, convertTwitter = false).first)
        assertEquals(expected, service.deepCleanWithoutCache(expected))
        assertEquals(input, UrlProcessor(service).processUrl(input, cleanTracking = false, convertTwitter = false).first)
    }

    @Test
    fun `NYT preserves encoded gift access codes and unknown query and fragment data`() {
        val kept = "unlocked_article_code=1.a%2Bb%26c&unlocked_article_code=second&smtyp=cur&token=x%2Fy" +
            "&id=42&smid_extra=keep&SMID=keep&value=smid"
        val expected = "$article?$kept#smid=fragment"
        assertEquals(expected, service.deepCleanWithoutCache("$article?smid=share&$kept&smid=duplicate#smid=fragment"))
    }

    @Test
    fun `NYT rule applies only to exact public website hosts`() {
        listOf("nytimes.com", "www.nytimes.com", "WWW.NYTIMES.COM").forEach { host ->
            assertEquals("https://$host/article", service.deepCleanWithoutCache("https://$host/article?smid=share"))
        }
        listOf("example.com", "cooking.nytimes.com", "games.nytimes.com", "myaccount.nytimes.com",
            "nytimes.com.example.org", "notnytimes.com").forEach { host ->
            val input = "https://$host/article?smid=keep"
            assertEquals(input, service.deepCleanWithoutCache(input))
        }
        val pathOnly = "https://example.com/nytimes.com?smid=keep"
        assertEquals(pathOnly, service.deepCleanWithoutCache(pathOnly))
    }

    @Test
    fun `NYT fragment pseudo query is preserved and global trackers still clean`() {
        assertEquals("$article#section?smid=keep", service.deepCleanWithoutCache("$article#section?smid=keep"))
        assertEquals("$article?smtyp=cur#section", service.deepCleanWithoutCache(
            "$article?smid=share&ttclid=one&__hsfp=two&smtyp=cur#section"
        ))
    }
}
