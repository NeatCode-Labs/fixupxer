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


package com.fixupxer

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.fixupxer.utils.Constants

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
        private const val KEY_INSTAGRAM_PROXY = "instagram_proxy_domain"
        private const val KEY_HISTORY_ENABLED = "history_enabled"
        private const val KEY_MAX_HISTORY_ENTRIES = "max_history_entries"
        private const val DEFAULT_MAX_HISTORY_ENTRIES = 100
        
        // Browser mode keys
        private const val KEY_BROWSER_ENABLED = "browser_enabled"
        private const val KEY_ACTION_MODE = "action_mode"
        private const val KEY_ACTION_PRIORITY = "action_priority"
        
        // Browser mode conversion keys
        private const val KEY_BROWSER_CONVERT_TWITTER = "browser_convert_twitter"
        private const val KEY_BROWSER_CONVERT_INSTAGRAM = "browser_convert_instagram"
        private const val KEY_BROWSER_CONVERT_FACEBOOK = "browser_convert_facebook"
        
        // Action mode constants
        const val ACTION_MODE_ASK = "ask"
        const val ACTION_MODE_PRIORITY = "priority"
        
        // Action types
        const val ACTION_NATIVE_APP = "native_app"
        const val ACTION_BROWSER = "browser"
        const val ACTION_SHARE_MENU = "share_menu"
        const val ACTION_CLIPBOARD = "clipboard"
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

    /**
     * Get the currently selected Instagram embed proxy domain.
     * Defaults to [Constants.INSTAGRAM_DEFAULT_PROXY]. If the stored value is no longer
     * a supported (active) proxy — e.g. a user upgrading from v1.4.7 had `kkinstagram.com`
     * or `eeinstagram.com` saved — we silently migrate to the default.
     */
    fun getInstagramProxy(): String {
        val value = prefs.getString(KEY_INSTAGRAM_PROXY, Constants.INSTAGRAM_DEFAULT_PROXY)
            ?: Constants.INSTAGRAM_DEFAULT_PROXY
        return if (Constants.INSTAGRAM_PROXY_DOMAINS.contains(value)) {
            value
        } else {
            Constants.INSTAGRAM_DEFAULT_PROXY
        }
    }

    /**
     * Set the selected Instagram embed proxy domain.
     */
    fun setInstagramProxy(domain: String) {
        prefs.edit { putString(KEY_INSTAGRAM_PROXY, domain) }
    }

    /**
     * Check if history is enabled
     */
    fun isHistoryEnabled(): Boolean {
        return prefs.getBoolean(KEY_HISTORY_ENABLED, true) // Enabled by default
    }
    
    /**
     * Set whether history is enabled
     */
    fun setHistoryEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_HISTORY_ENABLED, enabled) }
    }
    
    /**
     * Get maximum number of history entries to keep
     */
    fun getMaxHistoryEntries(): Int {
        return prefs.getInt(KEY_MAX_HISTORY_ENTRIES, DEFAULT_MAX_HISTORY_ENTRIES)
    }
    
    /**
     * Set maximum number of history entries to keep
     */
    fun setMaxHistoryEntries(maxEntries: Int) {
        prefs.edit { putInt(KEY_MAX_HISTORY_ENTRIES, maxEntries) }
    }
    
    /**
     * Check if browser mode is enabled
     */
    fun isBrowserModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_BROWSER_ENABLED, false)
    }
    
    /**
     * Set whether browser mode is enabled
     */
    fun setBrowserModeEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BROWSER_ENABLED, enabled) }
    }
    
    /**
     * Get the action mode (ask or priority)
     */
    fun getActionMode(): String {
        return prefs.getString(KEY_ACTION_MODE, ACTION_MODE_ASK) ?: ACTION_MODE_ASK
    }
    
    /**
     * Set the action mode
     */
    fun setActionMode(mode: String) {
        prefs.edit { putString(KEY_ACTION_MODE, mode) }
    }
    
    /**
     * Get the action priority list
     */
    fun getActionPriority(): List<String> {
        val priorityString = prefs.getString(KEY_ACTION_PRIORITY, null)
        return if (priorityString.isNullOrEmpty()) {
            // Default priority order
            listOf(ACTION_NATIVE_APP, ACTION_BROWSER, ACTION_SHARE_MENU, ACTION_CLIPBOARD)
        } else {
            priorityString.split(",")
        }
    }
    
    /**
     * Set the action priority list
     */
    fun setActionPriority(priority: List<String>) {
        prefs.edit { putString(KEY_ACTION_PRIORITY, priority.joinToString(",")) }
    }
    
    /**
     * Check if Twitter/X URL conversion is enabled for browser mode
     */
    fun isBrowserConvertTwitterEnabled(): Boolean {
        return prefs.getBoolean(KEY_BROWSER_CONVERT_TWITTER, false)
    }
    
    /**
     * Set whether Twitter/X URL conversion is enabled for browser mode
     */
    fun setBrowserConvertTwitterEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BROWSER_CONVERT_TWITTER, enabled) }
    }
    
    /**
     * Check if Instagram URL conversion is enabled for browser mode
     */
    fun isBrowserConvertInstagramEnabled(): Boolean {
        return prefs.getBoolean(KEY_BROWSER_CONVERT_INSTAGRAM, false)
    }
    
    /**
     * Set whether Instagram URL conversion is enabled for browser mode
     */
    fun setBrowserConvertInstagramEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BROWSER_CONVERT_INSTAGRAM, enabled) }
    }
    
    /**
     * Check if Facebook URL conversion is enabled for browser mode
     */
    fun isBrowserConvertFacebookEnabled(): Boolean {
        return prefs.getBoolean(KEY_BROWSER_CONVERT_FACEBOOK, false)
    }
    
    /**
     * Set whether Facebook URL conversion is enabled for browser mode
     */
    fun setBrowserConvertFacebookEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BROWSER_CONVERT_FACEBOOK, enabled) }
    }
} 