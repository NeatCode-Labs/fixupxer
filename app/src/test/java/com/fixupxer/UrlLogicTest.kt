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