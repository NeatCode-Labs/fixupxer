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

import com.fixupxer.BuildConfig
import com.fixupxer.data.database.CustomRuleEntity
import com.fixupxer.processing.ProcessingProfile
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class RuleBundle(
    val schemaVersion: Int,
    val rules: List<CustomUrlRule>
)

@Singleton
class RuleBundleCodec @Inject constructor() {
    companion object {
        const val FORMAT_ID = "fixupxer-custom-rules"
        const val SCHEMA_VERSION = 1
    }

    fun encodeBundle(rules: List<CustomUrlRule>): String = JSONObject()
        .put("format", FORMAT_ID)
        .put("schemaVersion", SCHEMA_VERSION)
        .put("appVersion", BuildConfig.VERSION_NAME)
        .put("rules", JSONArray().apply { rules.forEach { put(encodeRule(it)) } })
        .toString(2)

    fun decodeBundle(json: String): RuleBundle {
        val root = JSONObject(json)
        require(root.getString("format") == FORMAT_ID) { "Unsupported rule bundle format" }
        val version = root.getInt("schemaVersion")
        require(version == SCHEMA_VERSION) { "Unsupported rule bundle schema: $version" }
        val array = root.getJSONArray("rules")
        val rules = buildList {
            for (i in 0 until array.length()) add(decodeRule(array.getJSONObject(i)))
        }
        require(rules.map { it.id }.distinct().size == rules.size) {
            "Rule bundle contains duplicate IDs"
        }
        return RuleBundle(version, rules)
    }

    fun toEntity(rule: CustomUrlRule): CustomRuleEntity = CustomRuleEntity(
        id = rule.id,
        name = rule.name,
        enabled = rule.enabled,
        sortOrder = rule.sortOrder,
        phase = rule.phase.wireName,
        contextsJson = JSONArray(rule.contexts.map { it.name }).toString(),
        includeScopeJson = encodeScope(rule.includeScope).toString(),
        excludeScopesJson = JSONArray().apply {
            rule.excludeScopes.forEach { put(encodeScope(it)) }
        }.toString(),
        actionJson = encodeAction(rule.action).toString(),
        stopAfterMatch = rule.stopAfterMatch,
        testVectorsJson = JSONArray().apply {
            rule.testVectors.forEach {
                put(JSONObject().put("input", it.input).put("expected", it.expected))
            }
        }.toString(),
        createdAt = rule.createdAt,
        updatedAt = rule.updatedAt
    )

    fun fromEntity(entity: CustomRuleEntity): CustomUrlRule {
        val contextsArray = JSONArray(entity.contextsJson)
        val contexts = buildSet {
            for (i in 0 until contextsArray.length()) {
                add(ProcessingProfile.valueOf(contextsArray.getString(i)))
            }
        }
        val excludesArray = JSONArray(entity.excludeScopesJson)
        val excludes = buildList {
            for (i in 0 until excludesArray.length()) {
                add(decodeScope(excludesArray.getJSONObject(i)))
            }
        }
        val testsArray = JSONArray(entity.testVectorsJson)
        val tests = buildList {
            for (i in 0 until testsArray.length()) {
                val item = testsArray.getJSONObject(i)
                add(RuleTestVector(item.getString("input"), item.getString("expected")))
            }
        }
        return CustomUrlRule(
            id = entity.id,
            name = entity.name,
            enabled = entity.enabled,
            sortOrder = entity.sortOrder,
            phase = RulePhase.entries.first { it.wireName == entity.phase },
            contexts = contexts,
            includeScope = decodeScope(JSONObject(entity.includeScopeJson)),
            excludeScopes = excludes,
            action = decodeAction(JSONObject(entity.actionJson)),
            stopAfterMatch = entity.stopAfterMatch,
            testVectors = tests,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    private fun encodeRule(rule: CustomUrlRule): JSONObject = JSONObject()
        .put("id", rule.id)
        .put("name", rule.name)
        .put("enabled", rule.enabled)
        .put("sortOrder", rule.sortOrder)
        .put("phase", rule.phase.wireName)
        .put("contexts", JSONArray(rule.contexts.map { it.name }))
        .put("includeScope", encodeScope(rule.includeScope))
        .put("excludeScopes", JSONArray().apply {
            rule.excludeScopes.forEach { put(encodeScope(it)) }
        })
        .put("action", encodeAction(rule.action))
        .put("stopAfterMatch", rule.stopAfterMatch)
        .put("testVectors", JSONArray().apply {
            rule.testVectors.forEach {
                put(JSONObject().put("input", it.input).put("expected", it.expected))
            }
        })
        .put("createdAt", rule.createdAt)
        .put("updatedAt", rule.updatedAt)

    private fun decodeRule(json: JSONObject): CustomUrlRule {
        val contextsJson = json.getJSONArray("contexts")
        val contexts = buildSet {
            for (i in 0 until contextsJson.length()) {
                add(ProcessingProfile.valueOf(contextsJson.getString(i)))
            }
        }
        val excludesJson = json.optJSONArray("excludeScopes") ?: JSONArray()
        val excludes = buildList {
            for (i in 0 until excludesJson.length()) {
                add(decodeScope(excludesJson.getJSONObject(i)))
            }
        }
        val testsJson = json.optJSONArray("testVectors") ?: JSONArray()
        val tests = buildList {
            for (i in 0 until testsJson.length()) {
                val test = testsJson.getJSONObject(i)
                add(RuleTestVector(test.getString("input"), test.getString("expected")))
            }
        }
        return CustomUrlRule(
            id = json.getString("id"),
            name = json.getString("name"),
            enabled = json.optBoolean("enabled", true),
            sortOrder = json.optInt("sortOrder", 0),
            phase = RulePhase.entries.first { it.wireName == json.getString("phase") },
            contexts = contexts,
            includeScope = decodeScope(json.getJSONObject("includeScope")),
            excludeScopes = excludes,
            action = decodeAction(json.getJSONObject("action")),
            stopAfterMatch = json.optBoolean("stopAfterMatch", false),
            testVectors = tests,
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }

    private fun encodeScope(scope: RuleScope): JSONObject = when (scope) {
        RuleScope.AllUrls -> JSONObject().put("type", "all_urls")
        is RuleScope.ExactHost -> JSONObject()
            .put("type", "exact_host")
            .put("host", scope.host)
        is RuleScope.DomainAndSubdomains -> JSONObject()
            .put("type", "domain_and_subdomains")
            .put("host", scope.host)
        is RuleScope.HostList -> JSONObject()
            .put("type", "host_list")
            .put("entries", JSONArray().apply {
                scope.entries.forEach {
                    put(
                        JSONObject()
                            .put("host", it.host)
                            .put("mode", it.mode.wireName)
                    )
                }
            })
        is RuleScope.UrlRegex -> JSONObject()
            .put("type", "url_regex")
            .put("pattern", scope.pattern)
            .put("ignoreCase", scope.ignoreCase)
    }

    private fun decodeScope(json: JSONObject): RuleScope = when (json.getString("type")) {
        "all_urls" -> RuleScope.AllUrls
        "exact_host" -> RuleScope.ExactHost(json.getString("host"))
        "domain_and_subdomains" -> RuleScope.DomainAndSubdomains(json.getString("host"))
        "host_list" -> {
            val entriesJson = json.getJSONArray("entries")
            RuleScope.HostList(buildList {
                for (i in 0 until entriesJson.length()) {
                    val item = entriesJson.getJSONObject(i)
                    add(
                        HostScopeEntry(
                            host = item.getString("host"),
                            mode = HostMatchMode.entries.first {
                                it.wireName == item.getString("mode")
                            }
                        )
                    )
                }
            })
        }
        "url_regex" -> RuleScope.UrlRegex(
            json.getString("pattern"),
            json.optBoolean("ignoreCase", false)
        )
        else -> throw IllegalArgumentException("Unknown rule scope")
    }

    private fun encodeAction(action: RuleAction): JSONObject = when (action) {
        RuleAction.RemoveAllParams -> JSONObject().put("type", "remove_all_params")
        is RuleAction.RemoveParams -> JSONObject()
            .put("type", "remove_params")
            .put("names", JSONArray(action.names))
            .put("ignoreCase", action.ignoreCase)
        is RuleAction.KeepOnlyParams -> JSONObject()
            .put("type", "keep_only_params")
            .put("names", JSONArray(action.names))
            .put("ignoreCase", action.ignoreCase)
        is RuleAction.RegexReplace -> JSONObject()
            .put("type", "regex_replace")
            .put("pattern", action.pattern)
            .put("replacement", action.replacement)
            .put("mode", action.mode.wireName)
            .put("ignoreCase", action.ignoreCase)
        is RuleAction.ExtractRedirect -> JSONObject()
            .put("type", "extract_redirect")
            .put("parameterName", action.parameterName)
            .put("ignoreCase", action.ignoreCase)
            .put("decodeMode", action.decodeMode.wireName)
        is RuleAction.TemplateRewrite -> JSONObject()
            .put("type", "template_rewrite")
            .put("template", action.template)
    }

    private fun decodeAction(json: JSONObject): RuleAction = when (json.getString("type")) {
        "remove_all_params" -> RuleAction.RemoveAllParams
        "remove_params" -> RuleAction.RemoveParams(
            json.getJSONArray("names").strings(),
            json.optBoolean("ignoreCase", false)
        )
        "keep_only_params" -> RuleAction.KeepOnlyParams(
            json.getJSONArray("names").strings(),
            json.optBoolean("ignoreCase", false)
        )
        "regex_replace" -> RuleAction.RegexReplace(
            pattern = json.getString("pattern"),
            replacement = json.getString("replacement"),
            mode = ReplaceMode.entries.first { it.wireName == json.getString("mode") },
            ignoreCase = json.optBoolean("ignoreCase", false)
        )
        "extract_redirect" -> RuleAction.ExtractRedirect(
            parameterName = json.getString("parameterName"),
            ignoreCase = json.optBoolean("ignoreCase", false),
            decodeMode = RedirectDecodeMode.entries.first {
                it.wireName == json.getString("decodeMode")
            }
        )
        "template_rewrite" -> RuleAction.TemplateRewrite(json.getString("template"))
        else -> throw IllegalArgumentException("Unknown rule action")
    }

    private fun JSONArray.strings(): List<String> = buildList {
        for (i in 0 until length()) add(getString(i))
    }
}
