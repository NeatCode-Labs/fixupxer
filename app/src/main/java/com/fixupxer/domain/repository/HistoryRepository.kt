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