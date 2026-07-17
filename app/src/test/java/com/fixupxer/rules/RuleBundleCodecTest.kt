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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
class RuleBundleCodecTest {
    private val codec = RuleBundleCodec()
    private val compiler = RuleCompiler()

    @Test
    fun `bundled templates parse and compile`() {
        val assets = RuntimeEnvironment.getApplication().assets
        listOf("privacy_basics.json").forEach { file ->
            val json = assets.open("rule_templates/$file").bufferedReader().use { it.readText() }
            val bundle = codec.decodeBundle(json)
            compiler.compileAll(bundle.rules)
        }
    }

    @Test
    fun `bundle round trip preserves stable rule fields`() {
        val rule = CustomUrlRule(
            name = "Round trip",
            includeScope = RuleScope.UrlRegex("^https://example\\.com"),
            excludeScopes = listOf(RuleScope.ExactHost("private.example.com")),
            action = RuleAction.KeepOnlyParams(listOf("id"), ignoreCase = true),
            testVectors = listOf(
                RuleTestVector("https://example.com/?id=1&x=2", "https://example.com/?id=1"),
                RuleTestVector(
                    "https://example.com/?q=a+b&path=%2Fone%2Ftwo",
                    "https://example.com/?q=a+b&path=%2Fone%2Ftwo"
                )
            )
        )

        val decoded = codec.decodeBundle(codec.encodeBundle(listOf(rule))).rules.single()

        assertEquals(rule, decoded)
        assertEquals(
            rule.testVectors[1].input.toByteArray(StandardCharsets.UTF_8).toList(),
            decoded.testVectors[1].input.toByteArray(StandardCharsets.UTF_8).toList()
        )
        assertEquals(
            rule.testVectors[1].expected.toByteArray(StandardCharsets.UTF_8).toList(),
            decoded.testVectors[1].expected.toByteArray(StandardCharsets.UTF_8).toList()
        )
    }
}
