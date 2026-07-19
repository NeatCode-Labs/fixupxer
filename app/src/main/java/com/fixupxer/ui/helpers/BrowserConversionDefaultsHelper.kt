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

package com.fixupxer.ui.helpers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.databinding.ItemBrowserPrivacyPlatformBinding
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.FrontendTarget
import com.fixupxer.utils.ProxyPlatform
import com.google.android.material.materialswitch.MaterialSwitch
import timber.log.Timber

/**
 * Builds the data-driven browser privacy conversion rows for Settings.
 */
object BrowserConversionDefaultsHelper {

    data class BrowserPlatformEntry(
        val platform: ProxyPlatform,
        val labelRes: Int,
        val switchId: Int,
        val getter: (PreferencesManager) -> Boolean,
        val setter: (PreferencesManager, Boolean) -> Unit,
    )

    val entries: List<BrowserPlatformEntry> = AlternativeFrontendCatalog
        .privacyCapablePlatforms()
        .mapNotNull { platform -> entryFor(platform) }

    private fun entryFor(platform: ProxyPlatform): BrowserPlatformEntry? = when (platform) {
        ProxyPlatform.X -> BrowserPlatformEntry(
            platform = platform,
            labelRes = R.string.convert_twitter_browser,
            switchId = R.id.switchBrowserTwitter,
            getter = { it.isBrowserConvertTwitterEnabled() },
            setter = { prefs, enabled -> prefs.setBrowserConvertTwitterEnabled(enabled) },
        )
        ProxyPlatform.BLUESKY -> BrowserPlatformEntry(
            platform = platform,
            labelRes = R.string.convert_bluesky_browser,
            switchId = R.id.switchBrowserBluesky,
            getter = { it.isBrowserConvertBlueskyEnabled() },
            setter = { prefs, enabled -> prefs.setBrowserConvertBlueskyEnabled(enabled) },
        )
        ProxyPlatform.REDDIT -> BrowserPlatformEntry(
            platform = platform,
            labelRes = R.string.convert_reddit_browser,
            switchId = R.id.switchBrowserReddit,
            getter = { it.isBrowserConvertRedditEnabled() },
            setter = { prefs, enabled -> prefs.setBrowserConvertRedditEnabled(enabled) },
        )
        ProxyPlatform.PINTEREST -> BrowserPlatformEntry(
            platform = platform,
            labelRes = R.string.convert_pinterest_browser,
            switchId = R.id.switchBrowserPinterest,
            getter = { it.isBrowserConvertPinterestEnabled() },
            setter = { prefs, enabled -> prefs.setBrowserConvertPinterestEnabled(enabled) },
        )
        else -> {
            Timber.w("No browser conversion defaults mapping for privacy platform %s", platform)
            null
        }
    }

    data class BrowserPrivacyRow(
        val entry: BrowserPlatformEntry,
        val binding: ItemBrowserPrivacyPlatformBinding,
    )

    class DraftState internal constructor(
        private val preferencesManager: PreferencesManager,
    ) {
        val draftTargetIds: MutableMap<ProxyPlatform, String?> = mutableMapOf()
        val draftToggles: MutableMap<ProxyPlatform, Boolean> = mutableMapOf()
        private val initialDisabledBuiltIns: Map<ProxyPlatform, Set<String>>

        init {
            entries.forEach { entry ->
                // The resolver already prefers the stored id when it is still an
                // active reader and falls back to the first active one otherwise,
                // so a stale (disabled) stored id never leaks into the draft.
                draftTargetIds[entry.platform] =
                    preferencesManager.resolveBrowserPrivacyTarget(entry.platform)?.id
                draftToggles[entry.platform] = entry.getter(preferencesManager)
            }
            initialDisabledBuiltIns = entries.associate { entry ->
                entry.platform to preferencesManager.getDisabledBuiltIns(entry.platform)
            }
        }

        fun draftTarget(platform: ProxyPlatform): FrontendTarget? {
            val draftId = draftTargetIds[platform] ?: return null
            val target = AlternativeFrontendCatalog.byId(draftId) ?: return null
            if (target.id in preferencesManager.getDisabledBuiltIns(platform)) return null
            return target
        }

        fun displayTarget(platform: ProxyPlatform): FrontendTarget? =
            draftTarget(platform) ?: preferencesManager.resolveBrowserPrivacyTarget(platform)

        fun updateDraftTarget(platform: ProxyPlatform, target: FrontendTarget) {
            draftTargetIds[platform] = target.id
        }

        fun apply(preferencesManager: PreferencesManager) {
            entries.forEach { entry ->
                val platform = entry.platform
                draftTargetIds[platform]?.let { targetId ->
                    preferencesManager.setBrowserPrivacyTargetId(platform, targetId)
                }
                if (displayTarget(platform) != null) {
                    entry.setter(preferencesManager, draftToggles[platform] == true)
                } else {
                    entry.setter(preferencesManager, false)
                }
            }
        }

        /**
         * Rolls back any in-dialog "Restore built-ins" roster change to the state
         * captured when the draft was created. Called on every non-Save dismissal;
         * must NOT be called after [apply], which commits the restored roster.
         */
        fun discardRosterChanges() {
            initialDisabledBuiltIns.forEach { (platform, ids) ->
                if (preferencesManager.getDisabledBuiltIns(platform) != ids) {
                    preferencesManager.setDisabledBuiltIns(platform, ids)
                }
            }
        }
    }

    fun createDraft(preferencesManager: PreferencesManager): DraftState =
        DraftState(preferencesManager)

    fun populateContainer(
        context: Context,
        layoutInflater: LayoutInflater,
        container: LinearLayout,
        draft: DraftState,
        onChangePrivacyTarget: (ProxyPlatform) -> Unit,
    ): List<BrowserPrivacyRow> {
        container.removeAllViews()
        return entries.map { entry ->
            val rowBinding = ItemBrowserPrivacyPlatformBinding.inflate(layoutInflater, container, false)
            rowBinding.switchBrowserPrivacyPlatform.id = entry.switchId
            bindRow(context, rowBinding, entry, draft, onChangePrivacyTarget)
            container.addView(rowBinding.root)
            BrowserPrivacyRow(entry, rowBinding)
        }
    }

    fun refreshRows(
        context: Context,
        rows: List<BrowserPrivacyRow>,
        draft: DraftState,
        onChangePrivacyTarget: (ProxyPlatform) -> Unit,
    ) {
        rows.forEach { row ->
            bindRow(context, row.binding, row.entry, draft, onChangePrivacyTarget)
        }
    }

    private fun bindRow(
        context: Context,
        binding: ItemBrowserPrivacyPlatformBinding,
        entry: BrowserPlatformEntry,
        draft: DraftState,
        onChangePrivacyTarget: (ProxyPlatform) -> Unit,
    ) {
        val platform = entry.platform
        val displayTarget = draft.displayTarget(platform)
        val hasActiveReader = displayTarget != null

        binding.textViewPlatformLabel.setText(entry.labelRes)
        binding.switchBrowserPrivacyPlatform.contentDescription = context.getString(entry.labelRes)

        binding.textViewChangePrivacyTarget.isVisible = true
        binding.textViewChangePrivacyTarget.contentDescription = context.getString(
            R.string.change_proxy_link_desc,
            context.getString(FrontendDisplayHelper.platformNameRes(platform)),
        )
        binding.textViewChangePrivacyTarget.setOnClickListener {
            onChangePrivacyTarget(platform)
        }

        if (hasActiveReader) {
            binding.textViewPrivacyTargetStatus.isVisible = true
            binding.textViewPrivacyTargetStatus.text = context.getString(
                R.string.browser_privacy_frontend_label,
                FrontendDisplayHelper.displayLabel(displayTarget!!),
            )
            binding.textViewPrivacyTargetWarning.isVisible = false

            binding.switchBrowserPrivacyPlatform.isEnabled = true
            binding.switchBrowserPrivacyPlatform.setOnCheckedChangeListener(null)
            binding.switchBrowserPrivacyPlatform.isChecked = draft.draftToggles[platform] == true
            binding.switchBrowserPrivacyPlatform.setOnCheckedChangeListener { _, isChecked ->
                draft.draftToggles[platform] = isChecked
            }
        } else {
            binding.textViewPrivacyTargetStatus.isVisible = false
            binding.textViewPrivacyTargetWarning.isVisible = true
            binding.textViewPrivacyTargetWarning.setText(R.string.browser_privacy_no_frontend)

            binding.switchBrowserPrivacyPlatform.isEnabled = false
            binding.switchBrowserPrivacyPlatform.setOnCheckedChangeListener(null)
            binding.switchBrowserPrivacyPlatform.isChecked = false
        }
    }
}
