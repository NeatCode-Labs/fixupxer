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

import com.fixupxer.utils.ProxyPlatform

/**
 * Typed, whitelisted user settings for local backup export/replace.
 * Excludes history, rule snapshots, migration keys and other internal prefs.
 */
data class SettingsSnapshot(
    // Retained for schema-v1 compatibility; exports and restores enforce true.
    val cleanTracking: Boolean,
    val convertTwitter: Boolean,
    val convertInstagram: Boolean,
    val convertTikTok: Boolean,
    val convertBluesky: Boolean,
    val convertFacebook: Boolean,
    val convertReddit: Boolean,
    val convertYoutube: Boolean,
    val convertPinterest: Boolean,
    val convertThreads: Boolean,
    val customRulesEnabled: Boolean,
    val historyEnabled: Boolean,
    val maxHistoryEntries: Int,
    val themeMode: String,
    val dominantHand: String,
    val browserEnabled: Boolean,
    val showConfigurationStatusWidget: Boolean,
    val actionMode: String,
    val actionPriority: List<String>,
    val browserConvertTwitter: Boolean,
    val browserConvertBluesky: Boolean,
    val browserConvertReddit: Boolean,
    val browserConvertPinterest: Boolean,
    val proxySelections: Map<ProxyPlatform, String?>,
    val customProxies: Map<ProxyPlatform, List<String>>,
    val disabledBuiltIns: Map<ProxyPlatform, Set<String>>,
    val browserPrivacyTargetIds: Map<ProxyPlatform, String?>,
    val rememberedRoutes: Map<String, RememberedRoute>,
)
