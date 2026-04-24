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
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.utils.Constants
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for the "Currently using: <proxy>. Change." row in MainActivity.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityProxyLabelTest {

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

    private fun waitFor(millis: Long): ViewAction = object : ViewAction {
        override fun getConstraints() = isRoot()
        override fun getDescription() = "Wait for $millis ms"
        override fun perform(uiController: UiController, view: View?) {
            uiController.loopMainThreadForAtLeast(millis)
        }
    }

    @Test
    fun instagramUrlShowsCurrentlyUsingKkinstagramByDefault() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.editTextUrl))
                .perform(replaceText("https://instagram.com/p/abc"), closeSoftKeyboard())
            onView(isRoot()).perform(waitFor(1500))

            onView(withId(R.id.instagramProxyRow))
                .check(matches(isDisplayed()))
            onView(withId(R.id.textViewInstagramProxyStatus))
                .check(matches(withText("Active: kkinstagram.com.")))
            onView(withId(R.id.textViewChangeProxy))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun changingPrefToInstagram7UpdatesLabelOnRelaunch() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        PreferencesManager(ctx).setInstagramProxy(Constants.INSTAGRAM7_DOMAIN)

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.editTextUrl))
                .perform(replaceText("https://instagram.com/p/abc"), closeSoftKeyboard())
            onView(isRoot()).perform(waitFor(1500))

            onView(withId(R.id.textViewInstagramProxyStatus))
                .check(matches(withText("Active: instagram7.com.")))
        }
    }

    @Test
    fun facebookUrlDoesNotShowProxyRow() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.editTextUrl))
                .perform(replaceText("https://facebook.com/user/posts/1"), closeSoftKeyboard())
            onView(isRoot()).perform(waitFor(1500))

            // Toggle container is visible for Facebook but the proxy row is not.
            onView(withId(R.id.instagramToggleContainer))
                .check(matches(isDisplayed()))
            onView(withId(R.id.instagramProxyRow))
                .check(matches(allOf(not(isDisplayed()))))
        }
    }
}
