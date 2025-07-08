package com.fixupxer

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Manages user preferences for the app
 */
class PreferencesManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "FixupXerPrefs"

        // Preference keys
        private const val KEY_CLEAN_TRACKING = "clean_tracking"
        private const val KEY_CONVERT_TWITTER = "convert_twitter"
        private const val KEY_CONVERT_INSTAGRAM = "convert_instagram"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Check if tracking parameter cleaning is enabled
     */
    fun isCleanTrackingEnabled(): Boolean {
        return prefs.getBoolean(KEY_CLEAN_TRACKING, true)
    }

    /**
     * Set whether tracking parameter cleaning is enabled
     */
    fun setCleanTrackingEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CLEAN_TRACKING, enabled) }
    }

    /**
     * Check if Twitter/X URL conversion is enabled
     */
    fun isConvertTwitterEnabled(): Boolean {
        return prefs.getBoolean(KEY_CONVERT_TWITTER, true)
    }

    /**
     * Set whether Twitter/X URL conversion is enabled
     */
    fun setConvertTwitterEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CONVERT_TWITTER, enabled) }
    }
    
    /**
     * Check if Instagram URL conversion is enabled
     */
    fun isConvertInstagramEnabled(): Boolean {
        return prefs.getBoolean(KEY_CONVERT_INSTAGRAM, true)
    }

    /**
     * Set whether Instagram URL conversion is enabled
     */
    fun setConvertInstagramEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CONVERT_INSTAGRAM, enabled) }
    }


} 