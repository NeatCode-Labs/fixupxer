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

package com.fixupxer.presentation

import com.fixupxer.domain.model.ProcessedUrlResult
import com.fixupxer.domain.repository.UrlRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal class TestUrlRepository : UrlRepository {
    val instagramFlow = MutableStateFlow(true)
    val twitterFlow = MutableStateFlow(true)
    val tikTokFlow = MutableStateFlow(true)
    val blueskyFlow = MutableStateFlow(true)
    val facebookFlow = MutableStateFlow(true)
    val redditFlow = MutableStateFlow(false)
    val youtubeFlow = MutableStateFlow(false)
    val pinterestFlow = MutableStateFlow(false)
    val threadsFlow = MutableStateFlow(false)
    var processResult: ProcessedUrlResult = ProcessedUrlResult("", false)
    var processHandler: ((String, Boolean, String?) -> ProcessedUrlResult)? = null

    override suspend fun processUrl(url: String): ProcessedUrlResult =
        processUrl(url, false)

    override suspend fun processUrl(url: String, forceCleanTracking: Boolean): ProcessedUrlResult =
        processUrl(url, forceCleanTracking, null)

    override suspend fun processUrl(
        url: String,
        forceCleanTracking: Boolean,
        previousProcessedUrl: String?
    ): ProcessedUrlResult =
        processHandler?.invoke(url, forceCleanTracking, previousProcessedUrl) ?: processResult

    override suspend fun processUrlWithoutHistory(url: String): ProcessedUrlResult =
        processUrl(url, false)

    override suspend fun processSharedUrl(
        url: String,
        previousProcessedUrl: String?
    ): ProcessedUrlResult = processUrl(url, false, previousProcessedUrl)

    override suspend fun processUrlForSharing(url: String): String = url

    override suspend fun cleanUrl(url: String): String = url

    var detectInstagram: Boolean = false
    var detectTwitter: Boolean = false
    var detectFacebook: Boolean = false
    var detectTikTok: Boolean = false
    var detectBluesky: Boolean = false
    var detectReddit: Boolean = false
    var detectYouTube: Boolean = false
    var detectPinterest: Boolean = false
    var detectThreads: Boolean = false

    override fun isInstagramUrl(url: String): Boolean = detectInstagram

    override fun isFacebookUrl(url: String): Boolean = detectFacebook

    override fun isTwitterUrl(url: String): Boolean = detectTwitter

    override fun isTikTokUrl(url: String): Boolean = detectTikTok

    override fun isBlueskyUrl(url: String): Boolean = detectBluesky

    override fun isRedditUrl(url: String): Boolean = detectReddit

    override fun isYouTubeUrl(url: String): Boolean = detectYouTube

    override fun isPinterestUrl(url: String): Boolean = detectPinterest

    override fun isThreadsUrl(url: String): Boolean = detectThreads

    override fun hasTrackingParameters(url: String): Boolean = false

    override fun isInstagramConversionEnabled() = instagramFlow

    override suspend fun setInstagramConversionEnabled(enabled: Boolean) {
        instagramFlow.value = enabled
    }

    override fun isTwitterConversionEnabled() = twitterFlow

    override suspend fun setTwitterConversionEnabled(enabled: Boolean) {
        twitterFlow.value = enabled
    }

    override fun isTikTokConversionEnabled() = tikTokFlow

    override suspend fun setTikTokConversionEnabled(enabled: Boolean) {
        tikTokFlow.value = enabled
    }

    override fun isBlueskyConversionEnabled() = blueskyFlow

    override suspend fun setBlueskyConversionEnabled(enabled: Boolean) {
        blueskyFlow.value = enabled
    }

    override fun isFacebookConversionEnabled() = facebookFlow

    override suspend fun setFacebookConversionEnabled(enabled: Boolean) {
        facebookFlow.value = enabled
    }

    override fun isRedditConversionEnabled() = redditFlow

    override suspend fun setRedditConversionEnabled(enabled: Boolean) {
        redditFlow.value = enabled
    }

    override fun isYoutubeConversionEnabled() = youtubeFlow

    override suspend fun setYoutubeConversionEnabled(enabled: Boolean) {
        youtubeFlow.value = enabled
    }

    override fun isPinterestConversionEnabled() = pinterestFlow

    override suspend fun setPinterestConversionEnabled(enabled: Boolean) {
        pinterestFlow.value = enabled
    }

    override fun isThreadsConversionEnabled() = threadsFlow

    override suspend fun setThreadsConversionEnabled(enabled: Boolean) {
        threadsFlow.value = enabled
    }

    override suspend fun processUrlForBrowser(url: String): ProcessedUrlResult =
        processUrl(url, false)
}
