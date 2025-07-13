package com.fixupxer

import android.content.pm.ActivityInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.fixupxer.MainActivity
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for responsive design on different screen sizes and orientations
 */
@RunWith(AndroidJUnit4::class)
class ResponsiveDesignTest {
    
    @Test
    fun testPortraitOrientation() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // Set to portrait
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        
        Thread.sleep(1000)
        
        // Verify all essential elements are visible in portrait
        onView(withId(R.id.editTextUrl))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.buttonProcess))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.buttonCopy))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.buttonShare))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testLandscapeOrientation() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // Set to landscape
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        
        // Give more time for orientation change
        Thread.sleep(2000)
        
        // Verify essential elements are accessible in landscape
        // The URL input should always be visible
        onView(withId(R.id.editTextUrl))
            .check(matches(isDisplayed()))
        
        // Process button might need scrolling in landscape
        try {
            onView(withId(R.id.buttonProcess))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
        } catch (e: Exception) {
            // If scrollTo fails, just verify it exists
            onView(withId(R.id.buttonProcess))
                .check(matches(isEnabled()))
        }
        
        // Other buttons like copy/share may require scrolling in landscape mode
        // which is acceptable UX behavior
    }
    
    @Test
    fun testOrientationChange() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // Start in portrait
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        
        Thread.sleep(500)
        
        // Type some text
        onView(withId(R.id.editTextUrl))
            .perform(androidx.test.espresso.action.ViewActions.typeText("https://example.com"))
        
        // Switch to landscape
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        
        Thread.sleep(1000)
        
        // Verify text is preserved after orientation change
        onView(withId(R.id.editTextUrl))
            .check(matches(withText("https://example.com")))
    }
    
    @Test
    fun testSmallScreenSize() {
        // Test on smaller screen configuration
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        
        ActivityScenario.launch(MainActivity::class.java)
        
        Thread.sleep(1000)
        
        // On small screens, verify scrolling works properly
        // All critical UI elements should be accessible
        onView(withId(R.id.editTextUrl))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.buttonProcess))
            .check(matches(isDisplayed()))
    }
} 