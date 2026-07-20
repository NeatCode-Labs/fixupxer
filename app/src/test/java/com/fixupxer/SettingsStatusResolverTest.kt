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

package com.fixupxer

import com.fixupxer.utils.CustomRulesEffectiveStatus
import com.fixupxer.utils.SettingsStatusResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsStatusResolverTest {

    @Test
    fun `custom rules status covers empty active disabled paused and mixed libraries`() {
        assertStatus(CustomRulesEffectiveStatus.NO_RULES, false, 0, 0)
        assertStatus(CustomRulesEffectiveStatus.NO_RULES, true, 0, 0)
        assertStatus(CustomRulesEffectiveStatus.ON_WITH_ACTIVE_RULES, true, 2, 0)
        assertStatus(CustomRulesEffectiveStatus.ON_WITH_ACTIVE_RULES, true, 2, 3)
        assertStatus(CustomRulesEffectiveStatus.ON_WITHOUT_ACTIVE_RULES, true, 0, 3)
        assertStatus(CustomRulesEffectiveStatus.OFF_WITH_PAUSED_RULES, false, 2, 0)
        assertStatus(CustomRulesEffectiveStatus.OFF_WITH_PAUSED_RULES, false, 2, 3)
        assertStatus(CustomRulesEffectiveStatus.OFF_WITH_DISABLED_RULES, false, 0, 3)
    }

    private fun assertStatus(
        expected: CustomRulesEffectiveStatus,
        masterEnabled: Boolean,
        enabledCount: Int,
        disabledCount: Int,
    ) {
        assertEquals(
            expected,
            SettingsStatusResolver.resolveCustomRules(
                masterEnabled = masterEnabled,
                enabledCount = enabledCount,
                disabledCount = disabledCount,
            ).status,
        )
    }
}
