// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
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

package com.fixupxer.cleaners

import com.fixupxer.utils.Constants

data class PlatformParameterRule(
    val id: String,
    val displayName: String,
    val category: CleanerCategory,
    val domains: List<String>,
    val removeKeys: Set<String>,
    val removePrefixes: List<String> = emptyList(),
    val preserveKeys: Set<String> = emptySet()
)

object ParameterRuleCatalog {
    val rules: List<PlatformParameterRule> = listOf(
        PlatformParameterRule(
            id = "wikipedia",
            displayName = "Wikipedia",
            category = CleanerCategory.NEWS_MEDIA,
            domains = listOf(Constants.WIKIPEDIA_DOMAIN),
            removeKeys = setOf("wprov")
        ),
        PlatformParameterRule(
            id = "threads",
            displayName = "Threads",
            category = CleanerCategory.SOCIAL_MEDIA,
            domains = listOf(Constants.THREADS_NET_DOMAIN, Constants.THREADS_COM_DOMAIN),
            removeKeys = setOf("igshid", "xmt")
        ),
        PlatformParameterRule(
            id = "twitch",
            displayName = "Twitch",
            category = CleanerCategory.VIDEO_PLATFORMS,
            domains = listOf(Constants.TWITCH_DOMAIN),
            removeKeys = setOf("tt_medium", "tt_content")
        ),
        PlatformParameterRule(
            id = "spotify",
            displayName = "Spotify",
            category = CleanerCategory.OTHER,
            domains = listOf(Constants.SPOTIFY_DOMAIN),
            removeKeys = setOf("si", "dl_branch", "dl_mobileapp"),
            preserveKeys = setOf("context", "uri")
        ),
        PlatformParameterRule(
            id = "pinterest",
            displayName = "Pinterest",
            category = CleanerCategory.SOCIAL_MEDIA,
            domains = listOf(Constants.PINTEREST_DOMAIN),
            removeKeys = setOf("e_t", "e_t_s", "e_t_cs", "ouuid", "pin_unauth")
        ),
        PlatformParameterRule(
            id = "snapchat",
            displayName = "Snapchat",
            category = CleanerCategory.SOCIAL_MEDIA,
            domains = listOf(Constants.SNAPCHAT_DOMAIN),
            removeKeys = setOf("share_id")
        ),
        PlatformParameterRule(
            id = "whatsapp",
            displayName = "WhatsApp",
            category = CleanerCategory.SOCIAL_MEDIA,
            domains = listOf(Constants.WHATSAPP_DOMAIN),
            removeKeys = setOf("link_source", "link_medium")
        ),
        PlatformParameterRule(
            id = "medium",
            displayName = "Medium",
            category = CleanerCategory.NEWS_MEDIA,
            domains = listOf(Constants.MEDIUM_DOMAIN),
            removeKeys = setOf("source", "sk")
        ),
        PlatformParameterRule(
            id = "bing",
            displayName = "Bing",
            category = CleanerCategory.SEARCH_ENGINES,
            domains = listOf(Constants.BING_DOMAIN),
            removeKeys = setOf("cvid")
        ),
        PlatformParameterRule(
            id = "duckduckgo",
            displayName = "DuckDuckGo",
            category = CleanerCategory.SEARCH_ENGINES,
            domains = listOf(Constants.DUCKDUCKGO_DOMAIN),
            removeKeys = setOf("t", "atb")
        ),
        PlatformParameterRule(
            id = "google_store",
            displayName = "Google Store",
            category = CleanerCategory.E_COMMERCE,
            domains = listOf(Constants.GOOGLE_STORE_DOMAIN),
            removeKeys = setOf("hl", "selections")
        ),
        PlatformParameterRule(
            id = "ebay",
            displayName = "eBay",
            category = CleanerCategory.E_COMMERCE,
            domains = listOf(
                Constants.EBAY_COM_DOMAIN,
                Constants.EBAY_CO_UK_DOMAIN,
                Constants.EBAY_DE_DOMAIN
            ),
            removeKeys = setOf(
                "mkevt", "mkcid", "mkrid", "campid", "customid", "toolid", "ssspo",
                "sssrc", "ssuid", "widget_ver", "media", "_trkparms", "_trksid"
            ),
            preserveKeys = setOf("var", "selected", "hash", "epid")
        ),
        PlatformParameterRule(
            id = "netflix",
            displayName = "Netflix",
            category = CleanerCategory.VIDEO_PLATFORMS,
            domains = listOf(Constants.NETFLIX_DOMAIN),
            removeKeys = setOf("trkid", "tctx")
        ),
        PlatformParameterRule(
            id = "aliexpress",
            displayName = "AliExpress",
            category = CleanerCategory.E_COMMERCE,
            domains = listOf(Constants.ALIEXPRESS_DOMAIN),
            removeKeys = setOf(
                "spm", "srcSns", "businessType", "templateKey", "aff_fcid", "aff_fsk",
                "aff_platform", "aff_trace_key", "terminal_id", "afSmartRedirect",
                "utparam-url"
            ),
            preserveKeys = setOf("sku_id", "currency", "language", "gatewayAdapt")
        ),
        PlatformParameterRule(
            id = "bilibili",
            displayName = "Bilibili",
            category = CleanerCategory.VIDEO_PLATFORMS,
            domains = listOf(Constants.BILIBILI_DOMAIN),
            removeKeys = setOf("vd_source", "seid", "share_source", "copy_link")
        )
    )
}
