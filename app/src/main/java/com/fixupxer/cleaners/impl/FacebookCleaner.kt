package com.fixupxer.cleaners.impl

import com.fixupxer.cleaners.CleanerCategory
import com.fixupxer.cleaners.UrlCleaner
import com.fixupxer.utils.Constants

/**
 * Cleaner for Facebook URLs - comprehensive tracking removal
 */
object FacebookCleaner : UrlCleaner {
    override val id = "facebook"
    override val category = CleanerCategory.SOCIAL_MEDIA
    
    // Comprehensive Facebook tracking parameters
    // Most extensive tracking parameter list available
    private val facebookTracking = setOf(
        // Basic tracking
        "fbclid", "__cft__", "__tn__", "mibextid", "epa", "_gl",
        "fb_action_ids", "fb_action_types", "fb_source", "fb_ref",
        
        // Share & referral tracking
        "share_id", "share_source", "share_campaign", "share_medium",
        "_branch_match_id", "_branch_referrer", "branch_data",
        
        // Analytics & attribution
        "fb_comment_id", "fb_story_location", "fb_dtsg_ag",
        "attribution_id", "attribution_link", "click_id",
        
        // Feed & timeline tracking
        "feed_story_type", "timeline_context_item_type",
        "timeline_context_item_source", "privacy_mutation_token",
        
        // Engagement tracking
        "action_history", "action_type_map", "action_ref_map",
        "reaction_type", "reaction_surface", "like_source",
        
        // Navigation tracking
        "nav_source", "entry_point", "surface", "trigger",
        "ref_type", "ref_component", "ref_page", "ref_module",
        
        // Mobile app tracking
        "app_id", "app_data", "device_id", "install_id",
        "advertising_id", "limit_ad_tracking", "push_id",
        
        // A/B testing & experiments
        "variant", "experiment", "treatment", "bucket",
        "version_id", "rollout_hash", "test_group", "qe_info",
        
        // Session & request tracking
        "session_id", "request_id", "query_id", "impression_id",
        "tracking_id", "correlation_id", "client_token",
        
        // Video & media tracking
        "video_id", "media_id", "media_set", "media_type",
        "play_location", "video_time_position", "video_view_time",
        
        // Notification tracking
        "notif_id", "notif_t", "notif_type", "alert_id",
        "push_campaign", "email_id", "sms_id",
        
        // Groups & pages tracking
        "group_id", "page_id", "page_internal", "admin_panel_tab",
        "business_id", "asset_id", "content_id",
        
        // Ads & commerce tracking
        "ad_id", "campaign_id", "creative_id", "placement_id",
        "audience_id", "boost_id", "promotion_id", "product_id",
        
        // Messenger tracking
        "messaging_source", "source_id", "alert_message_id",
        "thread_id", "message_id", "mailbox_id",
        
        // Privacy & permissions
        "privacy_source", "privacy_mutation_source", "consent_id",
        "gdpr_consent", "data_policy_version",
        
        // Legacy & misc parameters
        "sk", "sfnsn", "s", "hc", "hc_ref", "hc_location",
        "pn_ref", "aref", "medium", "linkshim", "next",
        "acontext", "paipv", "entrypoint", "fref", "rc"
    )
    
    // Parameters essential for Facebook functionality
    private val preserveParams = setOf(
        "id",           // User/Page ID
        "story_fbid",   // Story ID
        "fbid",         // Facebook ID
        "set",          // Photo set
        "type",         // Content type
        "theater",      // Theater mode for photos
        "av",           // Video parameter
        "v",            // Version/Video parameter
        "locale",       // Language/locale
        "ref",          // Reference (sometimes needed)
        "hc_location",  // Location parameter
        "comment_id",   // Comment reference
        "reply_comment_id", // Reply reference
        "notif_id",     // Notification ID (kept for functionality)
        "notif_t",      // Notification type (kept for functionality)
        "tab",          // Tab selection
        "sk",           // Sort key
        "filter",       // Filter type
        "section"       // Section identifier
    )
    
    override fun matches(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains(Constants.FACEBOOK_DOMAIN) ||
               lowerUrl.contains(Constants.FACEBOOKEZ_DOMAIN) ||
               lowerUrl.contains("fb.com") ||
               lowerUrl.contains("m.facebook.com")
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
                val key = if (eqIdx == -1) {
                    // Parameter without value (e.g., "theater")
                    pair
                } else {
                    pair.substring(0, eqIdx)
                }
                
                // Keep if essential, remove if tracking or unknown
                when {
                    preserveParams.contains(key) -> pair  // Always keep essential params
                    facebookTracking.contains(key) -> null  // Remove tracking params
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