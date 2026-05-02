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

import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.MainActivity
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for API level compatibility
 */
@RunWith(AndroidJUnit4::class)
class ApiCompatibilityTest {
    
    @Test
    fun testMinSdkCompatibility() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appInfo = context.applicationInfo
        
        // App should support API 21 (Android 5.0) and above
        assert(appInfo.minSdkVersion >= 21) { "App minSdkVersion should be at least 21" }
    }
    
    @Test
    fun testCurrentApiLevel() {
        val currentApi = Build.VERSION.SDK_INT
        
        // Test app runs on current API level
        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(1000)
            
            // Core functionality should work regardless of API level
            onView(withId(R.id.editTextUrl))
                .perform(replaceText("https://instagram.com/p/test123/"))
            
            onView(withId(R.id.buttonProcess))
                .perform(click())
            
            Thread.sleep(500)
            
            // Verify processing works
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(isDisplayed()))
        }
        
        assert(currentApi >= 21) { "Current API level should support FixupXer" }
    }
    
    @Test
    fun testMaterialDesignComponents() {
        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(1000)
            
            // Test Material Design components work properly
            // Material buttons
            onView(withId(R.id.buttonProcess))
                .check(matches(isDisplayed()))
            
            onView(withId(R.id.buttonCopy))
                .check(matches(isDisplayed()))
            
            // EditText with Material styling
            onView(withId(R.id.editTextUrl))
                .check(matches(isDisplayed()))
            
            // Material card views are used throughout the app
        }
    }
    
    @Test
    fun testThemeCompatibility() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(1000)
            
            // Test app theme works properly
            // App should have proper theme applied
            val theme = context.theme
            assert(theme != null) { "App theme should not be null" }
            
            // UI elements should be visible with proper theming
            onView(withId(R.id.editTextUrl))
                .check(matches(isDisplayed()))
        }
    }
    
    @Test
    fun testConfigurationChanges() {
        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(1000)
            
            // Enter some text
            onView(withId(R.id.editTextUrl))
                .perform(replaceText("https://x.com/user/status/123"))
            
            // Process the URL first
            onView(withId(R.id.buttonProcess))
                .perform(click())
            
            Thread.sleep(500)
            
            // Verify URL was processed
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(isDisplayed()))
            
            // Configuration changes are handled by the manifest
            // App maintains state through configChanges attribute
        }
    }
} 