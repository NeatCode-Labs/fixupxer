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

import com.fixupxer.cleaners.impl.YouTubeCleaner
import org.junit.Assert.*
import org.junit.Test

class YouTubeCleanerTest {
    
    @Test
    fun `test YouTube cleaner matches YouTube URLs`() {
        // Should match
        assertTrue(YouTubeCleaner.matches("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(YouTubeCleaner.matches("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(YouTubeCleaner.matches("https://m.youtube.com/watch?v=test"))
        assertTrue(YouTubeCleaner.matches("https://music.youtube.com/watch?v=test"))
        assertTrue(YouTubeCleaner.matches("https://youtube-nocookie.com/embed/test"))
        
        // Should not match
        assertFalse(YouTubeCleaner.matches("https://google.com"))
        assertFalse(YouTubeCleaner.matches("https://vimeo.com"))
    }
    
    @Test
    fun `test YouTube cleaner preserves essential parameters`() {
        // Video with timestamp
        val videoWithTime = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=1m30s&utm_source=share"
        val cleanedTime = YouTubeCleaner.clean(videoWithTime)
        assertTrue(cleanedTime.contains("v=dQw4w9WgXcQ"))
        assertTrue(cleanedTime.contains("t=1m30s"))
        assertFalse(cleanedTime.contains("utm_source"))
        
        // Playlist video
        val playlistVideo = "https://www.youtube.com/watch?v=abc123&list=PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf&index=2&feature=share"
        val cleanedPlaylist = YouTubeCleaner.clean(playlistVideo)
        assertTrue(cleanedPlaylist.contains("v=abc123"))
        assertTrue(cleanedPlaylist.contains("list=PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf"))
        assertTrue(cleanedPlaylist.contains("index=2"))
        assertFalse(cleanedPlaylist.contains("feature=share"))
    }
    
    @Test
    fun `test YouTube short URL handling`() {
        // Short URL with timestamp
        val shortUrl = "https://youtu.be/dQw4w9WgXcQ?t=90&feature=share"
        val cleaned = YouTubeCleaner.clean(shortUrl)
        assertEquals("https://youtu.be/dQw4w9WgXcQ?t=90", cleaned)
        
        // Clean short URL
        val cleanShortUrl = "https://youtu.be/dQw4w9WgXcQ"
        val stillClean = YouTubeCleaner.clean(cleanShortUrl)
        assertEquals(cleanShortUrl, stillClean)
    }
    
    @Test
    fun `test YouTube Music handling`() {
        val musicUrl = "https://music.youtube.com/watch?v=abc123&list=RDAMVM&feature=share"
        val cleaned = YouTubeCleaner.clean(musicUrl)
        assertTrue(cleaned.contains("v=abc123"))
        assertTrue(cleaned.contains("list=RDAMVM"))
        assertFalse(cleaned.contains("feature=share"))
    }
    
    @Test
    fun `test YouTube search query preservation`() {
        val searchUrl = "https://www.youtube.com/results?search_query=rick+roll&sp=EgIQAQ%3D%3D"
        val cleaned = YouTubeCleaner.clean(searchUrl)
        assertTrue(cleaned.contains("search_query=rick+roll"))
        // sp parameter is removed as it's not in preserve list
        assertFalse(cleaned.contains("sp="))
    }
} 