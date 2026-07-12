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
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.TikTokProxyStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Manages user preferences for the app
 */
class PreferencesManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "FixupXerPrefs"

        // Preference keys (internal for reactive Flow consumers in the data layer)
        internal const val KEY_CLEAN_TRACKING = "clean_tracking"
        internal const val KEY_CONVERT_TWITTER = "convert_twitter"
        internal const val KEY_CONVERT_INSTAGRAM = "convert_instagram"
        internal const val KEY_CONVERT_TIKTOK = "convert_tiktok"
        private const val KEY_INSTAGRAM_PROXY = "instagram_proxy_domain"
        private const val KEY_CUSTOM_INSTAGRAM_PROXIES = "custom_instagram_proxies"
        private const val KEY_TIKTOK_PROXY = "tiktok_proxy_domain"
        private const val KEY_CUSTOM_TIKTOK_PROXIES = "custom_tiktok_proxies"
        private const val KEY_HISTORY_ENABLED = "history_enabled"
        private const val KEY_MAX_HISTORY_ENTRIES = "max_history_entries"
        private const val DEFAULT_MAX_HISTORY_ENTRIES = 100
        private const val KEY_THEME_MODE = "theme_mode"
        internal const val KEY_CUSTOM_RULES_ENABLED = "custom_rules_enabled"

        // Theme mode values
        const val THEME_MODE_SYSTEM = "system"
        const val THEME_MODE_LIGHT = "light"
        const val THEME_MODE_DARK = "dark"
        
        // Browser mode keys
        private const val KEY_BROWSER_ENABLED = "browser_enabled"
        private const val KEY_ACTION_MODE = "action_mode"
        private const val KEY_ACTION_PRIORITY = "action_priority"
        
        // Browser mode conversion keys
        private const val KEY_BROWSER_CONVERT_TWITTER = "browser_convert_twitter"
        private const val KEY_BROWSER_CONVERT_INSTAGRAM = "browser_convert_instagram"
        private const val KEY_BROWSER_CONVERT_FACEBOOK = "browser_convert_facebook"
        private const val KEY_BROWSER_CONVERT_TIKTOK = "browser_convert_tiktok"
        
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
     * Reactive stream for a boolean preference. Emits the current value immediately,
     * then on every subsequent change to [key].
     */
    fun booleanFlow(key: String, default: Boolean): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                trySend(prefs.getBoolean(key, default))
            }
        }
        trySend(prefs.getBoolean(key, default))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    init {
        // Mirror persisted custom proxies into the process-wide stores so stateless
        // consumers (UrlProcessor, InstagramCleaner, TikTokCleaner) see them immediately.
        InstagramProxyStore.setCustomProxies(getCustomInstagramProxies())
        TikTokProxyStore.setCustomProxies(getCustomTikTokProxies())
    }

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
     * Check if TikTok URL conversion is enabled
     */
    fun isConvertTikTokEnabled(): Boolean {
        return prefs.getBoolean(KEY_CONVERT_TIKTOK, true)
    }

    /**
     * Set whether TikTok URL conversion is enabled
     */
    fun setConvertTikTokEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CONVERT_TIKTOK, enabled) }
    }

    /**
     * Get the currently selected Instagram embed proxy domain.
     * Defaults to [Constants.INSTAGRAM_DEFAULT_PROXY]. If the stored value is no longer
     * an active proxy — e.g. a legacy domain like `eeinstagram.com` from an old version,
     * or a custom proxy the user has since deleted — we silently migrate to the default.
     */
    fun getInstagramProxy(): String {
        val value = prefs.getString(KEY_INSTAGRAM_PROXY, Constants.INSTAGRAM_DEFAULT_PROXY)
            ?: Constants.INSTAGRAM_DEFAULT_PROXY
        return if (InstagramProxyStore.activeProxies().contains(value)) {
            value
        } else {
            Constants.INSTAGRAM_DEFAULT_PROXY
        }
    }

    /**
     * Set the selected Instagram embed proxy domain. Must be one of the active
     * proxies (fixed roster or a saved custom proxy); anything else is ignored.
     */
    fun setInstagramProxy(domain: String) {
        if (!InstagramProxyStore.activeProxies().contains(domain)) return
        prefs.edit { putString(KEY_INSTAGRAM_PROXY, domain) }
    }

    /**
     * User-defined custom Instagram proxies (persisted comma-separated).
     */
    fun getCustomInstagramProxies(): List<String> {
        val stored = prefs.getString(KEY_CUSTOM_INSTAGRAM_PROXIES, null)
        return if (stored.isNullOrEmpty()) emptyList() else stored.split(",").filter { it.isNotBlank() }
    }

    /**
     * Add a custom Instagram proxy. The caller is expected to pass a domain that
     * already passed [InstagramProxyStore] normalization + validation.
     */
    fun addCustomInstagramProxy(domain: String) {
        val current = getCustomInstagramProxies()
        if (domain in current) return
        persistCustomProxies(current + domain)
    }

    /**
     * Remove a custom Instagram proxy. If it was the selected proxy,
     * [getInstagramProxy] transparently falls back to the default.
     */
    fun removeCustomInstagramProxy(domain: String) {
        persistCustomProxies(getCustomInstagramProxies() - domain)
    }

    private fun persistCustomProxies(proxies: List<String>) {
        prefs.edit { putString(KEY_CUSTOM_INSTAGRAM_PROXIES, proxies.joinToString(",")) }
        InstagramProxyStore.setCustomProxies(proxies)
    }

    /**
     * Get the currently selected TikTok embed proxy domain.
     * Defaults to [Constants.TIKTOK_DEFAULT_PROXY]. If the stored value is no longer
     * an active proxy (e.g. a custom proxy the user has since deleted), we silently
     * migrate to the default.
     */
    fun getTikTokProxy(): String {
        val value = prefs.getString(KEY_TIKTOK_PROXY, Constants.TIKTOK_DEFAULT_PROXY)
            ?: Constants.TIKTOK_DEFAULT_PROXY
        return if (TikTokProxyStore.activeProxies().contains(value)) {
            value
        } else {
            Constants.TIKTOK_DEFAULT_PROXY
        }
    }

    /**
     * Set the selected TikTok embed proxy domain. Must be one of the active
     * proxies (fixed roster or a saved custom proxy); anything else is ignored.
     */
    fun setTikTokProxy(domain: String) {
        if (!TikTokProxyStore.activeProxies().contains(domain)) return
        prefs.edit { putString(KEY_TIKTOK_PROXY, domain) }
    }

    /**
     * User-defined custom TikTok proxies (persisted comma-separated).
     */
    fun getCustomTikTokProxies(): List<String> {
        val stored = prefs.getString(KEY_CUSTOM_TIKTOK_PROXIES, null)
        return if (stored.isNullOrEmpty()) emptyList() else stored.split(",").filter { it.isNotBlank() }
    }

    /**
     * Add a custom TikTok proxy. The caller is expected to pass a domain that
     * already passed [TikTokProxyStore] normalization + validation.
     */
    fun addCustomTikTokProxy(domain: String) {
        val current = getCustomTikTokProxies()
        if (domain in current) return
        persistCustomTikTokProxies(current + domain)
    }

    /**
     * Remove a custom TikTok proxy. If it was the selected proxy,
     * [getTikTokProxy] transparently falls back to the default.
     */
    fun removeCustomTikTokProxy(domain: String) {
        persistCustomTikTokProxies(getCustomTikTokProxies() - domain)
    }

    private fun persistCustomTikTokProxies(proxies: List<String>) {
        prefs.edit { putString(KEY_CUSTOM_TIKTOK_PROXIES, proxies.joinToString(",")) }
        TikTokProxyStore.setCustomProxies(proxies)
    }

    /**
     * Get the selected theme mode: [THEME_MODE_SYSTEM] (default), [THEME_MODE_LIGHT]
     * or [THEME_MODE_DARK].
     */
    fun getThemeMode(): String {
        return when (val stored = prefs.getString(KEY_THEME_MODE, THEME_MODE_SYSTEM)) {
            THEME_MODE_LIGHT, THEME_MODE_DARK, THEME_MODE_SYSTEM -> stored
            // Unknown value (corrupted prefs / backup restore from a newer
            // version) — fall back to following the system.
            else -> THEME_MODE_SYSTEM
        }
    }

    /**
     * Persist the selected theme mode.
     */
    fun setThemeMode(mode: String) {
        prefs.edit { putString(KEY_THEME_MODE, mode) }
    }

    fun areCustomRulesEnabled(): Boolean =
        prefs.getBoolean(KEY_CUSTOM_RULES_ENABLED, true)

    fun setCustomRulesEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CUSTOM_RULES_ENABLED, enabled) }
    }

    fun customRulesEnabledFlow(): Flow<Boolean> =
        booleanFlow(KEY_CUSTOM_RULES_ENABLED, true)

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
    
    /**
     * Check if TikTok URL conversion is enabled for browser mode
     */
    fun isBrowserConvertTikTokEnabled(): Boolean {
        return prefs.getBoolean(KEY_BROWSER_CONVERT_TIKTOK, false)
    }
    
    /**
     * Set whether TikTok URL conversion is enabled for browser mode
     */
    fun setBrowserConvertTikTokEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BROWSER_CONVERT_TIKTOK, enabled) }
    }
} 