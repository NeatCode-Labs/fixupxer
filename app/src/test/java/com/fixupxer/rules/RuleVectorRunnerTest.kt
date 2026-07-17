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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleVectorRunnerTest {
    private lateinit var runner: RuleVectorRunner

    @Before
    fun setup() {
        val normalizer = UrlNormalizer()
        val compiler = RuleCompiler()
        runner = RuleVectorRunner(
            compiler,
            CustomRuleEngine(RuleMatcher(normalizer), RuleActionExecutor(normalizer))
        )
    }

    @Test
    fun `vector passes only on exact raw output`() {
        val rule = rule(
            RuleTestVector(
                "https://example.com/?q=a+b&q=c%26d&utm_source=x#fragment",
                "https://example.com/?q=a+b&q=c%26d#fragment"
            )
        )

        val result = runner.run(rule)

        assertTrue(result.allPassed)
        assertEquals("https://example.com/?q=a+b&q=c%26d#fragment", result.results.single().output)
    }

    @Test
    fun `vector fails when output differs only by raw encoding`() {
        val rule = rule(
            RuleTestVector(
                "https://example.com/?q=a+b&utm_source=x",
                "https://example.com/?q=a%20b"
            )
        )

        val result = runner.run(rule)

        assertFalse(result.allPassed)
        assertEquals(1, result.failingCount)
    }

    @Test
    fun `vector uses an enabled configured profile`() {
        val rule = rule(
            RuleTestVector("https://example.com/?x=1", "https://example.com/?x=1"),
            contexts = setOf(ProcessingProfile.BROWSER)
        )

        assertTrue(runner.run(rule).allPassed)
    }

    @Test
    fun `vector runner needs only the compiler and rule engine`() {
        // Isolation by construction: the runner's only collaborators are the
        // compiler and the rule engine, so history, the cleaner cache, and
        // preferences cannot be touched. This test documents that the runner
        // stays functional with nothing else on the classpath wired in.
        val normalizer = UrlNormalizer()
        val isolatedRunner = RuleVectorRunner(
            RuleCompiler(),
            CustomRuleEngine(RuleMatcher(normalizer), RuleActionExecutor(normalizer))
        )
        val rule = rule(
            RuleTestVector(
                "https://example.com/?utm_source=x&x=1",
                "https://example.com/?x=1"
            )
        )

        val result = isolatedRunner.run(rule)

        assertTrue(result.allPassed)
    }

    private fun rule(
        vector: RuleTestVector,
        contexts: Set<ProcessingProfile> = ProcessingProfile.entries.toSet()
    ) = CustomUrlRule(
        name = "Vector rule",
        contexts = contexts,
        action = RuleAction.RemoveParams(listOf("utm_source")),
        testVectors = listOf(vector)
    )
}
