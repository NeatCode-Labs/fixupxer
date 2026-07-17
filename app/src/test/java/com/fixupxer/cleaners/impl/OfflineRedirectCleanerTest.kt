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

package com.fixupxer.cleaners.impl

import com.fixupxer.cleaners.CleanerCatalog
import com.fixupxer.cleaners.CleanerRegistry
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.cache.CleanerCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfflineRedirectCleanerTest {
    private lateinit var cleanerService: CleanerService

    @Before
    fun setup() {
        cleanerService = CleanerService(
            CleanerRegistry().apply { registerAll(CleanerCatalog.createBuiltInCleaners()) },
            CleanerCache()
        )
    }

    @Test
    fun `unwraps every supported query redirect`() {
        val cases = listOf(
            "https://l.facebook.com/l.php?u=https%3A%2F%2Fexample.com%2Ffacebook" to
                "https://example.com/facebook",
            "https://lm.facebook.com/l.php?u=https%3A%2F%2Fexample.com%2Fmobile" to
                "https://example.com/mobile",
            "https://www.linkedin.com/safety/go?url=https%3A%2F%2Fexample.com%2Flinkedin" to
                "https://example.com/linkedin",
            "https://m.youtube.com/redirect?q=https%3A%2F%2Fexample.com%2Fyoutube" to
                "https://example.com/youtube",
            "https://go.bsky.app/redirect?u=https%3A%2F%2Fexample.com%2Fbluesky" to
                "https://example.com/bluesky",
            "https://www.googleadservices.com/pagead/aclk?adurl=https%3A%2F%2Fexample.com%2Fads" to
                "https://example.com/ads",
            "https://target.georiot.com/Proxy.ashx?GR_URL=https%3A%2F%2Fexample.com%2Fgeoriot" to
                "https://example.com/georiot",
            "https://click.linksynergy.com/link?id=123&murl=https%3A%2F%2Fexample.com%2Faffiliate" to
                "https://example.com/affiliate"
        )

        cases.forEach { (input, expected) ->
            assertEquals(expected, OfflineRedirectCleaner.clean(input))
        }
    }

    @Test
    fun `unwraps reddit mail path target`() {
        val input = "https://click.redditmail.com/CL0/https%3A%2F%2Fexample.com%2Freddit%3Fa%3D1/extra"

        assertEquals("https://example.com/reddit?a=1", OfflineRedirectCleaner.clean(input))
    }

    @Test
    fun `rejects lookalike hosts and wrong paths`() {
        val inputs = listOf(
            "https://l.facebook.com.evil.com/l.php?u=https%3A%2F%2Fexample.com",
            "https://xlinkedin.com/safety/go?url=https%3A%2F%2Fexample.com",
            "https://go.bsky.app.evil.com/redirect?u=https%3A%2F%2Fexample.com",
            "https://notclick.redditmail.com/CL0/https%3A%2F%2Fexample.com",
            "https://l.facebook.com/l.phpx?u=https%3A%2F%2Fexample.com",
            "https://youtube.com/redirectx?q=https%3A%2F%2Fexample.com",
            "https://target.georiot.com.evil.com/Proxy.ashx?GR_URL=https%3A%2F%2Fexample.com",
            "https://notclick.linksynergy.com/link?murl=https%3A%2F%2Fexample.com",
            "https://click.linksynergy.com/linkx?murl=https%3A%2F%2Fexample.com",
            "https://target.georiot.com/Proxy.ashxx?GR_URL=https%3A%2F%2Fexample.com",
            "https://target.georiot.com/Proxy.ashx/extra?GR_URL=https%3A%2F%2Fexample.com",
            "https://click.linksynergy.com/link/extra?murl=https%3A%2F%2Fexample.com"
        )

        inputs.forEach { input -> assertEquals(input, OfflineRedirectCleaner.clean(input)) }
    }

    @Test
    fun `ignores query-like text inside wrapper fragments`() {
        val inputs = listOf(
            "https://target.georiot.com/Proxy.ashx#note?GR_URL=https%3A%2F%2Fexample.com",
            "https://click.linksynergy.com/link#note?murl=https%3A%2F%2Fexample.com"
        )

        inputs.forEach { input -> assertEquals(input, OfflineRedirectCleaner.clean(input)) }
    }

    @Test
    fun `keeps missing empty double encoded and invalid targets wrapped`() {
        val inputs = listOf(
            "https://l.facebook.com/l.php?x=https%3A%2F%2Fexample.com",
            "https://l.facebook.com/l.php?u=",
            "https://l.facebook.com/l.php?u=javascript%3Aalert%281%29&u=https%3A%2F%2Fexample.com",
            "https://l.facebook.com/l.php?u=https%253A%252F%252Fexample.com",
            "https://l.facebook.com/l.php?u=javascript%3Aalert%281%29",
            "https://l.facebook.com/l.php?u=%2F%2Fevil.com",
            "https://l.facebook.com/l.php?u=%2Frelative",
            "https://target.georiot.com/Proxy.ashx?x=https%3A%2F%2Fexample.com",
            "https://target.georiot.com/Proxy.ashx?GR_URL=",
            "https://target.georiot.com/Proxy.ashx?GR_URL=https%253A%252F%252Fexample.com",
            "https://target.georiot.com/Proxy.ashx?GR_URL=javascript%3Aalert%281%29",
            "https://click.linksynergy.com/link?x=https%3A%2F%2Fexample.com",
            "https://click.linksynergy.com/link?murl=",
            "https://click.linksynergy.com/link?murl=https%253A%252F%252Fexample.com",
            "https://click.linksynergy.com/link?murl=javascript%3Aalert%281%29",
            "https://click.linksynergy.com/link?murl=%2Frelative"
        )

        inputs.forEach { input -> assertEquals(input, OfflineRedirectCleaner.clean(input)) }
    }

    @Test
    fun `rejects structurally invalid target ports`() {
        val inputs = listOf(
            "https://target.georiot.com/Proxy.ashx?GR_URL=https%3A%2F%2Fexample.com%3A0%2Fitem",
            "https://click.linksynergy.com/link?murl=https%3A%2F%2Fexample.com%3A99999%2Fitem"
        )

        inputs.forEach { input -> assertEquals(input, OfflineRedirectCleaner.clean(input)) }
    }

    @Test
    fun `rejects malformed target escapes and boundary whitespace`() {
        val inputs = listOf(
            "https://target.georiot.com/Proxy.ashx?GR_URL=https%3A%2F%2Fexample.com%2F%25ZZ",
            "https://click.linksynergy.com/link?murl=%20https%3A%2F%2Fexample.com%2Fitem",
            "https://click.linksynergy.com/link?murl=https%3A%2F%2Fexample.com%2Fitem%20"
        )

        inputs.forEach { input -> assertEquals(input, OfflineRedirectCleaner.clean(input)) }
    }

    @Test
    fun `uses case-sensitive destination keys`() {
        val inputs = listOf(
            "https://target.georiot.com/Proxy.ashx?gr_url=https%3A%2F%2Fexample.com",
            "https://click.linksynergy.com/link?Murl=https%3A%2F%2Fexample.com"
        )

        inputs.forEach { input -> assertEquals(input, OfflineRedirectCleaner.clean(input)) }
    }

    @Test
    fun `uses first destination value and cannot bypass an unsafe first value`() {
        val firstWins =
            "https://target.georiot.com/Proxy.ashx?GR_URL=https%3A%2F%2Ffirst.example%2Fitem" +
                "&GR_URL=https%3A%2F%2Fsecond.example%2Fitem"
        val unsafeFirst =
            "https://click.linksynergy.com/link?murl=javascript%3Aalert%281%29" +
                "&murl=https%3A%2F%2Fsafe.example%2Fitem"

        assertEquals("https://first.example/item", OfflineRedirectCleaner.clean(firstWins))
        assertEquals(unsafeFirst, OfflineRedirectCleaner.clean(unsafeFirst))
    }

    @Test
    fun `preserves plus and drops wrapper fragment`() {
        val input = "https://l.facebook.com/l.php?u=https%3A%2F%2Fexample.com%2Fsearch%3Fq%3Da+b#source"

        assertEquals("https://example.com/search?q=a+b", OfflineRedirectCleaner.clean(input))
    }

    @Test
    fun `preserves target plus encoded separators and fragment`() {
        val input =
            "https://target.georiot.com/Proxy.ashx?GR_URL=https%3A%2F%2Fexample.com%2Fsearch" +
                "%3Fq%3Da+b%26encoded%3Dx%2526y%23section%2Fone"

        assertEquals(
            "https://example.com/search?q=a+b&encoded=x%26y#section/one",
            OfflineRedirectCleaner.clean(input)
        )
    }

    @Test
    fun `is idempotent`() {
        val input = "https://www.linkedin.com/safety/go?url=https%3A%2F%2Fexample.com%2Farticle"
        val first = OfflineRedirectCleaner.clean(input)

        assertEquals(first, OfflineRedirectCleaner.clean(first))
    }

    @Test
    fun `deep clean resolves nested wrappers across passes`() {
        val input = "https://l.facebook.com/l.php?u=https%3A%2F%2Fwww.youtube.com%2Fredirect%3Fq%3Dhttps%253A%252F%252Fexample.com%252Farticle%253Futm_source%253Dmail"

        assertEquals("https://example.com/article", cleanerService.deepClean(input))
    }

    @Test
    fun `deep clean resolves nested georiot wrapper target`() {
        val input = "https://target.georiot.com/Proxy.ashx?GR_URL=https%3A%2F%2Fexample.com%2Farticle%3Futm_source%3Dmail"

        assertEquals("https://example.com/article", cleanerService.deepClean(input))
    }

    @Test
    fun `deep clean resolves linksynergy target and removes nested tracking`() {
        val input = "https://click.linksynergy.com/link?murl=https%3A%2F%2Fexample.com%2Farticle" +
            "%3Futm_source%3Dmail%26keep%3Dvalue"

        assertEquals("https://example.com/article?keep=value", cleanerService.deepClean(input))
    }

    @Test
    fun `self referring wrapper terminates within cleaner pass limit`() {
        val input = "https://l.facebook.com/l.php?u=https%3A%2F%2Fl.facebook.com%2Fl.php%3Fu%3D"

        val result = cleanerService.deepCleanWithDetails(input)

        assertTrue(result.totalPasses <= 5)
        assertEquals("https://l.facebook.com/l.php?u=", result.cleanedUrl)
    }
}
