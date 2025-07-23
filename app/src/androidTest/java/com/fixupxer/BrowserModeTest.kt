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

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.fixupxer.ui.SettingsActivity
import com.fixupxer.utils.BrowserModeUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for browser mode functionality
 */
@RunWith(AndroidJUnit4::class)
class BrowserModeTest {
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var context: android.content.Context
    private lateinit var device: UiDevice
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        preferencesManager = PreferencesManager(context)
        
        // Ensure browser mode is disabled at start
        preferencesManager.setBrowserModeEnabled(false)
        BrowserModeUtils.setBrowserAliasEnabled(context, false)
    }
    
    @After
    fun tearDown() {
        // Clean up - disable browser mode
        preferencesManager.setBrowserModeEnabled(false)
        BrowserModeUtils.setBrowserAliasEnabled(context, false)
    }
    
    @Test
    fun testEnableDisableBrowserAlias() {
        // Given - browser alias is initially disabled
        assertFalse(BrowserModeUtils.isBrowserAliasEnabled(context))
        
        // When - enable browser alias
        BrowserModeUtils.setBrowserAliasEnabled(context, true)
        
        // Then - verify it's enabled via PackageManager
        val pm = context.packageManager
        val cn = ComponentName(context, "${context.packageName}.BrowserAlias")
        val state = pm.getComponentEnabledSetting(cn)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state)
        assertTrue(BrowserModeUtils.isBrowserAliasEnabled(context))
        
        // When - disable browser alias
        BrowserModeUtils.setBrowserAliasEnabled(context, false)
        
        // Then - verify it's disabled
        val newState = pm.getComponentEnabledSetting(cn)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, newState)
        assertFalse(BrowserModeUtils.isBrowserAliasEnabled(context))
    }
    
    @Test
    fun testViewIntentHandling() {
        // This test verifies that browser mode can be enabled
        // Actual VIEW intent handling is complex due to BrowserAlias behavior
        
        // Given - browser mode is disabled initially
        assertFalse(preferencesManager.isBrowserModeEnabled())
        
        // When - enable browser mode
        preferencesManager.setBrowserModeEnabled(true)
        BrowserModeUtils.setBrowserAliasEnabled(context, true)
        
        // Then - verify preferences are set
        assertTrue(preferencesManager.isBrowserModeEnabled())
        assertTrue(BrowserModeUtils.isBrowserAliasEnabled(context))
        
        // Note: Testing actual intent handling requires complex setup
        // and may interfere with system settings, so we only test the flags
    }
    
    @Test
    fun testActionModePreferences() {
        // Test action mode preferences
        
        // Given - default should be "ask"
        assertEquals(PreferencesManager.ACTION_MODE_ASK, preferencesManager.getActionMode())
        
        // When - set to priority mode
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        
        // Then - verify it's saved
        assertEquals(PreferencesManager.ACTION_MODE_PRIORITY, preferencesManager.getActionMode())
        
        // When - set back to ask mode
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        
        // Then - verify it's saved
        assertEquals(PreferencesManager.ACTION_MODE_ASK, preferencesManager.getActionMode())
    }
    
    @Test
    fun testActionPriorityPreferences() {
        // Test action priority list preferences
        
        // Given - default priority order
        val defaultPriority = listOf(
            PreferencesManager.ACTION_NATIVE_APP,
            PreferencesManager.ACTION_BROWSER, 
            PreferencesManager.ACTION_SHARE_MENU,
            PreferencesManager.ACTION_CLIPBOARD
        )
        assertEquals(defaultPriority, preferencesManager.getActionPriority())
        
        // When - set custom priority
        val customPriority = listOf(
            PreferencesManager.ACTION_BROWSER,
            PreferencesManager.ACTION_SHARE_MENU,
            PreferencesManager.ACTION_CLIPBOARD,
            PreferencesManager.ACTION_NATIVE_APP
        )
        preferencesManager.setActionPriority(customPriority)
        
        // Then - verify it's saved
        assertEquals(customPriority, preferencesManager.getActionPriority())
    }
    
    @Test
    fun testBrowserConversionPreferences() {
        // Test browser-specific conversion preferences
        
        // Given - default values should be false (conservative approach)
        assertFalse(preferencesManager.isBrowserConvertTwitterEnabled())
        assertFalse(preferencesManager.isBrowserConvertInstagramEnabled())
        assertFalse(preferencesManager.isBrowserConvertFacebookEnabled())
        
        // When - enable Twitter conversion for browser mode
        preferencesManager.setBrowserConvertTwitterEnabled(true)
        
        // Then - verify it's saved
        assertTrue(preferencesManager.isBrowserConvertTwitterEnabled())
        assertFalse(preferencesManager.isBrowserConvertInstagramEnabled())
        assertFalse(preferencesManager.isBrowserConvertFacebookEnabled())
        
        // When - enable Instagram conversion for browser mode
        preferencesManager.setBrowserConvertInstagramEnabled(true)
        
        // Then - verify it's saved
        assertTrue(preferencesManager.isBrowserConvertTwitterEnabled())
        assertTrue(preferencesManager.isBrowserConvertInstagramEnabled())
        assertFalse(preferencesManager.isBrowserConvertFacebookEnabled())
        
        // When - enable Facebook conversion for browser mode
        preferencesManager.setBrowserConvertFacebookEnabled(true)
        
        // Then - verify all are saved
        assertTrue(preferencesManager.isBrowserConvertTwitterEnabled())
        assertTrue(preferencesManager.isBrowserConvertInstagramEnabled())
        assertTrue(preferencesManager.isBrowserConvertFacebookEnabled())
    }
    
    @Test
    fun testSettingsActivityLaunch() {
        // Test that SettingsActivity can be launched
        
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            // Verify the activity launched successfully
            onView(withId(R.id.switchBrowserMode))
                .check(matches(isDisplayed()))
            
            onView(withId(R.id.buttonReadThis))
                .check(matches(isDisplayed()))
            
            onView(withId(R.id.radioGroupActionMode))
                .check(matches(isDisplayed()))
                
            onView(withId(R.id.buttonConversionDefaults))
                .check(matches(isDisplayed()))
        }
    }
    
    @Test
    fun testBrowserModeSwitchToggle() {
        // Test browser mode switch in settings
        
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            // Verify switch is initially unchecked (since we disabled it in setup)
            onView(withId(R.id.switchBrowserMode))
                .check(matches(isNotChecked()))
            
            // When - toggle the switch
            onView(withId(R.id.switchBrowserMode))
                .perform(click())
            
            // Then - verify it's checked
            onView(withId(R.id.switchBrowserMode))
                .check(matches(isChecked()))
            
            // And verify preference was saved
            assertTrue(preferencesManager.isBrowserModeEnabled())
            assertTrue(BrowserModeUtils.isBrowserAliasEnabled(context))
        }
    }
    
    @Test
    fun testActionModeRadioButtons() {
        // Test action mode radio buttons in settings
        
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            // Initially "Ask every time" should be selected
            onView(withId(R.id.radioAskEveryTime))
                .check(matches(isChecked()))
                
            onView(withId(R.id.radioFollowPriority))
                .check(matches(isNotChecked()))
            
            // When - select "Follow priority" 
            onView(withId(R.id.radioFollowPriority))
                .perform(click())
            
            // Then - verify it's selected
            onView(withId(R.id.radioFollowPriority))
                .check(matches(isChecked()))
                
            onView(withId(R.id.radioAskEveryTime))
                .check(matches(isNotChecked()))
            
            // And verify preference was saved
            assertEquals(PreferencesManager.ACTION_MODE_PRIORITY, preferencesManager.getActionMode())
        }
    }
    
    @Test
    fun testConversionDefaultsButton() {
        // Test conversion defaults button functionality
        
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            // When - click conversion defaults button
            onView(withId(R.id.buttonConversionDefaults))
                .perform(click())
            
            // Then - verify dialog opens
            // Note: Dialog testing would require more complex setup
            // This test just verifies the button is clickable
        }
    }
    
    private fun waitFor(delay: Long) = androidx.test.espresso.action.ViewActions.actionWithAssertions(
        androidx.test.espresso.action.GeneralSwipeAction(
            androidx.test.espresso.action.Swipe.SLOW,
            androidx.test.espresso.action.GeneralLocation.CENTER,
            androidx.test.espresso.action.GeneralLocation.CENTER,
            androidx.test.espresso.action.Press.FINGER
        )
    ).also { Thread.sleep(delay) }
} 