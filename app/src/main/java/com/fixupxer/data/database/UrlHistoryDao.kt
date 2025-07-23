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