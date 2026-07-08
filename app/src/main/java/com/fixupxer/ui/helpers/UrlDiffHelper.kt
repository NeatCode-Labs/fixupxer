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

import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import timber.log.Timber

/**
 * Display-only helper for the "before" card of the flow layout: strikes through
 * the query parameters of the original text that were removed during cleaning,
 * so the user can see at a glance what got stripped.
 */
object UrlDiffHelper {

    /**
     * @param originalText the shared text as displayed (contains the original URL)
     * @param cleanedUrl the processed result, or empty if not processed yet
     * @return [originalText] with removed query params struck through; the plain
     *         text on any parse trouble. The character content is never altered.
     */
    fun strikeRemovedParams(originalText: String, cleanedUrl: String): CharSequence {
        val ranges = strikeRanges(originalText, cleanedUrl)
        if (ranges.isEmpty()) return originalText
        val spannable = SpannableString(originalText)
        for ((start, end) in ranges) {
            spannable.setSpan(StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannable
    }

    /**
     * Same visual as [strikeRemovedParams], but applied in place to an editable
     * text (Main screen input field). Only spans are touched — the character
     * content is never altered, so no TextWatcher fires. Passing an empty
     * [cleanedUrl] clears any previously applied strikes.
     */
    fun applyStrikesInPlace(editable: Editable, cleanedUrl: String) {
        editable.getSpans(0, editable.length, StrikethroughSpan::class.java)
            .forEach(editable::removeSpan)
        for ((start, end) in strikeRanges(editable.toString(), cleanedUrl)) {
            editable.setSpan(StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /** (startInclusive, endExclusive) pairs of removed query params in [text]. */
    private fun strikeRanges(text: String, cleanedUrl: String): List<Pair<Int, Int>> {
        if (text.isEmpty() || cleanedUrl.isEmpty()) return emptyList()
        return try {
            val queryStart = text.indexOf('?')
            if (queryStart == -1) return emptyList()

            // Query segment ends at the first whitespace or '#' after '?'
            // (shared text can contain words around the URL; a fragment is
            // not a query param).
            var queryEnd = text.length
            for (i in (queryStart + 1) until text.length) {
                if (text[i].isWhitespace() || text[i] == '#') {
                    queryEnd = i
                    break
                }
            }

            // Exact param set of the cleaned URL. Substring matching would
            // leave a removed param un-struck whenever its text happens to
            // appear anywhere inside the cleaned URL (e.g. "t=1" matching
            // "...?xt=123..."). No query in the cleaned URL means every
            // original param was removed.
            val keptParams = cleanedUrl.substringAfter('?', "")
                .substringBefore('#')
                .split("&")
                .filterTo(mutableSetOf()) { it.isNotEmpty() }

            val ranges = mutableListOf<Pair<Int, Int>>()
            val query = text.substring(queryStart + 1, queryEnd)
            var offset = queryStart + 1
            for (param in query.split("&")) {
                if (param.isNotEmpty() && param !in keptParams) {
                    // Include the preceding '?'/'&' separator in the strike.
                    ranges.add(offset - 1 to (offset + param.length).coerceAtMost(queryEnd))
                }
                offset += param.length + 1
            }
            ranges
        } catch (e: Exception) {
            Timber.w(e, "Failed to build URL diff display")
            emptyList()
        }
    }
}
