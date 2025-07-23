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


package com.fixupxer.data.repository

import com.fixupxer.data.database.UrlHistoryDao
import com.fixupxer.data.database.UrlHistoryEntity
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.utils.timeAgo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val urlHistoryDao: UrlHistoryDao
) : HistoryRepository {
    
    override suspend fun insertHistory(
        originalUrl: String,
        cleanedUrl: String,
        platform: String,
        conversionType: String
    ) {
        try {
            val entity = UrlHistoryEntity(
                originalUrl = originalUrl,
                cleanedUrl = cleanedUrl,
                platform = platform,
                conversionType = conversionType,
                timestamp = System.currentTimeMillis()
            )
            urlHistoryDao.insert(entity)
        } catch (e: Exception) {
            // Silently ignore errors
        }
    }
    
    override fun getAllHistory(): Flow<List<com.fixupxer.domain.model.UrlHistory>> {
        return urlHistoryDao.getAllHistory().map { entities ->
            entities.map { entity ->
                com.fixupxer.domain.model.UrlHistory(
                    id = entity.id,
                    originalUrl = entity.originalUrl,
                    cleanedUrl = entity.cleanedUrl,
                    platform = entity.platform,
                    conversionType = entity.conversionType,
                    timestamp = entity.timestamp,
                    timeAgo = entity.timestamp.timeAgo()
                )
            }
        }
    }
    
    override suspend fun deleteHistory(id: Long) {
        urlHistoryDao.delete(id)
    }
    
    override suspend fun deleteAllHistory() {
        urlHistoryDao.deleteAll()
    }
    
    override suspend fun trimHistory(maxEntries: Int) {
        urlHistoryDao.trimHistory(maxEntries)
    }
} 