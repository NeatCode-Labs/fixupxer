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

import com.fixupxer.rules.RuleTraceStep
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.ProxyPlatform

enum class ProcessingProfile {
    MAIN,
    SHARE,
    BROWSER
}

object BrowserConversionPolicy {
    fun shouldConvert(
        platform: ProxyPlatform?,
        toggleEnabled: Boolean,
        hasActiveTarget: Boolean,
    ): Boolean =
        platform != null &&
            platform in AlternativeFrontendCatalog.privacyCapablePlatforms() &&
            toggleEnabled &&
            hasActiveTarget
}

data class ProxySelections(val byPlatform: Map<ProxyPlatform, String?>) {
    fun domainFor(platform: ProxyPlatform): String? = byPlatform[platform]

    companion object {
        val EMPTY = ProxySelections(emptyMap())

        /** Catalog defaults — used by legacy [com.fixupxer.UrlProcessor] test helpers. */
        val DEFAULT: ProxySelections = ProxySelections(
            ProxyPlatform.entries.associateWith { platform ->
                AlternativeFrontendCatalog.defaultTarget(platform)?.domain
            }
        )
    }
}

data class ProcessingOptions(
    val profile: ProcessingProfile,
    val cleanTracking: Boolean,
    val convertDomains: Boolean,
    val proxySelections: ProxySelections,
    val customRulesEnabled: Boolean,
    val persistHistory: Boolean = true,
    val useCache: Boolean = true,
    val traceEnabled: Boolean = false
)

data class PipelineProcessingResult(
    val originalUrl: String,
    val url: String,
    val wasAlreadyClean: Boolean,
    val builtinChanged: Boolean,
    val domainConverted: Boolean,
    val customRuleChanged: Boolean,
    val rulesRevision: Long,
    val trace: List<RuleTraceStep>,
    val operations: List<ChangeOperation>,
    /**
     * Exact URLs used as cleaner-cache keys during this run (only when caching
     * was enabled). Needed to evict entries when a leak shows up in the output:
     * custom PRE_CLEAN rules or redirect re-entries can make these differ from
     * [originalUrl].
     */
    val cleanerCacheKeys: List<String> = emptyList(),
    /** ASCII host after cleaning/custom rules, before optional frontend conversion. */
    val routingHost: String? = null,
)
