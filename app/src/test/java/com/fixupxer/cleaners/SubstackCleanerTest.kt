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

import com.fixupxer.cleaners.impl.SubstackCleaner
import org.junit.Assert.*
import org.junit.Test

class SubstackCleanerTest {
    
    @Test
    fun `test Substack cleaner matches Substack URLs`() {
        // Should match
        assertTrue(SubstackCleaner.matches("https://substack.com/app-link/post"))
        assertTrue(SubstackCleaner.matches("https://example.substack.com/p/some-post"))
        assertTrue(SubstackCleaner.matches("http://newsletter.substack.com"))
        
        // Should not match
        assertFalse(SubstackCleaner.matches("https://google.com"))
        assertFalse(SubstackCleaner.matches("https://twitter.com"))
    }
    
    @Test
    fun `test Substack cleaner preserves essential parameters`() {
        // URL with publication_id and post_id
        val url = "https://substack.com/app-link/post?publication_id=806546&post_id=165696748&utm_source=post-email-title"
        val cleaned = SubstackCleaner.clean(url)
        
        // Should preserve publication_id and post_id
        assertTrue(cleaned.contains("publication_id=806546"))
        assertTrue(cleaned.contains("post_id=165696748"))
        
        // Should remove tracking parameters
        assertFalse(cleaned.contains("utm_source"))
    }
    
    @Test
    fun `test Substack cleaner removes all tracking parameters`() {
        val dirtyUrl = """https://substack.com/app-link/post?publication_id=806546&post_id=165696748&utm_source=post-email-title&utm_campaign=email-post-title&isFreemail=true&r=1ez2n3&token=eyJ1c2VyX2lkIjo4NTYxNzE4MywicG9zdF9pZCI6MTY1Njk2NzQ4LCJpYXQiOjE3NDk2NDAzNjAsImV4cCI6MTc1MjIzMjM2MCwiaXNzIjoicHViLTgwNjU0NiIsInN1YiI6InBvc3QtcmVhY3Rpb24ifQ.CR78H3BGztpRqBf1lnnDlafH_popPsMlwvTLQvQC9l8"""
        
        val cleaned = SubstackCleaner.clean(dirtyUrl)
        
        // Should only have publication_id and post_id
        assertEquals("https://substack.com/app-link/post?publication_id=806546&post_id=165696748", cleaned)
    }
    
    @Test
    fun `test Substack cleaner handles URLs with no tracking parameters`() {
        // Clean URL with only essential parameters
        val cleanUrl = "https://substack.com/app-link/post?publication_id=123456&post_id=789012"
        val result = SubstackCleaner.clean(cleanUrl)
        
        // Should remain unchanged
        assertEquals(cleanUrl, result)
    }
    
    @Test
    fun `test Substack cleaner handles URLs with no parameters`() {
        // URL without parameters
        val url = "https://example.substack.com/p/some-article"
        val cleaned = SubstackCleaner.clean(url)
        
        // Should remain unchanged
        assertEquals(url, cleaned)
    }
    
    @Test
    fun `test Substack cleaner removes UTM parameters`() {
        val url = "https://newsletter.substack.com/p/article?utm_source=twitter&utm_medium=social&utm_campaign=share"
        val cleaned = SubstackCleaner.clean(url)
        
        // Should remove all UTM parameters
        assertEquals("https://newsletter.substack.com/p/article", cleaned)
    }
    
    @Test
    fun `test Substack cleaner handles fragment identifiers`() {
        val url = "https://substack.com/post?publication_id=123&post_id=456&utm_source=email#comments"
        val cleaned = SubstackCleaner.clean(url)
        
        // Should preserve fragment and essential params
        assertEquals("https://substack.com/post?publication_id=123&post_id=456#comments", cleaned)
    }
    
    @Test
    fun `test Substack cleaner handles only tracking parameters`() {
        // URL with only tracking parameters (no essential params)
        val url = "https://example.substack.com/p/post?utm_source=email&r=1234&token=abc123"
        val cleaned = SubstackCleaner.clean(url)
        
        // Should remove all parameters
        assertEquals("https://example.substack.com/p/post", cleaned)
    }
} 