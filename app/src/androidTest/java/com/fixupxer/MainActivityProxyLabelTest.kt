// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2025  NeatCode Labs
 */

package com.fixupxer

import android.content.Context
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.utils.Constants
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for the "Active: <proxy>. Change." row in MainActivity.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityProxyLabelTest {

    @Before
    fun setup() {
        resetRelevantPrefs()
    }

    @After
    fun tearDown() {
        resetRelevantPrefs()
    }

    /**
     * Reset Instagram-related prefs that other instrumentation tests in the same
     * app process may have flipped (`convert_instagram` defaults to true in
     * production but other tests toggle it off). Without this, the auto-reprocess
     * test fails with "Nothing to do!" because conversion was disabled by a
     * previous test run.
     */
    private fun resetRelevantPrefs() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .remove("instagram_proxy_domain")
            .putBoolean("convert_instagram", true)
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
    fun instagramUrlShowsActiveToinstagramByDefault() {
        // v1.4.8: default = toinstagram.com
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.editTextUrl))
                .perform(replaceText("https://instagram.com/p/abc"), closeSoftKeyboard())
            onView(isRoot()).perform(waitFor(1500))

            onView(withId(R.id.instagramProxyRow))
                .check(matches(isDisplayed()))
            onView(withId(R.id.textViewInstagramProxyStatus))
                .check(matches(withText("Active: ${Constants.TOINSTAGRAM_DOMAIN}.")))
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
                .check(matches(withText("Active: ${Constants.INSTAGRAM7_DOMAIN}.")))
        }
    }

    @Test
    fun changingPrefToAdamlikesUpdatesLabelOnRelaunch() {
        // toinstagram is the default; this test exercises switching to the other primary proxy
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        PreferencesManager(ctx).setInstagramProxy(Constants.ADAMLIKES_DOMAIN)

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.editTextUrl))
                .perform(replaceText("https://instagram.com/p/abc"), closeSoftKeyboard())
            onView(isRoot()).perform(waitFor(1500))

            onView(withId(R.id.textViewInstagramProxyStatus))
                .check(matches(withText("Active: ${Constants.ADAMLIKES_DOMAIN}.")))
        }
    }

    @Test
    fun facebookUrlDoesNotShowProxyRow() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.editTextUrl))
                .perform(replaceText("https://facebook.com/user/posts/1"), closeSoftKeyboard())
            onView(isRoot()).perform(waitFor(1500))

            onView(withId(R.id.instagramToggleContainer))
                .check(matches(isDisplayed()))
            onView(withId(R.id.instagramProxyRow))
                .check(matches(allOf(not(isDisplayed()))))
        }
    }

    /**
     * v1.5.1 regression test: clicking "Change." in MainActivity must show the
     * proxy chooser dialog inline (parity with ShareActivity), NOT launch
     * SettingsActivity. Settings no longer hosts the proxy chooser.
     *
     * Dialog labels include a Primary/Backup badge appended to the domain
     * (e.g. "instagram7.com  · Backup"), so we match on a substring.
     */
    @Test
    fun changeProxyShowsDialogAndUpdatesLabelInPlace() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.editTextUrl))
                .perform(replaceText("https://instagram.com/p/abc"), closeSoftKeyboard())
            onView(isRoot()).perform(waitFor(1500))

            // Default proxy
            onView(withId(R.id.textViewInstagramProxyStatus))
                .check(matches(withText("Active: ${Constants.TOINSTAGRAM_DOMAIN}.")))

            // Processed URL field is empty (user hasn't pressed Process yet)
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("")))

            // Click Change. -> dialog appears
            onView(withId(R.id.textViewChangeProxy)).perform(click())
            onView(isRoot()).perform(waitFor(500))

            // Pick instagram7.com from the dialog
            onView(withText(containsString(Constants.INSTAGRAM7_DOMAIN)))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            // MainActivity is still in the foreground; label reflects the new proxy
            onView(withId(R.id.textViewInstagramProxyStatus))
                .check(matches(withText("Active: ${Constants.INSTAGRAM7_DOMAIN}.")))
            onView(withId(R.id.editTextUrl))
                .check(matches(isDisplayed()))

            // No auto-reprocess: Processed URL field stays empty until the user
            // taps Process (preserves the explicit Process-button flow for fresh inputs).
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("")))
        }
    }

    /**
     * v1.5.1 paired regression test: when a Processed URL already exists for an
     * Instagram input (i.e. the user has tapped Process at least once), changing
     * the proxy must auto-refresh the Processed URL field with the new proxy
     * domain — full parity with ShareActivity. Fresh inputs that were never
     * processed are intentionally excluded (covered by the test above).
     */
    @Test
    fun processedInstagramUrlReprocessesAfterProxyChange() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.editTextUrl))
                .perform(replaceText("https://instagram.com/p/abc"), closeSoftKeyboard())
            onView(isRoot()).perform(waitFor(1500))

            // Press Process to populate the Processed URL field with the default proxy
            onView(withId(R.id.buttonProcess)).perform(click())
            onView(isRoot()).perform(waitFor(1500))

            // Sanity: the processed text uses toinstagram.com (default)
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText(containsString(Constants.TOINSTAGRAM_DOMAIN))))

            // Open the proxy dialog and pick instagram7.com
            onView(withId(R.id.textViewChangeProxy)).perform(click())
            onView(isRoot()).perform(waitFor(500))
            onView(withText(containsString(Constants.INSTAGRAM7_DOMAIN)))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(1500))

            // Label updates AND the Processed URL field is automatically refreshed
            // with the newly selected proxy — no extra Process tap required.
            onView(withId(R.id.textViewInstagramProxyStatus))
                .check(matches(withText("Active: ${Constants.INSTAGRAM7_DOMAIN}.")))
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText(containsString(Constants.INSTAGRAM7_DOMAIN))))
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(not(withText(containsString(Constants.TOINSTAGRAM_DOMAIN)))))
        }
    }
}
