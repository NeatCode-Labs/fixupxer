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

package com.fixupxer.ui.dialogs

import android.app.AlertDialog
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.utils.Constants

/**
 * Helper that shows a single-choice dialog letting the user pick
 * which Instagram embed proxy the app should use.
 *
 * Used inline by ShareActivity (whose `noHistory="true"` flag prevents
 * launching SettingsActivity without destroying the share flow), and as
 * a fallback in any build where SettingsActivity is unavailable.
 */
object InstagramProxyDialogHelper {

    fun show(
        context: Context,
        prefs: PreferencesManager,
        onChanged: (String) -> Unit
    ) {
        val domains = listOf(
            Constants.TOINSTAGRAM_DOMAIN,
            Constants.ADAMLIKES_DOMAIN,
            Constants.INSTAGRAM7_DOMAIN
        )
        val labels = arrayOf<CharSequence>(
            buildBadgedLabel(context, Constants.TOINSTAGRAM_DOMAIN, R.string.instagram_proxy_primary_label),
            buildBadgedLabel(context, Constants.ADAMLIKES_DOMAIN, R.string.instagram_proxy_primary_label),
            buildBadgedLabel(context, Constants.INSTAGRAM7_DOMAIN, R.string.instagram_proxy_backup_label)
        )

        val current = prefs.getInstagramProxy()
        val checked = domains.indexOf(current).coerceAtLeast(0)

        val customTitle = buildTitleView(context)
        customTitle.findViewById<ImageView>(R.id.instagramProxyInfoIcon)
            .setOnClickListener { showInfoDialog(context) }

        AlertDialog.Builder(context)
            .setCustomTitle(customTitle)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                val selected = domains[which]
                prefs.setInstagramProxy(selected)
                onChanged(selected)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Programmatically builds the dialog title row: title text + info icon. */
    private fun buildTitleView(context: Context): View {
        val padding = (context.resources.displayMetrics.density * 16).toInt()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(padding, padding, padding, padding / 2)
        }

        val title = TextView(context).apply {
            text = context.getString(R.string.instagram_proxy_dialog_title)
            setTextAppearance(android.R.style.TextAppearance_Material_Title)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val info = ImageView(context).apply {
            id = R.id.instagramProxyInfoIcon
            setImageResource(R.drawable.ic_info_outline)
            contentDescription = context.getString(R.string.instagram_proxy_info_content_desc)
            isClickable = true
            isFocusable = true
            val iconSize = (context.resources.displayMetrics.density * 32).toInt()
            val pad = (context.resources.displayMetrics.density * 6).toInt()
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            setPadding(pad, pad, pad, pad)
            // Borderless ripple
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless,
                outValue,
                true
            )
            setBackgroundResource(outValue.resourceId)
        }

        layout.addView(title)
        layout.addView(info)
        return layout
    }

    private fun showInfoDialog(context: Context) {
        val message = HtmlCompat.fromHtml(
            context.getString(R.string.instagram_proxy_info_text),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        AlertDialog.Builder(context)
            .setTitle(R.string.instagram_proxy_info_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun buildBadgedLabel(context: Context, domain: String, badgeRes: Int): CharSequence {
        val builder = SpannableStringBuilder()
        builder.append(domain)
        builder.append("  ")
        val badgeStart = builder.length
        builder.append("· ")
        builder.append(context.getString(badgeRes))
        // Smaller + italic badge
        builder.setSpan(RelativeSizeSpan(0.85f), badgeStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(StyleSpan(android.graphics.Typeface.ITALIC), badgeStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return builder
    }
}
