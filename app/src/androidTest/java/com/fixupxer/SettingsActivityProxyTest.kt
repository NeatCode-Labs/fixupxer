// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2025  NeatCode Labs
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
 * Instrumentation tests for the Instagram embed proxy radio group in [SettingsActivity]
 * (v1.4.8 proxy set: toinstagram.com + adamlikes.men [primary], instagram7.com [backup]; default = toinstagram.com).
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
    fun defaultRadioIsToinstagram() {
        // Default = Constants.INSTAGRAM_DEFAULT_PROXY = toinstagram.com
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.radioProxyTo))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
                .check(matches(isChecked()))
        }
    }

    @Test
    fun clickAdamlikesPersistsChoice() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.radioProxyAdamlikes)).perform(scrollTo(), click())
        }

        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = PreferencesManager(ctx)
        assertEquals(Constants.ADAMLIKES_DOMAIN, prefs.getInstagramProxy())
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

    @Test
    fun infoIconIsDisplayed() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.instagramProxyInfoIcon))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
        }
    }
}
