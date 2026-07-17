// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
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

import com.fixupxer.UrlProcessor
import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.cleaners.impl.GoogleMapsCleaner
import com.fixupxer.cleaners.impl.GoogleSearchCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleMapsCleanerTest {
    @Test
    fun `canonicalizes coordinate segments and intentionally drops query fragment`() {
        val input = "http://maps.google.com/place/Example/@45.123,-93.456,15.5z/data=x" +
            "?keep=raw+plus%26value&keep=duplicate#frag%2Fvalue"

        assertEquals(
            "http://www.google.com/maps/@45.123,-93.456,15.5z",
            GoogleMapsCleaner.clean(input)
        )
    }

    @Test
    fun `matches only supported maps hosts paths and coordinate segments`() {
        assertTrue(GoogleMapsCleaner.matches("https://www.google.com/maps/@1,2,17z"))
        assertTrue(GoogleMapsCleaner.matches("https://maps.google.com/place/X/@-1.5,2.25,15z"))
        assertFalse(GoogleMapsCleaner.matches("https://www.google.com/maps-other/@1,2,17z"))
        assertFalse(GoogleMapsCleaner.matches("https://notgoogle.com/maps/@1,2,17z"))
        assertFalse(GoogleMapsCleaner.matches("https://mymaps.google.com.evil.com/@1,2,17z"))
        assertFalse(GoogleMapsCleaner.matches("https://maps.google.com/@one,2,17z"))
    }

    @Test
    fun `leaves malformed or coordinate-free maps URLs unchanged`() {
        val malformed = "https://maps.google.com/maps/@one,2,17z"
        val coordinateFree = "https://www.google.com/maps/search/cafe?query=coffee#section"

        assertEquals(malformed, GoogleMapsCleaner.clean(malformed))
        assertEquals(coordinateFree, GoogleMapsCleaner.clean(coordinateFree))
    }

    @Test
    fun `canonicalization is idempotent and does not overlap Google Search`() {
        val input = "https://www.google.com/maps/@45.0,16.0,17z?query=coffee#section"
        val cleaned = GoogleMapsCleaner.clean(input)

        assertEquals(cleaned, GoogleMapsCleaner.clean(cleaned))
        assertFalse(GoogleSearchCleaner.matches(input))
    }

    @Test
    fun `Google Maps cleaner runs through UrlProcessor pipeline`() {
        val registry = CleanerRegistry().apply {
            registerAll(CleanerCatalog.createBuiltInCleaners())
        }
        val processor = UrlProcessor(CleanerService(registry, CleanerCache()))

        assertEquals(
            "https://www.google.com/maps/@45.0,16.0,17z",
            processor.processUrl(
                "https://maps.google.com/place/X/@45.0,16.0,17z?query=coffee#section",
                cleanTracking = true,
                convertTwitter = false
            ).first
        )
    }
}
