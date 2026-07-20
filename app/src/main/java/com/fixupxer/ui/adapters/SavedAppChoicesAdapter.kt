// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.fixupxer.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fixupxer.R
import com.fixupxer.backup.RememberedRoute
import com.fixupxer.backup.RememberedRouteKind
import com.fixupxer.databinding.ItemSavedAppChoiceBinding

data class SavedAppChoiceRow(
    val host: String,
    val route: RememberedRoute,
    val appLabel: String,
    val installed: Boolean,
)

class SavedAppChoicesAdapter(
    private val onDelete: (String) -> Unit,
) : ListAdapter<SavedAppChoiceRow, SavedAppChoicesAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemSavedAppChoiceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSavedAppChoiceBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SavedAppChoiceRow) {
            binding.textSavedChoiceHost.text = item.host
            binding.textSavedChoiceKind.setText(
                when (item.route.kind) {
                    RememberedRouteKind.NATIVE -> R.string.remembered_route_kind_native
                    RememberedRouteKind.BROWSER -> R.string.remembered_route_kind_browser
                }
            )
            binding.textSavedChoiceAppLabel.text = item.appLabel
            binding.textSavedChoicePackage.text = item.route.packageName
            binding.textSavedChoiceMissing.isVisible = !item.installed
            binding.buttonDeleteSavedChoice.contentDescription = itemView.context.getString(
                R.string.saved_app_choice_delete_content_description,
                item.host,
            )
            binding.buttonDeleteSavedChoice.setOnClickListener { onDelete(item.host) }
            binding.root.setOnClickListener(null)
            binding.root.isClickable = false
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SavedAppChoiceRow>() {
        override fun areItemsTheSame(
            oldItem: SavedAppChoiceRow,
            newItem: SavedAppChoiceRow,
        ): Boolean = oldItem.host == newItem.host

        override fun areContentsTheSame(
            oldItem: SavedAppChoiceRow,
            newItem: SavedAppChoiceRow,
        ): Boolean = oldItem == newItem
    }
}
