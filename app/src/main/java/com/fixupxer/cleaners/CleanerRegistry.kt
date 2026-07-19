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


package com.fixupxer.cleaners

import com.fixupxer.cleaners.impl.CatalogParameterCleaner
import com.fixupxer.processing.UrlNormalizer
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry for managing URL cleaners with efficient domain-based dispatch
 */
@Singleton
class CleanerRegistry @Inject constructor() {
    // Domain → Cleaners mapping for O(1) lookup
    private val domainMap = ConcurrentHashMap<String, MutableList<UrlCleaner>>()
    
    // All registered cleaners
    private val allCleaners = mutableListOf<UrlCleaner>()
    
    /**
     * Register a new cleaner
     */
    fun register(cleaner: UrlCleaner) {
        synchronized(allCleaners) {
            allCleaners.add(cleaner)
            // Pre-compute domain associations if possible
            precomputeDomainAssociations(cleaner)
        }
    }
    
    /**
     * Register multiple cleaners at once
     */
    fun registerAll(cleaners: List<UrlCleaner>) {
        cleaners.forEach { register(it) }
    }
    
    /**
     * Get cleaners that can handle the given URL
     */
    fun getCleanersFor(url: String): List<UrlCleaner> {
        val host = UrlNormalizer.extractAsciiHost(url)

        var candidate = host
        val domainSpecific = mutableListOf<UrlCleaner>()
        while (!candidate.isNullOrBlank()) {
            domainMap[candidate]
                ?.filter { it.matches(url) }
                ?.let(domainSpecific::addAll)
            candidate = candidate.substringAfter('.', missingDelimiterValue = "")
        }

        if (domainSpecific.isNotEmpty()) {
            val generalCleaners = allCleaners.filter {
                it.category == CleanerCategory.GENERAL && it.matches(url)
            }
            return (domainSpecific + generalCleaners).distinct()
        }

        // Fallback to checking all cleaners
        return allCleaners.filter { cleaner ->
            cleaner.matches(url)
        }
    }
    
    /**
     * Get all registered cleaners
     */
    fun getAllCleaners(): List<UrlCleaner> = allCleaners.toList()
    
    /**
     * Pre-compute domain associations for efficient lookup
     */
    private fun precomputeDomainAssociations(cleaner: UrlCleaner) {
        if (cleaner is CatalogParameterCleaner) {
            cleaner.rule.domains.forEach { addDomainAssociation(it, cleaner) }
            return
        }

        // For known domain-specific cleaners, pre-populate the domain map
        // This is a simple heuristic based on common patterns
        when (cleaner.id) {
            "amazon" -> {
                addDomainAssociation(Constants.AMAZON_DOMAIN, cleaner)
                addDomainAssociation(Constants.AMAZON_SHORT_DOMAIN, cleaner)
            }
            "google_search" -> {
                addDomainAssociation(Constants.GOOGLE_DOMAIN, cleaner)
                addDomainAssociation("google.co.uk", cleaner)
                addDomainAssociation("google.de", cleaner)
                // Add more Google domains as needed
            }
            "google_maps" -> {
                addDomainAssociation(Constants.GOOGLE_MAPS_DOMAIN, cleaner)
                addDomainAssociation(Constants.GOOGLE_DOMAIN, cleaner)
            }
            "youtube" -> {
                addDomainAssociation(Constants.YOUTUBE_DOMAIN, cleaner)
                addDomainAssociation(Constants.YOUTUBE_SHORT_DOMAIN, cleaner)
                addDomainAssociation("m.${Constants.YOUTUBE_DOMAIN}", cleaner)
                addDomainAssociation("youtube-nocookie.com", cleaner)
                ProxyRoster.allKnownDomains(ProxyPlatform.YOUTUBE).forEach {
                    addDomainAssociation(it, cleaner)
                }
            }
            "facebook" -> {
                addDomainAssociation(Constants.FACEBOOK_DOMAIN, cleaner)
                addDomainAssociation("m.${Constants.FACEBOOK_DOMAIN}", cleaner)
                addDomainAssociation(Constants.FB_SHORT_DOMAIN, cleaner)
                addDomainAssociation(Constants.FACEBOOKEZ_DOMAIN, cleaner)
            }
            "reddit" -> {
                addDomainAssociation(Constants.REDDIT_DOMAIN, cleaner)
                addDomainAssociation("old.${Constants.REDDIT_DOMAIN}", cleaner)
                addDomainAssociation("new.${Constants.REDDIT_DOMAIN}", cleaner)
                addDomainAssociation(Constants.REDDIT_SHORT_DOMAIN, cleaner)
                ProxyRoster.allKnownDomains(ProxyPlatform.REDDIT).forEach {
                    addDomainAssociation(it, cleaner)
                }
            }
            "twitter" -> {
                addDomainAssociation(Constants.TWITTER_DOMAIN, cleaner)
                addDomainAssociation(Constants.X_DOMAIN, cleaner)
                ProxyRoster.allKnownDomains(ProxyPlatform.X).forEach { addDomainAssociation(it, cleaner) }
            }
            "instagram" -> {
                addDomainAssociation(Constants.INSTAGRAM_DOMAIN, cleaner)
                ProxyRoster.allKnownDomains(ProxyPlatform.INSTAGRAM).forEach {
                    addDomainAssociation(it, cleaner)
                }
            }
            "tiktok" -> {
                addDomainAssociation(Constants.TIKTOK_DOMAIN, cleaner)
                addDomainAssociation("vm.${Constants.TIKTOK_DOMAIN}", cleaner)
                addDomainAssociation("m.${Constants.TIKTOK_DOMAIN}", cleaner)
                ProxyRoster.allKnownDomains(ProxyPlatform.TIKTOK).forEach {
                    addDomainAssociation(it, cleaner)
                }
            }
            "linkedin" -> {
                addDomainAssociation(Constants.LINKEDIN_DOMAIN, cleaner)
                addDomainAssociation(Constants.LINKEDIN_SHORT_DOMAIN, cleaner)
            }
            "substack" -> {
                addDomainAssociation(Constants.SUBSTACK_DOMAIN, cleaner)
            }
            "offline_redirect" -> {
                addDomainAssociation(Constants.FACEBOOK_LINK_SHIM_DOMAIN, cleaner)
                addDomainAssociation(Constants.FACEBOOK_MOBILE_LINK_SHIM_DOMAIN, cleaner)
                addDomainAssociation(Constants.LINKEDIN_DOMAIN, cleaner)
                addDomainAssociation(Constants.YOUTUBE_DOMAIN, cleaner)
                addDomainAssociation(Constants.BLUESKY_GO_DOMAIN, cleaner)
                addDomainAssociation(Constants.GOOGLE_ADSERVICES_DOMAIN, cleaner)
                addDomainAssociation(Constants.REDDITMAIL_CLICK_DOMAIN, cleaner)
                addDomainAssociation(Constants.GEORIOT_TARGET_DOMAIN, cleaner)
                addDomainAssociation(Constants.LINKSYNERGY_CLICK_DOMAIN, cleaner)
            }
        }
    }
    
    private fun addDomainAssociation(domain: String, cleaner: UrlCleaner) {
        val key = domain.lowercase()
        synchronized(domainMap) {
            val list = domainMap[key] ?: mutableListOf<UrlCleaner>().also { domainMap[key] = it }
            list.add(cleaner)
        }
    }
} 