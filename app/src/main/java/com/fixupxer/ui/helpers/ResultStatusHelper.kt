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

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.fixupxer.R
import com.fixupxer.domain.model.ResultStatus
import com.google.android.material.color.MaterialColors

object ResultStatusHelper {

    fun bind(context: Context, statusView: TextView, status: ResultStatus?) {
        if (status == null) {
            statusView.visibility = View.GONE
            return
        }

        statusView.visibility = View.VISIBLE
        statusView.text = when (status) {
            ResultStatus.ALREADY_CLEAN -> context.getString(R.string.result_status_already_clean)
            ResultStatus.CLEANED -> context.getString(R.string.result_status_cleaned)
            ResultStatus.CONVERTED -> context.getString(R.string.result_status_converted)
            ResultStatus.CLEANED_AND_CONVERTED ->
                context.getString(R.string.result_status_cleaned_and_converted)
        }

        // The badge sits on the primary-container result card, so it uses the
        // solid primary/tertiary colors for contrast against that surface.
        val bgAttr = if (status == ResultStatus.ALREADY_CLEAN) {
            com.google.android.material.R.attr.colorTertiary
        } else {
            // colorPrimary is declared in appcompat (non-transitive R classes)
            androidx.appcompat.R.attr.colorPrimary
        }
        val textAttr = if (status == ResultStatus.ALREADY_CLEAN) {
            com.google.android.material.R.attr.colorOnTertiary
        } else {
            com.google.android.material.R.attr.colorOnPrimary
        }

        statusView.background = ContextCompat.getDrawable(context, R.drawable.bg_result_status)
        statusView.backgroundTintList = android.content.res.ColorStateList.valueOf(
            MaterialColors.getColor(statusView, bgAttr)
        )
        statusView.setTextColor(MaterialColors.getColor(statusView, textAttr))
    }
}
