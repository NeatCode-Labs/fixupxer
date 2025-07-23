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
import com.fixupxer.PreferencesManager
import com.fixupxer.UrlProcessor
import com.fixupxer.domain.repository.UrlRepository
import com.fixupxer.data.repository.UrlRepositoryImpl
import com.fixupxer.domain.repository.HistoryRepository
import com.fixupxer.data.repository.HistoryRepositoryImpl
import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.CleanerRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing app-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager {
        return PreferencesManager(context)
    }
    
    @Provides
    @Singleton
    fun provideCleanerCache(): CleanerCache {
        return CleanerCache()
    }
    
    @Provides
    @Singleton
    fun provideCleanerService(
        registry: CleanerRegistry,
        cache: CleanerCache,
        preferencesManager: PreferencesManager
    ): CleanerService {
        return CleanerService(registry, cache, preferencesManager)
    }
    
    @Provides
    @Singleton
    fun provideUrlRepository(
        urlRepository: UrlRepositoryImpl
    ): UrlRepository = urlRepository
    
    @Provides
    @Singleton
    fun provideHistoryRepository(
        historyRepository: HistoryRepositoryImpl
    ): HistoryRepository = historyRepository
} 