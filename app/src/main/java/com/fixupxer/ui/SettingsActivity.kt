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

package com.fixupxer.ui

import android.os.Bundle
import android.content.Intent
import android.view.MenuItem
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.databinding.ActivitySettingsBinding
import com.fixupxer.databinding.DialogConversionDefaultsBinding
import com.fixupxer.ui.adapters.ActionPriorityAdapter
import com.fixupxer.ui.helpers.ThemeHelper
import com.fixupxer.presentation.rules.CustomRulesViewModel
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber
import com.fixupxer.ui.helpers.BrowserConversionDefaultsHelper
import com.fixupxer.ui.helpers.ConfigurationStatusDialogHelper
import com.fixupxer.ui.helpers.SnackbarHelper
import com.fixupxer.ui.helpers.UrlActionHelper
import com.fixupxer.ui.dialogs.ProxyPickerDialogHelper
import com.fixupxer.utils.ProxyPlatform

/**
 * Settings activity for configuring browser mode and other app preferences
 */
@AndroidEntryPoint
class SettingsActivity : BaseActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var actionPriorityAdapter: ActionPriorityAdapter
    private val customRulesViewModel: CustomRulesViewModel by viewModels()
    private var conversionDefaultsDialog: androidx.appcompat.app.AlertDialog? = null
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        setupViews()
        loadSettings()
        observeCustomRules()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupViews() {
        // Theme picker
        binding.themeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.buttonThemeLight -> PreferencesManager.THEME_MODE_LIGHT
                R.id.buttonThemeDark -> PreferencesManager.THEME_MODE_DARK
                else -> PreferencesManager.THEME_MODE_SYSTEM
            }
            if (mode == preferencesManager.getThemeMode()) return@addOnButtonCheckedListener
            preferencesManager.setThemeMode(mode)
            ThemeHelper.apply(mode)
            Timber.d("Theme mode changed to: $mode")
        }

        binding.handToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val hand = when (checkedId) {
                R.id.buttonHandLeft -> PreferencesManager.DOMINANT_HAND_LEFT
                else -> PreferencesManager.DOMINANT_HAND_RIGHT
            }
            if (hand == preferencesManager.getDominantHand()) {
                return@addOnButtonCheckedListener
            }
            preferencesManager.setDominantHand(hand)
            Timber.d("Dominant hand changed to: $hand")
        }

        // Browser mode switch
        binding.switchBrowserMode.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setBrowserModeEnabled(isChecked)
            BrowserModeUtils.setBrowserAliasEnabled(this, isChecked)
            Timber.d("Browser mode enabled: $isChecked")
        }
        
        // Browser-mode guide
        binding.buttonReadThis.setOnClickListener {
            UrlActionHelper.openUrl(binding.root, this, Constants.BROWSER_MODE_GUIDE_URL)
        }
        
        // Action mode radio group
        binding.radioGroupActionMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioAskEveryTime -> PreferencesManager.ACTION_MODE_ASK
                R.id.radioFollowPriority -> PreferencesManager.ACTION_MODE_PRIORITY
                else -> PreferencesManager.ACTION_MODE_ASK
            }
            preferencesManager.setActionMode(mode)
            updateActionPriorityVisibility(mode)
            Timber.d("Action mode changed to: $mode")
        }
        
        // Setup action priority RecyclerView
        setupActionPriorityList()
        
        // Conversion defaults button
        binding.buttonConversionDefaults.setOnClickListener {
            showConversionDefaultsDialog()
        }

        binding.buttonConfigurationStatus.setOnClickListener {
            lifecycleScope.launch {
                val enabledRulesCount = customRulesViewModel.getRulesSnapshot().count { it.enabled }
                ConfigurationStatusDialogHelper.show(
                    context = this@SettingsActivity,
                    layoutInflater = layoutInflater,
                    preferencesManager = preferencesManager,
                    customRulesEnabled = customRulesViewModel.enabled.value,
                    enabledRulesCount = enabledRulesCount,
                )
            }
        }

        binding.buttonCustomRules.setOnClickListener {
            startActivity(Intent(this, CustomRulesActivity::class.java))
        }
        binding.buttonCustomRulesHowTo.setOnClickListener {
            UrlActionHelper.openUrl(binding.root, this, Constants.CUSTOM_RULES_GUIDE_URL)
        }
        binding.switchCustomRules.setOnCheckedChangeListener { _, checked ->
            customRulesViewModel.setEnabled(checked)
        }
    }
    
    private fun loadSettings() {
        // Load theme mode
        val themeButtonId = when (preferencesManager.getThemeMode()) {
            PreferencesManager.THEME_MODE_LIGHT -> R.id.buttonThemeLight
            PreferencesManager.THEME_MODE_DARK -> R.id.buttonThemeDark
            else -> R.id.buttonThemeSystem
        }
        binding.themeToggleGroup.check(themeButtonId)

        val handButtonId = when (preferencesManager.getDominantHand()) {
            PreferencesManager.DOMINANT_HAND_LEFT -> R.id.buttonHandLeft
            else -> R.id.buttonHandRight
        }
        binding.handToggleGroup.check(handButtonId)

        // Load browser mode state
        val browserModeEnabled = preferencesManager.isBrowserModeEnabled()
        binding.switchBrowserMode.isChecked = browserModeEnabled
        
        // Load action mode
        val actionMode = preferencesManager.getActionMode()
        when (actionMode) {
            PreferencesManager.ACTION_MODE_ASK -> binding.radioAskEveryTime.isChecked = true
            PreferencesManager.ACTION_MODE_PRIORITY -> binding.radioFollowPriority.isChecked = true
        }
        updateActionPriorityVisibility(actionMode)
        
        // Load action priority list
        val actionPriority = preferencesManager.getActionPriority()
        actionPriorityAdapter.updateItems(actionPriority)
    }

    private fun observeCustomRules() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    customRulesViewModel.rules.collect { rules ->
                        val enabledCount = rules.count { it.enabled }
                        binding.textCustomRulesCount.text = resources.getQuantityString(
                            R.plurals.custom_rules_count,
                            enabledCount,
                            enabledCount
                        )
                    }
                }
                launch {
                    customRulesViewModel.enabled.collect { enabled ->
                        if (binding.switchCustomRules.isChecked != enabled) {
                            binding.switchCustomRules.isChecked = enabled
                        }
                    }
                }
            }
        }
    }
    
    private fun setupActionPriorityList() {
        actionPriorityAdapter = ActionPriorityAdapter(
            onItemsReordered = { newOrder ->
                preferencesManager.setActionPriority(newOrder)
                Timber.d("Action priority reordered: $newOrder")
            }
        )
        
        binding.recyclerViewActionPriority.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = actionPriorityAdapter
        }
        
        // Setup drag and drop
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                return makeMovementFlags(dragFlags, 0)
            }
            
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition
                actionPriorityAdapter.moveItem(fromPosition, toPosition)
                return true
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not implemented - no swipe to delete
            }
            
            override fun isLongPressDragEnabled(): Boolean = false
        })
        
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewActionPriority)
        actionPriorityAdapter.setItemTouchHelper(itemTouchHelper)
    }
    
    private fun updateActionPriorityVisibility(mode: String) {
        val isPriorityMode = mode == PreferencesManager.ACTION_MODE_PRIORITY
        binding.actionPrioritySection.visibility = if (isPriorityMode) View.VISIBLE else View.GONE
    }
    
    override fun onDestroy() {
        // Dismissing fires the dialog's dismiss listener, which rolls back any
        // unsaved in-dialog roster restore before the activity goes away.
        conversionDefaultsDialog?.dismiss()
        conversionDefaultsDialog = null
        super.onDestroy()
    }

    private fun showConversionDefaultsDialog() {
        val dialogBinding = DialogConversionDefaultsBinding.inflate(layoutInflater)
        val draft = BrowserConversionDefaultsHelper.createDraft(preferencesManager)

        lateinit var rows: List<BrowserConversionDefaultsHelper.BrowserPrivacyRow>
        rows = BrowserConversionDefaultsHelper.populateContainer(
            context = this,
            layoutInflater = layoutInflater,
            container = dialogBinding.browserPlatformTogglesContainer,
            draft = draft,
            onChangePrivacyTarget = { platform ->
                openBrowserPrivacyTargetPicker(platform, draft, rows)
            },
        )

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.conversion_defaults_title)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        var saved = false
        dialogBinding.btnSave.setOnClickListener {
            draft.apply(preferencesManager)
            saved = true
            SnackbarHelper.showShort(binding.root, getString(R.string.browser_conversion_settings_saved))
            Timber.d("Browser privacy conversion settings saved for ${rows.size} platforms")
            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Cancel, Back, outside touch and activity teardown all funnel through
        // this single dismiss listener, so the rollback runs exactly once.
        dialog.setOnDismissListener {
            conversionDefaultsDialog = null
            if (!saved) {
                draft.discardRosterChanges()
            }
        }

        conversionDefaultsDialog = dialog
        dialog.show()
    }

    private fun openBrowserPrivacyTargetPicker(
        platform: ProxyPlatform,
        draft: BrowserConversionDefaultsHelper.DraftState,
        rows: List<BrowserConversionDefaultsHelper.BrowserPrivacyRow>,
    ) {
        fun refreshOuterRows() {
            BrowserConversionDefaultsHelper.refreshRows(
                context = this,
                rows = rows,
                draft = draft,
                onChangePrivacyTarget = { selectedPlatform ->
                    openBrowserPrivacyTargetPicker(selectedPlatform, draft, rows)
                },
            )
        }

        ProxyPickerDialogHelper.showPrivacySelection(
            context = this,
            layoutInflater = layoutInflater,
            platform = platform,
            preferencesManager = preferencesManager,
            selectedTargetId = draft.draftTargetIds[platform],
            onRosterRestored = { refreshOuterRows() },
        ) { target ->
            draft.updateDraftTarget(platform, target)
            refreshOuterRows()
        }
    }
}