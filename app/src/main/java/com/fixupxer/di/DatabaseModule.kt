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