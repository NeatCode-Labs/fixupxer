package com.fixupxer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for FixupXer
 * Handles app-wide initialization and dependency injection setup
 */
@HiltAndroidApp
class FixupXerApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Plant a tree that only logs errors in release builds
            Timber.plant(ReleaseTree())
        }
        
        Timber.d("FixupXer Application initialized")
    }
    
    /**
     * Custom Timber tree for release builds that only logs warnings and errors
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.WARN || priority == android.util.Log.ERROR) {
                // In a real app, you might want to send these to a crash reporting service
                super.log(priority, tag, message, t)
            }
        }
    }
} 