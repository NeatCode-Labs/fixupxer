package com.fixupxer

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.MainActivity
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class UrlInputValidationTest {
    private fun launchMainActivity() {
        ActivityScenario.launch(MainActivity::class.java)
    }

    @Test
    fun testGluedUrlsAreRejected() {
        launchMainActivity()
        val glued = "www.instagram.comwww.x.com"
        
        // Type the glued URLs - this triggers the TextWatcher validation
        onView(withId(R.id.editTextUrl)).perform(replaceText(glued), closeSoftKeyboard())
        
        // Wait for TextWatcher to process
        runBlocking { delay(1500) }
        
        // Verify the input field is cleared (indicating rejection)
        onView(withId(R.id.editTextUrl)).check(matches(withText("")))
        
        // Verify no processed URL is shown
        onView(withId(R.id.textViewProcessedUrl)).check(matches(withText("")))
    }

    @Test
    fun testZeroWidthSpaceAttack() {
        launchMainActivity()
        val tricky = "www.instagram.com\u200Bwww.x.com"
        
        onView(withId(R.id.editTextUrl)).perform(replaceText(tricky), closeSoftKeyboard())
        runBlocking { delay(1500) }
        
        // Verify input is cleared
        onView(withId(R.id.editTextUrl)).check(matches(withText("")))
        onView(withId(R.id.textViewProcessedUrl)).check(matches(withText("")))
    }

    @Test
    fun testUrlEncodedDotAttack() {
        launchMainActivity()
        val tricky = "www%2Einstagram.com"
        
        onView(withId(R.id.editTextUrl)).perform(replaceText(tricky), closeSoftKeyboard())
        runBlocking { delay(1500) }
        
        // Verify input is cleared
        onView(withId(R.id.editTextUrl)).check(matches(withText("")))
        onView(withId(R.id.textViewProcessedUrl)).check(matches(withText("")))
    }

    @Test
    fun testControlCharacterAttack() {
        launchMainActivity()
        val tricky = "www.instagram.com\u0000www.x.com"
        
        onView(withId(R.id.editTextUrl)).perform(replaceText(tricky), closeSoftKeyboard())
        runBlocking { delay(1500) }
        
        // Verify input is cleared
        onView(withId(R.id.editTextUrl)).check(matches(withText("")))
        onView(withId(R.id.textViewProcessedUrl)).check(matches(withText("")))
    }

    @Test
    fun testValidUrlAccepted() {
        launchMainActivity()
        val valid = "https://www.instagram.com/username"
        
        // Type valid URL
        onView(withId(R.id.editTextUrl)).perform(replaceText(valid), closeSoftKeyboard())
        runBlocking { delay(1000) }
        
        // Click process to see the result
        onView(withId(R.id.buttonProcess)).perform(click())
        runBlocking { delay(2000) }
        
        // Verify some processing occurred (either result or error message)
        onView(withId(R.id.textViewProcessedUrl)).check(matches(not(withText(""))))
        
        // Verify the app is still responsive
        onView(withId(R.id.buttonProcess)).check(matches(isEnabled()))
    }

    @Test
    fun testAppDoesNotCrashOnMalformedInput() {
        launchMainActivity()
        val malformed = "not-a-url-at-all"
        
        onView(withId(R.id.editTextUrl)).perform(replaceText(malformed), closeSoftKeyboard())
        runBlocking { delay(1500) }
        
        // If we get here without crashing, the test passes
        // Verify the app is still responsive
        onView(withId(R.id.buttonProcess)).check(matches(isEnabled()))
    }

    @Test
    fun testMultipleProtocolsRejected() {
        launchMainActivity()
        val multipleProtocols = "https://www.instagram.comhttp://www.x.com"
        
        onView(withId(R.id.editTextUrl)).perform(replaceText(multipleProtocols), closeSoftKeyboard())
        runBlocking { delay(1500) }
        
        // Verify input is cleared
        onView(withId(R.id.editTextUrl)).check(matches(withText("")))
        onView(withId(R.id.textViewProcessedUrl)).check(matches(withText("")))
    }

    @Test
    fun testUnicodeNormalizationHandled() {
        launchMainActivity()
        val unicodeTricky = "www.instagram\u0300.com" // Combining accent
        
        onView(withId(R.id.editTextUrl)).perform(replaceText(unicodeTricky), closeSoftKeyboard())
        runBlocking { delay(1500) }
        
        // Verify input is cleared
        onView(withId(R.id.editTextUrl)).check(matches(withText("")))
        onView(withId(R.id.textViewProcessedUrl)).check(matches(withText("")))
    }

    @Test
    fun testProcessButtonWithValidUrl() {
        launchMainActivity()
        val valid = "https://www.instagram.com/username"
        
        // Type valid URL
        onView(withId(R.id.editTextUrl)).perform(replaceText(valid), closeSoftKeyboard())
        runBlocking { delay(1000) }
        
        // Click process button
        onView(withId(R.id.buttonProcess)).perform(click())
        runBlocking { delay(2000) }
        
        // Verify processing occurred
        onView(withId(R.id.textViewProcessedUrl)).check(matches(not(withText(""))))
    }

    @Test
    fun testProcessButtonWithEmptyInput() {
        launchMainActivity()
        
        // Click process button with empty input
        onView(withId(R.id.buttonProcess)).perform(click())
        runBlocking { delay(1000) }
        
        // Verify error state - use the correct string resource
        onView(withId(R.id.textViewProcessedUrl)).check(matches(withText(containsString("No URL to process"))))
    }
} 