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
import com.fixupxer.utils.InstagramProxyStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for Instagram proxy persistence in [PreferencesManager].
 *
 * Active proxy set: toinstagram.com, adamlikes.men, instagram7.com
 * + any user-defined custom proxies.
 * Default = toinstagram.com. Legacy proxy (eeinstagram.com) and retired
 * kkinstagram.com silently migrate to default.
 */
@RunWith(AndroidJUnit4::class)
class InstagramProxyPreferenceTest {

    private lateinit var prefs: PreferencesManager

    private val customProxy = "myproxy.example.org"

    @Before
    fun setup() {
        clearProxyPrefs()
        InstagramProxyStore.reset()
        prefs = PreferencesManager(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun tearDown() {
        clearProxyPrefs()
        InstagramProxyStore.reset()
    }

    private fun clearProxyPrefs() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .remove("instagram_proxy_domain")
            .remove("custom_instagram_proxies")
            .commit()
    }

    @Test
    fun defaultProxyIsToinstagram() {
        // Default = Constants.INSTAGRAM_DEFAULT_PROXY = toinstagram.com
        assertEquals(Constants.TOINSTAGRAM_DOMAIN, prefs.getInstagramProxy())
        assertEquals(Constants.INSTAGRAM_DEFAULT_PROXY, prefs.getInstagramProxy())
    }

    @Test
    fun setToinstagramPersists() {
        prefs.setInstagramProxy(Constants.TOINSTAGRAM_DOMAIN)
        assertEquals(Constants.TOINSTAGRAM_DOMAIN, prefs.getInstagramProxy())
    }

    @Test
    fun setAdamlikesPersists() {
        prefs.setInstagramProxy(Constants.ADAMLIKES_DOMAIN)
        assertEquals(Constants.ADAMLIKES_DOMAIN, prefs.getInstagramProxy())
    }

    @Test
    fun setInstagram7Persists() {
        prefs.setInstagramProxy(Constants.INSTAGRAM7_DOMAIN)
        assertEquals(Constants.INSTAGRAM7_DOMAIN, prefs.getInstagramProxy())
    }

    @Test
    fun setRetiredKkinstagramIsIgnored() {
        prefs.setInstagramProxy(Constants.KKINSTAGRAM_DOMAIN)
        assertEquals(Constants.INSTAGRAM_DEFAULT_PROXY, prefs.getInstagramProxy())
    }

    @Test
    fun invalidStoredValueFallsBackToDefault() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString("instagram_proxy_domain", "malicious.example")
            .commit()

        assertEquals(Constants.TOINSTAGRAM_DOMAIN, prefs.getInstagramProxy())
    }

    @Test
    fun kkinstagramStoredValueMigratesToDefault() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString("instagram_proxy_domain", "kkinstagram.com")
            .commit()

        assertEquals(Constants.TOINSTAGRAM_DOMAIN, prefs.getInstagramProxy())
    }

    @Test
    fun legacyEeinstagramStoredValueMigratesToDefault() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString("instagram_proxy_domain", "eeinstagram.com")
            .commit()

        assertEquals(Constants.TOINSTAGRAM_DOMAIN, prefs.getInstagramProxy())
    }

    // -------------------------------------------------------------------
    // Custom proxies
    // -------------------------------------------------------------------

    @Test
    fun addCustomProxyPersistsAndSyncsStore() {
        prefs.addCustomInstagramProxy(customProxy)

        assertEquals(listOf(customProxy), prefs.getCustomInstagramProxies())
        assertTrue(InstagramProxyStore.activeProxies().contains(customProxy))
    }

    @Test
    fun addDuplicateCustomProxyIsNoOp() {
        prefs.addCustomInstagramProxy(customProxy)
        prefs.addCustomInstagramProxy(customProxy)

        assertEquals(listOf(customProxy), prefs.getCustomInstagramProxies())
    }

    @Test
    fun customProxyCanBeSelected() {
        prefs.addCustomInstagramProxy(customProxy)
        prefs.setInstagramProxy(customProxy)

        assertEquals(customProxy, prefs.getInstagramProxy())
    }

    @Test
    fun unknownDomainCannotBeSelected() {
        prefs.setInstagramProxy("not-registered.example.org")
        assertEquals(Constants.INSTAGRAM_DEFAULT_PROXY, prefs.getInstagramProxy())
    }

    @Test
    fun removeCustomProxyRemovesFromStore() {
        prefs.addCustomInstagramProxy(customProxy)
        prefs.removeCustomInstagramProxy(customProxy)

        assertTrue(prefs.getCustomInstagramProxies().isEmpty())
        assertTrue(!InstagramProxyStore.activeProxies().contains(customProxy))
    }

    @Test
    fun removingSelectedCustomProxyFallsBackToDefault() {
        prefs.addCustomInstagramProxy(customProxy)
        prefs.setInstagramProxy(customProxy)
        assertEquals(customProxy, prefs.getInstagramProxy())

        prefs.removeCustomInstagramProxy(customProxy)
        assertEquals(Constants.INSTAGRAM_DEFAULT_PROXY, prefs.getInstagramProxy())
    }

    @Test
    fun customProxiesSurvivePreferencesManagerRecreation() {
        prefs.addCustomInstagramProxy(customProxy)

        // Simulate app restart: fresh store, new PreferencesManager instance.
        InstagramProxyStore.reset()
        val fresh = PreferencesManager(ApplicationProvider.getApplicationContext<Context>())

        assertEquals(listOf(customProxy), fresh.getCustomInstagramProxies())
        assertTrue(InstagramProxyStore.activeProxies().contains(customProxy))
    }
}
