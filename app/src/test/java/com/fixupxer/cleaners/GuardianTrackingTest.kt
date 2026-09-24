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
import com.fixupxer.cleaners.impl.CatalogParameterCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GuardianTrackingTest {
    private val registry = CleanerRegistry().apply { registerAll(CleanerCatalog.createBuiltInCleaners()) }
    private val service = CleanerService(registry, CleanerCache())
    private val guardian = CatalogParameterCleaner(ParameterRuleCatalog.rules.first { it.id == "guardian" })
    private val article = "https://www.theguardian.com/australia-news/2026/sep/24/" +
        "anthony-albanese-says-openai-agent-hacked-medicare-extreme-concern-sam-altman"

    @Test
    fun `reported Guardian share URL is cleaned through the complete cleaner service`() {
        val input = "$article?CMP=Share_iOSApp_Other"
        assertEquals(article, service.deepCleanWithoutCache(input))
        assertEquals(article, UrlProcessor(service).processUrl(input, cleanTracking = true, convertTwitter = false).first)
        assertEquals(article, service.deepCleanWithoutCache(service.deepCleanWithoutCache(input)))
    }

    @Test
    fun `Guardian matches only public website hosts and removes mixed case duplicate CMP keys`() {
        listOf("theguardian.com", "www.theguardian.com", "WWW.THEGUARDIAN.COM").forEach { host ->
            assertEquals(
                "https://$host/article?keep=a%2Bb%26c&keep=second#section",
                service.deepCleanWithoutCache("https://$host/article?CMP=one&cmp=two&CmP=three&keep=a%2Bb%26c&keep=second#section")
            )
        }
    }

    @Test
    fun `Guardian rule preserves other hosts subdomains and lookalikes`() {
        listOf("example.com", "profile.theguardian.com", "support.theguardian.com", "m.theguardian.com",
            "theguardian.com.example.org", "nottheguardian.com").forEach { host ->
            val url = "https://$host/article?CMP=keep"
            assertFalse(host, guardian.matches(url))
            assertEquals(url, service.deepCleanWithoutCache(url))
        }
        val pathOnly = "https://example.com/theguardian.com?CMP=keep"
        assertEquals(pathOnly, service.deepCleanWithoutCache(pathOnly))
    }

    @Test
    fun `Guardian cleanup preserves unknown access and fragment data`() {
        val kept = "unlocked_article_code=a%2Bb&token=opaque&id=42&INTCMP=keep&CMP_extra=keep&value=CMP"
        assertEquals("$article?$kept#CMP=fragment", service.deepCleanWithoutCache("$article?CMP=share&$kept#CMP=fragment"))
        assertEquals("$article#section?CMP=keep", guardian.clean("$article#section?CMP=keep"))
        assertEquals("$article?", guardian.clean("$article?"))
    }

    @Test
    fun `new global keys are removed with Guardian and through UrlProcessor on other sites`() {
        assertEquals(article, service.deepCleanWithoutCache("$article?CMP=share&ttclid=one&rdt_cid=two&__hsfp=three"))
        assertEquals(
            "https://shop.example/product?sku=42#details",
            UrlProcessor(service).processUrl(
                "https://shop.example/product?sku=42&ttclid=one&li_fat_id=two&rdt_cid=three&ScCid=four#details",
                cleanTracking = true, convertTwitter = false
            ).first
        )
    }

    @Test
    fun `catalog case sensitivity remains unchanged for existing platforms`() {
        val bilibili = CatalogParameterCleaner(ParameterRuleCatalog.rules.first { it.id == "bilibili" })
        assertEquals(
            "https://www.bilibili.com/video/BV1?VD_SOURCE=keep",
            bilibili.clean("https://www.bilibili.com/video/BV1?vd_source=remove&VD_SOURCE=keep")
        )
    }
}
