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
import com.fixupxer.PreferencesManager
import timber.log.Timber

enum class DefaultBrowserStatus {
    FIXUPXER,
    OTHER_OR_UNSET,
    UNKNOWN,
}

data class BrowserAliasUpdateResult(
    val success: Boolean,
    val rollbackSucceeded: Boolean = true,
) {
    val needsAttention: Boolean
        get() = !success && !rollbackSucceeded
}

/**
 * Utility functions for browser mode functionality
 */
object BrowserModeUtils {
    
    /**
     * Enable or disable the browser alias component
     */
    fun setBrowserAliasEnabled(context: Context, enable: Boolean): Boolean {
        BrowserViewGate.invalidate()
        return try {
            val pm = context.packageManager
            val cn = ComponentName(context, "${context.packageName}.BrowserAlias")
            val newState = if (enable) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(cn, newState, PackageManager.DONT_KILL_APP)
            val verified = isBrowserAliasEnabled(context) == enable
            if (verified) {
                Timber.d("Browser alias enabled: $enable")
            } else {
                Timber.e("Browser alias state verification failed")
            }
            verified
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle browser alias")
            false
        }
    }

    /**
     * Persists the desired state, applies the component state, verifies both and
     * rolls both values back if any step fails.
     */
    fun updateBrowserMode(
        context: Context,
        preferencesManager: PreferencesManager,
        enable: Boolean,
    ): BrowserAliasUpdateResult = executeBrowserAliasTransaction(
        desiredEnabled = enable,
        readPreference = preferencesManager::isBrowserModeEnabled,
        writePreference = preferencesManager::setBrowserModeEnabled,
        readAlias = { isBrowserAliasEnabled(context) },
        writeAlias = { setBrowserAliasEnabled(context, it) },
    )

    /**
     * Reconciles cloud/local preference restore mismatches at process startup.
     * The preference remains the desired source of truth.
     */
    fun reconcileBrowserAlias(
        context: Context,
        preferencesManager: PreferencesManager,
    ): Boolean {
        val desired = preferencesManager.isBrowserModeEnabled()
        if (isBrowserAliasEnabled(context) == desired) return true
        val reconciled = setBrowserAliasEnabled(context, desired)
        if (!reconciled) {
            Timber.e("Failed to reconcile browser alias with desired preference")
        }
        return reconciled
    }

    internal fun executeBrowserAliasTransaction(
        desiredEnabled: Boolean,
        readPreference: () -> Boolean,
        writePreference: (Boolean) -> Boolean,
        readAlias: () -> Boolean,
        writeAlias: (Boolean) -> Boolean,
    ): BrowserAliasUpdateResult {
        val previousPreference = readPreference()
        val previousAlias = readAlias()

        val preferenceWritten = writePreference(desiredEnabled)
        val aliasWritten = preferenceWritten && writeAlias(desiredEnabled)
        val verified = preferenceWritten &&
            aliasWritten &&
            readPreference() == desiredEnabled &&
            readAlias() == desiredEnabled
        if (verified) return BrowserAliasUpdateResult(success = true)

        val aliasRolledBack = writeAlias(previousAlias) && readAlias() == previousAlias
        val preferenceRolledBack =
            writePreference(previousPreference) && readPreference() == previousPreference
        return BrowserAliasUpdateResult(
            success = false,
            rollbackSucceeded = aliasRolledBack && preferenceRolledBack,
        )
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