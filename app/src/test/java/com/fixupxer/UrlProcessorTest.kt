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

import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.CleanerRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class UrlProcessorTest {
    private lateinit var urlProcessor: UrlProcessor
    private lateinit var cleanerService: CleanerService
    
    @Before
    fun setup() {
        // Build real CleanerService with all cleaners
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
        val cache = com.fixupxer.cleaners.cache.CleanerCache()
        val preferences = mock<com.fixupxer.PreferencesManager>()
        val cleanerServiceReal = com.fixupxer.cleaners.CleanerService(registry, cache, preferences)

        cleanerService = cleanerServiceReal
        urlProcessor = UrlProcessor(cleanerService)
    }
    
    @Test
    fun `test remove tracking parameters from URL`() {
        val urlWithTracking = "https://example.com/page?utm_source=twitter&utm_campaign=test&ref=social"
        val expected = "https://example.com/page"
        val result = urlProcessor.processUrl(urlWithTracking, cleanTracking = true, convertTwitter = false).first
        assertEquals(expected, result)
    }
    
    @Test
    fun `test keep non-tracking parameters`() {
        val urlWithParams = "https://example.com/search?q=kotlin&utm_source=google&page=2"
        // No extra stubbing needed; real CleanerService will remove tracking params but keep q & page
        
        val expected = "https://example.com/search?q=kotlin&page=2"
        val result = urlProcessor.processUrl(urlWithParams, cleanTracking = true, convertTwitter = false).first
        assertEquals(expected, result)
    }
    
    @Test
    fun `test Gmail Google redirect with nested URL is accepted and extracted`() {
        val gmailRedirect = "https://www.google.com/url?q=https://gls-group.com/HR/hr/pracenje-posiljke?match%3D48610661969&source=gmail&ust=1777816100096000&usg=AOvVaw3ti1EXaAqM2E3SY72ndZNy"
        
        val result = urlProcessor.processUrl(gmailRedirect, cleanTracking = true, convertTwitter = false).first
        
        assertEquals("https://gls-group.com/HR/hr/pracenje-posiljke?match=48610661969", result)
    }
    
    @Test
    fun `test static URL extraction accepts Google redirect with nested URL`() {
        val gmailRedirect = "https://www.google.com/url?q=https://gls-group.com/HR/hr/pracenje-posiljke?match%3D48610661969&source=gmail"
        
        val result = UrlProcessor.findFirstValidUrl(gmailRedirect)
        
        assertEquals(gmailRedirect, result)
    }
    
    @Test
    fun `test convert Twitter URL to FixupX`() {
        val twitterUrl = "https://twitter.com/user/status/1234567890"
        val resultPair = urlProcessor.processUrl(twitterUrl, cleanTracking = false, convertTwitter = true)
        println("Converted Twitter result: ${resultPair.first}")
        val expected = "https://fixupx.com/user/status/1234567890"
        assertEquals(expected, resultPair.first)
    }
    
    @Test
    fun `test convert TikTok URL to kktiktok`() {
        val tiktokUrl = "https://www.tiktok.com/t/ZTB4YXjhF/"
        val resultPair = urlProcessor.processUrl(tiktokUrl, cleanTracking = false, convertTwitter = true)
        println("Converted TikTok result: ${resultPair.first}")
        val expected = "https://www.kktiktok.com/t/ZTB4YXjhF/"
        assertEquals(expected, resultPair.first)
    }
    
    @Test
    fun `test convert vm TikTok URL to kktiktok`() {
        val tiktokUrl = "https://vm.tiktok.com/ZTB4YXjhF/"
        val resultPair = urlProcessor.processUrl(tiktokUrl, cleanTracking = false, convertTwitter = true)
        println("Converted TikTok result: ${resultPair.first}")
        val expected = "https://vm.kktiktok.com/ZTB4YXjhF/"
        assertEquals(expected, resultPair.first)
    }

    @Test
    fun `test convert kktiktok URL back to tiktok`() {
        val proxyUrl = "https://www.kktiktok.com/t/ZTB4YXjhF/"
        val resultPair = urlProcessor.processUrl(proxyUrl, cleanTracking = false, convertTwitter = false)
        println("Reverted TikTok result: ${resultPair.first}")
        val expected = "https://www.tiktok.com/t/ZTB4YXjhF/"
        assertEquals(expected, resultPair.first)
    }
    
    @Test
    fun `test convert X com URL to FixupX`() {
        val xUrl = "https://x.com/user/status/1234567890"
        val resultPair = urlProcessor.processUrl(xUrl, cleanTracking = false, convertTwitter = true)
        println("Converted X result: ${resultPair.first}")
        val expected = "https://fixupx.com/user/status/1234567890"
        assertEquals(expected, resultPair.first)
    }
    
    @Test
    fun `test convert Instagram URL to default proxy strips www`() {
        // v1.4.8: www. is stripped on conversion + default proxy = toinstagram.com
        val instagramUrl = "https://www.instagram.com/p/ABC123/"
        val expected = "https://toinstagram.com/p/ABC123/"
        val result = urlProcessor.processUrl(instagramUrl, cleanTracking = false, convertTwitter = true).first
        assertEquals(expected, result)
    }
    
    @Test
    fun `test Instagram URL with tracking parameters`() {
        val instagramUrl = "https://instagram.com/p/ABC123/?igshid=abcdef&utm_source=ig_web"
        val expected = "https://toinstagram.com/p/ABC123/"
        val result = urlProcessor.processUrl(instagramUrl, cleanTracking = true, convertTwitter = true).first
        assertEquals(expected, result)
    }
    
    @Test
    fun `test bare toinstagram URL remains unchanged when converting`() {
        val proxyUrl = "https://toinstagram.com/p/ABC123/"
        val result = urlProcessor.processUrl(proxyUrl, cleanTracking = false, convertTwitter = true).first
        assertEquals(proxyUrl, result)
    }
    
    @Test
    fun `test Twitter URL with query parameters`() {
        val twitterUrl = "https://twitter.com/user/status/1234567890?s=20&t=abc123"
        // No stubbing needed
        
        val expected = "https://fixupx.com/user/status/1234567890"
        val result = urlProcessor.processUrl(twitterUrl, cleanTracking = true, convertTwitter = true).first
        assertEquals(expected, result)
    }
    
    @Test
    fun `test non-status Twitter URL remains unchanged`() {
        val twitterProfileUrl = "https://twitter.com/user"
        val expected = twitterProfileUrl // profile URLs should not be converted
        val result = urlProcessor.processUrl(twitterProfileUrl, cleanTracking = false, convertTwitter = true).first
        assertEquals(expected, result)
    }
    
    @Test
    fun `test URL with multiple tracking parameters`() {
        val complexUrl = "https://example.com/page?fbclid=123&gclid=456&utm_source=fb&utm_medium=social&ref=tw&important=keep"
        // No stubbing needed
        
        val expected = "https://example.com/page?important=keep"
        val result = urlProcessor.processUrl(complexUrl, cleanTracking = true, convertTwitter = false).first
        assertEquals(expected, result)
    }
    
    @Test
    fun `test Facebook URL with mibextid parameter`() {
        val fbUrl = "https://www.facebook.com/story.php?story_fbid=123&id=456&mibextid=abc"
        // No stubbing needed
        
        val expected = "https://www.facebook.com/story.php?story_fbid=123&id=456"
        val result = urlProcessor.processUrl(fbUrl, cleanTracking = true, convertTwitter = false).first
        assertEquals(expected, result)
    }
    
    @Test
    fun `test Amazon URL with tracking`() {
        val amazonUrl = "https://www.amazon.com/dp/B08N5WRWNW?tag=affiliate-20&linkCode=ogi"
        // No stubbing needed
        
        val expected = "https://www.amazon.com/dp/B08N5WRWNW"
        val result = urlProcessor.processUrl(amazonUrl, cleanTracking = true, convertTwitter = false).first
        assertEquals(expected, result)
    }
    
    @Test
    fun `test empty URL returns empty`() {
        try {
            urlProcessor.processUrl("", cleanTracking = true, convertTwitter = true).first
            fail("Expected IllegalArgumentException for empty URL")
        } catch (e: IllegalArgumentException) {
            assertEquals("Please enter a URL", e.message)
        }
    }
    
    @Test
    fun `test invalid URL returns original`() {
        val invalidUrl = "not a url"
        try {
            urlProcessor.processUrl(invalidUrl, cleanTracking = true, convertTwitter = true).first
            fail("Expected IllegalArgumentException for invalid URL")
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid URL format", e.message)
        }
    }
    
    @Test
    fun `test URL with @ prefix for Instagram`() {
        val urlWithAt = "@https://instagram.com/p/ABC123/"
        val expected = "https://toinstagram.com/p/ABC123/"
        val result = urlProcessor.processUrl(urlWithAt, cleanTracking = true, convertTwitter = true).first
        assertEquals(expected, result)
    }
    
    @Test
    fun `test encoded URL is decoded`() {
        val encodedUrl = "https%3A%2F%2Fexample.com%2Fpage%3Futm_source%3Dtwitter"
        try {
            urlProcessor.processUrl(encodedUrl, cleanTracking = true, convertTwitter = false).first
            fail("Expected IllegalArgumentException for invalid encoded URL")
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid URL format", e.message)
        }
    }
    
    @Test
    fun `test processUrlForSharing always cleans and converts`() {
        val twitterUrl = "https://twitter.com/user/status/123?utm_source=share"
        // No stubbing needed
        
        val expected = "https://fixupx.com/user/status/123"
        val result = urlProcessor.processUrlForSharing(twitterUrl)
        assertEquals(expected, result)
    }
    
    @Test
    fun `test Instagram subdomain stripped on conversion`() {
        // v1.4.8: any host-level prefix (www., business., etc.) is dropped when converting to a proxy
        val businessUrl = "https://business.instagram.com/p/ABC123/"
        val expected = "https://toinstagram.com/p/ABC123/"
        val result = urlProcessor.processUrl(businessUrl, cleanTracking = false, convertTwitter = true).first
        assertEquals(expected, result)
    }
    
    @Test
    fun `test extract URLs from text`() {
        val text = "Check out https://example.com and http://test.org/page"
        val urls = UrlProcessor.extractUrls(text)
        assertEquals(2, urls.size)
        assertEquals("https://example.com", urls[0])
        assertEquals("http://test.org/page", urls[1])
    }
} 