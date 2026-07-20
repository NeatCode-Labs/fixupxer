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

import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.CompoundButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fixupxer.BuildConfig
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.backup.LocalBackupManager
import com.fixupxer.databinding.ActivitySettingsBinding
import com.fixupxer.presentation.rules.CustomRulesViewModel
import com.fixupxer.presentation.settings.BackupRestoreUiState
import com.fixupxer.presentation.settings.SettingsBackupViewModel
import com.fixupxer.ui.helpers.BrowserStatusTextHelper
import com.fixupxer.ui.helpers.ConfigurationStatusDialogHelper
import com.fixupxer.ui.helpers.SnackbarHelper
import com.fixupxer.ui.helpers.ThemeHelper
import com.fixupxer.ui.helpers.UrlActionHelper
import com.fixupxer.utils.Constants
import com.fixupxer.utils.CustomRulesEffectiveStatus
import com.fixupxer.utils.SettingsStatusResolver
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.R as AppCompatR
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@AndroidEntryPoint
class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val customRulesViewModel: CustomRulesViewModel by viewModels()
    private val backupViewModel: SettingsBackupViewModel by viewModels()
    private var restoreProgressDialog: AlertDialog? = null
    private var restoreThemeAwaitingRecreation: String? = null

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var localBackupManager: LocalBackupManager

    private val customRulesListener = CompoundButton.OnCheckedChangeListener { _, checked ->
        if (customRulesViewModel.enabled.value != checked) {
            customRulesViewModel.setEnabled(checked)
        }
    }

    private val openBackupDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) restoreFrom(uri)
        }

    private val createBackupDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) exportTo(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupViews()
        observeCustomRules()
        observeBackupRestore()
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
            val hand = if (checkedId == R.id.buttonHandLeft) {
                PreferencesManager.DOMINANT_HAND_LEFT
            } else {
                PreferencesManager.DOMINANT_HAND_RIGHT
            }
            if (hand == preferencesManager.getDominantHand()) return@addOnButtonCheckedListener
            preferencesManager.setDominantHand(hand)
            Timber.d("Dominant hand changed to: $hand")
        }

        binding.switchCustomRules.setOnCheckedChangeListener(customRulesListener)
        binding.buttonCustomRules.setOnClickListener {
            startActivity(Intent(this, CustomRulesActivity::class.java))
        }
        binding.buttonCustomRulesHowTo.setOnClickListener {
            UrlActionHelper.openUrlInExternalBrowser(
                binding.root,
                this,
                Constants.CUSTOM_RULES_GUIDE_URL,
            )
        }
        binding.configurationStatusNavigation.setOnClickListener {
            showConfigurationStatus()
        }
        binding.browserModeNavigation.setOnClickListener {
            startActivity(Intent(this, BrowserSettingsActivity::class.java))
        }
        binding.buttonBackupSettings.setOnClickListener {
            showBackupWarning()
        }
        binding.buttonRestoreSettings.setOnClickListener {
            openBackupDocument.launch(arrayOf("application/json", "text/plain"))
        }
    }

    private fun renderState() {
        binding.themeToggleGroup.check(
            when (preferencesManager.getThemeMode()) {
                PreferencesManager.THEME_MODE_LIGHT -> R.id.buttonThemeLight
                PreferencesManager.THEME_MODE_DARK -> R.id.buttonThemeDark
                else -> R.id.buttonThemeSystem
            }
        )
        binding.handToggleGroup.check(
            if (preferencesManager.getDominantHand() == PreferencesManager.DOMINANT_HAND_LEFT) {
                R.id.buttonHandLeft
            } else {
                R.id.buttonHandRight
            }
        )

        binding.switchCustomRules.setOnCheckedChangeListener(null)
        binding.switchCustomRules.isChecked = customRulesViewModel.enabled.value
        binding.switchCustomRules.setOnCheckedChangeListener(customRulesListener)
        renderCustomRulesStatus()
        renderBrowserStatus()
    }

    private fun renderBrowserStatus() {
        val state = BrowserStatusTextHelper.resolveState(this, preferencesManager)
        val statusText = getString(
            BrowserStatusTextHelper.statusTextRes(state.effectiveStatus)
        )
        binding.textBrowserModeStatus.text = statusText
        binding.browserModeNavigation.contentDescription = getString(
            R.string.browser_mode_navigation_content_description,
            getString(R.string.configure_browser_mode),
            statusText,
        )
    }

    private fun showConfigurationStatus() {
        lifecycleScope.launch {
            val rules = customRulesViewModel.getRulesSnapshot()
            val enabledRules = rules.count { it.enabled }
            ConfigurationStatusDialogHelper.show(
                context = this@SettingsActivity,
                layoutInflater = layoutInflater,
                preferencesManager = preferencesManager,
                customRulesEnabled = customRulesViewModel.enabled.value,
                enabledRulesCount = enabledRules,
                disabledRulesCount = rules.size - enabledRules,
            )
        }
    }

    private fun renderCustomRulesStatus() {
        val enabledCount = customRulesViewModel.rules.value.count { it.enabled }
        val disabledCount = customRulesViewModel.rules.value.size - enabledCount
        val status = SettingsStatusResolver.resolveCustomRules(
            masterEnabled = customRulesViewModel.enabled.value,
            enabledCount = enabledCount,
            disabledCount = disabledCount,
        )
        binding.textCustomRulesStatus.text = when (status.status) {
            CustomRulesEffectiveStatus.NO_RULES ->
                getString(R.string.custom_rules_status_none)
            CustomRulesEffectiveStatus.ON_WITH_ACTIVE_RULES ->
                resources.getQuantityString(
                    R.plurals.custom_rules_status_on_active,
                    status.enabledCount,
                    status.enabledCount,
                )
            CustomRulesEffectiveStatus.ON_WITHOUT_ACTIVE_RULES ->
                resources.getQuantityString(
                    R.plurals.custom_rules_status_on_disabled,
                    status.disabledCount,
                    status.disabledCount,
                )
            CustomRulesEffectiveStatus.OFF_WITH_PAUSED_RULES ->
                resources.getQuantityString(
                    R.plurals.custom_rules_status_off_paused,
                    status.enabledCount,
                    status.enabledCount,
                )
            CustomRulesEffectiveStatus.OFF_WITH_DISABLED_RULES ->
                resources.getQuantityString(
                    R.plurals.custom_rules_status_off_disabled,
                    status.disabledCount,
                    status.disabledCount,
                )
        }
    }

    private fun observeCustomRules() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    customRulesViewModel.rules.collect {
                        renderCustomRulesStatus()
                    }
                }
                launch {
                    customRulesViewModel.enabled.collect { enabled ->
                        binding.switchCustomRules.setOnCheckedChangeListener(null)
                        binding.switchCustomRules.isChecked = enabled
                        binding.switchCustomRules.setOnCheckedChangeListener(customRulesListener)
                        renderCustomRulesStatus()
                    }
                }
            }
        }
    }

    private fun restoreFrom(uri: Uri) {
        lifecycleScope.launch {
            val json = runCatching {
                withContext(Dispatchers.IO) { readBackupJson(uri) }
            }.getOrElse { error ->
                val message = if (error is BackupFileTooLargeException) {
                    getString(R.string.local_backup_file_too_large)
                } else {
                    getString(R.string.local_backup_invalid)
                }
                SnackbarHelper.showShort(binding.root, message)
                return@launch
            }

            val preview = runCatching { localBackupManager.previewRestore(json) }.getOrElse {
                SnackbarHelper.showShort(binding.root, getString(R.string.local_backup_invalid))
                return@launch
            }

            val exportedAt = if (preview.exportedAt > 0L) {
                DateFormat.getDateTimeInstance().format(Date(preview.exportedAt))
            } else {
                getString(R.string.local_backup_date_unknown)
            }
            val historyState = getString(
                if (preview.historyEnabled) {
                    R.string.local_backup_history_on
                } else {
                    R.string.local_backup_history_off
                }
            )
            val restoreDialog = MaterialAlertDialogBuilder(this@SettingsActivity)
                .setTitle(R.string.local_backup_restore_confirm_title)
                .setMessage(
                    getString(
                        R.string.local_backup_restore_confirm_message,
                        preview.appVersion.ifBlank { BuildConfig.VERSION_NAME },
                        exportedAt,
                        preview.ruleCount,
                        preview.routeCount,
                        historyState,
                        preview.maxHistoryEntries,
                    )
                )
                .setPositiveButton(R.string.local_backup_replace_settings) { _, _ ->
                    backupViewModel.restore(json)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            restoreDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(
                MaterialColors.getColor(
                    restoreDialog.getButton(DialogInterface.BUTTON_POSITIVE),
                    AppCompatR.attr.colorError,
                )
            )
        }
    }

    private fun showBackupWarning() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.local_backup_warning_title)
            .setMessage(R.string.local_backup_warning_message)
            .setPositiveButton(R.string.local_backup_export) { _, _ ->
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                createBackupDocument.launch("fixupxer-backup-$date.json")
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showRestoreSuccess() {
        SnackbarHelper.showShort(
            binding.root,
            getString(R.string.local_backup_restore_success),
        )
    }

    private fun observeBackupRestore() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                backupViewModel.restoreState.collect { state ->
                    val restoreBusy = state == BackupRestoreUiState.Restoring ||
                        state is BackupRestoreUiState.ApplyTheme
                    binding.buttonRestoreSettings.isEnabled = !restoreBusy
                    if (restoreBusy) {
                        showRestoreProgress()
                    } else {
                        dismissRestoreProgress()
                    }
                    when (state) {
                        is BackupRestoreUiState.ApplyTheme -> {
                            val targetNightMode = ThemeHelper.nightModeFor(state.themeMode)
                            if (AppCompatDelegate.getDefaultNightMode() == targetNightMode) {
                                if (restoreThemeAwaitingRecreation != state.themeMode) {
                                    backupViewModel.markThemeApplied(state.themeMode)
                                }
                            } else {
                                val recreateExpected = effectiveNightMode() !=
                                    effectiveNightModeFor(targetNightMode)
                                restoreThemeAwaitingRecreation =
                                    state.themeMode.takeIf { recreateExpected }
                                ThemeHelper.apply(state.themeMode)
                                if (!recreateExpected) {
                                    backupViewModel.markThemeApplied(state.themeMode)
                                }
                            }
                        }
                        BackupRestoreUiState.Success -> {
                            if (restoreThemeAwaitingRecreation == null &&
                                backupViewModel.consumeResult(state)
                            ) {
                                renderState()
                                showRestoreSuccess()
                            }
                        }
                        BackupRestoreUiState.Failure -> {
                            if (backupViewModel.consumeResult(state)) {
                                SnackbarHelper.showShort(
                                    binding.root,
                                    getString(R.string.local_backup_restore_failed),
                                )
                            }
                        }
                        BackupRestoreUiState.Idle,
                        BackupRestoreUiState.Restoring,
                        -> Unit
                    }
                }
            }
        }
    }

    private fun effectiveNightMode(): Int =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

    private fun effectiveNightModeFor(targetNightMode: Int): Int =
        when (targetNightMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> Configuration.UI_MODE_NIGHT_NO
            AppCompatDelegate.MODE_NIGHT_YES -> Configuration.UI_MODE_NIGHT_YES
            else -> applicationContext.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        }

    private fun showRestoreProgress() {
        if (restoreProgressDialog?.isShowing == true) return
        restoreProgressDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.local_backup_restoring_title)
            .setMessage(R.string.local_backup_restoring_message)
            .setCancelable(false)
            .create()
            .also { it.show() }
    }

    private fun dismissRestoreProgress() {
        restoreProgressDialog?.dismiss()
        restoreProgressDialog = null
    }

    private fun exportTo(uri: Uri) {
        lifecycleScope.launch {
            runCatching {
                val json = localBackupManager.exportJson()
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use {
                        it.write(json)
                    } ?: throw IllegalStateException("Backup destination unavailable")
                }
            }.onSuccess {
                SnackbarHelper.showShort(
                    binding.root,
                    getString(R.string.local_backup_export_success),
                )
            }.onFailure {
                SnackbarHelper.showShort(
                    binding.root,
                    getString(R.string.local_backup_export_failed),
                )
            }
        }
    }

    private class BackupFileTooLargeException : Exception()

    private fun readBackupJson(uri: Uri): String {
        contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            if (descriptor.length >= 0 && descriptor.length > Constants.MAX_LOCAL_BACKUP_BYTES) {
                throw BackupFileTooLargeException()
            }
        }
        return contentResolver.openInputStream(uri)?.use { stream ->
            String(readLimitedBackup(stream), StandardCharsets.UTF_8)
        } ?: throw IllegalStateException("Backup source unavailable")
    }

    private fun readLimitedBackup(stream: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            if (output.size() > Constants.MAX_LOCAL_BACKUP_BYTES) {
                throw BackupFileTooLargeException()
            }
        }
        return output.toByteArray()
    }

    override fun onDestroy() {
        dismissRestoreProgress()
        super.onDestroy()
    }
}
