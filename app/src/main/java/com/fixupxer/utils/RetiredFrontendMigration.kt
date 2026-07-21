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


package com.fixupxer.utils

import com.fixupxer.backup.SettingsSnapshot

/**
 * One-time migration for retired unsafe embed frontends (2026-07 security audit).
 * Shared by [com.fixupxer.PreferencesManager] init and [com.fixupxer.backup.LocalBackupCodec] decode.
 */
object RetiredFrontendMigration {

    const val RETIRED_INSTAGRAM_DISABLED_ID = "ig_kkinstagram"
    const val RETIRED_FACEBOOK_DISABLED_ID = "fb_facebookez"

    fun firstActiveInstagramDomain(
        disabledBuiltInIds: Set<String>,
        customProxies: List<String>,
    ): String? {
        val activeBuiltIns = AlternativeFrontendCatalog.builtIn(ProxyPlatform.INSTAGRAM)
            .filterNot { it.id in disabledBuiltInIds }
        val defaultDomain = AlternativeFrontendCatalog.defaultTarget(ProxyPlatform.INSTAGRAM)?.domain
        if (defaultDomain != null && activeBuiltIns.any { it.domain == defaultDomain }) {
            return defaultDomain
        }
        return activeBuiltIns.firstOrNull()?.domain ?: customProxies.firstOrNull()
    }

    fun migrateDisabledBuiltIns(
        disabled: Map<ProxyPlatform, Set<String>>,
    ): Map<ProxyPlatform, Set<String>> =
        disabled.mapValues { (platform, ids) ->
            when (platform) {
                ProxyPlatform.INSTAGRAM -> ids - RETIRED_INSTAGRAM_DISABLED_ID
                ProxyPlatform.FACEBOOK -> ids - RETIRED_FACEBOOK_DISABLED_ID
                else -> ids
            }
        }

    fun migrateSnapshot(snapshot: SettingsSnapshot): SettingsSnapshot {
        val disabled = migrateDisabledBuiltIns(snapshot.disabledBuiltIns)
        val selections = snapshot.proxySelections.toMutableMap()

        if (selections[ProxyPlatform.INSTAGRAM] == Constants.KKINSTAGRAM_DOMAIN) {
            selections[ProxyPlatform.INSTAGRAM] = firstActiveInstagramDomain(
                disabled[ProxyPlatform.INSTAGRAM].orEmpty(),
                snapshot.customProxies[ProxyPlatform.INSTAGRAM].orEmpty(),
            )
        }

        var convertFacebook = snapshot.convertFacebook
        if (selections[ProxyPlatform.FACEBOOK] == Constants.FACEBOOKEZ_DOMAIN) {
            selections[ProxyPlatform.FACEBOOK] = null
            if (snapshot.customProxies[ProxyPlatform.FACEBOOK].orEmpty().isEmpty()) {
                convertFacebook = false
            }
        }

        return snapshot.copy(
            disabledBuiltIns = disabled,
            proxySelections = selections,
            convertFacebook = convertFacebook,
        )
    }
}
