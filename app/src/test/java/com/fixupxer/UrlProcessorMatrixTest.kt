package com.fixupxer

import android.net.Uri
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.CleanerRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

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
        val prefs = mock<com.fixupxer.PreferencesManager>()
        cleanerService = CleanerService(registry, cache, prefs)
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
            // Clean instagram.com
            Case("instagram clean, toggle OFF", "https://instagram.com/p/1", true, false, "https://instagram.com/p/1", true, true),
            Case("instagram clean, toggle ON", "https://instagram.com/p/1", true, true, "https://kkinstagram.com/p/1", false, false),
            // Dirty instagram.com
            Case("instagram dirty, toggle OFF", "https://instagram.com/p/1?utm_source=abc", true, false, "https://instagram.com/p/1", false, false),
            Case("instagram dirty, toggle ON", "https://instagram.com/p/1?utm_source=abc", true, true, "https://kkinstagram.com/p/1", false, false),
            // Clean kkinstagram.com
            Case("kkinstagram clean, toggle OFF", "https://kkinstagram.com/p/1", true, false, "https://instagram.com/p/1", false, false),
            Case("kkinstagram clean, toggle ON", "https://kkinstagram.com/p/1", true, true, "https://kkinstagram.com/p/1", true, true),
            // Dirty kkinstagram.com
            Case("kkinstagram dirty, toggle OFF", "https://kkinstagram.com/p/1?utm_source=abc", true, false, "https://instagram.com/p/1", false, false),
            Case("kkinstagram dirty, toggle ON", "https://kkinstagram.com/p/1?utm_source=abc", true, true, "https://kkinstagram.com/p/1", false, false),

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
            Case("fxtwitter.com dirty, toggle ON", "https://fxtwitter.com/user/status/1?utm_source=abc", true, true, "https://fixupx.com/user/status/1", false, false)
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