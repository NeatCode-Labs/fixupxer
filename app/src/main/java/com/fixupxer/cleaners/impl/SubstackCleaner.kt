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
import com.fixupxer.utils.Constants

/**
 * Cleaner for Substack URLs - removes all tracking while preserving essential publication/post IDs
 */
object SubstackCleaner : UrlCleaner {
    override val id = "substack"
    override val displayName = "Substack"
    override val category = CleanerCategory.NEWS_MEDIA
    
    // Comprehensive Substack tracking parameters
    private val substackTracking = setOf(
        // UTM parameters (most common)
        "utm_source", "utm_medium", "utm_campaign", 
        "utm_term", "utm_content", "utm_id",
        
        // Email tracking
        "isFreemail", "isFromEmail", "email_source",
        "email_type", "email_id", "email_click",
        
        // Referral & token tracking
        "r", "ref", "referrer", "token", "access_token",
        "auth_token", "jwt", "session_id",
        
        // Analytics & attribution
        "source", "src", "origin", "attribution",
        "attribution_id", "attribution_source",
        
        // Engagement tracking
        "action", "clicked", "opened", "shared",
        "forwarded", "replied", "reaction",
        
        // A/B testing
        "experiment", "variant", "test", "bucket",
        "ab_test", "feature_flag", "rollout",
        
        // Newsletter tracking
        "newsletter_id", "edition_id", "issue_id",
        "subscriber_id", "list_id", "segment_id",
        
        // Social media tracking
        "social_source", "social_action", "social_network",
        "tw_source", "fb_source", "li_source",
        
        // App tracking
        "app_source", "app_version", "device_id",
        "platform", "os_version", "app_link",
        
        // Payment & subscription tracking
        "payment_source", "subscription_source",
        "trial_source", "upgrade_source", "plan_id",
        
        // Navigation tracking
        "nav_source", "nav_type", "entry_point",
        "exit_point", "page_source", "section",
        
        // Comment & interaction tracking
        "comment_id", "reply_to", "thread_id",
        "notification_id", "notif_type",
        
        // Search & discovery
        "search_source", "search_term", "discovery_source",
        "recommendation_source", "suggested_by",
        
        // Time-based tracking
        "timestamp", "ts", "time", "date",
        "expires", "exp", "iat", "nbf"
    )
    
    // Parameters essential for Substack functionality
    private val preserveParams = setOf(
        "publication_id",  // Essential for identifying the publication
        "post_id"         // Essential for identifying the specific post
    )
    
    override fun matches(url: String): Boolean {
        return UrlNormalizer.urlMatchesDomain(url, Constants.SUBSTACK_DOMAIN)
    }
    
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
            
            // Process parameters - remove known tracking while preserving unknown parameters.
            val kept = query.split('&').mapNotNull { pair ->
                val eqIdx = pair.indexOf('=')
                val key = if (eqIdx == -1) pair else pair.substring(0, eqIdx)
                
                // Keep essential params first to protect them from future tracking-list additions.
                when {
                    preserveParams.contains(key) -> pair  // Keep essential params
                    substackTracking.contains(key) -> null  // Remove known tracking
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