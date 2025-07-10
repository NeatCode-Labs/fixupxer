package com.fixupxer

import com.fixupxer.cleaners.CleanerService
import com.fixupxer.data.config.TrackingParameters
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UrlLogicTest {
    private lateinit var processor: UrlProcessor
    private lateinit var cleanerService: CleanerService
    
    @Before
    fun setup() {
        cleanerService = mock()
        
        // Mock behavior for these tests
        whenever(cleanerService.deepClean("https://www.instagram.com/p/abc/"))
            .thenReturn("https://www.instagram.com/p/abc/")
        whenever(cleanerService.wouldModifyUrl("https://www.instagram.com/p/abc/"))
            .thenReturn(false)
            
        whenever(cleanerService.deepClean("https://fixupx.com/user/status/123"))
            .thenReturn("https://fixupx.com/user/status/123")
        whenever(cleanerService.wouldModifyUrl("https://fixupx.com/user/status/123"))
            .thenReturn(false)
            
        whenever(cleanerService.deepClean("https://x.com/user/status/123?utm_source=abc"))
            .thenReturn("https://x.com/user/status/123")
        whenever(cleanerService.wouldModifyUrl("https://x.com/user/status/123?utm_source=abc"))
            .thenReturn(true)
            
        processor = UrlProcessor(cleanerService)
    }

    @Test
    fun cleanInstagramToggleOff_nothingToDo() {
        val (out, clean) = processor.processUrl(
            "https://www.instagram.com/p/abc/",
            cleanTracking = true,
            convertTwitter = false
        )
        assertTrue(clean)
        assertEquals("https://www.instagram.com/p/abc/", out)
    }

    @Test
    fun cleanFixupxToggleOn_nothingToDo() {
        val (out, clean) = processor.processUrl(
            "https://fixupx.com/user/status/123",
            cleanTracking = true,
            convertTwitter = true
        )
        assertTrue(clean)
        assertEquals("https://fixupx.com/user/status/123", out)
    }

    @Test
    fun dirtyXToggleOn_cleanAndConvert() {
        val (out, clean) = processor.processUrl(
            "https://x.com/user/status/123?utm_source=abc",
            cleanTracking = true,
            convertTwitter = true
        )
        assertFalse(clean)
        assertEquals("https://fixupx.com/user/status/123", out)
    }
} 