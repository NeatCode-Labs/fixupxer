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


package com.fixupxer.presentation.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fixupxer.R
import com.fixupxer.UrlProcessor
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
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import javax.inject.Inject
import timber.log.Timber

/**
 * ViewModel for ShareActivity
 */
@HiltViewModel
class ShareViewModel @Inject constructor(
    private val urlRepository: UrlRepository,
    private val urlProcessor: UrlProcessor,
    application: Application
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()
    
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
    
    fun processSharedText(sharedText: String) {
        // Guard against re-delivery of the same intent (e.g. the activity being
        // recreated on a configuration change): the existing result — or the
        // still-running first pass — is valid, and reprocessing would write a
        // duplicate history entry.
        val current = _uiState.value
        if (sharedText == current.sharedText &&
            (current.isLoading || current.actionUrl.isNotEmpty() || current.error != null)
        ) {
            return
        }
        
        _uiState.update { it.copy(sharedText = sharedText, isLoading = true, error = null) }
        
        isProcessing = true
        viewModelScope.launch {
            try {
                withTimeout(1000) { // 1 second timeout
                    val validation = InputValidator.validate(sharedText)
                    
                    if (validation is InputValidator.ValidationResult.Invalid) {
                        val messageRes = when (validation.reason) {
                            InputValidator.InvalidReason.MULTIPLE_URLS -> R.string.error_multiple_urls
                            InputValidator.InvalidReason.OTHER -> R.string.error_invalid_input
                        }
                        _uiState.update {
                            it.copy(
                                processedUrl = "",
                                actionUrl = "",
                                resultStatus = null,
                                isLoading = false,
                                isInstagramUrl = false,
                                isFacebookUrl = false,
                                isTwitterUrl = false,
                                isTikTokUrl = false,
                                error = getApplication<Application>().getString(messageRes)
                            )
                        }
                        return@withTimeout
                    }
                    
                    val validated = (validation as InputValidator.ValidationResult.Valid).value
                    
                    // Continue with existing logic using validated input
                    val url = UrlProcessor.findFirstValidUrl(validated)

                    if (url == null) {
                        _uiState.update { 
                            it.copy(
                                processedUrl = "",
                                actionUrl = "",
                                resultStatus = null,
                                isLoading = false,
                                isInstagramUrl = false,
                                isFacebookUrl = false,
                                isTwitterUrl = false,
                                isTikTokUrl = false,
                                error = getApplication<Application>().getString(R.string.error_no_url_found_in_shared_text)
                            )
                        }
                        return@withTimeout
                    }
                    
                    // Check URL type to determine toggle visibility
                    val isInstagram = urlProcessor.isInstagramUrl(url)
                    val isTwitter = urlProcessor.isTwitterUrl(url)
                    // Facebook URLs use the Instagram toggle
                    val isFacebook = urlProcessor.isFacebookUrl(url)
                    val isTikTok = urlProcessor.isTikTokUrl(url)
                    
                    _uiState.update { 
                        it.copy(
                            isInstagramUrl = isInstagram,
                            isFacebookUrl = isFacebook,
                            isTwitterUrl = isTwitter,
                            isTikTokUrl = isTikTok
                        ) 
                    }
                    
                    // Process the URL
                    processUrlInternal(url)
                }
            } catch (e: TimeoutCancellationException) {
                Timber.w("Share processing timed out")
                _uiState.update { 
                    it.copy(
                        processedUrl = "",
                        actionUrl = "",
                        resultStatus = null,
                        isLoading = false,
                        error = getApplication<Application>().getString(R.string.error_processing_url)
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing shared text")
                _uiState.update { 
                    it.copy(
                        processedUrl = "",
                        actionUrl = "",
                        resultStatus = null,
                        isLoading = false,
                        error = getApplication<Application>().getString(R.string.error_processing_url_with_message, e.message)
                    )
                }
            } finally {
                isProcessing = false
                runPendingReprocess()
            }
        }
    }
    
    // Runs inside the caller's coroutine (no nested launch — a second launch would
    // escape processSharedText's withTimeout and allow racing state updates).
    private suspend fun processUrlInternal(url: String) {
        Timber.d("ShareViewModel processing URL: $url")
        
        try {
            val result = urlRepository.processSharedUrl(url)
            applyProcessResult(url, result)
        } catch (e: Exception) {
            Timber.e(e, "Error processing URL")
            _uiState.update { 
                it.copy(
                    processedUrl = "",
                    actionUrl = "",
                    resultStatus = null,
                    isLoading = false,
                    error = getApplication<Application>().getString(R.string.error_processing_url_with_message, e.message)
                )
            }
        }
    }
    
    fun onInstagramConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            if (_uiState.value.isInstagramConversionEnabled != enabled) {
                urlRepository.setInstagramConversionEnabled(enabled)
                _uiState.update { it.copy(isInstagramConversionEnabled = enabled) }
                requestReprocess()
            }
        }
    }
    
    fun onTwitterConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            if (_uiState.value.isTwitterConversionEnabled != enabled) {
                urlRepository.setTwitterConversionEnabled(enabled)
                _uiState.update { it.copy(isTwitterConversionEnabled = enabled) }
                requestReprocess()
            }
        }
    }
    
    fun onTikTokConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            if (_uiState.value.isTikTokConversionEnabled != enabled) {
                urlRepository.setTikTokConversionEnabled(enabled)
                _uiState.update { it.copy(isTikTokConversionEnabled = enabled) }
                requestReprocess()
            }
        }
    }
    
    /**
     * Re-process the shared URL after the user picks a different Instagram or
     * TikTok proxy in the picker dialog (the toggle value itself is unchanged,
     * so the toggle handlers above won't fire). Mirrors
     * `MainViewModel.reprocessAfterProxyChange()`.
     */
    fun reprocessAfterProxyChange() {
        val state = _uiState.value
        if (!state.isInstagramUrl && !state.isTikTokUrl) return
        requestReprocess()
    }
    
    private fun requestReprocess() {
        if (_uiState.value.sharedText.isEmpty()) return
        if (isProcessing) {
            // Initial processing (or another reprocess) is still in flight —
            // re-run once it finishes so this change is never lost.
            pendingReprocess = true
            return
        }
        isProcessing = true
        viewModelScope.launch {
            try {
                reprocessUrlLocally()
            } finally {
                isProcessing = false
                runPendingReprocess()
            }
        }
    }
    
    private fun runPendingReprocess() {
        if (!pendingReprocess) return
        pendingReprocess = false
        requestReprocess()
    }
    
    private suspend fun reprocessUrlLocally() {
        try {
            val sharedText = _uiState.value.sharedText
            val validated = InputValidator.validateAndSanitizeInput(sharedText) ?: return
            val url = UrlProcessor.findFirstValidUrl(validated) ?: return
            
            // If the previous run changed nothing (actionUrl == input), there is no
            // distinct previous result to compare against for history purposes.
            val previousUrl = _uiState.value.actionUrl.takeIf { it.isNotEmpty() && it != url }
            
            // Re-process through repository with the previous result for proper history comparison
            val result = urlRepository.processSharedUrl(url, previousUrl)
            applyProcessResult(url, result)
        } catch (e: Exception) {
            Timber.e(e, "Error reprocessing URL locally")
            _uiState.update {
                it.copy(
                    processedUrl = "",
                    actionUrl = "",
                    resultStatus = null,
                    isLoading = false,
                    error = getApplication<Application>().getString(R.string.error_processing_url)
                )
            }
        }
    }
    
    /**
     * Nothing usable arrived in the share intent (no EXTRA_TEXT and no clip
     * data) — show an explicit error instead of an endless "Processing…".
     */
    fun setNoSharedText() {
        _uiState.update {
            it.copy(
                sharedText = "",
                processedUrl = "",
                actionUrl = "",
                resultStatus = null,
                isLoading = false,
                isInstagramUrl = false,
                isFacebookUrl = false,
                isTwitterUrl = false,
                isTikTokUrl = false,
                error = getApplication<Application>().getString(R.string.error_no_url_found_in_shared_text)
            )
        }
    }
    
    fun clearState() {
        _uiState.update { 
            it.copy(
                sharedText = "",
                processedUrl = "",
                actionUrl = "",
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

    private fun applyProcessResult(
        inputUrl: String,
        result: ProcessedUrlResult,
        isLoading: Boolean = false
    ) {
        val processedUrl = result.url
        Timber.d("ShareViewModel processUrl result: $inputUrl -> $processedUrl")
        _uiState.update {
            it.copy(
                processedUrl = processedUrl,
                actionUrl = processedUrl,
                resultStatus = resolveResultStatus(inputUrl, processedUrl),
                isLoading = isLoading,
                error = null
            )
        }
    }
}

/**
 * UI state for ShareActivity
 *
 * [processedUrl] always shows the actual processed URL; [resultStatus] describes
 * whether it was already clean, cleaned, or converted. [actionUrl] is the URL the
 * Copy/Share/Open buttons act on — always a real URL or empty.
 */
data class ShareUiState(
    val sharedText: String = "",
    val processedUrl: String = "",
    val actionUrl: String = "",
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