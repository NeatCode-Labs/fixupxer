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


package com.fixupxer.cleaners.impl

import com.fixupxer.cleaners.CleanerCategory
import com.fixupxer.cleaners.UrlCleaner
import com.fixupxer.processing.UrlNormalizer

/**
 * Cleaner for YouTube URLs - comprehensive tracking removal
 */
object YouTubeCleaner : UrlCleaner {
    override val id = "youtube"
    override val displayName = "YouTube"
    override val category = CleanerCategory.VIDEO_PLATFORMS
    
    // Comprehensive YouTube tracking parameters
    // Most extensive tracking parameter list available
    private val youtubeTracking = setOf(
        // Basic tracking
        "si", "pp", "feature", "app", "attribution_link",
        "embeds_referring_euri", "embeds_referring_origin",
        "embeds_euri", "source_ve_path", "gclid", "ytclid",
        
        // Share & referral tracking
        "share", "shared_via", "ref", "referer", "referrer",
        "fbclid", "sms_ss", "ucbcb", "share_source",
        
        // Analytics & attribution
        "utm_source", "utm_medium", "utm_campaign", "utm_term",
        "utm_content", "utm_id", "utm_reader", "utm_pubreferrer",
        
        // Playback & session tracking
        "session_id", "ei", "ved", "sourceid", "ssfr",
        "continuation", "itct", "ctoken", "sttm",
        
        // Engagement tracking
        "lact", "action_name", "action_type", "annotation_id",
        "src_vid", "feature_id", "c", "client",
        
        // Navigation tracking
        "redir_token", "next", "flow", "entry_point",
        "bp", "sp", "sr_origin", "algo", "aq",
        
        // Mobile app tracking
        "app_version", "app_name", "device_id", "install_id",
        "app_install_ctx", "vndapp", "vndclient", "mweb",
        
        // A/B testing & experiments
        "variant", "experiment_id", "bucket", "treatment",
        "version_id", "test_group", "disable_polymer",
        
        // Recommendation tracking
        "rec_id", "recommendation_info", "shelf_id",
        "video_id_list", "suggested_video_id", "rv",
        
        // Notification tracking
        "notification_id", "notification_type", "alert_type",
        "push_id", "email_id", "sms_id",
        
        // Search & discovery
        "search_sort", "search_filter", "search_type",
        "search_token", "query_source", "suggested_by",
        
        // Ads & monetization
        "ad_id", "campaign_id", "creative_id", "placement_id",
        "ad_type", "ad_signal", "autonav", "autoplay",
        
        // Channel & creator tracking
        "channel_feature", "creator_channel_id", "ab_beacon",
        "subscribe_feature", "notification_menu_feature",
        
        // Playlist tracking (non-essential ones)
        "playnext", "shuffle", "mix_id", "nolist",
        
        // Technical & misc
        "bpctr", "pbjreload", "pbj", "hl", "gl", "persist_hl",
        "persist_gl", "has_verified", "nohtml5", "html5",
        "spfreload", "disable_related_pause_replay", "ytrcc",
        "vvt", "vl", "vq", "vss_host", "vps", "vpst", "vpstf",
        "vpsrc", "w", "h", "bh", "bw", "adsense_video_doc_id",
        
        // Legacy parameters
        "annotation_src", "endscreen_src", "iv_load_policy",
        "iv_allow_external_links", "iv_endscreen_url",
        "annotation_3_module", "cid", "cin", "cver"
    )
    
    // Parameters essential for YouTube functionality
    private val preserveParams = setOf(
        "v",     // Video ID
        "t",     // Timestamp (e.g., t=1m30s)
        "list",  // Playlist ID
        "index", // Position in playlist
        "start", // Start time in seconds
        "end",   // End time in seconds
        "ab_channel", // Channel name (sometimes needed)
        "search_query", // For search results
        "q",     // Alternative search query parameter
        "radio", // Radio station ID (YouTube Music)
        "p",     // Page parameter for search results
        "page",  // Alternative page parameter
        "sort",  // Sort order
        "view",  // View type (grid/list)
        "shelf_id", // Shelf identifier (needed for navigation)
        "nolist" // Disable playlist (user preference)
    )
    
    override fun matches(url: String): Boolean {
        return UrlNormalizer.urlMatchesAnyDomain(
            url,
            listOf("youtube.com", "youtu.be", "youtube-nocookie.com")
        )
    }
    
    override fun clean(url: String): String {
        if (!matches(url)) return url

        // Handle YouTube Music differently
        if (UrlNormalizer.urlMatchesDomain(url, "music.youtube.com")) {
            return cleanYouTubeMusic(url)
        }
        
        // Standard YouTube cleaning
        return cleanStandardUrl(url)
    }
    
    private fun cleanStandardUrl(url: String): String {
        try {
            // If no query parameters, return as is
            val idx = url.indexOf('?')
            if (idx == -1 || url.indexOf('#').let { it >= 0 && it < idx }) {
                return url
            }
            
            val base = url.substring(0, idx)
            val queryAndFragment = url.substring(idx + 1)
            
            // Handle fragment
            val fragmentIdx = queryAndFragment.indexOf('#')
            val query = if (fragmentIdx > -1) {
                queryAndFragment.substring(0, fragmentIdx)
            } else {
                queryAndFragment
            }
            val fragment = if (fragmentIdx > -1) {
                queryAndFragment.substring(fragmentIdx)
            } else {
                ""
            }
            
            // Process parameters - aggressive cleaning
            val kept = query.split('&').mapNotNull { pair ->
                val eqIdx = pair.indexOf('=')
                val key = if (eqIdx == -1) pair else pair.substring(0, eqIdx)
                
                // Keep essential and unknown params, remove only known tracking
                when {
                    preserveParams.contains(key) -> pair  // Always keep essential params
                    youtubeTracking.contains(key) -> null  // Remove tracking params
                    else -> pair
                }
            }.filter { it.isNotEmpty() }
            
            return if (kept.isEmpty()) {
                base + fragment
            } else {
                base + "?" + kept.joinToString("&") + fragment
            }
        } catch (e: Exception) {
            // On error, return original URL
            return url
        }
    }
    
    private fun cleanYouTubeMusic(url: String): String {
        // YouTube Music specific preserve params
        val musicPreserveParams = preserveParams + setOf(
            "radio", // Radio station
            "si" // Share ID (sometimes needed for music)
        )
        
        try {
            // If no query parameters, return as is
            val idx = url.indexOf('?')
            if (idx == -1 || url.indexOf('#').let { it >= 0 && it < idx }) {
                return url
            }
            
            val base = url.substring(0, idx)
            val queryAndFragment = url.substring(idx + 1)
            
            // Handle fragment
            val fragmentIdx = queryAndFragment.indexOf('#')
            val query = if (fragmentIdx > -1) {
                queryAndFragment.substring(0, fragmentIdx)
            } else {
                queryAndFragment
            }
            val fragment = if (fragmentIdx > -1) {
                queryAndFragment.substring(fragmentIdx)
            } else {
                ""
            }
            
            // Process parameters
            val kept = query.split('&').mapNotNull { pair ->
                val eqIdx = pair.indexOf('=')
                val key = if (eqIdx == -1) pair else pair.substring(0, eqIdx)
                
                // Keep music-essential and unknown params, remove only known tracking
                when {
                    musicPreserveParams.contains(key) -> pair
                    youtubeTracking.contains(key) -> null
                    else -> pair
                }
            }.filter { it.isNotEmpty() }
            
            return if (kept.isEmpty()) {
                base + fragment
            } else {
                base + "?" + kept.joinToString("&") + fragment
            }
        } catch (e: Exception) {
            // On error, return original URL
            return url
        }
    }
} 