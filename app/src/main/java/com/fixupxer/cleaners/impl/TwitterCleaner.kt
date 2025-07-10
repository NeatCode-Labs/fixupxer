package com.fixupxer.cleaners.impl

import com.fixupxer.cleaners.CleanerCategory
import com.fixupxer.cleaners.UrlCleaner
import com.fixupxer.utils.Constants

/**
 * Cleaner for Twitter/X URLs - comprehensive tracking removal
 */
object TwitterCleaner : UrlCleaner {
    override val id = "twitter"
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
    
    override fun matches(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("twitter.com") || 
               lowerUrl.contains("x.com") ||
               lowerUrl.contains(Constants.FIXUPX_DOMAIN) ||
               lowerUrl.contains(Constants.FXTWITTER_DOMAIN) ||
               lowerUrl.contains(Constants.VXTWITTER_DOMAIN)
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
                    twitterTracking.contains(key) -> null  // Remove tracking params
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