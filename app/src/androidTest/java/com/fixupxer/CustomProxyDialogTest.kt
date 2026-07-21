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
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyRoster
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for the custom proxy add/delete flow in the shared
 * proxy picker bottom sheet, exercised through MainActivity.
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
            .remove("disabled_builtin_proxies_instagram")
            .putBoolean("convert_instagram", true)
            .commit()
        com.fixupxer.utils.InstagramProxyStore.reset()
        ProxyRoster.reset()
    }

    private fun waitFor(millis: Long): ViewAction = object : ViewAction {
        override fun getConstraints() = isRoot()
        override fun getDescription() = "Wait for $millis ms"
        override fun perform(uiController: UiController, view: View?) {
            uiController.loopMainThreadForAtLeast(millis)
        }
    }

    private fun openProxyPicker() {
        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://instagram.com/p/abc"), closeSoftKeyboard())
        onView(isRoot()).perform(waitFor(1500))
        onView(withId(R.id.textViewChangeProxy)).perform(click())
        onView(isRoot()).perform(waitFor(500))
    }

    /** Scroll the picker list until the row whose item view matches is laid out. */
    private fun scrollPickerTo(itemMatcher: org.hamcrest.Matcher<View>) {
        onView(withId(R.id.recyclerViewProxyPicker))
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(itemMatcher))
        onView(isRoot()).perform(waitFor(300))
    }

    private fun openAddCustomDialog() {
        // The hidden empty-state button shares the same text; require a
        // displayed match and scroll the action row into view first.
        scrollPickerTo(withText(R.string.proxy_action_add_custom))
        onView(allOf(withText(R.string.proxy_action_add_custom), isDisplayed()))
            .perform(click())
        onView(isRoot()).perform(waitFor(500))
    }

    @Test
    fun dialogListsFixedProxiesAndAddAction() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openProxyPicker()

            for (domain in Constants.INSTAGRAM_PROXY_DOMAINS) {
                scrollPickerTo(hasDescendant(withText(containsString(domain))))
                onView(allOf(withText(containsString(domain)), isDisplayed()))
                    .check(matches(isDisplayed()))
            }
            scrollPickerTo(withText(R.string.proxy_action_add_custom))
            onView(allOf(withText(R.string.proxy_action_add_custom), isDisplayed()))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun selectingInstagram7UpdatesLabel() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openProxyPicker()

            scrollPickerTo(hasDescendant(withText(containsString(Constants.INSTAGRAM7_DOMAIN))))
            onView(allOf(withText(containsString(Constants.INSTAGRAM7_DOMAIN)), isDisplayed()))
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(withId(R.id.textViewPlatformProxyStatus))
                .check(matches(withText("Active: ${Constants.INSTAGRAM7_DOMAIN}.")))
        }
    }

    @Test
    fun invalidCustomProxyShowsInlineErrorAndKeepsDialogOpen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openProxyPicker()
            openAddCustomDialog()

            onView(withId(R.id.customProxyInput))
                .inRoot(isDialog())
                .perform(replaceText("not a domain"), closeSoftKeyboard())
            onView(withText("Add"))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(withId(R.id.customProxyInput))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withText(R.string.proxy_error_invalid_domain))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun reservedDomainIsRejected() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openProxyPicker()
            openAddCustomDialog()

            onView(withId(R.id.customProxyInput))
                .inRoot(isDialog())
                .perform(replaceText(Constants.FIXUPX_DOMAIN), closeSoftKeyboard())
            onView(withText("Add"))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(withText(R.string.proxy_error_reserved_domain))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun retiredKkinstagramDomainIsRejected() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openProxyPicker()
            openAddCustomDialog()

            onView(withId(R.id.customProxyInput))
                .inRoot(isDialog())
                .perform(replaceText(Constants.KKINSTAGRAM_DOMAIN), closeSoftKeyboard())
            onView(withText("Add"))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(withText(R.string.proxy_error_reserved_domain))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun retiredFacebookezDomainIsRejected() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openProxyPicker()
            openAddCustomDialog()

            onView(withId(R.id.customProxyInput))
                .inRoot(isDialog())
                .perform(replaceText(Constants.FACEBOOKEZ_DOMAIN), closeSoftKeyboard())
            onView(withText("Add"))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(withText(R.string.proxy_error_reserved_domain))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun addSelectAndDeleteCustomProxyFlow() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openProxyPicker()
            openAddCustomDialog()

            onView(withId(R.id.customProxyInput))
                .inRoot(isDialog())
                .perform(replaceText("https://www.$customProxy/some/path"), closeSoftKeyboard())
            onView(withText("Add"))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            scrollPickerTo(hasDescendant(withText(containsString(customProxy))))
            onView(allOf(withText(containsString(customProxy)), isDisplayed()))
                .check(matches(isDisplayed()))

            onView(allOf(withText(containsString(customProxy)), isDisplayed()))
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(withId(R.id.textViewPlatformProxyStatus))
                .check(matches(withText("Active: $customProxy.")))

            onView(withId(R.id.textViewChangeProxy)).perform(click())
            onView(isRoot()).perform(waitFor(500))
            scrollPickerTo(withText(R.string.proxy_action_edit))
            onView(allOf(withText(R.string.proxy_action_edit), isDisplayed())).perform(click())
            onView(isRoot()).perform(waitFor(300))

            scrollPickerTo(hasDescendant(withText(containsString(customProxy))))
            onView(
                allOf(
                    withId(R.id.proxyDeleteButton),
                    hasSibling(hasDescendant(withText(containsString(customProxy)))),
                    isDisplayed(),
                )
            ).perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(withText(containsString(customProxy)))
                .check(doesNotExist())

            pressBack()
            onView(isRoot()).perform(waitFor(500))

            onView(withId(R.id.textViewPlatformProxyStatus))
                .check(matches(withText("Active: ${Constants.TOINSTAGRAM_DOMAIN}.")))
        }
    }
}
