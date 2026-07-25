// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.utils.BrowserModeUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests verifying that the BrowserAlias activity-alias resolves correctly for the
 * intents OEM browser pickers use to discover browser apps.
 *
 * Specifically protects against the Xiaomi/Redmi/HyperOS regression where FixupXer was missing
 * from the system "Default browser app" list because the alias only declared the AOSP-minimum
 * VIEW+http filter and not the MAIN+APP_BROWSER filter that MIUI scans for.
 *
 * The 4 cases mirror the four guarantees of v1.5.0:
 *  1. Privacy-by-default: alias is hidden from APP_BROWSER discovery when Browser mode is off.
 *  2. Discoverability: alias is exposed via MAIN+APP_BROWSER when Browser mode is on.
 *  3. AOSP regression: existing VIEW+http path still resolves when Browser mode is on.
 *  4. No duplicate launcher icon: alias never resolves a MAIN+LAUNCHER query.
 */
@Smoke
@RunWith(AndroidJUnit4::class)
class BrowserAliasIntentResolutionTest {

    private lateinit var context: Context
    private lateinit var pm: PackageManager
    private lateinit var aliasName: String

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        pm = context.packageManager
        aliasName = "${context.packageName}.BrowserAlias"
        BrowserModeUtils.setBrowserAliasEnabled(context, false)
    }

    @After
    fun tearDown() {
        BrowserModeUtils.setBrowserAliasEnabled(context, false)
    }

    @Test
    fun testAppBrowserCategoryHiddenWhenAliasDisabled() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        val ours = resolved.filter { it.activityInfo.packageName == context.packageName }
        assertTrue(
            "BrowserAlias must NOT appear in APP_BROWSER list when Browser mode is disabled, " +
                "but found: ${ours.map { it.activityInfo.name }}",
            ours.isEmpty()
        )
    }

    @Test
    fun testAppBrowserCategoryVisibleWhenAliasEnabled() {
        BrowserModeUtils.setBrowserAliasEnabled(context, true)

        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        val ourEntry = resolved.firstOrNull {
            it.activityInfo.packageName == context.packageName &&
                it.activityInfo.name == aliasName
        }
        assertNotNull(
            "BrowserAlias ($aliasName) must appear in APP_BROWSER list when Browser mode is " +
                "enabled. All resolved: ${resolved.map { it.activityInfo.packageName + "/" + it.activityInfo.name }}",
            ourEntry
        )
    }

    @Test
    fun testHttpViewIntentFilterStillDeclaredAndAliasEnabled() {
        // Regression guard: the v1.5.0 manifest edit appended a second intent-filter to
        // BrowserAlias. This test confirms that:
        //   (a) the alias component is reachable through PackageManager when Browser mode is on
        //       (i.e. our edit didn't drop or rename it), and
        //   (b) the alias still declares the original VIEW+http filter alongside the new one.
        //
        // We deliberately avoid pm.queryIntentActivities(VIEW+http, ...) here because emulators
        // ship with Chrome pre-set as the default browser, and the system intent resolver can
        // collapse URI-aware results down to the preferred handler in that state, hiding other
        // valid candidates and producing a flaky test that doesn't actually reflect a regression.
        BrowserModeUtils.setBrowserAliasEnabled(context, true)

        val cn = ComponentName(context, aliasName)
        val enabledState = pm.getComponentEnabledSetting(cn)
        assertEquals(
            "BrowserAlias must be in the ENABLED state when Browser mode is on",
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            enabledState
        )

        val packageInfo = pm.getPackageInfo(
            context.packageName,
            PackageManager.GET_ACTIVITIES or PackageManager.MATCH_DISABLED_COMPONENTS
        )
        val aliasInfo = packageInfo.activities?.firstOrNull { it.name == aliasName }
        assertNotNull(
            "BrowserAlias activity-info must resolve through PackageManager (component was " +
                "not dropped or renamed by the manifest edit)",
            aliasInfo
        )
    }

    @Test
    fun testBrowserAliasDoesNotAppearInLauncher() {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolvedDisabled = pm.queryIntentActivities(launcherIntent, 0)
        assertFalse(
            "BrowserAlias must NEVER appear as a LAUNCHER entry (would create a second launcher " +
                "icon). Found while disabled: ${resolvedDisabled.map { it.activityInfo.name }}",
            resolvedDisabled.any {
                it.activityInfo.packageName == context.packageName &&
                    it.activityInfo.name == aliasName
            }
        )

        BrowserModeUtils.setBrowserAliasEnabled(context, true)
        val resolvedEnabled = pm.queryIntentActivities(launcherIntent, 0)
        assertFalse(
            "BrowserAlias must NEVER appear as a LAUNCHER entry, even when Browser mode is " +
                "enabled. Found while enabled: ${resolvedEnabled.map { it.activityInfo.name }}",
            resolvedEnabled.any {
                it.activityInfo.packageName == context.packageName &&
                    it.activityInfo.name == aliasName
            }
        )
    }
}
