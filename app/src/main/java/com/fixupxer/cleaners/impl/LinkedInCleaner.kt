package com.fixupxer.cleaners.impl

import com.fixupxer.cleaners.CleanerCategory
import com.fixupxer.cleaners.UrlCleaner

/**
 * Cleaner for LinkedIn URLs - comprehensive tracking removal
 */
object LinkedInCleaner : UrlCleaner {
    override val id = "linkedin"
    override val category = CleanerCategory.SOCIAL_MEDIA
    
    // Comprehensive LinkedIn tracking parameters
    // Most extensive tracking parameter list available
    private val linkedinTracking = setOf(
        // Basic tracking
        "trk", "trkInfo", "trkEmail", "li_theme", "li_ft",
        "li_mc", "li_tc", "li_uuid", "li_at", "li_rm",
        
        // Share & referral tracking
        "share", "src", "veh", "trackingId", "refId",
        "lipi", "licu", "lici", "lict", "lics",
        
        // Analytics & attribution
        "utm_source", "utm_medium", "utm_campaign",
        "utm_term", "utm_content", "mcid", "msgConversationId",
        
        // Session & request tracking
        "sessionId", "requestId", "contextId", "csrfToken",
        "pageKey", "sessionRedirect", "fromSignIn",
        
        // Navigation tracking
        "position", "pageNum", "secureUrl", "startTask",
        "submissionId", "upsellOrderOrigin", "fromEmail",
        
        // Feed & content tracking
        "midToken", "midSig", "fromSuggestionID", "suggestedToFollow",
        "updateEntityUrn", "originTrackingId", "veh",
        
        // Notification tracking
        "msgOverlay", "messageThreadUrn", "threadUrn",
        "conversationUrn", "creatorUrn", "entityUrn",
        
        // Job & application tracking
        "alternateChannel", "alertAction", "savedSearchId",
        "searchId", "searchRequestId", "currentJobId",
        "fromJobAlertEmail", "fromJobAlertMail", "jobAlertAction",
        
        // Mobile app tracking
        "appId", "appStore", "mobileApp", "isFromMobileJob",
        "isFromMobile", "deviceType", "os", "trk_ref",
        
        // Connection & network tracking
        "invitation", "sharedKey", "sig", "invitationId",
        "invitationType", "connectionOf", "senderIdentityHint",
        
        // Groups & company tracking
        "groupId", "companyId", "organizationId", "followerId",
        "memberIdentity", "moduleKey", "detailOrigin",
        
        // Article & post tracking
        "articleId", "commentUrn", "dashCommentUrn", "replyUrn",
        "parentCommentUrn", "articleSortOrder", "sectionName",
        
        // Ads & sponsored content
        "sp", "imp", "campaign", "account", "creative",
        "sendTime", "serveTime", "displayTime", "sponsored",
        
        // Learning/Lynda tracking
        "success", "u", "e", "courseClaim", "learningHistory",
        
        // Legacy & misc parameters
        "pk", "pv", "biz", "goback", "report", "urlhash",
        "eBP", "rs", "key", "pivot", "redirSrc", "spSrc"
    )
    
    // Parameters essential for LinkedIn functionality
    private val preserveParams = setOf(
        "keywords",     // Search keywords
        "origin",       // Search origin
        "q",            // Query parameter
        "start",        // Pagination
        "count",        // Number of results
        "filters",      // Search filters
        "sortBy",       // Sort order
        "facet",        // Faceted search
        "f_*",          // Filter parameters (pattern)
        "geoUrn",       // Geographic search
        "network",      // Network filter
        "schoolFilter", // School filter
        "companySize",  // Company size filter
        "datePosted",   // Job posting date
        "jobType",      // Job type filter
        "location",     // Location parameter
        "redirectUrl",  // OAuth redirect
        "response_type", // OAuth response type
        "client_id",    // OAuth client ID
        "state",        // OAuth state
        "scope"         // OAuth scope
    )
    
    override fun matches(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("linkedin.com") ||
               lowerUrl.contains("lnkd.in") // Short links
    }
    
    override fun clean(url: String): String {
        // LinkedIn short links need to be preserved as-is (can't expand client-side)
        if (url.contains("lnkd.in")) {
            val idx = url.indexOf('?')
            return if (idx > -1) url.substring(0, idx) else url
        }
        
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
                    key.startsWith("f_") -> pair  // Keep filter parameters
                    linkedinTracking.contains(key) -> null  // Remove tracking params
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
} 