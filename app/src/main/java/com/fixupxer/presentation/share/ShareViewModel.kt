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
import com.fixupxer.domain.repository.UrlRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.fixupxer.utils.InputValidator
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

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
        _uiState.update { it.copy(sharedText = sharedText, isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                withTimeout(1000) { // 1 second timeout
                    val validated = InputValidator.validateAndSanitizeInput(sharedText)
                    
                    if (validated == null) {
                        _uiState.update {
                            it.copy(
                                processedUrl = "",
                                isLoading = false,
                                isInstagramUrl = false,
                                isTwitterUrl = false,
                                error = getApplication<Application>().getString(R.string.error_multiple_urls)
                            )
                        }
                        return@withTimeout
                    }
                    
                    // Continue with existing logic using validated input
                    val url = UrlProcessor.findFirstValidUrl(validated)

                    if (url == null) {
                        _uiState.update { 
                            it.copy(
                                processedUrl = "",
                                actionUrl = "",
                                isLoading = false,
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
                        isLoading = false,
                        error = getApplication<Application>().getString(R.string.error_processing_url_with_message, e.message)
                    )
                }
            }
        }
    }
    
    // Runs inside the caller's coroutine (no nested launch — a second launch would
    // escape processSharedText's withTimeout and allow racing state updates).
    private suspend fun processUrlInternal(url: String) {
        Timber.d("ShareViewModel processing URL: $url")
        
        try {
            // Use processUrl which handles history saving and all conversion logic
            val result = urlRepository.processUrl(url, false)
            val processedUrl = result.url
            
            Timber.d("ShareViewModel processUrl result: $url -> $processedUrl")
            
            // When nothing changed, show "Nothing to do!" but keep the input URL as
            // the actionable URL so Copy/Share/Open still operate on a real URL.
            if (processedUrl == url) {
                _uiState.update {
                    it.copy(
                        processedUrl = getApplication<Application>().getString(R.string.nothing_to_do),
                        actionUrl = url,
                        isLoading = false,
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        processedUrl = processedUrl,
                        actionUrl = processedUrl,
                        isLoading = false,
                        error = null
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing URL")
            _uiState.update { 
                it.copy(
                    processedUrl = "",
                    actionUrl = "",
                    isLoading = false,
                    error = getApplication<Application>().getString(R.string.error_processing_url_with_message, e.message)
                )
            }
        }
    }
    
    fun onInstagramConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            urlRepository.setInstagramConversionEnabled(enabled)
            _uiState.update { it.copy(isInstagramConversionEnabled = enabled) }
            
            // Re-process the URL with new setting WITHOUT going through the full flow
            val sharedText = _uiState.value.sharedText
            if (sharedText.isNotEmpty()) {
                reprocessUrlLocally()
            }
        }
    }
    
    fun onTwitterConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            urlRepository.setTwitterConversionEnabled(enabled)
            _uiState.update { it.copy(isTwitterConversionEnabled = enabled) }
            
            // Re-process the URL with new setting WITHOUT going through the full flow
            val sharedText = _uiState.value.sharedText
            if (sharedText.isNotEmpty()) {
                reprocessUrlLocally()
            }
        }
    }
    
    fun onTikTokConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            urlRepository.setTikTokConversionEnabled(enabled)
            _uiState.update { it.copy(isTikTokConversionEnabled = enabled) }
            
            // Re-process the URL with new setting WITHOUT going through the full flow
            val sharedText = _uiState.value.sharedText
            if (sharedText.isNotEmpty()) {
                reprocessUrlLocally()
            }
        }
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
            val result = urlRepository.processUrl(url, false, previousUrl)
            val processedUrl = result.url
            
            if (processedUrl == url) {
                _uiState.update {
                    it.copy(
                        processedUrl = getApplication<Application>().getString(R.string.nothing_to_do),
                        actionUrl = url,
                        isLoading = false,
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        processedUrl = processedUrl,
                        actionUrl = processedUrl,
                        isLoading = false,
                        error = null
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reprocessing URL locally")
        }
    }
    
    fun clearState() {
        _uiState.update { 
            it.copy(
                sharedText = "",
                processedUrl = "",
                actionUrl = "",
                isLoading = false,
                isInstagramUrl = false,
                isFacebookUrl = false,
                isTwitterUrl = false,
                isTikTokUrl = false,
                error = null
            )
        }
    }
}

/**
 * UI state for ShareActivity
 *
 * [processedUrl] is the *display* text for the "Processed URL" field (may be the
 * localized "Nothing to do!" message); [actionUrl] is the URL the Copy/Share/Open
 * buttons act on — always a real URL or empty.
 */
data class ShareUiState(
    val sharedText: String = "",
    val processedUrl: String = "",
    val actionUrl: String = "",
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