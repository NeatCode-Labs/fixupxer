package com.fixupxer.di

import android.content.Context
import com.fixupxer.PreferencesManager
import com.fixupxer.UrlProcessor
import com.fixupxer.domain.repository.UrlRepository
import com.fixupxer.data.repository.UrlRepositoryImpl
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
} 