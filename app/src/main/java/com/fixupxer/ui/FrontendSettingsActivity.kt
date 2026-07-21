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

package com.fixupxer.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.databinding.ActivityFrontendSettingsBinding
import com.fixupxer.databinding.ItemFrontendPlatformBinding
import com.fixupxer.ui.dialogs.ProxyPickerDialogHelper
import com.fixupxer.ui.helpers.FrontendDisplayHelper
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FrontendSettingsActivity : BaseActivity() {

    private lateinit var binding: ActivityFrontendSettingsBinding
    private val platformRows = linkedMapOf<ProxyPlatform, ItemFrontendPlatformBinding>()

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFrontendSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        inflatePlatformRows()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            refreshAllRows()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun inflatePlatformRows() {
        val container = binding.frontendPlatformsContainer
        val inflater = layoutInflater
        ProxyPlatform.entries.forEachIndexed { index, platform ->
            val rowBinding = ItemFrontendPlatformBinding.inflate(inflater, container, true)
            rowBinding.textPlatformMonogram.setText(FrontendDisplayHelper.monogramRes(platform))
            rowBinding.textPlatformName.setText(FrontendDisplayHelper.platformNameRes(platform))
            rowBinding.platformRowDivider.visibility =
                if (index == ProxyPlatform.entries.lastIndex) View.GONE else View.VISIBLE
            rowBinding.root.setOnClickListener {
                ProxyPickerDialogHelper.show(
                    context = this,
                    layoutInflater = layoutInflater,
                    platform = platform,
                    preferencesManager = preferencesManager,
                ) {
                    refreshRow(platform)
                }
            }
            platformRows[platform] = rowBinding
        }
        refreshAllRows()
    }

    private fun refreshAllRows() {
        ProxyPlatform.entries.forEach(::refreshRow)
    }

    private fun refreshRow(platform: ProxyPlatform) {
        val rowBinding = platformRows[platform] ?: return
        val summary = summaryForPlatform(platform)
        rowBinding.textFrontendSummary.text = summary
        rowBinding.root.contentDescription = getString(
            R.string.frontend_platform_row_desc,
            getString(FrontendDisplayHelper.platformNameRes(platform)),
            summary,
        )
    }

    private fun summaryForPlatform(platform: ProxyPlatform): String {
        val active = ProxyRoster.activeTargets(platform)
        val selectedDomain = preferencesManager.getSelectedProxyDomain(platform)
        val selectedTarget = selectedDomain?.let { ProxyRoster.targetByDomain(platform, it) }
            ?: active.firstOrNull()
        return if (selectedTarget != null) {
            FrontendDisplayHelper.displayLabel(selectedTarget)
        } else {
            getString(R.string.frontend_not_configured)
        }
    }
}
