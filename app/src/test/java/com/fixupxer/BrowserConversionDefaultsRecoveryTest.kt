// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
 * This program is free software under the GNU General Public License,
 * version 3 or (at your option) any later version.
 */

package com.fixupxer

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.fixupxer.processing.BrowserConversionMode
import com.fixupxer.processing.BrowserFrontendPreference
import com.fixupxer.ui.helpers.BrowserConversionDefaultsHelper
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import com.fixupxer.utils.TikTokProxyStore
import org.junit.After
import org.junit.Assert.assertEquals
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
class BrowserConversionDefaultsRecoveryTest {
    private lateinit var context: Context
    private lateinit var preferences: PreferencesManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication().applicationContext
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        ProxyRoster.reset()
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
        preferences = PreferencesManager(context)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        ProxyRoster.reset()
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
    }

    @Test
    fun `draft contains all seven configurable platforms`() {
        val draft = BrowserConversionDefaultsHelper.createDraft(preferences)
        val themed = ContextThemeWrapper(context, R.style.Theme_FixupXer)
        val rows = BrowserConversionDefaultsHelper.populateContainer(
            themed,
            LayoutInflater.from(themed),
            LinearLayout(themed),
            draft,
            onChangeTarget = {},
        )

        assertEquals(7, rows.size)
        assertEquals(
            BrowserConversionDefaultsHelper.entries.map { it.platform }.toSet(),
            rows.map { it.entry.platform }.toSet(),
        )
    }

    @Test
    fun `cancelled reader restore and selection do not mutate prefs or roster`() {
        preferences.disableBuiltIn(ProxyPlatform.X, "x_xcancel")
        val before = preferences.exportSettingsSnapshot()
        val beforeRoster = ProxyRoster.getDisabledBuiltIns(ProxyPlatform.X)
        val draft = BrowserConversionDefaultsHelper.createDraft(preferences)

        draft.restoreCategory(ProxyPlatform.X, BrowserConversionMode.READER)
        draft.select(
            ProxyPlatform.X,
            BrowserFrontendPreference(BrowserConversionMode.READER, "x_xcancel"),
        )

        assertEquals(before, preferences.exportSettingsSnapshot())
        assertEquals(beforeRoster, ProxyRoster.getDisabledBuiltIns(ProxyPlatform.X))
    }

    @Test
    fun `disabled saved reader displays fallback without overwriting raw id`() {
        val initial = preferences.getBrowserFrontendPreferences()
        preferences.saveBrowserFrontendPreferences(
            mapOf(
                ProxyPlatform.X to BrowserFrontendPreference(
                    BrowserConversionMode.READER,
                    "x_xcancel",
                )
            ),
            initial,
        )
        preferences.disableBuiltIn(ProxyPlatform.X, "x_xcancel")

        val draft = BrowserConversionDefaultsHelper.createDraft(preferences)

        assertEquals("x_xcancel", draft.preference(ProxyPlatform.X).targetId)
        assertEquals("x_nitter_net", draft.effectiveTarget(ProxyPlatform.X)?.id)
        assertTrue(draft.isUsingFallback(ProxyPlatform.X))
    }

    @Test
    fun `save restores only requested category and preserves dormant clean choice`() {
        preferences.disableBuiltIn(ProxyPlatform.X, "x_fixupx")
        preferences.disableBuiltIn(ProxyPlatform.X, "x_xcancel")
        val draft = BrowserConversionDefaultsHelper.createDraft(preferences)
        draft.restoreCategory(ProxyPlatform.X, BrowserConversionMode.READER)
        draft.select(
            ProxyPlatform.X,
            BrowserFrontendPreference(BrowserConversionMode.CLEAN_ONLY, "x_xcancel"),
        )

        assertTrue(draft.apply())
        assertTrue("x_fixupx" in preferences.getDisabledBuiltIns(ProxyPlatform.X))
        assertFalse("x_xcancel" in preferences.getDisabledBuiltIns(ProxyPlatform.X))
        assertEquals(
            BrowserFrontendPreference(BrowserConversionMode.CLEAN_ONLY, "x_xcancel"),
            preferences.getBrowserFrontendPreferences()[ProxyPlatform.X],
        )
    }

    @Test
    fun `sequential reader and embed restores remain explicit draft operations`() {
        preferences.disableBuiltIn(ProxyPlatform.X, "x_fixupx")
        preferences.disableBuiltIn(ProxyPlatform.X, "x_xcancel")
        val draft = BrowserConversionDefaultsHelper.createDraft(preferences)

        draft.restoreCategory(ProxyPlatform.X, BrowserConversionMode.READER)
        draft.restoreCategory(ProxyPlatform.X, BrowserConversionMode.EMBED)

        assertTrue(draft.apply())
        assertFalse("x_fixupx" in preferences.getDisabledBuiltIns(ProxyPlatform.X))
        assertFalse("x_xcancel" in preferences.getDisabledBuiltIns(ProxyPlatform.X))
    }

    @Test
    fun `custom target is selectable but Browser draft does not edit custom roster`() {
        preferences.addCustomProxy(ProxyPlatform.FACEBOOK, "reader.example")
        val draft = BrowserConversionDefaultsHelper.createDraft(preferences)
        val custom = draft.activeTargets(ProxyPlatform.FACEBOOK).single()
        draft.select(
            ProxyPlatform.FACEBOOK,
            BrowserFrontendPreference(BrowserConversionMode.CUSTOM, custom.id),
        )

        assertTrue(draft.apply())
        assertEquals(listOf("reader.example"), preferences.getCustomProxies(ProxyPlatform.FACEBOOK))
        assertEquals(BrowserConversionMode.CUSTOM, preferences.getBrowserFrontendPreferences()[ProxyPlatform.FACEBOOK]?.mode)
    }

    @Test
    fun `stale draft save fails without overwriting newer choice`() {
        val draft = BrowserConversionDefaultsHelper.createDraft(preferences)
        draft.select(
            ProxyPlatform.X,
            BrowserFrontendPreference(BrowserConversionMode.READER, "x_xcancel"),
        )
        val current = preferences.getBrowserFrontendPreferences()
        preferences.saveBrowserFrontendPreferences(
            mapOf(
                ProxyPlatform.X to BrowserFrontendPreference(
                    BrowserConversionMode.EMBED,
                    AlternativeFrontendCatalog.defaultTargetId(ProxyPlatform.X),
                )
            ),
            current,
        )

        assertFalse(draft.apply())
        assertEquals(
            BrowserConversionMode.EMBED,
            preferences.getBrowserFrontendPreferences()[ProxyPlatform.X]?.mode,
        )
    }

    private companion object { const val PREFS_NAME = "FixupXerPrefs" }
}
