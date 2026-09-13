// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.fixupxer

import android.content.Context
import com.fixupxer.processing.BrowserConversionMode
import com.fixupxer.processing.BrowserFrontendPolicy
import com.fixupxer.processing.BrowserFrontendPreference
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import com.fixupxer.utils.TikTokProxyStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BrowserFrontendPreferencesTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication().applicationContext
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        ProxyRoster.reset()
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        ProxyRoster.reset()
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
    }

    @Test
    fun `legacy reader migration writes all platforms and preserves disabled target id`() {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        raw.edit()
            .putBoolean("browser_convert_twitter", true)
            .putString("browser_privacy_target_x", "x_xcancel")
            .putString("disabled_builtin_proxies_x", "x_xcancel")
            .putBoolean("browser_convert_bluesky", false)
            .putString("browser_privacy_target_bluesky", "bs_skylib_coffee")
            .putBoolean("browser_convert_tiktok", true)
            .commit()

        val manager = PreferencesManager(context)
        val preferences = manager.getBrowserFrontendPreferences()

        assertEquals(
            BrowserFrontendPreference(BrowserConversionMode.READER, "x_xcancel"),
            preferences[ProxyPlatform.X],
        )
        assertEquals(
            BrowserFrontendPreference.CLEAN_ONLY,
            preferences[ProxyPlatform.TIKTOK],
        )
        assertEquals(
            BrowserFrontendPreference(
                BrowserConversionMode.CLEAN_ONLY,
                "bs_skylib_coffee",
            ),
            preferences[ProxyPlatform.BLUESKY],
        )
        assertEquals(ProxyPlatform.entries.toSet(), preferences.keys)
        assertTrue(raw.getBoolean("browser_frontend_preferences_v2_migrated", false))
        ProxyPlatform.entries.forEach { platform ->
            assertTrue(raw.contains("browser_frontend_mode_${platform.name.lowercase()}"))
        }
    }

    @Test
    fun `atomic save rejects a stale touched field without overwriting newer choice`() {
        val manager = PreferencesManager(context)
        val stale = manager.getBrowserFrontendPreferences()
        val newer = BrowserFrontendPreference(BrowserConversionMode.EMBED, "x_fixupx")
        assertTrue(manager.saveBrowserFrontendPreferences(mapOf(ProxyPlatform.X to newer), stale))

        val staleWrite = BrowserFrontendPreference(BrowserConversionMode.READER, "x_xcancel")
        assertFalse(
            manager.saveBrowserFrontendPreferences(mapOf(ProxyPlatform.X to staleWrite), stale),
        )
        assertEquals(newer, manager.getBrowserFrontendPreferences()[ProxyPlatform.X])
    }

    @Test
    fun `custom deletion clears its browser choice in the same persisted update`() {
        val manager = PreferencesManager(context)
        manager.addCustomProxy(ProxyPlatform.FACEBOOK, "reader.example")
        val expected = manager.getBrowserFrontendPreferences()
        assertTrue(
            manager.saveBrowserFrontendPreferences(
                mapOf(
                    ProxyPlatform.FACEBOOK to BrowserFrontendPreference(
                        BrowserConversionMode.CUSTOM,
                        "custom:reader.example",
                    )
                ),
                expected,
            )
        )

        manager.removeCustomProxy(ProxyPlatform.FACEBOOK, "reader.example")

        assertEquals(emptyList<String>(), manager.getCustomProxies(ProxyPlatform.FACEBOOK))
        assertEquals(
            BrowserFrontendPreference.CLEAN_ONLY,
            manager.getBrowserFrontendPreferences()[ProxyPlatform.FACEBOOK],
        )
        val reloaded = PreferencesManager(context)
        assertEquals(
            BrowserFrontendPreference.CLEAN_ONLY,
            reloaded.getBrowserFrontendPreferences()[ProxyPlatform.FACEBOOK],
        )
    }

    @Test
    fun `reader fallback does not overwrite stored disabled reader id`() {
        val manager = PreferencesManager(context)
        val original = manager.getBrowserFrontendPreferences()
        assertTrue(
            manager.saveBrowserFrontendPreferences(
                mapOf(
                    ProxyPlatform.X to BrowserFrontendPreference(
                        BrowserConversionMode.READER,
                        "x_xcancel",
                    )
                ),
                original,
            )
        )
        manager.disableBuiltIn(ProxyPlatform.X, "x_xcancel")

        val resolved = BrowserFrontendPolicy.resolve(
            ProxyPlatform.X,
            manager.getBrowserFrontendPreferences().getValue(ProxyPlatform.X),
            ProxyRoster.activeTargets(ProxyPlatform.X),
        )

        assertEquals("x_nitter_net", resolved?.id)
        assertEquals(
            "x_xcancel",
            manager.getBrowserFrontendPreferences()[ProxyPlatform.X]?.targetId,
        )
    }

    @Test
    fun `reader restore is allowed while final choice remains clean only`() {
        val manager = PreferencesManager(context)
        manager.disableBuiltIn(ProxyPlatform.X, "x_xcancel")
        val expected = manager.getBrowserFrontendPreferences()

        assertTrue(
            manager.saveBrowserFrontendPreferences(
                changes = mapOf(
                    ProxyPlatform.X to BrowserFrontendPreference(
                        BrowserConversionMode.CLEAN_ONLY,
                        "x_xcancel",
                    )
                ),
                expected = expected,
                restoreBuiltInIds = mapOf(ProxyPlatform.X to setOf("x_xcancel")),
            )
        )
        assertFalse("x_xcancel" in manager.getDisabledBuiltIns(ProxyPlatform.X))
        assertEquals(
            BrowserFrontendPreference(BrowserConversionMode.CLEAN_ONLY, "x_xcancel"),
            manager.getBrowserFrontendPreferences()[ProxyPlatform.X],
        )
    }

    @Test
    fun `embed has no fallback and unsupported platforms remain clean only`() {
        val xEmbed = BrowserFrontendPreference(BrowserConversionMode.EMBED, "x_fixupx")
        assertNull(
            BrowserFrontendPolicy.resolve(
                ProxyPlatform.X,
                xEmbed,
                ProxyRoster.activeTargets(ProxyPlatform.X).filterNot { it.id == "x_fixupx" },
            )
        )
        val manager = PreferencesManager(context)
        val expected = manager.getBrowserFrontendPreferences()
        assertTrue(
            runCatching {
                manager.saveBrowserFrontendPreferences(
                    mapOf(
                        ProxyPlatform.YOUTUBE to BrowserFrontendPreference(
                            BrowserConversionMode.CUSTOM,
                            "custom:video.example",
                        )
                    ),
                    expected,
                )
            }.isFailure
        )
    }

    @Test
    fun `legacy reader toggle keeps dormant target while disabled and restores it`() {
        val manager = PreferencesManager(context)
        manager.setBrowserPrivacyTargetId(ProxyPlatform.X, "x_xcancel")
        manager.setBrowserConvertTwitterEnabled(false)

        assertEquals(
            BrowserFrontendPreference(BrowserConversionMode.CLEAN_ONLY, "x_xcancel"),
            manager.getBrowserFrontendPreferences()[ProxyPlatform.X],
        )
        manager.setBrowserConvertTwitterEnabled(true)
        assertEquals(
            BrowserFrontendPreference(BrowserConversionMode.READER, "x_xcancel"),
            manager.getBrowserFrontendPreferences()[ProxyPlatform.X],
        )
    }

    @Test
    fun `raw invalid browser preference is reported instead of trusted`() {
        val manager = PreferencesManager(context)
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        raw.edit()
            .putString("browser_frontend_mode_youtube", BrowserConversionMode.CUSTOM.name)
            .putString("browser_frontend_target_youtube", "custom:video.example")
            .commit()

        assertTrue(manager.hasInvalidBrowserFrontendPreferences())
        assertEquals(
            BrowserFrontendPreference.CLEAN_ONLY,
            manager.getBrowserFrontendPreferences()[ProxyPlatform.YOUTUBE],
        )
    }

    @Test
    fun `supported platform list excludes clean-only platforms`() {
        assertEquals(
            ProxyPlatform.entries.filterNot {
                it == ProxyPlatform.YOUTUBE || it == ProxyPlatform.THREADS
            },
            BrowserFrontendPolicy.supportedPlatforms(),
        )
    }

    @Test
    fun `fingerprint changes with browser choice and custom roster`() {
        val manager = PreferencesManager(context)
        val initial = manager.browserFrontendFingerprint()
        val expected = manager.getBrowserFrontendPreferences()
        manager.saveBrowserFrontendPreferences(
            mapOf(
                ProxyPlatform.INSTAGRAM to BrowserFrontendPreference(
                    BrowserConversionMode.EMBED,
                    AlternativeFrontendCatalog.builtIn(ProxyPlatform.INSTAGRAM).first().id,
                )
            ),
            expected,
        )
        val selected = manager.browserFrontendFingerprint()
        manager.addCustomProxy(ProxyPlatform.INSTAGRAM, "ig-reader.example")

        assertNotEquals(initial, selected)
        assertNotEquals(selected, manager.browserFrontendFingerprint())
    }

    @Test
    fun `failed commit acknowledgement blocks Browser until settings are saved successfully`() {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var failWrites = false
        val wrapped = object : android.content.ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences =
                object : android.content.SharedPreferences by raw {
                    override fun edit(): android.content.SharedPreferences.Editor {
                        val delegate = raw.edit()
                        return object : android.content.SharedPreferences.Editor by delegate {
                            override fun commit(): Boolean {
                                // Android can update memory even when durable commit fails.
                                val committed = delegate.commit()
                                return committed && !failWrites
                            }
                        }
                    }
                }
        }
        val manager = PreferencesManager(wrapped)
        val change = mapOf(ProxyPlatform.X to BrowserFrontendPreference(BrowserConversionMode.EMBED, "x_fixupx"))
        failWrites = true
        assertFalse(manager.saveBrowserFrontendPreferences(change, manager.getBrowserFrontendPreferences()))
        assertTrue(manager.hasInvalidBrowserFrontendPreferences())
        failWrites = false
        assertTrue(manager.saveBrowserFrontendPreferences(change, manager.getBrowserFrontendPreferences()))
        assertFalse(manager.hasInvalidBrowserFrontendPreferences())
    }

    private companion object {
        const val PREFS_NAME = "FixupXerPrefs"
    }
}
