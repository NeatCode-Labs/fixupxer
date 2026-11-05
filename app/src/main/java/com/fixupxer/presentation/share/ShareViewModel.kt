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
import com.fixupxer.processing.PipelineStatus
import com.fixupxer.utils.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    private var processingJob: Job? = null
    private var requestGeneration = 0L
    private var resultRevision = 0L
    private var autoCopyEnabled = false
    private var lastAttemptedCopyUrl: String? = null
    private var autoShareArmed = false
    
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
    
    fun processSharedText(
        sharedText: String,
        autoCopy: Boolean = false,
        newRequest: Boolean = false,
        autoShare: Boolean = false,
    ) {
        // A recreated Activity redelivers the old intent; onNewIntent is a new
        // request even when its text happens to match the previous share.
        val current = _uiState.value
        if (!newRequest && sharedText == current.sharedText &&
            (current.isLoading || current.actionUrl.isNotEmpty() || current.error != null)
        ) return

        invalidateRequest()
        autoCopyEnabled = autoCopy
        autoShareArmed = autoShare
        _uiState.update {
            clearPlatformFlags(it).copy(
                sharedText = sharedText,
                processedUrl = "",
                actionUrl = "",
                resultStatus = null,
                isLoading = true,
                leakFindings = emptyList(),
                error = null,
                pendingAutoCopy = null,
                autoCopyBlocked = false,
                pendingAutoShare = null,
                showResultScreen = !autoShare,
                autoShareNotice = null,
            )
        }
        startProcessing(sharedText, previousUrl = null)
    }

    private fun invalidateRequest() {
        requestGeneration++
        processingJob?.cancel()
        processingJob = null
        isProcessing = false
        pendingReprocess = false
        lastAttemptedCopyUrl = null
        autoShareArmed = false
    }

    private fun startProcessing(sharedText: String, previousUrl: String?) {
        val generation = requestGeneration
        isProcessing = true
        _uiState.update { it.copy(isLoading = true, pendingAutoCopy = null, autoCopyBlocked = false, pendingAutoShare = null) }
        processingJob = viewModelScope.launch {
            try {
                val validation = InputValidator.validate(sharedText)
                if (validation is InputValidator.ValidationResult.Invalid) {
                    val message = when (validation.reason) {
                        InputValidator.InvalidReason.MULTIPLE_URLS -> R.string.error_multiple_urls
                        InputValidator.InvalidReason.OTHER -> R.string.error_invalid_input
                    }
                    showProcessingError(generation, message)
                    return@launch
                }
                val url = UrlProcessor.findFirstValidUrl(
                    (validation as InputValidator.ValidationResult.Valid).value
                )
                if (url == null) {
                    showProcessingError(generation, R.string.error_no_url_found_in_shared_text)
                    return@launch
                }
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
                var previous = previousUrl?.takeIf { it != url }
                do {
                    pendingReprocess = false
                    val result = withTimeout(1000) {
                        urlRepository.processSharedUrl(url, previous)
                    }
                    currentCoroutineContext().ensureActive()
                    if (generation != requestGeneration) return@launch
                    // A preference changed while this pass was in flight. Keep
                    // actions disabled until the queued pass produces its result.
                    applyProcessResult(url, result, isLoading = pendingReprocess)
                    previous = result.url.takeIf { it.isNotEmpty() && it != url }
                } while (pendingReprocess)
            } catch (e: TimeoutCancellationException) {
                Timber.w("Share processing timed out")
                showProcessingError(generation, R.string.error_processing_url)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error processing shared URL")
                showProcessingError(generation, R.string.error_processing_url)
            } finally {
                // An older cancelled job must not clear a newer request's flags.
                if (generation == requestGeneration) {
                    isProcessing = false
                    pendingReprocess = false
                }
            }
        }
    }

    private fun showProcessingError(generation: Long, message: Int) {
        if (generation != requestGeneration) return
        autoShareArmed = false
        _uiState.update {
            clearPlatformFlags(it).copy(
                processedUrl = "",
                actionUrl = "",
                resultStatus = null,
                leakFindings = emptyList(),
                isLoading = false,
                error = getApplication<Application>().getString(message),
                pendingAutoCopy = null,
                autoCopyBlocked = false,
                pendingAutoShare = null,
                showResultScreen = true,
                autoShareNotice = null,
            )
        }
    }

    /** Consume on the UI thread immediately before writing, without suspension. */
    fun consumeAutoCopy(copy: ShareAutoCopy): String? {
        val state = _uiState.value
        if (state.pendingAutoCopy != copy || copy.generation != requestGeneration ||
            state.isLoading || state.error != null || state.processingStatus != PipelineStatus.COMPLETE ||
            state.leakFindings.isNotEmpty() ||
            state.actionUrl != copy.url
        ) return null
        // Consume attempts, including failures: retries stay an explicit Copy tap.
        lastAttemptedCopyUrl = copy.url
        _uiState.update { it.copy(pendingAutoCopy = null) }
        return copy.url
    }

    fun onManualCopy() {
        onManualInteraction()
        lastAttemptedCopyUrl = _uiState.value.actionUrl
        _uiState.update { it.copy(pendingAutoCopy = null) }
    }

    /** Consume before launching the chooser, on the UI thread without suspension. */
    fun consumeAutoShare(share: ShareAutoShare): String? {
        val state = _uiState.value
        if (!autoShareArmed || state.pendingAutoShare != share || share.generation != requestGeneration ||
            state.isLoading || state.error != null || state.processingStatus != PipelineStatus.COMPLETE ||
            state.leakFindings.isNotEmpty() || state.actionUrl != share.url
        ) return null
        autoShareArmed = false
        _uiState.update { it.copy(pendingAutoShare = null) }
        return share.url
    }

    fun onAutoShareFailed() {
        autoShareArmed = false
        _uiState.update {
            it.copy(pendingAutoShare = null, showResultScreen = true, autoShareNotice = R.string.error_sharing_url)
        }
    }

    fun onManualInteraction() {
        autoShareArmed = false
        _uiState.update { it.copy(pendingAutoShare = null, showResultScreen = true, autoShareNotice = null) }
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
        onManualInteraction()
        val state = _uiState.value
        if (state.sharedText.isEmpty()) return
        _uiState.update { it.copy(isLoading = true, pendingAutoCopy = null, autoCopyBlocked = false) }
        if (isProcessing) {
            pendingReprocess = true
        } else {
            startProcessing(state.sharedText, state.actionUrl.takeIf { it.isNotEmpty() })
        }
    }

    /**
     * Nothing usable arrived in the share intent (no EXTRA_TEXT and no clip
     * data) — show an explicit error instead of an endless "Processing…".
     */
    fun setNoSharedText() {
        invalidateRequest()
        _uiState.update {
            clearPlatformFlags(it).copy(
                sharedText = "",
                pendingAutoCopy = null,
                autoCopyBlocked = false,
                pendingAutoShare = null,
                showResultScreen = true,
                autoShareNotice = null,
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
        invalidateRequest()
        _uiState.update {
            clearPlatformFlags(it).copy(
                sharedText = "",
                pendingAutoCopy = null,
                autoCopyBlocked = false,
                pendingAutoShare = null,
                showResultScreen = true,
                autoShareNotice = null,
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
        val safe = result.status == PipelineStatus.COMPLETE && result.leakFindings.isEmpty() && processedUrl.isNotEmpty()
        val shareBlocked = autoShareArmed && !isLoading && !safe
        val pendingShare = if (autoShareArmed && !isLoading && safe) {
            ShareAutoShare(requestGeneration, ++resultRevision, processedUrl)
        } else null
        // Once review is needed, editing the result must never launch a chooser unexpectedly.
        if (shareBlocked) autoShareArmed = false
        val pendingCopy = if (!isLoading && autoCopyEnabled && result.status == PipelineStatus.COMPLETE &&
            result.leakFindings.isEmpty() &&
            processedUrl.isNotEmpty() && processedUrl != lastAttemptedCopyUrl
        ) ShareAutoCopy(requestGeneration, ++resultRevision, processedUrl) else null
        _uiState.update {
            it.copy(
                processedUrl = processedUrl,
                actionUrl = processedUrl,
                resultStatus = status,
                leakFindings = result.leakFindings,
                isLoading = isLoading,
                pendingAutoCopy = pendingCopy,
                autoCopyBlocked = !isLoading && autoCopyEnabled &&
                    (result.leakFindings.isNotEmpty() || result.status != PipelineStatus.COMPLETE),
                processingStatus = result.status,
                pendingAutoShare = pendingShare,
                showResultScreen = it.showResultScreen || shareBlocked,
                autoShareNotice = if (shareBlocked) {
                    if (result.leakFindings.isNotEmpty()) R.string.share_after_clean_blocked
                    else R.string.share_after_clean_incomplete
                } else it.autoShareNotice,
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
        if (state.isLoading || state.actionUrl.isEmpty() || parameterNames.isEmpty()) return

        val strippedUrl = removeRawQueryParameters(state.actionUrl, parameterNames)
        if (strippedUrl == state.actionUrl) return
        onManualInteraction()

        val inputUrl = UrlProcessor.findFirstValidUrl(state.sharedText) ?: strippedUrl
        applyProcessResult(
            inputUrl,
            ProcessedUrlResult(
                strippedUrl,
                false,
                leakFindings = LinkLeakAnalyzer.analyze(strippedUrl),
                status = state.processingStatus ?: PipelineStatus.COMPLETE,
            ),
        )
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
    val error: String? = null,
    val pendingAutoCopy: ShareAutoCopy? = null,
    val autoCopyBlocked: Boolean = false,
    val processingStatus: PipelineStatus? = null,
    val pendingAutoShare: ShareAutoShare? = null,
    val showResultScreen: Boolean = true,
    val autoShareNotice: Int? = null,
)

data class ShareAutoCopy(val generation: Long, val revision: Long, val url: String)

data class ShareAutoShare(val generation: Long, val revision: Long, val url: String)

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
