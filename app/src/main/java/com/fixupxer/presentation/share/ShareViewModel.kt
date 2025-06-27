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

/**
 * ViewModel for ShareActivity
 */
@HiltViewModel
class ShareViewModel @Inject constructor(
    private val urlRepository: UrlRepository,
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
                // Try to find a URL in the shared text
                val url = UrlProcessor.findFirstValidUrl(sharedText)
                if (url == null) {
                    _uiState.update { 
                        it.copy(
                            processedUrl = "",
                            isLoading = false,
                            error = getApplication<Application>().getString(R.string.error_no_url_found_in_shared_text)
                        )
                    }
                    return@launch
                }
                
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
                
                _uiState.update { 
                    it.copy(
                        isInstagramUrl = showInstagramToggle, 
                        isTwitterUrl = showTwitterToggle
                    ) 
                }
                
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
                val result: ProcessedUrlResult
                val showNothingToDo: Boolean
                
                when {
                    // No toggle scenarios
                    !showInstagramToggle && !showTwitterToggle -> {
                        if (!hasTracking) {
                            // Clean link, no toggle -> Nothing to do!
                            result = ProcessedUrlResult(url, true)
                            showNothingToDo = true
                        } else {
                            // Dirty link, no toggle -> Clean it
                            result = urlRepository.processUrl(url, forceCleanTracking = true)
                            showNothingToDo = false
                        }
                    }
                    
                    // Instagram scenarios
                    isInstagram && !toggleOn -> {
                        if (!hasTracking) {
                            // Clean instagram.com, toggle OFF -> Nothing to do!
                            result = ProcessedUrlResult(url, true)
                            showNothingToDo = true
                        } else {
                            // Dirty instagram.com, toggle OFF -> Just clean
                            result = urlRepository.processUrl(url, forceCleanTracking = true)
                            showNothingToDo = false
                        }
                    }
                    isInstagram && toggleOn -> {
                        // instagram.com, toggle ON -> Convert to kkinstagram (and clean if dirty)
                        val processedUrl = urlRepository.processUrlForSharing(url)
                        result = ProcessedUrlResult(processedUrl, false)
                        showNothingToDo = false
                    }
                    isKkInstagram && toggleOn -> {
                        if (!hasTracking) {
                            // Clean kkinstagram.com, toggle ON -> Nothing to do!
                            result = ProcessedUrlResult(url, true)
                            showNothingToDo = true
                        } else {
                            // Dirty kkinstagram.com, toggle ON -> Just clean
                            result = urlRepository.processUrl(url, forceCleanTracking = true)
                            showNothingToDo = false
                        }
                    }
                    isKkInstagram && !toggleOn -> {
                        // kkinstagram.com, toggle OFF -> Convert to instagram (and clean if dirty)
                        result = urlRepository.processUrl(url, forceCleanTracking = true)
                        showNothingToDo = false
                    }
                    
                    // X/Twitter scenarios
                    isXCom && !toggleOn -> {
                        if (!hasTracking) {
                            // Clean x.com, toggle OFF -> Nothing to do!
                            result = ProcessedUrlResult(url, true)
                            showNothingToDo = true
                        } else {
                            // Dirty x.com, toggle OFF -> Just clean
                            result = urlRepository.processUrl(url, forceCleanTracking = true)
                            showNothingToDo = false
                        }
                    }
                    isXCom && toggleOn -> {
                        // x.com, toggle ON -> Convert to fixupx (and clean if dirty)
                        val processedUrl = urlRepository.processUrlForSharing(url)
                        result = ProcessedUrlResult(processedUrl, false)
                        showNothingToDo = false
                    }
                    isFixupx && toggleOn -> {
                        if (!hasTracking) {
                            // Clean fixupx.com, toggle ON -> Nothing to do!
                            result = ProcessedUrlResult(url, true)
                            showNothingToDo = true
                        } else {
                            // Dirty fixupx.com, toggle ON -> Just clean
                            result = urlRepository.processUrl(url, forceCleanTracking = true)
                            showNothingToDo = false
                        }
                    }
                    isFixupx && !toggleOn -> {
                        // fixupx.com, toggle OFF -> Convert to x.com (and clean if dirty)
                        result = urlRepository.processUrl(url, forceCleanTracking = true)
                        showNothingToDo = false
                    }
                    
                    // fxtwitter scenarios
                    isFxTwitter && !toggleOn -> {
                        // fxtwitter.com, toggle OFF -> Convert to x.com (and clean if dirty)
                        result = urlRepository.processUrl(url, forceCleanTracking = true)
                        showNothingToDo = false
                    }
                    isFxTwitter && toggleOn -> {
                        // fxtwitter.com, toggle ON -> Convert to fixupx.com (and clean if dirty)
                        val processedUrl = urlRepository.processUrlForSharing(url)
                        result = ProcessedUrlResult(processedUrl, false)
                        showNothingToDo = false
                    }
                    
                    // Default case
                    else -> {
                        result = urlRepository.processUrl(url, forceCleanTracking = true)
                        showNothingToDo = false
                    }
                }
                
                // Set appropriate message
                val message = if (showNothingToDo) {
                    getApplication<Application>().getString(R.string.nothing_to_do)
                } else {
                    null
                }
                
                _uiState.update { 
                    it.copy(
                        processedUrl = result.url,
                        isLoading = false,
                        error = message
                    )
                }
            } catch (e: IllegalArgumentException) {
                _uiState.update { 
                    it.copy(
                        processedUrl = "",
                        isLoading = false,
                        error = getApplication<Application>().getString(R.string.error_no_valid_url_found)
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing shared URL")
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
            
            // Re-process the URL with new setting
            val sharedText = _uiState.value.sharedText
            if (sharedText.isNotEmpty()) {
                processSharedText(sharedText)
            }
        }
    }
    
    fun onTwitterConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            urlRepository.setTwitterConversionEnabled(enabled)
            _uiState.update { it.copy(isTwitterConversionEnabled = enabled) }
            
            // Re-process the URL with new setting
            val sharedText = _uiState.value.sharedText
            if (sharedText.isNotEmpty()) {
                processSharedText(sharedText)
            }
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