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


package com.fixupxer.presentation.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fixupxer.R
import com.fixupxer.domain.model.ProcessedUrlResult
import com.fixupxer.domain.model.ResultStatus
import com.fixupxer.domain.model.resolveResultStatus
import com.fixupxer.domain.repository.UrlRepository
import com.fixupxer.utils.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for MainActivity
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val urlRepository: UrlRepository,
    application: Application
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    // Both flags are only touched from viewModelScope (main dispatcher), so no
    // extra synchronization is needed.
    private var isProcessing = false
    private var pendingReprocess = false
    
    init {
        loadPreferences()
    }
    
    private fun loadPreferences() {
        viewModelScope.launch {
            urlRepository.isInstagramConversionEnabled().collect { enabled ->
                _uiState.update { it.copy(isInstagramConversionEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            urlRepository.isTwitterConversionEnabled().collect { enabled ->
                _uiState.update { it.copy(isTwitterConversionEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            urlRepository.isTikTokConversionEnabled().collect { enabled ->
                _uiState.update { it.copy(isTikTokConversionEnabled = enabled) }
            }
        }
    }
    
    fun onUrlChanged(url: String) {
        val isInstagram = url.isNotEmpty() && urlRepository.isInstagramUrl(url)
        val isFacebook = url.isNotEmpty() && urlRepository.isFacebookUrl(url)
        val showTwitterToggle = url.isNotEmpty() && urlRepository.isTwitterUrl(url)
        val isTikTok = url.isNotEmpty() && urlRepository.isTikTokUrl(url)

        _uiState.update {
            // A result belongs to the exact input snapshot that produced it
            // (processedInputUrl). As soon as the text differs, the old result,
            // status chip and action URL are stale — clear them so the buttons
            // and the result card never act on the previous URL.
            val keepResult = it.processedInputUrl.isNotEmpty() &&
                url.trim() == it.processedInputUrl
            it.copy(
                inputUrl = url,
                isInstagramUrl = isInstagram,
                isFacebookUrl = isFacebook,
                isTwitterUrl = showTwitterToggle,
                isTikTokUrl = isTikTok,
                processedUrl = if (keepResult) it.processedUrl else "",
                actionUrl = if (keepResult) it.actionUrl else "",
                processedInputUrl = if (keepResult) it.processedInputUrl else "",
                resultStatus = if (keepResult) it.resultStatus else null,
                error = null
            )
        }
    }
    
    fun onInstagramConversionToggled(enabled: Boolean) {
        if (_uiState.value.isInstagramConversionEnabled == enabled) return
        viewModelScope.launch {
            urlRepository.setInstagramConversionEnabled(enabled)
            _uiState.update { it.copy(isInstagramConversionEnabled = enabled) }
            // Reprocess path: toggle handlers only (pref Flow collectors update switch state).
            reprocessIfResultExists()
        }
    }
    
    fun onTwitterConversionToggled(enabled: Boolean) {
        if (_uiState.value.isTwitterConversionEnabled == enabled) return
        viewModelScope.launch {
            urlRepository.setTwitterConversionEnabled(enabled)
            _uiState.update { it.copy(isTwitterConversionEnabled = enabled) }
            reprocessIfResultExists()
        }
    }
    
    fun onTikTokConversionToggled(enabled: Boolean) {
        if (_uiState.value.isTikTokConversionEnabled == enabled) return
        viewModelScope.launch {
            urlRepository.setTikTokConversionEnabled(enabled)
            _uiState.update { it.copy(isTikTokConversionEnabled = enabled) }
            reprocessIfResultExists()
        }
    }
    
    fun processUrl() {
        // Prevent double processing
        if (isProcessing) {
            Timber.d("Already processing, ignoring duplicate request")
            return
        }
        
        val url = _uiState.value.inputUrl.trim()
        if (url.isEmpty()) {
            _uiState.update {
                it.copy(
                    processedUrl = "",
                    actionUrl = "",
                    processedInputUrl = "",
                    resultStatus = null,
                    error = getApplication<Application>().getString(R.string.error_please_enter_url)
                )
            }
            return
        }
        
        isProcessing = true
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                // Use processUrl which handles history saving
                val result = urlRepository.processUrl(url, false)
                applyProcessResult(url, result, isLoading = false)
            } catch (e: Exception) {
                Timber.e(e, "Error processing URL")
                _uiState.update { 
                    it.copy(
                        processedUrl = "", 
                        actionUrl = "",
                        processedInputUrl = "",
                        resultStatus = null,
                        error = e.message ?: getApplication<Application>().getString(R.string.error_processing_url), 
                        isLoading = false
                    ) 
                }
            } finally {
                isProcessing = false
                runPendingReprocess()
            }
        }
    }
    
    /**
     * Re-process the current input after the user picks a different Instagram or
     * TikTok proxy. Only acts when (a) the input is an Instagram/TikTok URL (the
     * platforms with multiple proxies), and (b) the user has already pressed Process
     * at least once (so an actionable URL exists). Otherwise no-op — the regular
     * Process button still owns the first-time processing flow.
     *
     * Mirrors `ShareViewModel.reprocessUrlLocally()` semantics: passes the previous
     * processed URL to the repository so history snapshots are not duplicated.
     */
    fun reprocessAfterProxyChange() {
        val state = _uiState.value
        if (!state.isInstagramUrl && !state.isTikTokUrl) return
        reprocessIfResultExists()
    }

    /**
     * Re-process the current input when conversion settings change, but only if the
     * user has already tapped Process (an actionable result exists). Pref Flow
     * collectors update toggle UI state only — they never call this method, so
     * toggle handlers are the single reprocess trigger for embed switches.
     */
    private fun reprocessIfResultExists() {
        val state = _uiState.value
        if (state.actionUrl.isEmpty()) return
        if (state.inputUrl.isBlank()) return
        if (isProcessing) {
            // A process/reprocess is already in flight — remember to re-run once
            // it finishes so a toggle flipped mid-processing is never lost.
            pendingReprocess = true
            return
        }

        isProcessing = true
        viewModelScope.launch {
            try {
                val url = state.inputUrl.trim()
                val previousProcessedUrl = state.actionUrl.takeIf { it != url }

                val result = urlRepository.processUrl(url, false, previousProcessedUrl)
                applyProcessResult(url, result)
            } catch (e: Exception) {
                Timber.e(e, "Error reprocessing URL after conversion change")
                _uiState.update {
                    it.copy(
                        processedUrl = "",
                        actionUrl = "",
                        processedInputUrl = "",
                        resultStatus = null,
                        isLoading = false,
                        error = e.message
                            ?: getApplication<Application>().getString(R.string.error_processing_url)
                    )
                }
            } finally {
                isProcessing = false
                runPendingReprocess()
            }
        }
    }

    private fun runPendingReprocess() {
        if (!pendingReprocess) return
        pendingReprocess = false
        reprocessIfResultExists()
    }

    fun clearInput() {
        _uiState.update { 
            it.copy(
                inputUrl = "",
                processedUrl = "",
                actionUrl = "",
                processedInputUrl = "",
                resultStatus = null,
                isLoading = false,
                isInstagramUrl = false,
                isFacebookUrl = false,
                isTwitterUrl = false,
                isTikTokUrl = false,
                error = null
            )
        }
    }
    
    /**
     * Input failed [InputValidator] — reset the whole input/result state and show
     * a message matching the actual failure (multi-URL paste vs anything else).
     */
    fun setValidationError(reason: InputValidator.InvalidReason) {
        val messageRes = when (reason) {
            InputValidator.InvalidReason.MULTIPLE_URLS -> R.string.error_multiple_urls
            InputValidator.InvalidReason.OTHER -> R.string.error_invalid_input
        }
        _uiState.update { 
            it.copy(
                inputUrl = "",
                processedUrl = "",
                actionUrl = "",
                processedInputUrl = "",
                isInstagramUrl = false,
                isFacebookUrl = false,
                isTwitterUrl = false,
                isTikTokUrl = false,
                resultStatus = null,
                error = getApplication<Application>().getString(messageRes)
            )
        }
    }

    private fun applyProcessResult(
        inputUrl: String,
        result: ProcessedUrlResult,
        isLoading: Boolean = false
    ) {
        val processedUrl = result.url
        Timber.d("MainViewModel processUrl result: $inputUrl -> $processedUrl")
        _uiState.update {
            it.copy(
                processedUrl = processedUrl,
                actionUrl = processedUrl,
                processedInputUrl = inputUrl,
                resultStatus = resolveResultStatus(inputUrl, processedUrl),
                isLoading = isLoading,
                error = null
            )
        }
    }
}

/**
 * UI state for MainActivity
 *
 * [processedUrl] always shows the actual processed URL; [resultStatus] describes
 * whether it was already clean, cleaned, or converted. [actionUrl] is the URL the
 * Share/Open/Copy buttons act on — always a real URL or empty.
 * [processedInputUrl] is the trimmed input snapshot that produced [actionUrl];
 * the view uses it to show the strike-through diff only while the input field
 * still matches the processed text.
 */
data class MainUiState(
    val inputUrl: String = "",
    val processedUrl: String = "",
    val actionUrl: String = "",
    val processedInputUrl: String = "",
    val resultStatus: ResultStatus? = null,
    val isLoading: Boolean = false,
    val isInstagramUrl: Boolean = false,
    val isFacebookUrl: Boolean = false,
    val isInstagramConversionEnabled: Boolean = true,
    val isTwitterUrl: Boolean = false,
    val isTwitterConversionEnabled: Boolean = true,
    val isTikTokUrl: Boolean = false,
    val isTikTokConversionEnabled: Boolean = true,
    val error: String? = null
) 