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


package com.fixupxer.data.repository

import com.fixupxer.PreferencesManager
import com.fixupxer.UrlProcessor
import com.fixupxer.domain.model.ProcessedUrlResult
import com.fixupxer.domain.repository.UrlRepository
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.processing.BrowserConversionPolicy
import com.fixupxer.processing.LinkLeakAnalyzer
import com.fixupxer.processing.PlatformDomainConverter
import com.fixupxer.processing.ProcessingOptions
import com.fixupxer.processing.ProcessingProfile
import com.fixupxer.processing.ProxySelections
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.processing.UrlProcessingOrchestrator
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementation of UrlRepository
 */
class UrlRepositoryImpl @Inject constructor(
    private val urlProcessor: UrlProcessor,
    private val preferencesManager: PreferencesManager,
    private val historyRepository: HistoryRepository,
    private val orchestrator: UrlProcessingOrchestrator
) : UrlRepository {

    companion object {
        // History data values persisted in Room — intentionally NOT localized;
        // changing them would break classification of existing history entries.
        private const val CONVERSION_DOMAIN_CONVERTED = "Domain converted"
        private const val CONVERSION_TRACKING_REMOVED = "Tracking removed"
        private const val CONVERSION_URL_CLEANED = "URL cleaned"
        private const val CONVERSION_CUSTOM_RULE_APPLIED = "Custom rule applied"
        private const val PLATFORM_INSTAGRAM = "Instagram"
        private const val PLATFORM_TWITTER = "Twitter/X"
        private const val PLATFORM_FACEBOOK = "Facebook"
        private const val PLATFORM_TIKTOK = "TikTok"
        private const val PLATFORM_BLUESKY = "Bluesky"
        private const val PLATFORM_REDDIT = "Reddit"
        private const val PLATFORM_YOUTUBE = "YouTube"
        private const val PLATFORM_PINTEREST = "Pinterest"
        private const val PLATFORM_THREADS = "Threads"
        private const val PLATFORM_OTHER = "Other"
    }

    private fun buildProxySelections(profile: ProcessingProfile): ProxySelections =
        when (profile) {
            ProcessingProfile.BROWSER ->
                ProxySelections(preferencesManager.resolveBrowserPrivacySelections())
            ProcessingProfile.MAIN, ProcessingProfile.SHARE ->
                ProxySelections(
                    ProxyPlatform.entries.associateWith { preferencesManager.getSelectedProxyDomain(it) },
                )
        }

    private fun detectPlatform(url: String): String = when {
        urlProcessor.isInstagramUrl(url) -> PLATFORM_INSTAGRAM
        urlProcessor.isTwitterUrl(url) -> PLATFORM_TWITTER
        urlProcessor.isFacebookUrl(url) -> PLATFORM_FACEBOOK
        urlProcessor.isTikTokUrl(url) -> PLATFORM_TIKTOK
        urlProcessor.isBlueskyUrl(url) -> PLATFORM_BLUESKY
        urlProcessor.isRedditUrl(url) -> PLATFORM_REDDIT
        urlProcessor.isYouTubeUrl(url) -> PLATFORM_YOUTUBE
        urlProcessor.isPinterestUrl(url) -> PLATFORM_PINTEREST
        urlProcessor.isThreadsUrl(url) -> PLATFORM_THREADS
        else -> PLATFORM_OTHER
    }

    /**
     * Classify how [url] → [processedUrl] should appear in history.
     * Shared by [processUrl] and [processUrlForBrowser].
     */
    private fun classifyConversion(
        url: String,
        processedUrl: String,
        trackingRemoved: Boolean,
        customRuleApplied: Boolean = false
    ): String {
        val domainConverted = ProxyPlatform.entries.any { platform ->
            isPlatformDomainConversion(url, processedUrl, platform)
        }
        return when {
            customRuleApplied -> CONVERSION_CUSTOM_RULE_APPLIED
            domainConverted -> CONVERSION_DOMAIN_CONVERTED
            trackingRemoved -> CONVERSION_TRACKING_REMOVED
            else -> CONVERSION_URL_CLEANED
        }
    }

    private fun isPlatformDomainConversion(
        originalUrl: String,
        processedUrl: String,
        platform: ProxyPlatform,
    ): Boolean {
        val originalHost = UrlNormalizer.extractAsciiHost(originalUrl)
        val processedHost = UrlNormalizer.extractAsciiHost(processedUrl)
        val originalIsSource = isPlatformSource(originalHost, platform)
        val processedIsSource = isPlatformSource(processedHost, platform)
        val originalIsProxy = isPlatformProxy(originalUrl, originalHost, platform)
        val processedIsProxy = isPlatformProxy(processedUrl, processedHost, platform)
        val fromSourceToProxy = originalIsSource && processedIsProxy && !originalIsProxy
        val fromProxyToSource = originalIsProxy && processedIsSource && !processedIsProxy
        val proxySwap = originalIsProxy && processedIsProxy && originalHost != processedHost
        return fromSourceToProxy || fromProxyToSource || proxySwap
    }

    private fun isPlatformSource(host: String?, platform: ProxyPlatform): Boolean {
        if (host == null) return false
        if (platform == ProxyPlatform.REDDIT) {
            return host == Constants.REDDIT_DOMAIN ||
                host == "www.${Constants.REDDIT_DOMAIN}" ||
                host == "old.${Constants.REDDIT_DOMAIN}" ||
                host == "new.${Constants.REDDIT_DOMAIN}"
        }
        return AlternativeFrontendCatalog.sourceDomains(platform).any {
            UrlNormalizer.hostMatchesDomain(host, it)
        }
    }

    private fun isPlatformProxy(
        url: String,
        host: String?,
        platform: ProxyPlatform,
    ): Boolean {
        if (host == null) return false
        return ProxyRoster.allKnownDomains(platform).any { domain ->
            if (platform == ProxyPlatform.X && domain == Constants.FARSIDE_DOMAIN) {
                PlatformDomainConverter.isFarsideNitterUrl(url)
            } else {
                UrlNormalizer.hostMatchesDomain(host, domain)
            }
        }
    }

    private suspend fun saveHistoryEntry(
        url: String,
        processedUrl: String,
        platform: String,
        wasAlreadyClean: Boolean,
        customRuleApplied: Boolean = false
    ) {
        val conversionType = classifyConversion(
            url,
            processedUrl,
            trackingRemoved = !wasAlreadyClean,
            customRuleApplied = customRuleApplied
        )
        try {
            historyRepository.insertHistory(
                originalUrl = url,
                cleanedUrl = processedUrl,
                platform = platform,
                conversionType = conversionType
            )
            historyRepository.trimHistory(preferencesManager.getMaxHistoryEntries())
        } catch (e: Exception) {
            Timber.e(e, "Failed to save history entry")
        }
    }
    
    override suspend fun processUrl(url: String): ProcessedUrlResult = processUrl(url, false)
    
    override suspend fun processUrl(url: String, forceCleanTracking: Boolean): ProcessedUrlResult = 
        processUrl(url, forceCleanTracking, null)
    
    override suspend fun processUrl(
        url: String,
        forceCleanTracking: Boolean,
        previousProcessedUrl: String?
    ): ProcessedUrlResult = withContext(Dispatchers.IO) {
        processWithProfile(
            url = url,
            profile = ProcessingProfile.MAIN,
            forceCleanTracking = forceCleanTracking,
            previousProcessedUrl = previousProcessedUrl,
            persistHistory = true
        )
    }

    override suspend fun processSharedUrl(
        url: String,
        previousProcessedUrl: String?
    ): ProcessedUrlResult = withContext(Dispatchers.IO) {
        processWithProfile(
            url = url,
            profile = ProcessingProfile.SHARE,
            forceCleanTracking = false,
            previousProcessedUrl = previousProcessedUrl,
            persistHistory = true
        )
    }

    override suspend fun processUrlWithoutHistory(url: String): ProcessedUrlResult =
        withContext(Dispatchers.IO) {
            processWithProfile(
                url = url,
                profile = ProcessingProfile.MAIN,
                forceCleanTracking = false,
                previousProcessedUrl = null,
                persistHistory = false
            )
        }
    
    override suspend fun processUrlForSharing(url: String): String = withContext(Dispatchers.IO) {
        // This legacy string-only path cannot return output findings, so it never caches.
        urlProcessor.processUrlForSharing(
            url = url,
            selections = buildProxySelections(ProcessingProfile.SHARE),
            useCache = false
        )
    }

    private suspend fun processWithProfile(
        url: String,
        profile: ProcessingProfile,
        forceCleanTracking: Boolean,
        previousProcessedUrl: String?,
        persistHistory: Boolean
    ): ProcessedUrlResult {
        if (url.isEmpty()) return ProcessedUrlResult(url, true)

        val inputFindings = LinkLeakAnalyzer.analyze(url)
        val isInstagram = urlProcessor.isInstagramUrl(url)
        val isFacebook = urlProcessor.isFacebookUrl(url)
        val isTwitter = urlProcessor.isTwitterUrl(url)
        val isTikTok = urlProcessor.isTikTokUrl(url)
        val isBluesky = urlProcessor.isBlueskyUrl(url)
        val isReddit = urlProcessor.isRedditUrl(url)
        val isYouTube = urlProcessor.isYouTubeUrl(url)
        val isPinterest = urlProcessor.isPinterestUrl(url)
        val isThreads = urlProcessor.isThreadsUrl(url)
        val cleanTracking = isInstagram ||
            forceCleanTracking ||
            preferencesManager.isCleanTrackingEnabled()
        val browserPlatform = when {
            isInstagram -> ProxyPlatform.INSTAGRAM
            isFacebook -> ProxyPlatform.FACEBOOK
            isTwitter -> ProxyPlatform.X
            isTikTok -> ProxyPlatform.TIKTOK
            isBluesky -> ProxyPlatform.BLUESKY
            isReddit -> ProxyPlatform.REDDIT
            isYouTube -> ProxyPlatform.YOUTUBE
            isPinterest -> ProxyPlatform.PINTEREST
            isThreads -> ProxyPlatform.THREADS
            else -> null
        }
        val convertDomains = when (profile) {
            ProcessingProfile.MAIN, ProcessingProfile.SHARE -> when {
                isInstagram -> preferencesManager.isConvertInstagramEnabled()
                isFacebook -> preferencesManager.isConvertFacebookEnabled()
                isTikTok -> preferencesManager.isConvertTikTokEnabled()
                isBluesky -> preferencesManager.isConvertBlueskyEnabled()
                isReddit -> preferencesManager.isConvertRedditEnabled()
                isYouTube -> preferencesManager.isConvertYoutubeEnabled()
                isPinterest -> preferencesManager.isConvertPinterestEnabled()
                isThreads -> preferencesManager.isConvertThreadsEnabled()
                else -> preferencesManager.isConvertTwitterEnabled()
            }
            ProcessingProfile.BROWSER -> BrowserConversionPolicy.shouldConvert(
                platform = browserPlatform,
                toggleEnabled = browserPlatform?.let {
                    preferencesManager.isBrowserPrivacyConversionEnabled(it)
                } == true,
                hasActiveTarget = browserPlatform?.let {
                    preferencesManager.resolveBrowserPrivacyTarget(it)
                } != null,
            )
        }
        val result = orchestrator.process(
            rawInput = url,
            options = ProcessingOptions(
                profile = profile,
                cleanTracking = cleanTracking,
                convertDomains = convertDomains,
                proxySelections = buildProxySelections(profile),
                customRulesEnabled = preferencesManager.areCustomRulesEnabled(),
                persistHistory = persistHistory,
                useCache = inputFindings.isEmpty()
            )
        )
        val outputFindings = LinkLeakAnalyzer.analyze(result.url)
        val containsSensitiveData = inputFindings.isNotEmpty() || outputFindings.isNotEmpty()
        if (outputFindings.isNotEmpty()) {
            // Purge every cleaner-cache entry this run created: custom PRE_CLEAN
            // rules or redirect re-entries can key entries by URLs that differ
            // from the original input.
            result.cleanerCacheKeys.forEach(orchestrator::evictFromCleanerCache)
        }

        val shouldSaveHistory = persistHistory &&
            preferencesManager.isHistoryEnabled() &&
            !containsSensitiveData &&
            if (previousProcessedUrl != null) {
                result.url != previousProcessedUrl
            } else {
                result.url != result.originalUrl
            }
        if (shouldSaveHistory) {
            saveHistoryEntry(
                result.originalUrl,
                result.url,
                detectPlatform(result.originalUrl),
                result.wasAlreadyClean,
                result.customRuleChanged
            )
        }
        return ProcessedUrlResult(
            url = result.url,
            wasAlreadyClean = result.wasAlreadyClean,
            customRuleApplied = result.customRuleChanged,
            rulesRevision = result.rulesRevision,
            operations = result.operations,
            leakFindings = outputFindings,
            routingHost = if (profile == ProcessingProfile.BROWSER) result.routingHost else null,
        )
    }
    
    override suspend fun cleanUrl(url: String): String = withContext(Dispatchers.IO) {
        // Clean URL with history tracking
        val result = processUrl(url, true)
        result.url
    }
    
    override fun isInstagramUrl(url: String): Boolean = urlProcessor.isInstagramUrl(url)

    override fun isFacebookUrl(url: String): Boolean = urlProcessor.isFacebookUrl(url)
    
    override fun isTwitterUrl(url: String): Boolean = urlProcessor.isTwitterUrl(url)
    
    override fun isTikTokUrl(url: String): Boolean = urlProcessor.isTikTokUrl(url)

    override fun isBlueskyUrl(url: String): Boolean = urlProcessor.isBlueskyUrl(url)

    override fun isRedditUrl(url: String): Boolean = urlProcessor.isRedditUrl(url)

    override fun isYouTubeUrl(url: String): Boolean = urlProcessor.isYouTubeUrl(url)

    override fun isPinterestUrl(url: String): Boolean = urlProcessor.isPinterestUrl(url)

    override fun isThreadsUrl(url: String): Boolean = urlProcessor.isThreadsUrl(url)

    override fun hasTrackingParameters(url: String): Boolean = urlProcessor.hasTrackingParameters(url)
    
    override fun isInstagramConversionEnabled(): Flow<Boolean> =
        preferencesManager.booleanFlow(PreferencesManager.KEY_CONVERT_INSTAGRAM, default = true)
    
    override suspend fun setInstagramConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (preferencesManager.isConvertInstagramEnabled() != enabled) {
                preferencesManager.setConvertInstagramEnabled(enabled)
            }
        }
    }
    
    override fun isTwitterConversionEnabled(): Flow<Boolean> =
        preferencesManager.booleanFlow(PreferencesManager.KEY_CONVERT_TWITTER, default = true)
    
    override suspend fun setTwitterConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (preferencesManager.isConvertTwitterEnabled() != enabled) {
                preferencesManager.setConvertTwitterEnabled(enabled)
            }
        }
    }
    
    override fun isTikTokConversionEnabled(): Flow<Boolean> =
        preferencesManager.booleanFlow(PreferencesManager.KEY_CONVERT_TIKTOK, default = true)
    
    override suspend fun setTikTokConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (preferencesManager.isConvertTikTokEnabled() != enabled) {
                preferencesManager.setConvertTikTokEnabled(enabled)
            }
        }
    }

    override fun isBlueskyConversionEnabled(): Flow<Boolean> =
        preferencesManager.booleanFlow(PreferencesManager.KEY_CONVERT_BLUESKY, default = true)

    override suspend fun setBlueskyConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (preferencesManager.isConvertBlueskyEnabled() != enabled) {
                preferencesManager.setConvertBlueskyEnabled(enabled)
            }
        }
    }

    override fun isFacebookConversionEnabled(): Flow<Boolean> =
        preferencesManager.booleanFlow(PreferencesManager.KEY_CONVERT_FACEBOOK, default = true)

    override suspend fun setFacebookConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (preferencesManager.isConvertFacebookEnabled() != enabled) {
                preferencesManager.setConvertFacebookEnabled(enabled)
            }
        }
    }

    override fun isRedditConversionEnabled(): Flow<Boolean> =
        preferencesManager.booleanFlow(PreferencesManager.KEY_CONVERT_REDDIT, default = false)

    override suspend fun setRedditConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (preferencesManager.isConvertRedditEnabled() != enabled) {
                preferencesManager.setConvertRedditEnabled(enabled)
            }
        }
    }

    override fun isYoutubeConversionEnabled(): Flow<Boolean> =
        preferencesManager.booleanFlow(PreferencesManager.KEY_CONVERT_YOUTUBE, default = false)

    override suspend fun setYoutubeConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (preferencesManager.isConvertYoutubeEnabled() != enabled) {
                preferencesManager.setConvertYoutubeEnabled(enabled)
            }
        }
    }

    override fun isPinterestConversionEnabled(): Flow<Boolean> =
        preferencesManager.booleanFlow(PreferencesManager.KEY_CONVERT_PINTEREST, default = false)

    override suspend fun setPinterestConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (preferencesManager.isConvertPinterestEnabled() != enabled) {
                preferencesManager.setConvertPinterestEnabled(enabled)
            }
        }
    }

    override fun isThreadsConversionEnabled(): Flow<Boolean> =
        preferencesManager.booleanFlow(PreferencesManager.KEY_CONVERT_THREADS, default = false)

    override suspend fun setThreadsConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (preferencesManager.isConvertThreadsEnabled() != enabled) {
                preferencesManager.setConvertThreadsEnabled(enabled)
            }
        }
    }

    override suspend fun processUrlForBrowser(url: String): ProcessedUrlResult =
        withContext(Dispatchers.IO) {
            processWithProfile(
                url = url,
                profile = ProcessingProfile.BROWSER,
                forceCleanTracking = false,
                previousProcessedUrl = null,
                persistHistory = true
            )
        }

} 