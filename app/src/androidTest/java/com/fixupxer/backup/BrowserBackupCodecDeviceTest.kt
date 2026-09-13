// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026 NeatCode Labs
 *
 * This program is free software under the GNU General Public License,
 * version 3 or (at your option) any later version.
 */

package com.fixupxer.backup

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixupxer.PreferencesManager
import com.fixupxer.processing.BrowserConversionMode
import com.fixupxer.processing.BrowserFrontendPreference
import com.fixupxer.rules.RuleBundleCodec
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import com.fixupxer.utils.RetiredFrontendMigration
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BrowserBackupCodecDeviceTest {
    private lateinit var codec: LocalBackupCodec
    private lateinit var isolatedContext: IsolatedPreferencesContext
    private lateinit var originalCustoms: Map<ProxyPlatform, List<String>>
    private lateinit var originalDisabled: Map<ProxyPlatform, Set<String>>

    @Before
    fun setUp() {
        codec = LocalBackupCodec(RuleBundleCodec())
        originalCustoms = ProxyPlatform.entries.associateWith(ProxyRoster::getCustomProxies)
        originalDisabled = ProxyPlatform.entries.associateWith(ProxyRoster::getDisabledBuiltIns)
        ProxyRoster.reset()
        val base = ApplicationProvider.getApplicationContext<Context>()
        isolatedContext = IsolatedPreferencesContext(
            base,
            "BrowserBackupCodecDeviceTest-${UUID.randomUUID()}",
        )
        assertTrue(isolatedContext.preferences.edit().clear().commit())
    }

    @After
    fun tearDown() {
        isolatedContext.preferences.edit().clear().commit()
        ProxyRoster.reset()
        ProxyPlatform.entries.forEach { platform ->
            ProxyRoster.setCustomProxies(platform, originalCustoms.getValue(platform))
            ProxyRoster.setDisabledBuiltIns(platform, originalDisabled.getValue(platform))
        }
    }

    @Test
    fun schemaTwoDecodesCompleteAllPlatformMapAndSnapshotLocalCustomTarget() {
        val root = validBackup(schemaVersion = 2)
        val settings = root.getJSONObject("settings")
        settings.getJSONObject("customProxies")
            .put(ProxyPlatform.X.wireName(), JSONArray().put("x-browser.example"))
        settings.getJSONObject("browserFrontends")
            .getJSONObject(ProxyPlatform.X.wireName())
            .put("mode", BrowserConversionMode.CUSTOM.name)
            .put("targetId", "custom:x-browser.example")

        val decoded = codec.decode(root.toString())

        assertEquals(2, decoded.schemaVersion)
        assertEquals(ProxyPlatform.entries.toSet(), decoded.settings.browserFrontends.keys)
        assertEquals(
            BrowserFrontendPreference(BrowserConversionMode.CUSTOM, "custom:x-browser.example"),
            decoded.settings.browserFrontends[ProxyPlatform.X],
        )
        assertEquals(listOf("x-browser.example"), decoded.settings.customProxies[ProxyPlatform.X])
        assertTrue(decoded.rules.rules.isEmpty())
    }

    @Test
    fun schemaOneMigratesEnabledAndDormantReadersAndIgnoresLegacyNonReaderToggles() {
        val root = validBackup(schemaVersion = 1)
        val settings = root.getJSONObject("settings")
        settings.put("browserPrivacyTargets", platformNullableStringMap().apply {
            put(ProxyPlatform.X.wireName(), "x_xcancel")
            put(ProxyPlatform.BLUESKY.wireName(), "bs_skylib_coffee")
            put(ProxyPlatform.REDDIT.wireName(), "rd_redlib_catsarch")
            put(ProxyPlatform.PINTEREST.wireName(), "pt_pinterest_bunk")
            put(ProxyPlatform.INSTAGRAM.wireName(), "ig_toinstagram")
        })
        settings
            .put("browserConvertTwitter", false)
            .put("browserConvertBluesky", true)
            .put("browserConvertReddit", false)
            .put("browserConvertPinterest", true)
            .put("browserConvertInstagram", true)
            .put("browserConvertTikTok", true)
            .put("browserConvertFacebook", true)
            .put("browserConvertYoutube", true)
            .put("browserConvertThreads", true)

        val decoded = codec.decode(root.toString()).settings.browserFrontends

        assertEquals(
            BrowserFrontendPreference(BrowserConversionMode.CLEAN_ONLY, "x_xcancel"),
            decoded[ProxyPlatform.X],
        )
        assertEquals(
            BrowserFrontendPreference(BrowserConversionMode.READER, "bs_skylib_coffee"),
            decoded[ProxyPlatform.BLUESKY],
        )
        assertEquals(
            BrowserFrontendPreference(BrowserConversionMode.CLEAN_ONLY, "rd_redlib_catsarch"),
            decoded[ProxyPlatform.REDDIT],
        )
        assertEquals(
            BrowserFrontendPreference(BrowserConversionMode.READER, "pt_pinterest_bunk"),
            decoded[ProxyPlatform.PINTEREST],
        )
        assertEquals(BrowserFrontendPreference.CLEAN_ONLY, decoded[ProxyPlatform.INSTAGRAM])
        assertEquals(BrowserFrontendPreference.CLEAN_ONLY, decoded[ProxyPlatform.TIKTOK])
        assertEquals(BrowserFrontendPreference.CLEAN_ONLY, decoded[ProxyPlatform.FACEBOOK])
        assertEquals(BrowserFrontendPreference.CLEAN_ONLY, decoded[ProxyPlatform.YOUTUBE])
        assertEquals(BrowserFrontendPreference.CLEAN_ONLY, decoded[ProxyPlatform.THREADS])
    }

    @Test
    fun schemaTwoRejectsCustomTargetAbsentFromImportedRosterAndUnknownMode() {
        val absentCustom = validBackup(schemaVersion = 2)
        absentCustom.getJSONObject("settings")
            .getJSONObject("browserFrontends")
            .getJSONObject(ProxyPlatform.X.wireName())
            .put("mode", BrowserConversionMode.CUSTOM.name)
            .put("targetId", "custom:missing.example")
        assertTrue(runCatching { codec.decode(absentCustom.toString()) }.isFailure)

        val unknownMode = validBackup(schemaVersion = 2)
        unknownMode.getJSONObject("settings")
            .getJSONObject("browserFrontends")
            .getJSONObject(ProxyPlatform.X.wireName())
            .put("mode", "AUTOMATIC")
            .put("targetId", "x_fixupx")
        assertTrue(runCatching { codec.decode(unknownMode.toString()) }.isFailure)
    }

    @Test
    fun schemaTwoMigratesHistoricalRetiredBrowserIdsButRejectsRetiredCustomDomains() {
        val retiredId = validBackup(schemaVersion = 2)
        retiredId.getJSONObject("settings")
            .getJSONObject("browserFrontends")
            .getJSONObject(ProxyPlatform.INSTAGRAM.wireName())
            .put("mode", BrowserConversionMode.EMBED.name)
            .put("targetId", RetiredFrontendMigration.RETIRED_INSTAGRAM_DISABLED_ID)

        val migrated = codec.decode(retiredId.toString())
        assertEquals(
            BrowserFrontendPreference.CLEAN_ONLY,
            migrated.settings.browserFrontends[ProxyPlatform.INSTAGRAM],
        )

        val retiredCustom = validBackup(schemaVersion = 2)
        retiredCustom.getJSONObject("settings")
            .getJSONObject("customProxies")
            .put(ProxyPlatform.FACEBOOK.wireName(), JSONArray().put("facebookez.com"))
        retiredCustom.getJSONObject("settings")
            .getJSONObject("browserFrontends")
            .getJSONObject(ProxyPlatform.FACEBOOK.wireName())
            .put("mode", BrowserConversionMode.CUSTOM.name)
            .put("targetId", "custom:facebookez.com")
        assertTrue(runCatching { codec.decode(retiredCustom.toString()) }.isFailure)
    }

    @Test
    fun preferencesMigrationRunsOnceAndPreservesDisabledReaderIdsAtomically() {
        assertTrue(
            isolatedContext.preferences.edit()
                .putBoolean("browser_convert_twitter", false)
                .putString("browser_privacy_target_x", "x_xcancel")
                .putBoolean("browser_convert_bluesky", true)
                .putString("browser_privacy_target_bluesky", "bs_skylib_coffee")
                .putBoolean("browser_convert_instagram", true)
                .putString("browser_privacy_target_instagram", "ig_toinstagram")
                .commit()
        )

        val first = PreferencesManager(isolatedContext).getBrowserFrontendPreferences()
        assertEquals(ProxyPlatform.entries.toSet(), first.keys)
        assertEquals(
            BrowserFrontendPreference(BrowserConversionMode.CLEAN_ONLY, "x_xcancel"),
            first[ProxyPlatform.X],
        )
        assertEquals(
            BrowserFrontendPreference(BrowserConversionMode.READER, "bs_skylib_coffee"),
            first[ProxyPlatform.BLUESKY],
        )
        assertEquals(BrowserFrontendPreference.CLEAN_ONLY, first[ProxyPlatform.INSTAGRAM])
        assertFalse(PreferencesManager(isolatedContext).hasInvalidBrowserFrontendPreferences())

        assertTrue(
            isolatedContext.preferences.edit()
                .putBoolean("browser_convert_twitter", true)
                .putString("browser_privacy_target_x", "x_nitter_net")
                .commit()
        )
        val second = PreferencesManager(isolatedContext).getBrowserFrontendPreferences()
        assertEquals(first, second)
        assertEquals("x_xcancel", second[ProxyPlatform.X]?.targetId)
        assertNull(second[ProxyPlatform.YOUTUBE]?.targetId)
        assertNull(second[ProxyPlatform.THREADS]?.targetId)
    }

    private fun validBackup(schemaVersion: Int): JSONObject {
        val settings = JSONObject()
            .put("cleanTracking", true)
            .put("convertTwitter", false)
            .put("convertInstagram", false)
            .put("convertTikTok", false)
            .put("convertBluesky", false)
            .put("convertFacebook", false)
            .put("convertReddit", false)
            .put("convertYoutube", false)
            .put("convertPinterest", false)
            .put("convertThreads", false)
            .put("customRulesEnabled", false)
            .put("historyEnabled", true)
            .put("maxHistoryEntries", 100)
            .put("themeMode", PreferencesManager.THEME_MODE_SYSTEM)
            .put("dominantHand", PreferencesManager.DOMINANT_HAND_RIGHT)
            .put("browserEnabled", true)
            .put("showConfigurationStatusWidget", true)
            .put("actionMode", PreferencesManager.ACTION_MODE_ASK)
            .put(
                "actionPriority",
                JSONArray()
                    .put(PreferencesManager.ACTION_NATIVE_APP)
                    .put(PreferencesManager.ACTION_BROWSER)
                    .put(PreferencesManager.ACTION_SHARE_MENU)
                    .put(PreferencesManager.ACTION_CLIPBOARD),
            )
            .put("proxySelections", platformNullableStringMap())
            .put("customProxies", platformArrayMap())
            .put("disabledBuiltIns", platformArrayMap())
            .put("rememberedRoutes", JSONObject())

        if (schemaVersion == 2) {
            settings.put("browserFrontends", JSONObject().apply {
                ProxyPlatform.entries.forEach { platform ->
                    put(
                        platform.wireName(),
                        JSONObject()
                            .put("mode", BrowserConversionMode.CLEAN_ONLY.name)
                            .put("targetId", JSONObject.NULL),
                    )
                }
            })
        }

        return JSONObject()
            .put("format", LocalBackupCodec.FORMAT_ID)
            .put("schemaVersion", schemaVersion)
            .put("appVersion", "device-test")
            .put("exportedAt", 0L)
            .put("settings", settings)
            .put(
                "customRules",
                JSONObject()
                    .put("format", RuleBundleCodec.FORMAT_ID)
                    .put("schemaVersion", RuleBundleCodec.SCHEMA_VERSION)
                    .put("appVersion", "device-test")
                    .put("rules", JSONArray()),
            )
    }

    private fun platformNullableStringMap(): JSONObject = JSONObject().apply {
        ProxyPlatform.entries.forEach { put(it.wireName(), JSONObject.NULL) }
    }

    private fun platformArrayMap(): JSONObject = JSONObject().apply {
        ProxyPlatform.entries.forEach { put(it.wireName(), JSONArray()) }
    }

    private fun ProxyPlatform.wireName(): String = name.lowercase()

    private class IsolatedPreferencesContext(
        base: Context,
        private val prefix: String,
    ) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this

        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            baseContext.getSharedPreferences("$prefix-$name", mode)

        val preferences: SharedPreferences
            get() = getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE)
    }
}
