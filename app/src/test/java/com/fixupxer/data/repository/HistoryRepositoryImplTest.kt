// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.fixupxer.data.repository

import androidx.room.Room
import com.fixupxer.data.database.FixupXerDatabase
import com.fixupxer.data.database.UrlHistoryDao
import com.fixupxer.data.database.UrlHistoryEntity
import com.fixupxer.domain.model.UrlHistory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HistoryRepositoryImplTest {
    private lateinit var database: FixupXerDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            FixupXerDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `undo preserves identity timestamp and order while newer entries are added`() = runTest {
        val dao = database.urlHistoryDao()
        val repository = HistoryRepositoryImpl(dao)
        dao.insert(entity(id = 11, timestamp = 1_000))
        dao.insert(entity(id = 22, timestamp = 2_000))
        val deleted = repository.getAllHistory().first().last()

        repository.deleteHistory(deleted.id)
        dao.insert(entity(id = 33, timestamp = 3_000))
        repository.restoreHistory(deleted)

        val restored = repository.getAllHistory().first()
        assertEquals(listOf(33L, 22L, 11L), restored.map { it.id })
        assertEquals(deleted.timestamp, restored.last().timestamp)
        assertEquals(deleted.originalUrl, restored.last().originalUrl)
        assertEquals(deleted.cleanedUrl, restored.last().cleanedUrl)
        assertEquals(deleted.conversionType, restored.last().conversionType)
    }

    @Test
    fun `restore conflict reports failure and does not overwrite an existing entry`() = runTest {
        val dao = database.urlHistoryDao()
        val repository = HistoryRepositoryImpl(dao)
        dao.insert(entity(id = 11, timestamp = 1_000))
        val original = repository.getAllHistory().first().single()

        val failure = runCatching {
            repository.restoreHistory(original.copy(cleanedUrl = "https://different.example/"))
        }.exceptionOrNull()

        assertNotNull(failure)
        assertEquals(original.cleanedUrl, repository.getAllHistory().first().single().cleanedUrl)
    }

    @Test
    fun `insert database error reaches its caller`() = runTest {
        val dao: UrlHistoryDao = mock()
        val failure = IllegalStateException("Database write failed")
        whenever(dao.insert(any())).thenThrow(failure)

        val actual = runCatching {
            HistoryRepositoryImpl(dao).insertHistory("original", "cleaned", "Other", "Cleaned")
        }.exceptionOrNull()

        // Coroutine stack recovery can copy exceptions across dispatcher boundaries.
        assertEquals(failure.javaClass, actual?.javaClass)
        assertEquals(failure.message, actual?.message)
    }

    @Test
    fun `restore cancellation reaches its caller`() = runTest {
        val dao: UrlHistoryDao = mock()
        val cancellation = CancellationException("Cancelled write")
        whenever(dao.insert(any())).thenThrow(cancellation)
        val entry = UrlHistory(1L, "original", "cleaned", "Other", "Cleaned", 100L, "now")

        val actual = runCatching {
            HistoryRepositoryImpl(dao).restoreHistory(entry)
        }.exceptionOrNull()

        assertEquals(cancellation.javaClass, actual?.javaClass)
        assertEquals(cancellation.message, actual?.message)
    }

    private fun entity(id: Long, timestamp: Long) = UrlHistoryEntity(
        id = id,
        originalUrl = "https://example.com/$id?utm_source=test",
        cleanedUrl = "https://example.com/$id",
        platform = "Other",
        conversionType = "Tracking removed",
        timestamp = timestamp,
    )
}
