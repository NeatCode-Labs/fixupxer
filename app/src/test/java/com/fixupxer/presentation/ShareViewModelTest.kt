// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2025  NeatCode Labs
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

package com.fixupxer.presentation

import android.app.Application
import com.fixupxer.R
import com.fixupxer.UrlProcessor
import com.fixupxer.domain.model.ProcessedUrlResult
import com.fixupxer.domain.model.ResultStatus
import com.fixupxer.presentation.share.ShareViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class ShareViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val urlProcessor: UrlProcessor = mock()
    private val application: Application = mock()

    @Before
    fun setUp() {
        whenever(application.getString(R.string.error_no_url_found_in_shared_text))
            .thenReturn("No URL found in shared text")
        whenever(application.getString(R.string.error_multiple_urls))
            .thenReturn("Please paste one URL at a time")
        whenever(application.getString(R.string.error_processing_url))
            .thenReturn("Error processing URL")

        whenever(urlProcessor.isInstagramUrl(any())).thenReturn(false)
        whenever(urlProcessor.isTwitterUrl(any())).thenReturn(false)
        whenever(urlProcessor.isFacebookUrl(any())).thenReturn(false)
        whenever(urlProcessor.isTikTokUrl(any())).thenReturn(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(urlRepository: TestUrlRepository = TestUrlRepository()): ShareViewModel {
        Dispatchers.setMain(testDispatcher)
        return ShareViewModel(urlRepository, urlProcessor, application)
    }

    @Test
    fun `processSharedText with valid URL sets result`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        val url = "https://example.com/article"
        urlRepository.processResult = ProcessedUrlResult(url, true)
        val viewModel = createViewModel(urlRepository)

        viewModel.processSharedText("Check this out: $url")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(url, state.processedUrl)
        assertEquals(url, state.actionUrl)
        assertEquals(ResultStatus.ALREADY_CLEAN, state.resultStatus)
        assertNull(state.error)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `processSharedText with no URL sets error`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.processSharedText("plain text without a link")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.processedUrl.isEmpty())
        assertTrue(state.actionUrl.isEmpty())
        assertNotNull(state.error)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `toggle skips pref write when value unchanged`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        val viewModel = createViewModel(urlRepository)
        advanceUntilIdle()

        viewModel.onTwitterConversionToggled(true)
        advanceUntilIdle()

        assertEquals(true, urlRepository.twitterFlow.value)
    }

    @Test
    fun `toggle reprocesses when shared text already processed`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        val url = "https://example.com/with-tracking"
        val cleaned = "https://example.com/clean"
        val recleaned = "https://example.com/cleaner"
        urlRepository.processHandler = { _, _, previous ->
            if (previous == cleaned) {
                ProcessedUrlResult(recleaned, false)
            } else {
                ProcessedUrlResult(cleaned, false)
            }
        }
        val viewModel = createViewModel(urlRepository)

        viewModel.processSharedText(url)
        advanceUntilIdle()

        viewModel.onTwitterConversionToggled(false)
        advanceUntilIdle()

        assertEquals(false, urlRepository.twitterFlow.value)
        assertEquals(recleaned, viewModel.uiState.value.actionUrl)
    }

    @Test
    fun `platform flags follow url processor detection`() = runTest(testDispatcher) {
        whenever(urlProcessor.isInstagramUrl(any())).thenReturn(true)
        val urlRepository = TestUrlRepository()
        val url = "https://www.instagram.com/p/abc/"
        urlRepository.processResult = ProcessedUrlResult("https://toinstagram.com/p/abc/", false)
        val viewModel = createViewModel(urlRepository)

        viewModel.processSharedText(url)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(true, state.isInstagramUrl)
        assertEquals(false, state.isTikTokUrl)
        assertEquals(ResultStatus.CONVERTED, state.resultStatus)
    }

    @Test
    fun `same shared text is not processed twice`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        var processCalls = 0
        urlRepository.processHandler = { _, _, _ ->
            processCalls++
            ProcessedUrlResult("https://example.com/clean", false)
        }
        val viewModel = createViewModel(urlRepository)
        val text = "https://example.com/with-tracking"

        viewModel.processSharedText(text)
        advanceUntilIdle()
        // Re-delivery of the same intent (activity recreation) must not write
        // a duplicate history entry.
        viewModel.processSharedText(text)
        advanceUntilIdle()

        assertEquals(1, processCalls)
        assertEquals("https://example.com/clean", viewModel.uiState.value.actionUrl)
    }

    @Test
    fun `reprocessAfterProxyChange reprocesses instagram share`() = runTest(testDispatcher) {
        whenever(urlProcessor.isInstagramUrl(any())).thenReturn(true)
        val urlRepository = TestUrlRepository()
        val first = "https://toinstagram.com/p/abc/"
        val second = "https://kkinstagram.com/p/abc/"
        urlRepository.processHandler = { _, _, previous ->
            if (previous == first) ProcessedUrlResult(second, false)
            else ProcessedUrlResult(first, false)
        }
        val viewModel = createViewModel(urlRepository)

        viewModel.processSharedText("https://www.instagram.com/p/abc/")
        advanceUntilIdle()
        assertEquals(first, viewModel.uiState.value.actionUrl)

        viewModel.reprocessAfterProxyChange()
        advanceUntilIdle()

        assertEquals(second, viewModel.uiState.value.actionUrl)
    }

    @Test
    fun `setNoSharedText surfaces error instead of endless loading`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setNoSharedText()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.actionUrl.isEmpty())
    }

    @Test
    fun `pref flow updates tiktok toggle state`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        val viewModel = createViewModel(urlRepository)
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.isTikTokConversionEnabled)

        urlRepository.tikTokFlow.value = false
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isTikTokConversionEnabled)
    }
}
