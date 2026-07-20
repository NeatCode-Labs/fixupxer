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

package com.fixupxer.ui.helpers

import androidx.appcompat.app.AppCompatDelegate
import com.fixupxer.PreferencesManager

/**
 * Applies the user's theme preference (system / light / dark) app-wide.
 */
object ThemeHelper {

    fun apply(themeMode: String) {
        AppCompatDelegate.setDefaultNightMode(nightModeFor(themeMode))
    }

    fun nightModeFor(themeMode: String): Int =
        when (themeMode) {
            PreferencesManager.THEME_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            PreferencesManager.THEME_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
}
