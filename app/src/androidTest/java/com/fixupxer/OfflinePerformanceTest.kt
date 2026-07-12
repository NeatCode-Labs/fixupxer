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

import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.MainActivity
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Tests for offline functionality and performance
 */
@RunWith(AndroidJUnit4::class)
class OfflinePerformanceTest {
    @Test
    fun testOfflineFunctionality() {
        // FixupXer works completely offline - no internet required
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // Test URL processing works without internet
        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://www.instagram.com/p/test123/?utm_source=ig_web"))
        
        onView(withId(R.id.buttonProcess))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify URL was processed
        onView(withId(R.id.textViewProcessedUrl))
            .check(matches(isDisplayed()))
            .check(matches(not(withText(""))))
    }
    
    @Test
    fun testAppStartupTime() {
        // Measure app startup time
        val startupTime = measureTimeMillis {
            ActivityScenario.launch(MainActivity::class.java)
            Thread.sleep(500) // Wait for activity to fully load
        }
        
        // App should start in under 3 seconds
        assert(startupTime < 3000) { "App startup took too long: ${startupTime}ms" }
    }
    
    @Test
    fun testUrlProcessingPerformance() {
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)

        // Text entry is setup, not URL processing. Keeping it outside the timed
        // section avoids measuring variable Espresso keyboard/view overhead.
        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://www.facebook.com/share.php?u=https://example.com&utm_source=fb&utm_medium=social&utm_campaign=share&fbclid=123456789"))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val processingDispatchTime = measureTimeMillis {
            onView(withId(R.id.buttonProcess))
                .perform(click())
        }

        // Espresso UI dispatch should remain responsive. Direct pipeline latency
        // is covered separately by CustomRulesPerformanceTest.
        assert(processingDispatchTime < 2000) {
            "URL processing dispatch took too long: ${processingDispatchTime}ms"
        }
        onView(withId(R.id.textViewProcessedUrl))
            .perform(waitForProcessedResult(3000))
    }
    
    @Test
    fun testMemoryUsage() {
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        val runtime = Runtime.getRuntime()
        val beforeMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Process multiple URLs
        repeat(10) { index ->
            onView(withId(R.id.editTextUrl))
                .perform(replaceText("https://x.com/user/status/$index?s=20&t=tracking"))
            
            onView(withId(R.id.buttonProcess))
                .perform(click())
            
            Thread.sleep(100)
        }
        
        val afterMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = afterMemory - beforeMemory
        
        // Memory increase should be reasonable (less than 10MB)
        val memoryIncreaseMB = memoryIncrease / (1024 * 1024)
        assert(memoryIncreaseMB < 10) { "Memory usage increased too much: ${memoryIncreaseMB}MB" }
    }
    
    @Test
    fun testHistoryPerformance() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        
        // Ensure history is enabled
        prefs.edit().putBoolean("history_enabled", true).commit()
        
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // Add multiple history entries
        repeat(5) { index ->
            onView(withId(R.id.editTextUrl))
                .perform(replaceText("https://instagram.com/p/test$index/"))
            
            onView(withId(R.id.buttonProcess))
                .perform(click())
            
            Thread.sleep(200)
        }

        // Let setup writes settle before timing only the dialog-opening path.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(500)

        // Measure history dialog opening time
        val historyOpenTime = measureTimeMillis {
            onView(withId(R.id.buttonHistory))
                .perform(click())

            onView(withId(R.id.switchHistoryEnabled))
                .check(matches(isDisplayed()))
        }
        
        // History should open quickly, allowing for loaded-emulator UI variance.
        assert(historyOpenTime < 3000) { "History dialog took too long to open: ${historyOpenTime}ms" }
    }

    private fun waitForProcessedResult(timeoutMillis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> =
                isAssignableFrom(TextView::class.java)

            override fun getDescription(): String =
                "Wait up to $timeoutMillis ms for the processed URL"

            override fun perform(uiController: UiController, view: View) {
                val textView = view as TextView
                val placeholder = view.context.getString(R.string.result_placeholder)
                val deadline = SystemClock.uptimeMillis() + timeoutMillis

                while (textView.text.toString() == placeholder &&
                    SystemClock.uptimeMillis() < deadline
                ) {
                    uiController.loopMainThreadForAtLeast(10)
                }

                if (textView.text.toString() == placeholder) {
                    throw AssertionError("Processed URL did not appear within $timeoutMillis ms")
                }
            }
        }
    }
} 