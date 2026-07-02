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
import com.fixupxer.utils.Constants
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
        viewModelScope.launch {
            urlRepository.isTikTokConversionEnabled().collect { enabled ->
                _uiState.update { it.copy(isTikTokConversionEnabled = enabled) }
            }
        }
    }
    
    fun onUrlChanged(url: String) {
        val isInstagram = url.isNotEmpty() && urlRepository.isInstagramUrl(url)

        val isFacebook = if (url.isNotEmpty()) {
            url.contains(Constants.FACEBOOK_DOMAIN, ignoreCase = true) ||
                url.contains(Constants.FACEBOOKEZ_DOMAIN, ignoreCase = true) ||
                url.contains(Constants.FB_SHORT_DOMAIN, ignoreCase = true)
        } else {
            false
        }

        val showTwitterToggle = url.isNotEmpty() && urlRepository.isTwitterUrl(url)
        val isTikTok = url.isNotEmpty() && urlRepository.isTikTokUrl(url)

        _uiState.update { 
            it.copy(
                inputUrl = url,
                isInstagramUrl = isInstagram,
                isFacebookUrl = isFacebook,
                isTwitterUrl = showTwitterToggle,
                isTikTokUrl = isTikTok,
                error = null
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
    
    fun onTikTokConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            urlRepository.setTikTokConversionEnabled(enabled)
            _uiState.update { it.copy(isTikTokConversionEnabled = enabled) }
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
            _uiState.update { it.copy(processedUrl = "", actionUrl = "", error = getApplication<Application>().getString(R.string.error_please_enter_url)) }
            return
        }
        
        isProcessing = true
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                // Use processUrl which handles history saving
                val result = urlRepository.processUrl(url, false)
                val processedUrl = result.url
                
                Timber.d("MainViewModel processUrl result: $url -> $processedUrl")
                
                // When nothing changed, display "Nothing to do!" but keep the input URL
                // as the actionable URL — it is already a valid, clean URL, so the
                // Share/Open/Copy buttons keep working.
                if (processedUrl == url) {
                    _uiState.update { 
                        it.copy(
                            processedUrl = getApplication<Application>().getString(R.string.nothing_to_do), 
                            actionUrl = url,
                            isLoading = false
                        ) 
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            processedUrl = processedUrl, 
                            actionUrl = processedUrl,
                            isLoading = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing URL")
                _uiState.update { 
                    it.copy(
                        processedUrl = "", 
                        actionUrl = "",
                        error = e.message ?: getApplication<Application>().getString(R.string.error_processing_url), 
                        isLoading = false
                    ) 
                }
            } finally {
                isProcessing = false
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
        if (state.actionUrl.isEmpty()) return
        if (state.inputUrl.isBlank()) return

        viewModelScope.launch {
            try {
                val url = state.inputUrl.trim()
                // If the previous run changed nothing (actionUrl == input), there is no
                // distinct previous result to compare against for history purposes.
                val previousProcessedUrl = state.actionUrl.takeIf { it != url }

                val result = urlRepository.processUrl(url, false, previousProcessedUrl)
                val processedUrl = result.url

                if (processedUrl == url) {
                    _uiState.update {
                        it.copy(
                            processedUrl = getApplication<Application>().getString(R.string.nothing_to_do),
                            actionUrl = url
                        )
                    }
                } else {
                    _uiState.update { it.copy(processedUrl = processedUrl, actionUrl = processedUrl) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error reprocessing URL after proxy change")
            }
        }
    }

    fun clearInput() {
        _uiState.update { 
            it.copy(
                inputUrl = "",
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
    
    fun setMultipleUrlsError() {
        _uiState.update { 
            it.copy(
                inputUrl = "",
                processedUrl = "",
                actionUrl = "",
                isInstagramUrl = false,
                isFacebookUrl = false,
                isTwitterUrl = false,
                isTikTokUrl = false,
                error = getApplication<Application>().getString(R.string.error_multiple_urls)
            )
        }
    }
}

/**
 * UI state for MainActivity
 *
 * [processedUrl] is the *display* text for the "Processed URL" field (may be the
 * localized "Nothing to do!" message); [actionUrl] is the URL the Share/Open/Copy
 * buttons act on — always a real URL or empty.
 */
data class MainUiState(
    val inputUrl: String = "",
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