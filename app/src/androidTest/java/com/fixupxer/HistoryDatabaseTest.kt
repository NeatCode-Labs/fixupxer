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


package com.fixupxer

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixupxer.data.database.FixupXerDatabase
import com.fixupxer.data.database.UrlHistoryDao
import com.fixupxer.data.database.UrlHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryDatabaseTest {
    private lateinit var database: FixupXerDatabase
    private lateinit var historyDao: UrlHistoryDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FixupXerDatabase::class.java
        ).allowMainThreadQueries().build()
        
        historyDao = database.urlHistoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveHistory() = runBlocking {
        // Insert a history entry
        val entry = UrlHistoryEntity(
            originalUrl = "https://www.instagram.com/p/test?utm_source=test",
            cleanedUrl = "https://www.kkinstagram.com/p/test",
            platform = "Instagram",
            conversionType = "Domain converted",
            timestamp = System.currentTimeMillis()
        )
        
        historyDao.insert(entry)
        
        // Retrieve and verify
        val allHistory = historyDao.getAllHistory().first()
        assertEquals(1, allHistory.size)
        assertEquals(entry.originalUrl, allHistory[0].originalUrl)
        assertEquals(entry.cleanedUrl, allHistory[0].cleanedUrl)
        assertEquals(entry.platform, allHistory[0].platform)
    }

    @Test
    fun testMultipleEntries() = runBlocking {
        // Insert multiple entries
        repeat(10) { i ->
            val entry = UrlHistoryEntity(
                originalUrl = "https://www.example.com/page$i",
                cleanedUrl = "https://www.example.com/page$i",
                platform = "Other",
                conversionType = "Tracking removed",
                timestamp = System.currentTimeMillis() - i * 1000 // Different timestamps
            )
            historyDao.insert(entry)
        }
        
        // Test retrieval
        val allHistory = historyDao.getAllHistory().first()
        assertEquals(10, allHistory.size)
        
        // Verify ordering (newest first)
        assertTrue(allHistory[0].timestamp > allHistory[1].timestamp)
    }

    @Test
    fun testDeleteHistoryById() = runBlocking {
        // Insert an entry
        val entry = UrlHistoryEntity(
            originalUrl = "https://www.facebook.com/test",
            cleanedUrl = "https://www.facebookez.com/test",
            platform = "Facebook",
            conversionType = "Domain converted",
            timestamp = System.currentTimeMillis()
        )
        
        historyDao.insert(entry)
        val allHistory = historyDao.getAllHistory().first()
        assertEquals(1, allHistory.size)
        
        // Delete the entry
        val insertedId = allHistory[0].id
        historyDao.delete(insertedId)
        
        // Verify deletion
        val afterDelete = historyDao.getAllHistory().first()
        assertEquals(0, afterDelete.size)
    }

    @Test
    fun testDeleteAllHistory() = runBlocking {
        // Insert multiple entries
        repeat(5) { i ->
            val entry = UrlHistoryEntity(
                originalUrl = "https://x.com/user/status/$i",
                cleanedUrl = "https://fixupx.com/user/status/$i",
                platform = "Twitter",
                conversionType = "Domain converted",
                timestamp = System.currentTimeMillis()
            )
            historyDao.insert(entry)
        }
        
        // Verify insertion
        val beforeClear = historyDao.getAllHistory().first()
        assertEquals(5, beforeClear.size)
        
        // Clear all
        historyDao.deleteAll()
        
        // Verify all cleared
        val afterClear = historyDao.getAllHistory().first()
        assertEquals(0, afterClear.size)
    }

    @Test
    fun testTrimHistory() = runBlocking {
        // Insert entries with different timestamps
        val entries = mutableListOf<UrlHistoryEntity>()
        repeat(10) { i ->
            val entry = UrlHistoryEntity(
                originalUrl = "https://old.com/$i",
                cleanedUrl = "https://old.com/$i",
                platform = "Other",
                conversionType = "Already clean",
                timestamp = System.currentTimeMillis() - (10 - i) * 60000 // Older entries have lower timestamps
            )
            historyDao.insert(entry)
            entries.add(entry)
        }
        
        // Keep only 5 newest
        historyDao.trimHistory(5)
        
        // Verify
        val remaining = historyDao.getAllHistory().first()
        assertEquals(5, remaining.size)
        
        // Verify that the newest 5 are kept
        assertTrue(remaining.all { historyEntry ->
            historyEntry.timestamp >= entries[5].timestamp
        })
    }

    @Test
    fun testFacebookPrefixRemoval() = runBlocking {
        // Test m.facebook.com conversion
        val entry = UrlHistoryEntity(
            originalUrl = "https://m.facebook.com/story.php?id=123",
            cleanedUrl = "https://facebookez.com/story.php?id=123",
            platform = "Facebook",
            conversionType = "Domain converted",
            timestamp = System.currentTimeMillis()
        )
        
        historyDao.insert(entry)
        
        val history = historyDao.getAllHistory().first()
        assertEquals(1, history.size)
        assertFalse(history[0].cleanedUrl.contains("m.facebookez.com"))
        assertTrue(history[0].cleanedUrl.startsWith("https://facebookez.com"))
    }
} 