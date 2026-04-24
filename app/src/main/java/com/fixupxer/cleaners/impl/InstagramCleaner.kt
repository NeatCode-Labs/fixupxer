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
import com.fixupxer.utils.Constants

/**
 * Cleaner for Instagram URLs - comprehensive tracking removal
 */
object InstagramCleaner : UrlCleaner {
    override val id = "instagram"
    override val category = CleanerCategory.SOCIAL_MEDIA
    
    // Comprehensive Instagram-specific tracking parameters
    // Most extensive tracking parameter list available
    private val instagramTracking = setOf(
        // Basic tracking
        "igsh", "igshid", "ig_cache_key", "ig_mid", 
        "ig_share_sheet", "__a", "__d", "_rdr", "hl",
        
        // Share tracking
        "share_app_id", "share_sheet_id", "share_id",
        "ig_did", "share_campaign_id", "share_link_id",
        
        // Analytics & attribution
        "_u_code", "_u_source", "_r", "_t", 
        "attribution_link", "ig_nux_id", "ig_referrer",
        
        // Feed & discovery tracking
        "feed_type", "feed_impression_id", "explore_source",
        "ranking_info_token", "media_id_attribution",
        
        // Story & reel tracking
        "story_media_owner", "reel_media_owner_id",
        "media_owner_id", "tray_session_id",
        
        // Engagement tracking
        "like_source", "comment_source", "save_source",
        "share_source", "follow_source", "profile_source",
        
        // Navigation & UI tracking
        "nav_chain", "from_module", "module_name",
        "entry_point", "surface", "trigger",
        
        // A/B testing & experiments
        "variant", "experiment_group", "test_group",
        "rollout_hash", "version_id",
        
        // Session & request tracking
        "session_id", "request_id", "query_id",
        "impression_id", "tracking_token",
        
        // Platform & device
        "device_id", "push_id", "app_id",
        "platform", "os_version", "app_version",
        
        // Ads & commerce
        "ad_id", "campaign_id", "creative_id",
        "merchant_id", "product_id_override",
        
        // Legacy parameters
        "taken-by", "tagged_users", "location_id"
    )
    
    // Parameters essential for Instagram functionality
    private val preserveParams = setOf(
        "img_index",      // Image position in carousel
        "story_media_id", // Story identifier
        "h",              // Height parameter for images
        "w",              // Width parameter for images
        "carousel_index", // Alternative carousel position
        "media_id",       // Direct media reference
        "reel_ids",       // Reel identifiers
        "highlight_reel_ids" // Highlight reel IDs
    )
    
    override fun matches(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains(Constants.INSTAGRAM_DOMAIN) ||
               Constants.INSTAGRAM_PROXY_DOMAINS.any { lowerUrl.contains(it) }
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
            
            // Process parameters - remove ALL tracking parameters
            val kept = query.split('&').mapNotNull { pair ->
                val eqIdx = pair.indexOf('=')
                if (eqIdx == -1) return@mapNotNull null
                
                val key = pair.substring(0, eqIdx)
                
                // Keep if essential, remove if tracking
                when {
                    preserveParams.contains(key) -> pair  // Always keep essential params
                    instagramTracking.contains(key) -> null  // Remove tracking params
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