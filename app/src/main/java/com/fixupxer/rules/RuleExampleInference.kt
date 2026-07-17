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
import javax.inject.Inject
import javax.inject.Singleton

enum class RuleExampleInferenceRejectionReason {
    INVALID_URL,
    IDENTICAL_URLS,
    DIFFERENT_SCHEME,
    DIFFERENT_HOST,
    DIFFERENT_PORT,
    DIFFERENT_PATH,
    DIFFERENT_FRAGMENT,
    AMBIGUOUS_DUPLICATES,
    CHANGED_VALUES,
    REORDERED_TOKENS,
    MULTIPLE_REDIRECT_CANDIDATES,
    INVALID_REDIRECT_OUTPUT,
    NO_SAFE_INFERENCE
}

sealed interface RuleExampleInferenceResult {
    data class Inferred(
        val action: RuleAction,
        val draft: CustomUrlRule
    ) : RuleExampleInferenceResult

    data class Rejected(
        val reason: RuleExampleInferenceRejectionReason
    ) : RuleExampleInferenceResult
}

/**
 * Conservatively derives a single safe custom-rule action from an input/output pair.
 *
 * Callers must validate both user-entered URLs with [com.fixupxer.utils.InputValidator]
 * before invoking this service. The inference itself remains pure and never changes,
 * logs, or persists either URL.
 */
@Singleton
class RuleExampleInference @Inject constructor(
    private val normalizer: UrlNormalizer
) {
    fun infer(before: String, desired: String): RuleExampleInferenceResult {
        val parsedBefore = runCatching { normalizer.normalize(before) }
            .getOrElse { return reject(RuleExampleInferenceRejectionReason.INVALID_URL) }
        val parsedDesired = runCatching { normalizer.normalize(desired) }
            .getOrElse {
                return if (hasInvalidRedirectCandidate(parsedBefore, desired)) {
                    reject(RuleExampleInferenceRejectionReason.INVALID_REDIRECT_OUTPUT)
                } else {
                    reject(RuleExampleInferenceRejectionReason.INVALID_URL)
                }
            }

        if (parsedBefore.comparisonUrl == parsedDesired.comparisonUrl) {
            return reject(RuleExampleInferenceRejectionReason.IDENTICAL_URLS)
        }

        inferRedirect(parsedBefore, parsedDesired.comparisonUrl)?.let { result ->
            return result
        }

        staticDifferenceReason(parsedBefore, parsedDesired)?.let(::reject)?.let { return it }

        return inferRemovedParameters(parsedBefore, parsedDesired)
    }

    private fun inferRedirect(
        before: com.fixupxer.processing.NormalizedUrl,
        desired: String
    ): RuleExampleInferenceResult? {
        val candidates = firstValuesByDecodedName(before.rawQuery)
            .flatMap { (name, rawValue) ->
                RedirectDecodeMode.entries.mapNotNull { mode ->
                    val decoded = decodeRedirectValue(rawValue, mode) ?: return@mapNotNull null
                    if (decoded == desired && isValidRedirectUrl(decoded)) {
                        name to mode
                    } else {
                        null
                    }
                }
            }
            .distinct()

        return when (candidates.size) {
            0 -> null
            1 -> {
                val (name, mode) = candidates.single()
                inferred(
                    before = before,
                    desired = desired,
                    action = RuleAction.ExtractRedirect(
                        parameterName = name,
                        ignoreCase = false,
                        decodeMode = mode
                    )
                )
            }
            else -> reject(RuleExampleInferenceRejectionReason.MULTIPLE_REDIRECT_CANDIDATES)
        }
    }

    private fun inferRemovedParameters(
        before: com.fixupxer.processing.NormalizedUrl,
        desired: com.fixupxer.processing.NormalizedUrl
    ): RuleExampleInferenceResult {
        val beforeTokens = rawTokens(before.rawQuery)
        if (beforeTokens.isEmpty()) {
            return reject(RuleExampleInferenceRejectionReason.NO_SAFE_INFERENCE)
        }
        val desiredTokens = rawTokens(desired.rawQuery)
        val survivingIndices = mutableListOf<Int>()
        var beforeIndex = 0

        for (desiredToken in desiredTokens) {
            val relativeIndex = beforeTokens
                .subList(beforeIndex, beforeTokens.size)
                .indexOf(desiredToken)
            val matchingIndex = if (relativeIndex >= 0) beforeIndex + relativeIndex else -1
            if (matchingIndex < 0) {
                return reject(
                    if (desiredToken in beforeTokens) {
                        RuleExampleInferenceRejectionReason.REORDERED_TOKENS
                    } else {
                        RuleExampleInferenceRejectionReason.CHANGED_VALUES
                    }
                )
            }
            survivingIndices += matchingIndex
            beforeIndex = matchingIndex + 1
        }

        if (survivingIndices.size == beforeTokens.size) {
            return reject(RuleExampleInferenceRejectionReason.NO_SAFE_INFERENCE)
        }

        val removedIndices = beforeTokens.indices.filterNot(survivingIndices::contains)
        val namesByToken = beforeTokens.map(::decodedName)
        if (namesByToken.any { it.isNullOrBlank() }) {
            return reject(RuleExampleInferenceRejectionReason.NO_SAFE_INFERENCE)
        }

        val removedNames = removedIndices
            .map { requireNotNull(namesByToken[it]) }
            .distinct()
        if (removedNames.size > Constants.MAX_SCOPE_ENTRIES ||
            removedNames.any { it.length > 256 }
        ) {
            return reject(RuleExampleInferenceRejectionReason.NO_SAFE_INFERENCE)
        }

        val allOccurrences = namesByToken.filterNotNull().groupingBy { it }.eachCount()
        val removedOccurrences = removedIndices
            .map { requireNotNull(namesByToken[it]) }
            .groupingBy { it }
            .eachCount()
        if (removedOccurrences.any { (name, count) -> count != allOccurrences[name] }) {
            return reject(RuleExampleInferenceRejectionReason.AMBIGUOUS_DUPLICATES)
        }

        return inferred(
            before = before,
            desired = desired.comparisonUrl,
            action = RuleAction.RemoveParams(removedNames, ignoreCase = false)
        )
    }

    private fun inferred(
        before: com.fixupxer.processing.NormalizedUrl,
        desired: String,
        action: RuleAction
    ): RuleExampleInferenceResult.Inferred = RuleExampleInferenceResult.Inferred(
        action = action,
        draft = CustomUrlRule(
            name = before.asciiHost,
            enabled = false,
            phase = RulePhase.PRE_CLEAN,
            contexts = setOf(ProcessingProfile.MAIN),
            includeScope = RuleScope.ExactHost(before.asciiHost),
            action = action,
            stopAfterMatch = false,
            testVectors = listOf(RuleTestVector(before.comparisonUrl, desired))
        )
    )

    private fun staticDifferenceReason(
        before: com.fixupxer.processing.NormalizedUrl,
        desired: com.fixupxer.processing.NormalizedUrl
    ): RuleExampleInferenceRejectionReason? = when {
        before.scheme != desired.scheme -> RuleExampleInferenceRejectionReason.DIFFERENT_SCHEME
        before.asciiHost != desired.asciiHost || before.userInfo != desired.userInfo ->
            RuleExampleInferenceRejectionReason.DIFFERENT_HOST
        before.port != desired.port -> RuleExampleInferenceRejectionReason.DIFFERENT_PORT
        before.rawPath != desired.rawPath -> RuleExampleInferenceRejectionReason.DIFFERENT_PATH
        before.rawFragment != desired.rawFragment ->
            RuleExampleInferenceRejectionReason.DIFFERENT_FRAGMENT
        staticUrlParts(before.comparisonUrl) != staticUrlParts(desired.comparisonUrl) ->
            RuleExampleInferenceRejectionReason.NO_SAFE_INFERENCE
        else -> null
    }

    private fun hasInvalidRedirectCandidate(
        before: com.fixupxer.processing.NormalizedUrl,
        desired: String
    ): Boolean = firstValuesByDecodedName(before.rawQuery).values.any { rawValue ->
        RedirectDecodeMode.entries.any { mode ->
            decodeRedirectValue(rawValue, mode) == desired
        }
    }

    private fun firstValuesByDecodedName(rawQuery: String?): Map<String, String> = buildMap {
        rawTokens(rawQuery).forEach { token ->
            val name = decodedName(token) ?: return@forEach
            if (name.isNotBlank() && name !in this) {
                put(name, token.substringAfter('=', ""))
            }
        }
    }

    private fun rawTokens(rawQuery: String?): List<String> =
        rawQuery?.takeIf(String::isNotEmpty)?.split('&').orEmpty()

    private fun decodedName(token: String): String? =
        strictPercentDecode(token.substringBefore('='))

    private fun staticUrlParts(url: String): String {
        val queryStart = url.indexOf('?')
        val fragmentStart = url.indexOf('#')
        val prefixEnd = listOf(queryStart, fragmentStart)
            .filter { it >= 0 }
            .minOrNull() ?: url.length
        return buildString {
            append(url.substring(0, prefixEnd))
            if (fragmentStart >= 0) append(url.substring(fragmentStart))
        }
    }

    private fun decodeRedirectValue(value: String, mode: RedirectDecodeMode): String? = when (mode) {
        RedirectDecodeMode.NONE -> value
        RedirectDecodeMode.PERCENT_ONCE -> strictPercentDecode(value)
        RedirectDecodeMode.FORM_ONCE -> runCatching {
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }.getOrNull()
        RedirectDecodeMode.BASE64URL -> decodeBase64Url(value)
    }

    private fun isValidRedirectUrl(value: String): Boolean =
        normalizer.isValidHttpUrl(value) && !hasMalformedPercent(value)

    private fun strictPercentDecode(value: String): String? = runCatching {
        val output = ByteArrayOutputStream(value.length)
        var index = 0
        while (index < value.length) {
            if (value[index] == '%') {
                require(index + 2 < value.length)
                output.write(value.substring(index + 1, index + 3).toInt(16))
                index += 3
            } else {
                output.write(value[index].toString().toByteArray(StandardCharsets.UTF_8))
                index++
            }
        }
        decodeUtf8(output.toByteArray())
    }.getOrNull()

    private fun decodeBase64Url(value: String): String? = runCatching {
        val paddingStart = value.indexOf('=')
        val core = if (paddingStart >= 0) {
            require(value.substring(paddingStart).all { it == '=' })
            value.substring(0, paddingStart)
        } else {
            value
        }
        val paddingCount = if (paddingStart >= 0) value.length - paddingStart else 0
        require(paddingCount <= 2 && core.length % 4 != 1)
        if (paddingCount > 0) {
            require(value.length % 4 == 0)
            require(
                (core.length % 4 == 2 && paddingCount == 2) ||
                    (core.length % 4 == 3 && paddingCount == 1)
            )
        }

        val output = ByteArrayOutputStream((core.length * 3) / 4)
        var index = 0
        while (index < core.length) {
            val remaining = core.length - index
            require(remaining >= 2)
            val first = base64UrlValue(core[index++])
            val second = base64UrlValue(core[index++])
            output.write((first shl 2) or (second shr 4))
            if (remaining >= 3) {
                val third = base64UrlValue(core[index++])
                output.write(((second and 0x0f) shl 4) or (third shr 2))
                if (remaining >= 4) {
                    val fourth = base64UrlValue(core[index++])
                    output.write(((third and 0x03) shl 6) or fourth)
                }
            }
        }
        decodeUtf8(output.toByteArray())
    }.getOrNull()

    private fun base64UrlValue(character: Char): Int = when (character) {
        in 'A'..'Z' -> character - 'A'
        in 'a'..'z' -> character - 'a' + 26
        in '0'..'9' -> character - '0' + 52
        '-' -> 62
        '_' -> 63
        else -> throw IllegalArgumentException("Invalid Base64 URL character")
    }

    private fun decodeUtf8(bytes: ByteArray): String =
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()

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

    private fun reject(reason: RuleExampleInferenceRejectionReason) =
        RuleExampleInferenceResult.Rejected(reason)
}
