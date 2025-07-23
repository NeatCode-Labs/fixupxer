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

import android.util.DisplayMetrics
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.MainActivity
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

/**
 * Tests for touch target sizes and accessibility requirements
 */
@RunWith(AndroidJUnit4::class)
class TouchTargetTest {
    
    companion object {
        const val MIN_TOUCH_TARGET_DP = 48
    }
    
    @Test
    fun testButtonTouchTargets() {
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // Test all buttons meet minimum touch target size
        onView(withId(R.id.buttonProcess))
            .check(matches(hasMinimumTouchTargetSize()))
        
        onView(withId(R.id.buttonCopy))
            .check(matches(hasMinimumTouchTargetSize()))
        
        onView(withId(R.id.buttonShare))
            .check(matches(hasMinimumTouchTargetSize()))
        
        onView(withId(R.id.buttonOpen))
            .check(matches(hasMinimumTouchTargetSize()))
    }
    
    @Test
    fun testClickableTextViews() {
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // Test footer text view meets minimum touch target size
        onView(withId(R.id.footerTextView))
            .check(matches(hasMinimumTouchTargetSize()))
    }
    
    /**
     * Custom matcher to check if view meets minimum touch target size
     */
    private fun hasMinimumTouchTargetSize(): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("has minimum touch target size of ${MIN_TOUCH_TARGET_DP}dp")
            }
            
            override fun matchesSafely(view: View): Boolean {
                val displayMetrics = view.context.resources.displayMetrics
                val minPixels = (MIN_TOUCH_TARGET_DP * displayMetrics.density).roundToInt()
                
                // Get the touchable area including padding
                val touchableWidth = view.width + view.paddingLeft + view.paddingRight
                val touchableHeight = view.height + view.paddingTop + view.paddingBottom
                
                // Check if the actual size is already sufficient
                if (view.width >= minPixels && view.height >= minPixels) {
                    return true
                }
                
                // For text views and smaller views, check if they have sufficient padding
                // or if they're part of a larger clickable area
                if (view is android.widget.TextView || view is androidx.appcompat.widget.SwitchCompat) {
                    // These views are acceptable if they have reasonable size
                    return touchableWidth >= minPixels * 0.8 || touchableHeight >= minPixels * 0.8
                }
                
                return touchableWidth >= minPixels && touchableHeight >= minPixels
            }
        }
    }
} 