// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
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
import com.fixupxer.utils.ProxyRoster
import com.fixupxer.utils.TikTokProxyStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DominantHandPreferenceTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setUp() {
        ProxyRoster.reset()
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        preferencesManager = PreferencesManager(context)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        ProxyRoster.reset()
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
    }

    @Test
    fun `dominant hand defaults to right`() {
        assertEquals(
            PreferencesManager.DOMINANT_HAND_RIGHT,
            preferencesManager.getDominantHand(),
        )
    }

    @Test
    fun `supported dominant hand values persist`() {
        preferencesManager.setDominantHand(PreferencesManager.DOMINANT_HAND_LEFT)
        assertEquals(
            PreferencesManager.DOMINANT_HAND_LEFT,
            preferencesManager.getDominantHand(),
        )

        preferencesManager.setDominantHand(PreferencesManager.DOMINANT_HAND_RIGHT)
        assertEquals(
            PreferencesManager.DOMINANT_HAND_RIGHT,
            preferencesManager.getDominantHand(),
        )
    }

    @Test
    fun `invalid stored dominant hand falls back to right`() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DOMINANT_HAND, "ambidextrous")
            .commit()

        assertEquals(
            PreferencesManager.DOMINANT_HAND_RIGHT,
            preferencesManager.getDominantHand(),
        )
    }

    @Test
    fun `setter ignores unsupported dominant hand`() {
        preferencesManager.setDominantHand(PreferencesManager.DOMINANT_HAND_LEFT)
        preferencesManager.setDominantHand("ambidextrous")

        assertEquals(
            PreferencesManager.DOMINANT_HAND_LEFT,
            preferencesManager.getDominantHand(),
        )
        assertEquals(
            PreferencesManager.DOMINANT_HAND_LEFT,
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DOMINANT_HAND, null),
        )
    }

    private companion object {
        const val PREFS_NAME = "FixupXerPrefs"
        const val KEY_DOMINANT_HAND = "dominant_hand"
    }
}
