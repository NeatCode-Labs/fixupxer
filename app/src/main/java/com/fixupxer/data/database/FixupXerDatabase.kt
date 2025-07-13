package com.fixupxer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for FixupXer app
 */
@Database(
    entities = [UrlHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FixupXerDatabase : RoomDatabase() {
    abstract fun urlHistoryDao(): UrlHistoryDao
} 