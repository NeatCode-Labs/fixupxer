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

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixupxer.MainActivity
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
@Smoke
@RunWith(AndroidJUnit4::class)
class UrlInputValidationTest {
    private fun launchMainActivity() {
        ActivityScenario.launch(MainActivity::class.java)
    }


    @Test
    fun testGluedUrlsAreRejected() {
        launchMainActivity()
        val glued = "www.instagram.comwww.x.com"
        
        // Type the glued URLs - this triggers the TextWatcher validation
        onView(withId(R.id.editTextUrl)).perform(replaceText(glued), closeSoftKeyboard())
        
        // Wait for TextWatcher to process
        awaitAssertion {
            // Verify the input field is cleared (indicating rejection)
            onView(withId(R.id.editTextUrl)).check(matches(withText("")))
        }
        
        // Verify error message is shown in the input field's error slot
        onView(withText(containsString("Please paste one URL at a time"))).check(matches(isDisplayed()))
    }

    @Test
    fun testZeroWidthSpaceAttack() {
        launchMainActivity()
        val tricky = "www.instagram.com\u200Bwww.x.com"
        
        onView(withId(R.id.editTextUrl)).perform(replaceText(tricky), closeSoftKeyboard())
        awaitAssertion {
            // Verify input is cleared
            onView(withId(R.id.editTextUrl)).check(matches(withText("")))
        }
        // Verify error message is shown
        onView(withText(containsString("Please paste one URL at a time"))).check(matches(isDisplayed()))
    }

    @Test
    fun testUrlEncodedDotAttack() {
        launchMainActivity()
        val tricky = "www%2Einstagram.com"
        
        onView(withId(R.id.editTextUrl)).perform(replaceText(tricky), closeSoftKeyboard())
        awaitAssertion {
            // Verify input is cleared
            onView(withId(R.id.editTextUrl)).check(matches(withText("")))
        }
        // Not a multi-URL paste — the generic invalid-input message is shown
        onView(withText(containsString("This input can't be processed"))).check(matches(isDisplayed()))
    }

    @Test
    fun testControlCharacterAttack() {
        launchMainActivity()
        val tricky = "www.instagram.com\u0000www.x.com"
        
        onView(withId(R.id.editTextUrl)).perform(replaceText(tricky), closeSoftKeyboard())
        awaitAssertion {
            // Verify input is cleared
            onView(withId(R.id.editTextUrl)).check(matches(withText("")))
        }
        // Verify error message is shown
        onView(withText(containsString("Please paste one URL at a time"))).check(matches(isDisplayed()))
    }

    @Test
    fun testValidUrlAccepted() {
        launchMainActivity()
        val valid = "https://www.instagram.com/username"
        
        // Type valid URL
        onView(withId(R.id.editTextUrl)).perform(replaceText(valid), closeSoftKeyboard())
        onView(isRoot()).perform(waitFor(1000))
        
        // Click process to see the result
        onView(withId(R.id.buttonProcess)).perform(click())
        awaitAssertion {
            // Verify some processing occurred (either result or error message)
            onView(withId(R.id.textViewProcessedUrl)).check(matches(not(withText(""))))
        }
        
        // Verify the app is still responsive
        onView(withId(R.id.buttonProcess)).check(matches(isEnabled()))
    }

    @Test
    fun testAppDoesNotCrashOnMalformedInput() {
        launchMainActivity()
        val malformed = "not-a-url-at-all"
        
        onView(withId(R.id.editTextUrl)).perform(replaceText(malformed), closeSoftKeyboard())
        awaitAssertion {
            // If we get here without crashing, the test passes
            // Verify the app is still responsive
            onView(withId(R.id.buttonProcess)).check(matches(isEnabled()))
        }
    }

    @Test
    fun testMultipleProtocolsRejected() {
        launchMainActivity()
        val multipleProtocols = "https://www.instagram.comhttp://www.x.com"
        
        onView(withId(R.id.editTextUrl)).perform(replaceText(multipleProtocols), closeSoftKeyboard())
        awaitAssertion {
            // Verify input is cleared
            onView(withId(R.id.editTextUrl)).check(matches(withText("")))
        }
        // Verify error message is shown
        onView(withText(containsString("Please paste one URL at a time"))).check(matches(isDisplayed()))
    }

    @Test
    fun testUnicodeNormalizationHandled() {
        launchMainActivity()
        val unicodeTricky = "www.instagram\u0300.com" // Combining accent
        
        onView(withId(R.id.editTextUrl)).perform(replaceText(unicodeTricky), closeSoftKeyboard())
        awaitAssertion {
            // Verify input is cleared
            onView(withId(R.id.editTextUrl)).check(matches(withText("")))
        }
        // Not a multi-URL paste — the generic invalid-input message is shown
        onView(withText(containsString("This input can't be processed"))).check(matches(isDisplayed()))
    }

    @Test
    fun testProcessButtonWithValidUrl() {
        launchMainActivity()
        val valid = "https://www.instagram.com/username"
        
        // Type valid URL
        onView(withId(R.id.editTextUrl)).perform(replaceText(valid), closeSoftKeyboard())
        onView(isRoot()).perform(waitFor(1000))
        
        // Click process button
        onView(withId(R.id.buttonProcess)).perform(click())
        awaitAssertion {
            // Verify processing occurred
            onView(withId(R.id.textViewProcessedUrl)).check(matches(not(withText(""))))
        }
    }

    @Test
    fun testProcessButtonWithEmptyInput() {
        launchMainActivity()
        
        // Click process button with empty input
        onView(isRoot()).perform(waitFor(500))
        onView(withId(R.id.buttonProcess)).perform(click())
        awaitAssertion {
            // Verify error state - input errors render in the TextInputLayout error slot
            onView(withText(containsString("Please enter a URL"))).check(matches(isDisplayed()))
        }
    }
} 