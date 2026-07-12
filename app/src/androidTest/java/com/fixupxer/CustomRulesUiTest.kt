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

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixupxer.ui.SettingsActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomRulesUiTest {
    @Test
    fun createRuleFromSettingsWithoutRawJson() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.buttonCustomRulesHowTo)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonCustomRules)).perform(click())
            onView(withText(R.string.custom_rules_title)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonAddRule)).perform(click())
            onView(withId(R.id.editName))
                .perform(replaceText("Instrumentation rule"), closeSoftKeyboard())
            onView(withId(R.id.mainScrollView)).perform(swipeUp(), swipeUp(), swipeUp())
            onView(withId(R.id.buttonSave)).perform(click())
            onView(withText("Instrumentation rule")).check(matches(isDisplayed()))
            onView(withId(R.id.buttonClear)).perform(click())
            onView(withText(R.string.custom_rules_clear)).perform(click())
        }
    }
}
