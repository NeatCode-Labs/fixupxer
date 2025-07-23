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
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.ui.ShareActivity
import org.hamcrest.CoreMatchers.containsString
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import android.view.View
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Before
import com.fixupxer.PreferencesManager

/**
 * Tests for bidirectional URL conversion scenarios that were missing
 */
@RunWith(AndroidJUnit4::class)
class BidirectionalConversionTest {
    
    private lateinit var preferencesManager: PreferencesManager
    
    @Before
    fun setup() {
        preferencesManager = PreferencesManager(InstrumentationRegistry.getInstrumentation().targetContext)
    }
    
    private fun waitFor(delay: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints() = isRoot()
            override fun getDescription() = "Wait for $delay milliseconds."
            override fun perform(uiController: UiController, view: View?) {
                uiController.loopMainThreadForAtLeast(delay)
            }
        }
    }
    
    private fun launchShareActivityWithText(text: String) {
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, ShareActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        ActivityScenario.launch<ShareActivity>(intent)
    }
    
    // ============ INSTAGRAM BIDIRECTIONAL TESTS ============
    
    @Test
    fun testCleanInstagramToKkinstagramConversion() {
        runBlocking {
            // Clean instagram.com → kkinstagram.com (toggle ON)
            preferencesManager.setConvertInstagramEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://www.instagram.com/p/test123/")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should convert to kkinstagram with toggle ON
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://www.kkinstagram.com/p/test123/")))
        }
    }
    
    @Test
    fun testCleanKkinstagramToInstagramConversion() {
        runBlocking {
            // Clean kkinstagram.com → instagram.com (toggle OFF)
            preferencesManager.setConvertInstagramEnabled(false)
            delay(100)
            
            launchShareActivityWithText("https://www.kkinstagram.com/p/test123/")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should convert back to instagram.com
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://www.instagram.com/p/test123/")))
        }
    }
    
    @Test
    fun testDirtyKkinstagramToCleanInstagram() {
        runBlocking {
            // Dirty kkinstagram.com → Clean instagram.com (toggle OFF)
            preferencesManager.setConvertInstagramEnabled(false)
            delay(100)
            
            launchShareActivityWithText("https://www.kkinstagram.com/p/test123/?utm_source=app&igshid=abc")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should clean and convert to instagram.com
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://www.instagram.com/p/test123/")))
        }
    }
    
    @Test
    fun testCleanInstagramNothingToDoWithToggleOff() {
        runBlocking {
            // Clean instagram.com with toggle OFF should show "Nothing to do"
            preferencesManager.setConvertInstagramEnabled(false)
            delay(100)
            
            launchShareActivityWithText("https://www.instagram.com/p/test123/")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should show "Nothing to do"
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText(containsString("Nothing to do"))))
        }
    }
    
    // ============ TWITTER/X BIDIRECTIONAL TESTS ============
    
    @Test
    fun testCleanTwitterToFixupxConversion() {
        runBlocking {
            // Clean twitter.com → fixupx.com (toggle ON)
            preferencesManager.setConvertTwitterEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://twitter.com/user/status/123456789")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should convert to fixupx with toggle ON
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://fixupx.com/user/status/123456789")))
        }
    }
    
    @Test
    fun testCleanXToFixupxConversion() {
        runBlocking {
            // Clean x.com → fixupx.com (toggle ON)
            preferencesManager.setConvertTwitterEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://x.com/user/status/123456789")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should convert to fixupx with toggle ON
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://fixupx.com/user/status/123456789")))
        }
    }
    
    @Test
    fun testCleanFixupxToXConversion() {
        runBlocking {
            // Clean fixupx.com → x.com (toggle OFF)
            preferencesManager.setConvertTwitterEnabled(false)
            delay(100)
            
            launchShareActivityWithText("https://fixupx.com/user/status/123456789")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should convert back to x.com
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://x.com/user/status/123456789")))
        }
    }
    
    @Test
    fun testDirtyFixupxToCleanX() {
        runBlocking {
            // Dirty fixupx.com → Clean x.com (toggle OFF)
            preferencesManager.setConvertTwitterEnabled(false)
            delay(100)
            
            launchShareActivityWithText("https://fixupx.com/user/status/123456789?t=test&s=09")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should clean and convert to x.com
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://x.com/user/status/123456789")))
        }
    }
    
    @Test
    fun testDirtyFixupxToCleanFixupx() {
        runBlocking {
            // Dirty fixupx.com → Clean fixupx.com (toggle ON)
            preferencesManager.setConvertTwitterEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://fixupx.com/user/status/123456789?utm_source=share")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should clean but stay fixupx.com
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://fixupx.com/user/status/123456789")))
        }
    }
    
    @Test
    fun testCleanXNothingToDoWithToggleOff() {
        runBlocking {
            // Clean x.com with toggle OFF should show "Nothing to do"
            preferencesManager.setConvertTwitterEnabled(false)
            delay(100)
            
            launchShareActivityWithText("https://x.com/user/status/123456789")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should show "Nothing to do"
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText(containsString("Nothing to do"))))
        }
    }
    
    // ============ FACEBOOK BIDIRECTIONAL TESTS ============
    
    @Test
    fun testCleanFacebookToFacebookezConversion() {
        runBlocking {
            // Clean facebook.com → facebookez.com (toggle ON)
            preferencesManager.setConvertInstagramEnabled(true) // Facebook uses Instagram toggle
            delay(100)
            
            launchShareActivityWithText("https://www.facebook.com/zuck/posts/123456789")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should convert to facebookez with toggle ON
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://facebookez.com/zuck/posts/123456789")))
        }
    }
    
    @Test
    fun testCleanFacebookezToFacebookConversion() {
        runBlocking {
            // Clean facebookez.com → facebook.com (toggle OFF)
            preferencesManager.setConvertInstagramEnabled(false) // Facebook uses Instagram toggle
            delay(100)
            
            launchShareActivityWithText("https://facebookez.com/zuck/posts/123456789")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should convert back to facebook.com
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://facebook.com/zuck/posts/123456789")))
        }
    }
    
    @Test
    fun testDirtyFacebookezToCleanFacebook() {
        runBlocking {
            // Dirty facebookez.com → Clean facebook.com (toggle OFF)
            preferencesManager.setConvertInstagramEnabled(false) // Facebook uses Instagram toggle
            delay(100)
            
            launchShareActivityWithText("https://facebookez.com/story.php?story_fbid=123&id=456&fbclid=abc")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should clean and convert to facebook.com
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://facebook.com/story.php?story_fbid=123&id=456")))
        }
    }
    
    @Test
    fun testWebFacebookPrefixRemoval() {
        runBlocking {
            // web.facebook.com → facebookez.com (prefix removal)
            preferencesManager.setConvertInstagramEnabled(true) // Facebook uses Instagram toggle
            delay(100)
            
            launchShareActivityWithText("https://web.facebook.com/story.php?story_fbid=123&id=456")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should remove web. prefix and convert to facebookez
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://facebookez.com/story.php?story_fbid=123&id=456")))
        }
    }
    
    @Test
    fun testWwwFacebookPrefixRemoval() {
        runBlocking {
            // www.facebook.com → facebookez.com (no www. in result)
            preferencesManager.setConvertInstagramEnabled(true) // Facebook uses Instagram toggle
            delay(100)
            
            launchShareActivityWithText("https://www.facebook.com/zuck/posts/123456789")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should convert to facebookez without www
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://facebookez.com/zuck/posts/123456789")))
        }
    }
    
    @Test
    fun testCleanFacebookNothingToDoWithToggleOff() {
        runBlocking {
            // Clean facebook.com with toggle OFF should show "Nothing to do"
            preferencesManager.setConvertInstagramEnabled(false) // Facebook uses Instagram toggle
            delay(100)
            
            launchShareActivityWithText("https://www.facebook.com/zuck/posts/123456789")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should show "Nothing to do"
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText(containsString("Nothing to do"))))
        }
    }
    
    // ============ EDGE CASES ============
    
    @Test
    fun testMixedCaseUrlHandling() {
        runBlocking {
            // INSTAGRAM.COM should still be processed correctly
            preferencesManager.setConvertInstagramEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://WWW.INSTAGRAM.COM/p/TEST123/")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should convert case-insensitively
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://WWW.kkinstagram.com/p/TEST123/")))
        }
    }
    
    @Test
    fun testUrlWithFragment() {
        runBlocking {
            // URL with fragment should preserve it
            preferencesManager.setConvertTwitterEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://x.com/user/status/123456789#reply")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should convert and preserve fragment
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://fixupx.com/user/status/123456789#reply")))
        }
    }
    
    @Test
    fun testFxTwitterToFixupxConversion() {
        runBlocking {
            // fxtwitter.com should convert to fixupx.com
            preferencesManager.setConvertTwitterEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://fxtwitter.com/user/status/123456789")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should convert fxtwitter to fixupx
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://fixupx.com/user/status/123456789")))
        }
    }
    
    @Test
    fun testDirtyKkinstagramToCleanKkinstagram() {
        runBlocking {
            // Dirty kkinstagram.com → Clean kkinstagram.com (toggle ON)
            preferencesManager.setConvertInstagramEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://www.kkinstagram.com/p/test/?utm_source=ig_web&igshid=test")
            onView(isRoot()).perform(waitFor(2000))
            
            // Should clean but stay kkinstagram
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://www.kkinstagram.com/p/test/")))
        }
    }
} 