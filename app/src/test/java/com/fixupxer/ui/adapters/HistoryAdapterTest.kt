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

package com.fixupxer.ui.adapters

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import com.fixupxer.R
import com.fixupxer.databinding.ItemHistoryBinding
import com.fixupxer.domain.model.UrlHistory
import com.fixupxer.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HistoryAdapterTest {

    @Test
    fun `redacted entry shows privacy labels and normal entry resets recycled holder`() {
        val context = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            R.style.Theme_FixupXer,
        )
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(context), null, false)
        val holder = HistoryAdapter.HistoryViewHolder(
            binding = binding,
            onItemClick = null,
            onItemDelete = {},
            snackbarAnchor = binding.root,
        )
        val safeUrl = "https://example.com/article"
        val redactedItem = UrlHistory(
            id = 1L,
            originalUrl = safeUrl,
            cleanedUrl = safeUrl,
            platform = "Other",
            conversionType = Constants.HISTORY_CONVERSION_INPUT_REDACTED,
            timestamp = 1_000L,
            timeAgo = "1 min ago",
        )

        holder.bind(redactedItem)

        assertEquals(
            context.getString(R.string.history_sensitive_input_label),
            binding.textViewOriginalUrlLabel.text,
        )
        assertEquals(
            context.getString(R.string.history_original_url_not_saved),
            binding.textViewOriginalUrl.text,
        )
        assertEquals(
            context.getString(R.string.history_input_redacted_badge),
            binding.textViewConversionType.text,
        )
        assertEquals(safeUrl, binding.textViewProcessedUrl.text)

        val normalItem = UrlHistory(
            id = 2L,
            originalUrl = "https://example.com/page?utm_source=test",
            cleanedUrl = safeUrl,
            platform = "Other",
            conversionType = "Tracking removed",
            timestamp = 2_000L,
            timeAgo = "2 min ago",
        )
        holder.bind(normalItem)

        assertEquals(
            context.getString(R.string.original_url),
            binding.textViewOriginalUrlLabel.text,
        )
        assertEquals(normalItem.originalUrl, binding.textViewOriginalUrl.text)
        assertEquals(normalItem.conversionType, binding.textViewConversionType.text)
    }
}
