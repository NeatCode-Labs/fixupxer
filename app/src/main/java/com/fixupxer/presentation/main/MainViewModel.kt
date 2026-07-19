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
import com.fixupxer.domain.model.ProcessedUrlResult
import com.fixupxer.domain.model.ResultStatus
import com.fixupxer.domain.model.resolveResultStatus
import com.fixupxer.domain.repository.UrlRepository
import com.fixupxer.processing.LeakFinding
import com.fixupxer.processing.LinkLeakAnalyzer
import com.fixupxer.rules.CustomRuleRepository
import com.fixupxer.utils.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import timber.log.Timber
import com.fixupxer.ui.helpers.PlatformToggleHelper
import com.fixupxer.utils.ProxyPlatform
import javax.inject.Inject

/**
 * ViewModel for MainActivity
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val urlRepository: UrlRepository,
    application: Application,
    private val customRuleRepository: CustomRuleRepository? = null
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    // Both flags are only touched from viewModelScope (main dispatcher), so no
    // extra synchronization is needed.
    private var isProcessing = false
    private var pendingReprocess = false
    
    init {
        loadPreferences()
        observeCustomRuleChanges()
    }

    private fun observeCustomRuleChanges() {
        val repository = customRuleRepository ?: return
        viewModelScope.launch {
            repository.revision.drop(1).collect {
                reprocessIfResultExists()
            }
        }
        viewModelScope.launch {
            repository.enabledFlow().drop(1).collect {
                reprocessIfResultExists()
            }
        }
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
    
    private fun detectPlatformFlags(url: String): PlatformDetection {
        val isInstagram = url.isNotEmpty() && urlRepository.isInstagramUrl(url)
        val isFacebook = url.isNotEmpty() && urlRepository.isFacebookUrl(url)
        val isTwitter = url.isNotEmpty() && urlRepository.isTwitterUrl(url)
        val isTikTok = url.isNotEmpty() && urlRepository.isTikTokUrl(url)
        val isBluesky = url.isNotEmpty() && urlRepository.isBlueskyUrl(url)
        val isReddit = url.isNotEmpty() && urlRepository.isRedditUrl(url)
        val isYouTube = url.isNotEmpty() && urlRepository.isYouTubeUrl(url)
        val isPinterest = url.isNotEmpty() && urlRepository.isPinterestUrl(url)
        val isThreads = url.isNotEmpty() && urlRepository.isThreadsUrl(url)
        val detectedPlatform = PlatformToggleHelper.detectPlatform(url, urlRepository)
        return PlatformDetection(
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

    fun onUrlChanged(url: String) {
        val detection = detectPlatformFlags(url)

        _uiState.update {
            val keepResult = it.processedInputUrl.isNotEmpty() &&
                url.trim() == it.processedInputUrl
            it.copy(
                inputUrl = url,
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
                processedUrl = if (keepResult) it.processedUrl else "",
                actionUrl = if (keepResult) it.actionUrl else "",
                processedInputUrl = if (keepResult) it.processedInputUrl else "",
                resultStatus = if (keepResult) it.resultStatus else null,
                leakFindings = if (keepResult) it.leakFindings else emptyList(),
                error = null
            )
        }
    }
    
    fun onInstagramConversionToggled(enabled: Boolean) {
        if (_uiState.value.isInstagramConversionEnabled == enabled) return
        viewModelScope.launch {
            urlRepository.setInstagramConversionEnabled(enabled)
            _uiState.update { it.copy(isInstagramConversionEnabled = enabled) }
            // Reprocess path: toggle handlers only (pref Flow collectors update switch state).
            reprocessIfResultExists()
        }
    }
    
    fun onTwitterConversionToggled(enabled: Boolean) {
        if (_uiState.value.isTwitterConversionEnabled == enabled) return
        viewModelScope.launch {
            urlRepository.setTwitterConversionEnabled(enabled)
            _uiState.update { it.copy(isTwitterConversionEnabled = enabled) }
            reprocessIfResultExists()
        }
    }
    
    fun onTikTokConversionToggled(enabled: Boolean) {
        if (_uiState.value.isTikTokConversionEnabled == enabled) return
        viewModelScope.launch {
            urlRepository.setTikTokConversionEnabled(enabled)
            _uiState.update { it.copy(isTikTokConversionEnabled = enabled) }
            reprocessIfResultExists()
        }
    }

    fun onBlueskyConversionToggled(enabled: Boolean) {
        if (_uiState.value.isBlueskyConversionEnabled == enabled) return
        viewModelScope.launch {
            urlRepository.setBlueskyConversionEnabled(enabled)
            _uiState.update { it.copy(isBlueskyConversionEnabled = enabled) }
            reprocessIfResultExists()
        }
    }

    fun onFacebookConversionToggled(enabled: Boolean) {
        if (_uiState.value.isFacebookConversionEnabled == enabled) return
        viewModelScope.launch {
            urlRepository.setFacebookConversionEnabled(enabled)
            _uiState.update { it.copy(isFacebookConversionEnabled = enabled) }
            reprocessIfResultExists()
        }
    }

    fun onRedditConversionToggled(enabled: Boolean) {
        if (_uiState.value.isRedditConversionEnabled == enabled) return
        viewModelScope.launch {
            urlRepository.setRedditConversionEnabled(enabled)
            _uiState.update { it.copy(isRedditConversionEnabled = enabled) }
            reprocessIfResultExists()
        }
    }

    fun onYoutubeConversionToggled(enabled: Boolean) {
        if (_uiState.value.isYoutubeConversionEnabled == enabled) return
        viewModelScope.launch {
            urlRepository.setYoutubeConversionEnabled(enabled)
            _uiState.update { it.copy(isYoutubeConversionEnabled = enabled) }
            reprocessIfResultExists()
        }
    }

    fun onPinterestConversionToggled(enabled: Boolean) {
        if (_uiState.value.isPinterestConversionEnabled == enabled) return
        viewModelScope.launch {
            urlRepository.setPinterestConversionEnabled(enabled)
            _uiState.update { it.copy(isPinterestConversionEnabled = enabled) }
            reprocessIfResultExists()
        }
    }

    fun onThreadsConversionToggled(enabled: Boolean) {
        if (_uiState.value.isThreadsConversionEnabled == enabled) return
        viewModelScope.launch {
            urlRepository.setThreadsConversionEnabled(enabled)
            _uiState.update { it.copy(isThreadsConversionEnabled = enabled) }
            reprocessIfResultExists()
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
    
    fun processUrl() {
        // Prevent double processing
        if (isProcessing) {
            Timber.d("Already processing, ignoring duplicate request")
            return
        }
        
        val url = _uiState.value.inputUrl.trim()
        if (url.isEmpty()) {
            _uiState.update {
                it.copy(
                    processedUrl = "",
                    actionUrl = "",
                    processedInputUrl = "",
                    resultStatus = null,
                    leakFindings = emptyList(),
                    error = getApplication<Application>().getString(R.string.error_please_enter_url)
                )
            }
            return
        }
        
        isProcessing = true
        _uiState.update {
            it.copy(
                isLoading = true,
                leakFindings = emptyList(),
                error = null
            )
        }
        
        viewModelScope.launch {
            try {
                // Use processUrl which handles history saving
                val result = urlRepository.processUrl(url, false)
                applyProcessResult(url, result, isLoading = false)
            } catch (e: Exception) {
                Timber.e(e, "Error processing URL")
                _uiState.update { 
                    it.copy(
                        processedUrl = "", 
                        actionUrl = "",
                        processedInputUrl = "",
                        resultStatus = null,
                        leakFindings = emptyList(),
                        error = e.message ?: getApplication<Application>().getString(R.string.error_processing_url), 
                        isLoading = false
                    ) 
                }
            } finally {
                isProcessing = false
                runPendingReprocess()
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
        if (_uiState.value.detectedPlatform == null) return
        reprocessIfResultExists()
    }

    /**
     * Re-process the current input when conversion settings change, but only if the
     * user has already tapped Process (an actionable result exists). Pref Flow
     * collectors update toggle UI state only — they never call this method, so
     * toggle handlers are the single reprocess trigger for embed switches.
     */
    private fun reprocessIfResultExists() {
        val state = _uiState.value
        if (state.actionUrl.isEmpty()) return
        if (state.inputUrl.isBlank()) return
        if (isProcessing) {
            // A process/reprocess is already in flight — remember to re-run once
            // it finishes so a toggle flipped mid-processing is never lost.
            pendingReprocess = true
            return
        }

        isProcessing = true
        viewModelScope.launch {
            try {
                val url = state.inputUrl.trim()
                val previousProcessedUrl = state.actionUrl.takeIf { it != url }

                val result = urlRepository.processUrl(url, false, previousProcessedUrl)
                applyProcessResult(url, result)
            } catch (e: Exception) {
                Timber.e(e, "Error reprocessing URL after conversion change")
                _uiState.update {
                    it.copy(
                        processedUrl = "",
                        actionUrl = "",
                        processedInputUrl = "",
                        resultStatus = null,
                        leakFindings = emptyList(),
                        isLoading = false,
                        error = e.message
                            ?: getApplication<Application>().getString(R.string.error_processing_url)
                    )
                }
            } finally {
                isProcessing = false
                runPendingReprocess()
            }
        }
    }

    private fun runPendingReprocess() {
        if (!pendingReprocess) return
        pendingReprocess = false
        reprocessIfResultExists()
    }

    fun clearInput() {
        _uiState.update { 
            it.copy(
                inputUrl = "",
                processedUrl = "",
                actionUrl = "",
                processedInputUrl = "",
                resultStatus = null,
                leakFindings = emptyList(),
                isLoading = false,
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
                error = null
            )
        }
    }
    
    /**
     * Input failed [InputValidator] — reset the whole input/result state and show
     * a message matching the actual failure (multi-URL paste vs anything else).
     */
    fun setValidationError(reason: InputValidator.InvalidReason) {
        val messageRes = when (reason) {
            InputValidator.InvalidReason.MULTIPLE_URLS -> R.string.error_multiple_urls
            InputValidator.InvalidReason.OTHER -> R.string.error_invalid_input
        }
        _uiState.update { 
            it.copy(
                inputUrl = "",
                processedUrl = "",
                actionUrl = "",
                processedInputUrl = "",
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
                resultStatus = null,
                leakFindings = emptyList(),
                error = getApplication<Application>().getString(messageRes)
            )
        }
    }

    private fun applyProcessResult(
        inputUrl: String,
        result: ProcessedUrlResult,
        isLoading: Boolean = false
    ) {
        val processedUrl = result.url
        Timber.d("MainViewModel processed URL (inputLength=${inputUrl.length}, outputLength=${processedUrl.length})")
        val status = resolveResultStatus(inputUrl, processedUrl)
        _uiState.update {
            it.copy(
                processedUrl = processedUrl,
                actionUrl = processedUrl,
                processedInputUrl = inputUrl,
                resultStatus = status,
                leakFindings = result.leakFindings,
                isLoading = isLoading,
                error = null
            )
        }
    }

    /**
     * Removes only raw query tokens selected by the Link Guard, then re-analyzes
     * the in-memory result. It never re-enters the persistence pipeline.
     */
    fun removeLeakedParameters(parameterNames: Set<String>) {
        val state = _uiState.value
        if (state.actionUrl.isEmpty() || parameterNames.isEmpty()) return

        val strippedUrl = removeRawQueryParameters(state.actionUrl, parameterNames)
        if (strippedUrl == state.actionUrl) return

        _uiState.update {
            it.copy(
                processedUrl = strippedUrl,
                actionUrl = strippedUrl,
                resultStatus = resolveResultStatus(it.inputUrl, strippedUrl),
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
 * UI state for MainActivity
 *
 * [processedUrl] always shows the actual processed URL; [resultStatus] describes
 * whether it was already clean, cleaned, or converted. [actionUrl] is the URL the
 * Share/Open/Copy buttons act on — always a real URL or empty.
 * [processedInputUrl] is the trimmed input snapshot that produced [actionUrl];
 * the view uses it to show the strike-through diff only while the input field
 * still matches the processed text.
 */
data class MainUiState(
    val inputUrl: String = "",
    val processedUrl: String = "",
    val actionUrl: String = "",
    val processedInputUrl: String = "",
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

private data class PlatformDetection(
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