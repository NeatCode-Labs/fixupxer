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


package com.fixupxer.di

import android.content.Context
import androidx.room.Room
import com.fixupxer.data.database.FixupXerDatabase
import com.fixupxer.data.database.UrlHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): FixupXerDatabase {
        return Room.databaseBuilder(
            context,
            FixupXerDatabase::class.java,
            "fixupxer_database"
        )
        .fallbackToDestructiveMigration() // Allow destructive migration for development
        .build()
    }
    
    @Provides
    @Singleton
    fun provideUrlHistoryDao(
        database: FixupXerDatabase
    ): UrlHistoryDao {
        return database.urlHistoryDao()
    }
} 