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
import com.fixupxer.domain.model.ResultStatus
import com.fixupxer.domain.model.resolveResultStatus
import com.fixupxer.domain.repository.UrlRepository
import com.fixupxer.processing.LeakFinding
import com.fixupxer.processing.LinkLeakAnalyzer
import com.fixupxer.utils.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import com.fixupxer.ui.helpers.PlatformToggleHelper
import com.fixupxer.utils.ProxyPlatform
import javax.inject.Inject
import timber.log.Timber

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
    
    // Both flags are only touched from viewModelScope (main dispatcher), so no
    // extra synchronization is needed.
    private var isProcessing = false
    private var pendingReprocess = false
    
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
        viewModelScope.launch {
            urlRepository.isBlueskyConversionEnabled().collect { enabled ->
                _uiState.update { it.copy(isBlueskyConversionEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            urlRepository.isFacebookConversionEnabled().collect { enabled ->
                _uiState.update { it.copy(isFacebookConversionEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            urlRepository.isRedditConversionEnabled().collect { enabled ->
                _uiState.update { it.copy(isRedditConversionEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            urlRepository.isYoutubeConversionEnabled().collect { enabled ->
                _uiState.update { it.copy(isYoutubeConversionEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            urlRepository.isPinterestConversionEnabled().collect { enabled ->
                _uiState.update { it.copy(isPinterestConversionEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            urlRepository.isThreadsConversionEnabled().collect { enabled ->
                _uiState.update { it.copy(isThreadsConversionEnabled = enabled) }
            }
        }
    }

    private fun detectPlatformFlags(url: String): SharePlatformDetection {
        val isInstagram = urlProcessor.isInstagramUrl(url)
        val isTwitter = urlProcessor.isTwitterUrl(url)
        val isFacebook = urlProcessor.isFacebookUrl(url)
        val isTikTok = urlProcessor.isTikTokUrl(url)
        val isBluesky = urlProcessor.isBlueskyUrl(url)
        val isReddit = urlProcessor.isRedditUrl(url)
        val isYouTube = urlProcessor.isYouTubeUrl(url)
        val isPinterest = urlProcessor.isPinterestUrl(url)
        val isThreads = urlProcessor.isThreadsUrl(url)
        val detectedPlatform = PlatformToggleHelper.detectPlatform(url, urlRepository)
        return SharePlatformDetection(
            isInstagramUrl = isInstagram,
            isFacebookUrl = isFacebook,
            isTwitterUrl = isTwitter,
            isTikTokUrl = isTikTok,
            isBlueskyUrl = isBluesky,
            isRedditUrl = isReddit,
            isYouTubeUrl = isYouTube,
            isPinterestUrl = isPinterest,
            isThreadsUrl = isThreads,
            detectedPlatform = detectedPlatform,
        )
    }

    private fun clearPlatformFlags(state: ShareUiState): ShareUiState =
        state.copy(
            isInstagramUrl = false,
            isFacebookUrl = false,
            isTwitterUrl = false,
            isTikTokUrl = false,
            isBlueskyUrl = false,
            isRedditUrl = false,
            isYouTubeUrl = false,
            isPinterestUrl = false,
            isThreadsUrl = false,
            detectedPlatform = null,
        )
    
    fun processSharedText(sharedText: String) {
        // Guard against re-delivery of the same intent (e.g. the activity being
        // recreated on a configuration change): the existing result — or the
        // still-running first pass — is valid, and reprocessing would write a
        // duplicate history entry.
        val current = _uiState.value
        if (sharedText == current.sharedText &&
            (current.isLoading || current.actionUrl.isNotEmpty() || current.error != null)
        ) {
            return
        }
        
        _uiState.update {
            clearPlatformFlags(it).copy(
                sharedText = sharedText,
                isLoading = true,
                leakFindings = emptyList(),
                error = null
            )
        }
        
        isProcessing = true
        viewModelScope.launch {
            try {
                withTimeout(1000) { // 1 second timeout
                    val validation = InputValidator.validate(sharedText)
                    
                    if (validation is InputValidator.ValidationResult.Invalid) {
                        val messageRes = when (validation.reason) {
                            InputValidator.InvalidReason.MULTIPLE_URLS -> R.string.error_multiple_urls
                            InputValidator.InvalidReason.OTHER -> R.string.error_invalid_input
                        }
                        _uiState.update {
                            clearPlatformFlags(it).copy(
                                processedUrl = "",
                                actionUrl = "",
                                resultStatus = null,
                                leakFindings = emptyList(),
                                isLoading = false,
                                error = getApplication<Application>().getString(messageRes)
                            )
                        }
                        return@withTimeout
                    }
                    
                    val validated = (validation as InputValidator.ValidationResult.Valid).value
                    
                    // Continue with existing logic using validated input
                    val url = UrlProcessor.findFirstValidUrl(validated)

                    if (url == null) {
                        _uiState.update {
                            clearPlatformFlags(it).copy(
                                processedUrl = "",
                                actionUrl = "",
                                resultStatus = null,
                                leakFindings = emptyList(),
                                isLoading = false,
                                error = getApplication<Application>().getString(R.string.error_no_url_found_in_shared_text)
                            )
                        }
                        return@withTimeout
                    }
                    
                    // Check URL type to determine toggle visibility
                    val detection = detectPlatformFlags(url)
                    
                    _uiState.update { 
                        it.copy(
                            isInstagramUrl = detection.isInstagramUrl,
                            isFacebookUrl = detection.isFacebookUrl,
                            isTwitterUrl = detection.isTwitterUrl,
                            isTikTokUrl = detection.isTikTokUrl,
                            isBlueskyUrl = detection.isBlueskyUrl,
                            isRedditUrl = detection.isRedditUrl,
                            isYouTubeUrl = detection.isYouTubeUrl,
                            isPinterestUrl = detection.isPinterestUrl,
                            isThreadsUrl = detection.isThreadsUrl,
                            detectedPlatform = detection.detectedPlatform,
                        ) 
                    }
                    
                    // Process the URL
                    processUrlInternal(url)
                }
            } catch (e: TimeoutCancellationException) {
                Timber.w("Share processing timed out")
                _uiState.update {
                    clearPlatformFlags(it).copy(
                        processedUrl = "",
                        actionUrl = "",
                        resultStatus = null,
                        leakFindings = emptyList(),
                        isLoading = false,
                        error = getApplication<Application>().getString(R.string.error_processing_url)
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing shared text")
                _uiState.update {
                    clearPlatformFlags(it).copy(
                        processedUrl = "",
                        actionUrl = "",
                        resultStatus = null,
                        leakFindings = emptyList(),
                        isLoading = false,
                        error = getApplication<Application>().getString(R.string.error_processing_url_with_message, e.message)
                    )
                }
            } finally {
                isProcessing = false
                runPendingReprocess()
            }
        }
    }
    
    // Runs inside the caller's coroutine (no nested launch — a second launch would
    // escape processSharedText's withTimeout and allow racing state updates).
    private suspend fun processUrlInternal(url: String) {
        Timber.d("ShareViewModel processing URL (length=${url.length})")
        
        try {
            val result = urlRepository.processSharedUrl(url)
            applyProcessResult(url, result)
        } catch (e: Exception) {
            Timber.e(e, "Error processing URL")
            _uiState.update {
                clearPlatformFlags(it).copy(
                    processedUrl = "",
                    actionUrl = "",
                    resultStatus = null,
                    leakFindings = emptyList(),
                    isLoading = false,
                    error = getApplication<Application>().getString(R.string.error_processing_url_with_message, e.message)
                )
            }
        }
    }
    
    fun onInstagramConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            if (_uiState.value.isInstagramConversionEnabled != enabled) {
                urlRepository.setInstagramConversionEnabled(enabled)
                _uiState.update { it.copy(isInstagramConversionEnabled = enabled) }
                requestReprocess()
            }
        }
    }
    
    fun onTwitterConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            if (_uiState.value.isTwitterConversionEnabled != enabled) {
                urlRepository.setTwitterConversionEnabled(enabled)
                _uiState.update { it.copy(isTwitterConversionEnabled = enabled) }
                requestReprocess()
            }
        }
    }
    
    fun onTikTokConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            if (_uiState.value.isTikTokConversionEnabled != enabled) {
                urlRepository.setTikTokConversionEnabled(enabled)
                _uiState.update { it.copy(isTikTokConversionEnabled = enabled) }
                requestReprocess()
            }
        }
    }

    fun onBlueskyConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            if (_uiState.value.isBlueskyConversionEnabled != enabled) {
                urlRepository.setBlueskyConversionEnabled(enabled)
                _uiState.update { it.copy(isBlueskyConversionEnabled = enabled) }
                requestReprocess()
            }
        }
    }

    fun onFacebookConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            if (_uiState.value.isFacebookConversionEnabled != enabled) {
                urlRepository.setFacebookConversionEnabled(enabled)
                _uiState.update { it.copy(isFacebookConversionEnabled = enabled) }
                requestReprocess()
            }
        }
    }

    fun onRedditConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            if (_uiState.value.isRedditConversionEnabled != enabled) {
                urlRepository.setRedditConversionEnabled(enabled)
                _uiState.update { it.copy(isRedditConversionEnabled = enabled) }
                requestReprocess()
            }
        }
    }

    fun onYoutubeConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            if (_uiState.value.isYoutubeConversionEnabled != enabled) {
                urlRepository.setYoutubeConversionEnabled(enabled)
                _uiState.update { it.copy(isYoutubeConversionEnabled = enabled) }
                requestReprocess()
            }
        }
    }

    fun onPinterestConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            if (_uiState.value.isPinterestConversionEnabled != enabled) {
                urlRepository.setPinterestConversionEnabled(enabled)
                _uiState.update { it.copy(isPinterestConversionEnabled = enabled) }
                requestReprocess()
            }
        }
    }

    fun onThreadsConversionToggled(enabled: Boolean) {
        viewModelScope.launch {
            if (_uiState.value.isThreadsConversionEnabled != enabled) {
                urlRepository.setThreadsConversionEnabled(enabled)
                _uiState.update { it.copy(isThreadsConversionEnabled = enabled) }
                requestReprocess()
            }
        }
    }

    fun onPlatformConversionToggled(platform: ProxyPlatform, enabled: Boolean) {
        when (platform) {
            ProxyPlatform.INSTAGRAM -> onInstagramConversionToggled(enabled)
            ProxyPlatform.X -> onTwitterConversionToggled(enabled)
            ProxyPlatform.FACEBOOK -> onFacebookConversionToggled(enabled)
            ProxyPlatform.TIKTOK -> onTikTokConversionToggled(enabled)
            ProxyPlatform.BLUESKY -> onBlueskyConversionToggled(enabled)
            ProxyPlatform.REDDIT -> onRedditConversionToggled(enabled)
            ProxyPlatform.YOUTUBE -> onYoutubeConversionToggled(enabled)
            ProxyPlatform.PINTEREST -> onPinterestConversionToggled(enabled)
            ProxyPlatform.THREADS -> onThreadsConversionToggled(enabled)
        }
    }

    fun notifyProxySelectionChanged() {
        _uiState.update { it.copy(proxySelectionRevision = it.proxySelectionRevision + 1) }
    }
    
    /**
     * Re-process the shared URL after the user picks a different Instagram or
     * TikTok proxy in the picker dialog (the toggle value itself is unchanged,
     * so the toggle handlers above won't fire). Mirrors
     * `MainViewModel.reprocessAfterProxyChange()`.
     */
    fun reprocessAfterProxyChange() {
        if (_uiState.value.detectedPlatform == null) return
        requestReprocess()
    }
    
    private fun requestReprocess() {
        if (_uiState.value.sharedText.isEmpty()) return
        if (isProcessing) {
            // Initial processing (or another reprocess) is still in flight —
            // re-run once it finishes so this change is never lost.
            pendingReprocess = true
            return
        }
        isProcessing = true
        viewModelScope.launch {
            try {
                reprocessUrlLocally()
            } finally {
                isProcessing = false
                runPendingReprocess()
            }
        }
    }
    
    private fun runPendingReprocess() {
        if (!pendingReprocess) return
        pendingReprocess = false
        requestReprocess()
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
            val result = urlRepository.processSharedUrl(url, previousUrl)
            applyProcessResult(url, result)
        } catch (e: Exception) {
            Timber.e(e, "Error reprocessing URL locally")
            _uiState.update {
                clearPlatformFlags(it).copy(
                    processedUrl = "",
                    actionUrl = "",
                    resultStatus = null,
                    leakFindings = emptyList(),
                    isLoading = false,
                    error = getApplication<Application>().getString(R.string.error_processing_url)
                )
            }
        }
    }
    
    /**
     * Nothing usable arrived in the share intent (no EXTRA_TEXT and no clip
     * data) — show an explicit error instead of an endless "Processing…".
     */
    fun setNoSharedText() {
        _uiState.update {
            clearPlatformFlags(it).copy(
                sharedText = "",
                processedUrl = "",
                actionUrl = "",
                resultStatus = null,
                leakFindings = emptyList(),
                isLoading = false,
                error = getApplication<Application>().getString(R.string.error_no_url_found_in_shared_text)
            )
        }
    }
    
    fun clearState() {
        _uiState.update {
            clearPlatformFlags(it).copy(
                sharedText = "",
                processedUrl = "",
                actionUrl = "",
                resultStatus = null,
                leakFindings = emptyList(),
                isLoading = false,
                error = null
            )
        }
    }

    private fun applyProcessResult(
        inputUrl: String,
        result: ProcessedUrlResult,
        isLoading: Boolean = false
    ) {
        val processedUrl = result.url
        Timber.d("ShareViewModel processed URL (inputLength=${inputUrl.length}, outputLength=${processedUrl.length})")
        val status = resolveResultStatus(inputUrl, processedUrl)
        _uiState.update {
            it.copy(
                processedUrl = processedUrl,
                actionUrl = processedUrl,
                resultStatus = status,
                leakFindings = result.leakFindings,
                isLoading = isLoading,
                error = null
            )
        }
    }

    /**
     * Removes only raw query tokens selected by the Link Guard without sending
     * the edited result back through history persistence.
     */
    fun removeLeakedParameters(parameterNames: Set<String>) {
        val state = _uiState.value
        if (state.actionUrl.isEmpty() || parameterNames.isEmpty()) return

        val strippedUrl = removeRawQueryParameters(state.actionUrl, parameterNames)
        if (strippedUrl == state.actionUrl) return

        val inputUrl = UrlProcessor.findFirstValidUrl(state.sharedText) ?: strippedUrl
        _uiState.update {
            it.copy(
                processedUrl = strippedUrl,
                actionUrl = strippedUrl,
                resultStatus = resolveResultStatus(inputUrl, strippedUrl),
                leakFindings = LinkLeakAnalyzer.analyze(strippedUrl)
            )
        }
    }

    private fun removeRawQueryParameters(url: String, names: Set<String>): String {
        val fragmentStart = url.indexOf('#')
        val queryStart = url.indexOf('?')
        if (queryStart < 0 || (fragmentStart >= 0 && queryStart > fragmentStart)) return url

        val queryEnd = if (fragmentStart >= 0) fragmentStart else url.length
        val remainingTokens = url.substring(queryStart + 1, queryEnd)
            .split('&')
            .filterNot { token -> token.substringBefore('=') in names }
        val prefix = url.substring(0, queryStart)
        val suffix = url.substring(queryEnd)
        return if (remainingTokens.isEmpty()) {
            prefix + suffix
        } else {
            "$prefix?${remainingTokens.joinToString("&")}$suffix"
        }
    }
}

/**
 * UI state for ShareActivity
 *
 * [processedUrl] always shows the actual processed URL; [resultStatus] describes
 * whether it was already clean, cleaned, or converted. [actionUrl] is the URL the
 * Copy/Share/Open buttons act on — always a real URL or empty.
 */
data class ShareUiState(
    val sharedText: String = "",
    val processedUrl: String = "",
    val actionUrl: String = "",
    val resultStatus: ResultStatus? = null,
    val leakFindings: List<LeakFinding> = emptyList(),
    val isLoading: Boolean = false,
    val isInstagramUrl: Boolean = false,
    val isFacebookUrl: Boolean = false,
    val isInstagramConversionEnabled: Boolean = true,
    val isFacebookConversionEnabled: Boolean = true,
    val isTwitterUrl: Boolean = false,
    val isTwitterConversionEnabled: Boolean = true,
    val isTikTokUrl: Boolean = false,
    val isTikTokConversionEnabled: Boolean = true,
    val isBlueskyUrl: Boolean = false,
    val isBlueskyConversionEnabled: Boolean = true,
    val isRedditUrl: Boolean = false,
    val isRedditConversionEnabled: Boolean = false,
    val isYouTubeUrl: Boolean = false,
    val isYoutubeConversionEnabled: Boolean = false,
    val isPinterestUrl: Boolean = false,
    val isPinterestConversionEnabled: Boolean = false,
    val isThreadsUrl: Boolean = false,
    val isThreadsConversionEnabled: Boolean = false,
    val detectedPlatform: ProxyPlatform? = null,
    val proxySelectionRevision: Int = 0,
    val error: String? = null
)

private data class SharePlatformDetection(
    val isInstagramUrl: Boolean,
    val isFacebookUrl: Boolean,
    val isTwitterUrl: Boolean,
    val isTikTokUrl: Boolean,
    val isBlueskyUrl: Boolean,
    val isRedditUrl: Boolean,
    val isYouTubeUrl: Boolean,
    val isPinterestUrl: Boolean,
    val isThreadsUrl: Boolean,
    val detectedPlatform: ProxyPlatform?,
) 