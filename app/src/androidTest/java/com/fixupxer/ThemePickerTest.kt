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

package com.fixupxer

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.ui.SettingsActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Theme picker in Settings: selection persists the preference and the picker
 * restores the persisted mode when Settings opens.
 *
 * NOTE deliberately avoids switching the live night mode to dark: a global
 * uiMode change recreates every activity mid-test, which is an ANR/flake
 * factory on emulators. Dark-mode *mapping* is covered by the ThemeHelper
 * unit test (ThemePreferenceTest); here only the picker <-> preference wiring
 * needs a real device. Selecting LIGHT is safe — test devices already run in
 * light mode, so no recreation happens.
 */
@RunWith(AndroidJUnit4::class)
class ThemePickerTest {

    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext

    private fun prefs() =
        context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)

    @Before
    fun resetThemeToSystem() {
        prefs().edit().remove("theme_mode").commit()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    @After
    fun tearDown() = resetThemeToSystem()

    @Test
    fun themePickerShowsSystemByDefault() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(isRoot()).perform(waitFor(500))
            onView(withId(R.id.buttonThemeSystem)).check(matches(isChecked()))
        }
    }

    @Test
    fun selectingLightPersistsPreferenceAndAppliesNightMode() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(isRoot()).perform(waitFor(500))
            onView(withId(R.id.buttonThemeLight)).perform(click())
            onView(isRoot()).perform(waitFor(500))

            assertEquals("light", prefs().getString("theme_mode", null))
            assertEquals(
                AppCompatDelegate.MODE_NIGHT_NO,
                AppCompatDelegate.getDefaultNightMode()
            )
        }
    }

    @Test
    fun persistedSelectionIsRestoredWhenSettingsOpens() {
        // Seed the preference directly — the picker must pre-select it on load
        // without re-applying the night mode (no recreation).
        prefs().edit().putString("theme_mode", "dark").commit()

        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(isRoot()).perform(waitFor(500))
            onView(withId(R.id.buttonThemeDark)).check(matches(isChecked()))
        }
    }
}
