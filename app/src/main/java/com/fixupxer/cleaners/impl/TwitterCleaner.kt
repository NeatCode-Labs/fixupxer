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
import com.fixupxer.processing.PlatformDomainConverter

/**
 * Cleaner for Twitter/X URLs - comprehensive tracking removal
 */
object TwitterCleaner : UrlCleaner {
    override val id = "twitter"
    override val displayName = "Twitter/X"
    override val priority = UrlCleaner.PRIORITY_CONVERSION
    override val category = CleanerCategory.SOCIAL_MEDIA
    
    // Comprehensive Twitter/X tracking parameters
    // Most extensive tracking parameter list available
    private val twitterTracking = setOf(
        // Basic tracking
        "s", "t", "ref_src", "ref_url", "via", 
        "__twitter_impression", "cn", "src",
        
        // Share & referral tracking
        "share", "shared_via", "ref", "referer",
        "twclid", "twgr", "twcamp", "twterm",
        
        // Analytics & attribution
        "impression_id", "promoted_content", "click_id",
        "attribution_id", "attribution_link", "source",
        
        // Timeline & feed tracking
        "timeline_id", "tweet_mode", "tweet_result_type",
        "include_entities", "include_rts", "exclude_replies",
        
        // Engagement tracking
        "like_source", "retweet_source", "reply_source",
        "follow_source", "unfollow_source", "block_source",
        
        // Navigation tracking
        "nav_source", "entry_point", "surface", "trigger",
        "context", "mx", "mi", "mk", "mz",
        
        // Mobile app tracking
        "app", "app_name", "app_id", "device_id",
        "install_id", "advertising_id", "limit_ad_tracking",
        
        // A/B testing & experiments
        "variant", "bucket", "experiment", "treatment",
        "version_id", "rollout_hash", "test_group",
        
        // Session & request tracking
        "session_id", "request_id", "query_id",
        "cursor", "max_id", "since_id", "count",
        
        // Card & media tracking
        "card_name", "card_url", "media_id",
        "media_tags", "tagged_users", "place_id",
        
        // Notification tracking
        "notif_id", "notif_type", "notif_source",
        "push_id", "email_id", "sms_id",
        
        // Search & discovery
        "q_source", "result_type", "vertical",
        "f", "src_hashtag", "src_trend",
        
        // Ads & promoted content
        "ad_id", "campaign_id", "creative_id",
        "advertiser_id", "placement_id", "targeting_criteria",
        
        // Twitter Blue / Premium tracking
        "premium_source", "subscription_source", "feature_source",
        "blue_verified", "premium_tier",
        
        // Legacy parameters
        "tw_p", "tw_i", "tw_u", "tw_o",
        "original_referer", "uprof"
    )
    
    // Parameters essential for Twitter functionality
    private val preserveParams = setOf(
        "lang",        // Language preference
        "theme",       // Dark/light theme
        "display",     // Display preferences
        "tz",          // Timezone
        "include_profile_interstitial_type", // Profile display
        "include_blocking", // Blocking info
        "include_blocked_by", // Blocked by info
        "include_followed_by", // Following info
        "include_want_retweets", // Retweet preferences
        "include_mute_edge", // Mute preferences
        "include_can_dm" // DM permissions
    )
    
    override fun matches(url: String): Boolean = PlatformDomainConverter.isKnownXUrl(url)
    
    override fun clean(url: String): String {
        if (!matches(url)) return url

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
            
            // Process parameters - remove known tracking, keep everything else
            val kept = query.split('&').mapNotNull { pair ->
                val eqIdx = pair.indexOf('=')
                val key = if (eqIdx == -1) pair else pair.substring(0, eqIdx)
                
                // Keep essential and unknown params, remove only known tracking
                when {
                    preserveParams.contains(key) -> pair  // Always keep essential params
                    twitterTracking.contains(key) -> null  // Remove tracking params
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