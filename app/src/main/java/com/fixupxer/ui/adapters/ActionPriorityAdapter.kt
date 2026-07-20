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

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import java.util.Collections

/**
 * Adapter for displaying and reordering action priority items
 */
class ActionPriorityAdapter(
    private val onItemsReordered: (List<String>) -> Unit
) : RecyclerView.Adapter<ActionPriorityAdapter.ViewHolder>() {
    
    private val items = mutableListOf<String>()
    private var itemTouchHelper: ItemTouchHelper? = null
    
    fun setItemTouchHelper(helper: ItemTouchHelper) {
        itemTouchHelper = helper
    }
    
    fun updateItems(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    fun moveItem(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition !in items.indices || toPosition !in items.indices) return false
        if (fromPosition == toPosition) return true
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        notifyItemChanged(fromPosition)
        notifyItemChanged(toPosition)
        onItemsReordered(items.toList())
        return true
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_action_priority, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        
        // Set up drag handle
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                itemTouchHelper?.startDrag(holder)
            }
            false
        }
    }
    
    override fun getItemCount(): Int = items.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textViewActionName)
        val dragHandle: ImageView = itemView.findViewById(R.id.imageViewDragHandle)
        private val moveUp: ImageButton = itemView.findViewById(R.id.buttonMoveUp)
        private val moveDown: ImageButton = itemView.findViewById(R.id.buttonMoveDown)
        
        fun bind(actionKey: String) {
            textView.text = getActionLabel(actionKey)
            val position = bindingAdapterPosition
            moveUp.isEnabled = position > 0
            moveDown.isEnabled = position in 0 until items.lastIndex
            moveUp.setOnClickListener {
                moveWithAnnouncement(bindingAdapterPosition, bindingAdapterPosition - 1)
            }
            moveDown.setOnClickListener {
                moveWithAnnouncement(bindingAdapterPosition, bindingAdapterPosition + 1)
            }
        }

        private fun moveWithAnnouncement(from: Int, to: Int) {
            if (!moveItem(from, to)) return
            itemView.announceForAccessibility(
                itemView.context.getString(
                    R.string.action_moved_announcement,
                    textView.text,
                    to + 1,
                    items.size,
                )
            )
        }
        
        private fun getActionLabel(actionKey: String): String {
            return when (actionKey) {
                PreferencesManager.ACTION_NATIVE_APP -> itemView.context.getString(R.string.action_native_app)
                PreferencesManager.ACTION_BROWSER -> itemView.context.getString(R.string.action_browser)
                PreferencesManager.ACTION_SHARE_MENU -> itemView.context.getString(R.string.action_share_menu)
                PreferencesManager.ACTION_CLIPBOARD -> itemView.context.getString(R.string.action_clipboard)
                else -> actionKey
            }
        }
    }
} 