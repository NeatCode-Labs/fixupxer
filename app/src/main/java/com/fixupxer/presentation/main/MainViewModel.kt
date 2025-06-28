package com.fixupxer.presentation.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fixupxer.R
import com.fixupxer.domain.repository.UrlRepository
import com.fixupxer.UrlProcessor
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
                val result = when {
                    // Instagram scenarios
                    url.contains("instagram.com") && !url.contains("kkinstagram.com") -> {
                        Timber.d("MainViewModel processing URL: $url")
                        Timber.d("Scenario: Instagram URL detected")
                        when {
                            _uiState.value.isInstagramConversionEnabled -> {
                                // Toggle ON: convert to kkinstagram.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("instagram.com", "kkinstagram.com")
                                Timber.d("MainViewModel processing URL: $url -> $convertedUrl")
                                convertedUrl
                            }
                            else -> {
                                // Toggle OFF: show "Nothing to do!" for clean URLs
                                val cleanUrl = urlRepository.cleanUrl(url)
                                if (cleanUrl == url) {
                                    Timber.d("MainViewModel processing URL: $url -> Nothing to do!")
                                    getApplication<Application>().getString(R.string.nothing_to_do)
                        } else {
                                    Timber.d("MainViewModel processing URL: $url -> $cleanUrl")
                                    cleanUrl
                                }
                            }
                        }
                    }
                    
                    // kkinstagram scenarios
                    url.contains("kkinstagram.com") -> {
                        Timber.d("MainViewModel processing URL: $url")
                        Timber.d("Scenario: kkinstagram URL detected")
                        when {
                            _uiState.value.isInstagramConversionEnabled -> {
                                // Toggle ON: show "Nothing to do!" for clean URLs
                                val cleanUrl = urlRepository.cleanUrl(url)
                                if (cleanUrl == url) {
                                    Timber.d("MainViewModel processing URL: $url -> Nothing to do!")
                                    getApplication<Application>().getString(R.string.nothing_to_do)
                        } else {
                                    Timber.d("MainViewModel processing URL: $url -> $cleanUrl")
                                    cleanUrl
                                }
                            }
                            else -> {
                                // Toggle OFF: convert to instagram.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("kkinstagram.com", "instagram.com")
                                Timber.d("MainViewModel processing URL: $url -> $convertedUrl")
                                convertedUrl
                            }
                        }
                    }
                    
                    // X.com scenarios
                    url.contains("x.com") && !url.contains("fixupx.com") && !url.contains("fxtwitter.com") -> {
                        Timber.d("MainViewModel processing URL: $url")
                        Timber.d("Scenario: X.com URL detected")
                        when {
                            _uiState.value.isTwitterConversionEnabled -> {
                                // Toggle ON: convert to fixupx.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("x.com", "fixupx.com")
                                Timber.d("MainViewModel processing URL: $url -> $convertedUrl")
                                convertedUrl
                            }
                            else -> {
                                // Toggle OFF: show "Nothing to do!" for clean URLs
                                val cleanUrl = urlRepository.cleanUrl(url)
                                if (cleanUrl == url) {
                                    Timber.d("MainViewModel processing URL: $url -> Nothing to do!")
                                    getApplication<Application>().getString(R.string.nothing_to_do)
                        } else {
                                    Timber.d("MainViewModel processing URL: $url -> $cleanUrl")
                                    cleanUrl
                                }
                            }
                        }
                    }
                    
                    // fixupx.com scenarios
                    url.contains("fixupx.com") -> {
                        Timber.d("MainViewModel processing URL: $url")
                        Timber.d("Scenario: fixupx.com URL detected")
                        when {
                            _uiState.value.isTwitterConversionEnabled -> {
                                // Toggle ON: show "Nothing to do!" for clean URLs
                                val cleanUrl = urlRepository.cleanUrl(url)
                                if (cleanUrl == url) {
                                    Timber.d("MainViewModel processing URL: $url -> Nothing to do!")
                                    getApplication<Application>().getString(R.string.nothing_to_do)
                        } else {
                                    Timber.d("MainViewModel processing URL: $url -> $cleanUrl")
                                    cleanUrl
                        }
                    }
                            else -> {
                                // Toggle OFF: convert to x.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("fixupx.com", "x.com")
                                Timber.d("MainViewModel processing URL: $url -> $convertedUrl")
                                convertedUrl
                            }
                        }
                    }
                    
                    // fxtwitter.com scenarios
                    url.contains("fxtwitter.com") -> {
                        Timber.d("MainViewModel processing URL: $url")
                        Timber.d("Scenario: fxtwitter.com URL detected")
                        when {
                            _uiState.value.isTwitterConversionEnabled -> {
                                // Toggle ON: convert to fixupx.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("fxtwitter.com", "fixupx.com")
                                Timber.d("MainViewModel processing URL: $url -> $convertedUrl")
                                convertedUrl
                            }
                            else -> {
                                // Toggle OFF: convert to x.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("fxtwitter.com", "x.com")
                                Timber.d("MainViewModel processing URL: $url -> $convertedUrl")
                                convertedUrl
                            }
                        }
                    }
                    
                    // twitter.com scenarios
                    url.contains("twitter.com") && !url.contains("fxtwitter.com") -> {
                        Timber.d("MainViewModel processing URL: $url")
                        Timber.d("Scenario: twitter.com URL detected")
                        when {
                            _uiState.value.isTwitterConversionEnabled -> {
                                // Toggle ON: convert to fixupx.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("twitter.com", "fixupx.com")
                                Timber.d("MainViewModel processing URL: $url -> $convertedUrl")
                                convertedUrl
                            }
                            else -> {
                                // Toggle OFF: convert to x.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("twitter.com", "x.com")
                                Timber.d("MainViewModel processing URL: $url -> $convertedUrl")
                                convertedUrl
                            }
                        }
                    }
                    
                    // Other URLs (no toggle scenarios)
                    else -> {
                        Timber.d("MainViewModel processing URL: $url")
                        val cleanUrl = urlRepository.cleanUrl(url)
                        if (cleanUrl == url) {
                            Timber.d("MainViewModel processing URL: $url -> Nothing to do!")
                            getApplication<Application>().getString(R.string.nothing_to_do)
                        } else {
                            Timber.d("MainViewModel processing URL: $url -> $cleanUrl")
                            cleanUrl
                        }
                    }
                }
                
                _uiState.update {
                    it.copy(
                        processedUrl = result,
                        isLoading = false,
                        error = null,
                        showErrorToast = false
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