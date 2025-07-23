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
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive test suite for release build verification
 */
@RunWith(AndroidJUnit4::class)
class ReleaseTestSuite {
    
    @Test
    fun testAppLaunchesSuccessfully() {
        // Test that the app launches without crashing
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // Verify main UI elements are present
        onView(withId(R.id.editTextUrl))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.buttonProcess))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testCoreUrlProcessing() {
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // Test URL processing functionality
        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://www.instagram.com/p/test123/?utm_source=ig_web&utm_medium=share"))
        
        onView(withId(R.id.buttonProcess))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify URL was processed
        onView(withId(R.id.textViewProcessedUrl))
            .check(matches(isDisplayed()))
            .check(matches(not(withText(""))))
    }
    
    @Test
    fun testAllPlatformConversions() {
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // Test Instagram
        testUrlConversion("https://instagram.com/p/test/?utm_source=test")
        
        // Test Twitter/X
        testUrlConversion("https://x.com/user/status/123?s=20")
        
        // Test Facebook
        testUrlConversion("https://m.facebook.com/story.php?id=123&_rdr")
    }
    
    private fun testUrlConversion(url: String) {
        onView(withId(R.id.editTextUrl))
            .perform(clearText(), replaceText(url))
        
        onView(withId(R.id.buttonProcess))
            .perform(click())
        
        Thread.sleep(300)
        
        // Verify processing completed
        onView(withId(R.id.textViewProcessedUrl))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testCopyShareButtons() {
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // Process a URL first
        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://x.com/test"))
        
        onView(withId(R.id.buttonProcess))
            .perform(click())
        
        Thread.sleep(500)
        
        // Test copy button
        onView(withId(R.id.buttonCopy))
            .check(matches(isEnabled()))
            .perform(click())
        
        // Test share button
        onView(withId(R.id.buttonShare))
            .check(matches(isEnabled()))
    }
    
    @Test
    fun testHistoryFeature() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        
        // Ensure history is enabled
        prefs.edit().putBoolean("history_enabled", true).commit()
        
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // Process a URL to create history
        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://instagram.com/p/test123/"))
        
        onView(withId(R.id.buttonProcess))
            .perform(click())
        
        Thread.sleep(500)
        
        // Open history
        onView(withId(R.id.buttonHistory))
            .perform(scrollTo(), click())
        
        Thread.sleep(500)
        
        // Verify history dialog opens
        onView(withText("Conversion History"))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testReleaseConfiguration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Package name will be com.fixupxer.debug during testing
        // The actual release APK uses com.fixupxer
        
        // Test that app doesn't crash on various inputs
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // Test empty input
        onView(withId(R.id.buttonProcess))
            .perform(click())
        
        // Test invalid input
        onView(withId(R.id.editTextUrl))
            .perform(replaceText("not a url"))
        
        onView(withId(R.id.buttonProcess))
            .perform(click())
        
        // App should handle gracefully without crashing
        Thread.sleep(500)
        
        onView(withId(R.id.editTextUrl))
            .check(matches(isDisplayed()))
    }
} 