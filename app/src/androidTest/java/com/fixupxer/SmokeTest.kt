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

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.MainActivity
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lightweight smoke tests for release build verification.
 */
@Smoke
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @Test
    fun testAppLaunchesSuccessfully() {
        ActivityScenario.launch(MainActivity::class.java)

        awaitAssertion {
            onView(withId(R.id.editTextUrl))
                .check(matches(isDisplayed()))
            onView(withId(R.id.buttonProcess))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testCoreUrlProcessing() {
        ActivityScenario.launch(MainActivity::class.java)

        awaitAssertion {
            onView(withId(R.id.editTextUrl)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://www.instagram.com/p/test123/?utm_source=ig_web&utm_medium=share"))

        onView(withId(R.id.buttonProcess))
            .perform(click())

        awaitAssertion {
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(isDisplayed()))
                .check(matches(not(withText(""))))
        }
    }

    @Test
    fun testAllPlatformConversions() {
        ActivityScenario.launch(MainActivity::class.java)

        awaitAssertion {
            onView(withId(R.id.editTextUrl)).check(matches(isDisplayed()))
        }

        testUrlConversion("https://instagram.com/p/test/?utm_source=test", "/p/test/")
        testUrlConversion("https://x.com/user/status/123?s=20", "/user/status/123")
        testUrlConversion("https://m.facebook.com/story.php?id=123&_rdr", "story.php")
    }

    /**
     * [expectedInResult] must be unique to [url] — the field still holds the previous result when
     * this runs, so a generic "not empty" check would pass before the new URL is processed.
     */
    private fun testUrlConversion(url: String, expectedInResult: String) {
        onView(withId(R.id.editTextUrl))
            .perform(clearText(), replaceText(url))

        onView(withId(R.id.buttonProcess))
            .perform(click())

        awaitAssertion {
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText(containsString(expectedInResult))))
        }
    }

    @Test
    fun testCopyShareButtons() {
        ActivityScenario.launch(MainActivity::class.java)

        awaitAssertion {
            onView(withId(R.id.editTextUrl)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://x.com/test"))

        onView(withId(R.id.buttonProcess))
            .perform(click())

        awaitAssertion {
            onView(withId(R.id.buttonCopy))
                .check(matches(isEnabled()))
        }

        onView(withId(R.id.buttonCopy))
            .perform(click())

        onView(withId(R.id.buttonShare))
            .check(matches(isEnabled()))
    }

    @Test
    fun testHistoryFeature() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)

        prefs.edit().putBoolean("history_enabled", true).commit()

        ActivityScenario.launch(MainActivity::class.java)

        awaitAssertion {
            onView(withId(R.id.editTextUrl)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://instagram.com/p/test123/"))

        onView(withId(R.id.buttonProcess))
            .perform(click())

        awaitProcessedUrl()

        onView(withId(R.id.buttonHistory))
            .perform(click())

        awaitAssertion {
            onView(withText("Conversion History"))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testReleaseConfiguration() {
        ActivityScenario.launch(MainActivity::class.java)

        awaitAssertion {
            onView(withId(R.id.editTextUrl)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.buttonProcess))
            .perform(click())

        onView(withId(R.id.editTextUrl))
            .perform(replaceText("not a url"))

        onView(withId(R.id.buttonProcess))
            .perform(click())

        awaitAssertion {
            onView(withId(R.id.editTextUrl))
                .check(matches(withText("not a url")))
        }
    }
}
