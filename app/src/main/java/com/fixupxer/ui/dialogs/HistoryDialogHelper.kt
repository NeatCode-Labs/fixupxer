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
import androidx.core.view.isVisible
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
import com.fixupxer.utils.Constants
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
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
    private val onEntrySelected: ((UrlHistory) -> Unit)? = null,
    private val onSettingsChanged: () -> Unit = {},
    private val onDismiss: () -> Unit = {},
) {
    
    private var dialog: AnimatedBottomSheetDialog? = null
    private lateinit var binding: DialogHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private var historyCollectJob: Job? = null
    
    fun showHistoryDialog() {
        binding = DialogHistoryBinding.inflate(
            (context as androidx.appcompat.app.AppCompatActivity).layoutInflater
        )
        
        setupUI()
        createDialog()
    }
    
    private fun setupUI() {
        adapter = HistoryAdapter(
            onItemClick = onEntrySelected?.let { callback ->
                { item ->
                    callback(item)
                    dialog?.dismissAnimated()
                }
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
            updateRecordingStatus(isChecked)
            onSettingsChanged()
        }
        
        // Max entries settings
        binding.btnMaxEntries.setOnClickListener {
            showMaxEntriesDialog()
        }
        
        // Clear all button
        binding.btnClearAll.setOnClickListener {
            showClearAllConfirmation()
        }
        
        // Close button
        binding.btnClose.setOnClickListener {
            dialog?.dismissAnimated()
        }
        
        updateRecordingStatus(preferencesManager.isHistoryEnabled())
        observeHistory()
    }
    
    private fun createDialog() {
        dialog = AnimatedBottomSheetDialog(context).apply {
            setContentView(binding.root)
            setOnDismissListener {
                historyCollectJob?.cancel()
                historyCollectJob = null
                onDismiss()
            }
            show()
        }
    }
    
    private fun observeHistory() {
        // Defensive cancellation keeps this safe if the dialog is refreshed later.
        historyCollectJob?.cancel()
        historyCollectJob = lifecycleOwner.lifecycleScope.launch {
            historyRepository.getAllHistory().collectLatest { historyList ->
                adapter.submitList(historyList)
                
                // Show/hide empty state
                if (historyList.isEmpty()) {
                    binding.recyclerViewHistory.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE
                    binding.textViewEmptyTitle.text = context.getString(R.string.no_history)
                    binding.textViewEmptyDescription.text = context.getString(
                        if (preferencesManager.isHistoryEnabled()) {
                            R.string.no_history_description
                        } else {
                            R.string.history_recording_off_empty
                        }
                    )
                } else {
                    binding.recyclerViewHistory.visibility = View.VISIBLE
                    binding.emptyState.visibility = View.GONE
                }
            }
        }
    }
    
    private fun updateRecordingStatus(enabled: Boolean) {
        binding.recordingOffBanner.isVisible = !enabled
        if (adapter.currentList.isEmpty()) {
            binding.textViewEmptyDescription.text = context.getString(
                if (enabled) {
                    R.string.no_history_description
                } else {
                    R.string.history_recording_off_empty
                }
            )
        }
    }
    
    private fun showMaxEntriesDialog() {
        val dialogBinding = DialogMaxEntriesBinding.inflate(
            (context as androidx.appcompat.app.AppCompatActivity).layoutInflater
        )
        
        val migrationPending = preferencesManager.isHistoryLimitMigrationPending()
        val displayedLimit = if (migrationPending) {
            preferencesManager.getSupportedHistoryLimit()
        } else {
            preferencesManager.getMaxHistoryEntries()
        }
        dialogBinding.editTextMaxEntries.setText(displayedLimit.toString())
        if (migrationPending) {
            dialogBinding.textInputMaxEntries.helperText = context.getString(
                R.string.history_limit_legacy_migration,
                displayedLimit,
            )
        }
        
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
            
            if (maxEntries != null &&
                maxEntries in Constants.MIN_HISTORY_ENTRIES..Constants.MAX_HISTORY_ENTRIES
            ) {
                dialogBinding.textInputMaxEntries.error = null
                dialogBinding.buttonOk.isEnabled = false
                lifecycleOwner.lifecycleScope.launch {
                    if (commitHistoryLimit(maxEntries)) {
                        maxEntriesDialog.dismiss()
                    } else {
                        dialogBinding.buttonOk.isEnabled = true
                        dialogBinding.textInputMaxEntries.error =
                            context.getString(R.string.history_limit_update_failed)
                    }
                }
            } else {
                dialogBinding.textInputMaxEntries.error =
                    context.getString(R.string.invalid_max_entries)
            }
        }
        
        maxEntriesDialog.show()
    }

    internal suspend fun commitHistoryLimit(maxEntries: Int): Boolean =
        try {
            historyRepository.trimHistory(maxEntries)
            preferencesManager.setMaxHistoryEntries(maxEntries)
            onSettingsChanged()
            true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "Failed to trim history before updating its limit")
            false
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
