package com.fixupxer.data.config

/**
 * Configuration for tracking parameters to remove from URLs
 * Organized by category for easier maintenance
 */
object TrackingParameters {
    
    val utmParameters = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "utm_reader", "utm_name", "utm_referrer", "utm_social", 
        "utm_social-type", "utm_brand", "utm_cid", "utm_viz_id", "utm_pubreferrer"
    )
    
    val socialMediaTracking = setOf(
        "fbclid", "gclid", "dclid", "msclkid", "twclid"
    )
    
    val analyticsTracking = setOf(
        "ga_source", "ga_medium", "ga_term", "ga_content", "ga_campaign", "ga_place",
        "yclid", "ml_subscriber", "ml_subscriber_hash", "_openstat"
    )
    
    val ecommerceTracking = setOf(
        "mc_cid", "mc_eid", "mc_tc"
    )
    
    val socialSharing = setOf(
        "share_id", "igshid", "igsh", "ref", "referrer", "source", "source_platform",
        "si", "__a", "__d", "_rdr", "hl"
    )
    
    val advertisingTracking = setOf(
        "fbadid", "campaign_id", "ad_id", "ad_set_id", "adset_id",
        "placement", "creative", "keyword", "partner", "sk", "sc_"
    )
    
    val instagramSpecific = setOf(
        "ig_cache_key", "ig_mid", "ig_share_sheet"
    )
    
    val facebookSpecific = setOf(
        "__cft__", "__tn__", "_branch_match_id", "epa", "_gl", 
        "mibextid", "fb_action_ids", "fb_action_types", "fb_source", "fb_ref",
        "fb_comment_id", "fb_story_location", "fb_dtsg_ag", "fbid"
    )
    
    val twitterSpecific = setOf(
        "s", "t", "ref_src", "ref_url", "via"
    )
    
    val redditSpecific = setOf(
        "context", "correlator", "feature", "rdt_cid"
    )
    
    val amazonSpecific = setOf(
        "tag", "linkcode", "camp", "creative", "ascsubtag", "ref_"
    )
    
    val substackSpecific = setOf(
        "token", "r", "isFreemail"
    )
    
    val youtubeSpecific = setOf(
        "feature", "pp", "si", "embeds_referring_euri", "embeds_referring_origin",
        "source_ve_path", "embeds_euri", "app", "attribution_link", "ytclid"
    )
    
    val tiktokSpecific = setOf(
        "_t", "_r", "checksum", "sec_uid", "share_app_id", "share_link_id",
        "tt_from", "u_code", "user_id", "preview_pb", "language", "timestamp",
        "utm_source", "utm_campaign", "utm_medium", "aid", "iid"
    )
    
    val linkedinSpecific = setOf(
        "trackingId", "lipi", "licu", "trk", "trkEmail", "trkInfo",
        "midToken", "midSig", "eBP", "tscp", "refId", "veh"
    )
    
    val pinterestSpecific = setOf(
        "e_t", "e_t_s", "e_t_cs", "ouuid", "cid", "sfo", "sfo_s",
        "nic", "nic_v", "pin_unauth", "dpi", "utm_source", "i", "w", "m", "n"
    )
    
    val spotifySpecific = setOf(
        "si", "utm_source", "utm_medium", "utm_campaign", "utm_content",
        "dl_branch", "dl_mobileapp", "go", "nd", "uri"
    )
    
    val mediumSpecific = setOf(
        "source", "sk", "source_user_id", "source_post_link", 
        "utm_source", "utm_medium", "utm_campaign"
    )
    
    val discordSpecific = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_content"
    )
    
    val telegramSpecific = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_content",
        "tgme", "start", "startgroup", "game", "voicechat"
    )
    
    val whatsappSpecific = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_content",
        "app_absent", "link_medium", "link_source"
    )
    
    val snapchatSpecific = setOf(
        "share_id", "locale", "attachment_url", "utm_source",
        "utm_campaign", "utm_medium", "utm_content", "utm_term"
    )
    
    val aliexpressSpecific = setOf(
        "spm", "scm", "scm_id", "scm-url", "pvid", "algo_expid",
        "algo_pvid", "ns", "abbucket", "acm", "utparam", "pos",
        "cv", "af", "mall_affr", "sk", "dp", "terminal_id",
        "aff_fcid", "aff_fsk", "aff_platform", "aff_trace_key",
        "shareId", "platform", "businessType", "title", "srcSns"
    )
    
    val ebaySpecific = setOf(
        "mkrid", "siteid", "mkcid", "mkevt", "mkpid", "trksid",
        "campid", "toolid", "customid", "amdata", "var", "selected"
    )
    
    val shopifySpecific = setOf(
        "variant", "utm_source", "utm_medium", "utm_campaign",
        "utm_content", "utm_term", "omnisendContactID", "sca_ref",
        "mc_cid", "mc_eid", "_pos", "_sid", "_ss", "_s", "_shopify_s",
        "_shopify_sa_t", "_shopify_sa_p", "_shopify_y", "cart_sig"
    )
    
    val netflixSpecific = setOf(
        "trackId", "tctx", "jb", "jbv", "dgs"
    )
    
    val twitchSpecific = setOf(
        "tt_medium", "tt_content", "utm_source", "utm_medium",
        "utm_campaign", "utm_content", "utm_term"
    )
    
    val githubSpecific = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_content",
        "email_token", "email_source"
    )
    
    val stackoverflowSpecific = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_content",
        "rq", "rv", "lq", "md"
    )
    
    val bingSpecific = setOf(
        "cvid", "FORM", "qpvt", "qs", "sc", "sp", "sk", "ghsh", "ghacc",
        "ghpl", "ghpr", "ghc", "ghhc", "ghpos"
    )
    
    val duckduckgoSpecific = setOf(
        "t", "ia", "iax", "atb", "v7-it"
    )
    
    val otherCommon = setOf(
        "vt", "timestamp", "ts", "ir", "user_id", "session_id",
        "from_ad", "from_tiktok", "from_twitter", "from_web", "contextual_post",
        "ref", "ref_src", "ref_url", "share", "shareid", "_share", "ref_sdk", "lr_ck"
    )
    
    /**
     * Get all tracking parameters as a single set
     */
    val allParameters: Set<String> by lazy {
        utmParameters +
        socialMediaTracking +
        analyticsTracking +
        ecommerceTracking +
        socialSharing +
        advertisingTracking +
        instagramSpecific +
        facebookSpecific +
        twitterSpecific +
        redditSpecific +
        amazonSpecific +
        substackSpecific +
        youtubeSpecific +
        tiktokSpecific +
        linkedinSpecific +
        pinterestSpecific +
        spotifySpecific +
        mediumSpecific +
        discordSpecific +
        telegramSpecific +
        whatsappSpecific +
        snapchatSpecific +
        aliexpressSpecific +
        ebaySpecific +
        shopifySpecific +
        netflixSpecific +
        twitchSpecific +
        githubSpecific +
        stackoverflowSpecific +
        bingSpecific +
        duckduckgoSpecific +
        otherCommon
    }
} 