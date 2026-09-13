// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.fixupxer.ui.helpers

import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.databinding.ItemBrowserPrivacyPlatformBinding
import com.fixupxer.processing.BrowserConversionMode
import com.fixupxer.processing.BrowserFrontendPolicy
import com.fixupxer.processing.BrowserFrontendPreference
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.FrontendTarget
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster

/** Builds the seven Browser frontend rows and owns their unsaved dialog state. */
object BrowserConversionDefaultsHelper {

    data class BrowserPlatformEntry(
        val platform: ProxyPlatform,
        val labelRes: Int,
        val switchId: Int,
    )

    val entries: List<BrowserPlatformEntry> = BrowserFrontendPolicy.supportedPlatforms().map { platform ->
        BrowserPlatformEntry(
            platform = platform,
            labelRes = when (platform) {
                ProxyPlatform.X -> R.string.convert_twitter_browser
                ProxyPlatform.INSTAGRAM -> R.string.convert_instagram_browser
                ProxyPlatform.TIKTOK -> R.string.convert_tiktok_browser
                ProxyPlatform.FACEBOOK -> R.string.convert_facebook_browser
                ProxyPlatform.BLUESKY -> R.string.convert_bluesky_browser
                ProxyPlatform.REDDIT -> R.string.convert_reddit_browser
                ProxyPlatform.PINTEREST -> R.string.convert_pinterest_browser
                ProxyPlatform.YOUTUBE, ProxyPlatform.THREADS -> error("Clean-only platform")
            },
            switchId = when (platform) {
                ProxyPlatform.X -> R.id.switchBrowserTwitter
                ProxyPlatform.INSTAGRAM -> R.id.switchBrowserInstagram
                ProxyPlatform.TIKTOK -> R.id.switchBrowserTikTok
                ProxyPlatform.FACEBOOK -> R.id.switchBrowserFacebook
                ProxyPlatform.BLUESKY -> R.id.switchBrowserBluesky
                ProxyPlatform.REDDIT -> R.id.switchBrowserReddit
                ProxyPlatform.PINTEREST -> R.id.switchBrowserPinterest
                ProxyPlatform.YOUTUBE, ProxyPlatform.THREADS -> error("Clean-only platform")
            },
        )
    }

    data class BrowserFrontendRow(
        val entry: BrowserPlatformEntry,
        val binding: ItemBrowserPrivacyPlatformBinding,
    )

    class DraftState internal constructor(
        private val preferencesManager: PreferencesManager,
    ) {
        private var expected: Map<ProxyPlatform, BrowserFrontendPreference> = emptyMap()
        private var initialDisabled: Map<ProxyPlatform, Set<String>> = emptyMap()
        private var repairInvalidState = false
        val preferences: MutableMap<ProxyPlatform, BrowserFrontendPreference> = mutableMapOf()
        val disabledBuiltIns: MutableMap<ProxyPlatform, Set<String>> = mutableMapOf()

        init { refreshFromPreferences() }

        fun refreshFromPreferences() {
            repairInvalidState = preferencesManager.hasInvalidBrowserFrontendPreferences()
            expected = preferencesManager.getBrowserFrontendPreferences()
            initialDisabled = ProxyPlatform.entries.associateWith {
                preferencesManager.getDisabledBuiltIns(it)
            }
            preferences.clear()
            preferences.putAll(expected)
            disabledBuiltIns.clear()
            disabledBuiltIns.putAll(initialDisabled)
        }

        fun preference(platform: ProxyPlatform): BrowserFrontendPreference = preferences.getValue(platform)

        fun activeTargets(platform: ProxyPlatform): List<FrontendTarget> {
            val disabled = disabledBuiltIns[platform].orEmpty()
            val runtime = ProxyRoster.activeTargets(platform).filterNot { it.id in disabled }
            val restored = AlternativeFrontendCatalog.builtIn(platform).filter { target ->
                target.id !in disabled && runtime.none { it.id == target.id }
            }
            return BrowserFrontendPolicy.allowedTargets(platform, runtime + restored)
        }

        fun effectiveTarget(platform: ProxyPlatform): FrontendTarget? =
            BrowserFrontendPolicy.resolve(platform, preference(platform), activeTargets(platform))

        fun isUsingFallback(platform: ProxyPlatform): Boolean {
            val preference = preference(platform)
            val effective = effectiveTarget(platform)
            return preference.mode == BrowserConversionMode.READER &&
                preference.targetId != null &&
                effective != null &&
                effective.id != preference.targetId
        }

        fun savedTargetLabel(platform: ProxyPlatform): String? {
            val id = preference(platform).targetId ?: return null
            return (AlternativeFrontendCatalog.byId(id)
                ?: ProxyRoster.activeTargets(platform).firstOrNull { it.id == id })
                ?.let(FrontendDisplayHelper::displayLabel)
        }

        fun setEnabled(platform: ProxyPlatform, enabled: Boolean) {
            val current = preference(platform)
            if (!enabled) {
                preferences[platform] = BrowserFrontendPreference(
                    BrowserConversionMode.CLEAN_ONLY,
                    current.targetId,
                )
                return
            }
            val remembered = activeTargets(platform).firstOrNull { it.id == current.targetId }
                ?: AlternativeFrontendCatalog.byId(current.targetId.orEmpty())
                    ?.takeIf { it.platform == platform }
            if (remembered != null) {
                val mode = runCatching { BrowserFrontendPolicy.modeFor(remembered) }.getOrNull()
                if (mode != null) {
                    val candidate = BrowserFrontendPreference(mode, remembered.id)
                    if (BrowserFrontendPolicy.resolve(platform, candidate, activeTargets(platform)) != null) {
                        preferences[platform] = candidate
                        return
                    }
                }
            }
            val first = activeTargets(platform).firstOrNull()
            preferences[platform] = first?.let {
                BrowserFrontendPreference(BrowserFrontendPolicy.modeFor(it), it.id)
            } ?: BrowserFrontendPreference.CLEAN_ONLY
        }

        fun select(platform: ProxyPlatform, preference: BrowserFrontendPreference) {
            preferences[platform] = preference
        }

        fun restoreCategory(platform: ProxyPlatform, mode: BrowserConversionMode) {
            val ids = AlternativeFrontendCatalog.builtIn(platform)
                .filter { target ->
                    runCatching { BrowserFrontendPolicy.modeFor(target) }.getOrNull() == mode
                }
                .mapTo(mutableSetOf()) { it.id }
            disabledBuiltIns[platform] = disabledBuiltIns[platform].orEmpty() - ids
        }

        fun apply(): Boolean {
            val changes = if (repairInvalidState) preferences.toMap() else preferences.filter {
                (platform, value) -> expected[platform] != value
            }
            val restores = ProxyPlatform.entries.mapNotNull { platform ->
                val ids = initialDisabled[platform].orEmpty() - disabledBuiltIns[platform].orEmpty()
                ids.takeIf { it.isNotEmpty() }?.let { platform to it }
            }.toMap()
            val saved = preferencesManager.saveBrowserFrontendPreferences(changes, expected, restores)
            if (saved) refreshFromPreferences()
            return saved
        }
    }

    fun createDraft(preferencesManager: PreferencesManager): DraftState = DraftState(preferencesManager)

    fun populateContainer(
        context: Context,
        layoutInflater: LayoutInflater,
        container: LinearLayout,
        draft: DraftState,
        onChangeTarget: (ProxyPlatform) -> Unit,
    ): List<BrowserFrontendRow> {
        container.removeAllViews()
        return entries.map { entry ->
            val binding = ItemBrowserPrivacyPlatformBinding.inflate(layoutInflater, container, false)
            binding.switchBrowserPrivacyPlatform.id = entry.switchId
            bindRow(context, binding, entry, draft, onChangeTarget)
            container.addView(binding.root)
            BrowserFrontendRow(entry, binding)
        }
    }

    fun refreshRows(
        context: Context,
        rows: List<BrowserFrontendRow>,
        draft: DraftState,
        onChangeTarget: (ProxyPlatform) -> Unit,
    ) = rows.forEach { bindRow(context, it.binding, it.entry, draft, onChangeTarget) }

    private fun bindRow(
        context: Context,
        binding: ItemBrowserPrivacyPlatformBinding,
        entry: BrowserPlatformEntry,
        draft: DraftState,
        onChangeTarget: (ProxyPlatform) -> Unit,
    ) {
        val platform = entry.platform
        val preference = draft.preference(platform)
        val effective = draft.effectiveTarget(platform)
        binding.textViewPlatformLabel.setText(entry.labelRes)
        binding.textViewChangePrivacyTarget.setText(R.string.change_browser_frontend)
        binding.textViewChangePrivacyTarget.contentDescription = context.getString(
            R.string.change_browser_frontend_desc,
            context.getString(FrontendDisplayHelper.platformNameRes(platform)),
        )
        binding.textViewChangePrivacyTarget.setOnClickListener { onChangeTarget(platform) }
        binding.textViewPrivacyTargetStatus.isVisible = true
        binding.textViewPrivacyTargetStatus.text = when {
            preference.mode == BrowserConversionMode.CLEAN_ONLY ->
                context.getString(R.string.browser_frontend_clean_only)
            draft.isUsingFallback(platform) -> context.getString(
                R.string.browser_frontend_reader_fallback,
                FrontendDisplayHelper.displayLabel(effective!!),
                draft.savedTargetLabel(platform).orEmpty(),
            )
            effective != null -> context.getString(
                R.string.browser_frontend_selected,
                FrontendDisplayHelper.displayLabel(effective),
            )
            else -> context.getString(R.string.browser_frontend_unavailable)
        }
        binding.textViewPrivacyTargetWarning.isVisible =
            preference.mode != BrowserConversionMode.CLEAN_ONLY && effective == null
        if (binding.textViewPrivacyTargetWarning.isVisible) {
            binding.textViewPrivacyTargetWarning.setText(R.string.browser_frontend_unavailable_help)
        }
        binding.switchBrowserPrivacyPlatform.setOnCheckedChangeListener(null)
        binding.switchBrowserPrivacyPlatform.isChecked = preference.mode != BrowserConversionMode.CLEAN_ONLY
        binding.switchBrowserPrivacyPlatform.isEnabled = draft.activeTargets(platform).isNotEmpty()
        binding.switchBrowserPrivacyPlatform.setOnCheckedChangeListener { _, checked ->
            draft.setEnabled(platform, checked)
            bindRow(context, binding, entry, draft, onChangeTarget)
        }
    }
}
