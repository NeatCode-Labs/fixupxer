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

package com.fixupxer.processing

import com.fixupxer.utils.FrontendRole
import com.fixupxer.utils.FrontendTarget
import com.fixupxer.utils.ProxyPlatform

enum class BrowserConversionMode {
    CLEAN_ONLY,
    READER,
    EMBED,
    CUSTOM,
}

data class BrowserFrontendPreference(
    val mode: BrowserConversionMode,
    val targetId: String?,
) {
    companion object {
        val CLEAN_ONLY = BrowserFrontendPreference(BrowserConversionMode.CLEAN_ONLY, null)
    }
}

/**
 * Browser-specific target policy. It deliberately consumes a supplied roster snapshot so
 * production processing, settings drafts, backup validation and Test Lab can make the same
 * decision without consulting mutable global state halfway through a run.
 */
object BrowserFrontendPolicy {
    fun supportedPlatforms(): List<ProxyPlatform> = ProxyPlatform.entries.filter {
        it in CUSTOM_PLATFORMS
    }

    fun allowedTargets(
        platform: ProxyPlatform,
        targets: List<FrontendTarget>,
    ): List<FrontendTarget> = targets.filter { target ->
        target.platform == platform && when {
            target.id.startsWith(CUSTOM_ID_PREFIX) -> platform in CUSTOM_PLATFORMS
            target.role == FrontendRole.READER -> platform in READER_PLATFORMS
            target.role == FrontendRole.EMBED -> platform in EMBED_PLATFORMS
            else -> false
        }
    }

    fun resolve(
        platform: ProxyPlatform,
        preference: BrowserFrontendPreference,
        activeTargets: List<FrontendTarget>,
    ): FrontendTarget? {
        val allowed = allowedTargets(platform, activeTargets)
        return when (preference.mode) {
            BrowserConversionMode.CLEAN_ONLY -> null
            BrowserConversionMode.READER -> {
                if (platform !in READER_PLATFORMS) return null
                allowed.firstOrNull {
                    it.id == preference.targetId && modeFor(it) == BrowserConversionMode.READER
                } ?: allowed.firstOrNull { modeFor(it) == BrowserConversionMode.READER }
            }
            BrowserConversionMode.EMBED -> {
                if (platform !in EMBED_PLATFORMS) return null
                allowed.firstOrNull {
                    it.id == preference.targetId && modeFor(it) == BrowserConversionMode.EMBED
                }
            }
            BrowserConversionMode.CUSTOM -> {
                if (platform !in CUSTOM_PLATFORMS) return null
                allowed.firstOrNull {
                    it.id == preference.targetId && modeFor(it) == BrowserConversionMode.CUSTOM
                }
            }
        }
    }

    fun modeFor(target: FrontendTarget): BrowserConversionMode = when {
        target.id.startsWith(CUSTOM_ID_PREFIX) -> BrowserConversionMode.CUSTOM
        target.role == FrontendRole.READER -> BrowserConversionMode.READER
        target.role == FrontendRole.EMBED -> BrowserConversionMode.EMBED
        else -> throw IllegalArgumentException("Target ${target.id} is unavailable in Browser mode")
    }

    fun isCombinationAllowed(
        platform: ProxyPlatform,
        preference: BrowserFrontendPreference,
        knownTargets: List<FrontendTarget>,
    ): Boolean = when (preference.mode) {
        BrowserConversionMode.CLEAN_ONLY ->
            preference.targetId == null ||
                (platform in CUSTOM_PLATFORMS && allowedTargets(platform, knownTargets).any {
                    it.id == preference.targetId
                })
        BrowserConversionMode.READER ->
            platform in READER_PLATFORMS &&
                (preference.targetId == null || knownTargets.any {
                    it.id == preference.targetId &&
                        it.platform == platform &&
                        !it.id.startsWith(CUSTOM_ID_PREFIX) &&
                        it.role == FrontendRole.READER
                })
        BrowserConversionMode.EMBED ->
            platform in EMBED_PLATFORMS && knownTargets.any {
                it.id == preference.targetId &&
                    it.platform == platform &&
                    !it.id.startsWith(CUSTOM_ID_PREFIX) &&
                    it.role == FrontendRole.EMBED
            }
        BrowserConversionMode.CUSTOM ->
            platform in CUSTOM_PLATFORMS && knownTargets.any {
                it.id == preference.targetId &&
                    it.platform == platform &&
                    it.id.startsWith(CUSTOM_ID_PREFIX)
            }
    }

    private const val CUSTOM_ID_PREFIX = "custom:"

    private val READER_PLATFORMS = setOf(
        ProxyPlatform.X,
        ProxyPlatform.BLUESKY,
        ProxyPlatform.REDDIT,
        ProxyPlatform.PINTEREST,
    )
    private val EMBED_PLATFORMS = setOf(
        ProxyPlatform.X,
        ProxyPlatform.BLUESKY,
        ProxyPlatform.INSTAGRAM,
        ProxyPlatform.TIKTOK,
    )
    private val CUSTOM_PLATFORMS = setOf(
        ProxyPlatform.X,
        ProxyPlatform.BLUESKY,
        ProxyPlatform.REDDIT,
        ProxyPlatform.PINTEREST,
        ProxyPlatform.INSTAGRAM,
        ProxyPlatform.TIKTOK,
        ProxyPlatform.FACEBOOK,
    )
}
