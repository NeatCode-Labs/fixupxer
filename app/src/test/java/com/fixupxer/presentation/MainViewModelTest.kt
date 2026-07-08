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
import com.fixupxer.domain.model.ProcessedUrlResult
import com.fixupxer.domain.model.ResultStatus
import com.fixupxer.domain.model.resolveResultStatus
import com.fixupxer.presentation.main.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val application: Application = mock()

    @Before
    fun setUp() {
        whenever(application.getString(R.string.error_please_enter_url))
            .thenReturn("Please enter a URL")
        whenever(application.getString(R.string.error_processing_url))
            .thenReturn("Error processing URL")
        whenever(application.getString(R.string.error_multiple_urls))
            .thenReturn("Please paste one URL at a time")
        whenever(application.getString(R.string.error_invalid_input))
            .thenReturn("This input can't be processed")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(urlRepository: TestUrlRepository = TestUrlRepository()): MainViewModel {
        Dispatchers.setMain(testDispatcher)
        return MainViewModel(urlRepository, application)
    }

    @Test
    fun `resolveResultStatus maps cleaned converted and already clean URLs`() {
        assertEquals(
            ResultStatus.ALREADY_CLEAN,
            resolveResultStatus("https://example.com", "https://example.com")
        )
        assertEquals(
            ResultStatus.CLEANED,
            resolveResultStatus("https://example.com/a", "https://example.com/b")
        )
        assertEquals(
            ResultStatus.CONVERTED,
            resolveResultStatus("https://twitter.com/x", "https://fixupx.com/x")
        )
        assertEquals(
            ResultStatus.CLEANED_AND_CONVERTED,
            resolveResultStatus("https://twitter.com/x?s=20&t=abc", "https://fixupx.com/x")
        )
    }

    @Test
    fun `processUrl sets ALREADY_CLEAN when output matches input`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        val url = "https://example.com/page"
        urlRepository.processResult = ProcessedUrlResult(url, true)
        val viewModel = createViewModel(urlRepository)
        advanceUntilIdle()

        viewModel.onUrlChanged(url)
        viewModel.processUrl()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(url, state.processedUrl)
        assertEquals(url, state.actionUrl)
        assertEquals(ResultStatus.ALREADY_CLEAN, state.resultStatus)
        assertNull(state.error)
    }

    @Test
    fun `processUrl sets CLEANED when host unchanged`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        val input = "https://example.com/page/extra"
        val output = "https://example.com/page"
        urlRepository.processResult = ProcessedUrlResult(output, false)
        val viewModel = createViewModel(urlRepository)
        advanceUntilIdle()

        viewModel.onUrlChanged(input)
        viewModel.processUrl()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertEquals(output, state.processedUrl)
        assertEquals(output, state.actionUrl)
        assertEquals(ResultStatus.CLEANED, state.resultStatus)
    }

    @Test
    fun `processUrl sets CONVERTED when host changes`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        val input = "https://twitter.com/user/status/1"
        val output = "https://fixupx.com/user/status/1"
        urlRepository.processResult = ProcessedUrlResult(output, false)
        val viewModel = createViewModel(urlRepository)
        advanceUntilIdle()

        viewModel.onUrlChanged(input)
        viewModel.processUrl()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertEquals(ResultStatus.CONVERTED, state.resultStatus)
        assertEquals(output, state.actionUrl)
    }

    @Test
    fun `processUrl sets CLEANED_AND_CONVERTED when host and query change`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        val input = "https://twitter.com/user/status/1?s=20&t=abc"
        val output = "https://fixupx.com/user/status/1"
        urlRepository.processResult = ProcessedUrlResult(output, false)
        val viewModel = createViewModel(urlRepository)
        advanceUntilIdle()

        viewModel.onUrlChanged(input)
        viewModel.processUrl()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertEquals(ResultStatus.CLEANED_AND_CONVERTED, state.resultStatus)
        assertEquals(output, state.actionUrl)
    }

    @Test
    fun `editing input clears stale result and action url`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        val input = "https://example.com/page?utm_source=x"
        val output = "https://example.com/page"
        urlRepository.processResult = ProcessedUrlResult(output, false)
        val viewModel = createViewModel(urlRepository)
        advanceUntilIdle()

        viewModel.onUrlChanged(input)
        viewModel.processUrl()
        advanceUntilIdle()
        assertEquals(output, viewModel.uiState.value.actionUrl)

        // Any change to the input text invalidates the previous result.
        viewModel.onUrlChanged("https://example.com/page?utm_source=x&more")
        val state = viewModel.uiState.value
        assertEquals("", state.processedUrl)
        assertEquals("", state.actionUrl)
        assertEquals("", state.processedInputUrl)
        assertNull(state.resultStatus)
    }

    @Test
    fun `unchanged input keeps existing result`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        val input = "https://example.com/page?utm_source=x"
        val output = "https://example.com/page"
        urlRepository.processResult = ProcessedUrlResult(output, false)
        val viewModel = createViewModel(urlRepository)
        advanceUntilIdle()

        viewModel.onUrlChanged(input)
        viewModel.processUrl()
        advanceUntilIdle()

        // Re-delivery of the same text (e.g. history selection of the same URL)
        // must not wipe the result.
        viewModel.onUrlChanged(input)
        val state = viewModel.uiState.value
        assertEquals(output, state.actionUrl)
        assertEquals(ResultStatus.CLEANED, state.resultStatus)
    }

    @Test
    fun `setValidationError maps reasons to distinct messages`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setValidationError(com.fixupxer.utils.InputValidator.InvalidReason.MULTIPLE_URLS)
        assertEquals("Please paste one URL at a time", viewModel.uiState.value.error)
        assertEquals("", viewModel.uiState.value.inputUrl)

        viewModel.setValidationError(com.fixupxer.utils.InputValidator.InvalidReason.OTHER)
        assertEquals("This input can't be processed", viewModel.uiState.value.error)
    }

    @Test
    fun `toggle reprocesses when actionable result exists`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        val input = "https://example.com/with-tracking"
        val firstOutput = "https://example.com/clean"
        val secondOutput = "https://example.com/cleaner"
        urlRepository.processHandler = { _, _, previous ->
            if (previous == firstOutput) {
                ProcessedUrlResult(secondOutput, false)
            } else {
                ProcessedUrlResult(firstOutput, false)
            }
        }
        val viewModel = createViewModel(urlRepository)
        advanceUntilIdle()

        viewModel.onUrlChanged(input)
        viewModel.processUrl()
        advanceUntilIdle()

        viewModel.onInstagramConversionToggled(false)
        advanceUntilIdle()

        assertEquals(false, urlRepository.instagramFlow.value)
        assertEquals(secondOutput, viewModel.uiState.value.actionUrl)
    }

    @Test
    fun `toggle does not reprocess when no result yet`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        var processCalls = 0
        urlRepository.processHandler = { _, _, _ ->
            processCalls++
            ProcessedUrlResult("", false)
        }
        val viewModel = createViewModel(urlRepository)
        advanceUntilIdle()

        viewModel.onInstagramConversionToggled(false)
        advanceUntilIdle()

        assertEquals(false, urlRepository.instagramFlow.value)
        assertEquals(0, processCalls)
    }

    @Test
    fun `toggle skips pref write when value unchanged`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        val viewModel = createViewModel(urlRepository)
        advanceUntilIdle()

        viewModel.onInstagramConversionToggled(true)
        advanceUntilIdle()

        assertEquals(true, urlRepository.instagramFlow.value)
    }

    @Test
    fun `pref flow updates instagram toggle state`() = runTest(testDispatcher) {
        val urlRepository = TestUrlRepository()
        val viewModel = createViewModel(urlRepository)
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.isInstagramConversionEnabled)

        urlRepository.instagramFlow.value = false
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isInstagramConversionEnabled)
    }
}
