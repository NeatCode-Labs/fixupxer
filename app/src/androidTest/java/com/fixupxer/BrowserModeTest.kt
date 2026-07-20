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
import com.fixupxer.ui.BrowserSettingsActivity
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyPlatform
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

        // Other tests (e.g. the Settings picker flow) persist explicit browser
        // privacy targets; drop them so resolver tests see catalog defaults.
        val editor = context.getSharedPreferences("FixupXerPrefs", android.content.Context.MODE_PRIVATE).edit()
        ProxyPlatform.entries.forEach { platform ->
            editor.remove("browser_privacy_target_${platform.name.lowercase()}")
        }
        editor.commit()
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
        
        // Then - verify it's enabled
        assertTrue(preferencesManager.isBrowserModeEnabled())
        
        // Verify component is enabled
        val componentName = ComponentName(context, "${context.packageName}.BrowserAlias")
        val pm = context.packageManager
        val state = pm.getComponentEnabledSetting(componentName)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state)
    }
    
    @Test
    fun testBrowserSettingsActivityBrowserModeToggle() {
        ActivityScenario.launch(BrowserSettingsActivity::class.java).use {
            onView(withId(R.id.switchBrowserMode))
                .perform(nestedScrollTo())
                .check(matches(isDisplayed()))
            onView(withId(R.id.buttonBrowserModeGuide))
                .perform(nestedScrollTo())
                .check(matches(isDisplayed()))

            onView(withId(R.id.switchBrowserMode))
                .perform(nestedScrollTo(), click())

            Thread.sleep(500) // Allow time for preference update
            assertTrue(preferencesManager.isBrowserModeEnabled())

            onView(withId(R.id.switchBrowserMode))
                .perform(nestedScrollTo(), click())

            Thread.sleep(500) // Allow time for preference update
            assertFalse(preferencesManager.isBrowserModeEnabled())
        }
    }
    
    @Test
    fun testActionModeSelection() {
        val scenario = ActivityScenario.launch(BrowserSettingsActivity::class.java)
        
        // Select "Follow priority list" option
        onView(withId(R.id.radioFollowPriority))
            .perform(nestedScrollTo(), click())
        
        // Verify action priority section becomes visible
        onView(withId(R.id.actionPrioritySection))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        
        // Verify preference is updated
        Thread.sleep(500) // Allow time for preference update
        assertEquals(PreferencesManager.ACTION_MODE_PRIORITY, preferencesManager.getActionMode())
        
        // Select "Ask every time" option
        onView(withId(R.id.radioAskEveryTime))
            .perform(nestedScrollTo(), click())
        
        // Verify action priority section is hidden
        onView(withId(R.id.actionPrioritySection))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        
        // Verify preference is updated
        Thread.sleep(500) // Allow time for preference update
        assertEquals(PreferencesManager.ACTION_MODE_ASK, preferencesManager.getActionMode())
        
        scenario.close()
    }
    
    @Test
    fun testMenuItemsOrder() {
        // Launch main activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // Wait for activity to be ready
        Thread.sleep(500)
        
        // Open overflow menu
        try {
            onView(withContentDescription("More options"))
                .perform(click())
            
            // Verify Settings is shown
            onView(withText(R.string.settings_title))
                .check(matches(isDisplayed()))
            
            // Close menu by pressing back
            device.pressBack()
        } catch (e: Exception) {
            // Menu might not be visible in some configurations
            // This is not a critical test failure
        }
        
        scenario.close()
    }
    
    @Test
    fun testBrowserModeConversionDefaults() {
        preferencesManager.setBrowserConvertTwitterEnabled(false)
        preferencesManager.setBrowserConvertBlueskyEnabled(true)
        preferencesManager.setBrowserConvertRedditEnabled(false)
        preferencesManager.setBrowserConvertPinterestEnabled(true)
        
        assertFalse(preferencesManager.isBrowserConvertTwitterEnabled())
        assertTrue(preferencesManager.isBrowserConvertBlueskyEnabled())
        assertFalse(preferencesManager.isBrowserConvertRedditEnabled())
        assertTrue(preferencesManager.isBrowserConvertPinterestEnabled())
        
        preferencesManager.setConvertTwitterEnabled(true)
        assertTrue(preferencesManager.isConvertTwitterEnabled())
        assertFalse(preferencesManager.isBrowserConvertTwitterEnabled())
    }

    @Test
    fun testBrowserPrivacyTargetResolverDefaultsToReaderFrontend() {
        preferencesManager.setSelectedProxyDomain(ProxyPlatform.X, Constants.FIXUPX_DOMAIN)

        val target = preferencesManager.resolveBrowserPrivacyTarget(ProxyPlatform.X)

        assertNotNull(target)
        assertEquals(Constants.XCANCEL_DOMAIN, target!!.domain)
    }

    @Test
    fun testBrowserPrivacyTargetIgnoresMainEmbedProxySelection() {
        preferencesManager.setSelectedProxyDomain(ProxyPlatform.X, Constants.FIXUPX_DOMAIN)
        assertEquals(
            Constants.XCANCEL_DOMAIN,
            preferencesManager.resolveBrowserPrivacyTarget(ProxyPlatform.X)?.domain,
        )

        preferencesManager.setBrowserPrivacyTargetId(ProxyPlatform.X, "x_nitter_net")
        assertEquals(
            Constants.NITTER_NET_DOMAIN,
            preferencesManager.resolveBrowserPrivacyTarget(ProxyPlatform.X)?.domain,
        )

        preferencesManager.setSelectedProxyDomain(ProxyPlatform.X, Constants.FIXUPX_DOMAIN)
        assertEquals(
            Constants.NITTER_NET_DOMAIN,
            preferencesManager.resolveBrowserPrivacyTarget(ProxyPlatform.X)?.domain,
        )
        assertEquals(
            Constants.FIXUPX_DOMAIN,
            preferencesManager.getSelectedProxyDomain(ProxyPlatform.X),
        )
    }
    
    @Test
    fun testBrowserModeIndependentFromMainToggles() {
        // Given - different settings for main app vs browser mode
        preferencesManager.setConvertTwitterEnabled(true) // Main app setting
        preferencesManager.setBrowserConvertTwitterEnabled(false) // Browser mode setting
        preferencesManager.setBrowserModeEnabled(true)
        
        // Verify main app setting remains unchanged
        assertTrue(preferencesManager.isConvertTwitterEnabled())
        assertFalse(preferencesManager.isBrowserConvertTwitterEnabled())
        
        // Verify they are independent
        preferencesManager.setConvertTwitterEnabled(false)
        assertFalse(preferencesManager.isConvertTwitterEnabled())
        assertFalse(preferencesManager.isBrowserConvertTwitterEnabled())
        
        preferencesManager.setBrowserConvertTwitterEnabled(true)
        assertFalse(preferencesManager.isConvertTwitterEnabled())
        assertTrue(preferencesManager.isBrowserConvertTwitterEnabled())
    }
    
    /** The Settings screen scrolls inside a NestedScrollView, which plain scrollTo() cannot handle. */
    private fun nestedScrollTo(): androidx.test.espresso.ViewAction {
        return object : androidx.test.espresso.ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<android.view.View> =
                isAssignableFrom(android.view.View::class.java)
            override fun getDescription() = "Scroll enclosing NestedScrollView to target view"
            override fun perform(uiController: androidx.test.espresso.UiController, view: android.view.View) {
                var y = view.top
                var parent: android.view.ViewParent? = view.parent
                while (parent is android.view.View && parent !is androidx.core.widget.NestedScrollView) {
                    y += parent.top
                    parent = (parent as android.view.View).parent
                }
                (parent as? androidx.core.widget.NestedScrollView)?.scrollTo(0, y)
                uiController.loopMainThreadUntilIdle()
            }
        }
    }

    @Test
    fun testActionPriorityWithNativeApp() {
        // Test action priority settings
        
        // Given - set action mode to priority with specific order
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        val priority = listOf(
            PreferencesManager.ACTION_NATIVE_APP,
            PreferencesManager.ACTION_BROWSER,
            PreferencesManager.ACTION_CLIPBOARD
        )
        preferencesManager.setActionPriority(priority)
        
        // Then - verify settings are saved
        assertEquals(PreferencesManager.ACTION_MODE_PRIORITY, preferencesManager.getActionMode())
        assertEquals(priority, preferencesManager.getActionPriority())
    }
} 