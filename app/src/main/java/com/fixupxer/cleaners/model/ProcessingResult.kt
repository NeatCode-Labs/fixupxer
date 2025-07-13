package com.fixupxer.cleaners.model

/**
 * Result of URL processing with detailed information about what was changed
 */
data class ProcessingResult(
    val originalUrl: String,
    val cleanedUrl: String,
    val removedParameters: List<RemovedParameter>,
    val appliedCleaners: List<AppliedCleaner>,
    val totalPasses: Int,
    val wasModified: Boolean = originalUrl != cleanedUrl
) {
    /**
     * Get a human-readable summary of changes
     */
    fun getSummary(): String {
        return buildString {
            if (!wasModified) {
                append("No changes needed - URL is already clean")
            } else {
                append("Removed ${removedParameters.size} tracking parameter(s)")
                if (appliedCleaners.isNotEmpty()) {
                    append(" using ${appliedCleaners.size} cleaner(s)")
                }
                append(" in $totalPasses pass(es)")
            }
        }
    }
    
    /**
     * Get detailed diff showing what was removed
     */
    fun getDetailedDiff(): String {
        return buildString {
            appendLine("Original: $originalUrl")
            appendLine("Cleaned:  $cleanedUrl")
            
            if (removedParameters.isNotEmpty()) {
                appendLine("\nRemoved parameters:")
                removedParameters.forEach { param ->
                    appendLine("  - ${param.key}=${param.value}")
                }
            }
            
            if (appliedCleaners.isNotEmpty()) {
                appendLine("\nApplied cleaners:")
                appliedCleaners.forEach { cleaner ->
                    appendLine("  - ${cleaner.name}: ${cleaner.action}")
                }
            }
        }
    }
}

/**
 * Represents a removed parameter
 */
data class RemovedParameter(
    val key: String,
    val value: String
)

/**
 * Represents a cleaner that was applied
 */
data class AppliedCleaner(
    val id: String,
    val name: String,
    val action: String
) 