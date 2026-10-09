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
import com.fixupxer.utils.InstagramProxyStore
import java.net.URI
import java.util.Locale

/**
 * Canonical post cleanup with selective fallback for special routes and proxies.
 */
object InstagramCleaner : UrlCleaner {
    override val id = "instagram"
    override val displayName = "Instagram"
    override val priority = UrlCleaner.PRIORITY_CONVERSION
    override val category = CleanerCategory.SOCIAL_MEDIA
    
    private val postPath = Regex("^/(?:([A-Za-z0-9._]+)/)?(?:p|reel|reels|tv)/[A-Za-z0-9_-]+/?$")
    private val reservedPrefixes = setOf(".", "..", "share", "accounts", "oauth", "explore", "direct", "stories")
    private val carouselIndex = Regex("[0-9]+")

    // Fallback for special routes and proxies, whose query may have other functions.
    private val instagramTracking = setOf(
        // Basic tracking
        // Instagram's IGShareIdParams lists stkn as current, followed by igsi/igsh/igshid.
        "igsh", "igshid", "igsi", "stkn", "ig_cache_key", "ig_mid",
        "ig_share_sheet", "__a", "__d", "_rdr", "hl",
        
        // Share tracking
        "share_app_id", "share_sheet_id", "share_id", "ig_rid", "exln", "obrf",
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
    
    override fun matches(url: String): Boolean {
        // All known proxies (fixed + custom + legacy) so e.g. legacy eeinstagram.com
        // links still get Instagram-specific parameter cleaning (igsh, igshid, igsi, ...).
        return UrlNormalizer.urlMatchesAnyDomain(
            url,
            listOf(Constants.INSTAGRAM_DOMAIN) + InstagramProxyStore.allKnownProxies()
        )
    }
    
    override fun clean(url: String): String {
        if (!matches(url)) return url

        try {
            cleanCanonicalPost(url)?.let { return it }

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
            
            // The fallback removes only listed keys; all other pairs survive.
            val kept = query.split('&').filter { pair ->
                val eqIdx = pair.indexOf('=')
                val key = if (eqIdx == -1) pair else pair.substring(0, eqIdx)
                pair.isNotEmpty() && key !in instagramTracking
            }
            
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

    /**
     * Public post permalinks identify their content in the path. Keep only a
     * numeric carousel selector, so rotating share-key names need no catalog update.
     * Proxy URLs retain their existing policy: custom proxies may use query options.
     */
    private fun cleanCanonicalPost(url: String): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "https" && scheme != "http") return null
        if (uri.rawUserInfo != null || uri.port !in listOf(-1, if (scheme == "https") 443 else 80)) return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        val domain = Constants.INSTAGRAM_DOMAIN
        if (host != domain && host != "www.$domain" && host != "m.$domain") return null
        val match = postPath.matchEntire(uri.rawPath.orEmpty()) ?: return null
        if (match.groupValues[1].lowercase(Locale.ROOT) in reservedPrefixes) return null

        val indices = uri.rawQuery.orEmpty().split('&')
            .filter { it.substringBefore('=') == "img_index" }
            .map { pair ->
                val value = pair.substringAfter('=', "")
                value.takeIf { carouselIndex.matches(it) }?.toIntOrNull()?.takeIf { it > 0 }
            }
        // Identical duplicate selectors are harmless; ambiguous/invalid ones are dropped.
        val index = indices.firstOrNull()?.takeIf { candidate -> indices.all { it == candidate } }
        val base = url.substringBefore('?').substringBefore('#')
        return base + (index?.let { "?img_index=$it" } ?: "")
    }
}
