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
    val trackingFlow = MutableStateFlow(true)

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

    override suspend fun processUrlForSharing(url: String): String = url

    override suspend fun cleanUrl(url: String): String = url

    override fun isInstagramUrl(url: String): Boolean = false

    override fun isFacebookUrl(url: String): Boolean = false

    override fun isTwitterUrl(url: String): Boolean = false

    override fun isTikTokUrl(url: String): Boolean = false

    override fun hasTrackingParameters(url: String): Boolean = false

    override fun isInstagramConversionEnabled() = instagramFlow

    override suspend fun setInstagramConversionEnabled(enabled: Boolean) {
        instagramFlow.value = enabled
    }

    override fun isTrackingRemovalEnabled() = trackingFlow

    override suspend fun setTrackingRemovalEnabled(enabled: Boolean) {
        trackingFlow.value = enabled
    }

    override fun isTwitterConversionEnabled() = twitterFlow

    override suspend fun setTwitterConversionEnabled(enabled: Boolean) {
        twitterFlow.value = enabled
    }

    override fun isTikTokConversionEnabled() = tikTokFlow

    override suspend fun setTikTokConversionEnabled(enabled: Boolean) {
        tikTokFlow.value = enabled
    }

    override suspend fun processUrlForBrowser(url: String): ProcessedUrlResult =
        processUrl(url, false)
}
