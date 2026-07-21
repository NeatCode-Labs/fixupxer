// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.fixupxer

import android.content.Context
import android.view.View
import android.view.ViewParent
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.ui.FrontendSettingsActivity
import com.fixupxer.ui.SettingsActivity
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matcher
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FrontendSettingsActivityTest {

    private lateinit var preferencesManager: PreferencesManager
    private val browserPrivacySeed = "x_xcancel"

    @Before
    fun setup() {
        resetRelevantPrefs()
        preferencesManager = PreferencesManager(
            InstrumentationRegistry.getInstrumentation().targetContext,
        )
        preferencesManager.setBrowserPrivacyTargetId(ProxyPlatform.X, browserPrivacySeed)
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
            .remove("browser_privacy_target_x")
            .commit()
        InstagramProxyStore.reset()
        ProxyRoster.reset()
    }

    private fun waitFor(millis: Long): ViewAction = object : ViewAction {
        override fun getConstraints() = isRoot()
        override fun getDescription() = "Wait for $millis ms"
        override fun perform(uiController: UiController, view: View?) {
            uiController.loopMainThreadForAtLeast(millis)
        }
    }

    private fun nestedScrollTo(): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(View::class.java)
        override fun getDescription() = "Scroll enclosing NestedScrollView to target view"
        override fun perform(uiController: UiController, view: View) {
            var y = view.top
            var parent: ViewParent? = view.parent
            while (parent is View && parent !is NestedScrollView) {
                y += parent.top
                parent = (parent as View).parent
            }
            (parent as? NestedScrollView)?.scrollTo(0, y)
            uiController.loopMainThreadUntilIdle()
        }
    }

    private fun scrollPickerTo(itemMatcher: Matcher<View>) {
        onView(withId(R.id.recyclerViewProxyPicker))
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(itemMatcher))
        onView(isRoot()).perform(waitFor(300))
    }

    @Test
    fun showsAllNinePlatformRows() {
        ActivityScenario.launch(FrontendSettingsActivity::class.java).use {
            val platformNames = listOf(
                R.string.platform_name_x,
                R.string.platform_name_instagram,
                R.string.platform_name_tiktok,
                R.string.platform_name_facebook,
                R.string.platform_name_bluesky,
                R.string.platform_name_reddit,
                R.string.platform_name_youtube,
                R.string.platform_name_pinterest,
                R.string.platform_name_threads,
            )
            for (nameRes in platformNames) {
                onView(withText(nameRes)).perform(nestedScrollTo()).check(matches(isDisplayed()))
            }
        }
    }

    @Test
    fun facebookRowShowsNotConfiguredByDefault() {
        ActivityScenario.launch(FrontendSettingsActivity::class.java).use {
            onView(
                allOf(
                    withText(R.string.frontend_not_configured),
                    hasSibling(withText(R.string.platform_name_facebook)),
                ),
            ).perform(nestedScrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun instagramPickerUpdatesSummaryWithoutTouchingBrowserPrivacy() {
        ActivityScenario.launch(FrontendSettingsActivity::class.java).use {
            onView(withText(R.string.platform_name_instagram))
                .perform(nestedScrollTo(), click())
            onView(isRoot()).perform(waitFor(500))

            scrollPickerTo(hasDescendant(withText(containsString(Constants.INSTAGRAM7_DOMAIN))))
            onView(allOf(withText(containsString(Constants.INSTAGRAM7_DOMAIN)), isDisplayed()))
                .perform(click())
            onView(isRoot()).perform(waitFor(500))

            onView(
                allOf(
                    withText(containsString(Constants.INSTAGRAM7_DOMAIN)),
                    hasSibling(withText(R.string.platform_name_instagram)),
                ),
            ).check(matches(isDisplayed()))

            assertEquals(
                Constants.INSTAGRAM7_DOMAIN,
                preferencesManager.getSelectedProxyDomain(ProxyPlatform.INSTAGRAM),
            )
            assertEquals(
                browserPrivacySeed,
                preferencesManager.getBrowserPrivacyTargetId(ProxyPlatform.X),
            )
        }
    }

    @Test
    fun settingsRowOpensFrontendSettingsActivity() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.buttonAlternativeFrontends))
                .perform(nestedScrollTo(), click())
            onView(withText(R.string.alternative_frontends_title)).check(matches(isDisplayed()))
            onView(withText(R.string.platform_name_instagram))
                .perform(nestedScrollTo())
                .check(matches(isDisplayed()))
        }
    }
}
