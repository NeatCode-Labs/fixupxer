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

import com.fixupxer.ui.helpers.BrowserStatusTextHelper
import com.fixupxer.utils.BrowserEffectiveStatus
import com.fixupxer.utils.BrowserPrivacyHealth
import com.fixupxer.utils.BrowserPrivacySummary
import com.fixupxer.utils.BrowserSettingsStateResolver
import com.fixupxer.utils.DefaultBrowserStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserSettingsStateResolverTest {

    @Test
    fun `preference alias mismatch and failed operation need attention first`() {
        listOf(
            resolve(preference = true, alias = false),
            resolve(preference = false, alias = true),
            resolve(
                preference = false,
                alias = false,
                aliasOperationFailed = true,
            ),
        ).forEach { state ->
            assertEquals(BrowserEffectiveStatus.NEEDS_ATTENTION, state.effectiveStatus)
        }
    }

    @Test
    fun `alias off matrix distinguishes default browser states`() {
        assertEquals(
            BrowserEffectiveStatus.NEEDS_ATTENTION,
            resolve(false, false, DefaultBrowserStatus.FIXUPXER).effectiveStatus,
        )
        assertEquals(
            BrowserEffectiveStatus.OFF,
            resolve(false, false, DefaultBrowserStatus.OTHER_OR_UNSET).effectiveStatus,
        )
        assertEquals(
            BrowserEffectiveStatus.OFF_NOT_VERIFIED,
            resolve(false, false, DefaultBrowserStatus.UNKNOWN).effectiveStatus,
        )
    }

    @Test
    fun `alias on matrix distinguishes Android setup states`() {
        assertEquals(
            BrowserEffectiveStatus.NEEDS_ANDROID_SETUP,
            resolve(true, true, DefaultBrowserStatus.OTHER_OR_UNSET).effectiveStatus,
        )
        assertEquals(
            BrowserEffectiveStatus.UNABLE_TO_VERIFY,
            resolve(true, true, DefaultBrowserStatus.UNKNOWN).effectiveStatus,
        )
    }

    @Test
    fun `ready requires FixupXer default and readers healthy or off`() {
        val expected = mapOf(
            BrowserPrivacyHealth.OFF to BrowserEffectiveStatus.READY,
            BrowserPrivacyHealth.HEALTHY to BrowserEffectiveStatus.READY,
            BrowserPrivacyHealth.BROKEN to BrowserEffectiveStatus.NEEDS_ATTENTION,
            BrowserPrivacyHealth.MIXED to BrowserEffectiveStatus.NEEDS_ATTENTION,
        )
        privacySummaries.forEach { summary ->
            assertEquals(
                summary.health.name,
                expected.getValue(summary.health),
                resolve(
                    preference = true,
                    alias = true,
                    default = DefaultBrowserStatus.FIXUPXER,
                    privacy = summary,
                ).effectiveStatus,
            )
        }
    }

    @Test
    fun `gate requires preference plus alias`() {
        val state = resolve(preference = true, alias = true)
        assertTrue(state.canProcessViewIntents)
        assertTrue(BrowserSettingsStateResolver.canProcessViewIntent(true, true))
        assertFalse(BrowserSettingsStateResolver.canProcessViewIntent(false, true))
        assertFalse(BrowserSettingsStateResolver.canProcessViewIntent(true, false))
    }

    @Test
    fun `saved choices show off only for an off preference and zero after delete`() {
        val aliasMismatch = resolve(preference = true, alias = false)
        assertEquals(
            BrowserStatusTextHelper.SavedChoicesStatus.SETUP_INCOMPLETE,
            BrowserStatusTextHelper.resolveSavedChoicesStatus(2, aliasMismatch, false),
        )
        assertEquals(
            BrowserStatusTextHelper.SavedChoicesStatus.BROWSER_OFF,
            BrowserStatusTextHelper.resolveSavedChoicesStatus(
                2,
                resolve(preference = false, alias = false),
                false,
            ),
        )
        assertEquals(
            BrowserStatusTextHelper.SavedChoicesStatus.NONE,
            BrowserStatusTextHelper.resolveSavedChoicesStatus(0, aliasMismatch, false),
        )
    }

    private fun resolve(
        preference: Boolean,
        alias: Boolean,
        default: DefaultBrowserStatus = DefaultBrowserStatus.OTHER_OR_UNSET,
        privacy: BrowserPrivacySummary = BrowserPrivacySummary(0, 0),
        aliasOperationFailed: Boolean = false,
    ) = BrowserSettingsStateResolver.resolve(
        preferenceEnabled = preference,
        aliasEnabled = alias,
        defaultBrowserStatus = default,
        privacySummary = privacy,
        aliasOperationFailed = aliasOperationFailed,
    )

    private companion object {
        val privacySummaries = listOf(
            BrowserPrivacySummary(activeCount = 0, attentionCount = 0),
            BrowserPrivacySummary(activeCount = 2, attentionCount = 0),
            BrowserPrivacySummary(activeCount = 0, attentionCount = 2),
            BrowserPrivacySummary(activeCount = 1, attentionCount = 1),
        )
    }
}
