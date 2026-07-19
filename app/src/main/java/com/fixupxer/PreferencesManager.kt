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
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.FrontendRole
import com.fixupxer.utils.FrontendTarget
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import com.fixupxer.utils.TikTokProxyStore
import timber.log.Timber
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
        internal const val KEY_CONVERT_BLUESKY = "convert_bluesky"
        internal const val KEY_CONVERT_FACEBOOK = "convert_facebook"
        internal const val KEY_CONVERT_REDDIT = "convert_reddit"
        internal const val KEY_CONVERT_YOUTUBE = "convert_youtube"
        internal const val KEY_CONVERT_PINTEREST = "convert_pinterest"
        internal const val KEY_CONVERT_THREADS = "convert_threads"
        private const val KEY_INSTAGRAM_PROXY = "instagram_proxy_domain"
        private const val KEY_CUSTOM_INSTAGRAM_PROXIES = "custom_instagram_proxies"
        private const val KEY_TIKTOK_PROXY = "tiktok_proxy_domain"
        private const val KEY_CUSTOM_TIKTOK_PROXIES = "custom_tiktok_proxies"
        private const val KEY_HISTORY_ENABLED = "history_enabled"
        private const val KEY_MAX_HISTORY_ENTRIES = "max_history_entries"
        private const val DEFAULT_MAX_HISTORY_ENTRIES = 100
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DOMINANT_HAND = "dominant_hand"
        internal const val KEY_CUSTOM_RULES_ENABLED = "custom_rules_enabled"

        // Theme mode values
        const val THEME_MODE_SYSTEM = "system"
        const val THEME_MODE_LIGHT = "light"
        const val THEME_MODE_DARK = "dark"

        // Dominant hand values
        const val DOMINANT_HAND_RIGHT = "right"
        const val DOMINANT_HAND_LEFT = "left"
        
        // Browser mode keys
        private const val KEY_BROWSER_ENABLED = "browser_enabled"
        private const val KEY_ACTION_MODE = "action_mode"
        private const val KEY_ACTION_PRIORITY = "action_priority"
        
        // Browser mode conversion keys
        private const val KEY_BROWSER_CONVERT_TWITTER = "browser_convert_twitter"
        private const val KEY_BROWSER_CONVERT_INSTAGRAM = "browser_convert_instagram"
        private const val KEY_BROWSER_CONVERT_FACEBOOK = "browser_convert_facebook"
        private const val KEY_BROWSER_CONVERT_TIKTOK = "browser_convert_tiktok"
        internal const val KEY_BROWSER_CONVERT_BLUESKY = "browser_convert_bluesky"
        private const val KEY_BROWSER_CONVERT_REDDIT = "browser_convert_reddit"
        private const val KEY_BROWSER_CONVERT_YOUTUBE = "browser_convert_youtube"
        private const val KEY_BROWSER_CONVERT_PINTEREST = "browser_convert_pinterest"
        private const val KEY_BROWSER_CONVERT_THREADS = "browser_convert_threads"
        
        // Action mode constants
        const val ACTION_MODE_ASK = "ask"
        const val ACTION_MODE_PRIORITY = "priority"
        
        // Action types
        const val ACTION_NATIVE_APP = "native_app"
        const val ACTION_BROWSER = "browser"
        const val ACTION_SHARE_MENU = "share_menu"
        const val ACTION_CLIPBOARD = "clipboard"

        private fun keyForSelection(platform: ProxyPlatform): String = when (platform) {
            ProxyPlatform.INSTAGRAM -> KEY_INSTAGRAM_PROXY
            ProxyPlatform.TIKTOK -> KEY_TIKTOK_PROXY
            ProxyPlatform.X -> "proxy_selection_x"
            ProxyPlatform.FACEBOOK -> "proxy_selection_facebook"
            ProxyPlatform.BLUESKY -> "proxy_selection_bluesky"
            ProxyPlatform.REDDIT -> "proxy_selection_reddit"
            ProxyPlatform.YOUTUBE -> "proxy_selection_youtube"
            ProxyPlatform.PINTEREST -> "proxy_selection_pinterest"
            ProxyPlatform.THREADS -> "proxy_selection_threads"
        }

        private fun keyForCustomProxies(platform: ProxyPlatform): String = when (platform) {
            ProxyPlatform.INSTAGRAM -> KEY_CUSTOM_INSTAGRAM_PROXIES
            ProxyPlatform.TIKTOK -> KEY_CUSTOM_TIKTOK_PROXIES
            else -> "custom_proxies_${platform.name.lowercase()}"
        }

        private fun keyForDisabledBuiltIns(platform: ProxyPlatform): String =
            "disabled_builtin_proxies_${platform.name.lowercase()}"

        private fun keyForBrowserPrivacyTarget(platform: ProxyPlatform): String =
            "browser_privacy_target_${platform.name.lowercase()}"
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
        migrateConvertFacebookIfNeeded()
        seedProxyRosterFromPrefs()
    }

    private fun migrateConvertFacebookIfNeeded() {
        if (!prefs.contains(KEY_CONVERT_FACEBOOK)) {
            prefs.edit {
                putBoolean(KEY_CONVERT_FACEBOOK, prefs.getBoolean(KEY_CONVERT_INSTAGRAM, true))
            }
        }
    }

    private fun seedProxyRosterFromPrefs() {
        ProxyPlatform.entries.forEach { platform ->
            ProxyRoster.setCustomProxies(platform, getCustomProxies(platform))
            ProxyRoster.setDisabledBuiltIns(platform, getDisabledBuiltIns(platform))
        }
        // Legacy facades read from the same ProxyRoster snapshot.
        InstagramProxyStore.setCustomProxies(getCustomInstagramProxies())
        TikTokProxyStore.setCustomProxies(getCustomTikTokProxies())
    }

    private fun readCsvList(key: String): List<String> {
        val stored = prefs.getString(key, null)
        return if (stored.isNullOrEmpty()) emptyList() else stored.split(",").filter { it.isNotBlank() }
    }

    private fun readCsvSet(key: String): Set<String> {
        val stored = prefs.getString(key, null)
        return if (stored.isNullOrEmpty()) emptySet() else stored.split(",").filter { it.isNotBlank() }.toSet()
    }

    private fun activeDomainStrings(platform: ProxyPlatform): List<String> =
        ProxyRoster.activeTargets(platform).map { it.domain }

    private fun resolveActiveSelection(platform: ProxyPlatform, stored: String?): String? {
        if (stored != null && stored in activeDomainStrings(platform)) return stored
        val defaultDomain = AlternativeFrontendCatalog.defaultTarget(platform)?.domain
        if (defaultDomain != null && defaultDomain in activeDomainStrings(platform)) return defaultDomain
        return activeDomainStrings(platform).firstOrNull()
    }

    /**
     * Returns the stored proxy domain when it maps to an active target; otherwise the
     * platform default if still active; otherwise the first active target; otherwise null.
     */
    fun getSelectedProxyDomain(platform: ProxyPlatform): String? {
        val key = keyForSelection(platform)
        val defaultDomain = AlternativeFrontendCatalog.defaultTarget(platform)?.domain
        val stored = prefs.getString(key, defaultDomain) ?: defaultDomain
        return resolveActiveSelection(platform, stored)
    }

    fun setSelectedProxyDomain(platform: ProxyPlatform, domain: String) {
        if (domain !in activeDomainStrings(platform)) return
        prefs.edit { putString(keyForSelection(platform), domain) }
    }

    fun getCustomProxies(platform: ProxyPlatform): List<String> =
        readCsvList(keyForCustomProxies(platform))

    fun addCustomProxy(platform: ProxyPlatform, domain: String) {
        val current = getCustomProxies(platform)
        if (domain in current) return
        persistCustomProxies(platform, current + domain)
    }

    fun removeCustomProxy(platform: ProxyPlatform, domain: String) {
        persistCustomProxies(platform, getCustomProxies(platform) - domain)
    }

    private fun persistCustomProxies(platform: ProxyPlatform, proxies: List<String>) {
        prefs.edit { putString(keyForCustomProxies(platform), proxies.joinToString(",")) }
        ProxyRoster.setCustomProxies(platform, proxies)
        when (platform) {
            ProxyPlatform.INSTAGRAM -> InstagramProxyStore.setCustomProxies(proxies)
            ProxyPlatform.TIKTOK -> TikTokProxyStore.setCustomProxies(proxies)
            else -> Unit
        }
    }

    fun getDisabledBuiltIns(platform: ProxyPlatform): Set<String> =
        readCsvSet(keyForDisabledBuiltIns(platform))

    fun disableBuiltIn(platform: ProxyPlatform, id: String) {
        val current = getDisabledBuiltIns(platform)
        if (id in current) return
        val newDisabled = current + id

        ProxyRoster.setDisabledBuiltIns(platform, newDisabled)
        val disabledTarget = AlternativeFrontendCatalog.byId(id)
        val selectionKey = keyForSelection(platform)
        val storedSelection = prefs.getString(selectionKey, null)
        val needsReselect = disabledTarget != null && storedSelection == disabledTarget.domain
        val nextSelection = if (needsReselect) resolveActiveSelection(platform, null) else null

        prefs.edit {
            putString(keyForDisabledBuiltIns(platform), newDisabled.joinToString(","))
            if (needsReselect) {
                if (nextSelection != null) {
                    putString(selectionKey, nextSelection)
                } else {
                    remove(selectionKey)
                }
            }
        }
    }

    fun enableBuiltIn(platform: ProxyPlatform, id: String) {
        val current = getDisabledBuiltIns(platform)
        if (id !in current) return
        val updated = current - id
        ProxyRoster.setDisabledBuiltIns(platform, updated)
        prefs.edit {
            if (updated.isEmpty()) {
                remove(keyForDisabledBuiltIns(platform))
            } else {
                putString(keyForDisabledBuiltIns(platform), updated.joinToString(","))
            }
        }
    }

    fun clearSelectedProxyDomain(platform: ProxyPlatform) {
        prefs.edit { remove(keyForSelection(platform)) }
    }

    fun restoreBuiltIns(platform: ProxyPlatform) {
        prefs.edit { remove(keyForDisabledBuiltIns(platform)) }
        ProxyRoster.setDisabledBuiltIns(platform, emptySet())
    }

    /**
     * Re-enables only the built-in READER targets for [platform]. Disabled embed or
     * automatic targets stay disabled: Browser privacy recovery needs Readers and must
     * not resurrect targets the user removed from the Main/Share pickers.
     */
    fun restoreBuiltInReaders(platform: ProxyPlatform) {
        val readerIds = AlternativeFrontendCatalog.builtInReaders(platform).map { it.id }.toSet()
        setDisabledBuiltIns(platform, getDisabledBuiltIns(platform) - readerIds)
    }

    /**
     * Overwrites the disabled built-in set for [platform] in both prefs and
     * [ProxyRoster]. Used to roll back an unsaved in-dialog roster restore.
     */
    fun setDisabledBuiltIns(platform: ProxyPlatform, ids: Set<String>) {
        ProxyRoster.setDisabledBuiltIns(platform, ids)
        prefs.edit {
            if (ids.isEmpty()) {
                remove(keyForDisabledBuiltIns(platform))
            } else {
                putString(keyForDisabledBuiltIns(platform), ids.joinToString(","))
            }
        }
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
     * Check if Bluesky post URL conversion is enabled.
     */
    fun isConvertBlueskyEnabled(): Boolean {
        return prefs.getBoolean(KEY_CONVERT_BLUESKY, true)
    }

    /**
     * Set whether Bluesky post URL conversion is enabled.
     */
    fun setConvertBlueskyEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CONVERT_BLUESKY, enabled) }
    }

    /**
     * Check if Facebook URL conversion is enabled (independent from Instagram since v2).
     */
    fun isConvertFacebookEnabled(): Boolean {
        return prefs.getBoolean(KEY_CONVERT_FACEBOOK, true)
    }

    /**
     * Set whether Facebook URL conversion is enabled.
     */
    fun setConvertFacebookEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CONVERT_FACEBOOK, enabled) }
    }

    fun isConvertRedditEnabled(): Boolean =
        prefs.getBoolean(KEY_CONVERT_REDDIT, false)

    fun setConvertRedditEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CONVERT_REDDIT, enabled) }
    }

    fun isConvertYoutubeEnabled(): Boolean =
        prefs.getBoolean(KEY_CONVERT_YOUTUBE, false)

    fun setConvertYoutubeEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CONVERT_YOUTUBE, enabled) }
    }

    fun isConvertPinterestEnabled(): Boolean =
        prefs.getBoolean(KEY_CONVERT_PINTEREST, false)

    fun setConvertPinterestEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CONVERT_PINTEREST, enabled) }
    }

    fun isConvertThreadsEnabled(): Boolean =
        prefs.getBoolean(KEY_CONVERT_THREADS, false)

    fun setConvertThreadsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CONVERT_THREADS, enabled) }
    }

    /**
     * Get the currently selected Instagram embed proxy domain.
     * Returns empty string when no active target exists (conversion no-op).
     */
    fun getInstagramProxy(): String = getSelectedProxyDomain(ProxyPlatform.INSTAGRAM) ?: ""

    /**
     * Set the selected Instagram embed proxy domain. Must be one of the active
     * proxies (fixed roster or a saved custom proxy); anything else is ignored.
     */
    fun setInstagramProxy(domain: String) {
        setSelectedProxyDomain(ProxyPlatform.INSTAGRAM, domain)
    }

    /**
     * User-defined custom Instagram proxies (persisted comma-separated).
     */
    fun getCustomInstagramProxies(): List<String> =
        getCustomProxies(ProxyPlatform.INSTAGRAM)

    /**
     * Add a custom Instagram proxy. The caller is expected to pass a domain that
     * already passed [InstagramProxyStore] normalization + validation.
     */
    fun addCustomInstagramProxy(domain: String) {
        addCustomProxy(ProxyPlatform.INSTAGRAM, domain)
    }

    /**
     * Remove a custom Instagram proxy. If it was the selected proxy,
     * [getInstagramProxy] transparently falls back to the next active target.
     */
    fun removeCustomInstagramProxy(domain: String) {
        removeCustomProxy(ProxyPlatform.INSTAGRAM, domain)
    }

    /**
     * Get the currently selected TikTok embed proxy domain.
     * Returns empty string when no active target exists (conversion no-op).
     */
    fun getTikTokProxy(): String = getSelectedProxyDomain(ProxyPlatform.TIKTOK) ?: ""

    /**
     * Set the selected TikTok embed proxy domain. Must be one of the active
     * proxies (fixed roster or a saved custom proxy); anything else is ignored.
     */
    fun setTikTokProxy(domain: String) {
        setSelectedProxyDomain(ProxyPlatform.TIKTOK, domain)
    }

    /**
     * User-defined custom TikTok proxies (persisted comma-separated).
     */
    fun getCustomTikTokProxies(): List<String> =
        getCustomProxies(ProxyPlatform.TIKTOK)

    /**
     * Add a custom TikTok proxy. The caller is expected to pass a domain that
     * already passed [TikTokProxyStore] normalization + validation.
     */
    fun addCustomTikTokProxy(domain: String) {
        addCustomProxy(ProxyPlatform.TIKTOK, domain)
    }

    /**
     * Remove a custom TikTok proxy. If it was the selected proxy,
     * [getTikTokProxy] transparently falls back to the next active target.
     */
    fun removeCustomTikTokProxy(domain: String) {
        removeCustomProxy(ProxyPlatform.TIKTOK, domain)
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

    /**
     * Returns the selected dominant hand. Unknown values safely fall back to right-handed.
     */
    fun getDominantHand(): String {
        return when (val stored = prefs.getString(KEY_DOMINANT_HAND, DOMINANT_HAND_RIGHT)) {
            DOMINANT_HAND_RIGHT, DOMINANT_HAND_LEFT -> stored
            else -> DOMINANT_HAND_RIGHT
        }
    }

    /**
     * Persists only supported dominant-hand values.
     */
    fun setDominantHand(hand: String) {
        if (hand != DOMINANT_HAND_RIGHT && hand != DOMINANT_HAND_LEFT) return
        prefs.edit { putString(KEY_DOMINANT_HAND, hand) }
    }

    fun areCustomRulesEnabled(): Boolean =
        prefs.getBoolean(KEY_CUSTOM_RULES_ENABLED, false)

    fun setCustomRulesEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CUSTOM_RULES_ENABLED, enabled) }
    }

    fun customRulesEnabledFlow(): Flow<Boolean> =
        booleanFlow(KEY_CUSTOM_RULES_ENABLED, false)

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
     * Check if Bluesky post URL conversion is enabled for browser mode.
     */
    fun isBrowserConvertBlueskyEnabled(): Boolean {
        return prefs.getBoolean(KEY_BROWSER_CONVERT_BLUESKY, false)
    }

    /**
     * Set whether Bluesky post URL conversion is enabled for browser mode.
     */
    fun setBrowserConvertBlueskyEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BROWSER_CONVERT_BLUESKY, enabled) }
    }

    fun isBrowserConvertRedditEnabled(): Boolean =
        prefs.getBoolean(KEY_BROWSER_CONVERT_REDDIT, false)

    fun setBrowserConvertRedditEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BROWSER_CONVERT_REDDIT, enabled) }
    }

    fun isBrowserConvertPinterestEnabled(): Boolean =
        prefs.getBoolean(KEY_BROWSER_CONVERT_PINTEREST, false)

    fun setBrowserConvertPinterestEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BROWSER_CONVERT_PINTEREST, enabled) }
    }

    fun isBrowserPrivacyConversionEnabled(platform: ProxyPlatform): Boolean = when (platform) {
        ProxyPlatform.X -> isBrowserConvertTwitterEnabled()
        ProxyPlatform.BLUESKY -> isBrowserConvertBlueskyEnabled()
        ProxyPlatform.REDDIT -> isBrowserConvertRedditEnabled()
        ProxyPlatform.PINTEREST -> isBrowserConvertPinterestEnabled()
        else -> false
    }

    fun setBrowserPrivacyTargetId(platform: ProxyPlatform, targetId: String) {
        val target = AlternativeFrontendCatalog.byId(targetId)
        if (target == null || target.platform != platform || target.role != FrontendRole.READER) {
            Timber.w("Ignoring invalid browser privacy target id=%s for platform=%s", targetId, platform)
            return
        }
        prefs.edit { putString(keyForBrowserPrivacyTarget(platform), targetId) }
    }

    fun getBrowserPrivacyTargetId(platform: ProxyPlatform): String? =
        prefs.getString(keyForBrowserPrivacyTarget(platform), null)

    fun resolveBrowserPrivacyTarget(platform: ProxyPlatform): FrontendTarget? {
        val storedId = getBrowserPrivacyTargetId(platform)
        if (storedId != null) {
            val stored = AlternativeFrontendCatalog.byId(storedId)
            if (stored != null &&
                stored.platform == platform &&
                stored.role == FrontendRole.READER &&
                storedId !in getDisabledBuiltIns(platform)
            ) {
                return stored
            }
        }
        return AlternativeFrontendCatalog.builtInReaders(platform)
            .firstOrNull { it.id !in getDisabledBuiltIns(platform) }
    }

    fun resolveBrowserPrivacySelections(): Map<ProxyPlatform, String?> =
        ProxyPlatform.entries.associateWith { resolveBrowserPrivacyTarget(it)?.domain }
}
