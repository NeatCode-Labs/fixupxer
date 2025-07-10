package com.fixupxer.cleaners.utils

/**
 * Utility functions for URL cleaners
 */
object CleanerUtils {
    
    /**
     * Fix common URL malformations before processing
     * 
     * @param url The URL to preprocess
     * @return Fixed URL
     */
    fun preprocessUrl(url: String): String {
        var fixedUrl = url
        
        // Fix URLs with & but no ? (common when query string is incorrectly formed)
        // Example: https://example.com&param=value -> https://example.com?param=value
        if (fixedUrl.contains('&') && !fixedUrl.contains('?')) {
            val firstAmpIndex = fixedUrl.indexOf('&')
            if (firstAmpIndex > 0) {
                // Check if this is after the domain/path part
                val beforeAmp = fixedUrl.substring(0, firstAmpIndex)
                if (beforeAmp.contains("://") && !beforeAmp.contains('#')) {
                    // Replace first & with ?
                    fixedUrl = fixedUrl.substring(0, firstAmpIndex) + '?' + fixedUrl.substring(firstAmpIndex + 1)
                }
            }
        }
        
        return fixedUrl
    }
    
    /**
     * Post-process cleaned URL to fix common issues
     * 
     * @param url The cleaned URL
     * @return Post-processed URL
     */
    fun postProcessUrl(url: String): String {
        var result = url
        
        // Remove trailing delimiters
        result = result.trimEnd('?', '&', '#')
        
        // Remove empty fragments (e.g., https://example.com#)
        if (result.endsWith("#")) {
            result = result.substring(0, result.length - 1)
        }
        
        // Remove empty query strings (e.g., https://example.com?)
        if (result.endsWith("?")) {
            result = result.substring(0, result.length - 1)
        }
        
        // Fix double slashes in path (but not in protocol)
        val protocolEnd = result.indexOf("://")
        if (protocolEnd > 0) {
            val afterProtocol = result.substring(protocolEnd + 3)
            val fixedPath = afterProtocol.replace(Regex("//+"), "/")
            result = result.substring(0, protocolEnd + 3) + fixedPath
        }
        
        return result
    }
    
    /**
     * Split URL into base, query, and fragment parts
     * Handles edge cases like missing ? before parameters
     * 
     * @param url The URL to split
     * @return Triple of (base, query, fragment)
     */
    fun splitUrl(url: String): Triple<String, String, String> {
        // Preprocess to fix common issues
        val fixedUrl = preprocessUrl(url)
        
        // Find query start
        val queryStart = fixedUrl.indexOf('?')
        if (queryStart == -1) {
            // No query parameters
            val fragmentStart = fixedUrl.indexOf('#')
            return if (fragmentStart == -1) {
                Triple(fixedUrl, "", "")
            } else {
                Triple(
                    fixedUrl.substring(0, fragmentStart),
                    "",
                    fixedUrl.substring(fragmentStart)
                )
            }
        }
        
        val base = fixedUrl.substring(0, queryStart)
        val queryAndFragment = fixedUrl.substring(queryStart + 1)
        
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
        
        return Triple(base, query, fragment)
    }
    
    /**
     * Rebuild URL from base, filtered parameters, and fragment
     * 
     * @param base The base URL
     * @param parameters The filtered parameters
     * @param fragment The fragment (including #)
     * @return Rebuilt URL
     */
    fun rebuildUrl(base: String, parameters: List<String>, fragment: String): String {
        val result = if (parameters.isEmpty()) {
            base + fragment
        } else {
            base + "?" + parameters.joinToString("&") + fragment
        }
        
        return postProcessUrl(result)
    }
} 