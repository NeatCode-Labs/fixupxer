// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
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
import com.fixupxer.ui.helpers.ConfigurationStatusDialogHelper
import com.fixupxer.ui.helpers.ConfigurationStatusDialogHelper.DetailSemanticType
import com.fixupxer.ui.helpers.ConfigurationStatusDialogHelper.IntegrationState
import com.fixupxer.ui.helpers.ConfigurationStatusDialogHelper.PrivacyRouteState
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.Constants
import com.fixupxer.utils.DefaultBrowserStatus
import com.fixupxer.utils.InstagramProxyStore
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import com.fixupxer.utils.TikTokProxyStore
import androidx.appcompat.R as AppCompatR
import com.google.android.material.R as MaterialR
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ConfigurationStatusSummaryTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setUp() {
        ProxyRoster.reset()
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        preferencesManager = PreferencesManager(context)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        ProxyRoster.reset()
        InstagramProxyStore.reset()
        TikTokProxyStore.reset()
    }

    @Test
    fun `summary matrix covers all alias default and privacy combinations`() {
        val aliasValues = listOf(true, false)
        val defaultStatuses = DefaultBrowserStatus.entries
        val privacyStates = PrivacyRouteState.entries

        aliasValues.forEach { aliasEnabled ->
            defaultStatuses.forEach { defaultStatus ->
                privacyStates.forEach { privacyState ->
                    val expected = expectedSummary(aliasEnabled, defaultStatus, privacyState)
                    val actual = ConfigurationStatusDialogHelper.summarize(
                        aliasEnabled = aliasEnabled,
                        defaultStatus = defaultStatus,
                        privacyRouteState = privacyState,
                    )
                    assertEquals(
                        "alias=$aliasEnabled default=$defaultStatus privacy=$privacyState",
                        expected,
                        actual,
                    )
                }
            }
        }
    }

    @Test
    fun `integration state resolves alias and default browser combinations`() {
        assertEquals(
            IntegrationState.ALIAS_ON_NOT_DEFAULT,
            ConfigurationStatusDialogHelper.resolveIntegrationState(true, DefaultBrowserStatus.OTHER_OR_UNSET),
        )
        assertEquals(
            IntegrationState.ALIAS_ON_UNVERIFIED,
            ConfigurationStatusDialogHelper.resolveIntegrationState(true, DefaultBrowserStatus.UNKNOWN),
        )
        assertEquals(
            IntegrationState.ALIAS_OFF_IS_DEFAULT,
            ConfigurationStatusDialogHelper.resolveIntegrationState(false, DefaultBrowserStatus.FIXUPXER),
        )
        assertEquals(
            IntegrationState.OPERATIONAL,
            ConfigurationStatusDialogHelper.resolveIntegrationState(true, DefaultBrowserStatus.FIXUPXER),
        )
        assertEquals(
            IntegrationState.FULLY_OFF,
            ConfigurationStatusDialogHelper.resolveIntegrationState(false, DefaultBrowserStatus.OTHER_OR_UNSET),
        )
    }

    @Test
    fun `privacy route state resolves optional off active broken and mixed`() {
        assertEquals(
            PrivacyRouteState.OPTIONAL_OFF,
            ConfigurationStatusDialogHelper.resolvePrivacyRouteState(preferencesManager),
        )

        preferencesManager.setBrowserConvertTwitterEnabled(true)
        assertEquals(
            PrivacyRouteState.ACTIVE,
            ConfigurationStatusDialogHelper.resolvePrivacyRouteState(preferencesManager),
        )

        disableAllReaders(ProxyPlatform.X)
        assertEquals(
            PrivacyRouteState.BROKEN,
            ConfigurationStatusDialogHelper.resolvePrivacyRouteState(preferencesManager),
        )

        preferencesManager.setBrowserConvertBlueskyEnabled(true)
        assertEquals(
            PrivacyRouteState.MIXED,
            ConfigurationStatusDialogHelper.resolvePrivacyRouteState(preferencesManager),
        )
    }

    @Test
    fun `privacy route state works for all four reader platforms`() {
        val platforms = listOf(
            ProxyPlatform.X to { preferencesManager.setBrowserConvertTwitterEnabled(true) },
            ProxyPlatform.BLUESKY to { preferencesManager.setBrowserConvertBlueskyEnabled(true) },
            ProxyPlatform.REDDIT to { preferencesManager.setBrowserConvertRedditEnabled(true) },
            ProxyPlatform.PINTEREST to { preferencesManager.setBrowserConvertPinterestEnabled(true) },
        )

        platforms.forEach { (platform, enable) ->
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
            preferencesManager = PreferencesManager(context)
            enable()
            assertEquals(
                platform.name,
                PrivacyRouteState.ACTIVE,
                ConfigurationStatusDialogHelper.resolvePrivacyRouteState(preferencesManager),
            )

            disableAllReaders(platform)
            assertEquals(
                platform.name,
                PrivacyRouteState.BROKEN,
                ConfigurationStatusDialogHelper.resolvePrivacyRouteState(preferencesManager),
            )
        }
    }

    @Test
    fun `semantic type maps to drawable and theme color attributes`() {
        assertEquals(R.drawable.ic_check, ConfigurationStatusDialogHelper.iconResFor(DetailSemanticType.ACTIVE))
        assertEquals(R.drawable.ic_error, ConfigurationStatusDialogHelper.iconResFor(DetailSemanticType.ATTENTION))
        assertEquals(
            R.drawable.ic_info_outline,
            ConfigurationStatusDialogHelper.iconResFor(DetailSemanticType.OPTIONAL_OFF),
        )
        assertEquals(
            R.drawable.ic_info_outline,
            ConfigurationStatusDialogHelper.iconResFor(DetailSemanticType.INFO),
        )

        assertEquals(
            AppCompatR.attr.colorPrimary,
            ConfigurationStatusDialogHelper.iconTintAttrFor(DetailSemanticType.ACTIVE),
        )
        assertEquals(
            R.attr.configuration_status_attention_tint,
            ConfigurationStatusDialogHelper.iconTintAttrFor(DetailSemanticType.ATTENTION),
        )
        assertEquals(
            MaterialR.attr.colorOnSurfaceVariant,
            ConfigurationStatusDialogHelper.iconTintAttrFor(DetailSemanticType.OPTIONAL_OFF),
        )
        assertEquals(
            MaterialR.attr.colorOnSurfaceVariant,
            ConfigurationStatusDialogHelper.iconTintAttrFor(DetailSemanticType.INFO),
        )
    }

    @Test
    fun `details show active privacy route with semantic type`() {
        preferencesManager.setBrowserConvertTwitterEnabled(true)

        val privacyLine = buildDetails().first {
            it.text.startsWith(context.getString(R.string.configuration_status_label_privacy_conversions))
        }
        assertEquals(
            context.getString(
                R.string.configuration_status_privacy_route,
                context.getString(R.string.platform_name_x),
                Constants.XCANCEL_DOMAIN,
            ),
            privacyLine.text,
        )
        assertEquals(DetailSemanticType.ACTIVE, privacyLine.semanticType)
    }

    @Test
    fun `details warn when enabled privacy platform has no active reader`() {
        preferencesManager.setBrowserConvertTwitterEnabled(true)
        disableAllReaders(ProxyPlatform.X)

        val privacyLine = buildDetails().first {
            it.text.contains(context.getString(R.string.platform_name_x))
        }
        assertTrue(privacyLine.text.contains("Restore built-in proxies"))
        assertEquals(DetailSemanticType.ATTENTION, privacyLine.semanticType)
    }

    @Test
    fun `hasPrivacyRecoveryPath reflects disabled built-in readers`() {
        assertEquals(
            false,
            ConfigurationStatusDialogHelper.hasPrivacyRecoveryPath(ProxyPlatform.X, preferencesManager),
        )

        disableAllReaders(ProxyPlatform.X)
        assertEquals(
            true,
            ConfigurationStatusDialogHelper.hasPrivacyRecoveryPath(ProxyPlatform.X, preferencesManager),
        )
    }

    @Test
    fun `details show none when no privacy conversion is enabled`() {
        val privacyLine = buildDetails().first {
            it.text.startsWith(context.getString(R.string.configuration_status_label_privacy_conversions))
        }
        assertEquals(
            context.getString(R.string.configuration_status_privacy_none),
            privacyLine.text,
        )
        assertEquals(DetailSemanticType.OPTIONAL_OFF, privacyLine.semanticType)
    }

    @Test
    fun `details map browser integration off as optional unless default role conflicts`() {
        val optionalOff = buildDetails(aliasEnabled = false, defaultStatus = DefaultBrowserStatus.OTHER_OR_UNSET)
            .first { it.text.startsWith(context.getString(R.string.configuration_status_label_browser_integration)) }
        assertEquals(DetailSemanticType.OPTIONAL_OFF, optionalOff.semanticType)

        val conflict = buildDetails(aliasEnabled = false, defaultStatus = DefaultBrowserStatus.FIXUPXER)
            .first { it.text.startsWith(context.getString(R.string.configuration_status_label_browser_integration)) }
        assertEquals(DetailSemanticType.ATTENTION, conflict.semanticType)
    }

    @Test
    fun `details map default browser other as attention only when alias is on`() {
        val attention = buildDetails(aliasEnabled = true, defaultStatus = DefaultBrowserStatus.OTHER_OR_UNSET)
            .first { it.text.startsWith(context.getString(R.string.configuration_status_label_default_browser)) }
        assertEquals(DetailSemanticType.ATTENTION, attention.semanticType)

        val info = buildDetails(aliasEnabled = false, defaultStatus = DefaultBrowserStatus.OTHER_OR_UNSET)
            .first { it.text.startsWith(context.getString(R.string.configuration_status_label_default_browser)) }
        assertEquals(DetailSemanticType.INFO, info.semanticType)
    }

    @Test
    fun `details show custom rules count when on and optional off when disabled`() {
        listOf(1, 2).forEach { count ->
            val enabledDetails = buildDetails(customRulesEnabled = true, enabledRulesCount = count)
            val enabledLine = enabledDetails.first { it.text.startsWith(context.getString(R.string.configuration_status_label_custom_rules)) }
            assertEquals(
                context.getString(
                    R.string.configuration_status_custom_rules_on,
                    context.resources.getQuantityString(
                        R.plurals.custom_rules_count,
                        count,
                        count,
                    ),
                ),
                enabledLine.text,
            )
            assertEquals(DetailSemanticType.ACTIVE, enabledLine.semanticType)
        }

        val disabledLine = buildDetails(customRulesEnabled = false, enabledRulesCount = 2)
            .first { it.text.startsWith(context.getString(R.string.configuration_status_label_custom_rules)) }
        assertEquals(
            context.getString(R.string.configuration_status_custom_rules_off),
            disabledLine.text,
        )
        assertEquals(DetailSemanticType.OPTIONAL_OFF, disabledLine.semanticType)
    }

    @Test
    fun `details show both after-clean modes as info`() {
        val askLine = buildDetails().first {
            it.text.startsWith(context.getString(R.string.configuration_status_label_after_clean))
        }
        assertEquals(
            context.getString(
                R.string.configuration_status_detail_after_clean,
                context.getString(R.string.action_mode_ask),
            ),
            askLine.text,
        )
        assertEquals(DetailSemanticType.INFO, askLine.semanticType)

        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        val priorityLine = buildDetails().first {
            it.text.startsWith(context.getString(R.string.configuration_status_label_after_clean))
        }
        assertEquals(
            context.getString(
                R.string.configuration_status_detail_after_clean,
                context.getString(R.string.action_mode_priority),
            ),
            priorityLine.text,
        )
        assertEquals(DetailSemanticType.INFO, priorityLine.semanticType)
    }

    private fun expectedSummary(
        aliasEnabled: Boolean,
        defaultStatus: DefaultBrowserStatus,
        privacyRouteState: PrivacyRouteState,
    ): Int = when {
        aliasEnabled && defaultStatus == DefaultBrowserStatus.OTHER_OR_UNSET ->
            R.string.configuration_status_summary_alias_on_not_default
        aliasEnabled && defaultStatus == DefaultBrowserStatus.UNKNOWN ->
            R.string.configuration_status_summary_alias_on_unverified
        !aliasEnabled && defaultStatus == DefaultBrowserStatus.FIXUPXER ->
            R.string.configuration_status_summary_alias_off_is_default
        aliasEnabled &&
            defaultStatus == DefaultBrowserStatus.FIXUPXER &&
            privacyRouteState == PrivacyRouteState.BROKEN ->
            R.string.configuration_status_summary_operational_broken
        aliasEnabled &&
            defaultStatus == DefaultBrowserStatus.FIXUPXER &&
            privacyRouteState == PrivacyRouteState.MIXED ->
            R.string.configuration_status_summary_operational_mixed
        aliasEnabled &&
            defaultStatus == DefaultBrowserStatus.FIXUPXER &&
            privacyRouteState == PrivacyRouteState.ACTIVE ->
            R.string.configuration_status_summary_active_with_conversions
        aliasEnabled &&
            defaultStatus == DefaultBrowserStatus.FIXUPXER &&
            privacyRouteState == PrivacyRouteState.OPTIONAL_OFF ->
            R.string.configuration_status_summary_active_cleaning_only
        else ->
            R.string.configuration_status_summary_alias_off
    }

    private fun disableAllReaders(platform: ProxyPlatform) {
        AlternativeFrontendCatalog.builtInReaders(platform).forEach { reader ->
            preferencesManager.disableBuiltIn(platform, reader.id)
        }
    }

    private fun buildDetails(
        aliasEnabled: Boolean = false,
        defaultStatus: DefaultBrowserStatus = DefaultBrowserStatus.OTHER_OR_UNSET,
        customRulesEnabled: Boolean = false,
        enabledRulesCount: Int = 0,
    ): List<ConfigurationStatusDialogHelper.DetailLine> =
        ConfigurationStatusDialogHelper.buildDetails(
            context = context,
            aliasEnabled = aliasEnabled,
            defaultStatus = defaultStatus,
            preferencesManager = preferencesManager,
            customRulesEnabled = customRulesEnabled,
            enabledRulesCount = enabledRulesCount,
        )

    private companion object {
        const val PREFS_NAME = "FixupXerPrefs"
    }
}
