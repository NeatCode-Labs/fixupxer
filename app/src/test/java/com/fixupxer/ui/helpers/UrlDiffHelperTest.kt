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

package com.fixupxer.ui.helpers

import android.text.Spanned
import android.text.SpannableStringBuilder
import android.text.style.StrikethroughSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class UrlDiffHelperTest {

    /** The substrings of [text] covered by strikethrough spans. */
    private fun struckParts(text: CharSequence): List<String> {
        if (text !is Spanned) return emptyList()
        return text.getSpans(0, text.length, StrikethroughSpan::class.java)
            .map { text.subSequence(text.getSpanStart(it), text.getSpanEnd(it)).toString() }
            .sorted()
    }

    @Test
    fun `removed params are struck, kept params are not`() {
        val original = "https://example.com/page?id=42&utm_source=nl&utm_medium=email"
        val cleaned = "https://example.com/page?id=42"

        val result = UrlDiffHelper.strikeRemovedParams(original, cleaned)

        assertEquals(result.toString(), original) // content never altered
        assertEquals(listOf("&utm_medium=email", "&utm_source=nl"), struckParts(result))
    }

    @Test
    fun `no query means no spans`() {
        val original = "https://example.com/page"
        val result = UrlDiffHelper.strikeRemovedParams(original, "https://example.com/")
        assertTrue(struckParts(result).isEmpty())
    }

    @Test
    fun `empty cleaned url means no spans`() {
        val original = "https://example.com/page?utm_source=x"
        val result = UrlDiffHelper.strikeRemovedParams(original, "")
        assertTrue(struckParts(result).isEmpty())
    }

    @Test
    fun `all params struck when cleaned url has no query`() {
        val original = "https://example.com/p?utm_source=x&fbclid=abc"
        val cleaned = "https://example.com/p"

        val result = UrlDiffHelper.strikeRemovedParams(original, cleaned)

        assertEquals(listOf("&fbclid=abc", "?utm_source=x"), struckParts(result))
    }

    @Test
    fun `removed param is struck even when its text appears elsewhere in cleaned url`() {
        // "t=1" is a substring of "xt=1" — naive contains() would skip the strike.
        val original = "https://example.com/p?t=1&xt=1"
        val cleaned = "https://example.com/p?xt=1"

        val result = UrlDiffHelper.strikeRemovedParams(original, cleaned)

        assertEquals(listOf("?t=1"), struckParts(result))
    }

    @Test
    fun `fragment is not treated as part of the last param`() {
        val original = "https://example.com/p?utm_source=x#section"
        val cleaned = "https://example.com/p#section"

        val result = UrlDiffHelper.strikeRemovedParams(original, cleaned)

        assertEquals(listOf("?utm_source=x"), struckParts(result))
    }

    @Test
    fun `query segment ends at whitespace in shared text`() {
        val original = "Look: https://example.com/p?utm_source=x more words"
        val cleaned = "https://example.com/p"

        val result = UrlDiffHelper.strikeRemovedParams(original, cleaned)

        assertEquals(listOf("?utm_source=x"), struckParts(result))
    }

    @Test
    fun `applyStrikesInPlace adds and clears spans without changing text`() {
        val editable = SpannableStringBuilder("https://example.com/p?utm_source=x&id=1")

        UrlDiffHelper.applyStrikesInPlace(editable, "https://example.com/p?id=1")
        assertEquals(listOf("?utm_source=x"), struckParts(editable))
        assertEquals("https://example.com/p?utm_source=x&id=1", editable.toString())

        // Empty cleaned URL clears previously applied strikes.
        UrlDiffHelper.applyStrikesInPlace(editable, "")
        assertTrue(struckParts(editable).isEmpty())
    }
}
