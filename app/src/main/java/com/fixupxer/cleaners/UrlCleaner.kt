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

/**
 * Interface for modular URL cleaners.
 * Each cleaner handles specific domains or URL patterns.
 */
interface UrlCleaner {
    /**
     * Unique identifier for this cleaner
     */
    val id: String
    
    /**
     * Category for grouping cleaners in UI
     */
    val category: CleanerCategory
        get() = CleanerCategory.OTHER
    
    /**
     * Check if this cleaner should process the given URL.
     * This should be a cheap operation (e.g., simple string contains).
     * 
     * @param url The URL to check
     * @return true if this cleaner can handle the URL
     */
    fun matches(url: String): Boolean
    
    /**
     * Clean the URL by removing tracking parameters or extracting the actual URL.
     * This should be a pure, stateless function.
     * 
     * @param url The URL to clean
     * @return The cleaned URL
     */
    fun clean(url: String): String
}

/**
 * Categories for grouping cleaners
 */
enum class CleanerCategory(val displayName: String) {
    SOCIAL_MEDIA("Social Media"),
    E_COMMERCE("E-Commerce"),
    SEARCH_ENGINES("Search Engines"),
    VIDEO_PLATFORMS("Video Platforms"),
    NEWS_MEDIA("News & Media"),
    GENERAL("General"),
    OTHER("Other")
} 