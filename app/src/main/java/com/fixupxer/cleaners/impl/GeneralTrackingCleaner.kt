package com.fixupxer.cleaners.impl

import com.fixupxer.cleaners.CleanerCategory
import com.fixupxer.cleaners.UrlCleaner
import com.fixupxer.cleaners.utils.CleanerUtils
import javax.inject.Inject

/**
 * General cleaner for removing tracking parameters from any URL
 * Used as a fallback when no domain-specific cleaner matches
 */
class GeneralTrackingCleaner @Inject constructor() : UrlCleaner {
    override val id = "general"
    override val category = CleanerCategory.GENERAL
    
    companion object {
        // Common tracking parameters used across many sites
        private val COMMON_TRACKING_PARAMS = setOf(
            // UTM parameters
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", 
            "utm_id", "utm_name", "utm_reader", "utm_brand", "utm_pubreferrer",
            "utm_swu", "utm_viz_id", "utm_referrer", "utm_social", "utm_social-type",
            
            // Click IDs
            "fbclid", "gclid", "dclid", "twclid", "msclkid", "yclid", "gbraid", 
            "wbraid", "ko_click_id", "epik", "pp", "gclsrc", "gad_source",
            
            // Analytics
            "_ga", "_gl", "_hsenc", "_hsmi", "__hssc", "__hstc", "mc_cid", "mc_eid",
            "_openstat", "vgo_ee", "hsCtaTracking", "_ke", "_kx", "__hmb", "__hmc",
            "__hmd", "__hml", "__s", "rb_clickid", "ai", "_bta_tid", "_bta_c",
            
            // General tracking
            "ref", "referer", "referrer", "source", "src", "share", "si", "spm", 
            "campaign_id", "ad_id", "affiliate", "aff_id", "click_id", "clickid",
            "session_id", "sessionid", "soc_src", "soc_trk", "trk", "trkInfo",
            
            // Email tracking
            "eid", "mid", "ml_subscriber", "ml_subscriber_hash", "eh", "amp",
            "amp_device_id", "usqp", "ved", "usp", "sa", "cid", "icid",
            
            // Social & sharing
            "shared_by", "share_id", "share_token", "__twitter_impression", "spJobID",
            "spMailingID", "spReportId", "spUserID", "__tn__", "fb_action_ids",
            
            // E-commerce
            "irclickid", "irgwc", "ircid", "sharedid", "sscid", "wickedid",
            "zanpid", "pepperjam_enterprise_affiliate", "ranMID", "ranEAID",
            "ranSiteID", "shareasale_site_id", "shareasale_user_id"
        )
        
        // Prefixes that indicate tracking parameters
        private val TRACKING_PREFIXES = listOf(
            "wt.", "WT.", "pk_", "at_", "sc_", "campaign", "itm_", "elq",
            "matomo_", "mtm_", "clk", "ito", "xtor", "piwik_", "dm_", "cx_"
        )
    }
    
    override fun matches(url: String): Boolean {
        // Matches any URL
        return true
    }
    
    override fun clean(url: String): String {
        try {
            // Use CleanerUtils to split URL and handle edge cases
            val (base, query, fragment) = CleanerUtils.splitUrl(url)
            
            // If no query parameters, return as is
            if (query.isEmpty()) {
                return CleanerUtils.rebuildUrl(base, emptyList(), fragment)
            }
            
            // Process parameters
            val kept = query.split('&').mapNotNull { pair ->
                val eqIdx = pair.indexOf('=')
                if (eqIdx == -1) return@mapNotNull null
                
                val key = pair.substring(0, eqIdx)
                val lowercaseKey = key.lowercase()
                
                // Check if should remove
                val shouldRemove = 
                    // Check exact matches
                    COMMON_TRACKING_PARAMS.contains(lowercaseKey) ||
                    // Check prefix matches
                    TRACKING_PREFIXES.any { prefix -> 
                        lowercaseKey.startsWith(prefix.lowercase()) 
                    }
                
                if (!shouldRemove) {
                    pair
                } else {
                    null
                }
            }.filter { it.isNotEmpty() }
            
            return CleanerUtils.rebuildUrl(base, kept, fragment)
        } catch (e: Exception) {
            // On error, return original URL
            return url
        }
    }
} 