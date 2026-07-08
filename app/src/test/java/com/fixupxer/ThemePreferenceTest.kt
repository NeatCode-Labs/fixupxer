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
import com.fixupxer.ui.helpers.ThemeHelper
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.TikTokProxyStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Theme preference round-trip, corrupted-value fallback and the
 * [ThemeHelper] mode mapping.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ThemePreferenceTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setUp() {
        // PreferencesManager's init mirrors custom proxies into the global stores.
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
        context = RuntimeEnvironment.getApplication()
        preferencesManager = PreferencesManager(context)
    }

    @After
    fun tearDown() {
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
    }

    @Test
    fun `default theme mode is system`() {
        assertEquals(PreferencesManager.THEME_MODE_SYSTEM, preferencesManager.getThemeMode())
    }

    @Test
    fun `theme mode round trips through preferences`() {
        preferencesManager.setThemeMode(PreferencesManager.THEME_MODE_DARK)
        assertEquals(PreferencesManager.THEME_MODE_DARK, preferencesManager.getThemeMode())

        preferencesManager.setThemeMode(PreferencesManager.THEME_MODE_LIGHT)
        assertEquals(PreferencesManager.THEME_MODE_LIGHT, preferencesManager.getThemeMode())
    }

    @Test
    fun `unknown stored value falls back to system`() {
        context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString("theme_mode", "amoled_from_the_future")
            .commit()

        assertEquals(PreferencesManager.THEME_MODE_SYSTEM, preferencesManager.getThemeMode())
    }

    @Test
    fun `theme helper maps modes to night mode flags`() {
        ThemeHelper.apply(PreferencesManager.THEME_MODE_LIGHT)
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())

        ThemeHelper.apply(PreferencesManager.THEME_MODE_DARK)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())

        ThemeHelper.apply(PreferencesManager.THEME_MODE_SYSTEM)
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.getDefaultNightMode()
        )
    }
}
