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

import com.fixupxer.utils.Constants
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
        val domain = extractDomain(url)?.lowercase()
        
        // First try domain-specific lookup
        domain?.let { 
            domainMap[it]?.let { cleaners ->
                return cleaners.filter { cleaner ->
                    cleaner.matches(url)
                }
            }
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
            "youtube" -> {
                addDomainAssociation(Constants.YOUTUBE_DOMAIN, cleaner)
                addDomainAssociation(Constants.YOUTUBE_SHORT_DOMAIN, cleaner)
                addDomainAssociation("m.${Constants.YOUTUBE_DOMAIN}", cleaner)
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
            }
            "twitter" -> {
                addDomainAssociation(Constants.TWITTER_DOMAIN, cleaner)
                addDomainAssociation(Constants.X_DOMAIN, cleaner)
                Constants.TWITTER_PROXY_DOMAINS.forEach { addDomainAssociation(it, cleaner) }
            }
            "instagram" -> {
                // Fixed proxies only — custom proxies are added at runtime, and
                // getCleanersFor() falls back to a full matches() scan for domains
                // not present in this map, so they are still handled correctly.
                addDomainAssociation(Constants.INSTAGRAM_DOMAIN, cleaner)
                Constants.INSTAGRAM_PROXY_DOMAINS.forEach { addDomainAssociation(it, cleaner) }
                Constants.INSTAGRAM_LEGACY_PROXIES.forEach { addDomainAssociation(it, cleaner) }
            }
            "tiktok" -> {
                addDomainAssociation(Constants.TIKTOK_DOMAIN, cleaner)
                addDomainAssociation("vm.${Constants.TIKTOK_DOMAIN}", cleaner)
                addDomainAssociation("m.${Constants.TIKTOK_DOMAIN}", cleaner)
            }
            "linkedin" -> {
                addDomainAssociation(Constants.LINKEDIN_DOMAIN, cleaner)
                addDomainAssociation(Constants.LINKEDIN_SHORT_DOMAIN, cleaner)
            }
            "substack" -> {
                addDomainAssociation(Constants.SUBSTACK_DOMAIN, cleaner)
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
    
    private fun extractDomain(url: String): String? {
        return try {
            val withoutProtocol = url
                .removePrefix("https://")
                .removePrefix("http://")
            
            val domainEnd = withoutProtocol.indexOfAny(charArrayOf('/', '?', '#', ':'))
            val domain = if (domainEnd > 0) {
                withoutProtocol.substring(0, domainEnd)
            } else {
                withoutProtocol
            }
            
            // Remove www. prefix for better matching
            domain.removePrefix("www.")
        } catch (e: Exception) {
            null
        }
    }
} 