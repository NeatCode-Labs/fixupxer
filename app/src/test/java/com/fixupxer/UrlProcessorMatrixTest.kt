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

import android.net.Uri
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.CleanerRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UrlProcessorMatrixTest {
    private lateinit var p: UrlProcessor
    private lateinit var cleanerService: CleanerService
    
    @Before
    fun setup() {
        val registry = CleanerRegistry().apply {
            registerAll(listOf(
                com.fixupxer.cleaners.impl.AmazonCleaner,
                com.fixupxer.cleaners.impl.YouTubeCleaner,
                com.fixupxer.cleaners.impl.GoogleSearchCleaner,
                com.fixupxer.cleaners.impl.TwitterCleaner,
                com.fixupxer.cleaners.impl.InstagramCleaner,
                com.fixupxer.cleaners.impl.FacebookCleaner,
                com.fixupxer.cleaners.impl.RedditCleaner,
                com.fixupxer.cleaners.impl.TikTokCleaner,
                com.fixupxer.cleaners.impl.LinkedInCleaner,
                com.fixupxer.cleaners.impl.GeneralTrackingCleaner()
            ))
        }
        val cache = com.fixupxer.cleaners.cache.CleanerCache()
        cleanerService = CleanerService(registry, cache)
        p = UrlProcessor(cleanerService)
    }

    private data class Case(
        val desc: String,
        val url: String,
        val cleanTracking: Boolean,
        val convertSpecial: Boolean,
        val expectedUrl: String,
        val expectAlreadyClean: Boolean,
        val expectNothingToDo: Boolean
    )

    @Test
    fun debugUrlValidation() {
        // Test the specific case that's failing
        val testUrl = "https://example.com/page"
        
        println("Testing URL: '$testUrl'")
        
        // Test Uri.parse directly
        try {
            val uri = Uri.parse(testUrl)
            println("Uri.parse result: $uri")
            if (uri != null) {
                println("URI components: scheme=${uri.scheme}, host=${uri.host}, path=${uri.path}")
            } else {
                println("Uri.parse returned null!")
            }
        } catch (e: Exception) {
            println("Uri.parse exception: ${e.message}")
            e.printStackTrace()
        }
        
        // Test findFirstValidUrl
        val result = UrlProcessor.findFirstValidUrl(testUrl)
        println("findFirstValidUrl result for '$testUrl': $result")
        
        // Test if it's a valid URL
        val isValid = p.isValidUrl(testUrl)
        println("isValidUrl result for '$testUrl': $isValid")
    }

    @Test
    fun runMatrix() {
        val cases = listOf(
            // === Non-special links ===
            Case("non-special clean, no toggle", "https://example.com/page", true, false, "https://example.com/page", true, true),
            Case("non-special dirty, no toggle", "https://example.com/page?utm_source=abc", true, false, "https://example.com/page", false, false),

            // === Instagram ===
            // Default proxy is toinstagram.com (Constants.INSTAGRAM_DEFAULT_PROXY).
            // Clean instagram.com
            Case("instagram clean, toggle OFF", "https://instagram.com/p/1", true, false, "https://instagram.com/p/1", true, true),
            Case("instagram clean, toggle ON", "https://instagram.com/p/1", true, true, "https://toinstagram.com/p/1", false, false),
            // Dirty instagram.com
            Case("instagram dirty, toggle OFF", "https://instagram.com/p/1?utm_source=abc", true, false, "https://instagram.com/p/1", false, false),
            Case("instagram dirty, toggle ON", "https://instagram.com/p/1?utm_source=abc", true, true, "https://toinstagram.com/p/1", false, false),
            // Clean active default proxy (toinstagram.com): no-op when toggle ON, reverts to instagram when OFF
            Case("toinstagram clean, toggle OFF", "https://toinstagram.com/p/1", true, false, "https://instagram.com/p/1", false, false),
            Case("toinstagram clean, toggle ON", "https://toinstagram.com/p/1", true, true, "https://toinstagram.com/p/1", true, true),
            // Active backup proxy (kkinstagram) converts to the selected proxy (default toinstagram.com) when toggle ON
            Case("backup kkinstagram clean, toggle OFF", "https://kkinstagram.com/p/1", true, false, "https://instagram.com/p/1", false, false),
            Case("backup kkinstagram clean, toggle ON", "https://kkinstagram.com/p/1", true, true, "https://toinstagram.com/p/1", false, false),
            // Legacy proxy auto-migration: eeinstagram converts to default (toinstagram.com) when toggle ON
            Case("legacy eeinstagram clean, toggle OFF", "https://eeinstagram.com/p/1", true, false, "https://instagram.com/p/1", false, false),
            Case("legacy eeinstagram clean, toggle ON", "https://eeinstagram.com/p/1", true, true, "https://toinstagram.com/p/1", false, false),
            // www. is stripped on conversion (proxies render best at bare hostname)
            Case("www instagram, toggle ON", "https://www.instagram.com/p/1", true, true, "https://toinstagram.com/p/1", false, false),

            // === X/Twitter/Fixupx/FxTwitter ===
            // Clean x.com
            Case("x.com clean, toggle OFF", "https://x.com/user/status/1", true, false, "https://x.com/user/status/1", true, true),
            Case("x.com clean, toggle ON", "https://x.com/user/status/1", true, true, "https://fixupx.com/user/status/1", false, false),
            // Dirty x.com
            Case("x.com dirty, toggle OFF", "https://x.com/user/status/1?utm_source=abc", true, false, "https://x.com/user/status/1", false, false),
            Case("x.com dirty, toggle ON", "https://x.com/user/status/1?utm_source=abc", true, true, "https://fixupx.com/user/status/1", false, false),
            // Clean fixupx.com
            Case("fixupx.com clean, toggle OFF", "https://fixupx.com/user/status/1", true, false, "https://x.com/user/status/1", false, false),
            Case("fixupx.com clean, toggle ON", "https://fixupx.com/user/status/1", true, true, "https://fixupx.com/user/status/1", true, true),
            // Dirty fixupx.com
            Case("fixupx.com dirty, toggle OFF", "https://fixupx.com/user/status/1?utm_source=abc", true, false, "https://x.com/user/status/1", false, false),
            Case("fixupx.com dirty, toggle ON", "https://fixupx.com/user/status/1?utm_source=abc", true, true, "https://fixupx.com/user/status/1", false, false),
            // Clean fxtwitter.com
            Case("fxtwitter.com clean, toggle OFF", "https://fxtwitter.com/user/status/1", true, false, "https://x.com/user/status/1", false, false),
            Case("fxtwitter.com clean, toggle ON", "https://fxtwitter.com/user/status/1", true, true, "https://fixupx.com/user/status/1", false, false),
            // Dirty fxtwitter.com
            Case("fxtwitter.com dirty, toggle OFF", "https://fxtwitter.com/user/status/1?utm_source=abc", true, false, "https://x.com/user/status/1", false, false),
            Case("fxtwitter.com dirty, toggle ON", "https://fxtwitter.com/user/status/1?utm_source=abc", true, true, "https://fixupx.com/user/status/1", false, false),
            // vxtwitter.com (same treatment as fxtwitter)
            Case("vxtwitter.com clean, toggle OFF", "https://vxtwitter.com/user/status/1", true, false, "https://x.com/user/status/1", false, false),
            Case("vxtwitter.com clean, toggle ON", "https://vxtwitter.com/user/status/1", true, true, "https://fixupx.com/user/status/1", false, false),

            // === TikTok ===
            // Default proxy is tnktok.com (Constants.TIKTOK_DEFAULT_PROXY).
            // Host prefix (www., vm., …) is preserved on conversion.
            Case("tiktok clean, toggle OFF", "https://www.tiktok.com/@user/video/1", true, false, "https://www.tiktok.com/@user/video/1", true, true),
            Case("tiktok clean, toggle ON", "https://www.tiktok.com/@user/video/1", true, true, "https://www.tnktok.com/@user/video/1", false, false),
            Case("tiktok dirty, toggle OFF", "https://www.tiktok.com/@user/video/1?is_from_webapp=1&_r=1", true, false, "https://www.tiktok.com/@user/video/1", false, false),
            Case("tiktok dirty, toggle ON", "https://www.tiktok.com/@user/video/1?is_from_webapp=1&_r=1", true, true, "https://www.tnktok.com/@user/video/1", false, false),
            // vm. short link keeps its prefix
            Case("vm.tiktok clean, toggle ON", "https://vm.tiktok.com/ZMabcdef/", true, true, "https://vm.tnktok.com/ZMabcdef/", false, false),
            // Clean active default proxy (tnktok.com): no-op when toggle ON, reverts to tiktok when OFF
            Case("tnktok clean, toggle OFF", "https://tnktok.com/@user/video/1", true, false, "https://tiktok.com/@user/video/1", false, false),
            Case("tnktok clean, toggle ON", "https://tnktok.com/@user/video/1", true, true, "https://tnktok.com/@user/video/1", true, true),
            // Active backup proxy (kktiktok) converts to the selected proxy (default tnktok.com) when toggle ON
            Case("backup kktiktok clean, toggle OFF", "https://kktiktok.com/@user/video/1", true, false, "https://tiktok.com/@user/video/1", false, false),
            Case("backup kktiktok clean, toggle ON", "https://kktiktok.com/@user/video/1", true, true, "https://tnktok.com/@user/video/1", false, false),
            // Legacy proxy auto-migration: vxtiktok converts to default (tnktok.com) when toggle ON
            Case("legacy vxtiktok clean, toggle OFF", "https://vxtiktok.com/@user/video/1", true, false, "https://tiktok.com/@user/video/1", false, false),
            Case("legacy vxtiktok clean, toggle ON", "https://vxtiktok.com/@user/video/1", true, true, "https://tnktok.com/@user/video/1", false, false),

            // === Facebook ===
            Case("facebook.com clean, toggle OFF", "https://facebook.com/somepage", true, false, "https://facebook.com/somepage", true, true),
            Case("facebook.com clean, toggle ON", "https://facebook.com/somepage", true, true, "https://facebookez.com/somepage", false, false),
            // fb.com short domain now routes through the facebookez conversion too
            Case("fb.com clean, toggle OFF", "https://fb.com/somepage", true, false, "https://fb.com/somepage", true, true),
            Case("fb.com clean, toggle ON", "https://fb.com/somepage", true, true, "https://facebookez.com/somepage", false, false),
            Case("m.facebook.com clean, toggle ON", "https://m.facebook.com/somepage", true, true, "https://facebookez.com/somepage", false, false),
            Case("facebookez.com clean, toggle OFF", "https://facebookez.com/somepage", true, false, "https://facebook.com/somepage", false, false),
            Case("facebookez.com clean, toggle ON", "https://facebookez.com/somepage", true, true, "https://facebookez.com/somepage", true, true)
        )

        cases.forEach { c ->
            try {
                val (out, alreadyClean) = p.processUrl(c.url, c.cleanTracking, c.convertSpecial)
                assertEquals("Fail: ${c.desc}", c.expectedUrl, out)
                assertEquals("Clean flag mismatch: ${c.desc}", c.expectAlreadyClean, alreadyClean)
                if (c.expectNothingToDo) {
                    assertTrue("Should be 'Nothing to do!': ${c.desc}", alreadyClean)
                } else {
                    assertFalse("Should NOT be 'Nothing to do!': ${c.desc}", alreadyClean)
                }
            } catch (e: Exception) {
                throw RuntimeException("Test case failed: ${c.desc} | url='${c.url}' | cleanTracking=${c.cleanTracking} | convertSpecial=${c.convertSpecial} | expectedUrl='${c.expectedUrl}' | expectAlreadyClean=${c.expectAlreadyClean} | expectNothingToDo=${c.expectNothingToDo}", e)
            }
        }
    }

    @Test
    fun debugRemoveTrackingParameters() {
        val dirtyUrl = "https://example.com/page?utm_source=abc"
        val cleaned = p.processUrl(dirtyUrl, cleanTracking = true, convertTwitter = false)
        println("Original: $dirtyUrl")
        println("Cleaned: ${cleaned.first}")
    }
} 