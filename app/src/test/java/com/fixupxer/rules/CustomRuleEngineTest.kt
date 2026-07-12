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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CustomRuleEngineTest {
    private lateinit var compiler: RuleCompiler
    private lateinit var engine: CustomRuleEngine

    @Before
    fun setup() {
        val normalizer = UrlNormalizer()
        compiler = RuleCompiler()
        engine = CustomRuleEngine(RuleMatcher(normalizer), RuleActionExecutor(normalizer))
    }

    @Test
    fun `remove params preserves raw tokens duplicates plus and fragment`() {
        val rule = rule(
            scope = RuleScope.DomainAndSubdomains("example.com"),
            action = RuleAction.RemoveParams(listOf("utm_source"))
        )
        val input = "https://sub.example.com/p?q=a+b&q=c%26d&utm_source=x#fragment"

        val result = apply(rule, input)

        assertEquals("https://sub.example.com/p?q=a+b&q=c%26d#fragment", result.url)
        assertTrue(result.changed)
    }

    @Test
    fun `keep only drops empty and flag tokens but keeps duplicate matches`() {
        val rule = rule(action = RuleAction.KeepOnlyParams(listOf("id")))

        val result = apply(rule, "https://example.com/p?&flag&id=1&id=2&x=3")

        assertEquals("https://example.com/p?id=1&id=2", result.url)
    }

    @Test
    fun `exclude wins over include`() {
        val rule = rule(
            scope = RuleScope.DomainAndSubdomains("example.com"),
            excludes = listOf(RuleScope.ExactHost("private.example.com")),
            action = RuleAction.RemoveAllParams
        )

        val result = apply(rule, "https://private.example.com/?x=1")

        assertFalse(result.changed)
        assertEquals(RuleTraceStatus.EXCLUDED, result.trace.single().status)
    }

    @Test
    fun `regex replace supports numbered groups`() {
        val rule = rule(
            action = RuleAction.RegexReplace(
                pattern = "https://example\\.com/u/([0-9]+)",
                replacement = "https://example.com/user/$1"
            )
        )

        val result = apply(rule, "https://example.com/u/42")

        assertEquals("https://example.com/user/42", result.url)
    }

    @Test
    fun `compiler rejects invalid replacement group`() {
        val invalid = rule(
            action = RuleAction.RegexReplace(
                pattern = "(example)",
                replacement = "$2"
            )
        )

        assertThrows(IllegalArgumentException::class.java) { compiler.compile(invalid) }
    }

    @Test
    fun `redirect extraction percent decodes once and requests reentry`() {
        val rule = rule(
            action = RuleAction.ExtractRedirect(
                "url",
                decodeMode = RedirectDecodeMode.PERCENT_ONCE
            )
        )

        val result = apply(
            rule,
            "https://redirect.example/?url=https%3A%2F%2Fexample.com%2Fp%3Fid%3D1"
        )

        assertEquals("https://example.com/p?id=1", result.url)
        assertTrue(result.redirectRequested)
    }

    @Test
    fun `template rewrite expands only URL components`() {
        val rule = rule(
            action = RuleAction.TemplateRewrite(
                "{scheme}://clean.example{path}?{query}#{fragment}"
            )
        )

        val result = apply(rule, "https://example.com/path?id=1#part")

        assertEquals("https://clean.example/path?id=1#part", result.url)
    }

    @Test
    fun `context mismatch skips rule`() {
        val rule = rule(
            contexts = setOf(ProcessingProfile.BROWSER),
            action = RuleAction.RemoveAllParams
        )
        val compiled = compiler.compile(rule)

        val result = engine.applyPhase(
            "https://example.com/?x=1",
            RulePhase.POST_CLEAN,
            ProcessingProfile.MAIN,
            RuleSnapshot(listOf(compiled), 1),
            traceEnabled = true
        )

        assertFalse(result.changed)
        assertEquals(RuleTraceStatus.CONTEXT_MISS, result.trace.single().status)
    }

    @Test
    fun `all scope types use host boundaries and regex find`() {
        val input = "https://a.example.com/path?id=1"
        val scopes = listOf(
            RuleScope.AllUrls,
            RuleScope.ExactHost("a.example.com"),
            RuleScope.DomainAndSubdomains("example.com"),
            RuleScope.HostList(
                listOf(HostScopeEntry("example.com", HostMatchMode.DOMAIN_AND_SUBDOMAINS))
            ),
            RuleScope.UrlRegex("example\\.com/path")
        )

        scopes.forEach { scope ->
            assertTrue(apply(rule(scope = scope, action = RuleAction.RemoveAllParams), input).changed)
        }
        assertFalse(
            apply(
                rule(
                    scope = RuleScope.DomainAndSubdomains("notexample.com"),
                    action = RuleAction.RemoveAllParams
                ),
                input
            ).changed
        )
    }

    @Test
    fun `invalid regex output keeps previous valid URL`() {
        val rule = rule(
            action = RuleAction.RegexReplace(
                pattern = "^https://",
                replacement = "file://"
            )
        )

        val result = apply(rule, "https://example.com/")

        assertEquals("https://example.com/", result.url)
        assertEquals(RuleTraceStatus.INVALID_OUTPUT, result.trace.single().status)
    }

    @Test
    fun `remove action is idempotent across generated raw query corpus`() {
        val compiled = compiler.compile(
            rule(action = RuleAction.RemoveParams(listOf("track"), ignoreCase = true))
        )
        val random = java.util.Random(1234)
        repeat(200) {
            val tokens = buildList {
                repeat(1 + random.nextInt(8)) { index ->
                    add(
                        when (random.nextInt(5)) {
                            0 -> "track=${random.nextInt()}"
                            1 -> "q=a+b"
                            2 -> "q=a%26b"
                            3 -> "flag$index"
                            else -> ""
                        }
                    )
                }
            }
            val input = "https://example.com/?" + tokens.joinToString("&") + "#raw%25"
            val first = engine.applyPhase(
                input,
                RulePhase.POST_CLEAN,
                ProcessingProfile.MAIN,
                RuleSnapshot(listOf(compiled), 1),
                false
            ).url
            val second = engine.applyPhase(
                first,
                RulePhase.POST_CLEAN,
                ProcessingProfile.MAIN,
                RuleSnapshot(listOf(compiled), 1),
                false
            ).url
            assertEquals(first, second)
        }
    }

    private fun apply(rule: CustomUrlRule, input: String): RuleEngineResult {
        val compiled = compiler.compile(rule)
        return engine.applyPhase(
            input,
            rule.phase,
            ProcessingProfile.MAIN,
            RuleSnapshot(listOf(compiled), 1),
            traceEnabled = true
        )
    }

    private fun rule(
        scope: RuleScope = RuleScope.AllUrls,
        excludes: List<RuleScope> = emptyList(),
        contexts: Set<ProcessingProfile> = ProcessingProfile.entries.toSet(),
        action: RuleAction
    ) = CustomUrlRule(
        name = "Test",
        includeScope = scope,
        excludeScopes = excludes,
        contexts = contexts,
        action = action
    )
}
