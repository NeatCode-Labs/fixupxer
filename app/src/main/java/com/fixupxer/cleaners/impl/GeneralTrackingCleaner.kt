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
import com.fixupxer.cleaners.utils.CleanerUtils
import javax.inject.Inject
import java.util.Locale

/**
 * General cleaner for removing tracking parameters from any URL
 * Used as a fallback when no domain-specific cleaner matches
 */
class GeneralTrackingCleaner @Inject constructor() : UrlCleaner {
    override val id = "general"
    override val displayName = "General Tracking"
    override val priority = UrlCleaner.PRIORITY_GENERAL
    override val category = CleanerCategory.GENERAL
    
    companion object {
        // Common tracking parameters used across many sites
        private val COMMON_TRACKING_PARAMS = setOf(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "utm_id", "utm_name", "utm_reader", "utm_brand", "utm_pubreferrer",
            "utm_swu", "utm_viz_id", "utm_referrer", "utm_social", "utm_social-type",
            "fbclid", "gclid", "gclsrc", "gad_source", "dclid", "twclid", "msclkid",
            "yclid", "gbraid", "wbraid", "ko_click_id", "epik", "_ga", "_gl",
            "_hsenc", "_hsmi", "__hssc", "__hstc", "hsctatracking", "mc_cid", "mc_eid",
            "_openstat", "_ke", "_kx", "__s", "vgo_ee", "rb_clickid", "_bta_tid",
            "_bta_c", "ml_subscriber", "ml_subscriber_hash", "oly_anon_id", "oly_enc_id",
            "vero_conv", "vero_id", "wickedid", "irclickid", "irgwc", "sscid", "zanpid",
            "sharedid", "ranmid", "raneaid", "ransiteid", "shareasale_site_id",
            "shareasale_user_id", "spjobid", "spmailingid", "spreportid", "spuserid",
            "__twitter_impression", "mkt_tok", "sfmc_activityid",
            "guccounter", "guce_referrer", "guce_referrer_sig"
        )
        
        // Prefixes that indicate tracking parameters
        private val TRACKING_PREFIXES = listOf(
            "pk_", "mtm_", "matomo_", "piwik_", "wt.", "wt_", "itm_", "elq", "xtor", "at_"
        )
    }
    
    override fun matches(url: String): Boolean {
        // Matches any URL
        return true
    }
    
    override fun clean(url: String): String {
        try {
            val queryIndex = url.indexOf('?')
            val fragmentIndex = url.indexOf('#')
            val removeEchoboxFragment = fragmentIndex >= 0 &&
                url.substring(fragmentIndex + 1).startsWith("Echobox=")
            if (queryIndex < 0 || (fragmentIndex >= 0 && fragmentIndex < queryIndex)) {
                return if (removeEchoboxFragment) url.substring(0, fragmentIndex) else url
            }

            // Use CleanerUtils to split URL and handle edge cases
            val (base, query, rawFragment) = CleanerUtils.splitUrl(url)
            val fragment = if (removeEchoboxFragment) "" else rawFragment
            
            // Preserve existing behavior for an explicit empty query.
            if (query.isEmpty()) {
                return CleanerUtils.rebuildUrl(base, emptyList(), fragment)
            }
            
            // Process parameters
            val kept = query.split('&').mapNotNull { pair ->
                val eqIdx = pair.indexOf('=')
                val key = if (eqIdx == -1) pair else pair.substring(0, eqIdx)
                val lowercaseKey = key.lowercase(Locale.ROOT)
                
                // Check if should remove
                val shouldRemove = 
                    // Check exact matches
                    COMMON_TRACKING_PARAMS.contains(lowercaseKey) ||
                    // Check prefix matches
                    TRACKING_PREFIXES.any { prefix -> lowercaseKey.startsWith(prefix) }
                
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