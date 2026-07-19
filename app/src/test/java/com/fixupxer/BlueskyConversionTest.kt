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

package com.fixupxer

import com.fixupxer.cleaners.CleanerCatalog
import com.fixupxer.cleaners.CleanerRegistry
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.data.repository.UrlRepositoryImpl
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.processing.PipelineProcessingResult
import com.fixupxer.processing.ProcessingOptions
import com.fixupxer.processing.UrlProcessingOrchestrator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BlueskyConversionTest {
    private lateinit var cleanerService: CleanerService
    private lateinit var urlProcessor: UrlProcessor

    @Before
    fun setup() {
        cleanerService = CleanerService(
            CleanerRegistry().apply { registerAll(CleanerCatalog.createBuiltInCleaners()) },
            CleanerCache()
        )
        urlProcessor = UrlProcessor(cleanerService)
    }

    @Test
    fun `converts Bluesky post to fxbsky preserving path query and fragment`() {
        val input = "https://www.bsky.app/profile/alice.bsky.social/post/3kabc?lang=en#reply"

        assertEquals(
            "https://fxbsky.app/profile/alice.bsky.social/post/3kabc?lang=en#reply",
            urlProcessor.applyDomainConversions(input, convertToAlternative = true)
        )
    }

    @Test
    fun `converts fxbsky post back when conversion is disabled`() {
        val input = "https://www.fxbsky.app/profile/alice.bsky.social/post/3kabc?lang=en#reply"

        assertEquals(
            "https://bsky.app/profile/alice.bsky.social/post/3kabc?lang=en#reply",
            urlProcessor.applyDomainConversions(input, convertToAlternative = false)
        )
    }

    @Test
    fun `profiles are detected but fxbsky embed converts posts only`() {
        val profile = "https://bsky.app/profile/alice.bsky.social"
        val proxyProfile = "https://fxbsky.app/profile/alice.bsky.social"
        val redirect = "https://go.bsky.app/redirect?u=https%3A%2F%2Fexample.com"

        assertTrue(urlProcessor.isBlueskyUrl(profile))
        assertTrue(urlProcessor.isBlueskyUrl(proxyProfile))
        assertFalse(urlProcessor.isBlueskyUrl(redirect))
        assertEquals(profile, urlProcessor.applyDomainConversions(profile, convertToAlternative = true))
        assertEquals(
            "https://bsky.app/profile/alice.bsky.social",
            urlProcessor.applyDomainConversions(proxyProfile, convertToAlternative = false),
        )
        assertEquals("https://example.com", cleanerService.deepClean(redirect))
    }

    @Test
    fun `keeps already converted Bluesky post unchanged`() {
        val input = "https://fxbsky.app/profile/alice.bsky.social/post/3kabc"

        assertTrue(urlProcessor.isBlueskyUrl(input))
        assertEquals(input, urlProcessor.applyDomainConversions(input, convertToAlternative = true))
    }

    @Test
    fun `main and share profiles use Bluesky conversion preference`() = runBlocking {
        val main = repositoryWithPreferences(mainEnabled = true, browserEnabled = false)
        main.repository.processUrl(POST_URL)
        assertCapturedConversion(main.orchestrator, expected = true)

        val share = repositoryWithPreferences(mainEnabled = false, browserEnabled = true)
        share.repository.processSharedUrl(POST_URL)
        assertCapturedConversion(share.orchestrator, expected = false)
    }

    @Test
    fun `browser profile uses its Bluesky conversion preference`() = runBlocking {
        val repository = repositoryWithPreferences(mainEnabled = true, browserEnabled = false)

        repository.repository.processUrlForBrowser(POST_URL)

        assertCapturedConversion(repository.orchestrator, expected = false)
    }

    private fun repositoryWithPreferences(
        mainEnabled: Boolean,
        browserEnabled: Boolean
    ): RepositoryFixture {
        val processor: UrlProcessor = mock()
        val preferences: PreferencesManager = mock()
        val history: HistoryRepository = mock()
        val orchestrator: UrlProcessingOrchestrator = mock()
        whenever(processor.isInstagramUrl(any())).thenReturn(false)
        whenever(processor.isFacebookUrl(any())).thenReturn(false)
        whenever(processor.isTwitterUrl(any())).thenReturn(false)
        whenever(processor.isTikTokUrl(any())).thenReturn(false)
        whenever(processor.isBlueskyUrl(POST_URL)).thenReturn(true)
        whenever(preferences.isCleanTrackingEnabled()).thenReturn(false)
        whenever(preferences.isConvertBlueskyEnabled()).thenReturn(mainEnabled)
        whenever(preferences.isBrowserPrivacyConversionEnabled(com.fixupxer.utils.ProxyPlatform.BLUESKY))
            .thenReturn(browserEnabled)
        whenever(preferences.getInstagramProxy()).thenReturn("toinstagram.com")
        whenever(preferences.getTikTokProxy()).thenReturn("tnktok.com")
        whenever(preferences.areCustomRulesEnabled()).thenReturn(false)
        whenever(preferences.isHistoryEnabled()).thenReturn(false)
        whenever(preferences.resolveBrowserPrivacySelections()).thenReturn(emptyMap())
        whenever(preferences.resolveBrowserPrivacyTarget(any())).thenReturn(null)
        runBlocking {
            whenever(orchestrator.process(any(), any(), isNull())).thenReturn(
                PipelineProcessingResult(
                    originalUrl = POST_URL,
                    url = POST_URL,
                    wasAlreadyClean = true,
                    builtinChanged = false,
                    domainConverted = false,
                    customRuleChanged = false,
                    rulesRevision = 0,
                    trace = emptyList(),
                    operations = emptyList()
                )
            )
        }
        return RepositoryFixture(
            UrlRepositoryImpl(processor, preferences, history, orchestrator),
            orchestrator
        )
    }

    private suspend fun assertCapturedConversion(
        orchestrator: UrlProcessingOrchestrator,
        expected: Boolean
    ) {
        val options = argumentCaptor<ProcessingOptions>()
        verify(orchestrator).process(eq(POST_URL), options.capture(), isNull())
        assertEquals(expected, options.firstValue.convertDomains)
    }

    private data class RepositoryFixture(
        val repository: UrlRepositoryImpl,
        val orchestrator: UrlProcessingOrchestrator
    )

    private companion object {
        const val POST_URL = "https://bsky.app/profile/alice.bsky.social/post/3kabc"
    }
}
