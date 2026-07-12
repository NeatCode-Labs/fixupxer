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

package com.fixupxer

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixupxer.processing.ProcessingProfile
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.rules.CustomRuleEngine
import com.fixupxer.rules.CustomUrlRule
import com.fixupxer.rules.RuleAction
import com.fixupxer.rules.RuleActionExecutor
import com.fixupxer.rules.RuleCompiler
import com.fixupxer.rules.RuleMatcher
import com.fixupxer.rules.RulePhase
import com.fixupxer.rules.RuleScope
import com.fixupxer.rules.RuleSnapshot
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class CustomRulesPerformanceTest {
    @Test
    fun twoHundredCompiledRulesStayInsideShareBudget() {
        val normalizer = UrlNormalizer()
        val engine = CustomRuleEngine(RuleMatcher(normalizer), RuleActionExecutor(normalizer))
        val compiler = RuleCompiler()
        val rules = List(200) { index ->
            CustomUrlRule(
                name = "Performance $index",
                sortOrder = index,
                phase = RulePhase.POST_CLEAN,
                includeScope = RuleScope.UrlRegex("example\\.com/.{0,64}[?&]id="),
                action = RuleAction.RemoveParams(listOf("track$index"))
            )
        }
        lateinit var snapshot: RuleSnapshot
        val cold = measureTimeMillis {
            snapshot = RuleSnapshot(compiler.compileAll(rules), 1)
            engine.applyPhase(
                "https://example.com/path?id=1&utm_source=test",
                RulePhase.POST_CLEAN,
                ProcessingProfile.SHARE,
                snapshot,
                false
            )
        }
        val warm = List(25) {
            measureTimeMillis {
                engine.applyPhase(
                    "https://example.com/path?id=1&utm_source=test",
                    RulePhase.POST_CLEAN,
                    ProcessingProfile.SHARE,
                    snapshot,
                    false
                )
            }
        }.sorted()
        val p95 = warm[(warm.size * 95 / 100).coerceAtMost(warm.lastIndex)]

        assertTrue("Cold custom-rule path took ${cold}ms", cold <= 850)
        assertTrue("Warm p95 custom-rule path took ${p95}ms", p95 <= 850)
    }
}
