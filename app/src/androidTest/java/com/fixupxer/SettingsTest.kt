package com.fixupxer

import android.content.Intent
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
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import android.view.View
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.preference.PreferenceManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

/**
 * Settings related UI tests
 */
@RunWith(AndroidJUnit4::class)
class SettingsTest {
    
    private fun launchMainActivity() {
        ActivityScenario.launch(MainActivity::class.java)
    }
    
    @Test
    fun testAboutDialog() {
        launchMainActivity()
        
        // Open overflow menu
        onView(withContentDescription("More options")).perform(click())
        
        // Wait for menu to appear
        onView(isRoot()).perform(waitFor(500))
        
        // Click About menu item
        onView(withText("About")).perform(click())
        
        // Wait for dialog
        onView(isRoot()).perform(waitFor(1000))
        
        // Verify About dialog is shown by checking the title
        onView(withText("About FixupXer"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        
        // Verify version info is displayed
        onView(withText(containsString("Version")))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testHistoryToggleInSettings() {
        launchMainActivity()
        
        // Open history dialog first to access settings
        onView(withId(R.id.buttonHistory))
            .perform(scrollTo(), click())
        
        // Wait for dialog
        onView(isRoot()).perform(waitFor(1000))
        
        // Toggle history off
        onView(withId(R.id.switchHistoryEnabled))
            .inRoot(isDialog())
            .perform(click())
        
        // Wait a bit
        onView(isRoot()).perform(waitFor(500))
        
        // Toggle history back on
        onView(withId(R.id.switchHistoryEnabled))
            .inRoot(isDialog())
            .perform(click())
        
        // Verify it's checked
        onView(withId(R.id.switchHistoryEnabled))
            .inRoot(isDialog())
            .check(matches(isChecked()))
    }
    
    @Test
    fun testMaxHistoryEntriesSetting() {
        launchMainActivity()
        
        // First open history dialog
        onView(withId(R.id.buttonHistory))
            .perform(scrollTo(), click())
        
        // Wait for history dialog
        onView(isRoot()).perform(waitFor(1000))
        
        // Click max entries button in history dialog
        onView(withId(R.id.btnMaxEntries))
            .inRoot(isDialog())
            .perform(click())
        
        // Wait for dialog
        onView(isRoot()).perform(waitFor(1000))
        
        // Verify dialog is shown
        onView(withText("Select max entries"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        
        // Verify input field exists
        onView(withId(R.id.editTextMaxEntries))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testBackNavigation() {
        launchMainActivity()
        
        // Open history dialog
        onView(withId(R.id.buttonHistory))
            .perform(scrollTo(), click())
        
        // Wait for dialog
        onView(isRoot()).perform(waitFor(1000))
        
        // Click close button
        onView(withId(R.id.btnClose))
            .inRoot(isDialog())
            .perform(click())
        
        // Verify we're back to main activity
        onView(withId(R.id.buttonProcess))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testMaxEntriesValidation() {
        launchMainActivity()
        
        // Open history dialog
        onView(withId(R.id.buttonHistory))
            .perform(scrollTo(), click())
        
        // Wait for dialog
        onView(isRoot()).perform(waitFor(1000))
        
        // Click max entries button
        onView(withId(R.id.btnMaxEntries))
            .inRoot(isDialog())
            .perform(click())
        
        // Wait for dialog
        onView(isRoot()).perform(waitFor(1000))
        
        // Clear and enter invalid value (too low)
        onView(withId(R.id.editTextMaxEntries))
            .inRoot(isDialog())
            .perform(clearText(), typeText("0"))
        
        // Try to confirm
        onView(withId(R.id.buttonOk))
            .inRoot(isDialog())
            .perform(click())
        
        // Wait a bit
        onView(isRoot()).perform(waitFor(500))
        
        // Dialog should still be open due to validation
        onView(withId(R.id.editTextMaxEntries))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        
        // Enter valid value
        onView(withId(R.id.editTextMaxEntries))
            .inRoot(isDialog())
            .perform(clearText(), typeText("50"))
        
        // Confirm
        onView(withId(R.id.buttonOk))
            .inRoot(isDialog())
            .perform(click())
        
        // Wait for dialog to close
        onView(isRoot()).perform(waitFor(1000))
        
        // Verify value was updated in history dialog
        onView(withId(R.id.btnMaxEntries))
            .inRoot(isDialog())
            .check(matches(withText("50")))
    }
    
    @Test
    fun testHistoryDisabledStopsRecording() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            
            // Ensure history is disabled
            prefs.edit().putBoolean("history_enabled", false).commit()
            
            // Launch main activity
            launchMainActivity()
            
            // Process a URL
            onView(withId(R.id.editTextUrl))
                .perform(replaceText("https://www.instagram.com/p/test123/"))
            onView(withId(R.id.buttonProcess)).perform(click())
            
            // Wait for processing
            onView(isRoot()).perform(waitFor(1000))
            
            // Open history dialog
            onView(withId(R.id.buttonHistory))
                .perform(scrollTo(), click())
            
            // Verify empty state message when history is disabled
            onView(withText(containsString("Enable history")))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }
    
    // Helper function to wait
    private fun waitFor(millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints() = isRoot()
            override fun getDescription() = "Wait for $millis milliseconds"
            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        }
    }
} 