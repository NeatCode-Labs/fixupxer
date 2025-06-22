package com.fixupxer

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import timber.log.Timber

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UrlProcessorTest {
    private val urlProcessor = UrlProcessor()
    
    @Before
    fun setup() {
        // Initialize Timber for tests to prevent crashes
        if (Timber.treeCount == 0) {
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    // Print to console for debugging
                    println("$tag: $message")
                }
            })
        }
    }
    
    @Test
    fun `test remove tracking parameters from URL`() {
        val urlWithTracking = "https://example.com/page?utm_source=twitter&utm_campaign=test&ref=social"
        val expected = "https://example.com/page"
        val result = urlProcessor.processUrl(urlWithTracking, cleanTracking = true, convertTwitter = false)
        assertEquals(expected, result)
    }
    
    @Test
    fun `test keep non-tracking parameters`() {
        val urlWithParams = "https://example.com/search?q=kotlin&utm_source=google&page=2"
        val expected = "https://example.com/search?q=kotlin&page=2"
        val result = urlProcessor.processUrl(urlWithParams, cleanTracking = true, convertTwitter = false)
        assertEquals(expected, result)
    }
    
    @Test
    fun `test convert Twitter URL to FixupX`() {
        val twitterUrl = "https://twitter.com/user/status/1234567890"
        val expected = "https://fixupx.com/user/status/1234567890"
        val result = urlProcessor.processUrl(twitterUrl, cleanTracking = false, convertTwitter = true)
        assertEquals(expected, result)
    }
    
    @Test
    fun `test convert X com URL to FixupX`() {
        val xUrl = "https://x.com/user/status/1234567890"
        val expected = "https://fixupx.com/user/status/1234567890"
        val result = urlProcessor.processUrl(xUrl, cleanTracking = false, convertTwitter = true)
        assertEquals(expected, result)
    }
    
    @Test
    fun `test convert Instagram URL to kkinstagram`() {
        val instagramUrl = "https://www.instagram.com/p/ABC123/"
        val expected = "https://www.kkinstagram.com/p/ABC123/"
        val result = urlProcessor.processUrl(instagramUrl, cleanTracking = false, convertTwitter = true)
        assertEquals(expected, result)
    }
    
    @Test
    fun `test Instagram URL with tracking parameters`() {
        val instagramUrl = "https://instagram.com/p/ABC123/?igshid=abcdef&utm_source=ig_web"
        val expected = "https://kkinstagram.com/p/ABC123/"
        val result = urlProcessor.processUrl(instagramUrl, cleanTracking = true, convertTwitter = true)
        assertEquals(expected, result)
    }
    
    @Test
    fun `test already converted kkinstagram URL remains unchanged`() {
        val kkinstagramUrl = "https://kkinstagram.com/p/ABC123/"
        val result = urlProcessor.processUrl(kkinstagramUrl, cleanTracking = false, convertTwitter = true)
        assertEquals(kkinstagramUrl, result)
    }
    
    @Test
    fun `test Twitter URL with query parameters`() {
        val twitterUrl = "https://twitter.com/user/status/1234567890?s=20&t=abc123"
        val expected = "https://fixupx.com/user/status/1234567890"
        val result = urlProcessor.processUrl(twitterUrl, cleanTracking = true, convertTwitter = true)
        assertEquals(expected, result)
    }
    
    @Test
    fun `test non-status Twitter URL remains unchanged`() {
        val twitterProfileUrl = "https://twitter.com/user"
        val result = urlProcessor.processUrl(twitterProfileUrl, cleanTracking = false, convertTwitter = true)
        assertEquals(twitterProfileUrl, result)
    }
    
    @Test
    fun `test URL with multiple tracking parameters`() {
        val complexUrl = "https://example.com/page?fbclid=123&gclid=456&utm_source=fb&utm_medium=social&ref=tw&important=keep"
        val expected = "https://example.com/page?important=keep"
        val result = urlProcessor.processUrl(complexUrl, cleanTracking = true, convertTwitter = false)
        assertEquals(expected, result)
    }
    
    @Test
    fun `test Facebook URL with mibextid parameter`() {
        val fbUrl = "https://www.facebook.com/story.php?story_fbid=123&id=456&mibextid=abc"
        val expected = "https://www.facebook.com/story.php?story_fbid=123&id=456"
        val result = urlProcessor.processUrl(fbUrl, cleanTracking = true, convertTwitter = false)
        assertEquals(expected, result)
    }
    
    @Test
    fun `test Amazon URL with tracking`() {
        val amazonUrl = "https://www.amazon.com/dp/B08N5WRWNW?tag=affiliate-20&linkCode=ogi"
        val expected = "https://www.amazon.com/dp/B08N5WRWNW"
        val result = urlProcessor.processUrl(amazonUrl, cleanTracking = true, convertTwitter = false)
        assertEquals(expected, result)
    }
    
    @Test
    fun `test empty URL returns empty`() {
        val result = urlProcessor.processUrl("", cleanTracking = true, convertTwitter = true)
        assertEquals("", result)
    }
    
    @Test
    fun `test invalid URL returns original`() {
        val invalidUrl = "not a url"
        val result = urlProcessor.processUrl(invalidUrl, cleanTracking = true, convertTwitter = true)
        assertEquals(invalidUrl, result)
    }
    
    @Test
    fun `test URL with @ prefix for Instagram`() {
        val urlWithAt = "@https://instagram.com/p/ABC123/"
        val expected = "https://kkinstagram.com/p/ABC123/"
        val result = urlProcessor.processUrl(urlWithAt, cleanTracking = true, convertTwitter = true)
        assertEquals(expected, result)
    }
    
    @Test
    fun `test encoded URL is decoded`() {
        val encodedUrl = "https%3A%2F%2Fexample.com%2Fpage%3Futm_source%3Dtwitter"
        val expected = "https://example.com/page"
        val result = urlProcessor.processUrl(encodedUrl, cleanTracking = true, convertTwitter = false)
        assertEquals(expected, result)
    }
    
    @Test
    fun `test processUrlForSharing always cleans and converts`() {
        val twitterUrl = "https://twitter.com/user/status/123?utm_source=share"
        val expected = "https://fixupx.com/user/status/123"
        val result = urlProcessor.processUrlForSharing(twitterUrl)
        assertEquals(expected, result)
    }
    
    @Test
    fun `test Instagram subdomain handling`() {
        val businessUrl = "https://business.instagram.com/p/ABC123/"
        val expected = "https://business.kkinstagram.com/p/ABC123/"
        val result = urlProcessor.processUrl(businessUrl, cleanTracking = false, convertTwitter = true)
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