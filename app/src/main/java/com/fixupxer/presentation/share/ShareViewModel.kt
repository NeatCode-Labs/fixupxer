package com.fixupxer.presentation.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * ViewModel for ShareActivity
 */
@HiltViewModel
class ShareViewModel @Inject constructor(
    private val urlRepository: UrlRepository
) : ViewModel() {
    
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
    }
    
    fun handleSharedText(sharedText: String?) {
        if (sharedText.isNullOrEmpty()) {
            _uiState.update { 
                it.copy(
                    sharedText = "",
                    processedUrl = "",
                    isInstagramUrl = false,
                    error = "No URL found in shared text"
                )
            }
            return
        }
        
        _uiState.update { 
            it.copy(
                sharedText = sharedText,
                isInstagramUrl = urlRepository.isInstagramUrl(sharedText)
            )
        }
        
        // Automatically process the URL
        processSharedUrl(sharedText)
    }
    
    fun onInstagramConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            urlRepository.setInstagramConversionEnabled(enabled)
            _uiState.update { it.copy(isInstagramConversionEnabled = enabled) }
            
            // Re-process the URL with new setting
            val sharedText = _uiState.value.sharedText
            if (sharedText.isNotEmpty()) {
                processSharedUrl(sharedText)
            }
        }
    }
    
    private fun processSharedUrl(url: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                val processed = urlRepository.processUrl(url)
                _uiState.update { 
                    it.copy(
                        processedUrl = processed,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing shared URL")
                _uiState.update { 
                    it.copy(
                        processedUrl = url,
                        isLoading = false,
                        error = "Error processing URL: ${e.message}"
                    )
                }
            }
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
    val error: String? = null
) 