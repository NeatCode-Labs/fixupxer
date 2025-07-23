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
            "amazon" -> addDomainAssociation("amazon.com", cleaner)
            "google_search" -> {
                addDomainAssociation("google.com", cleaner)
                addDomainAssociation("google.co.uk", cleaner)
                addDomainAssociation("google.de", cleaner)
                // Add more Google domains as needed
            }
            "youtube" -> {
                addDomainAssociation("youtube.com", cleaner)
                addDomainAssociation("youtu.be", cleaner)
                addDomainAssociation("m.youtube.com", cleaner)
            }
            "facebook" -> {
                addDomainAssociation("facebook.com", cleaner)
                addDomainAssociation("m.facebook.com", cleaner)
                addDomainAssociation("fb.com", cleaner)
            }
            "reddit" -> {
                addDomainAssociation("reddit.com", cleaner)
                addDomainAssociation("old.reddit.com", cleaner)
                addDomainAssociation("new.reddit.com", cleaner)
                addDomainAssociation("redd.it", cleaner)
            }
            "twitter" -> {
                addDomainAssociation("twitter.com", cleaner)
                addDomainAssociation("x.com", cleaner)
            }
            "instagram" -> {
                addDomainAssociation("instagram.com", cleaner)
                addDomainAssociation("www.instagram.com", cleaner)
            }
            "tiktok" -> {
                addDomainAssociation("tiktok.com", cleaner)
                addDomainAssociation("vm.tiktok.com", cleaner)
                addDomainAssociation("m.tiktok.com", cleaner)
            }
            "linkedin" -> {
                addDomainAssociation("linkedin.com", cleaner)
                addDomainAssociation("lnkd.in", cleaner)
            }
            "substack" -> {
                addDomainAssociation("substack.com", cleaner)
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