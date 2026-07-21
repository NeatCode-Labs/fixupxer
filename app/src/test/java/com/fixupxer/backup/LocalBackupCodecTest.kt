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

package com.fixupxer.backup

import com.fixupxer.PreferencesManager
import com.fixupxer.rules.RuleBundleCodec
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.RetiredFrontendMigration
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalBackupCodecTest {
    private lateinit var codec: LocalBackupCodec
    private lateinit var ruleCodec: RuleBundleCodec

    @Before
    fun setup() {
        ruleCodec = RuleBundleCodec()
        codec = LocalBackupCodec(ruleCodec)
    }

    @Test
    fun `round trip preserves empty rules and settings fields`() {
        val settings = validSettings()
        val rulesJson = ruleCodec.encodeBundle(emptyList())
        val encoded = codec.encode(settings, rulesJson)
        val decoded = codec.decode(encoded)
        assertEquals(settings.cleanTracking, decoded.settings.cleanTracking)
        assertEquals(settings.browserEnabled, decoded.settings.browserEnabled)
        assertEquals(
            settings.showConfigurationStatusWidget,
            decoded.settings.showConfigurationStatusWidget,
        )
        assertEquals(settings.actionPriority, decoded.settings.actionPriority)
        assertTrue(decoded.rules.rules.isEmpty())
        assertTrue(decoded.settings.rememberedRoutes.isEmpty())
    }

    @Test
    fun `newer schema is rejected`() {
        val encoded = encodeValid()
            .replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")
        assertTrue(runCatching { codec.decode(encoded) }.isFailure)
    }

    @Test
    fun `older backup without status widget preference defaults to shown`() {
        val root = JSONObject(encodeValid())
        root.getJSONObject("settings").remove("showConfigurationStatusWidget")

        val decoded = codec.decode(root.toString())

        assertTrue(decoded.settings.showConfigurationStatusWidget)
    }

    @Test
    fun `status widget true and false both round trip in schema one`() {
        listOf(true, false).forEach { value ->
            val settings = validSettings().copy(showConfigurationStatusWidget = value)
            val decoded = codec.decode(
                codec.encode(settings, ruleCodec.encodeBundle(emptyList()))
            )

            assertEquals(value, decoded.settings.showConfigurationStatusWidget)
            assertEquals(LocalBackupCodec.SCHEMA_VERSION, decoded.schemaVersion)
        }
    }

    @Test
    fun `preview exposes restored history contract`() {
        val settings = validSettings().copy(
            historyEnabled = false,
            maxHistoryEntries = 321,
        )

        val preview = codec.preview(codec.encode(settings, ruleCodec.encodeBundle(emptyList())))

        assertFalse(preview.historyEnabled)
        assertEquals(321, preview.maxHistoryEntries)
    }

    @Test
    fun `invalid format is rejected`() {
        assertTrue(runCatching { codec.decode("""{"format":"other"}""") }.isFailure)
    }

    @Test
    fun `oversize backup is rejected before parsing`() {
        val oversize = "x".repeat(com.fixupxer.utils.Constants.MAX_LOCAL_BACKUP_BYTES + 1)
        assertTrue(runCatching { codec.decode(oversize) }.isFailure)
    }

    @Test
    fun `incomplete action priority is rejected`() {
        val settings = validSettings().copy(
            actionPriority = listOf(
                PreferencesManager.ACTION_NATIVE_APP,
                PreferencesManager.ACTION_BROWSER,
            ),
        )
        assertDecodeRejects(settings)
    }

    @Test
    fun `duplicate action priority entries are rejected`() {
        val settings = validSettings().copy(
            actionPriority = listOf(
                PreferencesManager.ACTION_NATIVE_APP,
                PreferencesManager.ACTION_NATIVE_APP,
                PreferencesManager.ACTION_SHARE_MENU,
                PreferencesManager.ACTION_CLIPBOARD,
            ),
        )
        assertDecodeRejects(settings)
    }

    @Test
    fun `unknown action priority entry is rejected`() {
        val settings = validSettings().copy(
            actionPriority = listOf(
                PreferencesManager.ACTION_NATIVE_APP,
                PreferencesManager.ACTION_BROWSER,
                PreferencesManager.ACTION_SHARE_MENU,
                "email",
            ),
        )
        assertDecodeRejects(settings)
    }

    @Test
    fun `out of range max history entries is rejected`() {
        assertDecodeRejects(
            validSettings().copy(maxHistoryEntries = Constants.MIN_HISTORY_ENTRIES - 1)
        )
        assertDecodeRejects(
            validSettings().copy(maxHistoryEntries = Constants.MAX_HISTORY_ENTRIES + 1)
        )
    }

    @Test
    fun `unknown theme mode is rejected`() {
        assertDecodeRejects(validSettings().copy(themeMode = "sepia"))
    }

    @Test
    fun `uppercase custom proxy is rejected`() {
        assertDecodeRejects(withCustomProxies(ProxyPlatform.INSTAGRAM, listOf("MyProxy.Example")))
    }

    @Test
    fun `custom proxy colliding with built-in domain is rejected`() {
        assertDecodeRejects(withCustomProxies(ProxyPlatform.INSTAGRAM, listOf("fixupx.com")))
    }

    @Test
    fun `custom proxy colliding with source domain is rejected`() {
        assertDecodeRejects(withCustomProxies(ProxyPlatform.TIKTOK, listOf("instagram.com")))
    }

    @Test
    fun `duplicate custom proxies on one platform are rejected`() {
        assertDecodeRejects(
            withCustomProxies(ProxyPlatform.INSTAGRAM, listOf("proxy.example", "proxy.example")),
        )
    }

    @Test
    fun `overlapping custom proxies on one platform are rejected`() {
        assertDecodeRejects(
            withCustomProxies(
                ProxyPlatform.INSTAGRAM,
                listOf("proxy.example", "sub.proxy.example"),
            ),
        )
    }

    @Test
    fun `custom proxy colliding with another platform custom is rejected`() {
        val settings = validSettings().copy(
            customProxies = ProxyPlatform.entries.associateWith { platform ->
                when (platform) {
                    ProxyPlatform.INSTAGRAM -> listOf("proxy.example")
                    ProxyPlatform.TIKTOK -> listOf("proxy.example")
                    else -> emptyList()
                }
            },
        )
        assertDecodeRejects(settings)
    }

    @Test
    fun `valid custom proxy round trips`() {
        val settings = withCustomProxies(ProxyPlatform.INSTAGRAM, listOf("proxy.example"))
        val decoded = codec.decode(codec.encode(settings, ruleCodec.encodeBundle(emptyList())))
        assertEquals(listOf("proxy.example"), decoded.settings.customProxies[ProxyPlatform.INSTAGRAM])
    }

    @Test
    fun `disabled id from another platform is rejected`() {
        val settings = validSettings().copy(
            disabledBuiltIns = ProxyPlatform.entries.associateWith { platform ->
                if (platform == ProxyPlatform.INSTAGRAM) setOf("x_fixupx") else emptySet()
            },
        )
        assertDecodeRejects(settings)
    }

    @Test
    fun `unknown disabled id is rejected`() {
        val settings = validSettings().copy(
            disabledBuiltIns = ProxyPlatform.entries.associateWith { platform ->
                if (platform == ProxyPlatform.X) setOf("x_nonexistent") else emptySet()
            },
        )
        assertDecodeRejects(settings)
    }

    @Test
    fun `privacy target must be reader of same platform`() {
        val embedTarget = validSettings().copy(
            browserPrivacyTargetIds = ProxyPlatform.entries.associateWith { platform ->
                if (platform == ProxyPlatform.X) "x_fixupx" else null
            },
        )
        assertDecodeRejects(embedTarget)

        val wrongPlatform = validSettings().copy(
            browserPrivacyTargetIds = ProxyPlatform.entries.associateWith { platform ->
                if (platform == ProxyPlatform.REDDIT) "x_xcancel" else null
            },
        )
        assertDecodeRejects(wrongPlatform)
    }

    @Test
    fun `valid reader privacy target is accepted`() {
        val settings = validSettings().copy(
            browserPrivacyTargetIds = ProxyPlatform.entries.associateWith { platform ->
                if (platform == ProxyPlatform.X) "x_xcancel" else null
            },
        )
        val decoded = codec.decode(codec.encode(settings, ruleCodec.encodeBundle(emptyList())))
        assertEquals("x_xcancel", decoded.settings.browserPrivacyTargetIds[ProxyPlatform.X])
    }

    @Test
    fun `selection pointing at disabled built-in is rejected`() {
        val settings = validSettings().copy(
            proxySelections = ProxyPlatform.entries.associateWith { platform ->
                if (platform == ProxyPlatform.X) "fixupx.com" else null
            },
            disabledBuiltIns = ProxyPlatform.entries.associateWith { platform ->
                if (platform == ProxyPlatform.X) setOf("x_fixupx") else emptySet()
            },
        )
        assertDecodeRejects(settings)
    }

    @Test
    fun `selection of active custom target is accepted`() {
        val settings = validSettings().copy(
            proxySelections = ProxyPlatform.entries.associateWith { platform ->
                if (platform == ProxyPlatform.INSTAGRAM) "proxy.example" else null
            },
            customProxies = ProxyPlatform.entries.associateWith { platform ->
                if (platform == ProxyPlatform.INSTAGRAM) listOf("proxy.example") else emptyList()
            },
        )
        val decoded = codec.decode(codec.encode(settings, ruleCodec.encodeBundle(emptyList())))
        assertEquals("proxy.example", decoded.settings.proxySelections[ProxyPlatform.INSTAGRAM])
    }

    @Test
    fun `cross-device native route survives structural validation`() {
        val settings = validSettings().copy(
            rememberedRoutes = mapOf(
                "google.com" to RememberedRoute(
                    RememberedRouteKind.NATIVE,
                    "com.google.android.googlequicksearchbox",
                ),
            ),
        )
        val decoded = codec.decode(codec.encode(settings, ruleCodec.encodeBundle(emptyList())))
        assertEquals(
            "com.google.android.googlequicksearchbox",
            decoded.settings.rememberedRoutes["google.com"]?.packageName,
        )
    }

    @Test
    fun `cross-device browser route survives decode`() {
        val settings = validSettings().copy(
            rememberedRoutes = mapOf(
                "example.com" to RememberedRoute(RememberedRouteKind.BROWSER, "org.mozilla.firefox"),
            ),
        )
        val decoded = codec.decode(codec.encode(settings, ruleCodec.encodeBundle(emptyList())))
        assertEquals(
            RememberedRoute(RememberedRouteKind.BROWSER, "org.mozilla.firefox"),
            decoded.settings.rememberedRoutes["example.com"],
        )
    }

    @Test
    fun `decode migrates retired instagram selection to active default`() {
        val encoded = encodeValidWithRawSettings(
            proxySelections = proxyStringMap(ProxyPlatform.INSTAGRAM to Constants.KKINSTAGRAM_DOMAIN),
        )
        val decoded = codec.decode(encoded)
        assertEquals(Constants.TOINSTAGRAM_DOMAIN, decoded.settings.proxySelections[ProxyPlatform.INSTAGRAM])
    }

    @Test
    fun `decode migrates retired facebook selection and disables convert when no customs`() {
        val encoded = encodeValidWithRawSettings(
            proxySelections = proxyStringMap(ProxyPlatform.FACEBOOK to Constants.FACEBOOKEZ_DOMAIN),
            convertFacebook = true,
        )
        val decoded = codec.decode(encoded)
        assertNull(decoded.settings.proxySelections[ProxyPlatform.FACEBOOK])
        assertFalse(decoded.settings.convertFacebook)
    }

    @Test
    fun `decode preserves facebook conversion without a retired selection`() {
        val encoded = encodeValidWithRawSettings(convertFacebook = true)

        val decoded = codec.decode(encoded)

        assertTrue(decoded.settings.convertFacebook)
    }

    @Test
    fun `decode keeps facebook conversion when retired selection has a custom fallback`() {
        val encoded = encodeValidWithRawSettings(
            proxySelections = proxyStringMap(ProxyPlatform.FACEBOOK to Constants.FACEBOOKEZ_DOMAIN),
            customProxies = proxyListMap(ProxyPlatform.FACEBOOK to listOf("example-proxy.net")),
            convertFacebook = true,
        )

        val decoded = codec.decode(encoded)

        assertNull(decoded.settings.proxySelections[ProxyPlatform.FACEBOOK])
        assertTrue(decoded.settings.convertFacebook)
    }

    @Test
    fun `decode strips retired disabled built-in ids`() {
        val encoded = encodeValidWithRawSettings(
            disabledBuiltIns = disabledSetMap(
                ProxyPlatform.INSTAGRAM to setOf(RetiredFrontendMigration.RETIRED_INSTAGRAM_DISABLED_ID),
                ProxyPlatform.FACEBOOK to setOf(RetiredFrontendMigration.RETIRED_FACEBOOK_DISABLED_ID),
            ),
        )
        val decoded = codec.decode(encoded)
        assertTrue(decoded.settings.disabledBuiltIns[ProxyPlatform.INSTAGRAM]!!.isEmpty())
        assertTrue(decoded.settings.disabledBuiltIns[ProxyPlatform.FACEBOOK]!!.isEmpty())
    }

    @Test
    fun `backup with retired domain as facebook custom proxy is rejected`() {
        val encoded = encodeValidWithRawSettings(
            customProxies = proxyListMap(ProxyPlatform.FACEBOOK to listOf(Constants.FACEBOOKEZ_DOMAIN)),
        )
        assertTrue(runCatching { codec.decode(encoded) }.isFailure)
    }

    @Test
    fun `backup with retired domain as instagram custom proxy is rejected`() {
        val encoded = encodeValidWithRawSettings(
            customProxies = proxyListMap(ProxyPlatform.INSTAGRAM to listOf(Constants.KKINSTAGRAM_DOMAIN)),
        )
        assertTrue(runCatching { codec.decode(encoded) }.isFailure)
    }

    @Test
    fun `backup with retired subdomain as custom proxy is rejected`() {
        val encoded = encodeValidWithRawSettings(
            customProxies = proxyListMap(ProxyPlatform.X to listOf("evil.facebookez.com")),
        )
        assertTrue(runCatching { codec.decode(encoded) }.isFailure)
    }

    @Test
    fun `encoded settings contain only whitelisted keys and no history data`() {
        val encoded = encodeValid()
        val settingsJson = JSONObject(encoded).getJSONObject("settings")
        val keys = buildSet {
            settingsJson.keys().forEach { add(it) }
        }
        val expected = setOf(
            "cleanTracking", "convertTwitter", "convertInstagram", "convertTikTok",
            "convertBluesky", "convertFacebook", "convertReddit", "convertYoutube",
            "convertPinterest", "convertThreads", "customRulesEnabled", "historyEnabled",
            "maxHistoryEntries", "themeMode", "dominantHand", "browserEnabled",
            "showConfigurationStatusWidget", "actionMode", "actionPriority",
            "browserConvertTwitter", "browserConvertBluesky", "browserConvertReddit",
            "browserConvertPinterest", "proxySelections", "customProxies", "disabledBuiltIns",
            "browserPrivacyTargets", "rememberedRoutes",
        )
        assertEquals(expected, keys)
        assertFalse(encoded.contains("url_history"))
    }

    private fun encodeValid(): String =
        codec.encode(validSettings(), ruleCodec.encodeBundle(emptyList()))

    private fun encodeValidWithRawSettings(
        proxySelections: JSONObject? = null,
        customProxies: JSONObject? = null,
        disabledBuiltIns: JSONObject? = null,
        convertFacebook: Boolean? = null,
    ): String {
        val root = JSONObject(encodeValid())
        val settings = root.getJSONObject("settings")
        proxySelections?.let { settings.put("proxySelections", it) }
        customProxies?.let { settings.put("customProxies", it) }
        disabledBuiltIns?.let { settings.put("disabledBuiltIns", it) }
        convertFacebook?.let { settings.put("convertFacebook", it) }
        return root.toString()
    }

    private fun proxyStringMap(vararg pairs: Pair<ProxyPlatform, String?>): JSONObject =
        JSONObject().apply {
            ProxyPlatform.entries.forEach { platform ->
                put(platform.name.lowercase(), pairs.toMap()[platform])
            }
        }

    private fun proxyListMap(vararg pairs: Pair<ProxyPlatform, List<String>>): JSONObject =
        JSONObject().apply {
            ProxyPlatform.entries.forEach { platform ->
                put(platform.name.lowercase(), JSONArray(pairs.toMap()[platform].orEmpty()))
            }
        }

    private fun disabledSetMap(vararg pairs: Pair<ProxyPlatform, Set<String>>): JSONObject =
        JSONObject().apply {
            ProxyPlatform.entries.forEach { platform ->
                put(
                    platform.name.lowercase(),
                    JSONArray(pairs.toMap()[platform].orEmpty().sorted()),
                )
            }
        }

    private fun assertDecodeRejects(settings: SettingsSnapshot) {
        assertTrue(
            runCatching {
                val encoded = codec.encode(settings, ruleCodec.encodeBundle(emptyList()))
                codec.decode(encoded)
            }.isFailure,
        )
    }

    private fun withCustomProxies(
        platform: ProxyPlatform,
        proxies: List<String>,
    ): SettingsSnapshot = validSettings().copy(
        customProxies = ProxyPlatform.entries.associateWith {
            if (it == platform) proxies else emptyList()
        },
    )

    private fun validSettings(): SettingsSnapshot = SettingsSnapshot(
        cleanTracking = true,
        convertTwitter = true,
        convertInstagram = true,
        convertTikTok = true,
        convertBluesky = true,
        convertFacebook = true,
        convertReddit = false,
        convertYoutube = false,
        convertPinterest = false,
        convertThreads = false,
        customRulesEnabled = false,
        historyEnabled = true,
        maxHistoryEntries = 100,
        themeMode = PreferencesManager.THEME_MODE_SYSTEM,
        dominantHand = PreferencesManager.DOMINANT_HAND_RIGHT,
        browserEnabled = false,
        showConfigurationStatusWidget = false,
        actionMode = PreferencesManager.ACTION_MODE_ASK,
        actionPriority = listOf(
            PreferencesManager.ACTION_NATIVE_APP,
            PreferencesManager.ACTION_BROWSER,
            PreferencesManager.ACTION_SHARE_MENU,
            PreferencesManager.ACTION_CLIPBOARD,
        ),
        browserConvertTwitter = false,
        browserConvertBluesky = false,
        browserConvertReddit = false,
        browserConvertPinterest = false,
        proxySelections = ProxyPlatform.entries.associateWith { null },
        customProxies = ProxyPlatform.entries.associateWith { emptyList() },
        disabledBuiltIns = ProxyPlatform.entries.associateWith { emptySet() },
        browserPrivacyTargetIds = ProxyPlatform.entries.associateWith { null },
        rememberedRoutes = emptyMap(),
    )
}
