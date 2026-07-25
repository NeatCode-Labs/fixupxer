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
    fun `aliexpress cleaner removes reported tracking keys and preserves gatewayAdapt`() {
        val rule = ParameterRuleCatalog.rules.first { it.id == "aliexpress" }
        val cleaner = CatalogParameterCleaner(rule)
        val input = "https://he.aliexpress.com/item/1005007790675247.html?" +
            "pdp_npi=4%40dis%21USD%21US+%2425.80%21US+%2412.40%21%21%2125.80%2112.40" +
            "%21%402102ef5e17849031233483899e10b3%2112000042214375874%21sh%21IL%21134321167%21X" +
            "&spm=a2g0o.store_pc_allItems_or_groupList.new_all_items_2007523659251.1005007790675247" +
            "&gatewayAdapt=glo2isr"
        val expected = "https://he.aliexpress.com/item/1005007790675247.html?gatewayAdapt=glo2isr"
        val cleaned = cleaner.clean(input)

        assertEquals(expected, cleaned)
        assertFalse(cleaned.contains("pdp_npi="))
        assertFalse(cleaned.contains("spm="))
        assertTrue(cleaned.contains("gatewayAdapt=glo2isr"))
        assertEquals(expected, cleaner.clean(cleaned))
    }

    @Test
    fun `aliexpress cleaner preserves pdp_ext_f and pvid byte for byte`() {
        val rule = ParameterRuleCatalog.rules.first { it.id == "aliexpress" }
        val cleaner = CatalogParameterCleaner(rule)
        val pdpExtF = "%7B%22fromPage%22%3A%22item%22%2C%22order%22%3A%221%22%2C" +
            "%22eval%22%3A%22A%22%2C%22sku_id%22%3A%2212000042214375874%22%7D"
        val pvid = "abc123-variant-context"
        val base = "https://www.aliexpress.com/item/1005007790675247.html"
        val input = "$base?pdp_ext_f=$pdpExtF&pvid=$pvid&spm=tracked"
        val expected = "$base?pdp_ext_f=$pdpExtF&pvid=$pvid"

        val cleaned = cleaner.clean(input)

        assertEquals(expected, cleaned)
        assertTrue(cleaned.contains("pdp_ext_f=$pdpExtF"))
        assertTrue(cleaned.contains("pvid=$pvid"))
        assertFalse(cleaned.contains("spm="))
        assertEquals(expected, cleaner.clean(cleaned))
    }

    @Test
    fun `bilibili cleaner removes only documented keys and preserves from`() {
        val rule = ParameterRuleCatalog.rules.first { it.id == "bilibili" }
        val cleaner = CatalogParameterCleaner(rule)
        val input = "https://www.bilibili.com/video/BV1?vd_source=share&seid=123&share_source=copy" +
            "&copy_link=1&from=search&VD_SOURCE=keep&unknown=raw+plus%26value" +
            "&unknown=second#frag%2Fvalue"
        val expected = "https://www.bilibili.com/video/BV1?from=search&VD_SOURCE=keep" +
            "&unknown=raw+plus%26value&unknown=second#frag%2Fvalue"

        assertEquals(expected, cleaner.clean(input))
        assertEquals(expected, cleaner.clean(cleaner.clean(input)))

        assertFalse(cleaner.matches("https://notbilibili.com/video?${
            rule.removeKeys.first()}=value"))
        assertFalse(cleaner.matches("https://mybilibili.com.evil.com/video?${
            rule.removeKeys.first()}=value"))

        val subdomainInput = "https://m.bilibili.com/video/BV1?vd_source=share&from=app"
        assertEquals("https://m.bilibili.com/video/BV1?from=app", cleaner.clean(subdomainInput))
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
