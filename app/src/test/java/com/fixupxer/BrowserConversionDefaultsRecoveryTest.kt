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
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.fixupxer.ui.helpers.BrowserConversionDefaultsHelper
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import com.fixupxer.utils.TikTokProxyStore
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun `restoring and selecting a reader preserves an enabled conversion draft`() {
        preferencesManager.setBrowserConvertTwitterEnabled(true)
        AlternativeFrontendCatalog.builtInReaders(ProxyPlatform.X).forEach { reader ->
            preferencesManager.disableBuiltIn(ProxyPlatform.X, reader.id)
        }

        val draft = BrowserConversionDefaultsHelper.createDraft(preferencesManager)
        val themedContext = ContextThemeWrapper(context, R.style.Theme_FixupXer)
        val container = LinearLayout(themedContext)
        val rows = BrowserConversionDefaultsHelper.populateContainer(
            context = themedContext,
            layoutInflater = LayoutInflater.from(themedContext),
            container = container,
            draft = draft,
            onChangePrivacyTarget = {},
        )

        draft.restoreBuiltInReaders(ProxyPlatform.X)
        val restoredReader = AlternativeFrontendCatalog.builtInReaders(ProxyPlatform.X).first()
        draft.updateDraftTarget(ProxyPlatform.X, restoredReader)
        BrowserConversionDefaultsHelper.refreshRows(
            context = themedContext,
            rows = rows,
            draft = draft,
            onChangePrivacyTarget = {},
        )

        val xRow = rows.first { it.entry.platform == ProxyPlatform.X }
        assertTrue(xRow.binding.switchBrowserPrivacyPlatform.isEnabled)
        assertTrue(xRow.binding.switchBrowserPrivacyPlatform.isChecked)

        draft.apply(preferencesManager)

        assertTrue(preferencesManager.isBrowserConvertTwitterEnabled())
        assertEquals(
            restoredReader.id,
            preferencesManager.getBrowserPrivacyTargetId(ProxyPlatform.X),
        )
    }

    @Test
    fun `reader-only restore keeps disabled embed built-ins disabled after save`() {
        preferencesManager.setBrowserConvertTwitterEnabled(true)
        preferencesManager.disableBuiltIn(ProxyPlatform.X, EMBED_X_ID)
        AlternativeFrontendCatalog.builtInReaders(ProxyPlatform.X).forEach { reader ->
            preferencesManager.disableBuiltIn(ProxyPlatform.X, reader.id)
        }

        val draft = BrowserConversionDefaultsHelper.createDraft(preferencesManager)
        val initialDisabled = preferencesManager.getDisabledBuiltIns(ProxyPlatform.X)
        draft.restoreBuiltInReaders(ProxyPlatform.X)

        assertEquals(initialDisabled, preferencesManager.getDisabledBuiltIns(ProxyPlatform.X))
        assertEquals(initialDisabled, ProxyRoster.getDisabledBuiltIns(ProxyPlatform.X))
        assertEquals(setOf(EMBED_X_ID), draft.disabledBuiltIns(ProxyPlatform.X))

        val restoredReader = AlternativeFrontendCatalog.builtInReaders(ProxyPlatform.X).first()
        draft.updateDraftTarget(ProxyPlatform.X, restoredReader)
        draft.apply(preferencesManager)

        assertEquals(setOf(EMBED_X_ID), preferencesManager.getDisabledBuiltIns(ProxyPlatform.X))
        assertTrue(preferencesManager.isBrowserConvertTwitterEnabled())
    }

    @Test
    fun `cancelled draft restore never mutates preferences or proxy roster`() {
        preferencesManager.setBrowserConvertTwitterEnabled(true)
        preferencesManager.disableBuiltIn(ProxyPlatform.X, EMBED_X_ID)
        AlternativeFrontendCatalog.builtInReaders(ProxyPlatform.X).forEach { reader ->
            preferencesManager.disableBuiltIn(ProxyPlatform.X, reader.id)
        }
        val initialDisabled = preferencesManager.getDisabledBuiltIns(ProxyPlatform.X)

        val draft = BrowserConversionDefaultsHelper.createDraft(preferencesManager)
        draft.restoreBuiltInReaders(ProxyPlatform.X)

        assertEquals(initialDisabled, preferencesManager.getDisabledBuiltIns(ProxyPlatform.X))
        assertEquals(initialDisabled, ProxyRoster.getDisabledBuiltIns(ProxyPlatform.X))
        assertEquals(setOf(EMBED_X_ID), draft.disabledBuiltIns(ProxyPlatform.X))
    }

    @Test
    fun `new draft copies roster without mutating it`() {
        preferencesManager.disableBuiltIn(ProxyPlatform.X, EMBED_X_ID)
        val initialDisabled = preferencesManager.getDisabledBuiltIns(ProxyPlatform.X)

        val draft = BrowserConversionDefaultsHelper.createDraft(preferencesManager)

        assertEquals(initialDisabled, preferencesManager.getDisabledBuiltIns(ProxyPlatform.X))
        assertEquals(initialDisabled, ProxyRoster.getDisabledBuiltIns(ProxyPlatform.X))
        assertEquals(initialDisabled, draft.disabledBuiltIns(ProxyPlatform.X))
    }

    private companion object {
        const val PREFS_NAME = "FixupXerPrefs"

        /** Built-in X embed target (fixupx.com) — must never be resurrected by reader restore. */
        const val EMBED_X_ID = "x_fixupx"
    }
}
