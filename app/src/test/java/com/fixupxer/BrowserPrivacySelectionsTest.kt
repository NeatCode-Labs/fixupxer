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

import com.fixupxer.data.repository.UrlRepositoryImpl
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.processing.PipelineProcessingResult
import com.fixupxer.processing.ProcessingOptions
import com.fixupxer.processing.ProcessingProfile
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.processing.UrlProcessingOrchestrator
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyPlatform
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BrowserPrivacySelectionsTest {

    @Test
    fun `browser profile uses privacy selections independent of main proxy choice`() = runBlocking {
        val fixture = repositoryFixture(
            url = X_STATUS_URL,
            isTwitter = true,
            isInstagram = false,
        )
        whenever(fixture.preferences.getSelectedProxyDomain(ProxyPlatform.X))
            .thenReturn(Constants.FIXUPX_DOMAIN)
        whenever(fixture.preferences.resolveBrowserPrivacySelections()).thenReturn(
            mapOf(ProxyPlatform.X to Constants.XCANCEL_DOMAIN),
        )
        whenever(fixture.preferences.resolveBrowserPrivacyTarget(ProxyPlatform.X))
            .thenReturn(AlternativeFrontendCatalog.byId("x_xcancel"))
        whenever(fixture.preferences.isBrowserPrivacyConversionEnabled(ProxyPlatform.X))
            .thenReturn(true)

        fixture.repository.processUrlForBrowser(X_STATUS_URL)

        val options = captureOptions(fixture.orchestrator)
        assertTrue(options.convertDomains)
        assertEquals(Constants.XCANCEL_DOMAIN, options.proxySelections.domainFor(ProxyPlatform.X))
    }

    @Test
    fun `main and share profiles use selected proxy domain not browser privacy`() = runBlocking {
        val mainFixture = repositoryFixture(
            url = X_STATUS_URL,
            isTwitter = true,
            isInstagram = false,
        )
        whenever(mainFixture.preferences.getSelectedProxyDomain(ProxyPlatform.X))
            .thenReturn(Constants.FIXUPX_DOMAIN)
        whenever(mainFixture.preferences.isConvertTwitterEnabled()).thenReturn(true)

        mainFixture.repository.processUrl(X_STATUS_URL)
        assertEquals(
            Constants.FIXUPX_DOMAIN,
            captureOptions(mainFixture.orchestrator).proxySelections.domainFor(ProxyPlatform.X),
        )

        val shareFixture = repositoryFixture(
            url = X_STATUS_URL,
            isTwitter = true,
            isInstagram = false,
        )
        whenever(shareFixture.preferences.getSelectedProxyDomain(ProxyPlatform.X))
            .thenReturn(Constants.FIXUPX_DOMAIN)
        whenever(shareFixture.preferences.isConvertTwitterEnabled()).thenReturn(true)

        shareFixture.repository.processSharedUrl(X_STATUS_URL)
        assertEquals(
            Constants.FIXUPX_DOMAIN,
            captureOptions(shareFixture.orchestrator).proxySelections.domainFor(ProxyPlatform.X),
        )
    }

    @Test
    fun `browser profile keeps legacy instagram browser toggle inert`() = runBlocking {
        val fixture = repositoryFixture(
            url = IG_URL,
            isTwitter = false,
            isInstagram = true,
        )
        // Removed legacy browser toggles are intentionally ignored for non-reader platforms.
        whenever(fixture.preferences.resolveBrowserPrivacySelections()).thenReturn(emptyMap())

        fixture.repository.processUrlForBrowser(IG_URL)

        assertFalse(captureOptions(fixture.orchestrator).convertDomains)
    }

    @Test
    fun `browser profile skips X conversion when privacy resolver is null`() = runBlocking {
        val fixture = repositoryFixture(
            url = X_STATUS_URL,
            isTwitter = true,
            isInstagram = false,
        )
        whenever(fixture.preferences.isBrowserPrivacyConversionEnabled(ProxyPlatform.X))
            .thenReturn(true)
        whenever(fixture.preferences.resolveBrowserPrivacyTarget(ProxyPlatform.X)).thenReturn(null)
        whenever(fixture.preferences.resolveBrowserPrivacySelections()).thenReturn(
            mapOf(ProxyPlatform.X to null),
        )

        fixture.repository.processUrlForBrowser(X_STATUS_URL)

        assertFalse(captureOptions(fixture.orchestrator).convertDomains)
    }

    private suspend fun captureOptions(orchestrator: UrlProcessingOrchestrator): ProcessingOptions {
        val captor = argumentCaptor<ProcessingOptions>()
        verify(orchestrator).process(any(), captor.capture(), isNull())
        return captor.firstValue
    }

    private fun repositoryFixture(
        url: String,
        isTwitter: Boolean,
        isInstagram: Boolean,
    ): RepositoryFixture {
        val processor: UrlProcessor = mock()
        val preferences: PreferencesManager = mock()
        val history: HistoryRepository = mock()
        val orchestrator: UrlProcessingOrchestrator = mock()
        whenever(processor.isInstagramUrl(url)).thenReturn(isInstagram)
        whenever(processor.isFacebookUrl(any())).thenReturn(false)
        whenever(processor.isTwitterUrl(url)).thenReturn(isTwitter)
        whenever(processor.isTikTokUrl(any())).thenReturn(false)
        whenever(processor.isBlueskyUrl(any())).thenReturn(false)
        whenever(processor.isRedditUrl(any())).thenReturn(false)
        whenever(processor.isYouTubeUrl(any())).thenReturn(false)
        whenever(processor.isPinterestUrl(any())).thenReturn(false)
        whenever(processor.isThreadsUrl(any())).thenReturn(false)
        whenever(preferences.isCleanTrackingEnabled()).thenReturn(false)
        whenever(preferences.areCustomRulesEnabled()).thenReturn(false)
        whenever(preferences.isHistoryEnabled()).thenReturn(false)
        runBlocking {
            whenever(orchestrator.process(any(), any(), isNull())).thenReturn(
                PipelineProcessingResult(
                    originalUrl = url,
                    url = url,
                    wasAlreadyClean = true,
                    builtinChanged = false,
                    domainConverted = false,
                    customRuleChanged = false,
                    rulesRevision = 0,
                    trace = emptyList(),
                    operations = emptyList(),
                ),
            )
        }
        return RepositoryFixture(
            repository = UrlRepositoryImpl(processor, preferences, history, orchestrator, UrlNormalizer()),
            orchestrator = orchestrator,
            preferences = preferences,
        )
    }

    private data class RepositoryFixture(
        val repository: UrlRepositoryImpl,
        val orchestrator: UrlProcessingOrchestrator,
        val preferences: PreferencesManager,
    )

    private companion object {
        const val X_STATUS_URL = "https://x.com/user/status/123"
        const val IG_URL = "https://instagram.com/p/abc"
    }
}
