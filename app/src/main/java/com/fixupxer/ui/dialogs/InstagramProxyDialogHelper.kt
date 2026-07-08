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

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.widget.TextViewCompat
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InstagramProxyStore
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Helper that shows a single-choice dialog letting the user pick which Instagram
 * embed proxy the app should use. The roster is built dynamically:
 * fixed proxies from [Constants] (Primary/Backup badges) + user-defined custom
 * proxies (Custom badge, deletable) + a trailing "Add custom proxy…" row.
 *
 * Used inline by ShareActivity (whose `noHistory="true"` flag prevents
 * launching SettingsActivity without destroying the share flow) and MainActivity.
 */
object InstagramProxyDialogHelper {

    private sealed class Row {
        data class Proxy(val domain: String, val badgeRes: Int, val isCustom: Boolean) : Row()
        object AddCustom : Row()
    }

    fun show(
        context: Context,
        prefs: PreferencesManager,
        onChanged: (String) -> Unit
    ) {
        val customTitle = buildTitleView(context)
        customTitle.findViewById<ImageView>(R.id.instagramProxyInfoIcon)
            .setOnClickListener { showInfoDialog(context) }

        lateinit var dialog: AlertDialog
        lateinit var adapter: ProxyListAdapter

        adapter = ProxyListAdapter(
            context = context,
            prefs = prefs,
            onDeleted = { deletedDomain ->
                val selectedBefore = prefs.getInstagramProxy()
                prefs.removeCustomInstagramProxy(deletedDomain)
                adapter.reload()
                // Deleting the selected proxy silently falls back to the default —
                // let the caller refresh its "Active: <proxy>." label.
                val selectedAfter = prefs.getInstagramProxy()
                if (selectedAfter != selectedBefore) {
                    onChanged(selectedAfter)
                }
            }
        )

        dialog = MaterialAlertDialogBuilder(context)
            .setCustomTitle(customTitle)
            .setAdapter(adapter) { _, _ -> /* handled via list click listener below */ }
            .setNegativeButton(R.string.cancel, null)
            .create()

        // setAdapter's own click handler always dismisses the dialog; we need the
        // "Add custom proxy…" row to keep it open, so wire the item clicks manually.
        dialog.listView.setOnItemClickListener { _, _, position, _ ->
            when (val row = adapter.getItem(position)) {
                is Row.Proxy -> {
                    prefs.setInstagramProxy(row.domain)
                    onChanged(row.domain)
                    dialog.dismiss()
                }
                is Row.AddCustom -> showAddCustomProxyDialog(context, prefs) {
                    adapter.reload()
                }
            }
        }

        dialog.show()
    }

    /** List adapter for the proxy roster. The list is tiny, so rows are not recycled. */
    private class ProxyListAdapter(
        private val context: Context,
        private val prefs: PreferencesManager,
        private val onDeleted: (String) -> Unit
    ) : BaseAdapter() {

        private var rows: List<Row> = buildRows()

        fun reload() {
            rows = buildRows()
            notifyDataSetChanged()
        }

        private fun buildRows(): List<Row> {
            val fixed = Constants.INSTAGRAM_PROXY_DOMAINS.map { domain ->
                val badge = if (domain in Constants.INSTAGRAM_PRIMARY_PROXIES) {
                    R.string.instagram_proxy_primary_label
                } else {
                    R.string.instagram_proxy_backup_label
                }
                Row.Proxy(domain, badge, isCustom = false)
            }
            val custom = prefs.getCustomInstagramProxies().map { domain ->
                Row.Proxy(domain, R.string.instagram_proxy_custom_label, isCustom = true)
            }
            return fixed + custom + Row.AddCustom
        }

        override fun getCount(): Int = rows.size
        override fun getItem(position: Int): Row = rows[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_instagram_proxy_option, parent, false)
            val radio = view.findViewById<MaterialRadioButton>(R.id.proxyRadio)
            val text = view.findViewById<TextView>(R.id.proxyDomainText)
            val delete = view.findViewById<ImageButton>(R.id.proxyDeleteButton)

            when (val row = rows[position]) {
                is Row.Proxy -> {
                    radio.visibility = View.VISIBLE
                    radio.isChecked = row.domain == prefs.getInstagramProxy()
                    // Domain stays as plain text inside this TextView so Espresso's
                    // withText(containsString(domain)) matchers keep working.
                    text.text = buildBadgedLabel(context, row.domain, row.badgeRes)
                    if (row.isCustom) {
                        delete.visibility = View.VISIBLE
                        delete.contentDescription = context.getString(
                            R.string.instagram_proxy_delete_content_desc, row.domain
                        )
                        delete.setOnClickListener { onDeleted(row.domain) }
                    } else {
                        delete.visibility = View.GONE
                    }
                }
                is Row.AddCustom -> {
                    radio.visibility = View.INVISIBLE
                    delete.visibility = View.GONE
                    text.text = context.getString(R.string.instagram_proxy_add_custom)
                }
            }
            return view
        }
    }

    /** Input dialog for a new custom proxy with inline validation errors. */
    private fun showAddCustomProxyDialog(
        context: Context,
        prefs: PreferencesManager,
        onAdded: () -> Unit
    ) {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_add_custom_proxy, null)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.customProxyInputLayout)
        val input = view.findViewById<TextInputEditText>(R.id.customProxyInput)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.instagram_proxy_add_dialog_title)
            .setView(view)
            .setPositiveButton(R.string.add, null) // listener set below to control dismissal
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val domain = InstagramProxyStore.normalizeCustomProxyInput(input.text?.toString() ?: "")
            val errorRes = when {
                !InstagramProxyStore.isValidProxyDomainFormat(domain) ->
                    R.string.instagram_proxy_error_invalid_domain
                InstagramProxyStore.isReservedDomain(domain) ->
                    R.string.instagram_proxy_error_reserved_domain
                InstagramProxyStore.isDuplicate(domain) ->
                    R.string.instagram_proxy_error_duplicate
                else -> null
            }
            if (errorRes != null) {
                inputLayout.error = context.getString(errorRes)
            } else {
                prefs.addCustomInstagramProxy(domain)
                onAdded()
                dialog.dismiss()
            }
        }
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
            TextViewCompat.setTextAppearance(this, android.R.style.TextAppearance_Material_Title)
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
        MaterialAlertDialogBuilder(context)
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
