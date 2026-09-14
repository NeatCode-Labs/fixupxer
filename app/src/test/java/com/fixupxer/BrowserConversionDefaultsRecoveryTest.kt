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
    fun `browser picker CRUD stays staged until save and updates shared references`() {
        val expected = preferences.getBrowserFrontendPreferences()
        assertTrue(
            preferences.saveBrowserFrontendPreferences(
                mapOf(
                    ProxyPlatform.X to BrowserFrontendPreference(
                        BrowserConversionMode.READER,
                        "x_xcancel",
                    )
                ),
                expected,
            )
        )
        preferences.setSelectedProxyDomain(
            ProxyPlatform.X,
            AlternativeFrontendCatalog.builtIn(ProxyPlatform.X)
                .single { it.id == "x_xcancel" }
                .domain,
        )

        val draft = BrowserConversionDefaultsHelper.createDraft(preferences)
        val target = draft.activeTargets(ProxyPlatform.X).single { it.id == "x_xcancel" }

        assertTrue(draft.editTarget(ProxyPlatform.X, target, "edited-reader.example"))
        assertEquals(
            listOf("edited-reader.example"),
            draft.customProxies[ProxyPlatform.X],
        )
        assertEquals("edited-reader.example", draft.selectedProxyDomains[ProxyPlatform.X])
        assertEquals(
            BrowserFrontendPreference(
                BrowserConversionMode.CUSTOM,
                "custom:edited-reader.example",
            ),
            draft.preference(ProxyPlatform.X),
        )
        assertTrue("x_xcancel" !in preferences.getDisabledBuiltIns(ProxyPlatform.X))
        assertEquals(emptyList<String>(), preferences.getCustomProxies(ProxyPlatform.X))

        assertTrue(draft.apply())
        assertTrue("x_xcancel" in preferences.getDisabledBuiltIns(ProxyPlatform.X))
        assertEquals(listOf("edited-reader.example"), preferences.getCustomProxies(ProxyPlatform.X))
        assertEquals("edited-reader.example", preferences.getSelectedProxyDomain(ProxyPlatform.X))
        assertEquals(
            BrowserFrontendPreference(
                BrowserConversionMode.CUSTOM,
                "custom:edited-reader.example",
            ),
            preferences.getBrowserFrontendPreferences()[ProxyPlatform.X],
        )
    }

    @Test
    fun `editing a dormant target keeps clean mode and re-enables the new custom target`() {
        preferences.disableBuiltIn(ProxyPlatform.X, "x_xcancel")
        val expected = preferences.getBrowserFrontendPreferences()
        assertTrue(
            preferences.saveBrowserFrontendPreferences(
                mapOf(
                    ProxyPlatform.X to BrowserFrontendPreference(
                        BrowserConversionMode.CLEAN_ONLY,
                        "x_xcancel",
                    )
                ),
                expected,
            )
        )

        val draft = BrowserConversionDefaultsHelper.createDraft(preferences)
        val target = AlternativeFrontendCatalog.builtIn(ProxyPlatform.X)
            .single { it.id == "x_xcancel" }
        assertTrue(draft.editTarget(ProxyPlatform.X, target, "dormant-reader.example"))
        assertEquals(
            BrowserFrontendPreference(
                BrowserConversionMode.CLEAN_ONLY,
                "custom:dormant-reader.example",
            ),
            draft.preference(ProxyPlatform.X),
        )
        assertTrue(draft.apply())

        val enableDraft = BrowserConversionDefaultsHelper.createDraft(preferences)
        enableDraft.setEnabled(ProxyPlatform.X, true)
        assertTrue(enableDraft.apply())
        assertEquals(
            BrowserFrontendPreference(
                BrowserConversionMode.CUSTOM,
                "custom:dormant-reader.example",
            ),
            preferences.getBrowserFrontendPreferences()[ProxyPlatform.X],
        )
    }

    @Test
    fun `deleting selected target reports replacement before changing shared selections`() {
        preferences.addCustomProxy(ProxyPlatform.X, "custom-reader.example")
        preferences.setSelectedProxyDomain(ProxyPlatform.X, "custom-reader.example")
        val draft = BrowserConversionDefaultsHelper.createDraft(preferences)
        val target = draft.activeTargets(ProxyPlatform.X).single { it.domain == "custom-reader.example" }

        val impact = draft.removalImpact(ProxyPlatform.X, target)
        assertEquals("fixupx.com", impact.mainShareReplacement)
        assertFalse(impact.mainShareUsesCleanOnly)
        assertFalse(impact.browserUsesCleanOnly)

        draft.deleteTarget(ProxyPlatform.X, target)
        assertEquals("fixupx.com", draft.selectedProxyDomains[ProxyPlatform.X])
        assertTrue(draft.apply())
        assertEquals("fixupx.com", preferences.getSelectedProxyDomain(ProxyPlatform.X))
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

    @Test
    fun `draft save preserves unrelated newer roster and browser choices`() {
        val draft = BrowserConversionDefaultsHelper.createDraft(preferences)
        assertTrue(draft.addCustomProxy(ProxyPlatform.X, "x-reader.example"))
        preferences.addCustomProxy(ProxyPlatform.INSTAGRAM, "ig-reader.example")
        val newer = BrowserFrontendPreference(BrowserConversionMode.CUSTOM, "custom:ig-reader.example")
        assertTrue(preferences.saveBrowserFrontendPreferences(
            mapOf(ProxyPlatform.INSTAGRAM to newer), preferences.getBrowserFrontendPreferences(),
        ))

        assertTrue(draft.apply())
        assertEquals(listOf("x-reader.example"), preferences.getCustomProxies(ProxyPlatform.X))
        assertEquals(listOf("ig-reader.example"), preferences.getCustomProxies(ProxyPlatform.INSTAGRAM))
        assertEquals(newer, preferences.getBrowserFrontendPreferences()[ProxyPlatform.INSTAGRAM])
    }

    @Test
    fun `new cross platform collision rejects draft without partial writes`() {
        val draft = BrowserConversionDefaultsHelper.createDraft(preferences)
        assertTrue(draft.addCustomProxy(ProxyPlatform.X, "reader.example"))
        preferences.addCustomProxy(ProxyPlatform.INSTAGRAM, "sub.reader.example")

        assertFalse(draft.apply())
        assertTrue(preferences.getCustomProxies(ProxyPlatform.X).isEmpty())
        assertEquals(listOf("sub.reader.example"), preferences.getCustomProxies(ProxyPlatform.INSTAGRAM))
    }

    @Test
    fun `editing selected frontend cannot overwrite a newer Main choice`() {
        preferences.setSelectedProxyDomain(ProxyPlatform.X, "xcancel.com")
        val draft = BrowserConversionDefaultsHelper.createDraft(preferences)
        val target = draft.activeTargets(ProxyPlatform.X).single { it.id == "x_xcancel" }
        assertTrue(draft.editTarget(ProxyPlatform.X, target, "replacement.example"))
        preferences.setSelectedProxyDomain(ProxyPlatform.X, "fixupx.com")

        assertFalse(draft.apply())
        assertEquals("fixupx.com", preferences.getSelectedProxyDomain(ProxyPlatform.X))
        assertTrue(preferences.getCustomProxies(ProxyPlatform.X).isEmpty())
        assertFalse("x_xcancel" in preferences.getDisabledBuiltIns(ProxyPlatform.X))
    }

    private companion object { const val PREFS_NAME = "FixupXerPrefs" }
}
