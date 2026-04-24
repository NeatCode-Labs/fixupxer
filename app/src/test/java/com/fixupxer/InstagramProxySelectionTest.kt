// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2025  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
 * Unit tests for the Instagram proxy selection feature.
 *
 * Verifies that [UrlProcessor.processUrl] correctly converts
 * between instagram.com and the three supported proxies
 * (kkinstagram.com, eeinstagram.com, instagram7.com), including cross-proxy swaps.
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

    // ---------------------------------------------------------------------
    // Forward: instagram.com -> any proxy
    // ---------------------------------------------------------------------

    @Test
    fun `instagram_com converts to kkinstagram_com when proxy=kkinstagram`() {
        val result = processor.processUrl(
            "https://instagram.com/p/abc",
            cleanTracking = false,
            convertTwitter = true,
            instagramProxy = Constants.KKINSTAGRAM_DOMAIN
        ).first
        assertEquals("https://kkinstagram.com/p/abc", result)
    }

    @Test
    fun `instagram_com converts to eeinstagram_com when proxy=eeinstagram`() {
        val result = processor.processUrl(
            "https://instagram.com/p/abc",
            cleanTracking = false,
            convertTwitter = true,
            instagramProxy = Constants.EEINSTAGRAM_DOMAIN
        ).first
        assertEquals("https://eeinstagram.com/p/abc", result)
    }

    @Test
    fun `instagram_com converts to instagram7_com when proxy=instagram7`() {
        val result = processor.processUrl(
            "https://instagram.com/p/abc",
            cleanTracking = false,
            convertTwitter = true,
            instagramProxy = Constants.INSTAGRAM7_DOMAIN
        ).first
        assertEquals("https://instagram7.com/p/abc", result)
    }

    // ---------------------------------------------------------------------
    // Cross-proxy: swap between proxies
    // ---------------------------------------------------------------------

    @Test
    fun `kkinstagram_com converts to eeinstagram_com when proxy=eeinstagram`() {
        val result = processor.processUrl(
            "https://kkinstagram.com/p/abc",
            cleanTracking = false,
            convertTwitter = true,
            instagramProxy = Constants.EEINSTAGRAM_DOMAIN
        ).first
        assertEquals("https://eeinstagram.com/p/abc", result)
    }

    @Test
    fun `eeinstagram_com converts to instagram7_com when proxy=instagram7`() {
        val result = processor.processUrl(
            "https://eeinstagram.com/p/abc",
            cleanTracking = false,
            convertTwitter = true,
            instagramProxy = Constants.INSTAGRAM7_DOMAIN
        ).first
        assertEquals("https://instagram7.com/p/abc", result)
    }

    @Test
    fun `instagram7_com converts to kkinstagram_com when proxy=kkinstagram`() {
        val result = processor.processUrl(
            "https://instagram7.com/p/abc",
            cleanTracking = false,
            convertTwitter = true,
            instagramProxy = Constants.KKINSTAGRAM_DOMAIN
        ).first
        assertEquals("https://kkinstagram.com/p/abc", result)
    }

    // ---------------------------------------------------------------------
    // No-op: proxy already matches target
    // ---------------------------------------------------------------------

    @Test
    fun `kkinstagram_com unchanged when proxy=kkinstagram`() {
        val url = "https://kkinstagram.com/p/abc"
        val result = processor.processUrl(
            url, cleanTracking = false, convertTwitter = true,
            instagramProxy = Constants.KKINSTAGRAM_DOMAIN
        ).first
        assertEquals(url, result)
    }

    @Test
    fun `eeinstagram_com unchanged when proxy=eeinstagram`() {
        val url = "https://eeinstagram.com/p/abc"
        val result = processor.processUrl(
            url, cleanTracking = false, convertTwitter = true,
            instagramProxy = Constants.EEINSTAGRAM_DOMAIN
        ).first
        assertEquals(url, result)
    }

    @Test
    fun `instagram7_com unchanged when proxy=instagram7`() {
        val url = "https://instagram7.com/p/abc"
        val result = processor.processUrl(
            url, cleanTracking = false, convertTwitter = true,
            instagramProxy = Constants.INSTAGRAM7_DOMAIN
        ).first
        assertEquals(url, result)
    }

    // ---------------------------------------------------------------------
    // Backward: any proxy -> instagram.com when convertTwitter=false
    // ---------------------------------------------------------------------

    @Test
    fun `kkinstagram_com reverts to instagram_com when convertTwitter=false`() {
        val result = processor.processUrl(
            "https://kkinstagram.com/p/abc",
            cleanTracking = false,
            convertTwitter = false,
            instagramProxy = Constants.EEINSTAGRAM_DOMAIN
        ).first
        assertEquals("https://instagram.com/p/abc", result)
    }

    @Test
    fun `eeinstagram_com reverts to instagram_com when convertTwitter=false`() {
        val result = processor.processUrl(
            "https://eeinstagram.com/p/abc",
            cleanTracking = false,
            convertTwitter = false,
            instagramProxy = Constants.KKINSTAGRAM_DOMAIN
        ).first
        assertEquals("https://instagram.com/p/abc", result)
    }

    @Test
    fun `instagram7_com reverts to instagram_com when convertTwitter=false`() {
        val result = processor.processUrl(
            "https://instagram7.com/p/abc",
            cleanTracking = false,
            convertTwitter = false,
            instagramProxy = Constants.KKINSTAGRAM_DOMAIN
        ).first
        assertEquals("https://instagram.com/p/abc", result)
    }

    // ---------------------------------------------------------------------
    // Dirty URL cleaning + proxy conversion
    // ---------------------------------------------------------------------

    @Test
    fun `dirty instagram url cleans tracking and converts to eeinstagram`() {
        val result = processor.processUrl(
            "https://instagram.com/p/abc?igshid=zzz&utm_source=ig",
            cleanTracking = true,
            convertTwitter = true,
            instagramProxy = Constants.EEINSTAGRAM_DOMAIN
        ).first
        assertEquals("https://eeinstagram.com/p/abc", result)
    }

    @Test
    fun `dirty instagram7 url cleans tracking without changing proxy`() {
        val result = processor.processUrl(
            "https://instagram7.com/p/abc?igshid=zzz&utm_source=ig",
            cleanTracking = true,
            convertTwitter = true,
            instagramProxy = Constants.INSTAGRAM7_DOMAIN
        ).first
        assertEquals("https://instagram7.com/p/abc", result)
    }

    // ---------------------------------------------------------------------
    // Subdomain preservation
    // ---------------------------------------------------------------------

    @Test
    fun `business instagram subdomain converts to business instagram7`() {
        val result = processor.processUrl(
            "https://business.instagram.com/p/abc",
            cleanTracking = false,
            convertTwitter = true,
            instagramProxy = Constants.INSTAGRAM7_DOMAIN
        ).first
        assertEquals("https://business.instagram7.com/p/abc", result)
    }

    // ---------------------------------------------------------------------
    // isInstagramUrl covers all proxies
    // ---------------------------------------------------------------------

    @Test
    fun `isInstagramUrl recognizes all three proxies and instagram_com`() {
        assertTrue(processor.isInstagramUrl("https://instagram.com/p/abc"))
        assertTrue(processor.isInstagramUrl("https://kkinstagram.com/p/abc"))
        assertTrue(processor.isInstagramUrl("https://eeinstagram.com/p/abc"))
        assertTrue(processor.isInstagramUrl("https://instagram7.com/p/abc"))
        assertFalse(processor.isInstagramUrl("https://example.com/p/abc"))
    }
}
