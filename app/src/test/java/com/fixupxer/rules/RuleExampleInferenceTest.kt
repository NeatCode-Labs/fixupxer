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
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleExampleInferenceTest {
    private lateinit var inference: RuleExampleInference

    @Before
    fun setup() {
        inference = RuleExampleInference(UrlNormalizer())
    }

    @Test
    fun `infers simple named parameter removal`() {
        val result = inferred(
            "https://example.com/article?id=42&utm_source=newsletter",
            "https://example.com/article?id=42"
        )

        assertEquals(RuleAction.RemoveParams(listOf("utm_source")), result.action)
    }

    @Test
    fun `keeps plus-encoded surviving token unchanged`() {
        val result = inferred(
            "https://example.com/?q=a+b&utm_source=mail",
            "https://example.com/?q=a+b"
        )

        assertEquals(RuleAction.RemoveParams(listOf("utm_source")), result.action)
        assertEquals(
            RuleTestVector(
                "https://example.com/?q=a+b&utm_source=mail",
                "https://example.com/?q=a+b"
            ),
            result.draft.testVectors.single()
        )
    }

    @Test
    fun `keeps percent-encoded ampersand surviving token unchanged`() {
        val result = inferred(
            "https://example.com/?q=c%26d&utm_source=mail",
            "https://example.com/?q=c%26d"
        )

        assertEquals(RuleAction.RemoveParams(listOf("utm_source")), result.action)
    }

    @Test
    fun `handles removed and surviving flag parameters`() {
        val result = inferred(
            "https://example.com/?keepFlag&removeFlag&keep=value",
            "https://example.com/?keepFlag&keep=value"
        )

        assertEquals(RuleAction.RemoveParams(listOf("removeFlag")), result.action)
    }

    @Test
    fun `keeps an empty parameter value`() {
        val result = inferred(
            "https://example.com/?a=&tracking=mail",
            "https://example.com/?a="
        )

        assertEquals(RuleAction.RemoveParams(listOf("tracking")), result.action)
    }

    @Test
    fun `allows removing every instance of a duplicate parameter`() {
        val result = inferred(
            "https://example.com/?id=1&tracking=one&tracking=two",
            "https://example.com/?id=1"
        )

        assertEquals(RuleAction.RemoveParams(listOf("tracking")), result.action)
    }

    @Test
    fun `rejects removing only some duplicate parameter instances`() {
        val result = inference.infer(
            "https://example.com/?tracking=one&tracking=two&id=1",
            "https://example.com/?tracking=one&id=1"
        )

        assertRejected(result, RuleExampleInferenceRejectionReason.AMBIGUOUS_DUPLICATES)
    }

    @Test
    fun `rejects reordered surviving tokens`() {
        val result = inference.infer(
            "https://example.com/?id=1&keep=two&tracking=mail",
            "https://example.com/?keep=two&id=1"
        )

        assertRejected(result, RuleExampleInferenceRejectionReason.REORDERED_TOKENS)
    }

    @Test
    fun `rejects changed parameter values`() {
        val result = inference.infer(
            "https://example.com/?id=1&tracking=mail",
            "https://example.com/?id=2"
        )

        assertRejected(result, RuleExampleInferenceRejectionReason.CHANGED_VALUES)
    }

    @Test
    fun `rejects survivors whose raw encoding changed`() {
        // a+b and a%20b decode identically, but the raw token differs.
        val result = inference.infer(
            "https://example.com/?q=a+b&utm_source=x",
            "https://example.com/?q=a%20b"
        )

        assertRejected(result, RuleExampleInferenceRejectionReason.CHANGED_VALUES)
    }

    @Test
    fun `infers removal of every parameter for an empty desired query`() {
        val result = inferred(
            "https://example.com/path?a=1&flag",
            "https://example.com/path"
        )

        assertEquals(RuleAction.RemoveParams(listOf("a", "flag")), result.action)
    }

    @Test
    fun `infers redirect extraction without decoding`() {
        val destination = "https://destination.example/path?encoded=%25"

        val result = inferred(
            "https://wrapper.example/?target=$destination",
            destination
        )

        assertEquals(
            RuleAction.ExtractRedirect("target", decodeMode = RedirectDecodeMode.NONE),
            result.action
        )
    }

    @Test
    fun `infers percent-decoded redirect extraction`() {
        val destination = "https://destination.example/path?message=hello+world"

        val result = inferred(
            "https://wrapper.example/?target=https%3A%2F%2Fdestination.example%2Fpath%3Fmessage%3Dhello+world",
            destination
        )

        assertEquals(
            RuleAction.ExtractRedirect("target", decodeMode = RedirectDecodeMode.PERCENT_ONCE),
            result.action
        )
    }

    @Test
    fun `infers form-decoded redirect extraction`() {
        val destination = "https://destination.example/path?message=hello world"

        val result = inferred(
            "https://wrapper.example/?target=https%3A%2F%2Fdestination.example%2Fpath%3Fmessage%3Dhello+world",
            destination
        )

        assertEquals(
            RuleAction.ExtractRedirect("target", decodeMode = RedirectDecodeMode.FORM_ONCE),
            result.action
        )
    }

    @Test
    fun `infers base64url-decoded redirect extraction`() {
        val destination = "https://destination.example/path?id=42"
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(destination.toByteArray())

        val result = inferred("https://wrapper.example/?target=$encoded", destination)

        assertEquals(
            RuleAction.ExtractRedirect("target", decodeMode = RedirectDecodeMode.BASE64URL),
            result.action
        )
    }

    @Test
    fun `rejects multiple redirect candidates`() {
        val destination = "https://destination.example/path"

        val result = inference.infer(
            "https://wrapper.example/?first=$destination&second=$destination",
            destination
        )

        assertRejected(result, RuleExampleInferenceRejectionReason.MULTIPLE_REDIRECT_CANDIDATES)
    }

    @Test
    fun `rejects non-http redirect output`() {
        val result = inference.infer(
            "https://wrapper.example/?target=ftp%3A%2F%2Fdestination.example%2Ffile",
            "ftp://destination.example/file"
        )

        assertRejected(result, RuleExampleInferenceRejectionReason.INVALID_REDIRECT_OUTPUT)
    }

    @Test
    fun `rejects identical URLs`() {
        val result = inference.infer(
            "https://example.com/?id=1",
            "https://example.com/?id=1"
        )

        assertRejected(result, RuleExampleInferenceRejectionReason.IDENTICAL_URLS)
    }

    @Test
    fun `rejects a different host without a redirect candidate`() {
        val result = inference.infer(
            "https://example.com/path?id=1",
            "https://other.example/path"
        )

        assertRejected(result, RuleExampleInferenceRejectionReason.DIFFERENT_HOST)
    }

    @Test
    fun `rejects a different path without a redirect candidate`() {
        val result = inference.infer(
            "https://example.com/old?id=1",
            "https://example.com/new"
        )

        assertRejected(result, RuleExampleInferenceRejectionReason.DIFFERENT_PATH)
    }

    @Test
    fun `generated draft uses a disabled exact-host main pre-clean rule`() {
        val before = "https://example.com/path?id=1&tracking=mail"
        val desired = "https://example.com/path?id=1"

        val draft = inferred(before, desired).draft

        assertEquals("example.com", (draft.includeScope as RuleScope.ExactHost).host)
        assertEquals(setOf(ProcessingProfile.MAIN), draft.contexts)
        assertEquals(RulePhase.PRE_CLEAN, draft.phase)
        assertFalse(draft.enabled)
        assertFalse(draft.stopAfterMatch)
        assertEquals(listOf(RuleTestVector(before, desired)), draft.testVectors)
    }

    private fun inferred(before: String, desired: String): RuleExampleInferenceResult.Inferred {
        val result = inference.infer(before, desired)
        assertTrue(result is RuleExampleInferenceResult.Inferred)
        return result as RuleExampleInferenceResult.Inferred
    }

    private fun assertRejected(
        result: RuleExampleInferenceResult,
        reason: RuleExampleInferenceRejectionReason
    ) {
        assertTrue(result is RuleExampleInferenceResult.Rejected)
        assertEquals(reason, (result as RuleExampleInferenceResult.Rejected).reason)
    }
}
