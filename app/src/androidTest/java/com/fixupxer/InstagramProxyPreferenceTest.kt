// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2025  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
 * Uses the real [SharedPreferences] (on-device) to verify that the default proxy
 * is kkinstagram.com and that set/get round-trips correctly for all supported proxies.
 */
@RunWith(AndroidJUnit4::class)
class InstagramProxyPreferenceTest {

    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        // Clear the relevant SharedPreferences entry so each test starts clean.
        ctx.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .remove("instagram_proxy_domain")
            .commit()
        prefs = PreferencesManager(ctx)
    }

    @After
    fun tearDown() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .remove("instagram_proxy_domain")
            .commit()
    }

    @Test
    fun defaultProxyIsKkinstagram() {
        assertEquals(Constants.KKINSTAGRAM_DOMAIN, prefs.getInstagramProxy())
    }

    @Test
    fun setKkinstagramPersists() {
        prefs.setInstagramProxy(Constants.KKINSTAGRAM_DOMAIN)
        assertEquals(Constants.KKINSTAGRAM_DOMAIN, prefs.getInstagramProxy())
    }

    @Test
    fun setEeinstagramPersists() {
        prefs.setInstagramProxy(Constants.EEINSTAGRAM_DOMAIN)
        assertEquals(Constants.EEINSTAGRAM_DOMAIN, prefs.getInstagramProxy())
    }

    @Test
    fun setInstagram7Persists() {
        prefs.setInstagramProxy(Constants.INSTAGRAM7_DOMAIN)
        assertEquals(Constants.INSTAGRAM7_DOMAIN, prefs.getInstagramProxy())
    }

    @Test
    fun invalidStoredValueFallsBackToDefault() {
        // Write an unknown value directly and verify the getter guards against it.
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString("instagram_proxy_domain", "malicious.example")
            .commit()

        assertEquals(Constants.KKINSTAGRAM_DOMAIN, prefs.getInstagramProxy())
    }
}
