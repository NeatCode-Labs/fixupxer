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