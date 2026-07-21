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

import android.content.Context
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
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import android.view.View
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import com.fixupxer.PreferencesManager
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyPlatform
import org.junit.Before
import androidx.preference.PreferenceManager
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class ShareActivityTest {
    
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
    
    private fun launchShareActivityWithText(text: String): ActivityScenario<ShareActivity> {
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, ShareActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return ActivityScenario.launch(intent)
    }
    
    @Test
    fun testInstagramUrlConversionWithToggleOn() {
        runBlocking {
            // Set preferences to enable Instagram conversion
            preferencesManager.setConvertInstagramEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://www.instagram.com/p/test123/?utm_source=ig_web")
            
            // Wait for processing
            onView(isRoot()).perform(waitFor(2000))
            
            // v1.4.8: convert to default proxy (toinstagram.com), strip www., remove tracking
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://toinstagram.com/p/test123/")))
        }
    }
    
    @Test
    fun testInstagramUrlConversionWithToggleOff() {
        runBlocking {
            // Set preferences to disable Instagram conversion
            preferencesManager.setConvertInstagramEnabled(false)
            delay(100)
            
            launchShareActivityWithText("https://www.instagram.com/p/test123/?utm_source=ig_web")
            
            // Wait for processing
            onView(isRoot()).perform(waitFor(2000))
            
            // Verify it stays instagram.com but tracking is removed
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://www.instagram.com/p/test123/")))
        }
    }
    
    @Test
    fun testTwitterUrlConversionWithToggleOn() {
        runBlocking {
            // Set preferences to enable Twitter conversion
            preferencesManager.setConvertTwitterEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://x.com/user/status/123456789?t=abc&s=09")
            
            // Wait for processing
            onView(isRoot()).perform(waitFor(2000))
            
            // Verify conversion to fixupx.com with tracking removed
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://fixupx.com/user/status/123456789")))
        }
    }
    
    @Test
    fun testFacebookUrlConversionWithToggleOn() {
        runBlocking {
            // v2.4.0: Facebook has a dedicated toggle
            preferencesManager.setConvertFacebookEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://m.facebook.com/story.php?story_fbid=123&id=456&_rdr")
            
            // Wait for processing
            onView(isRoot()).perform(waitFor(2000))
            
            // Verify URL stays on facebook.com (no built-in frontend to convert to)
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://m.facebook.com/story.php?story_fbid=123&id=456")))
        }
    }
    
    @Test
    fun testRetiredFacebookezUrlStaysUnchanged() {
        runBlocking {
            preferencesManager.setConvertFacebookEnabled(true)
            delay(100)

            launchShareActivityWithText("https://facebookez.com/zuck/posts/10115959821974691")

            onView(isRoot()).perform(waitFor(2000))

            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://facebookez.com/zuck/posts/10115959821974691")))
            onView(withId(R.id.textViewResultStatus))
                .check(matches(withText(containsString("Already clean"))))
        }
    }

    @Test
    fun testDirtyRetiredFacebookezUrlTrackingRemoved() {
        runBlocking {
            preferencesManager.setConvertFacebookEnabled(true)
            delay(100)

            launchShareActivityWithText("https://facebookez.com/zuck/posts/10115959821974691?utm_source=twitter&utm_medium=social")

            onView(isRoot()).perform(waitFor(2000))

            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://facebookez.com/zuck/posts/10115959821974691")))
        }
    }
    
    @Test
    fun testDirtyFacebookUrlWithToggleOff() {
        runBlocking {
            // v2.4.0: Facebook has a dedicated toggle; enable it initially
            preferencesManager.setConvertFacebookEnabled(true)
            delay(100)
            
            // Share a dirty facebook.com URL (fbclid is a known tracking key;
            // unknown parameters survive the keep-unknown contract)
            launchShareActivityWithText("https://www.facebook.com/zuck/posts/10115959821974691?fbclid=test123")
            
            // Wait for initial processing
            onView(isRoot()).perform(waitFor(2000))
            
            // Toggle Facebook conversion off (dedicated convert_facebook pref)
            onView(withId(R.id.switchPlatform)).perform(click())
            
            // Wait for reprocessing
            onView(isRoot()).perform(waitFor(1500))
            
            // Verify URL is cleaned but stays facebook.com
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://www.facebook.com/zuck/posts/10115959821974691")))
        }
    }
    
    @Test
    fun testShareButtonFunctionality() {
        launchShareActivityWithText("https://www.instagram.com/p/test123/")
        
        // Wait for processing
        onView(isRoot()).perform(waitFor(2000))
        
        // Verify share button is enabled
        onView(withId(R.id.buttonShare))
            .check(matches(isEnabled()))
            .check(matches(isClickable()))
    }
    
    @Test
    fun testCopyButtonFunctionality() {
        launchShareActivityWithText("https://x.com/user/status/123456789")
        
        // Wait for processing
        onView(isRoot()).perform(waitFor(2000))
        
        // Click copy button
        onView(withId(R.id.buttonCopy)).perform(click())
        
        // Wait for toast or feedback
        onView(isRoot()).perform(waitFor(1000))
        
        // Verify button is still enabled (didn't crash)
        onView(withId(R.id.buttonCopy))
            .check(matches(isEnabled()))
    }
    
    @Test
    fun testToggleVisibilityForDifferentUrls() {
        runBlocking {
            // Test Instagram URL - should show Instagram toggle
            preferencesManager.setConvertInstagramEnabled(true)
            preferencesManager.setConvertTwitterEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://www.instagram.com/p/test/").use {
                onView(isRoot()).perform(waitFor(1500))
                onView(withId(R.id.switchPlatform)).check(matches(isDisplayed()))
            }
            
            // Test Twitter URL - should show Twitter toggle
            launchShareActivityWithText("https://x.com/user/status/123").use {
                onView(isRoot()).perform(waitFor(1500))
                onView(withId(R.id.switchPlatform)).check(matches(isDisplayed()))
            }
            
            // Test Facebook URL - should show Facebook toggle (uses convert_instagram pref)
            launchShareActivityWithText("https://www.facebook.com/test").use {
                onView(isRoot()).perform(waitFor(1500))
                onView(withId(R.id.switchPlatform)).check(matches(isDisplayed()))
            }
        }
    }
    
    @Test
    fun testMultipleUrlsRejected() {
        runBlocking {
            // Set preferences first
            preferencesManager.setConvertInstagramEnabled(true)
            delay(500)
            
            val multipleUrls = "https://www.instagram.com/test https://www.facebook.com/test"
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, multipleUrls)
            }
            launchShareActivityWithText(multipleUrls)
            
            // Verify error message for multiple URLs
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText(containsString("Please paste one URL at a time"))))
        }
    }
    
    @Test
    fun testInvalidUrlHandling() {
        runBlocking {
            val invalidUrl = "not a valid url"
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, invalidUrl)
            }
            launchShareActivityWithText(invalidUrl)
            
            // Verify error message
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText(containsString("No URL found in shared text"))))
        }
    }
    
    @Test
    fun testHistoryRecordingOnShare() {
        launchShareActivityWithText("https://www.instagram.com/p/test123/?ig_source=test")
        
        // Wait for processing
        onView(isRoot()).perform(waitFor(2000))
        
        // The history should be recorded automatically
        // We can't directly verify database here, but we can verify the URL was processed
        onView(withId(R.id.textViewProcessedUrl))
            .check(matches(not(withText(""))))
    }
    
    @Test
    fun testActionPriorityMode() {
        runBlocking {
            // Set action mode to priority and enable Instagram conversion
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val prefs = context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("action_mode", PreferencesManager.ACTION_MODE_PRIORITY)
                .putBoolean("action_clipboard", true)
                .putBoolean("convert_instagram", true) // Enable Instagram conversion
                .putBoolean("clean_tracking", true) // Enable tracking removal
                .commit()
            
            // Give time for preferences to be applied
            delay(300)
            
            // Launch with a URL
            launchShareActivityWithText("https://www.instagram.com/p/test123/?utm_source=test")
            
            // Wait for processing
            onView(isRoot()).perform(waitFor(2500))
            
            // v1.4.8: With Instagram conversion enabled, URL converts to default proxy (toinstagram.com),
            // www. is stripped, and tracking parameters are removed.
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://toinstagram.com/p/test123/")))
        }
    }
    
    @Test
    fun testBrowserConversionDefaultsDoNotAffectShareFlow() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()

            preferencesManager.setConvertTwitterEnabled(true)
            preferencesManager.setBrowserConvertTwitterEnabled(true)
            preferencesManager.setSelectedProxyDomain(ProxyPlatform.X, Constants.FIXUPX_DOMAIN)

            assertEquals(
                Constants.XCANCEL_DOMAIN,
                preferencesManager.resolveBrowserPrivacyTarget(ProxyPlatform.X)?.domain,
            )

            delay(200)

            launchShareActivityWithText("https://x.com/user/status/123456789")

            onView(isRoot()).perform(waitFor(2000))

            // Share uses main embed proxy (fixupx), not browser privacy reader (xcancel).
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://fixupx.com/user/status/123456789")))
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(not(withText(containsString(Constants.XCANCEL_DOMAIN)))))
        }
    }
    
    @Test
    fun testFollowActionMode() {
        runBlocking {
            // Set action mode to follow all
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit()
                .putString("action_mode", "follow")
                .putBoolean("action_clipboard", true)
                .putBoolean("action_share", true)
                .putBoolean("convert_twitter", true) // Enable Twitter conversion
                .commit()
            
            // Give time for preferences to be applied
            delay(200)
            
            // Launch with a URL
            launchShareActivityWithText("https://x.com/test/status/123")
            
            // Wait for processing
            onView(isRoot()).perform(waitFor(2000))
            
            // URL should be converted to fixupx.com
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText(containsString("fixupx.com"))))
        }
    }
} 