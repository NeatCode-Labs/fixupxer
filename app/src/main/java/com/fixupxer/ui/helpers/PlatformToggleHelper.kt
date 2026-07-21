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

package com.fixupxer.ui.helpers

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.domain.repository.UrlRepository
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Binds the single contextual platform toggle row on Main and Share screens.
 */
object PlatformToggleHelper {

    /**
     * Detect the primary platform for a URL using a fixed priority order.
     */
    fun detectPlatform(url: String, urlRepository: UrlRepository): ProxyPlatform? {
        if (url.isBlank()) return null
        return when {
            urlRepository.isInstagramUrl(url) -> ProxyPlatform.INSTAGRAM
            urlRepository.isTwitterUrl(url) -> ProxyPlatform.X
            urlRepository.isFacebookUrl(url) -> ProxyPlatform.FACEBOOK
            urlRepository.isTikTokUrl(url) -> ProxyPlatform.TIKTOK
            urlRepository.isBlueskyUrl(url) -> ProxyPlatform.BLUESKY
            urlRepository.isRedditUrl(url) -> ProxyPlatform.REDDIT
            urlRepository.isYouTubeUrl(url) -> ProxyPlatform.YOUTUBE
            urlRepository.isPinterestUrl(url) -> ProxyPlatform.PINTEREST
            urlRepository.isThreadsUrl(url) -> ProxyPlatform.THREADS
            else -> null
        }
    }

    fun bindPlatformToggle(
        context: android.content.Context,
        container: View,
        monogram: TextView,
        title: TextView,
        proxyRow: View,
        proxyStatus: TextView,
        changeProxy: TextView,
        platformSwitch: MaterialSwitch,
        platform: ProxyPlatform?,
        preferencesManager: PreferencesManager,
        conversionEnabled: Boolean,
        proxySelectionRevision: Int,
        onToggle: (Boolean) -> Unit,
        onChangeProxy: () -> Unit,
    ) {
        @Suppress("UNUSED_VARIABLE")
        val revisionTrigger = proxySelectionRevision

        container.isVisible = platform != null
        if (platform == null) return

        monogram.text = context.getString(FrontendDisplayHelper.monogramRes(platform))

        val active = ProxyRoster.activeTargets(platform)
        val selectedDomain = preferencesManager.getSelectedProxyDomain(platform)
        val selectedTarget = selectedDomain?.let { ProxyRoster.targetByDomain(platform, it) }
            ?: active.firstOrNull()

        if (active.isEmpty()) {
            title.setText(R.string.no_frontend_active_title)
        } else {
            title.setText(FrontendDisplayHelper.titleForTarget(selectedTarget))
        }

        proxyRow.isVisible = true
        proxyStatus.text = if (active.isEmpty()) {
            context.getString(R.string.no_proxy_available)
        } else {
            val label = selectedTarget?.let { FrontendDisplayHelper.displayLabel(it) }
                ?: selectedDomain.orEmpty()
            context.getString(R.string.currently_using_proxy, label)
        }

        changeProxy.contentDescription = context.getString(
            R.string.change_proxy_link_desc,
            context.getString(FrontendDisplayHelper.platformNameRes(platform))
        )

        platformSwitch.contentDescription = context.getString(
            FrontendDisplayHelper.toggleDescRes(platform)
        )
        platformSwitch.setOnCheckedChangeListener(null)
        if (active.isEmpty()) {
            platformSwitch.isEnabled = false
            platformSwitch.isChecked = false
        } else {
            platformSwitch.isEnabled = true
            platformSwitch.isChecked = conversionEnabled
        }
        platformSwitch.setOnCheckedChangeListener { _, isChecked ->
            onToggle(isChecked)
        }

        changeProxy.setOnClickListener { onChangeProxy() }
    }

    fun isConversionEnabled(
        platform: ProxyPlatform?,
        isInstagramConversionEnabled: Boolean,
        isTwitterConversionEnabled: Boolean,
        isFacebookConversionEnabled: Boolean,
        isTikTokConversionEnabled: Boolean,
        isBlueskyConversionEnabled: Boolean,
        isRedditConversionEnabled: Boolean,
        isYoutubeConversionEnabled: Boolean,
        isPinterestConversionEnabled: Boolean,
        isThreadsConversionEnabled: Boolean,
    ): Boolean {
        return when (platform) {
            ProxyPlatform.INSTAGRAM -> isInstagramConversionEnabled
            ProxyPlatform.X -> isTwitterConversionEnabled
            ProxyPlatform.FACEBOOK -> isFacebookConversionEnabled
            ProxyPlatform.TIKTOK -> isTikTokConversionEnabled
            ProxyPlatform.BLUESKY -> isBlueskyConversionEnabled
            ProxyPlatform.REDDIT -> isRedditConversionEnabled
            ProxyPlatform.YOUTUBE -> isYoutubeConversionEnabled
            ProxyPlatform.PINTEREST -> isPinterestConversionEnabled
            ProxyPlatform.THREADS -> isThreadsConversionEnabled
            null -> false
        }
    }
}
