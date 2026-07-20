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
import com.fixupxer.utils.Constants
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HistoryPreferencesTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext
        preferences().edit().clear().commit()
        preferencesManager = PreferencesManager(context)
    }

    @After
    fun tearDown() {
        preferences().edit().clear().commit()
    }

    @Test
    fun `legacy limit stays raw until explicitly accepted while export is clamped`() {
        preferences().edit().putInt("max_history_entries", 20_000).commit()

        assertEquals(20_000, preferencesManager.getMaxHistoryEntries())
        assertEquals(20_000, preferencesManager.getPendingLegacyHistoryLimit())
        assertEquals(Constants.MAX_HISTORY_ENTRIES, preferencesManager.getSupportedHistoryLimit())
        assertEquals(
            Constants.MAX_HISTORY_ENTRIES,
            preferencesManager.exportSettingsSnapshot().maxHistoryEntries,
        )
        assertEquals(20_000, preferencesManager.getMaxHistoryEntries())

        preferencesManager.setMaxHistoryEntries(preferencesManager.getSupportedHistoryLimit())
        assertNull(preferencesManager.getPendingLegacyHistoryLimit())
        assertEquals(Constants.MAX_HISTORY_ENTRIES, preferencesManager.getMaxHistoryEntries())
    }

    @Test
    fun `legacy limit below minimum stays raw while export clamps to minimum`() {
        preferences().edit().putInt("max_history_entries", 0).commit()

        assertEquals(0, preferencesManager.getMaxHistoryEntries())
        assertEquals(0, preferencesManager.getPendingLegacyHistoryLimit())
        assertEquals(
            Constants.MIN_HISTORY_ENTRIES,
            preferencesManager.exportSettingsSnapshot().maxHistoryEntries,
        )
        assertEquals(0, preferencesManager.getMaxHistoryEntries())
    }

    @Test
    fun `new history writes accept only shared supported boundaries`() {
        preferencesManager.setMaxHistoryEntries(Constants.MIN_HISTORY_ENTRIES)
        assertEquals(Constants.MIN_HISTORY_ENTRIES, preferencesManager.getMaxHistoryEntries())

        preferencesManager.setMaxHistoryEntries(Constants.MAX_HISTORY_ENTRIES)
        assertEquals(Constants.MAX_HISTORY_ENTRIES, preferencesManager.getMaxHistoryEntries())

        assertThrows(IllegalArgumentException::class.java) {
            preferencesManager.setMaxHistoryEntries(Constants.MIN_HISTORY_ENTRIES - 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            preferencesManager.setMaxHistoryEntries(Constants.MAX_HISTORY_ENTRIES + 1)
        }
    }

    private fun preferences() =
        context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
}
