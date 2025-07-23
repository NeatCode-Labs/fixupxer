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

package com.fixupxer.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import timber.log.Timber

/**
 * Utility functions for browser mode functionality
 */
object BrowserModeUtils {
    
    /**
     * Enable or disable the browser alias component
     */
    fun setBrowserAliasEnabled(context: Context, enable: Boolean) {
        try {
            val pm = context.packageManager
            val cn = ComponentName(context, "${context.packageName}.BrowserAlias")
            val newState = if (enable) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(cn, newState, PackageManager.DONT_KILL_APP)
            Timber.d("Browser alias enabled: $enable")
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle browser alias")
        }
    }
    
    /**
     * Check if the browser alias is currently enabled
     */
    fun isBrowserAliasEnabled(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val cn = ComponentName(context, "${context.packageName}.BrowserAlias")
            val state = pm.getComponentEnabledSetting(cn)
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } catch (e: Exception) {
            Timber.e(e, "Failed to check browser alias state")
            false
        }
    }
} 