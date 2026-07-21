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

import android.content.Context
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import com.fixupxer.utils.RetiredFrontendMigration
import com.fixupxer.utils.TikTokProxyStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RetiredFrontendMigrationPreferenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        ProxyRoster.reset()
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        ProxyRoster.reset()
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
    }

    @Test
    fun `init migrates retired frontend selections and disabled ids`() {
        seedRetiredPrefs()
        val manager = PreferencesManager(context)

        assertEquals(Constants.TOINSTAGRAM_DOMAIN, manager.getSelectedProxyDomain(ProxyPlatform.INSTAGRAM))
        assertNull(manager.getSelectedProxyDomain(ProxyPlatform.FACEBOOK))
        assertTrue(manager.getDisabledBuiltIns(ProxyPlatform.INSTAGRAM).isEmpty())
        assertFalse(manager.isConvertFacebookEnabled())
    }

    @Test
    fun `migration is idempotent`() {
        seedRetiredPrefs()
        val first = PreferencesManager(context)
        val second = PreferencesManager(context)

        assertEquals(
            first.getSelectedProxyDomain(ProxyPlatform.INSTAGRAM),
            second.getSelectedProxyDomain(ProxyPlatform.INSTAGRAM),
        )
        assertEquals(
            first.getSelectedProxyDomain(ProxyPlatform.FACEBOOK),
            second.getSelectedProxyDomain(ProxyPlatform.FACEBOOK),
        )
        assertEquals(
            first.getDisabledBuiltIns(ProxyPlatform.INSTAGRAM),
            second.getDisabledBuiltIns(ProxyPlatform.INSTAGRAM),
        )
        assertEquals(
            first.isConvertFacebookEnabled(),
            second.isConvertFacebookEnabled(),
        )
    }

    @Test
    fun `init preserves facebook conversion without a retired selection`() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CONVERT_FACEBOOK, true)
            .commit()

        val manager = PreferencesManager(context)

        assertTrue(manager.isConvertFacebookEnabled())
    }

    @Test
    fun `retired facebook selection keeps conversion enabled when a custom remains`() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FACEBOOK_PROXY, Constants.FACEBOOKEZ_DOMAIN)
            .putString(KEY_CUSTOM_FACEBOOK_PROXIES, LEGIT_FACEBOOK_CUSTOM)
            .putBoolean(KEY_CONVERT_FACEBOOK, true)
            .commit()

        val manager = PreferencesManager(context)

        assertEquals(
            LEGIT_FACEBOOK_CUSTOM,
            manager.getSelectedProxyDomain(ProxyPlatform.FACEBOOK),
        )
        assertTrue(manager.isConvertFacebookEnabled())
    }

    @Test
    fun `migration purges retired facebook custom and add blocks it`() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(
                KEY_CUSTOM_FACEBOOK_PROXIES,
                "${Constants.FACEBOOKEZ_DOMAIN},$LEGIT_FACEBOOK_CUSTOM",
            )
            .commit()

        val manager = PreferencesManager(context)

        assertEquals(
            listOf(LEGIT_FACEBOOK_CUSTOM),
            manager.getCustomProxies(ProxyPlatform.FACEBOOK),
        )
        assertFalse(
            ProxyRoster.allKnownDomains(ProxyPlatform.FACEBOOK)
                .contains(Constants.FACEBOOKEZ_DOMAIN),
        )

        manager.addCustomProxy(ProxyPlatform.FACEBOOK, Constants.FACEBOOKEZ_DOMAIN)

        assertEquals(
            listOf(LEGIT_FACEBOOK_CUSTOM),
            manager.getCustomProxies(ProxyPlatform.FACEBOOK),
        )
    }

    private fun seedRetiredPrefs() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_INSTAGRAM_PROXY, Constants.KKINSTAGRAM_DOMAIN)
            .putString(KEY_FACEBOOK_PROXY, Constants.FACEBOOKEZ_DOMAIN)
            .putString(
                KEY_DISABLED_INSTAGRAM,
                RetiredFrontendMigration.RETIRED_INSTAGRAM_DISABLED_ID,
            )
            .putBoolean(KEY_CONVERT_FACEBOOK, true)
            .commit()
    }

    private companion object {
        const val PREFS_NAME = "FixupXerPrefs"
        const val KEY_INSTAGRAM_PROXY = "instagram_proxy_domain"
        const val KEY_FACEBOOK_PROXY = "proxy_selection_facebook"
        const val KEY_DISABLED_INSTAGRAM = "disabled_builtin_proxies_instagram"
        const val KEY_CONVERT_FACEBOOK = "convert_facebook"
        const val KEY_CUSTOM_FACEBOOK_PROXIES = "custom_proxies_facebook"
        const val LEGIT_FACEBOOK_CUSTOM = "example-proxy.net"
    }
}
