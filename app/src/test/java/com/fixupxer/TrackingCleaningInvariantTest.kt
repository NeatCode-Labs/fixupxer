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

package com.fixupxer

import android.content.Context
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.TikTokProxyStore
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TrackingCleaningInvariantTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
    }

    @Test
    fun `legacy disabled preference is migrated to enabled`() {
        val rawPreferences = context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
        rawPreferences.edit().putBoolean("clean_tracking", false).commit()

        val preferencesManager = PreferencesManager(context)

        assertTrue(preferencesManager.isCleanTrackingEnabled())
        assertTrue(rawPreferences.getBoolean("clean_tracking", false))
    }

    @Test
    fun `backup cannot disable tracking cleaning`() {
        val preferencesManager = PreferencesManager(context)
        val legacySnapshot = preferencesManager.exportSettingsSnapshot().copy(cleanTracking = false)

        assertTrue(preferencesManager.replaceSettingsSnapshot(legacySnapshot))
        assertTrue(preferencesManager.isCleanTrackingEnabled())
        assertTrue(preferencesManager.exportSettingsSnapshot().cleanTracking)
    }
}
