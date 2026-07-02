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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixupxer.utils.Constants
import com.fixupxer.utils.TikTokProxyStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for TikTok proxy persistence in [PreferencesManager].
 *
 * Active proxy set (v1.7.0): tnktok.com, tfxktok.com, tiktokez.com, kktiktok.com
 * + any user-defined custom proxies. Default = tnktok.com.
 * Legacy proxies (vxtiktok.com, tiktxk.com) silently migrate to the default.
 */
@RunWith(AndroidJUnit4::class)
class TikTokProxyPreferenceTest {

    private lateinit var prefs: PreferencesManager

    private val customProxy = "myttproxy.example.org"

    @Before
    fun setup() {
        clearProxyPrefs()
        TikTokProxyStore.reset()
        prefs = PreferencesManager(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun tearDown() {
        clearProxyPrefs()
        TikTokProxyStore.reset()
    }

    private fun clearProxyPrefs() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .remove("tiktok_proxy_domain")
            .remove("custom_tiktok_proxies")
            .commit()
    }

    @Test
    fun defaultProxyIsTnktok() {
        assertEquals(Constants.TNKTOK_DOMAIN, prefs.getTikTokProxy())
        assertEquals(Constants.TIKTOK_DEFAULT_PROXY, prefs.getTikTokProxy())
    }

    @Test
    fun setTnktokPersists() {
        prefs.setTikTokProxy(Constants.TNKTOK_DOMAIN)
        assertEquals(Constants.TNKTOK_DOMAIN, prefs.getTikTokProxy())
    }

    @Test
    fun setTfxktokPersists() {
        prefs.setTikTokProxy(Constants.TFXKTOK_DOMAIN)
        assertEquals(Constants.TFXKTOK_DOMAIN, prefs.getTikTokProxy())
    }

    @Test
    fun setTiktokezPersists() {
        prefs.setTikTokProxy(Constants.TIKTOKEZ_DOMAIN)
        assertEquals(Constants.TIKTOKEZ_DOMAIN, prefs.getTikTokProxy())
    }

    @Test
    fun setKktiktokPersists() {
        prefs.setTikTokProxy(Constants.KKTIKTOK_DOMAIN)
        assertEquals(Constants.KKTIKTOK_DOMAIN, prefs.getTikTokProxy())
    }

    @Test
    fun invalidStoredValueFallsBackToDefault() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString("tiktok_proxy_domain", "malicious.example")
            .commit()

        assertEquals(Constants.TNKTOK_DOMAIN, prefs.getTikTokProxy())
    }

    @Test
    fun legacyVxtiktokStoredValueMigratesToDefault() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString("tiktok_proxy_domain", "vxtiktok.com")
            .commit()

        assertEquals(Constants.TNKTOK_DOMAIN, prefs.getTikTokProxy())
    }

    // -------------------------------------------------------------------
    // Custom proxies
    // -------------------------------------------------------------------

    @Test
    fun addCustomProxyPersistsAndSyncsStore() {
        prefs.addCustomTikTokProxy(customProxy)

        assertEquals(listOf(customProxy), prefs.getCustomTikTokProxies())
        assertTrue(TikTokProxyStore.activeProxies().contains(customProxy))
    }

    @Test
    fun addDuplicateCustomProxyIsNoOp() {
        prefs.addCustomTikTokProxy(customProxy)
        prefs.addCustomTikTokProxy(customProxy)

        assertEquals(listOf(customProxy), prefs.getCustomTikTokProxies())
    }

    @Test
    fun customProxyCanBeSelected() {
        prefs.addCustomTikTokProxy(customProxy)
        prefs.setTikTokProxy(customProxy)

        assertEquals(customProxy, prefs.getTikTokProxy())
    }

    @Test
    fun unknownDomainCannotBeSelected() {
        prefs.setTikTokProxy("not-registered.example.org")
        assertEquals(Constants.TIKTOK_DEFAULT_PROXY, prefs.getTikTokProxy())
    }

    @Test
    fun removeCustomProxyRemovesFromStore() {
        prefs.addCustomTikTokProxy(customProxy)
        prefs.removeCustomTikTokProxy(customProxy)

        assertTrue(prefs.getCustomTikTokProxies().isEmpty())
        assertTrue(!TikTokProxyStore.activeProxies().contains(customProxy))
    }

    @Test
    fun removingSelectedCustomProxyFallsBackToDefault() {
        prefs.addCustomTikTokProxy(customProxy)
        prefs.setTikTokProxy(customProxy)
        assertEquals(customProxy, prefs.getTikTokProxy())

        prefs.removeCustomTikTokProxy(customProxy)
        assertEquals(Constants.TIKTOK_DEFAULT_PROXY, prefs.getTikTokProxy())
    }

    @Test
    fun customProxiesSurvivePreferencesManagerRecreation() {
        prefs.addCustomTikTokProxy(customProxy)

        // Simulate app restart: fresh store, new PreferencesManager instance.
        TikTokProxyStore.reset()
        val fresh = PreferencesManager(ApplicationProvider.getApplicationContext<Context>())

        assertEquals(listOf(customProxy), fresh.getCustomTikTokProxies())
        assertTrue(TikTokProxyStore.activeProxies().contains(customProxy))
    }

    // -------------------------------------------------------------------
    // Independence from the Instagram roster
    // -------------------------------------------------------------------

    @Test
    fun tiktokAndInstagramCustomRostersAreIndependent() {
        prefs.addCustomTikTokProxy(customProxy)

        assertTrue(prefs.getCustomInstagramProxies().isEmpty())
        assertTrue(!com.fixupxer.utils.InstagramProxyStore.activeProxies().contains(customProxy))
    }
}
