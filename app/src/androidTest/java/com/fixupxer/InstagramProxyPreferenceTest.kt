// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2025  NeatCode Labs
 */

package com.fixupxer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixupxer.utils.Constants
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for Instagram proxy persistence in [PreferencesManager].
 *
 * Active proxy set (v1.4.8): toinstagram.com, adamlikes.men, instagram7.com.
 * Default = toinstagram.com. Legacy proxies (kkinstagram, eeinstagram) silently migrate.
 */
@RunWith(AndroidJUnit4::class)
class InstagramProxyPreferenceTest {

    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        clearProxyPref()
        prefs = PreferencesManager(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun tearDown() {
        clearProxyPref()
    }

    private fun clearProxyPref() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .remove("instagram_proxy_domain")
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
    fun invalidStoredValueFallsBackToDefault() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString("instagram_proxy_domain", "malicious.example")
            .commit()

        assertEquals(Constants.TOINSTAGRAM_DOMAIN, prefs.getInstagramProxy())
    }

    @Test
    fun legacyKkinstagramStoredValueMigratesToDefault() {
        // A user upgrading from v1.4.7 may have kkinstagram.com saved.
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
}
