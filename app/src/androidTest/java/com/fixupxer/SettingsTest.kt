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
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.MainActivity
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import com.fixupxer.ui.SettingsActivity
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyPlatform
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import android.view.View
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.preference.PreferenceManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.core.widget.NestedScrollView
import org.hamcrest.Matcher
import android.view.ViewParent

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
            .perform(click())
        
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
            .perform(click())
        
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
            .perform(click())
        
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
            .perform(click())
        
        // Wait for dialog
        onView(isRoot()).perform(waitFor(1000))
        
        // Click max entries button
        onView(withId(R.id.btnMaxEntries))
            .inRoot(isDialog())
            .perform(click())
        
        // Wait for dialog
        onView(isRoot()).perform(waitFor(1000))
        
        // Clear and enter invalid value (too low); close the keyboard so the
        // IME can't steal window focus when the dialog later dismisses.
        onView(withId(R.id.editTextMaxEntries))
            .inRoot(isDialog())
            .perform(clearText(), typeText("0"), closeSoftKeyboard())
        
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
            .perform(clearText(), typeText("50"), closeSoftKeyboard())
        
        // Confirm
        onView(withId(R.id.buttonOk))
            .inRoot(isDialog())
            .perform(click())
        
        // Wait for dialog to close
        onView(isRoot()).perform(waitFor(1000))
        
        // Reopen the compact setting and verify the saved value
        onView(withId(R.id.btnMaxEntries))
            .inRoot(isDialog())
            .perform(click())
        onView(withId(R.id.editTextMaxEntries))
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
                .perform(click())
            
            // Verify empty state message when history is disabled
            onView(withText(containsString("Enable history")))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }
    
    @Test
    fun testConversionDefaultsDialog() {
        ActivityScenario.launch(SettingsActivity::class.java)
        
        onView(withId(R.id.buttonConversionDefaults))
            .perform(nestedScrollTo(), click())
        
        onView(isRoot()).perform(waitFor(1000))
        
        onView(withText(R.string.conversion_defaults_title))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        
        onView(withText(containsString("privacy readers")))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        
        // Every supported row shows a target line; anchor on the X row to avoid ambiguity.
        onView(
            allOf(
                withId(R.id.textViewPrivacyTargetStatus),
                hasSibling(withText(R.string.convert_twitter_browser)),
            )
        )
            .inRoot(isDialog())
            .check(matches(allOf(isDisplayed(), withText(containsString("Privacy frontend:")))))

        // Exactly four supported platforms (X, Bluesky, Reddit, Pinterest).
        onView(withId(R.id.switchBrowserTwitter))
            .inRoot(isDialog())
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.switchBrowserBluesky))
            .inRoot(isDialog())
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.switchBrowserReddit))
            .inRoot(isDialog())
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withId(R.id.switchBrowserPinterest))
            .inRoot(isDialog())
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testConversionDefaultsButtonsVisibleWhenBrowserModeOff() {
        ActivityScenario.launch(SettingsActivity::class.java)

        onView(withId(R.id.switchBrowserMode))
            .check(matches(isNotChecked()))

        onView(withId(R.id.buttonConversionDefaults))
            .perform(nestedScrollTo())
            .check(matches(allOf(isDisplayed(), isClickable())))

        onView(withId(R.id.buttonConfigurationStatus))
            .perform(nestedScrollTo())
            .check(matches(allOf(isDisplayed(), isClickable())))
    }

    @Test
    fun testConfigurationStatusDialog() {
        ActivityScenario.launch(SettingsActivity::class.java)

        onView(withId(R.id.buttonConfigurationStatus))
            .perform(nestedScrollTo(), click())

        onView(isRoot()).perform(waitFor(1000))

        onView(withText(R.string.configuration_status_title))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))

        onView(withText(R.string.configuration_status_detail_browser_off))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testConversionDefaultsRestoreReaderFlow() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .putBoolean("browser_convert_twitter", true)
                .commit()
            delay(100)
            AlternativeFrontendCatalog.builtInReaders(ProxyPlatform.X).forEach { reader ->
                PreferencesManager(context).disableBuiltIn(ProxyPlatform.X, reader.id)
            }
            delay(100)
        }

        ActivityScenario.launch(SettingsActivity::class.java)

        onView(withId(R.id.buttonConversionDefaults))
            .perform(nestedScrollTo(), click())

        onView(isRoot()).perform(waitFor(1000))

        onView(
            allOf(
                withId(R.id.textViewChangePrivacyTarget),
                hasSibling(withText(R.string.convert_twitter_browser)),
            )
        )
            .inRoot(isDialog())
            .perform(click())

        onView(isRoot()).perform(waitFor(500))

        onView(allOf(withText(R.string.proxy_action_restore_builtins), isDisplayed()))
            .perform(click())

        onView(isRoot()).perform(waitFor(500))

        onView(allOf(withText(containsString(Constants.NITTER_NET_DOMAIN)), isDisplayed()))
            .perform(click())

        onView(isRoot()).perform(waitFor(500))

        onView(withId(R.id.btnSave))
            .inRoot(isDialog())
            .perform(click())

        onView(isRoot()).perform(waitFor(300))

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
        assertEquals("x_nitter_net", prefs.getString("browser_privacy_target_x", null))
        assertEquals(true, prefs.getBoolean("browser_convert_twitter", false))
    }

    @Test
    fun testConversionDefaultsRestoreCancelRollsBackRoster() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .putBoolean("browser_convert_twitter", true)
                .commit()
            delay(100)
            AlternativeFrontendCatalog.builtInReaders(ProxyPlatform.X).forEach { reader ->
                PreferencesManager(context).disableBuiltIn(ProxyPlatform.X, reader.id)
            }
            delay(100)
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val initialDisabled = PreferencesManager(context).getDisabledBuiltIns(ProxyPlatform.X)

        ActivityScenario.launch(SettingsActivity::class.java)

        onView(withId(R.id.buttonConversionDefaults))
            .perform(nestedScrollTo(), click())

        onView(isRoot()).perform(waitFor(1000))

        onView(
            allOf(
                withId(R.id.textViewChangePrivacyTarget),
                hasSibling(withText(R.string.convert_twitter_browser)),
            )
        )
            .inRoot(isDialog())
            .perform(click())

        onView(isRoot()).perform(waitFor(500))

        onView(allOf(withText(R.string.proxy_action_restore_builtins), isDisplayed()))
            .perform(click())

        onView(isRoot()).perform(waitFor(500))

        // Close the picker without choosing a reader, then cancel the outer dialog:
        // the unsaved roster restore must be rolled back completely.
        pressBack()
        onView(isRoot()).perform(waitFor(500))

        onView(withId(R.id.btnCancel))
            .inRoot(isDialog())
            .perform(click())

        onView(isRoot()).perform(waitFor(300))

        assertEquals(
            initialDisabled,
            PreferencesManager(context).getDisabledBuiltIns(ProxyPlatform.X),
        )
    }

    @Test
    fun testConversionDefaultsPrivacyTargetPickerSave() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .putBoolean("browser_convert_twitter", true)
                .commit()
            delay(100)
        }

        ActivityScenario.launch(SettingsActivity::class.java)

        onView(withId(R.id.buttonConversionDefaults))
            .perform(nestedScrollTo(), click())

        onView(isRoot()).perform(waitFor(1000))

        onView(
            allOf(
                withId(R.id.textViewChangePrivacyTarget),
                hasSibling(withText(R.string.convert_twitter_browser)),
            )
        )
            .inRoot(isDialog())
            .perform(click())

        onView(isRoot()).perform(waitFor(500))

        // Selection-only privacy picker: no embed/automatic targets, no section
        // header for embeds and no management actions may be present.
        onView(withText(containsString(Constants.FIXUPX_DOMAIN))).check(doesNotExist())
        onView(withText(containsString(Constants.TWIIIT_DOMAIN))).check(doesNotExist())
        onView(withText(R.string.proxy_section_embed)).check(doesNotExist())
        // Empty-state Add custom button is always in the hierarchy but must stay hidden.
        onView(withText(R.string.proxy_action_add_custom)).check(matches(not(isDisplayed())))
        onView(withText(R.string.proxy_action_edit)).check(doesNotExist())

        onView(allOf(withText(containsString(Constants.NITTER_NET_DOMAIN)), isDisplayed()))
            .perform(click())

        onView(isRoot()).perform(waitFor(500))

        onView(withId(R.id.btnSave))
            .inRoot(isDialog())
            .perform(click())

        onView(isRoot()).perform(waitFor(300))

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
        assertEquals("x_nitter_net", prefs.getString("browser_privacy_target_x", null))
    }
    
    @Test
    fun testConversionDefaultsToggleSaving() {
        runBlocking {
            // Set initial states
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val prefs = context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("browser_convert_twitter", true)
                .putBoolean("browser_convert_bluesky", true)
                .putBoolean("browser_convert_reddit", true)
                .putBoolean("browser_convert_pinterest", true)
                .commit()
            
            delay(100)
        }
        
        ActivityScenario.launch(SettingsActivity::class.java)
        
        // Open conversion defaults dialog
        onView(withId(R.id.buttonConversionDefaults))
            .perform(nestedScrollTo(), click())
        
        // Wait for dialog
        onView(isRoot()).perform(waitFor(1000))
        
        // Toggle Twitter conversion off
        onView(withId(R.id.switchBrowserTwitter))
            .inRoot(isDialog())
            .perform(scrollTo(), click())
        
        onView(withId(R.id.switchBrowserBluesky))
            .inRoot(isDialog())
            .perform(scrollTo(), click())

        onView(withId(R.id.switchBrowserReddit))
            .inRoot(isDialog())
            .perform(scrollTo(), click())
        
        // Save
        onView(withId(R.id.btnSave))
            .inRoot(isDialog())
            .perform(click())
        
        // Verify dialog is dismissed
        onView(withText(R.string.conversion_defaults_title))
            .check(doesNotExist())
        
        // Reopen dialog to verify changes were saved
        onView(withId(R.id.buttonConversionDefaults))
            .perform(nestedScrollTo(), click())
        
        // Verify toggles are still off
        onView(withId(R.id.switchBrowserTwitter))
            .inRoot(isDialog())
            .perform(scrollTo())
            .check(matches(isNotChecked()))
        
        onView(withId(R.id.switchBrowserBluesky))
            .inRoot(isDialog())
            .perform(scrollTo())
            .check(matches(isNotChecked()))

        onView(withId(R.id.switchBrowserReddit))
            .inRoot(isDialog())
            .perform(scrollTo())
            .check(matches(isNotChecked()))
    }
    
    @Test
    fun testConversionDefaultsCancel() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val prefs = context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("browser_convert_twitter", true)
                .putBoolean("browser_convert_bluesky", true)
                .putBoolean("browser_convert_reddit", true)
                .putBoolean("browser_convert_pinterest", true)
                .commit()
            
            delay(100)
        }
        
        ActivityScenario.launch(SettingsActivity::class.java)
        
        // Open conversion defaults dialog
        onView(withId(R.id.buttonConversionDefaults))
            .perform(nestedScrollTo(), click())
        
        // Wait for dialog
        onView(isRoot()).perform(waitFor(1000))
        
        onView(withId(R.id.switchBrowserPinterest))
            .inRoot(isDialog())
            .perform(scrollTo(), click())
        
        // Cancel
        onView(withId(R.id.btnCancel))
            .inRoot(isDialog())
            .perform(click())
        
        // Verify dialog is dismissed
        onView(withText(R.string.conversion_defaults_title))
            .check(doesNotExist())
        
        // Reopen dialog
        onView(withId(R.id.buttonConversionDefaults))
            .perform(nestedScrollTo(), click())
        
        onView(withId(R.id.switchBrowserPinterest))
            .inRoot(isDialog())
            .perform(scrollTo())
            .check(matches(isChecked()))
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

    private fun nestedScrollTo(): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(View::class.java)
            override fun getDescription() = "Scroll enclosing NestedScrollView to target view"
            override fun perform(uiController: UiController, view: View) {
                var y = view.top
                var parent: ViewParent? = view.parent
                while (parent is View && parent !is NestedScrollView) {
                    y += parent.top
                    parent = (parent as View).parent
                }
                (parent as? NestedScrollView)?.scrollTo(0, y)
                uiController.loopMainThreadUntilIdle()
            }
        }
    }
} 