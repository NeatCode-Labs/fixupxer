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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixupxer.ui.SettingsActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomRulesUiTest {
    @Test
    fun createRuleFromSettingsWithoutRawJson() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PreferencesManager(context).setCustomRulesEnabled(false)

        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.buttonCustomRulesHowTo)).check(matches(isDisplayed()))
            onView(withId(R.id.switchCustomRules)).check(matches(isNotChecked()))
            onView(withId(R.id.buttonCustomRules)).perform(click())
            onView(withText(R.string.custom_rules_title)).check(matches(isDisplayed()))
            onView(withId(R.id.switchCustomRules)).check(doesNotExist())
            onView(withId(R.id.buttonAddRule)).check { view, noViewFound ->
                noViewFound?.let { throw it }
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                val navigationInset = ViewCompat.getRootWindowInsets(view)
                    ?.getInsets(WindowInsetsCompat.Type.navigationBars())
                    ?.bottom ?: 0
                val usableBottom = view.rootView.height - navigationInset
                assertTrue(
                    "Add rule button overlaps the navigation bar",
                    location[1] + view.height <= usableBottom
                )
            }
            onView(withId(R.id.buttonAddRule)).perform(click())
            onView(withId(R.id.editName))
                .perform(replaceText("Instrumentation rule"), closeSoftKeyboard())
            onView(withId(R.id.mainScrollView))
                .perform(swipeUp(), swipeUp(), swipeUp(), swipeUp(), swipeUp())
            onView(withId(R.id.buttonSave)).perform(click())
            onView(withText("Instrumentation rule")).check(matches(isDisplayed()))
            onView(withId(R.id.buttonClear)).perform(click())
            onView(withText(R.string.custom_rules_clear)).perform(click())
            onView(withId(R.id.emptyState)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonTemplates)).perform(click())
            onView(withText(R.string.custom_rules_template_privacy)).perform(click())
            onView(withId(R.id.emptyState))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.recyclerRules)).check(matches(isDisplayed()))
            onView(withText("Remove common campaign parameters")).perform(click())
            onView(withId(R.id.editName))
                .check(matches(withText("Remove common campaign parameters")))
            pressBack()
            onView(withText(R.string.custom_rule_unsaved_changes)).check(doesNotExist())
            onView(withId(R.id.buttonTemplates)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonClear)).perform(click())
            onView(withText(R.string.custom_rules_clear)).perform(click())
        }
    }
}
