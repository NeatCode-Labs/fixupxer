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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixupxer.UrlProcessor
import com.fixupxer.domain.repository.UrlRepository
import com.fixupxer.utils.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProcessTextViewModel @Inject constructor(
    private val urlRepository: UrlRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProcessTextUiState>(ProcessTextUiState.Idle)
    val uiState: StateFlow<ProcessTextUiState> = _uiState.asStateFlow()

    private var requestHandled = false

    fun onProcessTextRequested(text: String?, readonly: Boolean) {
        if (requestHandled || _uiState.value !is ProcessTextUiState.Idle) return
        requestHandled = true

        val originalText = text?.takeIf { it.isNotBlank() } ?: run {
            _uiState.value = ProcessTextUiState.Cancel
            return
        }

        if (readonly) {
            _uiState.value = ProcessTextUiState.OpenPreview(originalText)
            return
        }

        viewModelScope.launch {
            try {
                val sanitized = (InputValidator.validate(originalText)
                    as? InputValidator.ValidationResult.Valid)
                    ?.value

                if (sanitized == null || !InputValidator.isSingleUrlToken(sanitized)) {
                    _uiState.value = ProcessTextUiState.OpenPreview(originalText)
                    return@launch
                }

                val url = UrlProcessor.findFirstValidUrl(sanitized)
                if (url == null) {
                    _uiState.value = ProcessTextUiState.OpenPreview(originalText)
                    return@launch
                }

                val result = withTimeout(1_000) {
                    urlRepository.processSharedUrl(url)
                }
                _uiState.value = ProcessTextUiState.ReplaceInline(result.url)
            } catch (e: TimeoutCancellationException) {
                Timber.w("Process Text processing timed out")
                _uiState.value = ProcessTextUiState.OpenPreview(originalText)
            } catch (e: CancellationException) {
                // Never swallow coroutine cancellation (e.g. ViewModel clearing).
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Process Text processing failed")
                _uiState.value = ProcessTextUiState.OpenPreview(originalText)
            }
        }
    }
}

sealed interface ProcessTextUiState {
    data object Idle : ProcessTextUiState
    data class ReplaceInline(val cleanedUrl: String) : ProcessTextUiState
    data class OpenPreview(val originalText: String) : ProcessTextUiState
    data object Cancel : ProcessTextUiState
}
