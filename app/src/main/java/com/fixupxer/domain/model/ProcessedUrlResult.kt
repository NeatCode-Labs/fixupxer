package com.fixupxer.domain.model

/**
 * Result of URL processing
 */
data class ProcessedUrlResult(
    val url: String,
    val wasAlreadyClean: Boolean
) 