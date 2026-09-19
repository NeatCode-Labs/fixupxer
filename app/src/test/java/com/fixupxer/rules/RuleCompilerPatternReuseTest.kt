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
import com.google.re2j.PatternSyntaxException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleCompilerPatternReuseTest {
    private val compiler = RuleCompiler()

    @Test
    fun `same source with different flags keeps distinct matching behavior`() {
        val source = "example\\.com/path"
        val compiled = compiler.compileAll(
            listOf(
                rule(
                    name = "case-sensitive",
                    sortOrder = 0,
                    scope = RuleScope.UrlRegex(source, ignoreCase = false)
                ),
                rule(
                    name = "case-insensitive",
                    sortOrder = 1,
                    scope = RuleScope.UrlRegex(source, ignoreCase = true)
                )
            )
        )

        val sensitive = requireNotNull(compiled[0].includePattern)
        val insensitive = requireNotNull(compiled[1].includePattern)
        val candidate = "https://EXAMPLE.com/path"

        assertNotSame(sensitive, insensitive)
        assertFalse(sensitive.matcher(candidate).find())
        assertTrue(insensitive.matcher(candidate).find())
    }

    @Test
    fun `compileAll reuses identical pattern across include excludes and action`() {
        val shared = "example(\\.com)"
        val other = "private\\.example\\.com"
        val first = rule(
            name = "shared-pattern",
            sortOrder = 0,
            scope = RuleScope.UrlRegex(shared),
            excludes = listOf(
                RuleScope.UrlRegex(other),
                RuleScope.UrlRegex(shared)
            ),
            action = RuleAction.RegexReplace(shared, "example.test$1")
        )
        val second = rule(
            name = "same-pattern-again",
            sortOrder = 1,
            scope = RuleScope.UrlRegex(shared)
        )

        val compiled = compiler.compileAll(listOf(first, second))
        val firstCompiled = compiled[0]
        val secondCompiled = compiled[1]
        val sharedPattern = requireNotNull(firstCompiled.includePattern)

        assertSame(sharedPattern, firstCompiled.actionPattern)
        assertSame(sharedPattern, firstCompiled.excludePatterns[1])
        assertSame(sharedPattern, secondCompiled.includePattern)
        assertNotSame(sharedPattern, firstCompiled.excludePatterns[0])
        assertEquals(setOf(0, 1), firstCompiled.excludePatterns.keys)

        val matcher = RuleMatcher(UrlNormalizer())
        val input = "https://example.com/path"
        assertFalse(matcher.matches(firstCompiled, first.excludeScopes[0], input, 0))
        assertTrue(matcher.matches(firstCompiled, first.excludeScopes[1], input, 1))
    }

    @Test
    fun `pattern cache is isolated between compile calls`() {
        val rule = rule(
            name = "isolated",
            scope = RuleScope.UrlRegex("example\\.com")
        )

        val firstCompile = compiler.compile(rule)
        val secondCompile = compiler.compile(rule)
        val firstBatch = compiler.compileAll(listOf(rule)).single()
        val secondBatch = compiler.compileAll(listOf(rule)).single()

        assertNotSame(
            requireNotNull(firstCompile.includePattern),
            requireNotNull(secondCompile.includePattern)
        )
        assertNotSame(
            requireNotNull(firstBatch.includePattern),
            requireNotNull(secondBatch.includePattern)
        )
    }

    @Test
    fun `cached pattern does not bypass replacement validation`() {
        val source = "(example)\\.com"
        val valid = rule(
            name = "valid-replacement",
            sortOrder = 0,
            action = RuleAction.RegexReplace(source, "example.test$1")
        )
        val invalid = rule(
            name = "invalid-replacement",
            sortOrder = 1,
            action = RuleAction.RegexReplace(source, "$2")
        )

        assertThrows(IllegalArgumentException::class.java) {
            compiler.compileAll(listOf(valid, invalid))
        }
    }

    @Test
    fun `scope validation still runs after the pattern cache is populated`() {
        val seed = rule(
            name = "seed",
            sortOrder = 0,
            scope = RuleScope.UrlRegex("example\\.com")
        )

        assertThrows(PatternSyntaxException::class.java) {
            compiler.compileAll(
                listOf(
                    seed,
                    rule(
                        name = "invalid-regex",
                        sortOrder = 1,
                        scope = RuleScope.UrlRegex("[")
                    )
                )
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            compiler.compileAll(
                listOf(
                    seed,
                    rule(
                        name = "invalid-host-list",
                        sortOrder = 1,
                        scope = RuleScope.HostList(emptyList())
                    )
                )
            )
        }
    }

    private fun rule(
        name: String,
        sortOrder: Int = 0,
        scope: RuleScope = RuleScope.AllUrls,
        excludes: List<RuleScope> = emptyList(),
        action: RuleAction = RuleAction.RemoveAllParams
    ) = CustomUrlRule(
        name = name,
        sortOrder = sortOrder,
        contexts = ProcessingProfile.entries.toSet(),
        includeScope = scope,
        excludeScopes = excludes,
        action = action
    )
}
