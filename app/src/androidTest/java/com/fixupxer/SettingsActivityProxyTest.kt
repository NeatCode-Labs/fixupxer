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
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.ui.SettingsActivity
import com.fixupxer.utils.Constants
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for the Instagram embed proxy radio group in [SettingsActivity].
 */
@RunWith(AndroidJUnit4::class)
class SettingsActivityProxyTest {

    @Before
    fun setup() {
        clearProxyPref()
    }

    @After
    fun tearDown() {
        clearProxyPref()
    }

    private fun clearProxyPref() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .remove("instagram_proxy_domain")
            .commit()
    }

    @Test
    fun defaultRadioIsKkinstagram() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.radioProxyKk))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
                .check(matches(isChecked()))
        }
    }

    @Test
    fun clickEeinstagramPersistsChoice() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.radioProxyEe)).perform(scrollTo(), click())
        }

        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = PreferencesManager(ctx)
        assertEquals(Constants.EEINSTAGRAM_DOMAIN, prefs.getInstagramProxy())
    }

    @Test
    fun clickInstagram7PersistsChoice() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.radioProxy7)).perform(scrollTo(), click())
        }

        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = PreferencesManager(ctx)
        assertEquals(Constants.INSTAGRAM7_DOMAIN, prefs.getInstagramProxy())
    }

    @Test
    fun previouslySelectedProxyIsRestoredOnRelaunch() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        PreferencesManager(ctx).setInstagramProxy(Constants.INSTAGRAM7_DOMAIN)

        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.radioProxy7))
                .perform(scrollTo())
                .check(matches(isChecked()))
        }
    }
}
