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
import com.fixupxer.processing.LinkLeakAnalyzer
import com.fixupxer.processing.ProcessingOptions
import com.fixupxer.processing.ProcessingProfile
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.processing.UrlProcessingOrchestrator
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.TikTokProxyStore
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
        private const val PLATFORM_OTHER = "Other"
    }

    private fun detectPlatform(url: String): String = when {
        urlProcessor.isInstagramUrl(url) -> PLATFORM_INSTAGRAM
        urlProcessor.isTwitterUrl(url) -> PLATFORM_TWITTER
        urlProcessor.isFacebookUrl(url) -> PLATFORM_FACEBOOK
        urlProcessor.isTikTokUrl(url) -> PLATFORM_TIKTOK
        urlProcessor.isBlueskyUrl(url) -> PLATFORM_BLUESKY
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
        val originalHost = UrlNormalizer.extractAsciiHost(url)
        val processedHost = UrlNormalizer.extractAsciiHost(processedUrl)
        val knownProxies = InstagramProxyStore.allKnownProxies()
        val urlHasProxy = knownProxies.any { UrlNormalizer.hostMatchesDomain(originalHost, it) }
        val resultHasProxy = knownProxies.any { UrlNormalizer.hostMatchesDomain(processedHost, it) }
        val isInstagramConversion =
            (UrlNormalizer.hostMatchesDomain(originalHost, Constants.INSTAGRAM_DOMAIN) &&
                !urlHasProxy && resultHasProxy) ||
                (urlHasProxy &&
                    UrlNormalizer.hostMatchesDomain(processedHost, Constants.INSTAGRAM_DOMAIN) &&
                    !resultHasProxy) ||
                (urlHasProxy && resultHasProxy && knownProxies.none { p ->
                    UrlNormalizer.hostMatchesDomain(originalHost, p) &&
                        UrlNormalizer.hostMatchesDomain(processedHost, p)
                })

        val isFacebookConversion =
            ((UrlNormalizer.hostMatchesDomain(originalHost, Constants.FACEBOOK_DOMAIN) ||
                UrlNormalizer.hostMatchesDomain(originalHost, Constants.FB_SHORT_DOMAIN)) &&
                UrlNormalizer.hostMatchesDomain(processedHost, Constants.FACEBOOKEZ_DOMAIN)) ||
                (UrlNormalizer.hostMatchesDomain(originalHost, Constants.FACEBOOKEZ_DOMAIN) &&
                    UrlNormalizer.hostMatchesDomain(processedHost, Constants.FACEBOOK_DOMAIN) &&
                    !UrlNormalizer.hostMatchesDomain(processedHost, Constants.FACEBOOKEZ_DOMAIN))

        val toFixupx = UrlNormalizer.hostMatchesDomain(processedHost, Constants.FIXUPX_DOMAIN)
        val isTwitterConversion =
            ((UrlNormalizer.hostMatchesDomain(originalHost, Constants.TWITTER_DOMAIN) ||
                UrlNormalizer.hostMatchesDomain(originalHost, Constants.X_DOMAIN)) &&
                !UrlNormalizer.hostMatchesDomain(originalHost, Constants.FIXUPX_DOMAIN) && toFixupx) ||
                (UrlNormalizer.hostMatchesDomain(originalHost, Constants.FIXUPX_DOMAIN) && !toFixupx &&
                    (UrlNormalizer.hostMatchesDomain(processedHost, Constants.X_DOMAIN) ||
                        UrlNormalizer.hostMatchesDomain(processedHost, Constants.TWITTER_DOMAIN))) ||
                (UrlNormalizer.hostMatchesDomain(originalHost, Constants.FXTWITTER_DOMAIN) &&
                    (toFixupx || UrlNormalizer.hostMatchesDomain(processedHost, Constants.X_DOMAIN))) ||
                (UrlNormalizer.hostMatchesDomain(originalHost, Constants.VXTWITTER_DOMAIN) &&
                    (toFixupx || UrlNormalizer.hostMatchesDomain(processedHost, Constants.X_DOMAIN)))

        val tiktokProxies = TikTokProxyStore.allKnownProxies()
        val urlHasTikTokProxy = tiktokProxies.any {
            UrlNormalizer.hostMatchesDomain(originalHost, it)
        }
        val resultHasTikTokProxy = tiktokProxies.any {
            UrlNormalizer.hostMatchesDomain(processedHost, it)
        }
        val isTikTokConversion =
            (UrlNormalizer.hostMatchesDomain(originalHost, Constants.TIKTOK_DOMAIN) &&
                !urlHasTikTokProxy && resultHasTikTokProxy) ||
                (urlHasTikTokProxy &&
                    UrlNormalizer.hostMatchesDomain(processedHost, Constants.TIKTOK_DOMAIN) &&
                    !resultHasTikTokProxy) ||
                (urlHasTikTokProxy && resultHasTikTokProxy && tiktokProxies.none { p ->
                    UrlNormalizer.hostMatchesDomain(originalHost, p) &&
                        UrlNormalizer.hostMatchesDomain(processedHost, p)
                })
        val isBlueskyConversion =
            (UrlNormalizer.hostMatchesDomain(originalHost, Constants.BLUESKY_DOMAIN) &&
                UrlNormalizer.hostMatchesDomain(processedHost, Constants.FXBSKY_DOMAIN)) ||
                (UrlNormalizer.hostMatchesDomain(originalHost, Constants.FXBSKY_DOMAIN) &&
                    UrlNormalizer.hostMatchesDomain(processedHost, Constants.BLUESKY_DOMAIN))

        return when {
            customRuleApplied -> CONVERSION_CUSTOM_RULE_APPLIED
            isInstagramConversion || isFacebookConversion || isTwitterConversion ||
                isTikTokConversion || isBlueskyConversion -> CONVERSION_DOMAIN_CONVERTED
            trackingRemoved -> CONVERSION_TRACKING_REMOVED
            else -> CONVERSION_URL_CLEANED
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
            url,
            preferencesManager.getInstagramProxy(),
            preferencesManager.getTikTokProxy(),
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
        val cleanTracking = isInstagram ||
            forceCleanTracking ||
            preferencesManager.isCleanTrackingEnabled()
        val convertDomains = when (profile) {
            ProcessingProfile.MAIN, ProcessingProfile.SHARE -> when {
                isInstagram || isFacebook -> preferencesManager.isConvertInstagramEnabled()
                isTikTok -> preferencesManager.isConvertTikTokEnabled()
                isBluesky -> preferencesManager.isConvertBlueskyEnabled()
                else -> preferencesManager.isConvertTwitterEnabled()
            }
            ProcessingProfile.BROWSER -> when {
                isInstagram -> preferencesManager.isBrowserConvertInstagramEnabled()
                isFacebook -> preferencesManager.isBrowserConvertFacebookEnabled()
                isTwitter -> preferencesManager.isBrowserConvertTwitterEnabled()
                isTikTok -> preferencesManager.isBrowserConvertTikTokEnabled()
                isBluesky -> preferencesManager.isBrowserConvertBlueskyEnabled()
                else -> false
            }
        }
        val result = orchestrator.process(
            rawInput = url,
            options = ProcessingOptions(
                profile = profile,
                cleanTracking = cleanTracking,
                convertDomains = convertDomains,
                instagramProxy = preferencesManager.getInstagramProxy(),
                tiktokProxy = preferencesManager.getTikTokProxy(),
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
            leakFindings = outputFindings
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
    
    override fun isTrackingRemovalEnabled(): Flow<Boolean> =
        preferencesManager.booleanFlow(PreferencesManager.KEY_CLEAN_TRACKING, default = true)
    
    override suspend fun setTrackingRemovalEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (preferencesManager.isCleanTrackingEnabled() != enabled) {
                preferencesManager.setCleanTrackingEnabled(enabled)
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