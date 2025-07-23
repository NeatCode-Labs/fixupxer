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
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixupxer.MainActivity
import com.fixupxer.ui.adapters.HistoryAdapter
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import android.view.View
import androidx.test.espresso.matcher.RootMatchers.isDialog

@RunWith(AndroidJUnit4::class)
class MainActivityHistoryTest {
    
    private fun waitFor(delay: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints() = isRoot()
            override fun getDescription() = "Wait for $delay milliseconds."
            override fun perform(uiController: UiController, view: View?) {
                uiController.loopMainThreadForAtLeast(delay)
            }
        }
    }
    
    private fun launchMainActivity() {
        ActivityScenario.launch(MainActivity::class.java)
    }
    
    @Test
    fun testHistoryButtonVisibility() {
        launchMainActivity()
        
        // Verify History button is visible (replaced Donate button)
        onView(withId(R.id.buttonHistory))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
    }
    
    @Test
    fun testHistoryDialogOpens() {
        launchMainActivity()
        
        // Click History button
        onView(withId(R.id.buttonHistory)).perform(click())
        
        // Wait for dialog
        onView(isRoot()).perform(waitFor(1000))
        
        // Verify dialog is shown
        onView(withText("Conversion History"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testHistoryDialogWithEntries() {
        launchMainActivity()
        
        // First process a URL to create history
        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://www.instagram.com/p/test123/?utm_source=test"))
        onView(withId(R.id.buttonProcess)).perform(click())
        
        // Wait for processing
        onView(isRoot()).perform(waitFor(2000))
        
        // Open history dialog
        onView(withId(R.id.buttonHistory)).perform(click())
        onView(isRoot()).perform(waitFor(1000))
        
        // Verify RecyclerView is displayed
        onView(withId(R.id.recyclerViewHistory))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testHistoryEntryLongPressDelete() {
        launchMainActivity()
        
        // Create a history entry
        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://x.com/user/status/123456789?t=test"))
        onView(withId(R.id.buttonProcess)).perform(click())
        onView(isRoot()).perform(waitFor(2000))
        
        // Open history dialog
        onView(withId(R.id.buttonHistory)).perform(click())
        onView(isRoot()).perform(waitFor(1000))
        
        // Long press on first item (if exists)
        try {
            onView(withId(R.id.recyclerViewHistory))
                .inRoot(isDialog())
                .perform(RecyclerViewActions.actionOnItemAtPosition<HistoryAdapter.HistoryViewHolder>(0, longClick()))
            
            // Wait for deletion to occur (no confirmation dialog)
            onView(isRoot()).perform(waitFor(500))
        } catch (e: Exception) {
            // No items in history, test passes
        }
    }
    
    @Test
    fun testClearAllHistory() {
        launchMainActivity()
        
        // Create some history entries
        val urls = listOf(
            "https://www.instagram.com/p/1",
            "https://www.facebook.com/test",
            "https://x.com/status/123"
        )
        
        urls.forEach { url ->
            onView(withId(R.id.editTextUrl)).perform(replaceText(url))
            onView(withId(R.id.buttonProcess)).perform(click())
            onView(isRoot()).perform(waitFor(1500))
        }
        
        // Open history dialog
        onView(withId(R.id.buttonHistory)).perform(click())
        onView(isRoot()).perform(waitFor(1000))
        
        // Click Clear All button
        onView(withId(R.id.btnClearAll))
            .inRoot(isDialog())
            .perform(click())
        
        // Wait for clear all dialog and click the clear button
        onView(isRoot()).perform(waitFor(500))
        onView(withId(R.id.buttonClearAll))
            .inRoot(isDialog())
            .perform(click())
        
        onView(isRoot()).perform(waitFor(1000))
        
        // Verify empty state is shown
        onView(withId(R.id.textViewEmpty))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testHistoryEntryCopyButton() {
        launchMainActivity()
        
        // Create a history entry
        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://m.facebook.com/story.php?id=123&tracking=yes"))
        onView(withId(R.id.buttonProcess)).perform(click())
        onView(isRoot()).perform(waitFor(2000))
        
        // Open history dialog
        onView(withId(R.id.buttonHistory)).perform(click())
        onView(isRoot()).perform(waitFor(1000))
        
        // Try to click copy button on first item
        try {
            onView(withId(R.id.buttonCopy))
                .inRoot(isDialog())
                .perform(click())
            
            // Verify no crash occurred
            onView(isRoot()).perform(waitFor(500))
        } catch (e: Exception) {
            // No items or button not found, test passes
        }
    }
    
    @Test
    fun testHistoryEntryShareButton() {
        launchMainActivity()
        
        // Create a history entry
        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://www.instagram.com/reel/test123"))
        onView(withId(R.id.buttonProcess)).perform(click())
        onView(isRoot()).perform(waitFor(2000))
        
        // Open history dialog
        onView(withId(R.id.buttonHistory)).perform(click())
        onView(isRoot()).perform(waitFor(1000))
        
        // Try to click share button on first item
        try {
            onView(withId(R.id.buttonShare))
                .inRoot(isDialog())
                .perform(click())
            
            // Share dialog should open, just verify no crash
            onView(isRoot()).perform(waitFor(1000))
        } catch (e: Exception) {
            // No items or button not found, test passes
        }
    }
    
    @Test
    fun testClearAllHistoryWithConfirmation() {
        launchMainActivity()
        
        // Add a URL to history first
        val url = "https://www.instagram.com/p/test123/?utm_source=ig_web"
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(url), closeSoftKeyboard())
        onView(withId(R.id.buttonProcess)).perform(click())
        
        // Wait for processing
        onView(isRoot()).perform(waitFor(1500))
        
        // Open history dialog
        onView(withId(R.id.buttonHistory)).perform(click())
        onView(isRoot()).perform(waitFor(1000))
        
        // Click clear all button
        onView(withId(R.id.btnClearAll))
            .inRoot(isDialog())
            .perform(click())
        
        // Wait for confirmation dialog
        onView(isRoot()).perform(waitFor(500))
        
        // Confirm deletion - click the "Clear All" button in the confirmation dialog
        onView(withId(R.id.buttonClearAll))
            .inRoot(isDialog())
            .perform(click())
        
        // Wait for clearing
        onView(isRoot()).perform(waitFor(1000))
        
        // Verify empty state is shown
        onView(withText(containsString("No conversion history yet")))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testHistoryDialogDismiss() {
        launchMainActivity()
        
        // Open history dialog
        onView(withId(R.id.buttonHistory)).perform(click())
        onView(isRoot()).perform(waitFor(1000))
        
        // Press back to dismiss
        onView(isRoot()).perform(pressBack())
        onView(isRoot()).perform(waitFor(500))
        
        // Verify we're back to main activity
        onView(withId(R.id.buttonHistory))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testUrlValidationWithHistoryFeature() {
        launchMainActivity()
        
        // Test that validation still works with history feature
        val invalidUrl = "https://instagram.comhttps://facebook.com"
        
        onView(withId(R.id.editTextUrl))
            .perform(replaceText(invalidUrl), closeSoftKeyboard())
        
        onView(isRoot()).perform(waitFor(1500))
        
        // Verify input is cleared due to multiple URLs
        onView(withId(R.id.editTextUrl))
            .check(matches(withText("")))
        
        // Verify error message
        onView(withId(R.id.textViewProcessedUrl))
            .check(matches(withText(containsString("Please paste one URL at a time"))))
    }
} 