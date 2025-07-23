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