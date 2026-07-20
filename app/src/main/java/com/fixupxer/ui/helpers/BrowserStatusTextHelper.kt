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

package com.fixupxer.ui.helpers

import android.content.Context
import androidx.annotation.StringRes
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.utils.BrowserEffectiveStatus
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.BrowserPrivacySummary
import com.fixupxer.utils.BrowserSettingsState
import com.fixupxer.utils.BrowserSettingsStateResolver

object BrowserStatusTextHelper {

    enum class SavedChoicesStatus {
        NONE,
        BROWSER_OFF,
        SETUP_INCOMPLETE,
        AUTOMATIC,
        READY,
    }

    fun resolveState(
        context: Context,
        preferencesManager: PreferencesManager,
        aliasOperationFailed: Boolean = false,
    ): BrowserSettingsState {
        val privacySummary = privacySummary(preferencesManager)
        return BrowserSettingsStateResolver.resolve(
            preferenceEnabled = preferencesManager.isBrowserModeEnabled(),
            aliasEnabled = BrowserModeUtils.isBrowserAliasEnabled(context),
            defaultBrowserStatus = BrowserModeUtils.getDefaultBrowserStatus(context),
            privacySummary = privacySummary,
            aliasOperationFailed = aliasOperationFailed,
        )
    }

    fun privacySummary(preferencesManager: PreferencesManager): BrowserPrivacySummary {
        var active = 0
        var attention = 0
        BrowserConversionDefaultsHelper.entries.forEach { entry ->
            if (!entry.getter(preferencesManager)) return@forEach
            if (preferencesManager.resolveBrowserPrivacyTarget(entry.platform) == null) {
                attention++
            } else {
                active++
            }
        }
        return BrowserPrivacySummary(activeCount = active, attentionCount = attention)
    }

    fun resolveSavedChoicesStatus(
        count: Int,
        state: BrowserSettingsState,
        automaticActions: Boolean,
    ): SavedChoicesStatus {
        require(count >= 0)
        return when {
            count == 0 -> SavedChoicesStatus.NONE
            !state.preferenceEnabled -> SavedChoicesStatus.BROWSER_OFF
            !state.canProcessViewIntents ||
                state.effectiveStatus == BrowserEffectiveStatus.NEEDS_ATTENTION ||
                state.effectiveStatus == BrowserEffectiveStatus.NEEDS_ANDROID_SETUP ||
                state.effectiveStatus == BrowserEffectiveStatus.UNABLE_TO_VERIFY ->
                SavedChoicesStatus.SETUP_INCOMPLETE
            automaticActions -> SavedChoicesStatus.AUTOMATIC
            else -> SavedChoicesStatus.READY
        }
    }

    @StringRes
    fun statusTextRes(status: BrowserEffectiveStatus): Int = when (status) {
        BrowserEffectiveStatus.NEEDS_ATTENTION -> R.string.browser_status_needs_attention
        BrowserEffectiveStatus.OFF -> R.string.browser_status_off
        BrowserEffectiveStatus.OFF_NOT_VERIFIED -> R.string.browser_status_off_not_verified
        BrowserEffectiveStatus.NEEDS_ANDROID_SETUP -> R.string.browser_status_needs_android_setup
        BrowserEffectiveStatus.UNABLE_TO_VERIFY -> R.string.browser_status_unable_to_verify
        BrowserEffectiveStatus.READY -> R.string.browser_status_ready
    }
}
