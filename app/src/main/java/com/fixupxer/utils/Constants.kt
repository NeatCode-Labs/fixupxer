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
    const val GITHUB_REPOSITORY_URL = "$GITHUB_URL/fixupxer"
    const val RELEASE_NOTES_URL = "$GITHUB_REPOSITORY_URL/blob/main/docs/RELEASE_NOTES.md"
    private const val BUG_REPORT_EMAIL_LOCAL_PART = "neatcodelabs"
    private const val BUG_REPORT_EMAIL_DOMAIN = "gmail.com"
    val BUG_REPORT_EMAIL = "$BUG_REPORT_EMAIL_LOCAL_PART@$BUG_REPORT_EMAIL_DOMAIN"
    const val BROWSER_MODE_GUIDE_URL =
        "$GITHUB_REPOSITORY_URL/blob/main/docs/BROWSER_MODE_GUIDE.md"
    const val CUSTOM_RULES_GUIDE_URL =
        "$GITHUB_REPOSITORY_URL/blob/main/docs/CUSTOM_URL_RULES_GUIDE.md"
    
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
    const val FACEBOOK_LINK_SHIM_DOMAIN = "l.facebook.com"
    const val FACEBOOK_MOBILE_LINK_SHIM_DOMAIN = "lm.facebook.com"
    const val BLUESKY_DOMAIN = "bsky.app"
    const val FXBSKY_DOMAIN = "fxbsky.app"
    const val BLUESKY_GO_DOMAIN = "go.bsky.app"

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
    const val GOOGLE_MAPS_DOMAIN = "maps.google.com"
    const val GOOGLE_ADSERVICES_DOMAIN = "googleadservices.com"
    const val GOOGLE_STORE_DOMAIN = "store.google.com"
    const val REDDITMAIL_CLICK_DOMAIN = "click.redditmail.com"
    const val WIKIPEDIA_DOMAIN = "wikipedia.org"
    const val THREADS_NET_DOMAIN = "threads.net"
    const val THREADS_COM_DOMAIN = "threads.com"
    const val TWITCH_DOMAIN = "twitch.tv"
    const val SPOTIFY_DOMAIN = "spotify.com"
    const val PINTEREST_DOMAIN = "pinterest.com"
    const val SNAPCHAT_DOMAIN = "snapchat.com"
    const val WHATSAPP_DOMAIN = "whatsapp.com"
    const val MEDIUM_DOMAIN = "medium.com"
    const val BING_DOMAIN = "bing.com"
    const val DUCKDUCKGO_DOMAIN = "duckduckgo.com"
    const val EBAY_COM_DOMAIN = "ebay.com"
    const val EBAY_CO_UK_DOMAIN = "ebay.co.uk"
    const val EBAY_DE_DOMAIN = "ebay.de"
    const val NETFLIX_DOMAIN = "netflix.com"
    const val ALIEXPRESS_DOMAIN = "aliexpress.com"
    const val BILIBILI_DOMAIN = "bilibili.com"
    const val GEORIOT_TARGET_DOMAIN = "target.georiot.com"
    const val LINKSYNERGY_CLICK_DOMAIN = "click.linksynergy.com"
    
    // URL path identifiers
    const val TWITTER_STATUS_PATH = "/status/"

    // Custom URL rule engine resource limits
    const val MAX_INPUT_LENGTH = 2048
    const val MAX_URL_SCAN_LENGTH = 1000
    const val MAX_URL_LENGTH = 2048
    const val MAX_CUSTOM_RULES = 200
    const val MAX_RULE_PATTERN_LENGTH = 2048
    const val MAX_RULE_REPLACEMENT_LENGTH = 4096
    const val MAX_RULE_TEMPLATE_LENGTH = 4096
    const val MAX_SCOPE_ENTRIES = 100
    const val MAX_EXCLUDES_PER_RULE = 50
    const val MAX_TEST_VECTORS_PER_RULE = 20
    const val MAX_REGEX_PROGRAM_SIZE = 10_000
    const val MAX_PIPELINE_REENTRIES = 5
    const val MAX_TRACE_STEPS = 1000
    const val MAX_CHANGE_OPERATIONS = 20
    const val MAX_RULE_BUNDLE_BYTES = 1_048_576
    const val MAX_RULE_SNAPSHOTS = 3

    // Private Link Guard: high-confidence literal URL leak indicators only.
    const val LEAK_MIN_TOKEN_VALUE_LENGTH = 8
    const val MAX_LEAK_FINDINGS = 10

    val LEAK_EMAIL_PATTERN = Regex(
        "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
    )
    val LEAK_JWT_PATTERN = Regex(
        "eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"
    )
    val LEAK_PRECISE_COORDINATE_PATTERN = Regex(
        "[-+]?\\d+\\.\\d{4,}"
    )
    val LEAK_SENSITIVE_PARAM_NAMES = setOf(
        "access_token",
        "refresh_token",
        "id_token",
        "token",
        "auth",
        "authorization",
        "auth_token",
        "otp",
        "one_time_code",
        "reset",
        "reset_token",
        "password_reset_token",
        "invite",
        "invite_code",
        "invitation_token",
        "api_key",
        "apikey",
        "secret",
        "client_secret",
        "session",
        "session_id",
        "session_token",
        "sessionid",
        "signature",
        "sig",
        "private_token",
        "share_token",
        "confirmation_token",
        "unlock_token",
        "recovery_token",
        "bearer"
    )
    /*
     * Deliberately excluded: "code" (frequently a discount/region/article code),
     * "key" (too generic), and "sid" (ambiguous service/session identifier).
     * The guard favours high confidence over broad token detection.
     */
    val LEAK_LATITUDE_PARAM_NAMES = setOf("lat", "latitude")
    val LEAK_LONGITUDE_PARAM_NAMES = setOf("lon", "lng", "longitude")
    
    // Tag for logging
    const val LOG_TAG = "FixupXer"
} 