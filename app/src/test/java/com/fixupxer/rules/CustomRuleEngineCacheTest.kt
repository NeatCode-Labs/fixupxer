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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CustomRuleEngineCacheTest {
    private lateinit var compiler: RuleCompiler
    private lateinit var engine: CustomRuleEngine
    private lateinit var executor: RuleActionExecutor

    @Before
    fun setUp() {
        val normalizer = UrlNormalizer()
        compiler = RuleCompiler()
        executor = RuleActionExecutor(normalizer)
        engine = CustomRuleEngine(RuleMatcher(normalizer), executor)
    }

    @Test
    fun `phase cache refreshes parsed URL after host and query rewrite`() {
        val rewrite = rule(
            name = "rewrite",
            sortOrder = 0,
            action = RuleAction.TemplateRewrite("https://new.example{path}?id=1")
        )
        val remove = rule(
            name = "remove",
            sortOrder = 1,
            scope = RuleScope.ExactHost("new.example"),
            action = RuleAction.RemoveParams(listOf("id"))
        )

        val result = apply(
            "https://old.example/path?old=1",
            rewrite,
            remove
        )

        assertEquals("https://new.example/path", result.url)
        assertTrue(result.changed)
        assertEquals(
            listOf(RuleTraceStatus.APPLIED, RuleTraceStatus.APPLIED),
            result.trace.map { it.status }
        )
    }

    @Test
    fun `regex match cache is invalidated after a URL change`() {
        val oldHost = RuleScope.UrlRegex("^https://old\\.example/")
        val result = apply(
            "https://old.example/path?id=1",
            rule("rewrite", sortOrder = 0, scope = oldHost,
                action = RuleAction.TemplateRewrite("https://new.example{path}?{query}")),
            rule("old host must miss", sortOrder = 1, scope = oldHost,
                action = RuleAction.TemplateRewrite("https://wrong.example{path}")),
            rule("new host", sortOrder = 2, scope = RuleScope.UrlRegex("^https://new\\.example/"),
                action = RuleAction.RemoveParams(listOf("id")))
        )

        assertEquals("https://new.example/path", result.url)
        assertEquals(
            listOf(RuleTraceStatus.APPLIED, RuleTraceStatus.SCOPE_MISS, RuleTraceStatus.APPLIED),
            result.trace.map { it.status }
        )
    }

    @Test
    fun `invalid output leaves current URL for later rules`() {
        val invalid = rule(
            name = "invalid",
            sortOrder = 0,
            action = RuleAction.RegexReplace("^https://", "file://")
        )
        val cleanup = rule(
            name = "cleanup",
            sortOrder = 1,
            scope = RuleScope.ExactHost("old.example"),
            action = RuleAction.RemoveParams(listOf("track"))
        )

        val result = apply(
            "https://old.example/path?track=1",
            invalid,
            cleanup
        )

        assertEquals("https://old.example/path", result.url)
        assertTrue(result.invalidOutput)
        assertEquals(
            listOf(RuleTraceStatus.INVALID_OUTPUT, RuleTraceStatus.APPLIED),
            result.trace.map { it.status }
        )
        assertEquals(
            "https://old.example/path?track=1",
            result.trace[1].before
        )
    }

    @Test
    fun `direct executor still validates an invalid URL when input is not prevalidated`() {
        val compiled = compiler.compile(
            rule(
                name = "direct",
                action = RuleAction.RemoveAllParams
            )
        )

        val result = executor.execute(compiled, "not-a-url")

        assertEquals("not-a-url", result.url)
        assertNotNull(result.error)
    }

    @Test
    fun `regex identity keeps malformed percent validation after scope matching`() {
        val malformed = "https://example.com/path?value=%ZZ"

        val result = apply(
            malformed,
            rule(
                name = "identity",
                scope = RuleScope.ExactHost("example.com"),
                action = RuleAction.RegexReplace(
                    pattern = "^https://example\\.com",
                    replacement = "https://example.com"
                )
            )
        )

        assertEquals(malformed, result.url)
        assertTrue(result.invalidOutput)
        assertEquals(RuleTraceStatus.INVALID_OUTPUT, result.trace.single().status)
    }

    private fun apply(input: String, vararg rules: CustomUrlRule): RuleEngineResult {
        val snapshot = RuleSnapshot(compiler.compileAll(rules.toList()), revision = 1)
        return engine.applyPhase(
            input,
            RulePhase.POST_CLEAN,
            ProcessingProfile.MAIN,
            snapshot,
            traceEnabled = true
        )
    }

    private fun rule(
        name: String,
        sortOrder: Int = 0,
        scope: RuleScope = RuleScope.AllUrls,
        action: RuleAction
    ) = CustomUrlRule(
        name = name,
        sortOrder = sortOrder,
        includeScope = scope,
        action = action
    )
}
