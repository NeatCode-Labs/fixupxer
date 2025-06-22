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
    
    val otherCommon = setOf(
        "vt", "timestamp", "ts", "ir", "user_id", "session_id",
        "from_ad", "from_tiktok", "from_twitter", "from_web", "contextual_post"
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
        otherCommon
    }
} 