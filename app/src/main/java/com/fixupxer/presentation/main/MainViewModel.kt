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
import com.fixupxer.domain.repository.UrlRepository
import com.fixupxer.UrlProcessor
import com.fixupxer.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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
    
    private var isProcessing = false
    
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
    
    fun onUrlChanged(url: String) {
        val isInstagram = if (url.isNotEmpty()) {
            url.contains(Constants.INSTAGRAM_DOMAIN, ignoreCase = true) ||
                Constants.INSTAGRAM_ALL_KNOWN_PROXIES.any { url.contains(it, ignoreCase = true) }
        } else {
            false
        }

        val isFacebook = if (url.isNotEmpty()) {
            url.contains("facebook.com", ignoreCase = true) ||
            url.contains("facebookez.com", ignoreCase = true)
        } else {
            false
        }
        
        val showTwitterToggle = if (url.isNotEmpty()) {
            urlRepository.isTwitterUrl(url) || 
            url.contains("x.com", ignoreCase = true) ||
            url.contains("fixupx.com", ignoreCase = true) || 
            url.contains("fxtwitter.com", ignoreCase = true)
        } else {
            false
        }
        
        _uiState.update { 
            it.copy(
                inputUrl = url,
                isInstagramUrl = isInstagram,
                isFacebookUrl = isFacebook,
                isTwitterUrl = showTwitterToggle,
                error = null,
                showErrorToast = false
            )
        }
    }
    
    fun onInstagramConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            urlRepository.setInstagramConversionEnabled(enabled)
            _uiState.update { it.copy(isInstagramConversionEnabled = enabled) }
        }
    }
    
    fun onTwitterConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            urlRepository.setTwitterConversionEnabled(enabled)
            _uiState.update { it.copy(isTwitterConversionEnabled = enabled) }
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
            _uiState.update { it.copy(processedUrl = "", error = getApplication<Application>().getString(R.string.error_please_enter_url), showErrorToast = true) }
            return
        }
        
        isProcessing = true
        _uiState.update { it.copy(isLoading = true, error = null, showErrorToast = false) }
        
        viewModelScope.launch {
            try {
                // Use processUrl which handles history saving
                val result = urlRepository.processUrl(url, false)
                val processedUrl = result.url
                
                Timber.d("MainViewModel processUrl result: $url -> $processedUrl")
                
                // Check if nothing was changed
                if (processedUrl == url) {
                    _uiState.update { 
                        it.copy(
                            processedUrl = getApplication<Application>().getString(R.string.nothing_to_do), 
                            isLoading = false
                        ) 
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            processedUrl = processedUrl, 
                            isLoading = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing URL")
                _uiState.update { 
                    it.copy(
                        processedUrl = "", 
                        error = e.message ?: getApplication<Application>().getString(R.string.error_processing_url), 
                        isLoading = false,
                        showErrorToast = true
                    ) 
                }
            } finally {
                isProcessing = false
            }
        }
    }
    
    /**
     * Re-process the current input after the user picks a different Instagram proxy.
     * Only acts when (a) the input is an Instagram URL (the only platform with multiple
     * proxies), and (b) the user has already pressed Process at least once (so a
     * Processed URL is currently displayed). Otherwise no-op — the regular Process
     * button still owns the first-time processing flow.
     *
     * Mirrors `ShareViewModel.reprocessUrlLocally()` semantics: passes the previous
     * processed URL to the repository so history snapshots are not duplicated.
     */
    fun reprocessAfterProxyChange() {
        val state = _uiState.value
        if (!state.isInstagramUrl) return
        if (state.processedUrl.isEmpty()) return
        if (state.inputUrl.isBlank()) return

        viewModelScope.launch {
            try {
                val url = state.inputUrl.trim()
                val nothingToDo = getApplication<Application>().getString(R.string.nothing_to_do)
                val previousProcessedUrl = if (state.processedUrl == nothingToDo) {
                    null
                } else {
                    state.processedUrl
                }

                val result = urlRepository.processUrl(url, false, previousProcessedUrl)
                val processedUrl = result.url

                val finalResult = if (processedUrl == url) nothingToDo else processedUrl
                _uiState.update { it.copy(processedUrl = finalResult) }
            } catch (e: Exception) {
                Timber.e(e, "Error reprocessing URL after Instagram proxy change")
            }
        }
    }

    fun clearInput() {
        _uiState.update { 
            it.copy(
                inputUrl = "",
                processedUrl = "",
                isInstagramUrl = false,
                isFacebookUrl = false,
                error = null,
                showErrorToast = false
            )
        }
    }
    
    fun setMultipleUrlsError() {
        _uiState.update { 
            it.copy(
                inputUrl = "",
                processedUrl = "",
                isInstagramUrl = false,
                isFacebookUrl = false,
                error = getApplication<Application>().getString(R.string.error_multiple_urls),
                showErrorToast = false
            )
        }
    }
}

/**
 * UI state for MainActivity
 */
data class MainUiState(
    val inputUrl: String = "",
    val processedUrl: String = "",
    val isLoading: Boolean = false,
    val isInstagramUrl: Boolean = false,
    val isFacebookUrl: Boolean = false,
    val isInstagramConversionEnabled: Boolean = true,
    val isTwitterUrl: Boolean = false,
    val isTwitterConversionEnabled: Boolean = true,
    val error: String? = null,
    val showErrorToast: Boolean = false
) 