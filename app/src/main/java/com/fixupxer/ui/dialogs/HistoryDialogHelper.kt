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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import android.widget.Toast

/**
 * Helper class to manage the history dialog
 */
class HistoryDialogHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val historyRepository: HistoryRepository,
    private val preferencesManager: PreferencesManager
) {
    
    private var dialog: AlertDialog? = null
    private lateinit var binding: DialogHistoryBinding
    private lateinit var adapter: HistoryAdapter
    
    fun showHistoryDialog() {
        binding = DialogHistoryBinding.inflate(
            (context as androidx.appcompat.app.AppCompatActivity).layoutInflater
        )
        
        setupUI()
        createDialog()
        observeHistory()
    }
    
    private fun setupUI() {
        // Initialize RecyclerView
        adapter = HistoryAdapter(
            onItemClick = { /* Optional: Handle item click */ },
            onItemDelete = { item ->
                showDeleteConfirmation(item)
            }
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
        dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()
        
        dialog?.show()
    }
    
    private fun observeHistory() {
        lifecycleOwner.lifecycleScope.launch {
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
            binding.recyclerViewHistory.visibility = View.GONE
            binding.textViewEmpty.visibility = View.VISIBLE
            binding.textViewEmpty.text = context.getString(R.string.enable_history)
        } else {
            observeHistory()
        }
    }
    
    private fun showMaxEntriesDialog() {
        val dialogBinding = DialogMaxEntriesBinding.inflate(
            (context as androidx.appcompat.app.AppCompatActivity).layoutInflater
        )
        
        // Set current value
        dialogBinding.editTextMaxEntries.setText(preferencesManager.getMaxHistoryEntries().toString())
        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogBinding.root)
            .create()
        
        // Set click listeners
        dialogBinding.buttonClose.setOnClickListener {
            dialog.dismiss()
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
                dialog.dismiss()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.invalid_max_entries),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        dialog.show()
    }
    
    private fun showClearAllConfirmation() {
        val dialogBinding = DialogClearHistoryBinding.inflate(
            (context as androidx.appcompat.app.AppCompatActivity).layoutInflater
        )
        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogBinding.root)
            .create()
        
        dialogBinding.buttonClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialogBinding.buttonClearAll.setOnClickListener {
            lifecycleOwner.lifecycleScope.launch {
                historyRepository.deleteAllHistory()
                Timber.d("History cleared")
            }
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showDeleteConfirmation(item: UrlHistory) {
        lifecycleOwner.lifecycleScope.launch {
            historyRepository.deleteHistory(item.id)
            Timber.d("History entry deleted")
        }
    }
} 