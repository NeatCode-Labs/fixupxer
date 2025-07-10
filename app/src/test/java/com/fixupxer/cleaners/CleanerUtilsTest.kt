package com.fixupxer.cleaners

import com.fixupxer.cleaners.utils.CleanerUtils
import org.junit.Assert.*
import org.junit.Test

class CleanerUtilsTest {
    
    @Test
    fun `test fix URL with ampersand but no question mark`() {
        // Edge case: URL has & but no ?
        val malformedUrl = "https://example.com&utm_source=test&utm_campaign=promo"
        val fixed = CleanerUtils.preprocessUrl(malformedUrl)
        assertEquals("https://example.com?utm_source=test&utm_campaign=promo", fixed)
    }
    
    @Test
    fun `test fix URL with path and ampersand but no question mark`() {
        val malformedUrl = "https://example.com/page&param1=value1&param2=value2"
        val fixed = CleanerUtils.preprocessUrl(malformedUrl)
        assertEquals("https://example.com/page?param1=value1&param2=value2", fixed)
    }
    
    @Test
    fun `test dont fix URL with fragment containing ampersand`() {
        // Should not fix & in fragments
        val normalUrl = "https://example.com#section&subsection"
        val result = CleanerUtils.preprocessUrl(normalUrl)
        assertEquals(normalUrl, result)
    }
    
    @Test
    fun `test normal URL unchanged`() {
        val normalUrl = "https://example.com/page?param=value&other=test"
        val result = CleanerUtils.preprocessUrl(normalUrl)
        assertEquals(normalUrl, result)
    }
    
    @Test
    fun `test post process removes trailing delimiters`() {
        assertEquals("https://example.com", CleanerUtils.postProcessUrl("https://example.com?"))
        assertEquals("https://example.com", CleanerUtils.postProcessUrl("https://example.com#"))
        assertEquals("https://example.com", CleanerUtils.postProcessUrl("https://example.com&"))
        assertEquals("https://example.com", CleanerUtils.postProcessUrl("https://example.com?&#"))
    }
    
    @Test
    fun `test post process fixes double slashes in path`() {
        val urlWithDoubleSlash = "https://example.com//path//to///resource"
        val fixed = CleanerUtils.postProcessUrl(urlWithDoubleSlash)
        assertEquals("https://example.com/path/to/resource", fixed)
        
        // But not in protocol
        val protocolUrl = "https://example.com/path"
        assertEquals(protocolUrl, CleanerUtils.postProcessUrl(protocolUrl))
    }
    
    @Test
    fun `test split URL with edge cases`() {
        // Normal URL
        val (base1, query1, fragment1) = CleanerUtils.splitUrl("https://example.com/page?param=value#section")
        assertEquals("https://example.com/page", base1)
        assertEquals("param=value", query1)
        assertEquals("#section", fragment1)
        
        // URL with & but no ?
        val (base2, query2, fragment2) = CleanerUtils.splitUrl("https://example.com&param=value")
        assertEquals("https://example.com", base2)
        assertEquals("param=value", query2)
        assertEquals("", fragment2)
        
        // URL with no params
        val (base3, query3, fragment3) = CleanerUtils.splitUrl("https://example.com")
        assertEquals("https://example.com", base3)
        assertEquals("", query3)
        assertEquals("", fragment3)
    }
    
    @Test
    fun `test rebuild URL`() {
        // With parameters
        val url1 = CleanerUtils.rebuildUrl("https://example.com", listOf("param1=value1", "param2=value2"), "")
        assertEquals("https://example.com?param1=value1&param2=value2", url1)
        
        // Without parameters
        val url2 = CleanerUtils.rebuildUrl("https://example.com", emptyList(), "#section")
        assertEquals("https://example.com#section", url2)
        
        // Empty parameters should not add ?
        val url3 = CleanerUtils.rebuildUrl("https://example.com", emptyList(), "")
        assertEquals("https://example.com", url3)
    }
    
    @Test
    fun `test end to end URL cleaning with edge case`() {
        val malformedUrl = "https://example.com/page&utm_source=test&important=keep&utm_campaign=remove"
        
        // Split (which includes preprocessing)
        val (base, query, fragment) = CleanerUtils.splitUrl(malformedUrl)
        
        // Filter parameters (simulating what a cleaner would do)
        val filtered = query.split('&').filter { !it.startsWith("utm_") }
        
        // Rebuild
        val cleaned = CleanerUtils.rebuildUrl(base, filtered, fragment)
        
        assertEquals("https://example.com/page?important=keep", cleaned)
    }
} 