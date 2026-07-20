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

import android.content.Context
import com.fixupxer.utils.BrowserViewGate
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.TikTokProxyStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CustomRulesPreferenceTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setUp() {
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        preferencesManager = PreferencesManager(context)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
    }

    @Test
    fun `custom rules default to disabled when preference is absent`() {
        assertFalse(preferencesManager.areCustomRulesEnabled())
    }

    @Test
    fun `custom rules explicit choice persists`() {
        preferencesManager.setCustomRulesEnabled(true)
        assertTrue(preferencesManager.areCustomRulesEnabled())

        preferencesManager.setCustomRulesEnabled(false)
        assertFalse(preferencesManager.areCustomRulesEnabled())
    }

    @Test
    fun `custom rules master switch invalidates in-flight browser work`() {
        val snapshot = BrowserViewGate.begin(true, true)!!

        preferencesManager.setCustomRulesEnabled(true)

        assertFalse(BrowserViewGate.isValid(snapshot, true, true))
    }

    @Test
    fun `custom rules flow uses the disabled default`() = runBlocking {
        assertFalse(preferencesManager.customRulesEnabledFlow().first())
    }

    private companion object {
        const val PREFS_NAME = "FixupXerPrefs"
    }
}
