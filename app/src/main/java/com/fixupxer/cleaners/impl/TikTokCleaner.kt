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

/**
 * Cleaner for TikTok URLs - comprehensive tracking removal
 */
object TikTokCleaner : UrlCleaner {
    override val id = "tiktok"
    override val category = CleanerCategory.SOCIAL_MEDIA
    
    // Comprehensive TikTok tracking parameters
    // Most extensive tracking parameter list available
    private val tiktokTracking = setOf(
        // Basic tracking
        "_r", "_t", "_d", "tt_from", "tt_content", "share_source",
        "u_code", "preview_pb", "share_app_id", "share_link_id",
        
        // Share & referral tracking
        "share_item_id", "share_author_id", "share_app_name",
        "timestamp", "utm_source", "utm_campaign", "utm_medium",
        
        // Analytics & attribution
        "enter_from", "enter_method", "from_page", "refer",
        "referer_url", "referer_video_id", "anchor_id", "source",
        
        // Session & request tracking
        "session_id", "request_id", "webcast_app_id", "room_id",
        "sec_user_id", "sec_uid", "u_ser", "user_id",
        
        // Device & client tracking
        "device_platform", "device_type", "browser_language",
        "browser_name", "browser_version", "os_version",
        "screen_width", "screen_height", "client_ab_test",
        
        // Navigation tracking
        "previous_page", "current_page", "depth", "tab_name",
        "gd_label", "category_name", "schema_type", "obj_id",
        
        // Feed & content tracking
        "item_id", "group_id", "music_id", "mix_id", "poi_id",
        "region", "priority_region", "language", "verifyFp",
        
        // Live stream tracking
        "live_id", "from_source", "stream_id", "webcast_sdk_version",
        "webcast_language", "webcast_locale", "room_token",
        
        // E-commerce tracking
        "pdp_item_id", "shop_id", "product_id", "promotion_id",
        "cart_id", "checkout_id", "payment_id", "order_id",
        
        // Ads & marketing
        "campaign_id", "ad_id", "creative_id", "placement_id",
        "ad_tag", "traffic_type", "is_ads", "ads_creative_id",
        
        // A/B testing & experiments
        "test_id", "variant_id", "experiment_id", "version_id",
        "bucket_id", "layer_id", "service_id", "strategy_id",
        
        // App tracking
        "app", "app_name", "app_language", "carrier", "mcc_mnc",
        "sys_language", "tz_name", "residence", "is_copy_url",
        
        // Social features tracking
        "is_from_self", "is_from_friend", "relation_type",
        "social_info", "group_source", "click_reason",
        
        // Legacy & misc parameters
        "checksum", "cursor", "count", "offset", "openudid",
        "uuid", "_aem", "_aem_test", "msToken", "X-Bogus",
        "magic", "scene", "comment_id", "reply_id", "question_id",
        "sticker_id", "effect_id", "filter_id", "game_id"
    )
    
    // Parameters essential for TikTok functionality
    private val preserveParams = setOf(
        "lang",         // Language preference
        "q",            // Search query
        "t",            // Timestamp/time parameter
        "embed",        // Embed parameter
        "video_id",     // Direct video ID (essential)
        "item_id",      // Content item ID (sometimes essential)
        "modal",        // Modal display parameter
        "search_keyword", // Search keyword
        "sort_type",    // Sort order
        "content_type", // Content type filter
        "creation_time", // Time filter
        "hashtag",      // Hashtag parameter
        "sound",        // Sound/music parameter
        "effect",       // Effect parameter (sometimes needed)
        "display_method", // Display method
        "is_fullscreen" // Fullscreen parameter
    )
    
    override fun matches(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("tiktok.com") ||
               lowerUrl.contains("tiktokcdn.com") ||
               lowerUrl.contains("tiktokv.com")
    }
    
    override fun clean(url: String): String {
        try {
            // If no query parameters, return as is
            if (!url.contains("?")) {
                return url
            }
            
            val idx = url.indexOf('?')
            if (idx == -1) return url
            
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
                if (eqIdx == -1) return@mapNotNull null
                
                val key = pair.substring(0, eqIdx)
                
                // Keep if essential, remove if tracking or unknown
                when {
                    preserveParams.contains(key) -> pair  // Always keep essential params
                    tiktokTracking.contains(key) -> null  // Remove tracking params
                    else -> null  // Remove unknown params (aggressive cleaning)
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