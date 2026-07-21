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

enum class ProxyPlatform {
    X,
    INSTAGRAM,
    TIKTOK,
    FACEBOOK,
    BLUESKY,
    REDDIT,
    YOUTUBE,
    PINTEREST,
    THREADS,
}

enum class FrontendRole {
    EMBED,
    READER,
    AUTOMATIC,
    EXPERIMENTAL,
}

data class FrontendTarget(
    val id: String,
    val platform: ProxyPlatform,
    val domain: String,
    val pathPrefix: String? = null,
    val role: FrontendRole,
    val allowNativeApp: Boolean,
    val communityGroup: Boolean = false,
    val sfwOnly: Boolean = false,
)

/**
 * Built-in catalog of verified alternative privacy frontends (2026-07-18 roster).
 *
 * Display order is the list order of [builtInTargets]. User-defined custom domains
 * are NOT part of this catalog — they live in [ProxyRoster].
 */
object AlternativeFrontendCatalog {

    val builtInTargets: List<FrontendTarget> = listOf(
        // ----- X / Twitter -----
        FrontendTarget(
            id = "x_fixupx",
            platform = ProxyPlatform.X,
            domain = Constants.FIXUPX_DOMAIN,
            role = FrontendRole.EMBED,
            allowNativeApp = true,
        ),
        FrontendTarget(
            id = "x_xcancel",
            platform = ProxyPlatform.X,
            domain = Constants.XCANCEL_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
        ),
        FrontendTarget(
            id = "x_nitter_net",
            platform = ProxyPlatform.X,
            domain = Constants.NITTER_NET_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
        ),
        FrontendTarget(
            id = "x_twitterviewer",
            platform = ProxyPlatform.X,
            domain = Constants.TWITTERVIEWER_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
        ),
        FrontendTarget(
            id = "x_nitter_catsarch",
            platform = ProxyPlatform.X,
            domain = Constants.NITTER_CATSARCH_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
            communityGroup = true,
        ),
        FrontendTarget(
            id = "x_nitter_tiekoetter",
            platform = ProxyPlatform.X,
            domain = Constants.NITTER_TIEKOETTER_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
            communityGroup = true,
        ),
        FrontendTarget(
            id = "x_nitter_kareem",
            platform = ProxyPlatform.X,
            domain = Constants.NITTER_KAREEM_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
            communityGroup = true,
        ),
        FrontendTarget(
            id = "x_nitter_privacyredirect",
            platform = ProxyPlatform.X,
            domain = Constants.NITTER_PRIVACYREDIRECT_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
            communityGroup = true,
        ),
        FrontendTarget(
            id = "x_nuku_trabun",
            platform = ProxyPlatform.X,
            domain = Constants.NUKU_TRABUN_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
            communityGroup = true,
        ),
        FrontendTarget(
            id = "x_twiiit",
            platform = ProxyPlatform.X,
            domain = Constants.TWIIIT_DOMAIN,
            role = FrontendRole.AUTOMATIC,
            allowNativeApp = false,
        ),
        FrontendTarget(
            id = "x_farside",
            platform = ProxyPlatform.X,
            domain = Constants.FARSIDE_DOMAIN,
            pathPrefix = "/nitter",
            role = FrontendRole.AUTOMATIC,
            allowNativeApp = false,
        ),

        // ----- Instagram -----
        FrontendTarget(
            id = "ig_toinstagram",
            platform = ProxyPlatform.INSTAGRAM,
            domain = Constants.TOINSTAGRAM_DOMAIN,
            role = FrontendRole.EMBED,
            allowNativeApp = true,
        ),
        FrontendTarget(
            id = "ig_adamlikes",
            platform = ProxyPlatform.INSTAGRAM,
            domain = Constants.ADAMLIKES_DOMAIN,
            role = FrontendRole.EMBED,
            allowNativeApp = true,
        ),
        FrontendTarget(
            id = "ig_instagram7",
            platform = ProxyPlatform.INSTAGRAM,
            domain = Constants.INSTAGRAM7_DOMAIN,
            role = FrontendRole.EMBED,
            allowNativeApp = true,
        ),
        FrontendTarget(
            id = "ig_kittygram",
            platform = ProxyPlatform.INSTAGRAM,
            domain = Constants.KITTYGRAM_DOMAIN,
            role = FrontendRole.EXPERIMENTAL,
            allowNativeApp = false,
        ),
        FrontendTarget(
            id = "ig_kg_meowing",
            platform = ProxyPlatform.INSTAGRAM,
            domain = Constants.KG_MEOWING_DOMAIN,
            role = FrontendRole.EXPERIMENTAL,
            allowNativeApp = false,
        ),
        FrontendTarget(
            id = "ig_kittygram_kareem",
            platform = ProxyPlatform.INSTAGRAM,
            domain = Constants.KITTYGRAM_KAREEM_DOMAIN,
            role = FrontendRole.EXPERIMENTAL,
            allowNativeApp = false,
        ),

        // ----- TikTok -----
        FrontendTarget(
            id = "tk_tnktok",
            platform = ProxyPlatform.TIKTOK,
            domain = Constants.TNKTOK_DOMAIN,
            role = FrontendRole.EMBED,
            allowNativeApp = true,
        ),
        FrontendTarget(
            id = "tk_tfxktok",
            platform = ProxyPlatform.TIKTOK,
            domain = Constants.TFXKTOK_DOMAIN,
            role = FrontendRole.EMBED,
            allowNativeApp = true,
        ),
        FrontendTarget(
            id = "tk_tiktokez",
            platform = ProxyPlatform.TIKTOK,
            domain = Constants.TIKTOKEZ_DOMAIN,
            role = FrontendRole.EMBED,
            allowNativeApp = true,
        ),
        FrontendTarget(
            id = "tk_kktiktok",
            platform = ProxyPlatform.TIKTOK,
            domain = Constants.KKTIKTOK_DOMAIN,
            role = FrontendRole.EMBED,
            allowNativeApp = true,
        ),

        // ----- Bluesky -----
        FrontendTarget(
            id = "bs_fxbsky",
            platform = ProxyPlatform.BLUESKY,
            domain = Constants.FXBSKY_DOMAIN,
            role = FrontendRole.EMBED,
            allowNativeApp = true,
        ),
        FrontendTarget(
            id = "bs_skylib_coffee",
            platform = ProxyPlatform.BLUESKY,
            domain = Constants.SKYLIB_COFFEE_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
        ),
        FrontendTarget(
            id = "bs_skylib_catsarch",
            platform = ProxyPlatform.BLUESKY,
            domain = Constants.SKYLIB_CATSARCH_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
        ),

        // ----- Reddit -----
        FrontendTarget(
            id = "rd_redlib_catsarch",
            platform = ProxyPlatform.REDDIT,
            domain = Constants.REDLIB_CATSARCH_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
        ),
        FrontendTarget(
            id = "rd_redlib_privacyredirect",
            platform = ProxyPlatform.REDDIT,
            domain = Constants.REDLIB_PRIVACYREDIRECT_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
        ),
        FrontendTarget(
            id = "rd_redlib_nadeko",
            platform = ProxyPlatform.REDDIT,
            domain = Constants.REDLIB_NADEKO_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
        ),
        FrontendTarget(
            id = "rd_redlib_privadency",
            platform = ProxyPlatform.REDDIT,
            domain = Constants.REDLIB_PRIVADENCY_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
        ),
        FrontendTarget(
            id = "rd_safereddit",
            platform = ProxyPlatform.REDDIT,
            domain = Constants.SAFEREDDIT_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
            sfwOnly = true,
        ),
        FrontendTarget(
            id = "rd_red_artemislena",
            platform = ProxyPlatform.REDDIT,
            domain = Constants.RED_ARTEMISLENA_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
            communityGroup = true,
        ),
        FrontendTarget(
            id = "rd_redlib_r4fo",
            platform = ProxyPlatform.REDDIT,
            domain = Constants.REDLIB_R4FO_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
            communityGroup = true,
        ),
        FrontendTarget(
            id = "rd_redlib_cow",
            platform = ProxyPlatform.REDDIT,
            domain = Constants.REDLIB_COW_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
            communityGroup = true,
        ),

        // ----- YouTube -----
        FrontendTarget(
            id = "yt_inv_nadeko",
            platform = ProxyPlatform.YOUTUBE,
            domain = Constants.INV_NADEKO_DOMAIN,
            role = FrontendRole.EXPERIMENTAL,
            allowNativeApp = false,
        ),
        FrontendTarget(
            id = "yt_invidious_nerdvpn",
            platform = ProxyPlatform.YOUTUBE,
            domain = Constants.INVIDIOUS_NERDVPN_DOMAIN,
            role = FrontendRole.EXPERIMENTAL,
            allowNativeApp = false,
        ),

        // ----- Pinterest -----
        FrontendTarget(
            id = "pt_pinterest_bunk",
            platform = ProxyPlatform.PINTEREST,
            domain = Constants.PINTEREST_BUNK_DOMAIN,
            role = FrontendRole.READER,
            allowNativeApp = false,
        ),

        // ----- Threads -----
        FrontendTarget(
            id = "th_shoelace_mint",
            platform = ProxyPlatform.THREADS,
            domain = Constants.SHOELACE_MINT_DOMAIN,
            role = FrontendRole.EXPERIMENTAL,
            allowNativeApp = false,
        ),
    )

    private val defaultTargetIds: Map<ProxyPlatform, String> = mapOf(
        ProxyPlatform.X to "x_fixupx",
        ProxyPlatform.INSTAGRAM to "ig_toinstagram",
        ProxyPlatform.TIKTOK to "tk_tnktok",
        ProxyPlatform.BLUESKY to "bs_fxbsky",
        ProxyPlatform.REDDIT to "rd_redlib_catsarch",
        ProxyPlatform.YOUTUBE to "yt_inv_nadeko",
        ProxyPlatform.PINTEREST to "pt_pinterest_bunk",
        ProxyPlatform.THREADS to "th_shoelace_mint",
    )

    private val legacyDomainsByPlatform: Map<ProxyPlatform, List<String>> = mapOf(
        ProxyPlatform.X to listOf(Constants.FXTWITTER_DOMAIN, Constants.VXTWITTER_DOMAIN),
        ProxyPlatform.INSTAGRAM to Constants.INSTAGRAM_LEGACY_PROXIES,
        ProxyPlatform.TIKTOK to Constants.TIKTOK_LEGACY_PROXIES,
        ProxyPlatform.FACEBOOK to emptyList(),
        ProxyPlatform.BLUESKY to emptyList(),
        ProxyPlatform.REDDIT to emptyList(),
        ProxyPlatform.YOUTUBE to emptyList(),
        ProxyPlatform.PINTEREST to emptyList(),
        ProxyPlatform.THREADS to emptyList(),
    )

    private val sourceDomainsByPlatform: Map<ProxyPlatform, List<String>> = mapOf(
        ProxyPlatform.X to listOf(Constants.TWITTER_DOMAIN, Constants.X_DOMAIN),
        ProxyPlatform.INSTAGRAM to listOf(Constants.INSTAGRAM_DOMAIN),
        ProxyPlatform.TIKTOK to listOf(Constants.TIKTOK_DOMAIN),
        ProxyPlatform.FACEBOOK to listOf(Constants.FACEBOOK_DOMAIN, Constants.FB_SHORT_DOMAIN),
        ProxyPlatform.BLUESKY to listOf(Constants.BLUESKY_DOMAIN),
        ProxyPlatform.REDDIT to listOf(Constants.REDDIT_DOMAIN, Constants.REDDIT_SHORT_DOMAIN),
        ProxyPlatform.YOUTUBE to listOf(Constants.YOUTUBE_DOMAIN, Constants.YOUTUBE_SHORT_DOMAIN),
        ProxyPlatform.PINTEREST to listOf(Constants.PINTEREST_DOMAIN),
        ProxyPlatform.THREADS to listOf(Constants.THREADS_NET_DOMAIN, Constants.THREADS_COM_DOMAIN),
    )

    fun builtIn(platform: ProxyPlatform): List<FrontendTarget> =
        builtInTargets.filter { it.platform == platform }

    fun builtInReaders(platform: ProxyPlatform): List<FrontendTarget> =
        builtIn(platform).filter { it.role == FrontendRole.READER }

    fun privacyCapablePlatforms(): List<ProxyPlatform> =
        ProxyPlatform.entries.filter { builtInReaders(it).isNotEmpty() }

    fun byId(id: String): FrontendTarget? = builtInTargets.find { it.id == id }

    fun byDomain(platform: ProxyPlatform, domain: String): FrontendTarget? =
        builtIn(platform).find { it.domain == domain }

    fun defaultTargetId(platform: ProxyPlatform): String? = defaultTargetIds[platform]

    fun defaultTarget(platform: ProxyPlatform): FrontendTarget? =
        defaultTargetId(platform)?.let { byId(it) }

    fun legacyDomains(platform: ProxyPlatform): List<String> =
        legacyDomainsByPlatform.getValue(platform)

    fun sourceDomains(platform: ProxyPlatform): List<String> =
        sourceDomainsByPlatform.getValue(platform)

    /** Every built-in domain across all platforms (for cross-platform uniqueness checks). */
    fun allBuiltInDomains(): List<String> = builtInTargets.map { it.domain }
}
