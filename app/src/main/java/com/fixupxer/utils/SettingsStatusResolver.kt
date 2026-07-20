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

enum class CustomRulesEffectiveStatus {
    NO_RULES,
    ON_WITH_ACTIVE_RULES,
    ON_WITHOUT_ACTIVE_RULES,
    OFF_WITH_PAUSED_RULES,
    OFF_WITH_DISABLED_RULES,
}

data class CustomRulesStatus(
    val status: CustomRulesEffectiveStatus,
    val enabledCount: Int,
    val disabledCount: Int,
)

object SettingsStatusResolver {

    fun resolveCustomRules(
        masterEnabled: Boolean,
        enabledCount: Int,
        disabledCount: Int,
    ): CustomRulesStatus {
        require(enabledCount >= 0)
        require(disabledCount >= 0)

        val status = when {
            enabledCount == 0 && disabledCount == 0 ->
                CustomRulesEffectiveStatus.NO_RULES
            masterEnabled && enabledCount > 0 ->
                CustomRulesEffectiveStatus.ON_WITH_ACTIVE_RULES
            masterEnabled ->
                CustomRulesEffectiveStatus.ON_WITHOUT_ACTIVE_RULES
            enabledCount > 0 ->
                CustomRulesEffectiveStatus.OFF_WITH_PAUSED_RULES
            else ->
                CustomRulesEffectiveStatus.OFF_WITH_DISABLED_RULES
        }
        return CustomRulesStatus(status, enabledCount, disabledCount)
    }
}
