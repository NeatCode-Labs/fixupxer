// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2025  NeatCode Labs
 */

package com.fixupxer

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.ui.ShareActivity
import com.fixupxer.utils.Constants
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for the "Active: <proxy>. Change." row in ShareActivity.
 */
@RunWith(AndroidJUnit4::class)
class ShareActivityProxyLabelTest {

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

    private fun shareIntent(url: String): Intent {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        return Intent(ctx, ShareActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
    }

    private fun waitFor(millis: Long): ViewAction = object : ViewAction {
        override fun getConstraints() = isRoot()
        override fun getDescription() = "Wait for $millis ms"
        override fun perform(uiController: UiController, view: View?) {
            uiController.loopMainThreadForAtLeast(millis)
        }
    }

    @Test
    fun instagramShareShowsActiveToinstagramByDefault() {
        // v1.4.8: default = toinstagram.com
        ActivityScenario.launch<ShareActivity>(shareIntent("https://instagram.com/p/abc")).use {
            onView(isRoot()).perform(waitFor(1500))

            onView(withId(R.id.instagramProxyRow))
                .check(matches(isDisplayed()))
            onView(withId(R.id.textViewInstagramProxyStatus))
                .check(matches(withText("Active: ${Constants.TOINSTAGRAM_DOMAIN}.")))
        }
    }

    @Test
    fun shareUsesAdamlikesWhenPrefSet() {
        // toinstagram is the default; this test exercises switching to the other primary proxy
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        PreferencesManager(ctx).setInstagramProxy(Constants.ADAMLIKES_DOMAIN)

        ActivityScenario.launch<ShareActivity>(shareIntent("https://instagram.com/p/abc")).use {
            onView(isRoot()).perform(waitFor(1500))

            onView(withId(R.id.textViewInstagramProxyStatus))
                .check(matches(withText("Active: ${Constants.ADAMLIKES_DOMAIN}.")))
        }
    }

    /**
     * Regression test: clicking "Change." in the Share screen must show the
     * proxy dialog inline (not launch SettingsActivity), because ShareActivity
     * is android:noHistory="true" and would be destroyed on navigation away.
     * After picking a new proxy, the Share screen must stay on top, the label
     * must refresh, and the processed URL must be re-computed with the new proxy.
     *
     * Dialog labels include a Primary/Backup badge appended to the domain
     * (e.g. "instagram7.com  · Backup"), so we match on a substring.
     */
    @Test
    fun changeProxyInShowsDialogAndUpdatesLabelInPlace() {
        ActivityScenario.launch<ShareActivity>(shareIntent("https://instagram.com/p/abc")).use {
            onView(isRoot()).perform(waitFor(1500))

            // Default proxy should be toinstagram.com (v1.4.8)
            onView(withId(R.id.textViewInstagramProxyStatus))
                .check(matches(withText("Active: ${Constants.TOINSTAGRAM_DOMAIN}.")))

            // Click the "Change." link -> dialog must appear (in-activity, not Settings)
            onView(withId(R.id.textViewChangeProxy)).perform(click())
            onView(isRoot()).perform(waitFor(500))

            // Select the instagram7.com row from the dialog (label has a "Backup" badge)
            onView(withText(containsString(Constants.INSTAGRAM7_DOMAIN)))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            // Share screen must still be alive and the label must reflect the new proxy
            onView(withId(R.id.textViewInstagramProxyStatus))
                .check(matches(withText("Active: ${Constants.INSTAGRAM7_DOMAIN}.")))
            onView(withId(R.id.instagramToggleContainer))
                .check(matches(isDisplayed()))
        }
    }
}
