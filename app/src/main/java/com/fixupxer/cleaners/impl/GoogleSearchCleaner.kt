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
import java.net.URLDecoder

/**
 * Cleaner for Google Search URLs - comprehensive tracking removal and redirect extraction
 */
object GoogleSearchCleaner : UrlCleaner {
    override val id = "google_search"
    override val displayName = "Google Search"
    override val priority = UrlCleaner.PRIORITY_EXTRACTION
    override val category = CleanerCategory.SEARCH_ENGINES
    
    // Google domains across different regions
    private val googleDomains = setOf(
        "google.com", "google.co.uk", "google.de", "google.fr",
        "google.it", "google.es", "google.co.jp", "google.ca",
        "google.com.au", "google.co.in", "google.com.br", "google.ru",
        "google.nl", "google.pl", "google.com.mx", "google.co.kr"
    )
    
    // Comprehensive Google tracking parameters
    // Most extensive tracking parameter list available
    private val googleTracking = setOf(
        // Basic tracking
        "ved", "ei", "usg", "sa", "source", "sourceid",
        "sxsrf", "biw", "bih", "dpr", "cad", "uact",
        
        // Search tracking
        "oq", "aq", "aqs", "gs_lcp", "gs_lcrp", "sclient",
        "gs_l", "gs_ssp", "gs_rn", "gs_ri", "gs_mss",
        
        // Click & interaction tracking
        "rct", "cd", "vet", "esrc", "espv", "cshid",
        "ictx", "usg", "sig2", "act", "rc", "jfp",
        
        // Analytics & attribution
        "utm_source", "utm_medium", "utm_campaign",
        "utm_term", "utm_content", "gclid", "dclid",
        
        // Session & request tracking
        "bvm", "ion", "prmd", "iflsig", "rlz", "pccc",
        "psi", "stick", "tci", "sqi", "bav", "pf",
        
        // Device & client tracking
        "atyp", "asst", "client", "hs", "authuser",
        "pq", "pdl", "nfpr", "spell", "npsic", "mvs",
        
        // Feature & experiment tracking
        "agsad", "gfe_rd", "gws_rd", "complete", "pval",
        "noj", "btnG", "btnI", "site", "output", "domains",
        
        // Image search tracking
        "tbm", "ijn", "imgrc", "imgrefurl", "imgurl",
        "docid", "tbnid", "vet", "zoom", "vt",
        
        // Maps & local tracking
        "sll", "sspn", "vps", "vpsrc", "msa", "msid",
        "mid", "skstate", "rtlr", "rlha", "rllag",
        
        // News tracking
        "cf", "ncl", "ndsp", "nca", "nds", "ncf",
        
        // Shopping tracking
        "psb", "psig", "rflfq", "rldimm", "lsft", "shm",
        "ssta", "sstk", "st", "tch", "tcfs", "rfl",
        
        // AMP & mobile tracking
        "amp", "amp_ct", "amp_url", "ampshare", "usqp",
        
        // Ads tracking
        "adtest", "adsafe", "adk", "aggsa", "ccd", "dq",
        "npa", "ohost", "opv", "ovss", "pf_rd_i", "pglt",
        
        // Legacy & misc parameters
        "pbx", "rbas", "rbs", "rciv", "rct", "tbo",
        "trex", "uule", "uuld", "vld", "zx"
    )
    
    // Parameters essential for Google functionality
    private val preserveParams = setOf(
        "q",       // Search query
        "tbm",     // Search type (images, videos, news, etc.)
        "tbs",     // Search tools/filters
        "start",   // Pagination offset
        "num",     // Number of results
        "hl",      // Interface language
        "lr",      // Language restrict
        "safe",    // Safe search setting
        "nfpr",    // Exact search flag
        "filter",  // Duplicate filter
        "as_q",    // Advanced search query
        "as_epq",  // Exact phrase
        "as_oq",   // OR terms
        "as_eq",   // Exclude terms
        "as_qdr",  // Time range
        "as_rights", // Usage rights
        "imgsz",   // Image size
        "imgtype", // Image type
        "imgc",    // Image color
        "gl",      // Geographic location
        "cr"       // Country restrict
    )
    
    override fun matches(url: String): Boolean {
        val host = UrlNormalizer.extractAsciiHost(url)
        return googleDomains.any { UrlNormalizer.hostMatchesDomain(host, it) } &&
            rawPath(url).let { isPathSegment(it, "/url") || isPathSegment(it, "/search") }
    }
    
    override fun clean(url: String): String {
        if (!matches(url)) return url

        // Check if this is a redirect URL
        if (isPathSegment(rawPath(url), "/url") && hasRawQuery(url)) {
            // Extract the actual URL from 'url' or 'q' parameter
            extractRedirectUrl(url)?.let { extractedUrl ->
                return extractedUrl
            }
        }
        
        // For search queries and other Google URLs, clean tracking parameters
        return cleanGoogleUrl(url)
    }
    
    private fun cleanGoogleUrl(url: String): String {
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
            
            // Process parameters - aggressive cleaning
            val kept = query.split('&').mapNotNull { pair ->
                val eqIdx = pair.indexOf('=')
                val key = if (eqIdx == -1) pair else pair.substring(0, eqIdx)
                
                // Keep essential and unknown params, remove only known tracking
                when {
                    preserveParams.contains(key) -> pair  // Always keep essential params
                    googleTracking.contains(key) -> null  // Remove tracking params
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
    
    private fun extractRedirectUrl(url: String): String? {
        // Try to extract from 'url' parameter first, then 'q'
        val patterns = listOf(
            Regex("[?&]url=([^&]+)"),
            Regex("[?&]q=([^&]+)")
        )
        
        for (pattern in patterns) {
            pattern.find(url)?.let { match ->
                try {
                    // Decode the URL
                    val encodedUrl = match.groupValues[1]
                    val decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8")
                    
                    // Validate it's a proper URL
                    if (decodedUrl.startsWith("http://") || decodedUrl.startsWith("https://")) {
                        // Return the extracted URL as-is
                        // The CleanerService will handle cleaning it with appropriate cleaners
                        return decodedUrl
                    }
                } catch (e: Exception) {
                    // Continue to next pattern
                }
            }
        }
        
        return null
    }

    /** Segment-boundary path check: "/url" matches "/url" and "/url/…" but not "/urlfoo". */
    private fun isPathSegment(path: String, segment: String): Boolean =
        path == segment || path.startsWith("$segment/")

    private fun rawPath(url: String): String {
        val authorityStart = url.indexOf("://")
            .let { if (it >= 0) it + 3 else 0 }
        val pathStart = url.indexOfAny(charArrayOf('/', '?', '#'), authorityStart)
        if (pathStart < 0 || url[pathStart] != '/') return ""
        val pathEnd = url.indexOfAny(charArrayOf('?', '#'), pathStart)
            .let { if (it >= 0) it else url.length }
        return url.substring(pathStart, pathEnd)
    }

    private fun hasRawQuery(url: String): Boolean {
        val queryIndex = url.indexOf('?')
        val fragmentIndex = url.indexOf('#')
        return queryIndex >= 0 && (fragmentIndex < 0 || queryIndex < fragmentIndex)
    }
} 