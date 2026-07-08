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

package com.fixupxer.domain

import com.fixupxer.domain.model.ResultStatus
import com.fixupxer.domain.model.resolveResultStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Edge cases of [resolveResultStatus], especially the subdomain-tolerant
 * host comparison (a subdomain normalization is CLEANED, a proxy domain
 * swap is CONVERTED — even for lookalike proxy names).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class ResultStatusTest {

    @Test
    fun `blank processed url yields null`() {
        assertNull(resolveResultStatus("https://example.com", ""))
        assertNull(resolveResultStatus("https://example.com", "   "))
    }

    @Test
    fun `identical urls are already clean`() {
        assertEquals(
            ResultStatus.ALREADY_CLEAN,
            resolveResultStatus("https://example.com/a", "https://example.com/a")
        )
    }

    @Test
    fun `subdomain normalization is cleaned not converted`() {
        assertEquals(
            ResultStatus.CLEANED,
            resolveResultStatus("https://m.facebook.com/story", "https://facebook.com/story")
        )
        assertEquals(
            ResultStatus.CLEANED,
            resolveResultStatus("https://mobile.twitter.com/x", "https://twitter.com/x")
        )
        // And the other direction (subdomain added).
        assertEquals(
            ResultStatus.CLEANED,
            resolveResultStatus("https://example.com/a", "https://sub.example.com/a")
        )
    }

    @Test
    fun `www prefix is ignored for comparison`() {
        assertEquals(
            ResultStatus.CLEANED,
            resolveResultStatus("https://www.example.com/a?utm_source=x", "https://example.com/a")
        )
    }

    @Test
    fun `lookalike proxy domain is converted despite shared suffix`() {
        // "toinstagram.com" must NOT be treated as a subdomain of "instagram.com".
        assertEquals(
            ResultStatus.CONVERTED,
            resolveResultStatus("https://instagram.com/p/abc/", "https://toinstagram.com/p/abc/")
        )
        assertEquals(
            ResultStatus.CONVERTED,
            resolveResultStatus("https://tiktok.com/@u/video/1", "https://kktiktok.com/@u/video/1")
        )
    }

    @Test
    fun `tiktok conversion preserving subdomain is converted`() {
        assertEquals(
            ResultStatus.CONVERTED,
            resolveResultStatus("https://vm.tiktok.com/ZM1abc/", "https://vm.tnktok.com/ZM1abc/")
        )
    }

    @Test
    fun `conversion with query removal is cleaned and converted`() {
        assertEquals(
            ResultStatus.CLEANED_AND_CONVERTED,
            resolveResultStatus(
                "https://www.instagram.com/p/abc/?igsh=xyz",
                "https://toinstagram.com/p/abc/"
            )
        )
    }

    @Test
    fun `malformed urls fall back to cleaned`() {
        assertEquals(
            ResultStatus.CLEANED,
            resolveResultStatus("not a url", "also not a url")
        )
    }
}
