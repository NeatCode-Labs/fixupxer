package com.fixupxer.cleaners

import com.fixupxer.cleaners.impl.AmazonCleaner
import org.junit.Assert.*
import org.junit.Test

class AmazonCleanerTest {
    
    @Test
    fun `test Amazon cleaner matches Amazon URLs`() {
        // Should match
        assertTrue(AmazonCleaner.matches("https://www.amazon.com/dp/B00XXXXXX"))
        assertTrue(AmazonCleaner.matches("https://amazon.co.uk/product"))
        assertTrue(AmazonCleaner.matches("https://amzn.to/shortlink"))
        
        // Should not match
        assertFalse(AmazonCleaner.matches("https://google.com"))
        assertFalse(AmazonCleaner.matches("https://example.com"))
    }
    
    @Test
    fun `test Amazon cleaner extracts product ID and creates clean URL`() {
        // Product URL with tracking
        val dirtyUrl = "https://www.amazon.com/dp/B08N5WRWNW?utm_source=google&utm_medium=cpc&ref=ppx_yo2ov"
        val cleaned = AmazonCleaner.clean(dirtyUrl)
        assertEquals("https://www.amazon.com/dp/B08N5WRWNW", cleaned)
        
        // Different product URL format
        val gpUrl = "https://www.amazon.com/gp/product/B08N5WRWNW?ref=ox_sc_act_title_1&smid=ATVPDKIKX0DER"
        val cleanedGp = AmazonCleaner.clean(gpUrl)
        assertEquals("https://www.amazon.com/dp/B08N5WRWNW", cleanedGp)
    }
    
    @Test
    fun `test Amazon cleaner preserves search parameters`() {
        // Search URL with tracking
        val searchUrl = "https://www.amazon.com/s?k=laptop&utm_source=google&ref=nav_logo"
        val cleaned = AmazonCleaner.clean(searchUrl)
        
        // Should preserve 'k' parameter but remove tracking
        assertTrue(cleaned.contains("k=laptop"))
        assertFalse(cleaned.contains("utm_source"))
        assertFalse(cleaned.contains("ref="))
    }
    
    @Test
    fun `test Amazon cleaner handles international domains`() {
        // UK Amazon
        val ukUrl = "https://www.amazon.co.uk/dp/B08N5WRWNW?tag=affiliate-20"
        val cleanedUk = AmazonCleaner.clean(ukUrl)
        assertEquals("https://www.amazon.co.uk/dp/B08N5WRWNW", cleanedUk)
        
        // German Amazon
        val deUrl = "https://www.amazon.de/dp/B08N5WRWNW?language=de_DE"
        val cleanedDe = AmazonCleaner.clean(deUrl)
        assertEquals("https://www.amazon.de/dp/B08N5WRWNW", cleanedDe)
    }
} 