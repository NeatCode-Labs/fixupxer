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

package com.fixupxer.data.repository

import com.fixupxer.PreferencesManager
import com.fixupxer.UrlProcessor
import com.fixupxer.cleaners.CleanerCatalog
import com.fixupxer.cleaners.CleanerRegistry
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.processing.DomainConversionService
import com.fixupxer.processing.PipelineProcessingResult
import com.fixupxer.processing.RawUrlExtractor
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.processing.UrlProcessingOrchestrator
import com.fixupxer.rules.CustomRuleEngine
import com.fixupxer.rules.CustomRuleRepository
import com.fixupxer.rules.CustomUrlRule
import com.fixupxer.rules.RuleAction
import com.fixupxer.rules.RuleActionExecutor
import com.fixupxer.rules.RuleCompiler
import com.fixupxer.rules.RuleMatcher
import com.fixupxer.rules.RulePhase
import com.fixupxer.rules.RuleSnapshot
import com.fixupxer.utils.Constants
import java.net.URLEncoder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LinkGuardRepositoryTest {

    @Test
    fun `sensitive input and output skip history and cleaner cache`() = runTest {
        val fixture = fixture()
        val input = "https://example.com/?access_token=abcdef123456&utm_source=tracking"

        val result = fixture.repository.processUrl(input)

        assertTrue(result.leakFindings.isNotEmpty())
        assertEquals(0, fixture.cache.getStats().size)
        verify(fixture.history, never()).insertHistory(any(), any(), any(), any())
        verify(fixture.history, never()).trimHistory(any())
    }

    @Test
    fun `sensitive unchanged URL stays out of history`() = runTest {
        val fixture = fixture()
        val input = "https://unknown-example.org/page?access_token=abcdef123456"

        val result = fixture.repository.processUrl(input)

        assertEquals(input, result.url)
        assertTrue(result.leakFindings.isNotEmpty())
        assertEquals(0, fixture.cache.getStats().size)
        // Both the output-finding rule and the unchanged-URL dedupe independently block history.
        verify(fixture.history, never()).insertHistory(any(), any(), any(), any())
        verify(fixture.history, never()).trimHistory(any())
    }

    @Test
    fun `invalid cleaned URL stays out of redacted history`() = runTest {
        val input = "https://example.com/page?access_token=abcdef123456"
        val invalidUrl = "not a url"
        val orchestrator: UrlProcessingOrchestrator = mock()
        whenever(orchestrator.process(any(), any(), isNull())).thenReturn(
            PipelineProcessingResult(
                originalUrl = input,
                url = invalidUrl,
                wasAlreadyClean = false,
                builtinChanged = true,
                domainConverted = false,
                customRuleChanged = false,
                rulesRevision = 0,
                trace = emptyList(),
                operations = emptyList(),
                cleanerCacheKeys = emptyList(),
            ),
        )
        val fixture = fixture(orchestratorOverride = orchestrator)

        val result = fixture.repository.processUrl(input)

        assertEquals(invalidUrl, result.url)
        assertTrue(result.leakFindings.isEmpty())
        verify(fixture.history, never()).insertHistory(any(), any(), any(), any())
        verify(fixture.history, never()).trimHistory(any())
    }

    @Test
    fun `clean input saves history as before`() = runTest {
        val fixture = fixture()
        val input = "https://example.com/?utm_source=tracking"

        val result = fixture.repository.processUrl(input)

        assertFalse(result.url.contains("utm_source"))
        assertTrue(result.leakFindings.isEmpty())
        assertTrue(fixture.cache.getStats().size > 0)
        verify(fixture.history).insertHistory(eq(input), eq(result.url), eq("Other"), any())
        verify(fixture.history).trimHistory(50)
    }

    @Test
    fun `pending legacy limit trims history to the raw legacy value`() = runTest {
        val fixture = fixture(pendingLegacyHistoryLimit = 20_000)
        val input = "https://example.com/?utm_source=tracking"

        val result = fixture.repository.processUrl(input)

        verify(fixture.history).insertHistory(eq(input), eq(result.url), eq("Other"), any())
        verify(fixture.history).trimHistory(20_000)
    }

    @Test
    fun `output leak from redirect skips history and evicts cached wrapper`() = runTest {
        val fixture = fixture()
        val destination = "https://example.com/?access_token=abcdef123456"
        val input = "https://l.facebook.com/l.php?u=${URLEncoder.encode(destination, "UTF-8")}"

        val result = fixture.repository.processUrl(input)

        assertEquals(destination, result.url)
        assertTrue(result.leakFindings.isNotEmpty())
        assertEquals(0, fixture.cache.getStats().size)
        verify(fixture.history, never()).insertHistory(any(), any(), any(), any())
        verify(fixture.history, never()).trimHistory(any())
    }

    @Test
    fun `output leak evicts cache entries keyed after a pre-clean custom rule`() = runTest {
        // The PRE_CLEAN rule changes the URL before cleaning, so the cleaner
        // cache is keyed by the rewritten URL — eviction must hit that key,
        // not the original input.
        val rule = CustomUrlRule(
            name = "Drop decoy",
            phase = RulePhase.PRE_CLEAN,
            action = RuleAction.RemoveParams(listOf("decoy"))
        )
        val snapshot = RuleSnapshot(listOf(RuleCompiler().compile(rule)), revision = 1)
        val fixture = fixture(customRuleSnapshot = snapshot)
        val destination = "https://example.com/?access_token=abcdef123456"
        val input = "https://l.facebook.com/l.php?decoy=1&u=${URLEncoder.encode(destination, "UTF-8")}"

        val result = fixture.repository.processUrl(input)

        assertEquals(destination, result.url)
        assertTrue(result.leakFindings.isNotEmpty())
        assertEquals(0, fixture.cache.getStats().size)
        verify(fixture.history, never()).insertHistory(any(), any(), any(), any())
        verify(fixture.history, never()).trimHistory(any())
    }

    @Test
    fun `sharing leaves instagram unchanged when no target is selected`() = runTest {
        val fixture = fixture()
        val input = "https://instagram.com/p/abc"

        val result = fixture.repository.processUrlForSharing(input)

        assertEquals(input, result)
    }

    @Test
    fun `reddit wrapper saves redacted history without sensitive input`() = runTest {
        val fixture = fixture()
        val token = "AQAAsomelongtokenvalue"
        val expectedUrl = "https://example.com/news/article"
        val input =
            "https://out.reddit.com/t3_abc123?url=${URLEncoder.encode(expectedUrl, "UTF-8")}" +
                "&token=$token&app_name=android"

        val result = fixture.repository.processUrl(input)

        assertEquals(expectedUrl, result.url)
        assertTrue(result.leakFindings.isEmpty())
        assertEquals(0, fixture.cache.getStats().size)
        assertRedactedHistory(fixture, expectedUrl, token, "out.reddit.com")
    }

    @Test
    fun `reddit wrapper saves redacted history for share profile`() = runTest {
        val fixture = fixture()
        val token = "AQAAsomelongtokenvalue"
        val expectedUrl = "https://example.com/news/article"
        val input =
            "https://out.reddit.com/t3_abc123?url=${URLEncoder.encode(expectedUrl, "UTF-8")}" +
                "&token=$token&app_name=android"

        val result = fixture.repository.processSharedUrl(input, null)

        assertEquals(expectedUrl, result.url)
        assertTrue(result.leakFindings.isEmpty())
        assertRedactedHistory(fixture, expectedUrl, token, "out.reddit.com")
    }

    @Test
    fun `reddit wrapper saves redacted history for browser profile`() = runTest {
        val fixture = fixture()
        val token = "AQAAsomelongtokenvalue"
        val expectedUrl = "https://example.com/news/article"
        val input =
            "https://out.reddit.com/t3_abc123?url=${URLEncoder.encode(expectedUrl, "UTF-8")}" +
                "&token=$token&app_name=android"

        val result = fixture.repository.processUrlForBrowser(input)

        assertEquals(expectedUrl, result.url)
        assertTrue(result.leakFindings.isEmpty())
        assertRedactedHistory(fixture, expectedUrl, token, "out.reddit.com")
    }

    @Test
    fun `google ads wrapper saves redacted history without sensitive input`() = runTest {
        val fixture = fixture()
        val sig = "AOD64_3abcdefgh"
        val expectedUrl = "https://example.com/product"
        val input =
            "https://www.googleadservices.com/pagead/aclk?sa=L&sig=$sig" +
                "&adurl=${URLEncoder.encode(expectedUrl, "UTF-8")}"

        val result = fixture.repository.processUrl(input)

        assertEquals(expectedUrl, result.url)
        assertTrue(result.leakFindings.isEmpty())
        assertEquals(0, fixture.cache.getStats().size)
        assertRedactedHistory(fixture, expectedUrl, sig, "googleadservices")
    }

    @Test
    fun `substack jwt token param saves redacted history`() = runTest {
        val fixture = fixture()
        val input =
            """https://substack.com/app-link/post?publication_id=806546&post_id=165696748""" +
                """&utm_source=post-email-title&utm_campaign=email-post-title&isFreemail=true&r=1ez2n3""" +
                """&token=eyJ1c2VyX2lkIjo4NTYxNzE4MywicG9zdF9pZCI6MTY1Njk2NzQ4LCJpYXQiOjE3NDk2NDAzNjAsImV4cCI6MTc1MjIzMjM2MCwiaXNzIjoicHViLTgwNjU0NiIsInN1YiI6InBvc3QtcmVhY3Rpb24ifQ.CR78H3BGztpRqBf1lnnDlafH_popPsMlwvTLQvQC9l8"""
        val expectedUrl =
            "https://substack.com/app-link/post?publication_id=806546&post_id=165696748"

        val result = fixture.repository.processUrl(input)

        assertEquals(expectedUrl, result.url)
        assertTrue(result.leakFindings.isEmpty())
        assertEquals(0, fixture.cache.getStats().size)
        assertRedactedHistory(fixture, expectedUrl)
    }

    @Test
    fun `custom rule removing token param saves redacted history`() = runTest {
        val token = "abcdefgh12345"
        val rule = CustomUrlRule(
            name = "Strip token",
            phase = RulePhase.PRE_CLEAN,
            action = RuleAction.RemoveParams(listOf("token")),
        )
        val snapshot = RuleSnapshot(listOf(RuleCompiler().compile(rule)), revision = 1)
        val fixture = fixture(customRuleSnapshot = snapshot)
        val input = "https://example.com/page?token=$token"
        val expectedUrl = "https://example.com/page"

        val result = fixture.repository.processUrl(input)

        assertEquals(expectedUrl, result.url)
        assertTrue(result.leakFindings.isEmpty())
        assertEquals(0, fixture.cache.getStats().size)
        assertRedactedHistory(fixture, expectedUrl, token)
    }

    @Test
    fun `redacted entry dedupe skips insert when previous processed url matches final url`() = runTest {
        val fixture = fixture()
        val expectedUrl = "https://example.com/news/article"
        val input =
            "https://out.reddit.com/t3_abc123?url=${URLEncoder.encode(expectedUrl, "UTF-8")}" +
                "&token=AQAAsomelongtokenvalue&app_name=android"

        fixture.repository.processUrl(input, false, expectedUrl)

        verify(fixture.history, never()).insertHistory(any(), any(), any(), any())
        verify(fixture.history, never()).trimHistory(any())
    }

    private suspend fun assertRedactedHistory(
        fixture: Fixture,
        expectedUrl: String,
        vararg forbiddenFragments: String,
    ) {
        val originalCaptor = argumentCaptor<String>()
        val cleanedCaptor = argumentCaptor<String>()
        val platformCaptor = argumentCaptor<String>()
        val conversionCaptor = argumentCaptor<String>()
        verify(fixture.history).insertHistory(
            originalCaptor.capture(),
            cleanedCaptor.capture(),
            platformCaptor.capture(),
            conversionCaptor.capture(),
        )
        verify(fixture.history).trimHistory(50)
        assertEquals(expectedUrl, originalCaptor.firstValue)
        assertEquals(expectedUrl, cleanedCaptor.firstValue)
        assertEquals("Other", platformCaptor.firstValue)
        assertEquals(Constants.HISTORY_CONVERSION_INPUT_REDACTED, conversionCaptor.firstValue)
        forbiddenFragments.forEach { fragment ->
            assertFalse(originalCaptor.firstValue.contains(fragment))
            assertFalse(cleanedCaptor.firstValue.contains(fragment))
        }
    }

    private suspend fun fixture(
        customRuleSnapshot: RuleSnapshot? = null,
        pendingLegacyHistoryLimit: Int? = null,
        orchestratorOverride: UrlProcessingOrchestrator? = null,
    ): Fixture {
        val cache = CleanerCache()
        val registry = CleanerRegistry().apply {
            registerAll(CleanerCatalog.createBuiltInCleaners())
        }
        val cleanerService = CleanerService(registry, cache)
        val processor = UrlProcessor(cleanerService)
        val preferences: PreferencesManager = mock()
        val history: HistoryRepository = mock()
        whenever(preferences.isCleanTrackingEnabled()).thenReturn(true)
        whenever(preferences.isConvertInstagramEnabled()).thenReturn(false)
        whenever(preferences.isConvertTwitterEnabled()).thenReturn(false)
        whenever(preferences.isConvertTikTokEnabled()).thenReturn(false)
        whenever(preferences.isConvertBlueskyEnabled()).thenReturn(false)
        whenever(preferences.getInstagramProxy()).thenReturn("toinstagram.com")
        whenever(preferences.getTikTokProxy()).thenReturn("tnktok.com")
        whenever(preferences.areCustomRulesEnabled()).thenReturn(customRuleSnapshot != null)
        whenever(preferences.isHistoryEnabled()).thenReturn(true)
        whenever(preferences.getMaxHistoryEntries()).thenReturn(pendingLegacyHistoryLimit ?: 50)
        whenever(preferences.isHistoryLimitMigrationPending())
            .thenReturn(pendingLegacyHistoryLimit != null)
        whenever(preferences.resolveBrowserPrivacySelections()).thenReturn(emptyMap())

        val customRuleRepository: CustomRuleRepository = mock()
        if (customRuleSnapshot != null) {
            whenever(customRuleRepository.awaitSnapshot()).thenReturn(customRuleSnapshot)
        }

        val normalizer = UrlNormalizer()
        val orchestrator = orchestratorOverride ?: UrlProcessingOrchestrator(
            extractor = RawUrlExtractor(),
            normalizer = normalizer,
            cleanerService = cleanerService,
            domainConversionService = DomainConversionService(processor),
            ruleEngine = CustomRuleEngine(
                RuleMatcher(normalizer),
                RuleActionExecutor(normalizer)
            ),
            customRuleRepository = customRuleRepository
        )
        return Fixture(
            repository = UrlRepositoryImpl(processor, preferences, history, orchestrator, normalizer),
            history = history,
            cache = cache
        )
    }

    private data class Fixture(
        val repository: UrlRepositoryImpl,
        val history: HistoryRepository,
        val cache: CleanerCache
    )
}
