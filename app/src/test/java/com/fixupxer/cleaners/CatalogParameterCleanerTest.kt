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

package com.fixupxer.cleaners

import com.fixupxer.UrlProcessor
import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.cleaners.impl.CatalogParameterCleaner
import com.fixupxer.cleaners.impl.FacebookCleaner
import com.fixupxer.cleaners.impl.GeneralTrackingCleaner
import com.fixupxer.cleaners.impl.GoogleSearchCleaner
import com.fixupxer.cleaners.impl.InstagramCleaner
import com.fixupxer.cleaners.impl.TwitterCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogParameterCleanerTest {
    @Test
    fun `every documented catalog key has a positive realistic fixture`() {
        ParameterRuleCatalog.rules.forEach { rule ->
            val cleaner = CatalogParameterCleaner(rule)
            rule.removeKeys.forEach { trackingKey ->
                val input = platformUrl(rule, "$trackingKey=tracked&keep=raw+plus%26value")
                val expected = "${input.substringBefore('?')}?keep=raw+plus%26value"

                assertEquals("${rule.id}: $trackingKey", expected, cleaner.clean(input))
            }
        }
    }

    @Test
    fun `catalog cleaners preserve unknown raw duplicate and fragment tokens`() {
        ParameterRuleCatalog.rules.forEach { rule ->
            val cleaner = CatalogParameterCleaner(rule)
            val trackingKey = rule.removeKeys.first()
            val input = platformUrl(
                rule,
                "$trackingKey=one&$trackingKey=two&unknown=raw+plus%26value" +
                    "&unknown=second#frag%2Fvalue"
            )
            val expected = "${input.substringBefore('?')}?unknown=raw+plus%26value" +
                "&unknown=second#frag%2Fvalue"
            val cleaned = cleaner.clean(input)

            assertEquals(rule.id, expected, cleaned)
            assertEquals(rule.id, cleaned, cleaner.clean(cleaned))
        }
    }

    @Test
    fun `catalog cleaner keeps preserve keys flags and empty query forms`() {
        ParameterRuleCatalog.rules.forEach { rule ->
            val cleaner = CatalogParameterCleaner(rule)
            val trackingKey = rule.removeKeys.first()
            val preserved = rule.preserveKeys.firstOrNull() ?: "functional"
            val input = platformUrl(rule, "$trackingKey&$preserved=value&flag")
            val expected = "${input.substringBefore('?')}?$preserved=value&flag"

            assertEquals(rule.id, expected, cleaner.clean(input))
            assertEquals(rule.id, input.substringBefore('?') + "?", cleaner.clean(input.substringBefore('?') + "?"))
        }
    }

    @Test
    fun `catalog cleaners require host label boundaries and leave malformed URLs`() {
        ParameterRuleCatalog.rules.forEach { rule ->
            val cleaner = CatalogParameterCleaner(rule)
            val domain = rule.domains.first()
            val key = rule.removeKeys.first()

            assertFalse(rule.id, cleaner.matches("https://not$domain/path?$key=value"))
            assertFalse(rule.id, cleaner.matches("https://my$domain.evil.com/path?$key=value"))
            assertEquals(rule.id, "https://", cleaner.clean("https://"))
        }
    }

    @Test
    fun `google store cleaner does not overlap Google Search`() {
        val storeUrl = "https://store.google.com/product?hl=en&selections=blue&sku=phone"

        assertFalse(GoogleSearchCleaner.matches(storeUrl))
        assertEquals(
            "https://store.google.com/product?sku=phone",
            CatalogParameterCleaner(
                ParameterRuleCatalog.rules.first { it.id == "google_store" }
            ).clean(storeUrl)
        )
    }

    @Test
    fun `catalog cleaner runs through UrlProcessor pipeline`() {
        val registry = CleanerRegistry().apply {
            registerAll(CleanerCatalog.createBuiltInCleaners())
        }
        val processor = UrlProcessor(CleanerService(registry, CleanerCache()))

        assertEquals(
            "https://www.wikipedia.org/wiki/URL?title=URL",
            processor.processUrl(
                "https://www.wikipedia.org/wiki/URL?wprov=sfla1&title=URL",
                cleanTracking = true,
                convertTwitter = false
            ).first
        )
    }

    @Test
    fun `cleaners expose explicit execution priorities`() {
        assertEquals(UrlCleaner.PRIORITY_EXTRACTION, GoogleSearchCleaner.priority)
        assertEquals(UrlCleaner.PRIORITY_CONVERSION, TwitterCleaner.priority)
        assertEquals(UrlCleaner.PRIORITY_CONVERSION, InstagramCleaner.priority)
        assertEquals(UrlCleaner.PRIORITY_CONVERSION, FacebookCleaner.priority)
        assertEquals(UrlCleaner.PRIORITY_DOMAIN, CatalogParameterCleaner(ParameterRuleCatalog.rules.first()).priority)
        assertEquals(UrlCleaner.PRIORITY_GENERAL, GeneralTrackingCleaner().priority)
        assertTrue(GoogleSearchCleaner.priority < TwitterCleaner.priority)
        assertTrue(TwitterCleaner.priority < CatalogParameterCleaner(ParameterRuleCatalog.rules.first()).priority)
        assertTrue(CatalogParameterCleaner(ParameterRuleCatalog.rules.first()).priority < GeneralTrackingCleaner().priority)
    }

    private fun platformUrl(rule: PlatformParameterRule, query: String): String {
        val host = when (rule.id) {
            "ebay" -> "www.ebay.com"
            "netflix" -> "www.netflix.com"
            "aliexpress" -> "www.aliexpress.com"
            else -> "www.${rule.domains.first()}"
        }
        val path = when (rule.id) {
            "ebay" -> "/itm/123456789012"
            "netflix" -> "/title/80057281"
            "aliexpress" -> "/item/1005000000000000.html"
            else -> "/path"
        }
        return "https://$host$path?$query"
    }
}
