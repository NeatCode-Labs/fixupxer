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

import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.domain.model.UrlHistory
import com.fixupxer.databinding.DialogHistoryBinding
import com.fixupxer.databinding.DialogMaxEntriesBinding
import com.fixupxer.databinding.DialogClearHistoryBinding
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.ui.adapters.HistoryAdapter
import com.fixupxer.ui.helpers.SnackbarHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Helper class to manage the history dialog
 */
class HistoryDialogHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val historyRepository: HistoryRepository,
    private val preferencesManager: PreferencesManager,
    private val onEntrySelected: ((UrlHistory) -> Unit)? = null
) {
    
    private var dialog: BottomSheetDialog? = null
    private lateinit var binding: DialogHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private var historyCollectJob: Job? = null
    
    fun showHistoryDialog() {
        binding = DialogHistoryBinding.inflate(
            (context as androidx.appcompat.app.AppCompatActivity).layoutInflater
        )
        
        // setupUI() → updateHistoryVisibility() already starts the collector
        // when history is enabled — starting it here unconditionally would
        // override the "history disabled" empty state.
        setupUI()
        createDialog()
    }
    
    private fun setupUI() {
        adapter = HistoryAdapter(
            onItemClick = { item ->
                onEntrySelected?.invoke(item)
                dialog?.dismiss()
            },
            onItemDelete = { item ->
                deleteHistoryEntry(item)
            },
            snackbarAnchor = binding.root
        )
        
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@HistoryDialogHelper.adapter
        }
        
        // History enabled switch
        binding.switchHistoryEnabled.isChecked = preferencesManager.isHistoryEnabled()
        binding.switchHistoryEnabled.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setHistoryEnabled(isChecked)
            updateHistoryVisibility(isChecked)
        }
        
        // Max entries button
        val currentMax = preferencesManager.getMaxHistoryEntries()
        binding.btnMaxEntries.text = currentMax.toString()
        binding.btnMaxEntries.setOnClickListener {
            showMaxEntriesDialog()
        }
        
        // Clear all button
        binding.btnClearAll.setOnClickListener {
            showClearAllConfirmation()
        }
        
        // Close button
        binding.btnClose.setOnClickListener {
            dialog?.dismiss()
        }
        
        // Update visibility based on history enabled state
        updateHistoryVisibility(preferencesManager.isHistoryEnabled())
    }
    
    private fun createDialog() {
        dialog = BottomSheetDialog(context).apply {
            setContentView(binding.root)
            setOnDismissListener {
                historyCollectJob?.cancel()
                historyCollectJob = null
            }
            // Open fully expanded: the action buttons sit at the bottom of the
            // sheet and would otherwise be hidden below the collapsed peek.
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            show()
        }
    }
    
    private fun observeHistory() {
        // Called from both showHistoryDialog() and updateHistoryVisibility() —
        // cancel the previous collector so we never stack duplicates.
        historyCollectJob?.cancel()
        historyCollectJob = lifecycleOwner.lifecycleScope.launch {
            historyRepository.getAllHistory().collectLatest { historyList ->
                adapter.submitList(historyList)
                
                // Show/hide empty state
                if (historyList.isEmpty()) {
                    binding.recyclerViewHistory.visibility = View.GONE
                    binding.textViewEmpty.visibility = View.VISIBLE
                } else {
                    binding.recyclerViewHistory.visibility = View.VISIBLE
                    binding.textViewEmpty.visibility = View.GONE
                }
            }
        }
    }
    
    private fun updateHistoryVisibility(enabled: Boolean) {
        binding.maxEntriesContainer.visibility = if (enabled) View.VISIBLE else View.GONE
        
        if (!enabled) {
            // Stop collecting: a later Room emission (delete, undo, …) must not
            // flip the RecyclerView back to VISIBLE while history is disabled.
            historyCollectJob?.cancel()
            historyCollectJob = null
            binding.recyclerViewHistory.visibility = View.GONE
            binding.textViewEmpty.visibility = View.VISIBLE
            binding.textViewEmpty.text = context.getString(R.string.enable_history)
        } else {
            binding.textViewEmpty.text = context.getString(R.string.no_history)
            observeHistory()
        }
    }
    
    private fun showMaxEntriesDialog() {
        val dialogBinding = DialogMaxEntriesBinding.inflate(
            (context as androidx.appcompat.app.AppCompatActivity).layoutInflater
        )
        
        // Set current value
        dialogBinding.editTextMaxEntries.setText(preferencesManager.getMaxHistoryEntries().toString())
        
        val maxEntriesDialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()
        
        // Set click listeners
        dialogBinding.buttonClose.setOnClickListener {
            maxEntriesDialog.dismiss()
        }
        
        dialogBinding.buttonOk.setOnClickListener {
            val input = dialogBinding.editTextMaxEntries.text?.toString() ?: ""
            val maxEntries = input.toIntOrNull()
            
            if (maxEntries != null && maxEntries > 0) {
                preferencesManager.setMaxHistoryEntries(maxEntries)
                binding.btnMaxEntries.text = maxEntries.toString()
                
                // Trim history to new max
                lifecycleOwner.lifecycleScope.launch {
                    historyRepository.trimHistory(maxEntries)
                }
                maxEntriesDialog.dismiss()
            } else {
                SnackbarHelper.showShort(
                    binding.root,
                    context.getString(R.string.invalid_max_entries)
                )
            }
        }
        
        maxEntriesDialog.show()
    }
    
    private fun showClearAllConfirmation() {
        val dialogBinding = DialogClearHistoryBinding.inflate(
            (context as androidx.appcompat.app.AppCompatActivity).layoutInflater
        )
        
        val confirmDialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()
        
        dialogBinding.buttonClose.setOnClickListener {
            confirmDialog.dismiss()
        }
        
        dialogBinding.buttonClearAll.setOnClickListener {
            lifecycleOwner.lifecycleScope.launch {
                historyRepository.deleteAllHistory()
                Timber.d("History cleared")
                SnackbarHelper.showShort(binding.root, context.getString(R.string.history_cleared))
            }
            confirmDialog.dismiss()
        }
        
        confirmDialog.show()
    }
    
    private fun deleteHistoryEntry(item: UrlHistory) {
        lifecycleOwner.lifecycleScope.launch {
            historyRepository.deleteHistory(item.id)
            Timber.d("History entry deleted")
            SnackbarHelper.showShortWithAction(
                anchor = binding.root,
                message = context.getString(R.string.history_entry_deleted),
                actionLabel = context.getString(R.string.undo)
            ) {
                lifecycleOwner.lifecycleScope.launch {
                    historyRepository.insertHistory(
                        originalUrl = item.originalUrl,
                        cleanedUrl = item.cleanedUrl,
                        platform = item.platform,
                        conversionType = item.conversionType
                    )
                }
            }
        }
    }
}
