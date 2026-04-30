// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2025  NeatCode Labs
 */

package com.fixupxer

import com.fixupxer.cleaners.CleanerRegistry
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Unit tests for the Instagram proxy selection feature (v1.4.8 proxy set).
 *
 * Active proxies: adamlikes.men (primary), toinstagram.com (primary), instagram7.com (backup).
 * Legacy proxies (kkinstagram.com, eeinstagram.com) are still recognized so that previously
 * pasted/shared links continue to work, but are always converted to one of the active proxies.
 *
 * Verifies:
 *   1. Forward conversion instagram.com -> any active proxy
 *   2. Cross-proxy swaps between active and legacy domains
 *   3. `www.` and any host-level sub-prefix is stripped on conversion
 *      (proxies render best at the bare hostname)
 *   4. Backward conversion: any known proxy -> instagram.com
 *   5. isInstagramUrl recognizes the full known set
 */
class InstagramProxySelectionTest {

    private lateinit var processor: UrlProcessor

    @Before
    fun setup() {
        val registry = CleanerRegistry().apply {
            registerAll(
                listOf(
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
                )
            )
        }
        val cache = CleanerCache()
        val prefs = mock<PreferencesManager>()
        processor = UrlProcessor(CleanerService(registry, cache, prefs))
    }

    private fun convert(url: String, target: String, convertOn: Boolean = true) =
        processor.processUrl(
            url,
            cleanTracking = false,
            convertTwitter = convertOn,
            instagramProxy = target
        ).first

    // ---------------------------------------------------------------------
    // Forward: instagram.com -> any active proxy
    // ---------------------------------------------------------------------

    @Test
    fun `instagram_com converts to adamlikes_men`() {
        assertEquals(
            "https://adamlikes.men/p/abc",
            convert("https://instagram.com/p/abc", Constants.ADAMLIKES_DOMAIN)
        )
    }

    @Test
    fun `instagram_com converts to toinstagram_com`() {
        assertEquals(
            "https://toinstagram.com/p/abc",
            convert("https://instagram.com/p/abc", Constants.TOINSTAGRAM_DOMAIN)
        )
    }

    @Test
    fun `instagram_com converts to instagram7_com`() {
        assertEquals(
            "https://instagram7.com/p/abc",
            convert("https://instagram.com/p/abc", Constants.INSTAGRAM7_DOMAIN)
        )
    }

    // ---------------------------------------------------------------------
    // www. stripping (v1.4.8 — proxies render best at bare hostname)
    // ---------------------------------------------------------------------

    @Test
    fun `www_instagram_com converts to bare adamlikes_men (no www)`() {
        assertEquals(
            "https://adamlikes.men/p/abc",
            convert("https://www.instagram.com/p/abc", Constants.ADAMLIKES_DOMAIN)
        )
    }

    @Test
    fun `www_instagram_com converts to bare toinstagram_com (no www)`() {
        assertEquals(
            "https://toinstagram.com/reel/xyz",
            convert("https://www.instagram.com/reel/xyz", Constants.TOINSTAGRAM_DOMAIN)
        )
    }

    @Test
    fun `host-level subdomain is stripped on conversion`() {
        // business.instagram.com -> bare adamlikes.men (no business prefix)
        assertEquals(
            "https://adamlikes.men/p/abc",
            convert("https://business.instagram.com/p/abc", Constants.ADAMLIKES_DOMAIN)
        )
    }

    @Test
    fun `URL already on target proxy with www gets www stripped`() {
        assertEquals(
            "https://adamlikes.men/p/abc",
            convert("https://www.adamlikes.men/p/abc", Constants.ADAMLIKES_DOMAIN)
        )
    }

    // ---------------------------------------------------------------------
    // Cross-proxy swaps among active proxies
    // ---------------------------------------------------------------------

    @Test
    fun `adamlikes_men converts to toinstagram_com`() {
        assertEquals(
            "https://toinstagram.com/p/abc",
            convert("https://adamlikes.men/p/abc", Constants.TOINSTAGRAM_DOMAIN)
        )
    }

    @Test
    fun `toinstagram_com converts to instagram7_com`() {
        assertEquals(
            "https://instagram7.com/p/abc",
            convert("https://toinstagram.com/p/abc", Constants.INSTAGRAM7_DOMAIN)
        )
    }

    @Test
    fun `instagram7_com converts to adamlikes_men`() {
        assertEquals(
            "https://adamlikes.men/p/abc",
            convert("https://instagram7.com/p/abc", Constants.ADAMLIKES_DOMAIN)
        )
    }

    // ---------------------------------------------------------------------
    // Legacy proxies (kkinstagram, eeinstagram) — auto-migrate to active
    // ---------------------------------------------------------------------

    @Test
    fun `legacy kkinstagram converts to adamlikes_men`() {
        assertEquals(
            "https://adamlikes.men/p/abc",
            convert("https://kkinstagram.com/p/abc", Constants.ADAMLIKES_DOMAIN)
        )
    }

    @Test
    fun `legacy eeinstagram converts to toinstagram_com`() {
        assertEquals(
            "https://toinstagram.com/p/abc",
            convert("https://eeinstagram.com/p/abc", Constants.TOINSTAGRAM_DOMAIN)
        )
    }

    // ---------------------------------------------------------------------
    // No-op: bare proxy already matches target
    // ---------------------------------------------------------------------

    @Test
    fun `bare adamlikes_men unchanged when target=adamlikes`() {
        val url = "https://adamlikes.men/p/abc"
        assertEquals(url, convert(url, Constants.ADAMLIKES_DOMAIN))
    }

    @Test
    fun `bare toinstagram_com unchanged when target=toinstagram`() {
        val url = "https://toinstagram.com/p/abc"
        assertEquals(url, convert(url, Constants.TOINSTAGRAM_DOMAIN))
    }

    @Test
    fun `bare instagram7_com unchanged when target=instagram7`() {
        val url = "https://instagram7.com/p/abc"
        assertEquals(url, convert(url, Constants.INSTAGRAM7_DOMAIN))
    }

    // ---------------------------------------------------------------------
    // Backward: any proxy -> instagram.com (Embed toggle OFF)
    // ---------------------------------------------------------------------

    @Test
    fun `adamlikes_men reverts to instagram_com when convertOff`() {
        assertEquals(
            "https://instagram.com/p/abc",
            convert("https://adamlikes.men/p/abc", Constants.ADAMLIKES_DOMAIN, convertOn = false)
        )
    }

    @Test
    fun `toinstagram_com reverts to instagram_com when convertOff`() {
        assertEquals(
            "https://instagram.com/p/abc",
            convert("https://toinstagram.com/p/abc", Constants.ADAMLIKES_DOMAIN, convertOn = false)
        )
    }

    @Test
    fun `legacy kkinstagram reverts to instagram_com when convertOff`() {
        assertEquals(
            "https://instagram.com/p/abc",
            convert("https://kkinstagram.com/p/abc", Constants.ADAMLIKES_DOMAIN, convertOn = false)
        )
    }

    // ---------------------------------------------------------------------
    // Dirty URL cleaning + proxy conversion
    // ---------------------------------------------------------------------

    @Test
    fun `dirty instagram url cleans tracking and converts to toinstagram`() {
        // Use cleanTracking = true explicitly (default helper has it off)
        val result = processor.processUrl(
            "https://instagram.com/p/abc?igshid=zzz&utm_source=ig",
            cleanTracking = true,
            convertTwitter = true,
            instagramProxy = Constants.TOINSTAGRAM_DOMAIN
        ).first
        assertEquals("https://toinstagram.com/p/abc", result)
    }

    // ---------------------------------------------------------------------
    // isInstagramUrl covers active + legacy
    // ---------------------------------------------------------------------

    @Test
    fun `isInstagramUrl recognizes active and legacy proxies and instagram_com`() {
        assertTrue(processor.isInstagramUrl("https://instagram.com/p/abc"))
        assertTrue(processor.isInstagramUrl("https://www.instagram.com/p/abc"))
        assertTrue(processor.isInstagramUrl("https://adamlikes.men/p/abc"))
        assertTrue(processor.isInstagramUrl("https://toinstagram.com/p/abc"))
        assertTrue(processor.isInstagramUrl("https://instagram7.com/p/abc"))
        // legacy still detected so the toggle still triggers
        assertTrue(processor.isInstagramUrl("https://kkinstagram.com/p/abc"))
        assertTrue(processor.isInstagramUrl("https://eeinstagram.com/p/abc"))
        assertFalse(processor.isInstagramUrl("https://example.com/p/abc"))
    }
}
