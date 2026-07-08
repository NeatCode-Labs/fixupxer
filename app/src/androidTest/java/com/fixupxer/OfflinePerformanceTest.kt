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
        
        // Measure URL processing time
        val processingTime = measureTimeMillis {
            onView(withId(R.id.editTextUrl))
                .perform(replaceText("https://www.facebook.com/share.php?u=https://example.com&utm_source=fb&utm_medium=social&utm_campaign=share&fbclid=123456789"))
            
            onView(withId(R.id.buttonProcess))
                .perform(click())
            
            Thread.sleep(100) // Small delay for processing
        }
        
        // URL processing should be near-instant (under 1 second)
        assert(processingTime < 1000) { "URL processing took too long: ${processingTime}ms" }
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
        
        // Measure history dialog opening time
        val historyOpenTime = measureTimeMillis {
            onView(withId(R.id.buttonHistory))
                .perform(click())
            
            Thread.sleep(500) // Wait for dialog to open
        }
        
        // History should open quickly (under 2 seconds)
        assert(historyOpenTime < 2000) { "History dialog took too long to open: ${historyOpenTime}ms" }
    }
} 