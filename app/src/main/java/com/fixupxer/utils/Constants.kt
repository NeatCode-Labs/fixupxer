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


package com.fixupxer.utils

/**
 * Constants used throughout the app
 */
object Constants {
    // Donation URL
    const val DONATION_URL = "https://ko-fi.com/neatcodelabs"
    
    // Monero donation address
    const val MONERO_ADDRESS = "45TiAPismHb5TbJdX5iscCShfwQ9gSZyMcxKXjjEyabjf98dV2y8F7SHaConCAUkqUNbHuCKZk4NE4d6xpiCBRvMNPEWu1b"
    
    // Company URLs
    const val WEBSITE_URL = "https://neatcodelabs.com/"
    const val GITHUB_URL = "https://github.com/NeatCode-Labs"
    
    // Domain identifiers
    const val INSTAGRAM_DOMAIN = "instagram.com"
    // Primary proxies — embed media + post/reel title & description
    const val TOINSTAGRAM_DOMAIN = "toinstagram.com"
    const val ADAMLIKES_DOMAIN = "adamlikes.men"
    // Backup proxies — embed media only (no title/description)
    const val INSTAGRAM7_DOMAIN = "instagram7.com"
    const val KKINSTAGRAM_DOMAIN = "kkinstagram.com"
    val INSTAGRAM_PRIMARY_PROXIES = listOf(TOINSTAGRAM_DOMAIN, ADAMLIKES_DOMAIN)
    val INSTAGRAM_BACKUP_PROXIES = listOf(INSTAGRAM7_DOMAIN, KKINSTAGRAM_DOMAIN)
    // Built-in (fixed) proxies offered in the chooser, in UI order.
    // User-defined custom proxies live in InstagramProxyStore, NOT here.
    val INSTAGRAM_PROXY_DOMAINS = INSTAGRAM_PRIMARY_PROXIES + INSTAGRAM_BACKUP_PROXIES
    const val INSTAGRAM_DEFAULT_PROXY = TOINSTAGRAM_DOMAIN
    // Legacy proxies (no longer offered, but still detected so existing pasted links auto-convert)
    val INSTAGRAM_LEGACY_PROXIES = listOf("eeinstagram.com")
    const val TWITTER_DOMAIN = "twitter.com"
    const val X_DOMAIN = "x.com"
    const val FIXUPX_DOMAIN = "fixupx.com"
    const val FXTWITTER_DOMAIN = "fxtwitter.com"
    const val VXTWITTER_DOMAIN = "vxtwitter.com"
    val TWITTER_PROXY_DOMAINS = listOf(FIXUPX_DOMAIN, FXTWITTER_DOMAIN, VXTWITTER_DOMAIN)
    const val FACEBOOK_DOMAIN = "facebook.com"
    const val FB_SHORT_DOMAIN = "fb.com"
    const val FACEBOOKEZ_DOMAIN = "facebookez.com"

    // TikTok + embed proxies (v1.7.0)
    // NOTE: kktiktok.com and vxtiktok.com contain "tiktok.com" as a substring —
    // proxy checks must run before (or be combined with) plain tiktok.com checks.
    const val TIKTOK_DOMAIN = "tiktok.com"
    // Primary proxies — embed videos AND multi-image slideshows, with post stats
    const val TNKTOK_DOMAIN = "tnktok.com"
    const val TFXKTOK_DOMAIN = "tfxktok.com"
    // Backup proxies — tiktokez.com embeds media like the primaries;
    // kktiktok.com embeds the video only (no slideshows, no stats)
    const val TIKTOKEZ_DOMAIN = "tiktokez.com"
    const val KKTIKTOK_DOMAIN = "kktiktok.com"
    val TIKTOK_PRIMARY_PROXIES = listOf(TNKTOK_DOMAIN, TFXKTOK_DOMAIN)
    val TIKTOK_BACKUP_PROXIES = listOf(TIKTOKEZ_DOMAIN, KKTIKTOK_DOMAIN)
    // Built-in (fixed) proxies offered in the chooser, in UI order.
    // User-defined custom proxies live in TikTokProxyStore, NOT here.
    val TIKTOK_PROXY_DOMAINS = TIKTOK_PRIMARY_PROXIES + TIKTOK_BACKUP_PROXIES
    const val TIKTOK_DEFAULT_PROXY = TNKTOK_DOMAIN
    // Legacy proxies (dead services, but still detected so old pasted links auto-convert)
    val TIKTOK_LEGACY_PROXIES = listOf("vxtiktok.com", "tiktxk.com")

    // Other services (native-app mapping / cleaner dispatch)
    const val YOUTUBE_DOMAIN = "youtube.com"
    const val YOUTUBE_SHORT_DOMAIN = "youtu.be"
    const val REDDIT_DOMAIN = "reddit.com"
    const val REDDIT_SHORT_DOMAIN = "redd.it"
    const val LINKEDIN_DOMAIN = "linkedin.com"
    const val LINKEDIN_SHORT_DOMAIN = "lnkd.in"
    const val AMAZON_DOMAIN = "amazon.com"
    const val AMAZON_SHORT_DOMAIN = "amzn.to"
    const val SUBSTACK_DOMAIN = "substack.com"
    const val GOOGLE_DOMAIN = "google.com"
    
    // URL path identifiers
    const val TWITTER_STATUS_PATH = "/status/"
    
    // Tag for logging
    const val LOG_TAG = "FixupXer"
} 