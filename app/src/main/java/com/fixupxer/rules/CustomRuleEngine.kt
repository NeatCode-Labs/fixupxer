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
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.utils.Constants
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

internal data class ActionResult(
    val url: String,
    val redirectRequested: Boolean = false,
    val error: String? = null
)

@Singleton
class RuleMatcher @Inject constructor(
    private val normalizer: UrlNormalizer
) {
    fun matches(compiled: CompiledRule, scope: RuleScope, url: String, excludeIndex: Int? = null): Boolean {
        val parsed = runCatching { normalizer.normalize(url) }.getOrNull() ?: return false
        return when (scope) {
            RuleScope.AllUrls -> true
            is RuleScope.ExactHost -> parsed.asciiHost == normalizeScopeHost(scope.host)
            is RuleScope.DomainAndSubdomains -> hostMatchesDomain(
                parsed.asciiHost,
                normalizeScopeHost(scope.host)
            )
            is RuleScope.HostList -> scope.entries.any {
                val host = normalizeScopeHost(it.host)
                when (it.mode) {
                    HostMatchMode.EXACT -> parsed.asciiHost == host
                    HostMatchMode.DOMAIN_AND_SUBDOMAINS ->
                        hostMatchesDomain(parsed.asciiHost, host)
                }
            }
            is RuleScope.UrlRegex -> {
                val pattern = if (excludeIndex == null) {
                    compiled.includePattern
                } else {
                    compiled.excludePatterns[excludeIndex]
                }
                pattern?.matcher(url)?.find() == true
            }
        }
    }

    private fun normalizeScopeHost(host: String): String =
        java.net.IDN.toASCII(host.trim().removePrefix("[").removeSuffix("]").removeSuffix("."))
            .lowercase()

    private fun hostMatchesDomain(host: String, domain: String): Boolean =
        host == domain || host.endsWith(".$domain")
}

@Singleton
class RuleActionExecutor @Inject constructor(
    private val normalizer: UrlNormalizer
) {
    internal fun execute(compiled: CompiledRule, url: String): ActionResult {
        val action = compiled.rule.action
        val result = runCatching {
            when (action) {
                RuleAction.RemoveAllParams -> ActionResult(removeAllParams(url))
                is RuleAction.RemoveParams -> ActionResult(
                    filterParams(url, action.names, action.ignoreCase, keepMatches = false)
                )
                is RuleAction.KeepOnlyParams -> ActionResult(
                    filterParams(url, action.names, action.ignoreCase, keepMatches = true)
                )
                is RuleAction.RegexReplace -> {
                    val matcher = requireNotNull(compiled.actionPattern).matcher(url)
                    val output = when (action.mode) {
                        ReplaceMode.FIRST -> matcher.replaceFirst(action.replacement)
                        ReplaceMode.ALL -> matcher.replaceAll(action.replacement)
                    }
                    ActionResult(output)
                }
                is RuleAction.ExtractRedirect -> ActionResult(
                    extractRedirect(url, action),
                    redirectRequested = true
                )
                is RuleAction.TemplateRewrite -> ActionResult(expandTemplate(url, action.template))
            }
        }.getOrElse { return ActionResult(url, error = it.message ?: "Rule action failed") }

        if (result.url.length > Constants.MAX_URL_LENGTH ||
            !normalizer.isValidHttpUrl(result.url) ||
            (action is RuleAction.RegexReplace ||
                action is RuleAction.TemplateRewrite ||
                action is RuleAction.ExtractRedirect) &&
            hasMalformedPercent(result.url)
        ) {
            return ActionResult(url, error = "Rule produced an invalid HTTP(S) URL")
        }
        return result
    }

    private fun removeAllParams(url: String): String {
        val queryStart = url.indexOf('?')
        if (queryStart < 0) return url
        val fragmentStart = url.indexOf('#', queryStart)
        return url.substring(0, queryStart) +
            if (fragmentStart >= 0) url.substring(fragmentStart) else ""
    }

    private fun filterParams(
        url: String,
        names: List<String>,
        ignoreCase: Boolean,
        keepMatches: Boolean
    ): String {
        val queryStart = url.indexOf('?')
        if (queryStart < 0) return url
        val fragmentStart = url.indexOf('#', queryStart)
        val rawQuery = url.substring(
            queryStart + 1,
            if (fragmentStart >= 0) fragmentStart else url.length
        )
        val fragment = if (fragmentStart >= 0) url.substring(fragmentStart) else ""
        val tokens = rawQuery.split('&')
        val filtered = tokens.filter { token ->
            val rawName = token.substringBefore('=')
            val decodedName = strictPercentDecode(rawName)
            val matches = decodedName != null && names.any {
                it.equals(decodedName, ignoreCase = ignoreCase)
            }
            if (keepMatches) matches else !matches
        }
        return if (filtered.isEmpty()) {
            url.substring(0, queryStart) + fragment
        } else {
            url.substring(0, queryStart) + "?" + filtered.joinToString("&") + fragment
        }
    }

    private fun extractRedirect(url: String, action: RuleAction.ExtractRedirect): String {
        val queryStart = url.indexOf('?')
        require(queryStart >= 0) { "Redirect parameter not found" }
        val fragmentStart = url.indexOf('#', queryStart)
        val query = url.substring(
            queryStart + 1,
            if (fragmentStart >= 0) fragmentStart else url.length
        )
        val token = query.split('&').firstOrNull {
            val decoded = strictPercentDecode(it.substringBefore('='))
            decoded?.equals(action.parameterName, ignoreCase = action.ignoreCase) == true
        } ?: throw IllegalArgumentException("Redirect parameter not found")
        val value = token.substringAfter('=', "")
        return when (action.decodeMode) {
            RedirectDecodeMode.NONE -> value
            RedirectDecodeMode.PERCENT_ONCE ->
                strictPercentDecode(value) ?: throw IllegalArgumentException("Invalid encoding")
            RedirectDecodeMode.FORM_ONCE ->
                URLDecoder.decode(value, StandardCharsets.UTF_8.name())
            RedirectDecodeMode.BASE64URL -> {
                val padded = value + "=".repeat((4 - value.length % 4) % 4)
                String(
                    Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP),
                    StandardCharsets.UTF_8
                )
            }
        }
    }

    private fun expandTemplate(url: String, template: String): String {
        val parsed = normalizer.normalize(url)
        return template
            .replace("{scheme}", parsed.scheme)
            .replace("{host}", parsed.asciiHost)
            .replace("{port}", parsed.port?.let { ":$it" } ?: "")
            .replace("{path}", parsed.rawPath)
            .replace("{query}", parsed.rawQuery ?: "")
            .replace("{fragment}", parsed.rawFragment ?: "")
    }

    private fun strictPercentDecode(value: String): String? = runCatching {
        val output = ByteArrayOutputStream(value.length)
        var index = 0
        while (index < value.length) {
            if (value[index] == '%') {
                require(index + 2 < value.length)
                val byte = value.substring(index + 1, index + 3).toInt(16)
                output.write(byte)
                index += 3
            } else {
                output.write(value[index].toString().toByteArray(StandardCharsets.UTF_8))
                index++
            }
        }
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(output.toByteArray()))
            .toString()
    }.getOrNull()

    private fun hasMalformedPercent(value: String): Boolean {
        var index = value.indexOf('%')
        while (index >= 0) {
            if (index + 2 >= value.length ||
                value.substring(index + 1, index + 3).toIntOrNull(16) == null
            ) {
                return true
            }
            index = value.indexOf('%', index + 3)
        }
        return false
    }
}

@Singleton
class CustomRuleEngine @Inject constructor(
    private val matcher: RuleMatcher,
    private val executor: RuleActionExecutor
) {
    fun applyPhase(
        url: String,
        phase: RulePhase,
        profile: ProcessingProfile,
        snapshot: RuleSnapshot,
        traceEnabled: Boolean
    ): RuleEngineResult {
        var current = url
        var changed = false
        var redirect = false
        val trace = mutableListOf<RuleTraceStep>()

        snapshot.rules
            .asSequence()
            .filter { it.rule.phase == phase }
            .forEach { compiled ->
                val rule = compiled.rule
                val before = current
                val status: RuleTraceStatus
                var message: String? = null

                when {
                    !rule.enabled -> status = RuleTraceStatus.DISABLED
                    profile !in rule.contexts -> status = RuleTraceStatus.CONTEXT_MISS
                    !matcher.matches(compiled, rule.includeScope, current) ->
                        status = RuleTraceStatus.SCOPE_MISS
                    rule.excludeScopes.anyIndexed { index, scope ->
                        matcher.matches(compiled, scope, current, index)
                    } -> status = RuleTraceStatus.EXCLUDED
                    else -> {
                        val result = executor.execute(compiled, current)
                        if (result.error != null) {
                            status = RuleTraceStatus.INVALID_OUTPUT
                            message = result.error
                        } else if (result.url == current) {
                            status = RuleTraceStatus.NO_OP
                        } else {
                            current = result.url
                            changed = true
                            redirect = redirect || result.redirectRequested
                            status = RuleTraceStatus.APPLIED
                        }
                    }
                }

                if (traceEnabled && trace.size < Constants.MAX_TRACE_STEPS) {
                    trace += RuleTraceStep(
                        rule.id,
                        rule.name,
                        phase,
                        status,
                        before,
                        current,
                        message
                    )
                }
                if (status == RuleTraceStatus.APPLIED && rule.stopAfterMatch) {
                    return RuleEngineResult(current, changed, redirect, trace)
                }
            }

        return RuleEngineResult(current, changed, redirect, trace)
    }

    private inline fun <T> Iterable<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
        forEachIndexed { index, item -> if (predicate(index, item)) return true }
        return false
    }
}
