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

package com.fixupxer.rules

import com.fixupxer.processing.ProcessingProfile
import com.google.re2j.Pattern
import java.util.UUID

enum class RulePhase(val wireName: String) {
    PRE_CLEAN("pre_clean"),
    POST_CLEAN("post_clean"),
    POST_CONVERSION("post_conversion")
}

enum class HostMatchMode(val wireName: String) {
    EXACT("exact"),
    DOMAIN_AND_SUBDOMAINS("domain_and_subdomains")
}

data class HostScopeEntry(
    val host: String,
    val mode: HostMatchMode
)

sealed interface RuleScope {
    data object AllUrls : RuleScope
    data class ExactHost(val host: String) : RuleScope
    data class DomainAndSubdomains(val host: String) : RuleScope
    data class HostList(val entries: List<HostScopeEntry>) : RuleScope
    data class UrlRegex(val pattern: String, val ignoreCase: Boolean = false) : RuleScope
}

enum class ReplaceMode(val wireName: String) {
    FIRST("first"),
    ALL("all")
}

enum class RedirectDecodeMode(val wireName: String) {
    NONE("none"),
    PERCENT_ONCE("percent_once"),
    FORM_ONCE("form_once"),
    BASE64URL("base64url")
}

sealed interface RuleAction {
    data object RemoveAllParams : RuleAction
    data class RemoveParams(
        val names: List<String>,
        val ignoreCase: Boolean = false
    ) : RuleAction

    data class KeepOnlyParams(
        val names: List<String>,
        val ignoreCase: Boolean = false
    ) : RuleAction

    data class RegexReplace(
        val pattern: String,
        val replacement: String,
        val mode: ReplaceMode = ReplaceMode.FIRST,
        val ignoreCase: Boolean = false
    ) : RuleAction

    data class ExtractRedirect(
        val parameterName: String,
        val ignoreCase: Boolean = false,
        val decodeMode: RedirectDecodeMode = RedirectDecodeMode.PERCENT_ONCE
    ) : RuleAction

    data class TemplateRewrite(val template: String) : RuleAction
}

data class RuleTestVector(
    val input: String,
    val expected: String
)

data class CustomUrlRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val phase: RulePhase = RulePhase.POST_CLEAN,
    val contexts: Set<ProcessingProfile> = ProcessingProfile.entries.toSet(),
    val includeScope: RuleScope = RuleScope.AllUrls,
    val excludeScopes: List<RuleScope> = emptyList(),
    val action: RuleAction,
    val stopAfterMatch: Boolean = false,
    val testVectors: List<RuleTestVector> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

data class CompiledRule(
    val rule: CustomUrlRule,
    val includePattern: Pattern? = null,
    val excludePatterns: Map<Int, Pattern> = emptyMap(),
    val actionPattern: Pattern? = null
)

enum class RuleTraceStatus {
    DISABLED,
    CONTEXT_MISS,
    SCOPE_MISS,
    EXCLUDED,
    NO_OP,
    APPLIED,
    INVALID_RULE,
    INVALID_OUTPUT,
    CYCLE,
    HOP_LIMIT
}

data class RuleTraceStep(
    val ruleId: String,
    val ruleName: String,
    val phase: RulePhase,
    val status: RuleTraceStatus,
    val before: String,
    val after: String,
    val message: String? = null
)

data class RuleSnapshot(
    val rules: List<CompiledRule>,
    val revision: Long
) {
    companion object {
        val EMPTY = RuleSnapshot(emptyList(), 0)
    }
}

data class RuleEngineResult(
    val url: String,
    val changed: Boolean,
    val redirectRequested: Boolean,
    val trace: List<RuleTraceStep>
)
