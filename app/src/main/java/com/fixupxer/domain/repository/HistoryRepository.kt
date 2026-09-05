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


package com.fixupxer.domain.repository

import com.fixupxer.domain.model.UrlHistory
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for URL history operations
 */
interface HistoryRepository {
    /**
     * Get all history entries
     */
    fun getAllHistory(): Flow<List<UrlHistory>>
    
    /**
     * Insert a new history entry
     */
    suspend fun insertHistory(
        originalUrl: String,
        cleanedUrl: String,
        platform: String,
        conversionType: String
    )

    /** Restore a deleted entry with its original identity and chronological position. */
    suspend fun restoreHistory(entry: UrlHistory)
    
    /**
     * Delete a specific history entry by ID
     */
    suspend fun deleteHistory(id: Long)
    
    /**
     * Delete all history entries
     */
    suspend fun deleteAllHistory()
    
    /**
     * Trim history to keep only the most recent maxEntries
     */
    suspend fun trimHistory(maxEntries: Int)
}
