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
 * Cleaner for Reddit URLs - comprehensive tracking removal
 */
object RedditCleaner : UrlCleaner {
    override val id = "reddit"
    override val category = CleanerCategory.SOCIAL_MEDIA
    
    // Comprehensive Reddit tracking parameters
    // Most extensive tracking parameter list available
    private val redditTracking = setOf(
        // Basic tracking
        "context", "correlator", "rdt_cid", "share_id",
        "ref", "ref_source", "ref_campaign", "ref_share",
        
        // Mobile app tracking
        "_branch_match_id", "_branch_referrer", "_bta_tid",
        "app", "app_name", "app_id", "device_id",
        
        // Analytics & attribution
        "utm_source", "utm_medium", "utm_campaign", "utm_term",
        "utm_content", "utm_id", "utm_name", "utm_reader",
        
        // Session & request tracking
        "session_id", "request_id", "correlation_id",
        "impression_id", "experiment_id", "variant_id",
        
        // Share & referral tracking
        "share", "shared_via", "share_from", "share_source",
        "source", "medium", "campaign", "\$deep_link",
        
        // Navigation tracking
        "nav_source", "entry_point", "surface", "trigger",
        "source_element", "action_source", "ref_ui",
        
        // A/B testing & experiments
        "variant", "experiment", "treatment", "bucket",
        "feature", "rollout_hash", "test_group",
        
        // Notification tracking
        "notification_id", "message_id", "email_id",
        "push_id", "alert_id", "digest_id",
        
        // Ads & promoted content
        "ad_id", "campaign_id", "creative_id", "placement_id",
        "promoted", "sponsor_id", "attribution_id",
        
        // Feed & timeline tracking
        "feed_sort", "timeline_sort", "feed_position",
        "nsfw_reason", "quarantine_opt_in", "show_media",
        
        // Award & coin tracking
        "award_id", "coin_reward_id", "coin_bonus_id",
        "gilding_detail", "award_type", "is_anonymous",
        
        // Chat & messaging
        "chat_id", "channel_id", "thread_id", "invite_code",
        "chat_subreddit_id", "member_count",
        
        // Legacy & misc parameters
        "st", "sh", "s", "dest", "id_token", "state",
        "quicklink", "subredditName", "creation_source"
    )
    
    // Parameters essential for Reddit functionality
    private val preserveParams = setOf(
        "context",      // Comment context (how many parent comments)
        "sort",         // Sort order (hot, new, top, etc.)
        "t",            // Time filter (day, week, month, year, all)
        "q",            // Search query
        "type",         // Search type (link, comment, user)
        "restrict_sr",  // Restrict to subreddit
        "include_over_18", // NSFW filter
        "after",        // Pagination cursor
        "before",       // Pagination cursor
        "limit",        // Results per page
        "count",        // Pagination count
        "show",         // Show parameter
        "raw_json",     // JSON format preference
        "depth",        // Comment depth
        "showmore",     // Show more comments
        "sr_detail",    // Subreddit details
        "sr",           // Subreddit
        "is_multi"      // Multi-reddit indicator
    )
    
    override fun matches(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("reddit.com") ||
               lowerUrl.contains("redd.it") // Short links
    }
    
    override fun clean(url: String): String {
        // Reddit short links need to be preserved as-is (can't expand client-side)
        if (url.contains("redd.it")) {
            val idx = url.indexOf('?')
            return if (idx > -1) url.substring(0, idx) else url
        }
        
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
                    redditTracking.contains(key) -> null  // Remove tracking params
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