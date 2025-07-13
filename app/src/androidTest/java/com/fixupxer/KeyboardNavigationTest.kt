package com.fixupxer

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixupxer.MainActivity
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for keyboard handling and navigation
 */
@RunWith(AndroidJUnit4::class)
class KeyboardNavigationTest {
    
    @Test
    fun testKeyboardInputAndDismissal() {
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // Click on URL input field
        onView(withId(R.id.editTextUrl))
            .perform(click())
        
        // Type some text
        onView(withId(R.id.editTextUrl))
            .perform(typeText("https://example.com"))
        
        // Close keyboard
        closeSoftKeyboard()
        
        // Verify text was entered
        onView(withId(R.id.editTextUrl))
            .check(matches(withText("https://example.com")))
        
        // Verify buttons are still accessible after keyboard dismissal
        onView(withId(R.id.buttonProcess))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testPasteButton() {
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // Clear any existing text
        onView(withId(R.id.editTextUrl))
            .perform(clearText())
        
        // The paste button should be visible
        onView(withId(R.id.buttonPaste))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }
    
    @Test
    fun testNavigationFlow() {
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // Enter URL
        onView(withId(R.id.editTextUrl))
            .perform(replaceText("https://www.instagram.com/p/test123/?utm_source=test"))
        
        // Process URL
        onView(withId(R.id.buttonProcess))
            .perform(click())
        
        Thread.sleep(1000)
        
        // Verify processed URL is displayed
        onView(withId(R.id.textViewProcessedUrl))
            .check(matches(isDisplayed()))
        
        // Verify action buttons are enabled
        onView(withId(R.id.buttonCopy))
            .check(matches(isEnabled()))
        
        onView(withId(R.id.buttonShare))
            .check(matches(isEnabled()))
    }
    
    @Test
    fun testEmptyStateValidation() {
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // Clear any text
        onView(withId(R.id.editTextUrl))
            .perform(clearText())
        
        // Try to process empty URL
        onView(withId(R.id.buttonProcess))
            .perform(click())
        
        // Should show error or maintain empty state
        Thread.sleep(500)
        
        // Process button should still be visible
        onView(withId(R.id.buttonProcess))
            .check(matches(isDisplayed()))
    }
} 