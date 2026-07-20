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

package com.fixupxer.ui

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.databinding.ActivityBrowserSettingsBinding
import com.fixupxer.databinding.DialogConversionDefaultsBinding
import com.fixupxer.ui.adapters.ActionPriorityAdapter
import com.fixupxer.ui.dialogs.ProxyPickerDialogHelper
import com.fixupxer.ui.dialogs.RememberedRoutesDialogHelper
import com.fixupxer.ui.helpers.BrowserConversionDefaultsHelper
import com.fixupxer.ui.helpers.BrowserStatusTextHelper
import com.fixupxer.ui.helpers.SnackbarHelper
import com.fixupxer.ui.helpers.UrlActionHelper
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.BrowserPrivacySummary
import com.fixupxer.utils.BrowserSettingsState
import com.fixupxer.utils.Constants
import com.fixupxer.utils.DefaultBrowserStatus
import com.fixupxer.utils.ProxyPlatform
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class BrowserSettingsActivity : BaseActivity() {

    private lateinit var binding: ActivityBrowserSettingsBinding
    private lateinit var actionPriorityAdapter: ActionPriorityAdapter
    private var conversionDefaultsDialog: androidx.appcompat.app.AlertDialog? = null
    private var aliasOperationFailed = false
    private var renderedState: BrowserSettingsState? = null

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val browserModeListener = CompoundButton.OnCheckedChangeListener { _, checked ->
        if (checked == preferencesManager.isBrowserModeEnabled()) return@OnCheckedChangeListener
        val result = BrowserModeUtils.updateBrowserMode(this, preferencesManager, checked)
        aliasOperationFailed = result.needsAttention
        if (!result.success) {
            val message = if (result.rollbackSucceeded) {
                R.string.browser_alias_update_failed
            } else {
                R.string.browser_alias_rollback_failed
            }
            SnackbarHelper.showShort(binding.root, getString(message))
        }
        renderState()
    }

    private val actionModeListener = RadioGroup.OnCheckedChangeListener { _, checkedId ->
        val mode = if (checkedId == R.id.radioFollowPriority) {
            PreferencesManager.ACTION_MODE_PRIORITY
        } else {
            PreferencesManager.ACTION_MODE_ASK
        }
        if (mode != preferencesManager.getActionMode()) {
            preferencesManager.setActionMode(mode)
            Timber.d("Browser action mode changed to: $mode")
        }
        updateActionPriorityVisibility(mode)
        renderSavedChoicesStatus(renderedState ?: resolveState())
    }

    private val defaultBrowserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            renderState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupActionPriorityList()
        setupViews()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) renderState()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setupViews() {
        binding.switchBrowserMode.setOnCheckedChangeListener(browserModeListener)
        binding.radioGroupActionMode.setOnCheckedChangeListener(actionModeListener)
        binding.buttonDefaultBrowser.setOnClickListener {
            val state = renderedState ?: resolveState()
            if (state.defaultBrowserStatus == DefaultBrowserStatus.FIXUPXER &&
                (!state.preferenceEnabled || !state.aliasEnabled)
            ) {
                openDefaultAppsSettings()
            } else {
                chooseDefaultBrowser()
            }
        }
        binding.buttonBrowserModeGuide.setOnClickListener {
            UrlActionHelper.openUrlInExternalBrowser(
                binding.root,
                this,
                Constants.BROWSER_MODE_GUIDE_URL,
            )
        }
        binding.buttonConversionDefaults.setOnClickListener {
            showConversionDefaultsDialog()
        }
        binding.buttonSavedAppChoices.setOnClickListener {
            RememberedRoutesDialogHelper.show(this, preferencesManager) {
                renderState()
            }
        }
    }

    private fun renderState() {
        val state = resolveState()
        renderedState = state

        binding.switchBrowserMode.setOnCheckedChangeListener(null)
        binding.switchBrowserMode.isChecked = state.preferenceEnabled
        binding.switchBrowserMode.setOnCheckedChangeListener(browserModeListener)

        binding.radioGroupActionMode.setOnCheckedChangeListener(null)
        when (preferencesManager.getActionMode()) {
            PreferencesManager.ACTION_MODE_PRIORITY -> binding.radioFollowPriority.isChecked = true
            else -> binding.radioAskEveryTime.isChecked = true
        }
        binding.radioGroupActionMode.setOnCheckedChangeListener(actionModeListener)

        val actionMode = preferencesManager.getActionMode()
        updateActionPriorityVisibility(actionMode)
        actionPriorityAdapter.updateItems(preferencesManager.getActionPriority())

        binding.textBrowserStatus.setText(
            BrowserStatusTextHelper.statusTextRes(state.effectiveStatus)
        )
        renderDefaultBrowserButton(state)
        renderPrivacySummary(state.privacySummary)
        renderSavedChoicesStatus(state)
    }

    private fun resolveState(): BrowserSettingsState =
        BrowserStatusTextHelper.resolveState(
            context = this,
            preferencesManager = preferencesManager,
            aliasOperationFailed = aliasOperationFailed,
        )

    private fun renderDefaultBrowserButton(state: BrowserSettingsState) {
        val chooseAnother = state.defaultBrowserStatus == DefaultBrowserStatus.FIXUPXER &&
            (!state.preferenceEnabled || !state.aliasEnabled)
        val chooseFixupXer = state.preferenceEnabled &&
            state.aliasEnabled &&
            state.defaultBrowserStatus != DefaultBrowserStatus.FIXUPXER

        binding.buttonDefaultBrowser.visibility =
            if (chooseAnother || chooseFixupXer) View.VISIBLE else View.GONE
        binding.buttonDefaultBrowser.setText(
            if (chooseAnother) {
                R.string.browser_choose_another
            } else {
                R.string.browser_choose_default
            }
        )
    }

    private fun renderPrivacySummary(summary: BrowserPrivacySummary) {
        val active = resources.getQuantityString(
            R.plurals.privacy_readers_summary_active,
            summary.activeCount,
            summary.activeCount,
        )
        val attention = resources.getQuantityString(
            R.plurals.privacy_readers_summary_attention,
            summary.attentionCount,
            summary.attentionCount,
        )
        binding.textPrivacyReadersSummary.text = when {
            summary.activeCount == 0 && summary.attentionCount == 0 ->
                getString(R.string.privacy_readers_summary_none)
            summary.activeCount == 0 -> attention
            summary.attentionCount == 0 -> active
            else -> getString(R.string.privacy_readers_summary_mixed, active, attention)
        }
    }

    private fun renderSavedChoicesStatus(state: BrowserSettingsState) {
        val count = preferencesManager.getRememberedRouteCount()
        val askWhatToDoEnabled =
            preferencesManager.getActionMode() == PreferencesManager.ACTION_MODE_ASK
        binding.buttonSavedAppChoices.isEnabled = askWhatToDoEnabled
        if (!askWhatToDoEnabled) {
            binding.textSavedAppChoicesStatus.text = if (count == 0) {
                getString(R.string.saved_app_choices_requires_ask)
            } else {
                resources.getQuantityString(
                    R.plurals.saved_app_choices_automatic,
                    count,
                    count,
                )
            }
            return
        }

        val status = BrowserStatusTextHelper.resolveSavedChoicesStatus(
            count = count,
            state = state,
            automaticActions = false,
        )
        binding.textSavedAppChoicesStatus.text = when (status) {
            BrowserStatusTextHelper.SavedChoicesStatus.NONE ->
                getString(R.string.saved_app_choices_none)
            BrowserStatusTextHelper.SavedChoicesStatus.BROWSER_OFF ->
                resources.getQuantityString(
                    R.plurals.saved_app_choices_browser_off,
                    count,
                    count,
                )
            BrowserStatusTextHelper.SavedChoicesStatus.SETUP_INCOMPLETE ->
                resources.getQuantityString(
                    R.plurals.saved_app_choices_setup_incomplete,
                    count,
                    count,
                )
            BrowserStatusTextHelper.SavedChoicesStatus.AUTOMATIC ->
                resources.getQuantityString(
                    R.plurals.saved_app_choices_automatic,
                    count,
                    count,
                )
            BrowserStatusTextHelper.SavedChoicesStatus.READY ->
                resources.getQuantityString(
                    R.plurals.saved_app_choices_ready,
                    count,
                    count,
                )
        }
    }

    private fun chooseDefaultBrowser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val handledByRoleManager = runCatching {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager == null ||
                    !roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)
                ) {
                    false
                } else {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                        renderState()
                    } else {
                        defaultBrowserLauncher.launch(
                            roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                        )
                    }
                    true
                }
            }.onFailure {
                Timber.w(it, "Browser role request failed; using Settings fallback")
            }.getOrDefault(false)
            if (handledByRoleManager) {
                return
            }
        }
        openDefaultAppsSettings()
    }

    private fun openDefaultAppsSettings() {
        val launched = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                if (intent.resolveActivity(packageManager) != null) {
                    defaultBrowserLauncher.launch(intent)
                    true
                } else {
                    false
                }
            }.onFailure {
                Timber.w(it, "Default apps Settings launch failed")
            }.getOrDefault(false)
        } else {
            false
        }
        if (launched) return

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.browser_default_manual_title)
            .setMessage(R.string.browser_default_manual_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun setupActionPriorityList() {
        actionPriorityAdapter = ActionPriorityAdapter(
            onItemsReordered = { newOrder ->
                preferencesManager.setActionPriority(newOrder)
                Timber.d("Browser action order changed: $newOrder")
            }
        )
        binding.recyclerViewActionPriority.apply {
            layoutManager = LinearLayoutManager(this@BrowserSettingsActivity)
            adapter = actionPriorityAdapter
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
            ): Int = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean = actionPriorityAdapter.moveItem(
                viewHolder.bindingAdapterPosition,
                target.bindingAdapterPosition,
            )

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun isLongPressDragEnabled(): Boolean = false
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewActionPriority)
        actionPriorityAdapter.setItemTouchHelper(itemTouchHelper)
    }

    private fun updateActionPriorityVisibility(mode: String) {
        binding.actionPrioritySection.visibility =
            if (mode == PreferencesManager.ACTION_MODE_PRIORITY) View.VISIBLE else View.GONE
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

        dialogBinding.btnSave.setOnClickListener {
            draft.apply(preferencesManager)
            renderState()
            SnackbarHelper.showShort(
                binding.root,
                getString(R.string.browser_conversion_settings_saved),
            )
            Timber.d("Browser privacy reader settings saved for ${rows.size} platforms")
            dialog.dismiss()
        }
        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener {
            conversionDefaultsDialog = null
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
            selectedTargetId = draft.draftTargetIds[platform],
            disabledBuiltIns = { draft.disabledBuiltIns(platform) },
            onRestoreReaders = {
                draft.restoreBuiltInReaders(platform)
                refreshOuterRows()
            },
        ) { target ->
            draft.updateDraftTarget(platform, target)
            refreshOuterRows()
        }
    }

    override fun onDestroy() {
        conversionDefaultsDialog?.dismiss()
        conversionDefaultsDialog = null
        super.onDestroy()
    }
}
