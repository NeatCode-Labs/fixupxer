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


package com.fixupxer.ui.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fixupxer.R
import com.fixupxer.databinding.ItemHistoryBinding
import com.fixupxer.domain.model.UrlHistory
import com.fixupxer.ui.helpers.SnackbarHelper
import timber.log.Timber

/**
 * Adapter for displaying URL history items
 */
class HistoryAdapter(
    private val onItemClick: ((UrlHistory) -> Unit)?,
    private val onItemDelete: (UrlHistory) -> Unit,
    private val snackbarAnchor: View
) : ListAdapter<UrlHistory, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding, onItemClick, onItemDelete, snackbarAnchor)
    }
    
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    /**
     * ViewHolder for history items
     */
    class HistoryViewHolder(
        private val binding: ItemHistoryBinding,
        private val onItemClick: ((UrlHistory) -> Unit)?,
        private val onItemDelete: (UrlHistory) -> Unit,
        private val snackbarAnchor: View
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: UrlHistory) {
            binding.apply {
                // Original URL
                textViewOriginalUrl.text = item.originalUrl
                
                // Processed URL (cleaned URL)
                textViewProcessedUrl.text = item.cleanedUrl
                
                // Timestamp
                textViewTimestamp.text = item.timeAgo
                textViewConversionType.text = item.conversionType
                
                // Platform - show if available
                if (item.platform != "Other") {
                    textViewPlatform.visibility = View.VISIBLE
                    textViewPlatform.text = item.platform
                } else {
                    textViewPlatform.visibility = View.GONE
                }
                
                // Copy button
                buttonCopy.setOnClickListener {
                    val clipboard = binding.root.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(
                        binding.root.context.getString(R.string.clipboard_label_url),
                        item.cleanedUrl
                    )
                    clipboard.setPrimaryClip(clip)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        SnackbarHelper.showShort(snackbarAnchor, binding.root.context.getString(R.string.url_copied))
                    } else {
                        Timber.d("History item URL copied (Android 10+)")
                    }
                }
                
                // Share button
                buttonShare.setOnClickListener {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, item.cleanedUrl)
                        type = "text/plain"
                    }
                    binding.root.context.startActivity(Intent.createChooser(shareIntent, binding.root.context.getString(R.string.share_via)))
                }

                buttonDelete.setOnClickListener {
                    onItemDelete(item)
                }
                
                // Long press to delete
                root.setOnLongClickListener {
                    onItemDelete(item)
                    true
                }
                
                // Click listener
                root.setOnClickListener(
                    onItemClick?.let { callback ->
                        View.OnClickListener { callback(item) }
                    }
                )
                root.isClickable = onItemClick != null

                if (onItemClick != null) {
                    val openLabel = itemView.context.getString(R.string.history_open_action_label)
                    ViewCompat.replaceAccessibilityAction(
                        root,
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                        openLabel,
                        null
                    )
                } else {
                    ViewCompat.removeAccessibilityAction(
                        root,
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK.id,
                    )
                }

                val deleteLabel = itemView.context.getString(R.string.history_delete_action_label)
                ViewCompat.replaceAccessibilityAction(
                    root,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK,
                    deleteLabel,
                    null
                )
            }
        }
    }
    
    /**
     * DiffUtil callback for efficient updates
     */
    class HistoryDiffCallback : DiffUtil.ItemCallback<UrlHistory>() {
        override fun areItemsTheSame(oldItem: UrlHistory, newItem: UrlHistory): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: UrlHistory, newItem: UrlHistory): Boolean {
            return oldItem == newItem
        }
    }
}
