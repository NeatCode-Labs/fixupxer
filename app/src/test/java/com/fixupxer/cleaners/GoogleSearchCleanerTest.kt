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


package com.fixupxer.cleaners

import com.fixupxer.cleaners.impl.GoogleSearchCleaner
import org.junit.Assert.*
import org.junit.Test

class GoogleSearchCleanerTest {
    
    @Test
    fun `test Google Search cleaner matches Google URLs`() {
        // Should match redirect URLs
        assertTrue(GoogleSearchCleaner.matches("https://www.google.com/url?q=https://example.com"))
        assertTrue(GoogleSearchCleaner.matches("https://google.co.uk/url?q=test"))
        assertTrue(GoogleSearchCleaner.matches("https://google.de/url?q=test"))
        
        // Should match search URLs
        assertTrue(GoogleSearchCleaner.matches("https://www.google.com/search?q=test"))
        assertTrue(GoogleSearchCleaner.matches("https://google.com/search?q=test"))
        
        // Should not match non-search/redirect URLs
        assertFalse(GoogleSearchCleaner.matches("https://www.google.com/maps"))
        assertFalse(GoogleSearchCleaner.matches("https://mail.google.com"))
        assertFalse(GoogleSearchCleaner.matches("https://bing.com/search?q=test"))
    }
    
    @Test
    fun `test Google Search URL extraction`() {
        // Standard redirect URL
        val redirectUrl = "https://www.google.com/url?q=https%3A%2F%2Fexample.com%2Fpage%3Fid%3D123&sa=D&source=editors&ust=1234567890"
        val extracted = GoogleSearchCleaner.clean(redirectUrl)
        assertEquals("https://example.com/page?id=123", extracted)
        
        // URL with tracking parameters (GoogleSearchCleaner only extracts, doesn't clean)
        val redirectWithTracking = "https://www.google.com/url?q=https%3A%2F%2Fexample.com%3Futm_source%3Dgoogle&sa=D"
        val extractedClean = GoogleSearchCleaner.clean(redirectWithTracking)
        // GoogleSearchCleaner extracts but doesn't clean - CleanerService handles that
        assertEquals("https://example.com?utm_source=google", extractedClean)
    }
    
    @Test
    fun `test Google Search query preservation`() {
        // Search URL with various parameters
        val searchUrl = "https://www.google.com/search?q=kotlin+android&tbm=isch&safe=active&hl=en&utm_source=app"
        val cleaned = GoogleSearchCleaner.clean(searchUrl)
        
        assertTrue(cleaned.contains("q=kotlin+android"))
        assertTrue(cleaned.contains("tbm=isch")) // Image search
        assertTrue(cleaned.contains("safe=active"))
        assertTrue(cleaned.contains("hl=en"))
        assertFalse(cleaned.contains("utm_source"))
    }
    
    @Test
    fun `test Google Search handles malformed URLs`() {
        // Malformed redirect URL - when extraction fails, it cleans the Google URL but keeps 'q' param
        val malformedUrl = "https://www.google.com/url?q=not-a-url&sa=D"
        val result = GoogleSearchCleaner.clean(malformedUrl)
        // Should clean tracking params but keep 'q' since it's in preserveParams
        assertEquals("https://www.google.com/url?q=not-a-url", result)
        
        // Missing q parameter - just has tracking params
        val missingParam = "https://www.google.com/url?sa=D&source=test"
        val resultMissing = GoogleSearchCleaner.clean(missingParam)
        // Should clean all params since none are in preserveParams
        assertEquals("https://www.google.com/url", resultMissing)
    }
    
    @Test
    fun `test Google Search handles international domains`() {
        // UK domain
        val ukUrl = "https://www.google.co.uk/url?q=https%3A%2F%2Fbbc.co.uk"
        val ukExtracted = GoogleSearchCleaner.clean(ukUrl)
        assertEquals("https://bbc.co.uk", ukExtracted)
        
        // German domain
        val deUrl = "https://www.google.de/search?q=test&hl=de"
        val deCleaned = GoogleSearchCleaner.clean(deUrl)
        assertTrue(deCleaned.contains("q=test"))
        assertTrue(deCleaned.contains("hl=de"))
    }
} 