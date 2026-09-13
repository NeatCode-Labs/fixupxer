// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026 NeatCode Labs
 *
 * This program is free software under the GNU General Public License,
 * version 3 or (at your option) any later version.
 */

package com.fixupxer

import android.content.Context
import android.content.res.Configuration
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.backup.LocalBackupManager
import com.fixupxer.presentation.settings.BackupRestoreUiState
import com.fixupxer.presentation.settings.SettingsBackupViewModel
import com.fixupxer.ui.SettingsActivity
import com.fixupxer.ui.BrowserSettingsActivity
import com.fixupxer.ui.helpers.ThemeHelper
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.ProxyRoster
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsBackupThemeRecreationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.targetContext
    private lateinit var manager: LocalBackupManager
    private lateinit var preferences: PreferencesManager
    private lateinit var originalBackup: String
    private var originalAlias = false
    private var originalNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

    @Before
    fun setUp() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { manager = it.localBackupManager }
        }
        originalBackup = runBlocking { manager.exportJson() }
        originalAlias = BrowserModeUtils.isBrowserAliasEnabled(context)
        instrumentation.runOnMainSync { originalNightMode = AppCompatDelegate.getDefaultNightMode() }
        assertTrue(context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE).edit().clear().commit())
        ProxyRoster.reset()
        preferences = PreferencesManager(context)
        preferences.setBrowserModeEnabled(false)
        preferences.setHistoryEnabled(false)
    }

    @After
    fun tearDown() {
        if (::originalBackup.isInitialized) {
            assertTrue(runBlocking { manager.restore(originalBackup) }.isSuccess)
            BrowserModeUtils.setBrowserAliasEnabled(context, originalAlias)
            instrumentation.runOnMainSync { AppCompatDelegate.setDefaultNightMode(originalNightMode) }
        }
    }

    @Test
    fun systemThemeRestoreDoesNotReapplyOldCheckedStates() {
        val systemDark = context.applicationContext.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        assertThemeRestore(
            targetTheme = PreferencesManager.THEME_MODE_SYSTEM,
            oldTheme = if (systemDark) PreferencesManager.THEME_MODE_LIGHT else PreferencesManager.THEME_MODE_DARK,
            targetButton = R.id.buttonThemeSystem,
        )
    }

    @Test
    fun darkThemeRestoreDoesNotReapplyOldCheckedStates() {
        assertThemeRestore(
            targetTheme = PreferencesManager.THEME_MODE_DARK,
            oldTheme = PreferencesManager.THEME_MODE_LIGHT,
            targetButton = R.id.buttonThemeDark,
        )
    }

    @Test
    fun browserSettingsRecreationKeepsNewerPreferences() {
        preferences.setBrowserModeEnabled(true)
        preferences.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        assertTrue(BrowserModeUtils.setBrowserAliasEnabled(context, true))
        ActivityScenario.launch(BrowserSettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.findViewById<MaterialSwitch>(R.id.switchBrowserMode).isChecked)
                // Another settings transaction commits while this view still has old values.
                preferences.setBrowserModeEnabled(false)
                preferences.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
                assertTrue(BrowserModeUtils.setBrowserAliasEnabled(context, false))
            }
            scenario.recreate()
            assertFalse(preferences.isBrowserModeEnabled())
            assertFalse(BrowserModeUtils.isBrowserAliasEnabled(context))
            assertEquals(PreferencesManager.ACTION_MODE_PRIORITY, preferences.getActionMode())
            scenario.onActivity { activity ->
                assertFalse(activity.findViewById<MaterialSwitch>(R.id.switchBrowserMode).isChecked)
                assertTrue(activity.findViewById<RadioButton>(R.id.radioFollowPriority).isChecked)
            }
        }
    }

    private fun assertThemeRestore(targetTheme: String, oldTheme: String, targetButton: Int) {
        preferences.setThemeMode(targetTheme)
        preferences.setDominantHand(PreferencesManager.DOMINANT_HAND_RIGHT)
        preferences.setCustomRulesEnabled(false)
        val backup = runBlocking { manager.exportJson() }
        preferences.setThemeMode(oldTheme)
        preferences.setDominantHand(PreferencesManager.DOMINANT_HAND_LEFT)
        preferences.setCustomRulesEnabled(true)
        instrumentation.runOnMainSync { ThemeHelper.apply(oldTheme) }

        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            var originalActivity: SettingsActivity? = null
            scenario.onActivity { activity ->
                originalActivity = activity
                assertTrue(activity.findViewById<MaterialButton>(R.id.buttonHandLeft).isChecked)
                ViewModelProvider(activity)[SettingsBackupViewModel::class.java].restore(backup)
            }
            val deadline = System.nanoTime() + 10_000_000_000L
            var completed = false
            while (!completed && System.nanoTime() < deadline) {
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    completed = activity !== originalActivity &&
                        ViewModelProvider(activity)[SettingsBackupViewModel::class.java].restoreState.value ==
                        BackupRestoreUiState.Idle &&
                        activity.findViewById<MaterialButton>(targetButton).isChecked
                }
                if (!completed) Thread.sleep(50)
            }
            assertTrue("Restore must finish in a recreated Activity with the restored theme", completed)
            // A second idle checkpoint catches callbacks from the old checked state.
            instrumentation.waitForIdleSync()
            assertEquals(targetTheme, preferences.getThemeMode())
            assertEquals(PreferencesManager.DOMINANT_HAND_RIGHT, preferences.getDominantHand())
            assertFalse(preferences.areCustomRulesEnabled())
            scenario.onActivity { activity ->
                assertTrue(activity.findViewById<MaterialButton>(targetButton).isChecked)
                assertTrue(activity.findViewById<MaterialButton>(R.id.buttonHandRight).isChecked)
                assertFalse(activity.findViewById<MaterialSwitch>(R.id.switchCustomRules).isChecked)
            }
        }
    }
}
