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
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.Constants
import com.fixupxer.utils.DefaultBrowserStatus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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

    private fun browsableHttpIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(Constants.BROWSER_PROBE_URL))
            .addCategory(Intent.CATEGORY_BROWSABLE)
    }
}
