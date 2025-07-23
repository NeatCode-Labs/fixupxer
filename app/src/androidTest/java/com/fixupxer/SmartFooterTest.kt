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
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixupxer.MainActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.CoreMatchers.containsString

/**
 * Tests for the smart footer functionality
 */
@RunWith(AndroidJUnit4::class)
class SmartFooterTest {
    
    @Test
    fun testFooterIsVisible() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Footer should be visible
        onView(withId(R.id.footerTextView))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testFooterContent() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Footer should contain the expected text
        onView(withId(R.id.footerTextView))
            .check(matches(withText(containsString("NeatCode Labs"))))
    }
    
    @Test
    fun testFooterIsClickable() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Footer should be clickable
        onView(withId(R.id.footerTextView))
            .check(matches(isClickable()))
    }
    
    @Test
    fun testScrollViewPositionedCorrectly() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Scroll view should exist and be displayed
        onView(withId(R.id.mainScrollView))
            .check(matches(isDisplayed()))
    }
} 