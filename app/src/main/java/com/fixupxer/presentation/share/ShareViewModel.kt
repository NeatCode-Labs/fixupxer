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
                                isLoading = false,
                                error = getApplication<Application>().getString(R.string.error_no_url_found_in_shared_text)
                            )
                        }
                        return@withTimeout
                    }
                    
                    // Check URL type to determine toggle visibility
                    val isInstagram = url.contains("instagram.com", ignoreCase = true) || 
                                    url.contains("kkinstagram.com", ignoreCase = true)
                    
                    val isTwitter = url.contains("twitter.com", ignoreCase = true) ||
                                   url.contains("x.com", ignoreCase = true) ||
                                   url.contains("fixupx.com", ignoreCase = true) || 
                                   url.contains("fxtwitter.com", ignoreCase = true)
                    
                    // Facebook URLs use the Instagram toggle
                    val isFacebook = url.contains("facebook.com", ignoreCase = true) ||
                                     url.contains("facebookez.com", ignoreCase = true)
                    
                    _uiState.update { 
                        it.copy(
                            isInstagramUrl = isInstagram || isFacebook,  // Show Instagram toggle for Facebook too
                            isTwitterUrl = isTwitter
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
                        isLoading = false,
                        error = getApplication<Application>().getString(R.string.error_processing_url)
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing shared text")
                _uiState.update { 
                    it.copy(
                        processedUrl = "",
                        isLoading = false,
                        error = getApplication<Application>().getString(R.string.error_processing_url_with_message, e.message)
                    )
                }
            }
        }
    }
    
    private fun processUrlInternal(url: String) {
        Timber.d("ShareViewModel processing URL: $url")
        
        viewModelScope.launch {
            try {
                // Use processUrl which handles history saving and all conversion logic
                val result = urlRepository.processUrl(url, false)
                val processedUrl = result.url
                
                Timber.d("ShareViewModel processUrl result: $url -> $processedUrl")
                
                // Check if nothing was changed
                val finalResult = if (processedUrl == url) {
                    getApplication<Application>().getString(R.string.nothing_to_do)
                } else {
                    processedUrl
                }
                
                _uiState.update { 
                    it.copy(
                        processedUrl = finalResult,
                        isLoading = false,
                        error = null
                    )
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Error processing URL")
                _uiState.update { 
                    it.copy(
                        processedUrl = "",
                        isLoading = false,
                        error = getApplication<Application>().getString(R.string.error_processing_url_with_message, e.message)
                    )
                }
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
    
    private suspend fun reprocessUrlLocally() {
        try {
            val sharedText = _uiState.value.sharedText
            val validated = InputValidator.validateAndSanitizeInput(sharedText) ?: return
            val url = UrlProcessor.findFirstValidUrl(validated) ?: return
            
            // Get the current processed URL before reprocessing
            val currentProcessedUrl = _uiState.value.processedUrl
            
            // Skip if current result is the "nothing to do" message
            val previousUrl = if (currentProcessedUrl == getApplication<Application>().getString(R.string.nothing_to_do)) {
                null
            } else {
                currentProcessedUrl
            }
            
            // Re-process through repository with the previous result for proper history comparison
            val result = urlRepository.processUrl(url, false, previousUrl)
            val processedUrl = result.url
            
            // Check if nothing was changed
            val finalResult = if (processedUrl == url) {
                getApplication<Application>().getString(R.string.nothing_to_do)
            } else {
                processedUrl
            }
            
            _uiState.update { 
                it.copy(
                    processedUrl = finalResult,
                    isLoading = false,
                    error = null
                )
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
                isInstagramUrl = false,
                error = null
            )
        }
    }
}

/**
 * UI state for ShareActivity
 */
data class ShareUiState(
    val sharedText: String = "",
    val processedUrl: String = "",
    val isLoading: Boolean = false,
    val isInstagramUrl: Boolean = false,
    val isInstagramConversionEnabled: Boolean = true,
    val isTwitterUrl: Boolean = false,
    val isTwitterConversionEnabled: Boolean = true,
    val error: String? = null
) 