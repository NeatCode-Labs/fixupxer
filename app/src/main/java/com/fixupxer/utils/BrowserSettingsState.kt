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

package com.fixupxer.utils

enum class BrowserPrivacyHealth {
    OFF,
    HEALTHY,
    BROKEN,
    MIXED,
}

data class BrowserPrivacySummary(
    val activeCount: Int,
    val attentionCount: Int,
) {
    init {
        require(activeCount >= 0)
        require(attentionCount >= 0)
    }

    val health: BrowserPrivacyHealth
        get() = when {
            activeCount == 0 && attentionCount == 0 -> BrowserPrivacyHealth.OFF
            activeCount > 0 && attentionCount == 0 -> BrowserPrivacyHealth.HEALTHY
            activeCount == 0 -> BrowserPrivacyHealth.BROKEN
            else -> BrowserPrivacyHealth.MIXED
        }
}

enum class BrowserEffectiveStatus {
    NEEDS_ATTENTION,
    OFF,
    OFF_NOT_VERIFIED,
    NEEDS_ANDROID_SETUP,
    UNABLE_TO_VERIFY,
    READY,
}

data class BrowserSettingsState(
    val preferenceEnabled: Boolean,
    val aliasEnabled: Boolean,
    val defaultBrowserStatus: DefaultBrowserStatus,
    val privacySummary: BrowserPrivacySummary,
    val aliasOperationFailed: Boolean,
    val effectiveStatus: BrowserEffectiveStatus,
) {
    val canProcessViewIntents: Boolean
        get() = preferenceEnabled && aliasEnabled
}

/**
 * Pure Browser-mode state resolver shared by Settings UI and the VIEW-intent gate.
 */
object BrowserSettingsStateResolver {

    fun resolve(
        preferenceEnabled: Boolean,
        aliasEnabled: Boolean,
        defaultBrowserStatus: DefaultBrowserStatus,
        privacySummary: BrowserPrivacySummary,
        aliasOperationFailed: Boolean = false,
    ): BrowserSettingsState {
        val effectiveStatus = when {
            aliasOperationFailed || preferenceEnabled != aliasEnabled ->
                BrowserEffectiveStatus.NEEDS_ATTENTION
            !aliasEnabled && defaultBrowserStatus == DefaultBrowserStatus.FIXUPXER ->
                BrowserEffectiveStatus.NEEDS_ATTENTION
            !aliasEnabled && defaultBrowserStatus == DefaultBrowserStatus.UNKNOWN ->
                BrowserEffectiveStatus.OFF_NOT_VERIFIED
            !aliasEnabled ->
                BrowserEffectiveStatus.OFF
            defaultBrowserStatus == DefaultBrowserStatus.OTHER_OR_UNSET ->
                BrowserEffectiveStatus.NEEDS_ANDROID_SETUP
            defaultBrowserStatus == DefaultBrowserStatus.UNKNOWN ->
                BrowserEffectiveStatus.UNABLE_TO_VERIFY
            privacySummary.health == BrowserPrivacyHealth.BROKEN ||
                privacySummary.health == BrowserPrivacyHealth.MIXED ->
                BrowserEffectiveStatus.NEEDS_ATTENTION
            else ->
                BrowserEffectiveStatus.READY
        }

        return BrowserSettingsState(
            preferenceEnabled = preferenceEnabled,
            aliasEnabled = aliasEnabled,
            defaultBrowserStatus = defaultBrowserStatus,
            privacySummary = privacySummary,
            aliasOperationFailed = aliasOperationFailed,
            effectiveStatus = effectiveStatus,
        )
    }

    fun canProcessViewIntent(
        preferenceEnabled: Boolean,
        aliasEnabled: Boolean,
    ): Boolean = preferenceEnabled && aliasEnabled
}
