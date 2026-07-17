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

package com.fixupxer.presentation.processtext

import com.fixupxer.domain.model.ProcessedUrlResult
import com.fixupxer.domain.repository.UrlRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessTextViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var urlRepository: UrlRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        urlRepository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ProcessTextViewModel(urlRepository)

    @Test
    fun `editable exact URL replaces selection with repository result`() = runTest(testDispatcher) {
        val input = "https://example.com/article?utm_source=source"
        val cleaned = "https://example.com/article"
        whenever(urlRepository.processSharedUrl(input))
            .thenReturn(ProcessedUrlResult(cleaned, false))
        val viewModel = createViewModel()

        viewModel.onProcessTextRequested(input, readonly = false)
        advanceUntilIdle()

        assertEquals(
            ProcessTextUiState.ReplaceInline(cleaned),
            viewModel.uiState.value
        )
        verify(urlRepository).processSharedUrl(input)
    }

    @Test
    fun `editable exact URL replaces selection when repository result is identical`() =
        runTest(testDispatcher) {
            val input = "https://example.com/article"
            whenever(urlRepository.processSharedUrl(input))
                .thenReturn(ProcessedUrlResult(input, true))
            val viewModel = createViewModel()

            viewModel.onProcessTextRequested(input, readonly = false)
            advanceUntilIdle()

            assertEquals(
                ProcessTextUiState.ReplaceInline(input),
                viewModel.uiState.value
            )
            verify(urlRepository).processSharedUrl(input)
        }

    @Test
    fun `editable prose containing URL opens preview without processing`() = runTest(testDispatcher) {
        val input = "Read this: https://example.com/article"
        val viewModel = createViewModel()

        viewModel.onProcessTextRequested(input, readonly = false)
        advanceUntilIdle()

        assertEquals(ProcessTextUiState.OpenPreview(input), viewModel.uiState.value)
        verifyNoInteractions(urlRepository)
    }

    @Test
    fun `editable multi URL input opens preview without processing`() = runTest(testDispatcher) {
        val input = "https://a.com https://b.com"
        val viewModel = createViewModel()

        viewModel.onProcessTextRequested(input, readonly = false)
        advanceUntilIdle()

        assertEquals(ProcessTextUiState.OpenPreview(input), viewModel.uiState.value)
        verifyNoInteractions(urlRepository)
    }

    @Test
    fun `editable garbage opens preview without processing`() = runTest(testDispatcher) {
        val input = "hello world"
        val viewModel = createViewModel()

        viewModel.onProcessTextRequested(input, readonly = false)
        advanceUntilIdle()

        assertEquals(ProcessTextUiState.OpenPreview(input), viewModel.uiState.value)
        verifyNoInteractions(urlRepository)
    }

    @Test
    fun `null and blank text cancel`() {
        val nullViewModel = createViewModel()
        nullViewModel.onProcessTextRequested(null, readonly = false)
        assertEquals(ProcessTextUiState.Cancel, nullViewModel.uiState.value)

        val blankViewModel = createViewModel()
        blankViewModel.onProcessTextRequested("   ", readonly = false)
        assertEquals(ProcessTextUiState.Cancel, blankViewModel.uiState.value)

        verifyNoInteractions(urlRepository)
    }

    @Test
    fun `readonly text opens preview without processing`() = runTest(testDispatcher) {
        val input = "Anything selected by the source app"
        val viewModel = createViewModel()

        viewModel.onProcessTextRequested(input, readonly = true)
        advanceUntilIdle()

        assertEquals(ProcessTextUiState.OpenPreview(input), viewModel.uiState.value)
        verifyNoInteractions(urlRepository)
    }

    @Test
    fun `repository failure opens preview`() = runTest(testDispatcher) {
        val input = "https://example.com/article"
        whenever(urlRepository.processSharedUrl(input))
            .thenThrow(IllegalStateException("Repository failure"))
        val viewModel = createViewModel()

        viewModel.onProcessTextRequested(input, readonly = false)
        advanceUntilIdle()

        assertEquals(ProcessTextUiState.OpenPreview(input), viewModel.uiState.value)
        verify(urlRepository).processSharedUrl(input)
    }

    @Test
    fun `re-delivery after terminal state does not process twice`() = runTest(testDispatcher) {
        val input = "https://example.com/article"
        whenever(urlRepository.processSharedUrl(input))
            .thenReturn(ProcessedUrlResult(input, true))
        val viewModel = createViewModel()

        viewModel.onProcessTextRequested(input, readonly = false)
        advanceUntilIdle()
        viewModel.onProcessTextRequested(input, readonly = false)
        advanceUntilIdle()

        assertEquals(ProcessTextUiState.ReplaceInline(input), viewModel.uiState.value)
        verify(urlRepository, times(1)).processSharedUrl(input)
    }
}
