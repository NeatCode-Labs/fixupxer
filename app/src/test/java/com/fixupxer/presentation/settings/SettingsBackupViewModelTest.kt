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

import com.fixupxer.PreferencesManager
import com.fixupxer.backup.LocalBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsBackupViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful restore retains theme application and success phases`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val manager: LocalBackupManager = mock()
        val preferences: PreferencesManager = mock()
        whenever(preferences.getThemeMode()).thenReturn(
            PreferencesManager.THEME_MODE_LIGHT,
            PreferencesManager.THEME_MODE_DARK,
        )
        whenever(manager.restore(any())).thenReturn(Result.success(Unit))
        val viewModel = SettingsBackupViewModel(manager, preferences)

        viewModel.restore("backup")
        advanceUntilIdle()

        assertEquals(
            BackupRestoreUiState.ApplyTheme(PreferencesManager.THEME_MODE_DARK),
            viewModel.restoreState.value,
        )
        assertFalse(viewModel.markThemeApplied(PreferencesManager.THEME_MODE_LIGHT))
        assertTrue(viewModel.markThemeApplied(PreferencesManager.THEME_MODE_DARK))
        assertEquals(BackupRestoreUiState.Success, viewModel.restoreState.value)
        assertTrue(viewModel.consumeResult(BackupRestoreUiState.Success))
        assertFalse(viewModel.consumeResult(BackupRestoreUiState.Success))
        assertEquals(BackupRestoreUiState.Idle, viewModel.restoreState.value)
    }

    @Test
    fun `failed restore never requests theme application`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val manager: LocalBackupManager = mock()
        val preferences: PreferencesManager = mock()
        whenever(preferences.getThemeMode()).thenReturn(PreferencesManager.THEME_MODE_LIGHT)
        whenever(manager.restore(any())).thenReturn(Result.failure(IllegalStateException("bad")))
        val viewModel = SettingsBackupViewModel(manager, preferences)

        viewModel.restore("backup")
        advanceUntilIdle()

        assertEquals(BackupRestoreUiState.Failure, viewModel.restoreState.value)
    }
}
