// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fixupxer.processing

import com.fixupxer.PreferencesManager
import com.fixupxer.UrlProcessor
import com.fixupxer.cleaners.CleanerCatalog
import com.fixupxer.cleaners.CleanerRegistry
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.UrlCleaner
import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.data.repository.UrlRepositoryImpl
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.rules.CustomRuleEngine
import com.fixupxer.rules.CustomRuleRepository
import com.fixupxer.rules.CustomUrlRule
import com.fixupxer.rules.RuleAction
import com.fixupxer.rules.RuleActionExecutor
import com.fixupxer.rules.RuleCompiler
import com.fixupxer.rules.RuleMatcher
import com.fixupxer.rules.RuleSnapshot
import com.fixupxer.utils.Constants
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ChangeOperationTraceTest {

    @Test
    fun `redirect cleaning conversion operations preserve pipeline order`() = runTest {
        val result = orchestrator().process(
            rawInput = "https://www.google.com/url?q=" + encode(
                "https://twitter.com/example/status/1?utm_source=trace&normal=keep"
            ),
            options = options(cleanTracking = true, convertDomains = true)
        )

        val redirect = result.operations.indexOfFirst {
            it.type == ChangeOperationType.REDIRECT_EXTRACTED
        }
        val parameters = result.operations.indexOfFirst {
            it.type == ChangeOperationType.PARAMETERS_REMOVED &&
                "utm_source" in it.parameterNames
        }
        val conversion = result.operations.indexOfFirst {
            it.type == ChangeOperationType.HOST_CONVERTED &&
                it.fromHost == "twitter.com" &&
                it.toHost == "fixupx.com"
        }

        assertTrue(redirect >= 0)
        assertTrue(parameters > redirect)
        assertTrue(conversion > parameters)
    }

    @Test
    fun `cached cleaning retains an identical operation trace`() {
        val service = cleanerService()
        val url = "https://example.com/?utm_source=trace&normal=keep"

        val cold = service.deepCleanWithDetails(url)
        val warm = service.deepCleanWithDetails(url)

        assertEquals(cold.cleanedUrl, warm.cleanedUrl)
        assertEquals(cold.operations, warm.operations)
        assertTrue(cold.totalPasses >= 1)
        assertEquals(cold.totalPasses, warm.totalPasses)
        assertEquals(1, service.getCacheStats().size)
    }

    @Test
    fun `nested redirect wrappers are recorded before parameter removal`() {
        val finalUrl = "https://example.com/watch?utm_source=trace&normal=keep"
        val youtubeRedirect = "https://www.youtube.com/redirect?q=${encode(finalUrl)}"
        val wrapped = "https://l.facebook.com/l.php?u=${encode(youtubeRedirect)}"

        val operations = cleanerService().deepCleanWithDetails(wrapped).operations
        val redirects = operations.withIndex()
            .filter { it.value.type == ChangeOperationType.REDIRECT_EXTRACTED }
        val parameters = operations.indexOfFirst {
            it.type == ChangeOperationType.PARAMETERS_REMOVED &&
                "utm_source" in it.parameterNames
        }

        assertTrue(redirects.size >= 2)
        assertEquals("l.facebook.com", redirects[0].value.fromHost)
        assertEquals("www.youtube.com", redirects[0].value.toHost)
        assertEquals("www.youtube.com", redirects[1].value.fromHost)
        assertEquals("example.com", redirects[1].value.toHost)
        assertTrue(parameters > redirects[1].index)
    }

    @Test
    fun `cleaner operation trace is capped without stopping cleaning`() {
        val registry = CleanerRegistry()
        repeat(Constants.MAX_CHANGE_OPERATIONS + 5) { index ->
            registry.register(parameterRemovingCleaner(index))
        }
        val service = CleanerService(registry, CleanerCache())
        val input = "https://example.com/?" + (0 until Constants.MAX_CHANGE_OPERATIONS + 5)
            .joinToString("&") { "p$it=value" }

        val result = service.deepCleanWithDetailsWithoutCache(input)

        assertEquals(Constants.MAX_CHANGE_OPERATIONS, result.operations.size)
        assertFalse(result.cleanedUrl.contains("p0="))
        assertFalse(result.cleanedUrl.contains("p${Constants.MAX_CHANGE_OPERATIONS + 4}="))
    }

    @Test
    fun `operations expose parameter names but no values or URLs`() {
        val result = cleanerService().deepCleanWithDetails(
            "https://example.com/?utm_source=SECRETVALUE&normal=keepme"
        )
        val serialized = result.operations.toString()

        assertTrue(serialized.contains("utm_source"))
        assertFalse(serialized.contains("SECRETVALUE"))
        assertFalse(serialized.contains("keepme"))
        assertNoSensitiveOperationData(result.operations)
    }

    @Test
    fun `custom rule operation is generic without verbose rule tracing`() = runTest {
        val rule = CustomUrlRule(
            name = "Remove private tracking",
            action = RuleAction.RemoveParams(listOf("private_tracking"))
        )
        val snapshot = RuleSnapshot(listOf(RuleCompiler().compile(rule)), revision = 1)

        val result = orchestrator().process(
            rawInput = "https://example.com/?private_tracking=SECRET&normal=keep",
            options = options(cleanTracking = false, convertDomains = false),
            snapshotOverride = snapshot
        )

        val operation = result.operations.single {
            it.type == ChangeOperationType.CUSTOM_RULE_APPLIED
        }
        assertTrue(operation.source.isBlank())
        assertNoSensitiveOperationData(result.operations)
        assertFalse(result.operations.toString().contains("SECRET"))
        assertFalse(result.operations.toString().contains("keep"))
    }

    @Test
    fun `history persistence receives URLs only not operations`() = runTest {
        val input = "https://example.com/?utm_source=trace"
        val cleaned = "https://example.com/"
        val operations = listOf(
            ChangeOperation(
                type = ChangeOperationType.PARAMETERS_REMOVED,
                source = "General Tracking",
                parameterNames = listOf("utm_source")
            )
        )
        val processor: UrlProcessor = mock()
        val preferences: PreferencesManager = mock()
        val history: HistoryRepository = mock()
        val pipeline: UrlProcessingOrchestrator = mock()
        whenever(processor.isInstagramUrl(any())).thenReturn(false)
        whenever(processor.isFacebookUrl(any())).thenReturn(false)
        whenever(processor.isTwitterUrl(any())).thenReturn(false)
        whenever(processor.isTikTokUrl(any())).thenReturn(false)
        whenever(processor.isBlueskyUrl(any())).thenReturn(false)
        whenever(preferences.isCleanTrackingEnabled()).thenReturn(true)
        whenever(preferences.isConvertTwitterEnabled()).thenReturn(false)
        whenever(preferences.areCustomRulesEnabled()).thenReturn(false)
        whenever(preferences.getInstagramProxy()).thenReturn("toinstagram.com")
        whenever(preferences.getTikTokProxy()).thenReturn("tnktok.com")
        whenever(preferences.isHistoryEnabled()).thenReturn(true)
        whenever(preferences.getMaxHistoryEntries()).thenReturn(50)
        whenever(pipeline.process(any(), any(), isNull())).thenReturn(
            PipelineProcessingResult(
                originalUrl = input,
                url = cleaned,
                wasAlreadyClean = false,
                builtinChanged = true,
                domainConverted = false,
                customRuleChanged = false,
                rulesRevision = 0,
                trace = emptyList(),
                operations = operations
            )
        )
        val repository = UrlRepositoryImpl(processor, preferences, history, pipeline)

        val result = repository.processUrl(input)

        assertEquals(operations, result.operations)
        verify(history).insertHistory(eq(input), eq(cleaned), eq("Other"), any())
        verify(history).trimHistory(50)
    }

    private fun cleanerService(): CleanerService {
        val registry = CleanerRegistry().apply {
            registerAll(CleanerCatalog.createBuiltInCleaners())
        }
        return CleanerService(registry, CleanerCache())
    }

    private fun orchestrator(): UrlProcessingOrchestrator {
        val cleanerService = cleanerService()
        val normalizer = UrlNormalizer()
        return UrlProcessingOrchestrator(
            RawUrlExtractor(),
            normalizer,
            cleanerService,
            DomainConversionService(UrlProcessor(cleanerService)),
            CustomRuleEngine(RuleMatcher(normalizer), RuleActionExecutor(normalizer)),
            mock<CustomRuleRepository>()
        )
    }

    private fun options(
        cleanTracking: Boolean,
        convertDomains: Boolean
    ) = ProcessingOptions(
        profile = ProcessingProfile.MAIN,
        cleanTracking = cleanTracking,
        convertDomains = convertDomains,
        instagramProxy = "toinstagram.com",
        tiktokProxy = "tnktok.com",
        customRulesEnabled = false
    )

    private fun parameterRemovingCleaner(index: Int) = object : UrlCleaner {
        override val id = "synthetic_$index"
        override val displayName = "Synthetic $index"

        override fun matches(url: String): Boolean = url.contains("p$index=")

        override fun clean(url: String): String {
            val base = url.substringBefore('?')
            val query = url.substringAfter('?', missingDelimiterValue = "")
            val remaining = query.split('&').filterNot { it.substringBefore('=') == "p$index" }
            return if (remaining.isEmpty()) base else "$base?${remaining.joinToString("&")}"
        }
    }

    private fun assertNoSensitiveOperationData(operations: List<ChangeOperation>) {
        operations.forEach { operation ->
            val fields = listOfNotNull(
                operation.source,
                operation.fromHost,
                operation.toHost
            ) + operation.parameterNames
            fields.forEach { value ->
                assertFalse(value.contains("https://"))
                assertFalse(value.contains("http://"))
                assertFalse(value.contains("="))
            }
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
}
