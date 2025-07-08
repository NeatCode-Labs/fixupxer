package com.fixupxer.di

import android.content.Context
import com.fixupxer.PreferencesManager
import com.fixupxer.UrlProcessor
import com.fixupxer.data.repository.UrlRepositoryImpl
import com.fixupxer.domain.repository.UrlRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing app-wide dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager = PreferencesManager(context)
    
    @Provides
    @Singleton
    fun provideUrlProcessor(): UrlProcessor = UrlProcessor()
    
    @Provides
    @Singleton
    fun provideUrlRepository(
        urlProcessor: UrlProcessor,
        preferencesManager: PreferencesManager
    ): UrlRepository = UrlRepositoryImpl(urlProcessor, preferencesManager)
} 