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
                    val isTwitter = urlRepository.isTwitterUrl(url) || 
                                   url.contains("x.com", ignoreCase = true) ||
                                   url.contains("fixupx.com", ignoreCase = true) || 
                                   url.contains("fxtwitter.com", ignoreCase = true)
                    
                    _uiState.update { 
                        it.copy(
                            isInstagramUrl = isInstagram, 
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
                val result = when {
                    // Instagram scenarios
                    url.contains("instagram.com") && !url.contains("kkinstagram.com") -> {
                        Timber.d("ShareViewModel Scenario: Instagram URL detected")
                when {
                            _uiState.value.isInstagramConversionEnabled -> {
                                // Toggle ON: convert to kkinstagram.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("instagram.com", "kkinstagram.com")
                                Timber.d("ShareViewModel Instagram toggle ON: $url -> $convertedUrl")
                                convertedUrl
                            }
                            else -> {
                                // Toggle OFF: show "Nothing to do!" for clean URLs
                                val cleanUrl = urlRepository.cleanUrl(url)
                                if (cleanUrl == url) {
                                    Timber.d("ShareViewModel Instagram toggle OFF: $url -> Nothing to do!")
                                    getApplication<Application>().getString(R.string.nothing_to_do)
                        } else {
                                    Timber.d("ShareViewModel Instagram toggle OFF: $url -> $cleanUrl")
                                    cleanUrl
                                }
                            }
                        }
                    }
                    
                    // kkinstagram scenarios
                    url.contains("kkinstagram.com") -> {
                        Timber.d("ShareViewModel Scenario: kkinstagram URL detected")
                        when {
                            _uiState.value.isInstagramConversionEnabled -> {
                                // Toggle ON: show "Nothing to do!" for clean URLs
                                val cleanUrl = urlRepository.cleanUrl(url)
                                if (cleanUrl == url) {
                                    Timber.d("ShareViewModel kkinstagram toggle ON: $url -> Nothing to do!")
                                    getApplication<Application>().getString(R.string.nothing_to_do)
                        } else {
                                    Timber.d("ShareViewModel kkinstagram toggle ON: $url -> $cleanUrl")
                                    cleanUrl
                                }
                            }
                            else -> {
                                // Toggle OFF: convert to instagram.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("kkinstagram.com", "instagram.com")
                                Timber.d("ShareViewModel kkinstagram toggle OFF: $url -> $convertedUrl")
                                convertedUrl
                        }
                    }
                    }
                    
                    // X.com scenarios
                    url.contains("x.com") && !url.contains("fixupx.com") && !url.contains("fxtwitter.com") -> {
                        Timber.d("ShareViewModel Scenario: X.com URL detected")
                        when {
                            _uiState.value.isTwitterConversionEnabled -> {
                                // Toggle ON: convert to fixupx.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("x.com", "fixupx.com")
                                Timber.d("ShareViewModel X.com toggle ON: $url -> $convertedUrl")
                                convertedUrl
                            }
                            else -> {
                                // Toggle OFF: show "Nothing to do!" for clean URLs
                                val cleanUrl = urlRepository.cleanUrl(url)
                                if (cleanUrl == url) {
                                    Timber.d("ShareViewModel X.com toggle OFF: $url -> Nothing to do!")
                                    getApplication<Application>().getString(R.string.nothing_to_do)
                        } else {
                                    Timber.d("ShareViewModel X.com toggle OFF: $url -> $cleanUrl")
                                    cleanUrl
                                }
                            }
                        }
                    }
                    
                    // fixupx.com scenarios
                    url.contains("fixupx.com") -> {
                        Timber.d("ShareViewModel Scenario: fixupx.com URL detected")
                        when {
                            _uiState.value.isTwitterConversionEnabled -> {
                                // Toggle ON: show "Nothing to do!" for clean URLs
                                val cleanUrl = urlRepository.cleanUrl(url)
                                if (cleanUrl == url) {
                                    Timber.d("ShareViewModel fixupx.com toggle ON: $url -> Nothing to do!")
                                    getApplication<Application>().getString(R.string.nothing_to_do)
                        } else {
                                    Timber.d("ShareViewModel fixupx.com toggle ON: $url -> $cleanUrl")
                                    cleanUrl
                        }
                    }
                            else -> {
                                // Toggle OFF: convert to x.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("fixupx.com", "x.com")
                                Timber.d("ShareViewModel fixupx.com toggle OFF: $url -> $convertedUrl")
                                convertedUrl
                            }
                        }
                    }
                    
                    // fxtwitter.com scenarios
                    url.contains("fxtwitter.com") -> {
                        Timber.d("ShareViewModel Scenario: fxtwitter.com URL detected")
                        when {
                            _uiState.value.isTwitterConversionEnabled -> {
                                // Toggle ON: convert to fixupx.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("fxtwitter.com", "fixupx.com")
                                Timber.d("ShareViewModel fxtwitter.com toggle ON: $url -> $convertedUrl")
                                convertedUrl
                            }
                            else -> {
                                // Toggle OFF: convert to x.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("fxtwitter.com", "x.com")
                                Timber.d("ShareViewModel fxtwitter.com toggle OFF: $url -> $convertedUrl")
                                convertedUrl
                            }
                        }
                    }
                    
                    // twitter.com scenarios
                    url.contains("twitter.com") && !url.contains("fxtwitter.com") -> {
                        Timber.d("ShareViewModel Scenario: twitter.com URL detected")
                        when {
                            _uiState.value.isTwitterConversionEnabled -> {
                                // Toggle ON: convert to fixupx.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("twitter.com", "fixupx.com")
                                Timber.d("ShareViewModel twitter.com toggle ON: $url -> $convertedUrl")
                                convertedUrl
                            }
                            else -> {
                                // Toggle OFF: convert to x.com
                                val cleanUrl = urlRepository.cleanUrl(url)
                                val convertedUrl = cleanUrl.replace("twitter.com", "x.com")
                                Timber.d("ShareViewModel twitter.com toggle OFF: $url -> $convertedUrl")
                                convertedUrl
                            }
                        }
                    }
                    
                    // Other URLs (no toggle scenarios)
                    else -> {
                        Timber.d("ShareViewModel Scenario: Other URL detected")
                        val cleanUrl = urlRepository.cleanUrl(url)
                        if (cleanUrl == url) {
                            Timber.d("ShareViewModel Other URL: $url -> Nothing to do!")
                    getApplication<Application>().getString(R.string.nothing_to_do)
                } else {
                            Timber.d("ShareViewModel Other URL: $url -> $cleanUrl")
                            cleanUrl
                        }
                    }
                }
                
                _uiState.update {
                    it.copy(
                        processedUrl = result,
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