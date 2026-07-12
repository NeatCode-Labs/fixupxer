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

import com.fixupxer.utils.Constants
import com.google.re2j.Pattern
import java.net.IDN
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleCompiler @Inject constructor() {
    private val templatePlaceholders = setOf(
        "scheme", "host", "port", "path", "query", "fragment"
    )

    fun compile(rule: CustomUrlRule): CompiledRule {
        validateCommon(rule)
        val includePattern = compileScopePattern(rule.includeScope)
        val excludePatterns = buildMap {
            rule.excludeScopes.forEachIndexed { index, scope ->
                compileScopePattern(scope)?.let { put(index, it) }
            }
        }
        val actionPattern = when (val action = rule.action) {
            is RuleAction.RegexReplace -> compilePattern(action.pattern, action.ignoreCase).also {
                validateReplacement(action.replacement, it)
            }
            else -> null
        }
        validateAction(rule.action)
        return CompiledRule(rule, includePattern, excludePatterns, actionPattern)
    }

    fun compileAll(rules: List<CustomUrlRule>): List<CompiledRule> {
        require(rules.size <= Constants.MAX_CUSTOM_RULES) { "Too many custom rules" }
        require(rules.map { it.id }.distinct().size == rules.size) { "Duplicate rule ID" }
        return rules
            .sortedWith(compareBy<CustomUrlRule>({ it.phase.ordinal }, { it.sortOrder }, { it.id }))
            .map(::compile)
    }

    private fun validateCommon(rule: CustomUrlRule) {
        require(runCatching { UUID.fromString(rule.id) }.isSuccess) { "Invalid rule ID" }
        require(rule.name.trim().isNotEmpty()) { "Rule name is required" }
        require(rule.name.length <= 120) { "Rule name is too long" }
        require(rule.contexts.isNotEmpty()) { "Select at least one processing context" }
        require(rule.excludeScopes.size <= Constants.MAX_EXCLUDES_PER_RULE) {
            "Too many excludes"
        }
        require(rule.testVectors.size <= Constants.MAX_TEST_VECTORS_PER_RULE) {
            "Too many test vectors"
        }
        validateScope(rule.includeScope)
        rule.excludeScopes.forEach(::validateScope)
    }

    private fun validateScope(scope: RuleScope) {
        when (scope) {
            RuleScope.AllUrls -> Unit
            is RuleScope.ExactHost -> normalizeHost(scope.host)
            is RuleScope.DomainAndSubdomains -> {
                val host = normalizeHost(scope.host)
                require(!isIpLiteral(host)) { "IP literals cannot have subdomains" }
            }
            is RuleScope.HostList -> {
                require(scope.entries.isNotEmpty()) { "Host list cannot be empty" }
                require(scope.entries.size <= Constants.MAX_SCOPE_ENTRIES) { "Too many hosts" }
                val normalized = scope.entries.map {
                    val host = normalizeHost(it.host)
                    if (it.mode == HostMatchMode.DOMAIN_AND_SUBDOMAINS) {
                        require(!isIpLiteral(host)) { "IP literals cannot have subdomains" }
                    }
                    it.mode to host
                }
                require(normalized.distinct().size == normalized.size) { "Duplicate host" }
            }
            is RuleScope.UrlRegex -> compilePattern(scope.pattern, scope.ignoreCase)
        }
    }

    private fun compileScopePattern(scope: RuleScope): Pattern? =
        (scope as? RuleScope.UrlRegex)?.let { compilePattern(it.pattern, it.ignoreCase) }

    private fun compilePattern(source: String, ignoreCase: Boolean): Pattern {
        require(source.isNotEmpty()) { "Regex is required" }
        require(source.length <= Constants.MAX_RULE_PATTERN_LENGTH) { "Regex is too long" }
        val flags = if (ignoreCase) Pattern.CASE_INSENSITIVE else 0
        val pattern = Pattern.compile(source, flags)
        require(pattern.programSize() <= Constants.MAX_REGEX_PROGRAM_SIZE) {
            "Regex is too complex"
        }
        return pattern
    }

    private fun validateAction(action: RuleAction) {
        when (action) {
            RuleAction.RemoveAllParams -> Unit
            is RuleAction.RemoveParams -> validateNames(action.names)
            is RuleAction.KeepOnlyParams -> validateNames(action.names, allowEmpty = true)
            is RuleAction.RegexReplace -> {
                require(action.replacement.length <= Constants.MAX_RULE_REPLACEMENT_LENGTH) {
                    "Replacement is too long"
                }
            }
            is RuleAction.ExtractRedirect -> {
                require(action.parameterName.isNotBlank()) { "Redirect parameter is required" }
                require(action.parameterName.length <= 256) { "Redirect parameter is too long" }
            }
            is RuleAction.TemplateRewrite -> validateTemplate(action.template)
        }
    }

    private fun validateNames(names: List<String>, allowEmpty: Boolean = false) {
        if (!allowEmpty) require(names.isNotEmpty()) { "At least one parameter is required" }
        require(names.size <= Constants.MAX_SCOPE_ENTRIES) { "Too many parameter names" }
        require(names.all { it.isNotBlank() && it.length <= 256 }) { "Invalid parameter name" }
        require(names.distinct().size == names.size) { "Duplicate parameter name" }
    }

    private fun validateTemplate(template: String) {
        require(template.isNotBlank()) { "Template is required" }
        require(template.length <= Constants.MAX_RULE_TEMPLATE_LENGTH) { "Template is too long" }
        var cursor = 0
        while (cursor < template.length) {
            val open = template.indexOf('{', cursor)
            if (open < 0) break
            val close = template.indexOf('}', open + 1)
            require(close > open) { "Unclosed template placeholder" }
            val name = template.substring(open + 1, close)
            require(name in templatePlaceholders) { "Unknown template placeholder: $name" }
            cursor = close + 1
        }
    }

    private fun validateReplacement(replacement: String, pattern: Pattern) {
        require(replacement.length <= Constants.MAX_RULE_REPLACEMENT_LENGTH) {
            "Replacement is too long"
        }
        var index = 0
        while (index < replacement.length) {
            when (replacement[index]) {
                '\\' -> index += 2
                '$' -> {
                    require(index + 1 < replacement.length) { "Dangling \$ in replacement" }
                    if (replacement[index + 1] == '{') {
                        val close = replacement.indexOf('}', index + 2)
                        require(close > index + 2) { "Invalid named group reference" }
                        val name = replacement.substring(index + 2, close)
                        require(name in pattern.namedGroups()) { "Unknown capture group: $name" }
                        index = close + 1
                    } else {
                        var end = index + 1
                        while (end < replacement.length && replacement[end].isDigit()) end++
                        require(end > index + 1) { "Invalid group reference" }
                        val group = replacement.substring(index + 1, end).toInt()
                        require(group == 0 || group <= pattern.groupCount()) {
                            "Unknown capture group: $group"
                        }
                        index = end
                    }
                }
                else -> index++
            }
        }
    }

    private fun normalizeHost(value: String): String {
        val host = value.trim().removeSuffix(".")
        require(host.isNotEmpty()) { "Host is required" }
        return if (host.startsWith("[") && host.endsWith("]")) {
            host.substring(1, host.length - 1).lowercase()
        } else {
            IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).lowercase()
        }
    }

    private fun isIpLiteral(host: String): Boolean =
        host.contains(':') || host.split('.').let { parts ->
            parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }
        }
}

@Singleton
class RuleInputValidator @Inject constructor(
    private val compiler: RuleCompiler
) {
    fun validate(rule: CustomUrlRule): Result<CompiledRule> = runCatching {
        compiler.compile(rule.copy(name = rule.name.trim()))
    }
}
