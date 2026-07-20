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
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.ui.BrowserSettingsActivity
import com.fixupxer.ui.SettingsActivity
import com.fixupxer.utils.BrowserModeUtils
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRegressionTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val preferences
        get() = context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        preferences.edit()
            .clear()
            .putBoolean("browser_enabled", true)
            .putString("action_mode", PreferencesManager.ACTION_MODE_ASK)
            .putBoolean("show_configuration_status_widget", true)
            .commit()
    }

    @After
    fun tearDown() {
        BrowserModeUtils.setBrowserAliasEnabled(context, false)
        preferences.edit().clear().commit()
    }

    @Test
    fun savedAppChoicesCountRefreshesWhenBrowserSettingsResumes() {
        ActivityScenario.launch(BrowserSettingsActivity::class.java).use { scenario ->
            onView(withId(R.id.buttonSavedAppChoices))
                .perform(nestedScrollTo())
                .check(matches(isEnabled()))
            onView(withText(R.string.saved_app_choices_none))
                .check(matches(isDisplayed()))

            scenario.moveToState(Lifecycle.State.CREATED)
            preferences.edit()
                .putString(
                    PreferencesManager.KEY_REMEMBERED_ROUTES,
                    """{"example.com":{"kind":"BROWSER","packageName":"com.android.chrome"}}""",
                )
                .commit()
            scenario.moveToState(Lifecycle.State.RESUMED)

            onView(
                withText(
                    context.resources.getQuantityString(
                        R.plurals.saved_app_choices_setup_incomplete,
                        1,
                        1,
                    )
                )
            )
                .perform(nestedScrollTo())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun rememberedDestinationsDescriptionLivesInDialogWhenRoutingIsInactive() {
        preferences.edit()
            .putBoolean("browser_enabled", false)
            .putString("action_mode", PreferencesManager.ACTION_MODE_ASK)
            .commit()

        ActivityScenario.launch(BrowserSettingsActivity::class.java).use {
            onView(withId(R.id.buttonSavedAppChoices))
                .perform(nestedScrollTo())
                .check(matches(isEnabled()))
                .perform(click())
            onView(withText(R.string.remembered_destinations_scope))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun priorityModeShowsActionPriorityAndMainSettingsKeepsBackup() {
        preferences.edit()
            .putString("action_mode", PreferencesManager.ACTION_MODE_PRIORITY)
            .commit()

        ActivityScenario.launch(BrowserSettingsActivity::class.java).use {
            onView(withId(R.id.actionPrioritySection))
                .perform(nestedScrollTo())
                .check(matches(isDisplayed()))
            onView(withId(R.id.buttonSavedAppChoices))
                .perform(nestedScrollTo())
                .check(matches(not(isEnabled())))
            onView(withText(R.string.saved_app_choices_requires_ask))
                .check(matches(isDisplayed()))
        }
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.buttonBackupSettings))
                .perform(nestedScrollTo())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun inertStatusPreferenceDoesNotHideBrowserDetails() {
        preferences.edit().putBoolean("show_configuration_status_widget", false).commit()

        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.browserModeNavigation))
                .perform(nestedScrollTo())
                .check(matches(allOf(isDisplayed(), isClickable())))
            onView(withId(R.id.configurationStatusNavigation))
                .perform(nestedScrollTo(), click())
            onView(withText(R.string.configuration_status_title))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            assertFalse(preferences.getBoolean("show_configuration_status_widget", true))
            assertTrue(preferences.getBoolean("browser_enabled", false))
        }
    }

    private fun nestedScrollTo(): ViewAction {
        return object : ViewAction {
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
    }
}
