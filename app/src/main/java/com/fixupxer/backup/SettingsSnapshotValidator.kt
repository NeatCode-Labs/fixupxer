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
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.Constants
import com.fixupxer.utils.FrontendRole
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster

/**
 * Semantic validation of a full [SettingsSnapshot]. Shared by the backup codec
 * (decode path) and [PreferencesManager.replaceSettingsSnapshot] (apply path) so
 * an invalid snapshot can never be written: every check throws, nothing is
 * silently filtered.
 *
 * All checks are device-independent (catalog + snapshot data only) except the
 * optional own-package route rejection, which needs the caller's package name.
 */
object SettingsSnapshotValidator {

    private val REQUIRED_ACTIONS = setOf(
        PreferencesManager.ACTION_NATIVE_APP,
        PreferencesManager.ACTION_BROWSER,
        PreferencesManager.ACTION_SHARE_MENU,
        PreferencesManager.ACTION_CLIPBOARD,
    )

    fun validate(snapshot: SettingsSnapshot, ownPackageName: String? = null) {
        val platforms = ProxyPlatform.entries.toSet()
        require(snapshot.proxySelections.keys == platforms) { "Incomplete proxy selections" }
        require(snapshot.customProxies.keys == platforms) { "Incomplete custom proxy map" }
        require(snapshot.disabledBuiltIns.keys == platforms) { "Incomplete disabled built-ins map" }
        require(snapshot.browserPrivacyTargetIds.keys == platforms) {
            "Incomplete browser privacy target map"
        }
        require(
            snapshot.maxHistoryEntries in
                Constants.MIN_HISTORY_ENTRIES..Constants.MAX_HISTORY_ENTRIES
        ) { "Invalid maxHistoryEntries" }
        require(
            snapshot.themeMode in setOf(
                PreferencesManager.THEME_MODE_SYSTEM,
                PreferencesManager.THEME_MODE_LIGHT,
                PreferencesManager.THEME_MODE_DARK,
            )
        ) { "Invalid theme mode" }
        require(
            snapshot.dominantHand in setOf(
                PreferencesManager.DOMINANT_HAND_RIGHT,
                PreferencesManager.DOMINANT_HAND_LEFT,
            )
        ) { "Invalid dominant hand" }
        require(
            snapshot.actionMode in setOf(
                PreferencesManager.ACTION_MODE_ASK,
                PreferencesManager.ACTION_MODE_PRIORITY,
            )
        ) { "Invalid action mode" }
        require(
            snapshot.actionPriority.size == REQUIRED_ACTIONS.size &&
                snapshot.actionPriority.toSet() == REQUIRED_ACTIONS
        ) { "Action priority must be a permutation of the four actions" }

        validateCustomProxies(snapshot)
        validateDisabledBuiltIns(snapshot)
        validatePrivacyTargets(snapshot)
        validateSelections(snapshot)

        require(snapshot.rememberedRoutes.size <= Constants.MAX_REMEMBERED_ROUTES) {
            "Too many remembered routes"
        }
        RememberedRouteValidator.requireValidSnapshotRoutes(snapshot.rememberedRoutes, ownPackageName)
    }

    private fun validateCustomProxies(snapshot: SettingsSnapshot) {
        val catalogDomains = buildList {
            add(Constants.FARSIDE_DOMAIN)
            addAll(Constants.RETIRED_UNSAFE_FRONTEND_DOMAINS)
            ProxyPlatform.entries.forEach { platform ->
                addAll(AlternativeFrontendCatalog.sourceDomains(platform))
                addAll(AlternativeFrontendCatalog.builtIn(platform).map { it.domain })
                addAll(AlternativeFrontendCatalog.legacyDomains(platform))
            }
        }.distinct()

        ProxyPlatform.entries.forEach { platform ->
            val customs = snapshot.customProxies[platform].orEmpty()
            require(customs.size == customs.toSet().size) {
                "Duplicate custom proxy for $platform"
            }
            val otherPlatformCustoms = ProxyPlatform.entries
                .filter { it != platform }
                .flatMap { snapshot.customProxies[it].orEmpty() }
            customs.forEachIndexed { index, domain ->
                require(
                    domain == ProxyRoster.normalizeCustomProxyInput(domain) &&
                        ProxyRoster.isValidProxyDomainFormat(domain)
                ) { "Invalid custom proxy domain format" }
                require(
                    catalogDomains.none { reserved ->
                        UrlNormalizer.hostMatchesDomain(domain, reserved) ||
                            UrlNormalizer.hostMatchesDomain(reserved, domain)
                    }
                ) { "Custom proxy collides with a reserved domain" }
                require(
                    otherPlatformCustoms.none { other ->
                        UrlNormalizer.hostMatchesDomain(domain, other) ||
                            UrlNormalizer.hostMatchesDomain(other, domain)
                    }
                ) { "Custom proxy collides with another platform's custom proxy" }
                require(
                    customs.drop(index + 1).none { other ->
                        UrlNormalizer.hostMatchesDomain(domain, other) ||
                            UrlNormalizer.hostMatchesDomain(other, domain)
                    }
                ) { "Custom proxies on one platform overlap" }
            }
        }
    }

    private fun validateDisabledBuiltIns(snapshot: SettingsSnapshot) {
        ProxyPlatform.entries.forEach { platform ->
            snapshot.disabledBuiltIns[platform].orEmpty().forEach { id ->
                require(AlternativeFrontendCatalog.byId(id)?.platform == platform) {
                    "Disabled built-in id is not a built-in of its platform"
                }
            }
        }
    }

    private fun validatePrivacyTargets(snapshot: SettingsSnapshot) {
        ProxyPlatform.entries.forEach { platform ->
            val targetId = snapshot.browserPrivacyTargetIds[platform] ?: return@forEach
            val target = AlternativeFrontendCatalog.byId(targetId)
            require(target != null && target.platform == platform && target.role == FrontendRole.READER) {
                "Browser privacy target must be a READER of its platform"
            }
        }
    }

    private fun validateSelections(snapshot: SettingsSnapshot) {
        ProxyPlatform.entries.forEach { platform ->
            val selection = snapshot.proxySelections[platform] ?: return@forEach
            val disabled = snapshot.disabledBuiltIns[platform].orEmpty()
            val activeDomains = AlternativeFrontendCatalog.builtIn(platform)
                .filterNot { it.id in disabled }
                .map { it.domain } + snapshot.customProxies[platform].orEmpty()
            require(selection in activeDomains) {
                "Proxy selection is not an active target for its platform"
            }
        }
    }
}
