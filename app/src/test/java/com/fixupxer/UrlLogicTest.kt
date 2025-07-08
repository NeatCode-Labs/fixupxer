package com.fixupxer

import com.fixupxer.data.config.TrackingParameters
import org.junit.Assert.*
import org.junit.Test

class UrlLogicTest {
    private val processor = UrlProcessor()

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