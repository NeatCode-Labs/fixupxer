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
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProxyPickerDialogTest {

    @Before
    fun setup() {
        resetPrefs()
    }

    @After
    fun tearDown() {
        resetPrefs()
    }

    private fun resetPrefs() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putBoolean("convert_twitter", true)
            .putBoolean("convert_instagram", true)
            .commit()
        ProxyRoster.reset()
    }

    private fun waitFor(millis: Long): ViewAction = object : ViewAction {
        override fun getConstraints() = isRoot()
        override fun getDescription() = "Wait for $millis ms"
        override fun perform(uiController: UiController, view: View?) {
            uiController.loopMainThreadForAtLeast(millis)
        }
    }

    private fun openXPicker() {
        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://twitter.com/user/status/1"), closeSoftKeyboard())
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

    @Test
    fun xGroupsRenderInCatalogOrder() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openXPicker()

            onView(withText(R.string.proxy_section_embed)).check(matches(isDisplayed()))
            onView(withText(R.string.proxy_section_privacy)).check(matches(isDisplayed()))
            onView(withText(R.string.proxy_group_recommended)).check(matches(isDisplayed()))
            onView(allOf(withText(containsString(Constants.FIXUPX_DOMAIN)), isDisplayed()))
                .check(matches(isDisplayed()))
            onView(allOf(withText(containsString(Constants.XCANCEL_DOMAIN)), isDisplayed()))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun fullPickerShowsEmbedSectionBeforePrivacySection() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openXPicker()

            onView(withText(R.string.proxy_section_embed)).check(matches(isDisplayed()))
            onView(allOf(withText(containsString(Constants.FIXUPX_DOMAIN)), isDisplayed()))
                .check(matches(isDisplayed()))

            // Section header rows are LinearLayouts; match the item view by descendant text.
            scrollPickerTo(hasDescendant(withText(R.string.proxy_section_privacy)))
            onView(withText(R.string.proxy_section_privacy)).check(matches(isDisplayed()))
            onView(allOf(withText(containsString(Constants.XCANCEL_DOMAIN)), isDisplayed()))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun xReaderSelectionPersistsInLabel() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        PreferencesManager(ctx).setSelectedProxyDomain(ProxyPlatform.X, Constants.XCANCEL_DOMAIN)

        ActivityScenario.launch(MainActivity::class.java).use {
            openXPicker()
            onView(withText(containsString(Constants.XCANCEL_DOMAIN))).perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(withId(R.id.textViewPlatformProxyStatus))
                .check(matches(withText("Active: ${Constants.XCANCEL_DOMAIN}.")))
            onView(withId(R.id.platformTitle))
                .check(matches(withText(R.string.read_without_account)))
        }
    }

    @Test
    fun restoreBuiltInsRowAppearsWhenDisabled() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = PreferencesManager(ctx)
        prefs.disableBuiltIn(ProxyPlatform.X, "x_fixupx")

        ActivityScenario.launch(MainActivity::class.java).use {
            openXPicker()
            // The action rows sit below all target rows; the hidden empty-state
            // button shares the same text, so require a displayed match.
            scrollPickerTo(withText(R.string.proxy_action_restore_builtins))
            onView(allOf(withText(R.string.proxy_action_restore_builtins), isDisplayed()))
                .perform(click())
            onView(isRoot()).perform(waitFor(500))
            scrollPickerTo(hasDescendant(withText(containsString(Constants.FIXUPX_DOMAIN))))
            onView(allOf(withText(containsString(Constants.FIXUPX_DOMAIN)), isDisplayed()))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun deleteBuiltInCanBeUndoneViaSnackbar() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openXPicker()
            scrollPickerTo(withText(R.string.proxy_action_edit))
            onView(allOf(withText(R.string.proxy_action_edit), isDisplayed())).perform(click())
            onView(isRoot()).perform(waitFor(300))
            // Every row shows a delete button in edit mode; target the fixupx row.
            scrollPickerTo(hasDescendant(withText(containsString(Constants.FIXUPX_DOMAIN))))
            onView(
                allOf(
                    withId(R.id.proxyDeleteButton),
                    hasSibling(hasDescendant(withText(containsString(Constants.FIXUPX_DOMAIN)))),
                    isDisplayed(),
                )
            ).perform(click())
            onView(isRoot()).perform(waitFor(500))
            onView(allOf(withText(R.string.undo), isDisplayed())).perform(click())
            onView(isRoot()).perform(waitFor(500))
            pressBack()
            onView(isRoot()).perform(waitFor(500))
            onView(withId(R.id.textViewPlatformProxyStatus))
                .check(matches(withText("Active: ${Constants.FIXUPX_DOMAIN}.")))
        }
    }
}
