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