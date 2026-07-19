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

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import timber.log.Timber

enum class DefaultBrowserStatus {
    FIXUPXER,
    OTHER_OR_UNSET,
    UNKNOWN,
}

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

    /**
     * Read-only check of whether FixupXer appears to be the default browser.
     * Does not launch intents or use the network.
     */
    fun getDefaultBrowserStatus(context: Context): DefaultBrowserStatus {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                    DefaultBrowserStatus.UNKNOWN
                } else if (roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                    DefaultBrowserStatus.FIXUPXER
                } else {
                    DefaultBrowserStatus.OTHER_OR_UNSET
                }
            } else {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.BROWSER_PROBE_URL))
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                if (resolveInfo == null) {
                    DefaultBrowserStatus.UNKNOWN
                } else {
                    val resolvedPackage = resolveInfo.activityInfo?.packageName
                    when {
                        resolvedPackage == context.packageName -> DefaultBrowserStatus.FIXUPXER
                        resolvedPackage == "android" || resolveInfo.activityInfo == null ->
                            DefaultBrowserStatus.UNKNOWN
                        else -> DefaultBrowserStatus.OTHER_OR_UNSET
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to determine default browser status")
            DefaultBrowserStatus.UNKNOWN
        }
    }
} 