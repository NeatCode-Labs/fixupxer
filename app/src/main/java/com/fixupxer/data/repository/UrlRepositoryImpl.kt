package com.fixupxer.data.repository

import com.fixupxer.PreferencesManager
import com.fixupxer.UrlProcessor
import com.fixupxer.domain.model.ProcessedUrlResult
import com.fixupxer.domain.repository.UrlRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of UrlRepository
 */
class UrlRepositoryImpl @Inject constructor(
    private val urlProcessor: UrlProcessor,
    private val preferencesManager: PreferencesManager
) : UrlRepository {
    
    override suspend fun processUrl(url: String): ProcessedUrlResult = processUrl(url, false)
    
    override suspend fun processUrl(url: String, forceCleanTracking: Boolean): ProcessedUrlResult = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext ProcessedUrlResult(url, true)
        
        val isInstagram = urlProcessor.isInstagramUrl(url)
        
        val (processedUrl, wasAlreadyClean) = if (isInstagram) {
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
                cleanTracking = forceCleanTracking || preferencesManager.isCleanTrackingEnabled(),
                convertTwitter = preferencesManager.isConvertTwitterEnabled()
            )
        }
        
        ProcessedUrlResult(processedUrl, wasAlreadyClean)
    }
    
    override suspend fun processUrlForSharing(url: String): String = withContext(Dispatchers.IO) {
        urlProcessor.processUrlForSharing(url)
    }
    
    override suspend fun cleanUrl(url: String): String = withContext(Dispatchers.IO) {
        urlProcessor.cleanUrl(url)
    }
    
    override fun isInstagramUrl(url: String): Boolean = urlProcessor.isInstagramUrl(url)
    
    override fun isTwitterUrl(url: String): Boolean = urlProcessor.isTwitterUrl(url)
    
    override fun hasTrackingParameters(url: String): Boolean = urlProcessor.hasTrackingParameters(url)
    
    override fun isInstagramConversionEnabled(): Flow<Boolean> = flowOf(preferencesManager.isConvertInstagramEnabled())
    
    override suspend fun setInstagramConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesManager.setConvertInstagramEnabled(enabled)
        }
    }
    
    override fun isTrackingRemovalEnabled(): Flow<Boolean> = flowOf(preferencesManager.isCleanTrackingEnabled())
    
    override suspend fun setTrackingRemovalEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesManager.setCleanTrackingEnabled(enabled)
        }
    }
    
    override fun isTwitterConversionEnabled(): Flow<Boolean> = flowOf(preferencesManager.isConvertTwitterEnabled())
    
    override suspend fun setTwitterConversionEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesManager.setConvertTwitterEnabled(enabled)
        }
    }
} 