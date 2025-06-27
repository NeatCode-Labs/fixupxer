package com.fixupxer.presentation.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fixupxer.R
import com.fixupxer.domain.repository.UrlRepository
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
        val showInstagramToggle = if (url.isNotEmpty()) {
            url.contains("instagram.com", ignoreCase = true) || 
            url.contains("kkinstagram.com", ignoreCase = true)
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
                isInstagramUrl = showInstagramToggle,
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
        val url = _uiState.value.inputUrl.trim()
        if (url.isEmpty()) {
            _uiState.update { it.copy(processedUrl = "", error = getApplication<Application>().getString(R.string.error_please_enter_url), showErrorToast = true) }
            return
        }
        
        _uiState.update { it.copy(isLoading = true, error = null, showErrorToast = false) }
        
        viewModelScope.launch {
            try {
                // Check URL type
                val isInstagram = url.contains("instagram.com", ignoreCase = true)
                val isKkInstagram = url.contains("kkinstagram.com", ignoreCase = true)
                val isXCom = url.contains("x.com", ignoreCase = true)
                val isFixupx = url.contains("fixupx.com", ignoreCase = true)
                val isFxTwitter = url.contains("fxtwitter.com", ignoreCase = true)
                val isTwitterUrl = urlRepository.isTwitterUrl(url)
                
                // Determine if we should show toggle
                val showInstagramToggle = isInstagram || isKkInstagram
                val showTwitterToggle = isXCom || isFixupx || isFxTwitter || isTwitterUrl
                
                // Check if URL has tracking parameters
                val hasTracking = urlRepository.hasTrackingParameters(url)
                val toggleOn = if (showInstagramToggle) {
                    _uiState.value.isInstagramConversionEnabled
                } else if (showTwitterToggle) {
                    _uiState.value.isTwitterConversionEnabled
                } else {
                    false
                }
                
                // Process according to specifications
                val showNothingToDo: Boolean
                val result = when {
                    // No toggle scenarios
                    !showInstagramToggle && !showTwitterToggle -> {
                        if (!hasTracking) {
                            // Clean link, no toggle -> Nothing to do!
                            showNothingToDo = true
                            url
                        } else {
                            // Dirty link, no toggle -> Clean it
                            showNothingToDo = false
                            urlRepository.processUrl(url).url
                        }
                    }
                    
                    // Instagram scenarios
                    isInstagram && !toggleOn -> {
                        if (!hasTracking) {
                            // Clean instagram.com, toggle OFF -> Nothing to do!
                            showNothingToDo = true
                            url
                        } else {
                            // Dirty instagram.com, toggle OFF -> Just clean
                            showNothingToDo = false
                            urlRepository.processUrl(url).url
                        }
                    }
                    isInstagram && toggleOn -> {
                        // instagram.com, toggle ON -> Convert to kkinstagram (and clean if dirty)
                        showNothingToDo = false
                        urlRepository.processUrl(url).url
                    }
                    isKkInstagram && toggleOn -> {
                        if (!hasTracking) {
                            // Clean kkinstagram.com, toggle ON -> Nothing to do!
                            showNothingToDo = true
                            url
                        } else {
                            // Dirty kkinstagram.com, toggle ON -> Just clean
                            showNothingToDo = false
                            urlRepository.processUrl(url).url
                        }
                    }
                    isKkInstagram && !toggleOn -> {
                        // kkinstagram.com, toggle OFF -> Convert to instagram (and clean if dirty)
                        showNothingToDo = false
                        urlRepository.processUrl(url).url
                    }
                    
                    // X/Twitter scenarios
                    isXCom && !toggleOn -> {
                        if (!hasTracking) {
                            // Clean x.com, toggle OFF -> Nothing to do!
                            showNothingToDo = true
                            url
                        } else {
                            // Dirty x.com, toggle OFF -> Just clean
                            showNothingToDo = false
                            urlRepository.processUrl(url).url
                        }
                    }
                    isXCom && toggleOn -> {
                        // x.com, toggle ON -> Convert to fixupx (and clean if dirty)
                        showNothingToDo = false
                        urlRepository.processUrl(url).url
                    }
                    isFixupx && toggleOn -> {
                        if (!hasTracking) {
                            // Clean fixupx.com, toggle ON -> Nothing to do!
                            showNothingToDo = true
                            url
                        } else {
                            // Dirty fixupx.com, toggle ON -> Just clean
                            showNothingToDo = false
                            urlRepository.processUrl(url).url
                        }
                    }
                    isFixupx && !toggleOn -> {
                        // fixupx.com, toggle OFF -> Convert to x.com (and clean if dirty)
                        showNothingToDo = false
                        urlRepository.processUrl(url).url
                    }
                    
                    // fxtwitter scenarios
                    isFxTwitter && !toggleOn -> {
                        // fxtwitter.com, toggle OFF -> Convert to x.com
                        showNothingToDo = false
                        urlRepository.processUrl(url).url
                    }
                    isFxTwitter && toggleOn -> {
                        // fxtwitter.com, toggle ON -> Convert to fixupx.com
                        showNothingToDo = false
                        urlRepository.processUrl(url).url
                    }
                    
                    // Default case
                    else -> {
                        showNothingToDo = false
                        urlRepository.processUrl(url).url
                    }
                }
                
                // Set appropriate message
                if (showNothingToDo) {
                    _uiState.update { 
                        it.copy(
                            processedUrl = result,
                            isLoading = false,
                            error = getApplication<Application>().getString(R.string.nothing_to_do),
                            showErrorToast = true
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            processedUrl = result,
                            isLoading = false,
                            error = null,
                            showErrorToast = false
                        )
                    }
                }
            } catch (e: IllegalArgumentException) {
                Timber.w("Invalid URL: ${e.message}")
                _uiState.update { 
                    it.copy(
                        processedUrl = "",
                        isLoading = false,
                        error = e.message ?: getApplication<Application>().getString(R.string.error_invalid_url_format),
                        showErrorToast = true
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing URL")
                _uiState.update { 
                    it.copy(
                        processedUrl = "",
                        isLoading = false,
                        error = getApplication<Application>().getString(R.string.error_processing_url_with_message, e.message),
                        showErrorToast = true
                    )
                }
            }
        }
    }
    
    fun clearInput() {
        _uiState.update { 
            it.copy(
                inputUrl = "",
                processedUrl = "",
                isInstagramUrl = false,
                error = null,
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
    val isInstagramConversionEnabled: Boolean = true,
    val isTwitterUrl: Boolean = false,
    val isTwitterConversionEnabled: Boolean = true,
    val error: String? = null,
    val showErrorToast: Boolean = false
) 