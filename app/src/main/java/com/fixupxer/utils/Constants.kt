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
    // Active proxies. UI lists them in the order: toinstagram.com, adamlikes.men, instagram7.com.
    // Primary proxies — embed media + post/reel title & description
    const val TOINSTAGRAM_DOMAIN = "toinstagram.com"
    const val ADAMLIKES_DOMAIN = "adamlikes.men"
    // Backup proxy — embeds media only (no title/description)
    const val INSTAGRAM7_DOMAIN = "instagram7.com"
    val INSTAGRAM_PROXY_DOMAINS = listOf(TOINSTAGRAM_DOMAIN, ADAMLIKES_DOMAIN, INSTAGRAM7_DOMAIN)
    val INSTAGRAM_PRIMARY_PROXIES = listOf(TOINSTAGRAM_DOMAIN, ADAMLIKES_DOMAIN)
    const val INSTAGRAM_BACKUP_PROXY = INSTAGRAM7_DOMAIN
    const val INSTAGRAM_DEFAULT_PROXY = TOINSTAGRAM_DOMAIN
    // Legacy proxies (no longer offered, but still detected so existing pasted links auto-convert)
    val INSTAGRAM_LEGACY_PROXIES = listOf("kkinstagram.com", "eeinstagram.com")
    val INSTAGRAM_ALL_KNOWN_PROXIES = INSTAGRAM_PROXY_DOMAINS + INSTAGRAM_LEGACY_PROXIES
    const val TWITTER_DOMAIN = "twitter.com"
    const val X_DOMAIN = "x.com"
    const val FIXUPX_DOMAIN = "fixupx.com"
    const val FXTWITTER_DOMAIN = "fxtwitter.com"
    const val VXTWITTER_DOMAIN = "vxtwitter.com"
    const val FACEBOOK_DOMAIN = "facebook.com"
    const val FACEBOOKEZ_DOMAIN = "facebookez.com"
    const val TIKTOK_DOMAIN = "tiktok.com"
    const val KKTIKTOK_DOMAIN = "kktiktok.com"
    
    // URL path identifiers
    const val TWITTER_STATUS_PATH = "/status/"
    
    // Tag for logging
    const val LOG_TAG = "FixupXer"
} 