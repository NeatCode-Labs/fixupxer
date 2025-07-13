package com.fixupxer.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a URL history entry in the database
 */
@Entity(tableName = "url_history")
data class UrlHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalUrl: String,
    val cleanedUrl: String,
    val platform: String,
    val conversionType: String,
    val timestamp: Long = System.currentTimeMillis()
) 