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
import java.net.URLEncoder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LinkGuardRepositoryTest {

    @Test
    fun `sensitive input skips history and cleaner cache`() = runTest {
        val fixture = fixture()
        val input = "https://example.com/?access_token=abcdef123456&utm_source=tracking"

        val result = fixture.repository.processUrl(input)

        assertTrue(result.leakFindings.isNotEmpty())
        assertEquals(0, fixture.cache.getStats().size)
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

    private suspend fun fixture(customRuleSnapshot: RuleSnapshot? = null): Fixture {
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
        whenever(preferences.getMaxHistoryEntries()).thenReturn(50)

        val customRuleRepository: CustomRuleRepository = mock()
        if (customRuleSnapshot != null) {
            whenever(customRuleRepository.awaitSnapshot()).thenReturn(customRuleSnapshot)
        }

        val normalizer = UrlNormalizer()
        val orchestrator = UrlProcessingOrchestrator(
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
            repository = UrlRepositoryImpl(processor, preferences, history, orchestrator),
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
