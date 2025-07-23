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

import com.fixupxer.PreferencesManager
import com.fixupxer.cleaners.CleanerRegistry
import com.fixupxer.cleaners.CleanerService
import com.fixupxer.cleaners.cache.CleanerCache
import com.fixupxer.cleaners.impl.AmazonCleaner
import com.fixupxer.cleaners.impl.YouTubeCleaner
import com.fixupxer.cleaners.impl.GoogleSearchCleaner
import com.fixupxer.cleaners.impl.TwitterCleaner
import com.fixupxer.cleaners.impl.InstagramCleaner
import com.fixupxer.cleaners.impl.FacebookCleaner
import com.fixupxer.cleaners.impl.RedditCleaner
import com.fixupxer.cleaners.impl.TikTokCleaner
import com.fixupxer.cleaners.impl.LinkedInCleaner
import com.fixupxer.cleaners.impl.SubstackCleaner
import com.fixupxer.cleaners.impl.GeneralTrackingCleaner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for cleaner-specific dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object CleanerModule {
    
    @Provides
    @Singleton
    fun provideCleanerRegistry(
        generalTrackingCleaner: GeneralTrackingCleaner
    ): CleanerRegistry {
        return CleanerRegistry().apply {
            // Register all built-in cleaners
            registerAll(listOf(
                // Domain-specific cleaners (order matters - specific before general)
                AmazonCleaner,
                YouTubeCleaner,
                GoogleSearchCleaner,
                TwitterCleaner,
                InstagramCleaner,
                FacebookCleaner,
                RedditCleaner,
                TikTokCleaner,
                LinkedInCleaner,
                SubstackCleaner,
                // General cleaner as fallback
                generalTrackingCleaner
            ))
        }
    }
} 