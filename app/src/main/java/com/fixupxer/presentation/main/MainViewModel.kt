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
        val showInstagramToggle = if (url.isNotEmpty()) {
            url.contains("instagram.com", ignoreCase = true) || 
            url.contains("kkinstagram.com", ignoreCase = true) ||
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
    
    fun setMultipleUrlsError() {
        _uiState.update { 
            it.copy(
                inputUrl = "",
                processedUrl = "",
                isInstagramUrl = false,
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
    val isInstagramConversionEnabled: Boolean = true,
    val isTwitterUrl: Boolean = false,
    val isTwitterConversionEnabled: Boolean = true,
    val error: String? = null,
    val showErrorToast: Boolean = false
) 