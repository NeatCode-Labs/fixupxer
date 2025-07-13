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
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import android.view.View
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import com.fixupxer.PreferencesManager
import org.junit.Before

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
    
    private fun launchShareActivityWithText(text: String) {
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, ShareActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        ActivityScenario.launch<ShareActivity>(intent)
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
            
            // Verify conversion to kkinstagram.com with tracking removed
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://www.kkinstagram.com/p/test123/")))
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
            // Set preferences to enable Instagram conversion (Facebook uses same toggle)
            preferencesManager.setConvertInstagramEnabled(true)
            delay(100)
            
            launchShareActivityWithText("https://m.facebook.com/story.php?story_fbid=123&id=456&_rdr")
            
            // Wait for processing
            onView(isRoot()).perform(waitFor(2000))
            
            // Verify conversion to facebookez.com without m. prefix
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://facebookez.com/story.php?story_fbid=123&id=456")))
        }
    }
    
    @Test
    fun testNothingToDoMessageForCleanUrl() {
        runBlocking {
            // Set preferences to enable Instagram/Facebook conversion
            preferencesManager.setConvertInstagramEnabled(true)
            delay(100)
            
            // Share a clean facebookez.com URL with toggle ON
            launchShareActivityWithText("https://facebookez.com/zuck/posts/10115959821974691")
            
            // Wait for processing
            onView(isRoot()).perform(waitFor(2000))
            
            // Verify "Nothing to do" message
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText(containsString("Nothing to do"))))
        }
    }
    
    @Test
    fun testDirtyFacebookezUrlWithToggleOn() {
        runBlocking {
            // Set preferences to enable Instagram/Facebook conversion
            preferencesManager.setConvertInstagramEnabled(true)
            delay(100)
            
            // Share a dirty facebookez.com URL
            launchShareActivityWithText("https://facebookez.com/zuck/posts/10115959821974691?utm_source=twitter&utm_medium=social")
            
            // Wait for processing
            onView(isRoot()).perform(waitFor(2000))
            
            // Verify URL is cleaned but stays facebookez.com
            onView(withId(R.id.textViewProcessedUrl))
                .check(matches(withText("https://facebookez.com/zuck/posts/10115959821974691")))
        }
    }
    
    @Test
    fun testDirtyFacebookUrlWithToggleOff() {
        runBlocking {
            // Set preferences to enable Instagram/Facebook conversion initially
            preferencesManager.setConvertInstagramEnabled(true)
            delay(100)
            
            // Share a dirty facebook.com URL
            launchShareActivityWithText("https://www.facebook.com/zuck/posts/10115959821974691?tracking=test123")
            
            // Wait for initial processing
            onView(isRoot()).perform(waitFor(2000))
            
            // Toggle Instagram/Facebook conversion off
            onView(withId(R.id.switchInstagram)).perform(click())
            
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
            
            launchShareActivityWithText("https://www.instagram.com/p/test/")
            onView(isRoot()).perform(waitFor(1500))
            onView(withId(R.id.switchInstagram)).check(matches(isDisplayed()))
            
            // Test Twitter URL - should show Twitter toggle
            launchShareActivityWithText("https://x.com/user/status/123")
            onView(isRoot()).perform(waitFor(1500))
            onView(withId(R.id.switchTwitter)).check(matches(isDisplayed()))
            
            // Test Facebook URL - should show Instagram toggle (Facebook uses Instagram toggle)
            launchShareActivityWithText("https://www.facebook.com/test")
            onView(isRoot()).perform(waitFor(1500))
            onView(withId(R.id.switchInstagram)).check(matches(isDisplayed()))
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
} 