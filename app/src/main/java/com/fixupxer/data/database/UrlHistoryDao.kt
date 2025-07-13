package com.fixupxer.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for URL history database operations
 */
@Dao
interface UrlHistoryDao {
    @Query("SELECT * FROM url_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<UrlHistoryEntity>>
    
    @Insert
    suspend fun insert(entry: UrlHistoryEntity)
    
    @Query("DELETE FROM url_history WHERE id = :id")
    suspend fun delete(id: Long)
    
    @Query("DELETE FROM url_history")
    suspend fun deleteAll()
    
    @Query("DELETE FROM url_history WHERE id NOT IN (SELECT id FROM url_history ORDER BY timestamp DESC LIMIT :maxEntries)")
    suspend fun trimHistory(maxEntries: Int)
} 