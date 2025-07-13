package com.fixupxer.domain.model

/**
 * Domain model for URL history
 */
data class UrlHistory(
    val id: Long,
    val originalUrl: String,
    val cleanedUrl: String,
    val platform: String,
    val conversionType: String,
    val timestamp: Long,
    val timeAgo: String
) 