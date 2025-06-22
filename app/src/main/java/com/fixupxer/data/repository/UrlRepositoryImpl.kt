package com.fixupxer.data.repository

import com.fixupxer.PreferencesManager
import com.fixupxer.UrlProcessor
import com.fixupxer.domain.repository.UrlRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of UrlRepository
 */
class UrlRepositoryImpl @Inject constructor(
    private val urlProcessor: UrlProcessor,
    private val preferencesManager: PreferencesManager
) : UrlRepository {
    
    override suspend fun processUrl(url: String): String = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext url
        
        val isInstagram = urlProcessor.isInstagramUrl(url)
        
        if (isInstagram) {
            // For Instagram URLs, use the Instagram conversion preference
            urlProcessor.processUrl(
                url,
                cleanTracking = true, // Always clean tracking
                convertTwitter = preferencesManager.isConvertInstagramEnabled()
            )
        } else {
            // For other URLs, use the standard preferences
            urlProcessor.processUrl(
                url,
                cleanTracking = preferencesManager.isCleanTrackingEnabled(),
                convertTwitter = preferencesManager.isConvertTwitterEnabled()
            )
        }
    }
    
    override suspend fun processUrlForSharing(url: String): String = withContext(Dispatchers.IO) {
        urlProcessor.processUrlForSharing(url)
    }
    
    override fun isInstagramUrl(url: String): Boolean = urlProcessor.isInstagramUrl(url)
    
    override fun isInstagramConversionEnabled(): Flow<Boolean> = flow {
        emit(preferencesManager.isConvertInstagramEnabled())
    }
    
    override suspend fun setInstagramConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesManager.setConvertInstagramEnabled(enabled)
        }
    }
    
    override fun isTrackingRemovalEnabled(): Flow<Boolean> = flow {
        emit(preferencesManager.isCleanTrackingEnabled())
    }
    
    override suspend fun setTrackingRemovalEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesManager.setCleanTrackingEnabled(enabled)
        }
    }
    
    override fun isTwitterConversionEnabled(): Flow<Boolean> = flow {
        emit(preferencesManager.isConvertTwitterEnabled())
    }
    
    override suspend fun setTwitterConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesManager.setConvertTwitterEnabled(enabled)
        }
    }
} 