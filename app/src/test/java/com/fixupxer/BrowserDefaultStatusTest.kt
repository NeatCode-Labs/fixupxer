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

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.Constants
import com.fixupxer.utils.DefaultBrowserStatus
import com.fixupxer.utils.BrowserViewGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BrowserDefaultStatusTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    @Config(sdk = [28])
    fun `legacy returns FIXUPXER when default resolves to self`() {
        val intent = browsableHttpIntent()
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = context.packageName
                name = "BrowserAlias"
            }
        }
        Shadows.shadowOf(context.packageManager).addResolveInfoForIntent(intent, resolveInfo)

        assertEquals(
            DefaultBrowserStatus.FIXUPXER,
            BrowserModeUtils.getDefaultBrowserStatus(context),
        )
    }

    @Test
    @Config(sdk = [28])
    fun `legacy returns OTHER_OR_UNSET when default resolves to another package`() {
        val intent = browsableHttpIntent()
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.example.otherbrowser"
                name = "OtherBrowserActivity"
            }
        }
        Shadows.shadowOf(context.packageManager).addResolveInfoForIntent(intent, resolveInfo)

        assertEquals(
            DefaultBrowserStatus.OTHER_OR_UNSET,
            BrowserModeUtils.getDefaultBrowserStatus(context),
        )
    }

    @Test
    @Config(sdk = [28])
    fun `legacy returns UNKNOWN when no default browser resolves`() {
        assertEquals(
            DefaultBrowserStatus.UNKNOWN,
            BrowserModeUtils.getDefaultBrowserStatus(context),
        )
    }

    @Test
    @Config(sdk = [28])
    fun `legacy returns UNKNOWN when resolver activity is system chooser`() {
        val intent = browsableHttpIntent()
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "android"
                name = "ResolverActivity"
            }
        }
        Shadows.shadowOf(context.packageManager).addResolveInfoForIntent(intent, resolveInfo)

        assertEquals(
            DefaultBrowserStatus.UNKNOWN,
            BrowserModeUtils.getDefaultBrowserStatus(context),
        )
    }

    @Test
    @Config(sdk = [33])
    fun `API 29 returns FIXUPXER when browser role is held`() {
        val roleManager = context.getSystemService(RoleManager::class.java)
        val shadowRoleManager = Shadows.shadowOf(roleManager)
        shadowRoleManager.addAvailableRole(RoleManager.ROLE_BROWSER)
        shadowRoleManager.addHeldRole(RoleManager.ROLE_BROWSER)

        assertEquals(
            DefaultBrowserStatus.FIXUPXER,
            BrowserModeUtils.getDefaultBrowserStatus(context),
        )
    }

    @Test
    @Config(sdk = [33])
    fun `API 29 returns OTHER_OR_UNSET when browser role is available but not held`() {
        val roleManager = context.getSystemService(RoleManager::class.java)
        val shadowRoleManager = Shadows.shadowOf(roleManager)
        shadowRoleManager.addAvailableRole(RoleManager.ROLE_BROWSER)

        assertEquals(
            DefaultBrowserStatus.OTHER_OR_UNSET,
            BrowserModeUtils.getDefaultBrowserStatus(context),
        )
    }

    @Test
    @Config(sdk = [33])
    fun `API 29 returns UNKNOWN when browser role is unavailable`() {
        assertEquals(
            DefaultBrowserStatus.UNKNOWN,
            BrowserModeUtils.getDefaultBrowserStatus(context),
        )
    }

    @Test
    @Config(sdk = [33])
    fun `startup reconciliation makes alias follow desired preference`() {
        val preferencesManager = PreferencesManager(context)
        try {
            preferencesManager.setBrowserModeEnabled(false)
            BrowserModeUtils.setBrowserAliasEnabled(context, true)

            org.junit.Assert.assertTrue(
                BrowserModeUtils.reconcileBrowserAlias(context, preferencesManager)
            )
            org.junit.Assert.assertFalse(BrowserModeUtils.isBrowserAliasEnabled(context))

            preferencesManager.setBrowserModeEnabled(true)
            org.junit.Assert.assertTrue(
                BrowserModeUtils.reconcileBrowserAlias(context, preferencesManager)
            )
            org.junit.Assert.assertTrue(BrowserModeUtils.isBrowserAliasEnabled(context))
        } finally {
            preferencesManager.setBrowserModeEnabled(false)
            BrowserModeUtils.setBrowserAliasEnabled(context, false)
        }
    }

    @Test
    @Config(sdk = [33])
    fun `startup alias failure leaves view gate closed`() {
        val preferencesManager = PreferencesManager(context)
        preferencesManager.setBrowserModeEnabled(true)
        val packageManager: PackageManager = mock()
        whenever(packageManager.getComponentEnabledSetting(any<ComponentName>()))
            .thenReturn(PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
        doThrow(SecurityException("blocked"))
            .whenever(packageManager)
            .setComponentEnabledSetting(any(), any(), any())
        val failingContext = object : ContextWrapper(context) {
            override fun getPackageManager(): PackageManager = packageManager
        }

        assertFalse(BrowserModeUtils.reconcileBrowserAlias(failingContext, preferencesManager))
        assertEquals(
            null,
            BrowserViewGate.begin(
                preferenceEnabled = preferencesManager.isBrowserModeEnabled(),
                aliasEnabled = BrowserModeUtils.isBrowserAliasEnabled(failingContext),
            ),
        )
        preferencesManager.setBrowserModeEnabled(false)
    }

    private fun browsableHttpIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(Constants.BROWSER_PROBE_URL))
            .addCategory(Intent.CATEGORY_BROWSABLE)
    }
}
