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

import com.fixupxer.BuildConfig
import com.fixupxer.processing.BrowserConversionMode
import com.fixupxer.processing.BrowserFrontendPreference
import com.fixupxer.rules.RuleBundle
import com.fixupxer.rules.RuleBundleCodec
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.RetiredFrontendMigration
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class LocalBackupBundle(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: Long,
    val settings: SettingsSnapshot,
    val rules: RuleBundle,
)

data class LocalBackupPreview(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: Long,
    val ruleCount: Int,
    val routeCount: Int,
    val historyEnabled: Boolean,
    val maxHistoryEntries: Int,
)

@Singleton
class LocalBackupCodec @Inject constructor(
    private val ruleBundleCodec: RuleBundleCodec,
) {
    companion object {
        const val FORMAT_ID = "fixupxer-local-backup"
        const val SCHEMA_VERSION = 2
    }

    fun encode(settings: SettingsSnapshot, rulesJson: String): String {
        SettingsSnapshotValidator.validate(settings)
        val rulesObject = JSONObject(rulesJson)
        require(rulesObject.getString("format") == RuleBundleCodec.FORMAT_ID) {
            "Invalid embedded rule bundle"
        }
        val encoded = JSONObject()
            .put("format", FORMAT_ID)
            .put("schemaVersion", SCHEMA_VERSION)
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("exportedAt", System.currentTimeMillis())
            .put("settings", encodeSettings(settings))
            .put("customRules", rulesObject)
            .toString(2)
        require(encoded.toByteArray(Charsets.UTF_8).size <= Constants.MAX_LOCAL_BACKUP_BYTES) {
            "Backup is too large to export"
        }
        return encoded
    }

    fun decode(json: String): LocalBackupBundle {
        require(json.toByteArray(Charsets.UTF_8).size <= Constants.MAX_LOCAL_BACKUP_BYTES) {
            "Backup file is too large"
        }
        val root = JSONObject(json)
        require(root.getString("format") == FORMAT_ID) { "Unsupported backup format" }
        val schemaVersion = root.getInt("schemaVersion")
        require(schemaVersion in 1..SCHEMA_VERSION) { "Unsupported backup schema: $schemaVersion" }
        val settings = decodeSettings(root.getJSONObject("settings"), schemaVersion)
        val rules = ruleBundleCodec.decodeBundle(root.getJSONObject("customRules").toString())
        return LocalBackupBundle(
            schemaVersion = schemaVersion,
            appVersion = root.optString("appVersion", ""),
            exportedAt = root.optLong("exportedAt", 0L),
            settings = settings,
            rules = rules,
        )
    }

    fun preview(json: String): LocalBackupPreview {
        val bundle = decode(json)
        return LocalBackupPreview(
            schemaVersion = bundle.schemaVersion,
            appVersion = bundle.appVersion,
            exportedAt = bundle.exportedAt,
            ruleCount = bundle.rules.rules.size,
            routeCount = bundle.settings.rememberedRoutes.size,
            historyEnabled = bundle.settings.historyEnabled,
            maxHistoryEntries = bundle.settings.maxHistoryEntries,
        )
    }

    private fun encodeSettings(snapshot: SettingsSnapshot): JSONObject = JSONObject()
        .put("cleanTracking", snapshot.cleanTracking)
        .put("convertTwitter", snapshot.convertTwitter)
        .put("convertInstagram", snapshot.convertInstagram)
        .put("convertTikTok", snapshot.convertTikTok)
        .put("convertBluesky", snapshot.convertBluesky)
        .put("convertFacebook", snapshot.convertFacebook)
        .put("convertReddit", snapshot.convertReddit)
        .put("convertYoutube", snapshot.convertYoutube)
        .put("convertPinterest", snapshot.convertPinterest)
        .put("convertThreads", snapshot.convertThreads)
        .put("customRulesEnabled", snapshot.customRulesEnabled)
        .put("historyEnabled", snapshot.historyEnabled)
        .put("maxHistoryEntries", snapshot.maxHistoryEntries)
        .put("themeMode", snapshot.themeMode)
        .put("dominantHand", snapshot.dominantHand)
        .put("browserEnabled", snapshot.browserEnabled)
        .put("showConfigurationStatusWidget", snapshot.showConfigurationStatusWidget)
        .put("actionMode", snapshot.actionMode)
        .put("actionPriority", JSONArray(snapshot.actionPriority))
        .put("proxySelections", encodePlatformStringMap(snapshot.proxySelections))
        .put("customProxies", encodePlatformListMap(snapshot.customProxies))
        .put("disabledBuiltIns", encodePlatformSetMap(snapshot.disabledBuiltIns))
        .put("browserFrontends", encodeBrowserFrontends(snapshot.browserFrontends))
        .put("rememberedRoutes", encodeRememberedRoutes(snapshot.rememberedRoutes))

    private fun decodeSettings(json: JSONObject, schemaVersion: Int): SettingsSnapshot {
        val customProxies = decodePlatformListMap(json.getJSONObject("customProxies"))
        val browserFrontends = if (schemaVersion == 1) {
            decodeV1BrowserFrontends(json)
        } else {
            decodeBrowserFrontends(json.getJSONObject("browserFrontends"), customProxies)
        }
        val snapshot = SettingsSnapshot(
            cleanTracking = json.getBoolean("cleanTracking"),
            convertTwitter = json.getBoolean("convertTwitter"),
            convertInstagram = json.getBoolean("convertInstagram"),
            convertTikTok = json.getBoolean("convertTikTok"),
            convertBluesky = json.getBoolean("convertBluesky"),
            convertFacebook = json.getBoolean("convertFacebook"),
            convertReddit = json.getBoolean("convertReddit"),
            convertYoutube = json.getBoolean("convertYoutube"),
            convertPinterest = json.getBoolean("convertPinterest"),
            convertThreads = json.getBoolean("convertThreads"),
            customRulesEnabled = json.getBoolean("customRulesEnabled"),
            historyEnabled = json.getBoolean("historyEnabled"),
            maxHistoryEntries = json.getInt("maxHistoryEntries"),
            themeMode = json.getString("themeMode"),
            dominantHand = json.getString("dominantHand"),
            browserEnabled = json.getBoolean("browserEnabled"),
            showConfigurationStatusWidget = json.optBoolean("showConfigurationStatusWidget", true),
            actionMode = json.getString("actionMode"),
            actionPriority = json.getJSONArray("actionPriority").strings(),
            proxySelections = decodePlatformStringMap(json.getJSONObject("proxySelections")),
            customProxies = customProxies,
            disabledBuiltIns = decodePlatformSetMap(json.getJSONObject("disabledBuiltIns")),
            browserFrontends = browserFrontends,
            rememberedRoutes = decodeRememberedRoutes(json.getJSONObject("rememberedRoutes")),
        )
        val migrated = migrateRetiredBrowserFrontends(
            RetiredFrontendMigration.migrateSnapshot(snapshot)
        )
        SettingsSnapshotValidator.validate(migrated)
        return migrated
    }

    private fun decodeV1BrowserFrontends(json: JSONObject): Map<ProxyPlatform, BrowserFrontendPreference> {
        val targetIds = decodePlatformStringMap(json.getJSONObject("browserPrivacyTargets"))
        val enabledByPlatform = mapOf(
            ProxyPlatform.X to json.getBoolean("browserConvertTwitter"),
            ProxyPlatform.BLUESKY to json.getBoolean("browserConvertBluesky"),
            ProxyPlatform.REDDIT to json.getBoolean("browserConvertReddit"),
            ProxyPlatform.PINTEREST to json.getBoolean("browserConvertPinterest"),
        )
        return ProxyPlatform.entries.associateWith { platform ->
            if (enabledByPlatform[platform] == true) {
                BrowserFrontendPreference(BrowserConversionMode.READER, targetIds[platform])
            } else {
                BrowserFrontendPreference(
                    BrowserConversionMode.CLEAN_ONLY,
                    targetIds[platform].takeIf { platform in enabledByPlatform },
                )
            }
        }
    }

    private fun migrateRetiredBrowserFrontends(snapshot: SettingsSnapshot): SettingsSnapshot {
        val migrated = snapshot.browserFrontends.mapValues { (platform, preference) ->
            when (preference.targetId) {
                RetiredFrontendMigration.RETIRED_INSTAGRAM_DISABLED_ID -> {
                    require(platform == ProxyPlatform.INSTAGRAM) { "Retired Browser target belongs to a different platform" }
                    BrowserFrontendPreference.CLEAN_ONLY
                }
                RetiredFrontendMigration.RETIRED_FACEBOOK_DISABLED_ID -> {
                    require(platform == ProxyPlatform.FACEBOOK) { "Retired Browser target belongs to a different platform" }
                    BrowserFrontendPreference.CLEAN_ONLY
                }
                else -> preference
            }
        }
        return snapshot.copy(browserFrontends = migrated)
    }

    private fun encodeRememberedRoutes(routes: Map<String, RememberedRoute>): JSONObject =
        JSONObject().apply {
            routes.forEach { (host, route) ->
                put(
                    host,
                    JSONObject()
                        .put("kind", route.kind.wireName)
                        .put("packageName", route.packageName)
                )
            }
        }

    private fun decodeRememberedRoutes(json: JSONObject): Map<String, RememberedRoute> =
        buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val host = keys.next()
                require(host == host.trim().lowercase() && host.isNotBlank()) {
                    "Invalid remembered route host"
                }
                val item = json.getJSONObject(host)
                val kind = RememberedRouteKind.fromWire(item.getString("kind"))
                val rawPackageName = item.getString("packageName")
                val packageName = rawPackageName.trim()
                require(packageName == rawPackageName && packageName.isNotBlank()) {
                    "Remembered route package is invalid"
                }
                put(host, RememberedRoute(kind, packageName))
            }
        }

    private fun encodePlatformStringMap(values: Map<ProxyPlatform, String?>): JSONObject =
        JSONObject().apply {
            ProxyPlatform.entries.forEach { platform ->
                put(platform.name.lowercase(), values[platform])
            }
        }

    private fun decodePlatformStringMap(json: JSONObject): Map<ProxyPlatform, String?> =
        ProxyPlatform.entries.associateWith { platform ->
            if (json.isNull(platform.name.lowercase())) null else json.getString(platform.name.lowercase())
        }

    private fun encodePlatformListMap(values: Map<ProxyPlatform, List<String>>): JSONObject =
        JSONObject().apply {
            ProxyPlatform.entries.forEach { platform ->
                put(platform.name.lowercase(), JSONArray(values[platform].orEmpty()))
            }
        }

    private fun decodePlatformListMap(json: JSONObject): Map<ProxyPlatform, List<String>> =
        ProxyPlatform.entries.associateWith { platform ->
            json.getJSONArray(platform.name.lowercase()).strings()
        }

    private fun encodePlatformSetMap(values: Map<ProxyPlatform, Set<String>>): JSONObject =
        JSONObject().apply {
            ProxyPlatform.entries.forEach { platform ->
                put(platform.name.lowercase(), JSONArray(values[platform].orEmpty().sorted()))
            }
        }

    private fun decodePlatformSetMap(json: JSONObject): Map<ProxyPlatform, Set<String>> =
        ProxyPlatform.entries.associateWith { platform ->
            json.getJSONArray(platform.name.lowercase()).strings().toSet()
        }

    private fun encodeBrowserFrontends(
        values: Map<ProxyPlatform, BrowserFrontendPreference>,
    ): JSONObject = JSONObject().apply {
        ProxyPlatform.entries.forEach { platform ->
            val preference = values.getValue(platform)
            put(
                platform.name.lowercase(),
                JSONObject()
                    .put("mode", preference.mode.name)
                    .put("targetId", preference.targetId ?: JSONObject.NULL),
            )
        }
    }

    private fun decodeBrowserFrontends(
        json: JSONObject,
        customProxies: Map<ProxyPlatform, List<String>>,
    ): Map<ProxyPlatform, BrowserFrontendPreference> =
        ProxyPlatform.entries.associateWith { platform ->
            val item = json.getJSONObject(platform.name.lowercase())
            val rawMode = item.getString("mode")
            val mode = BrowserConversionMode.entries.firstOrNull { it.name == rawMode }
                ?: throw IllegalArgumentException("Unknown Browser conversion mode")
            val targetId = if (item.isNull("targetId")) null else item.getString("targetId")
            if (mode == BrowserConversionMode.CUSTOM && targetId != null) {
                require(targetId.removePrefix("custom:") in customProxies[platform].orEmpty()) {
                    "Browser custom target is absent from imported roster"
                }
            }
            BrowserFrontendPreference(mode, targetId)
        }

    private fun JSONArray.strings(): List<String> = buildList {
        for (i in 0 until length()) add(getString(i))
    }
}
