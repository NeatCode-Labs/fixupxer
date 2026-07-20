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

package com.fixupxer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixupxer.PreferencesManager
import com.fixupxer.backup.LocalBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BackupRestoreUiState {
    data object Idle : BackupRestoreUiState
    data object Restoring : BackupRestoreUiState
    data class ApplyTheme(val themeMode: String) : BackupRestoreUiState
    data object Success : BackupRestoreUiState
    data object Failure : BackupRestoreUiState
}

@HiltViewModel
class SettingsBackupViewModel @Inject constructor(
    private val localBackupManager: LocalBackupManager,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {
    private val _restoreState = MutableStateFlow<BackupRestoreUiState>(BackupRestoreUiState.Idle)
    val restoreState: StateFlow<BackupRestoreUiState> = _restoreState.asStateFlow()

    fun restore(json: String) {
        if (_restoreState.value == BackupRestoreUiState.Restoring) return
        viewModelScope.launch {
            _restoreState.value = BackupRestoreUiState.Restoring
            val previousTheme = preferencesManager.getThemeMode()
            localBackupManager.restore(json)
                .onSuccess {
                    val restoredTheme = preferencesManager.getThemeMode()
                    _restoreState.value = if (previousTheme != restoredTheme) {
                        BackupRestoreUiState.ApplyTheme(restoredTheme)
                    } else {
                        BackupRestoreUiState.Success
                    }
                }
                .onFailure {
                    _restoreState.value = BackupRestoreUiState.Failure
                }
        }
    }

    fun markThemeApplied(themeMode: String): Boolean =
        _restoreState.compareAndSet(
            BackupRestoreUiState.ApplyTheme(themeMode),
            BackupRestoreUiState.Success,
        )

    fun consumeResult(result: BackupRestoreUiState): Boolean {
        if (result != BackupRestoreUiState.Success &&
            result != BackupRestoreUiState.Failure
        ) {
            return false
        }
        return _restoreState.compareAndSet(result, BackupRestoreUiState.Idle)
    }
}
