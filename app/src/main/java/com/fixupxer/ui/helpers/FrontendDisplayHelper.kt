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

import android.content.Context
import com.fixupxer.R
import com.fixupxer.utils.FrontendRole
import com.fixupxer.utils.FrontendTarget
import com.fixupxer.utils.ProxyPlatform

/**
 * Shared display helpers for alternative frontend targets in the UI layer.
 */
object FrontendDisplayHelper {

    /** Domain label shown in picker rows and the platform toggle "Active:" line. */
    fun displayLabel(target: FrontendTarget): String {
        val prefix = target.pathPrefix?.removePrefix("/").orEmpty()
        return if (prefix.isNotEmpty()) {
            "${target.domain}/$prefix"
        } else {
            target.domain
        }
    }

    fun optionPrimaryLabel(context: Context, target: FrontendTarget): String {
        val base = displayLabel(target)
        return if (target.sfwOnly) {
            context.getString(R.string.proxy_sfw_only_suffix, base)
        } else {
            base
        }
    }

    fun titleForTarget(target: FrontendTarget?): Int {
        return if (target?.role == FrontendRole.EMBED) {
            R.string.embed_friendly_link
        } else {
            R.string.read_without_account
        }
    }

    fun monogramRes(platform: ProxyPlatform): Int = when (platform) {
        ProxyPlatform.X -> R.string.platform_monogram_twitter
        ProxyPlatform.INSTAGRAM -> R.string.platform_monogram_instagram
        ProxyPlatform.TIKTOK -> R.string.platform_monogram_tiktok
        ProxyPlatform.FACEBOOK -> R.string.platform_monogram_facebook
        ProxyPlatform.BLUESKY -> R.string.platform_monogram_bluesky
        ProxyPlatform.REDDIT -> R.string.platform_monogram_reddit
        ProxyPlatform.YOUTUBE -> R.string.platform_monogram_youtube
        ProxyPlatform.PINTEREST -> R.string.platform_monogram_pinterest
        ProxyPlatform.THREADS -> R.string.platform_monogram_threads
    }

    fun toggleDescRes(platform: ProxyPlatform): Int = when (platform) {
        ProxyPlatform.X -> R.string.convert_twitter_toggle_desc
        ProxyPlatform.INSTAGRAM -> R.string.convert_instagram_toggle_desc
        ProxyPlatform.TIKTOK -> R.string.convert_tiktok_toggle_desc
        ProxyPlatform.FACEBOOK -> R.string.convert_facebook_toggle_desc
        ProxyPlatform.BLUESKY -> R.string.convert_bluesky_toggle_desc
        ProxyPlatform.REDDIT -> R.string.convert_reddit_toggle_desc
        ProxyPlatform.YOUTUBE -> R.string.convert_youtube_toggle_desc
        ProxyPlatform.PINTEREST -> R.string.convert_pinterest_toggle_desc
        ProxyPlatform.THREADS -> R.string.convert_threads_toggle_desc
    }

    fun platformNameRes(platform: ProxyPlatform): Int = when (platform) {
        ProxyPlatform.X -> R.string.platform_name_x
        ProxyPlatform.INSTAGRAM -> R.string.platform_name_instagram
        ProxyPlatform.TIKTOK -> R.string.platform_name_tiktok
        ProxyPlatform.FACEBOOK -> R.string.platform_name_facebook
        ProxyPlatform.BLUESKY -> R.string.platform_name_bluesky
        ProxyPlatform.REDDIT -> R.string.platform_name_reddit
        ProxyPlatform.YOUTUBE -> R.string.platform_name_youtube
        ProxyPlatform.PINTEREST -> R.string.platform_name_pinterest
        ProxyPlatform.THREADS -> R.string.platform_name_threads
    }

    fun infoIntroRes(platform: ProxyPlatform): Int = when (platform) {
        ProxyPlatform.X -> R.string.proxy_picker_info_intro_x
        ProxyPlatform.INSTAGRAM -> R.string.proxy_picker_info_intro_instagram
        ProxyPlatform.TIKTOK -> R.string.proxy_picker_info_intro_tiktok
        ProxyPlatform.FACEBOOK -> R.string.proxy_picker_info_intro_facebook
        ProxyPlatform.BLUESKY -> R.string.proxy_picker_info_intro_bluesky
        ProxyPlatform.REDDIT -> R.string.proxy_picker_info_intro_reddit
        ProxyPlatform.YOUTUBE -> R.string.proxy_picker_info_intro_youtube
        ProxyPlatform.PINTEREST -> R.string.proxy_picker_info_intro_pinterest
        ProxyPlatform.THREADS -> R.string.proxy_picker_info_intro_threads
    }
}
