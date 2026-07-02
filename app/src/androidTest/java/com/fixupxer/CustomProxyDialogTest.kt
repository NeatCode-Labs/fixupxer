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
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.utils.Constants
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for the custom Instagram proxy add/delete flow in the
 * proxy chooser dialog (v1.6.0), exercised through MainActivity.
 */
@RunWith(AndroidJUnit4::class)
class CustomProxyDialogTest {

    private val customProxy = "myproxy.example.org"

    @Before
    fun setup() {
        resetRelevantPrefs()
    }

    @After
    fun tearDown() {
        resetRelevantPrefs()
    }

    private fun resetRelevantPrefs() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .remove("instagram_proxy_domain")
            .remove("custom_instagram_proxies")
            .putBoolean("convert_instagram", true)
            .commit()
        // Keep the process-wide store in sync with the wiped prefs.
        com.fixupxer.utils.InstagramProxyStore.reset()
    }

    private fun waitFor(millis: Long): ViewAction = object : ViewAction {
        override fun getConstraints() = isRoot()
        override fun getDescription() = "Wait for $millis ms"
        override fun perform(uiController: UiController, view: View?) {
            uiController.loopMainThreadForAtLeast(millis)
        }
    }

    private fun openProxyDialog() {
        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://instagram.com/p/abc"), closeSoftKeyboard())
        onView(isRoot()).perform(waitFor(1500))
        onView(withId(R.id.textViewChangeProxy)).perform(click())
        onView(isRoot()).perform(waitFor(500))
    }

    @Test
    fun dialogListsAllFixedProxiesAndAddRow() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openProxyDialog()

            for (domain in Constants.INSTAGRAM_PROXY_DOMAINS) {
                onView(withText(containsString(domain)))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
            }
            // kkinstagram.com must be present again as an active backup proxy
            onView(withText(containsString(Constants.KKINSTAGRAM_DOMAIN)))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withText("Add custom proxy…"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun selectingKkinstagramUpdatesLabel() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openProxyDialog()

            onView(withText(containsString(Constants.KKINSTAGRAM_DOMAIN)))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(withId(R.id.textViewInstagramProxyStatus))
                .check(matches(withText("Active: ${Constants.KKINSTAGRAM_DOMAIN}.")))
        }
    }

    @Test
    fun invalidCustomProxyShowsInlineErrorAndKeepsDialogOpen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openProxyDialog()

            onView(withText("Add custom proxy…"))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(withId(R.id.customProxyInput))
                .inRoot(isDialog())
                .perform(replaceText("not a domain"), closeSoftKeyboard())
            onView(withText("Add"))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            // Input dialog stays open and shows the inline error
            onView(withId(R.id.customProxyInput))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withText("Enter a valid domain, e.g. myproxy.com"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun reservedDomainIsRejected() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openProxyDialog()

            onView(withText("Add custom proxy…"))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(withId(R.id.customProxyInput))
                .inRoot(isDialog())
                .perform(replaceText(Constants.FIXUPX_DOMAIN), closeSoftKeyboard())
            onView(withText("Add"))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(withText("This domain is already known to the app and can't be used as a custom proxy"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun addSelectAndDeleteCustomProxyFlow() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openProxyDialog()

            // --- Add ---
            onView(withText("Add custom proxy…"))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            // Input accepts a full URL and normalizes it to the bare domain
            onView(withId(R.id.customProxyInput))
                .inRoot(isDialog())
                .perform(replaceText("https://www.$customProxy/some/path"), closeSoftKeyboard())
            onView(withText("Add"))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            // New row is listed in the chooser
            onView(withText(containsString(customProxy)))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))

            // --- Select ---
            onView(withText(containsString(customProxy)))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(withId(R.id.textViewInstagramProxyStatus))
                .check(matches(withText("Active: $customProxy.")))

            // --- Delete ---
            onView(withId(R.id.textViewChangeProxy)).perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(
                allOf(
                    withId(R.id.proxyDeleteButton),
                    hasSibling(withText(containsString(customProxy)))
                )
            )
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            // Row is gone from the chooser
            onView(withText(containsString(customProxy)))
                .inRoot(isDialog())
                .check(doesNotExist())

            // Close the dialog; selection fell back to the default proxy
            onView(withText("Cancel"))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(withId(R.id.textViewInstagramProxyStatus))
                .check(matches(withText("Active: ${Constants.INSTAGRAM_DEFAULT_PROXY}.")))
        }
    }
}
