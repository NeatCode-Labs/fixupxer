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
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import com.fixupxer.utils.TikTokProxyStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BrowserPrivacyPreferenceTest {

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
    fun `setBrowserPrivacyTargetId rejects embed wrong platform missing and custom ids`() {
        preferencesManager.setBrowserPrivacyTargetId(ProxyPlatform.X, "x_fixupx")
        assertNull(preferencesManager.getBrowserPrivacyTargetId(ProxyPlatform.X))

        preferencesManager.setBrowserPrivacyTargetId(ProxyPlatform.X, "bs_skylib_coffee")
        assertNull(preferencesManager.getBrowserPrivacyTargetId(ProxyPlatform.X))

        preferencesManager.setBrowserPrivacyTargetId(ProxyPlatform.X, "does_not_exist")
        assertNull(preferencesManager.getBrowserPrivacyTargetId(ProxyPlatform.X))

        preferencesManager.addCustomProxy(ProxyPlatform.X, "myreader.example")
        preferencesManager.setBrowserPrivacyTargetId(ProxyPlatform.X, "myreader.example")
        assertNull(preferencesManager.getBrowserPrivacyTargetId(ProxyPlatform.X))
    }

    @Test
    fun `setBrowserPrivacyTargetId accepts built-in reader id`() {
        preferencesManager.setBrowserPrivacyTargetId(ProxyPlatform.X, "x_xcancel")
        assertEquals("x_xcancel", preferencesManager.getBrowserPrivacyTargetId(ProxyPlatform.X))
        assertEquals(
            Constants.XCANCEL_DOMAIN,
            preferencesManager.resolveBrowserPrivacyTarget(ProxyPlatform.X)?.domain,
        )
    }

    @Test
    fun `resolveBrowserPrivacyTarget falls back when stored reader is disabled`() {
        preferencesManager.setBrowserPrivacyTargetId(ProxyPlatform.X, "x_xcancel")
        preferencesManager.disableBuiltIn(ProxyPlatform.X, "x_xcancel")

        val resolved = preferencesManager.resolveBrowserPrivacyTarget(ProxyPlatform.X)
        assertEquals("x_nitter_net", resolved?.id)
        assertEquals(Constants.NITTER_NET_DOMAIN, resolved?.domain)
    }

    @Test
    fun `resolveBrowserPrivacyTarget is null when all readers are disabled`() {
        AlternativeFrontendCatalog.builtInReaders(ProxyPlatform.X).forEach { reader ->
            preferencesManager.disableBuiltIn(ProxyPlatform.X, reader.id)
        }
        assertNull(preferencesManager.resolveBrowserPrivacyTarget(ProxyPlatform.X))
    }

    @Test
    fun `browser privacy selection is independent from main proxy selection`() {
        preferencesManager.setSelectedProxyDomain(ProxyPlatform.X, Constants.FIXUPX_DOMAIN)
        preferencesManager.setBrowserPrivacyTargetId(ProxyPlatform.X, "x_xcancel")

        assertEquals(Constants.FIXUPX_DOMAIN, preferencesManager.getSelectedProxyDomain(ProxyPlatform.X))
        assertEquals(
            Constants.XCANCEL_DOMAIN,
            preferencesManager.resolveBrowserPrivacyTarget(ProxyPlatform.X)?.domain,
        )
    }

    @Test
    fun `resolveBrowserPrivacySelections returns null for platforms without readers`() {
        val selections = preferencesManager.resolveBrowserPrivacySelections()
        assertNull(selections[ProxyPlatform.FACEBOOK])
        assertNull(selections[ProxyPlatform.TIKTOK])
        assertEquals(Constants.XCANCEL_DOMAIN, selections[ProxyPlatform.X])
    }

    private companion object {
        const val PREFS_NAME = "FixupXerPrefs"
    }
}
