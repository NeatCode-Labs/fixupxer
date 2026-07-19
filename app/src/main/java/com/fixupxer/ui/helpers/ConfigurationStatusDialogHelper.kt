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
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.databinding.DialogConfigurationStatusBinding
import com.fixupxer.databinding.ItemConfigurationStatusRowBinding
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.DefaultBrowserStatus
import com.fixupxer.utils.ProxyPlatform
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.R as AppCompatR
import com.google.android.material.R as MaterialR

/**
 * Read-only snapshot of browser integration and related settings.
 */
object ConfigurationStatusDialogHelper {

    enum class IntegrationState {
        FULLY_OFF,
        ALIAS_OFF_IS_DEFAULT,
        ALIAS_ON_NOT_DEFAULT,
        ALIAS_ON_UNVERIFIED,
        OPERATIONAL,
    }

    enum class PrivacyRouteState {
        OPTIONAL_OFF,
        ACTIVE,
        BROKEN,
        MIXED,
    }

    enum class DetailSemanticType {
        ACTIVE,
        OPTIONAL_OFF,
        ATTENTION,
        INFO,
    }

    data class DetailLine(
        val text: String,
        val semanticType: DetailSemanticType,
    )

    fun resolveIntegrationState(
        aliasEnabled: Boolean,
        defaultStatus: DefaultBrowserStatus,
    ): IntegrationState = when {
        aliasEnabled && defaultStatus == DefaultBrowserStatus.OTHER_OR_UNSET ->
            IntegrationState.ALIAS_ON_NOT_DEFAULT
        aliasEnabled && defaultStatus == DefaultBrowserStatus.UNKNOWN ->
            IntegrationState.ALIAS_ON_UNVERIFIED
        !aliasEnabled && defaultStatus == DefaultBrowserStatus.FIXUPXER ->
            IntegrationState.ALIAS_OFF_IS_DEFAULT
        aliasEnabled && defaultStatus == DefaultBrowserStatus.FIXUPXER ->
            IntegrationState.OPERATIONAL
        else -> IntegrationState.FULLY_OFF
    }

    fun resolvePrivacyRouteState(preferencesManager: PreferencesManager): PrivacyRouteState {
        var activeCount = 0
        var brokenCount = 0

        BrowserConversionDefaultsHelper.entries.forEach { entry ->
            if (!entry.getter(preferencesManager)) return@forEach
            if (preferencesManager.resolveBrowserPrivacyTarget(entry.platform) != null) {
                activeCount++
            } else {
                brokenCount++
            }
        }

        return when {
            activeCount == 0 && brokenCount == 0 -> PrivacyRouteState.OPTIONAL_OFF
            activeCount > 0 && brokenCount == 0 -> PrivacyRouteState.ACTIVE
            activeCount == 0 && brokenCount > 0 -> PrivacyRouteState.BROKEN
            else -> PrivacyRouteState.MIXED
        }
    }

    fun summarize(
        aliasEnabled: Boolean,
        defaultStatus: DefaultBrowserStatus,
        privacyRouteState: PrivacyRouteState,
    ): Int {
        return when (resolveIntegrationState(aliasEnabled, defaultStatus)) {
            IntegrationState.ALIAS_ON_NOT_DEFAULT ->
                R.string.configuration_status_summary_alias_on_not_default
            IntegrationState.ALIAS_ON_UNVERIFIED ->
                R.string.configuration_status_summary_alias_on_unverified
            IntegrationState.ALIAS_OFF_IS_DEFAULT ->
                R.string.configuration_status_summary_alias_off_is_default
            IntegrationState.OPERATIONAL -> when (privacyRouteState) {
                PrivacyRouteState.BROKEN ->
                    R.string.configuration_status_summary_operational_broken
                PrivacyRouteState.MIXED ->
                    R.string.configuration_status_summary_operational_mixed
                PrivacyRouteState.ACTIVE ->
                    R.string.configuration_status_summary_active_with_conversions
                PrivacyRouteState.OPTIONAL_OFF ->
                    R.string.configuration_status_summary_active_cleaning_only
            }
            IntegrationState.FULLY_OFF ->
                R.string.configuration_status_summary_alias_off
        }
    }

    fun iconResFor(semanticType: DetailSemanticType): Int = when (semanticType) {
        DetailSemanticType.ACTIVE -> R.drawable.ic_check
        DetailSemanticType.ATTENTION -> R.drawable.ic_error
        DetailSemanticType.OPTIONAL_OFF,
        DetailSemanticType.INFO,
        -> R.drawable.ic_info_outline
    }

    fun iconTintAttrFor(semanticType: DetailSemanticType): Int = when (semanticType) {
        DetailSemanticType.ACTIVE -> AppCompatR.attr.colorPrimary
        DetailSemanticType.ATTENTION -> R.attr.configuration_status_attention_tint
        DetailSemanticType.OPTIONAL_OFF,
        DetailSemanticType.INFO,
        -> MaterialR.attr.colorOnSurfaceVariant
    }

    fun show(
        context: Context,
        layoutInflater: LayoutInflater,
        preferencesManager: PreferencesManager,
        customRulesEnabled: Boolean,
        enabledRulesCount: Int,
    ) {
        val aliasEnabled = BrowserModeUtils.isBrowserAliasEnabled(context)
        val defaultStatus = BrowserModeUtils.getDefaultBrowserStatus(context)
        val privacyRouteState = resolvePrivacyRouteState(preferencesManager)

        val binding = DialogConfigurationStatusBinding.inflate(layoutInflater)
        binding.textSummary.setText(
            summarize(aliasEnabled, defaultStatus, privacyRouteState),
        )
        populateDetails(
            context = context,
            layoutInflater = layoutInflater,
            container = binding.detailsContainer,
            aliasEnabled = aliasEnabled,
            defaultStatus = defaultStatus,
            preferencesManager = preferencesManager,
            customRulesEnabled = customRulesEnabled,
            enabledRulesCount = enabledRulesCount,
        )

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.configuration_status_title)
            .setView(binding.root)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun populateDetails(
        context: Context,
        layoutInflater: LayoutInflater,
        container: LinearLayout,
        aliasEnabled: Boolean,
        defaultStatus: DefaultBrowserStatus,
        preferencesManager: PreferencesManager,
        customRulesEnabled: Boolean,
        enabledRulesCount: Int,
    ) {
        container.removeAllViews()
        buildDetails(
            context = context,
            aliasEnabled = aliasEnabled,
            defaultStatus = defaultStatus,
            preferencesManager = preferencesManager,
            customRulesEnabled = customRulesEnabled,
            enabledRulesCount = enabledRulesCount,
        ).forEach { detail ->
            val rowBinding = ItemConfigurationStatusRowBinding.inflate(
                layoutInflater,
                container,
                false,
            )
            rowBinding.textStatusLine.text = detail.text
            rowBinding.imageStatusIcon.setImageResource(iconResFor(detail.semanticType))
            val tintColor = MaterialColors.getColor(
                rowBinding.imageStatusIcon,
                iconTintAttrFor(detail.semanticType),
            )
            rowBinding.imageStatusIcon.imageTintList = ColorStateList.valueOf(tintColor)
            container.addView(rowBinding.root)
        }
    }

    internal fun buildDetails(
        context: Context,
        aliasEnabled: Boolean,
        defaultStatus: DefaultBrowserStatus,
        preferencesManager: PreferencesManager,
        customRulesEnabled: Boolean,
        enabledRulesCount: Int,
    ): List<DetailLine> {
        val details = mutableListOf<DetailLine>()
        details += browserIntegrationDetail(context, aliasEnabled, defaultStatus)
        details += defaultBrowserDetail(context, aliasEnabled, defaultStatus)
        details += buildPrivacyConversionDetails(context, preferencesManager)
        details += customRulesDetail(context, customRulesEnabled, enabledRulesCount)
        details += afterCleanDetail(context, preferencesManager.getActionMode())
        return details
    }

    private fun browserIntegrationDetail(
        context: Context,
        aliasEnabled: Boolean,
        defaultStatus: DefaultBrowserStatus,
    ): DetailLine {
        return when {
            aliasEnabled ->
                DetailLine(
                    context.getString(R.string.configuration_status_detail_browser_on),
                    DetailSemanticType.ACTIVE,
                )
            defaultStatus == DefaultBrowserStatus.FIXUPXER ->
                DetailLine(
                    context.getString(R.string.configuration_status_detail_browser_off_conflict),
                    DetailSemanticType.ATTENTION,
                )
            else ->
                DetailLine(
                    context.getString(R.string.configuration_status_detail_browser_off),
                    DetailSemanticType.OPTIONAL_OFF,
                )
        }
    }

    private fun defaultBrowserDetail(
        context: Context,
        aliasEnabled: Boolean,
        defaultStatus: DefaultBrowserStatus,
    ): DetailLine = when (defaultStatus) {
        DefaultBrowserStatus.FIXUPXER ->
            DetailLine(
                context.getString(R.string.configuration_status_detail_default_fixupxer),
                DetailSemanticType.ACTIVE,
            )
        DefaultBrowserStatus.OTHER_OR_UNSET ->
            if (aliasEnabled) {
                DetailLine(
                    context.getString(R.string.configuration_status_detail_default_other_attention),
                    DetailSemanticType.ATTENTION,
                )
            } else {
                DetailLine(
                    context.getString(R.string.configuration_status_detail_default_other),
                    DetailSemanticType.INFO,
                )
            }
        DefaultBrowserStatus.UNKNOWN ->
            DetailLine(
                context.getString(R.string.configuration_status_detail_default_unknown),
                DetailSemanticType.INFO,
            )
    }

    private fun buildPrivacyConversionDetails(
        context: Context,
        preferencesManager: PreferencesManager,
    ): List<DetailLine> {
        data class PrivacyDetailPart(
            val platform: ProxyPlatform,
            val platformName: String,
            val targetLabel: String?,
        )

        val activeParts = mutableListOf<PrivacyDetailPart>()
        val brokenParts = mutableListOf<PrivacyDetailPart>()

        BrowserConversionDefaultsHelper.entries.forEach { entry ->
            if (!entry.getter(preferencesManager)) return@forEach
            val platformName = context.getString(FrontendDisplayHelper.platformNameRes(entry.platform))
            val target = preferencesManager.resolveBrowserPrivacyTarget(entry.platform)
            if (target != null) {
                activeParts += PrivacyDetailPart(
                    platform = entry.platform,
                    platformName = platformName,
                    targetLabel = FrontendDisplayHelper.displayLabel(target),
                )
            } else {
                brokenParts += PrivacyDetailPart(
                    platform = entry.platform,
                    platformName = platformName,
                    targetLabel = null,
                )
            }
        }

        if (activeParts.isEmpty() && brokenParts.isEmpty()) {
            return listOf(
                DetailLine(
                    context.getString(R.string.configuration_status_privacy_none),
                    DetailSemanticType.OPTIONAL_OFF,
                ),
            )
        }

        val lines = mutableListOf<DetailLine>()
        activeParts.forEach { part ->
            lines += DetailLine(
                text = context.getString(
                    R.string.configuration_status_privacy_route,
                    part.platformName,
                    part.targetLabel,
                ),
                semanticType = DetailSemanticType.ACTIVE,
            )
        }
        brokenParts.forEach { part ->
            val brokenRes = if (hasPrivacyRecoveryPath(part.platform, preferencesManager)) {
                R.string.configuration_status_privacy_broken_restore
            } else {
                R.string.configuration_status_privacy_broken
            }
            lines += DetailLine(
                text = context.getString(brokenRes, part.platformName),
                semanticType = DetailSemanticType.ATTENTION,
            )
        }
        return lines
    }

    internal fun hasPrivacyRecoveryPath(
        platform: ProxyPlatform,
        preferencesManager: PreferencesManager,
    ): Boolean {
        // Restore in the Browser privacy picker re-enables built-in Readers only,
        // so recovery guidance applies only when a disabled Reader exists.
        val disabled = preferencesManager.getDisabledBuiltIns(platform)
        return AlternativeFrontendCatalog.builtInReaders(platform).any { it.id in disabled }
    }

    private fun customRulesDetail(
        context: Context,
        customRulesEnabled: Boolean,
        enabledRulesCount: Int,
    ): DetailLine {
        return if (customRulesEnabled) {
            DetailLine(
                context.getString(
                    R.string.configuration_status_custom_rules_on,
                    context.resources.getQuantityString(
                        R.plurals.custom_rules_count,
                        enabledRulesCount,
                        enabledRulesCount,
                    ),
                ),
                DetailSemanticType.ACTIVE,
            )
        } else {
            DetailLine(
                context.getString(R.string.configuration_status_custom_rules_off),
                DetailSemanticType.OPTIONAL_OFF,
            )
        }
    }

    private fun afterCleanDetail(context: Context, actionMode: String): DetailLine {
        val modeLabel = when (actionMode) {
            PreferencesManager.ACTION_MODE_PRIORITY ->
                context.getString(R.string.action_mode_priority)
            else -> context.getString(R.string.action_mode_ask)
        }
        return DetailLine(
            context.getString(R.string.configuration_status_detail_after_clean, modeLabel),
            DetailSemanticType.INFO,
        )
    }
}
