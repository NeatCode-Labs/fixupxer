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
 * Cleaner for Amazon URLs - extracts product IDs and removes tracking
 */
object AmazonCleaner : UrlCleaner {
    override val id = "amazon"
    override val category = CleanerCategory.E_COMMERCE
    
    // Common Amazon domains
    private val amazonDomains = listOf(
        "amazon.com", "amazon.co.uk", "amazon.de", "amazon.fr", 
        "amazon.it", "amazon.es", "amazon.ca", "amazon.co.jp",
        "amazon.in", "amazon.com.br", "amazon.com.mx", "amazon.com.au",
        "amzn.to", "amzn.eu", "amzn.asia"
    )
    
    // Comprehensive Amazon tracking parameters
    // Most extensive tracking parameter list available
    private val amazonTracking = setOf(
        // Basic tracking
        "tag", "linkCode", "linkId", "camp", "creative",
        "creativeASIN", "ref_", "ref", "refRID", "ie",
        
        // Product tracking
        "pf_rd_m", "pf_rd_s", "pf_rd_r", "pf_rd_t", "pf_rd_p", "pf_rd_i",
        "pd_rd_w", "pd_rd_wg", "pd_rd_r", "pd_rd_i", "pf_rd_sg",
        "pd_rd_sg", "pd_dcr", "pd_rd_w-k", "pd_rd_r-k",
        
        // Search & discovery tracking
        "qid", "rank", "spIA", "spLa", "suggestionType",
        "sprefix", "crid", "cv_ct_cx", "cv_ct_id", "cv_ct_pg",
        
        // Share & referral tracking
        "share", "share_at", "socialMediaReferral", "referer",
        "ms3_c", "spLM", "spPD", "spIAC",
        
        // Analytics & attribution
        "smid", "ascsubtag", "tag_id", "sa-no-redirect",
        "pldnSite", "th", "psc", "dchild", "almBrandId",
        
        // Navigation tracking
        "navLanguage", "rnid", "rh", "srs", "starsLeft",
        "qsid", "sr", "sres", "__mk_de_DE", "__mk_en_US",
        
        // Session & request tracking
        "sessionId", "sqid", "colid", "coliid", "ubid",
        "visitId", "asc_campaign", "asc_source", "asc_refurl",
        
        // A/B testing & experiments
        "feature", "variant", "experiment", "treatment",
        "bucket", "version", "isTest", "qualifier",
        
        // Recommendation tracking
        "_encoding", "bbn", "dc", "dcm", "field-lbr_brands_browse-bin",
        "fst", "lo", "me", "pf", "pf_rd_r", "pf_rd_t",
        
        // Device & platform tracking
        "deviceType", "deviceVersion", "appVersion", "platform",
        "isAndroid", "isIOS", "isMobile", "isTablet",
        
        // Cart & checkout tracking
        "carId", "cartInitiateId", "fromAnywhere", "sbbutton",
        "ascsess", "asc_contentid", "asc_item", "asc_topic",
        
        // Affiliate & commission tracking
        "associateTag", "ascsubtag", "marketplaceID", "maas",
        "adId", "ad-id", "campaign", "adgrpid",
        
        // Prime & subscription tracking
        "primeCampaignId", "primeExpirationDate", "primeStatus",
        "benefitId", "benefitOptimizationId", "subscriptionId",
        
        // Wish list & registry tracking
        "colid", "coliid", "wl_id", "registry", "registryId",
        "registryType", "weblab-wl", "wl-id",
        
        // Technical & misc parameters
        "encoding", "node", "nodeId", "field-keywords",
        "url", "path", "hidden-keywords", "fap", "fie",
        "redirect", "redirectASIN", "sspa", "spNs",
        
        // Legacy parameters
        "t", "marketplaceId", "merchant", "queueName",
        "sr", "rdc", "rdm", "rdl", "sourcecustomerorglistid",
        "sourcecustomerorglistitemid", "sz", "idx"
    )
    
    // Parameters to preserve for non-product pages
    private val preserveParams = setOf(
        "keywords", // Search keywords
        "k", // Search keywords (alternative)
        "i", // Category/index
        "rh", // Refinement hierarchy
        "s", // Sort parameter
        "page", // Pagination
        "node", // Category node
        "field-keywords", // Search field
        "field-brand", // Brand filter
        "field-price-range", // Price filter
        "field-availability", // Availability filter
        "sort", // Sort order
        "lo", // Layout option
        "fs" // Filter state
    )
    
    override fun matches(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return amazonDomains.any { domain ->
            lowerUrl.contains(domain)
        }
    }
    
    override fun clean(url: String): String {
        // Extract product ID if present
        val productId = extractProductId(url)
        if (productId != null) {
            // Build minimal product URL
            val domain = extractDomain(url) ?: "www.amazon.com"
            return "https://$domain/dp/$productId"
        }
        
        // Otherwise clean tracking parameters from non-product pages
        return cleanNonProductUrl(url)
    }
    
    private fun cleanNonProductUrl(url: String): String {
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
                    amazonTracking.contains(key) -> null  // Remove tracking params
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
    
    private fun extractProductId(url: String): String? {
        // Amazon product IDs are typically 10 characters (B00XXXXXX format)
        val patterns = listOf(
            // /dp/PRODUCTID format
            Regex("/dp/([A-Z0-9]{10})"),
            // /gp/product/PRODUCTID format
            Regex("/gp/product/([A-Z0-9]{10})"),
            // /exec/obidos/ASIN/PRODUCTID format
            Regex("/exec/obidos/ASIN/([A-Z0-9]{10})"),
            // Sometimes in the path like /Product-Name/PRODUCTID
            Regex("/([A-Z0-9]{10})(?:/|\\?|$)")
        )
        
        for (pattern in patterns) {
            pattern.find(url)?.let { match ->
                return match.groupValues[1]
            }
        }
        
        return null
    }
    
    private fun extractDomain(url: String): String? {
        for (domain in amazonDomains) {
            if (url.contains(domain)) {
                // Check if it's a subdomain
                val regex = Regex("((?:www\\.|smile\\.)?$domain)")
                regex.find(url)?.let { match ->
                    return match.value
                }
            }
        }
        return null
    }
} 