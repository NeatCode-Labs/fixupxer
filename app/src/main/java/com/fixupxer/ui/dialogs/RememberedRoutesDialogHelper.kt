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

package com.fixupxer.ui.dialogs

import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.databinding.DialogRememberedDestinationsBinding
import com.fixupxer.ui.adapters.SavedAppChoiceRow
import com.fixupxer.ui.adapters.SavedAppChoicesAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object RememberedRoutesDialogHelper {

    fun show(
        context: Context,
        preferencesManager: PreferencesManager,
        onChanged: () -> Unit = {},
    ) {
        val binding = DialogRememberedDestinationsBinding.inflate(
            android.view.LayoutInflater.from(context)
        )
        lateinit var refresh: () -> Unit
        lateinit var dialog: androidx.appcompat.app.AlertDialog
        val adapter = SavedAppChoicesAdapter { host ->
            confirmDeleteOne(context, host) {
                preferencesManager.removeRememberedRoute(host)
                onChanged()
                refresh()
            }
        }
        binding.recyclerViewSavedAppChoices.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }

        dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.saved_app_choices)
            .setView(binding.root)
            .setPositiveButton(R.string.remembered_destinations_delete_all, null)
            .setNegativeButton(R.string.close, null)
            .create()

        refresh = {
            val rows = preferencesManager.getRememberedRoutes()
                .toSortedMap()
                .map { (host, route) ->
                    val app = resolveApp(context.packageManager, route.packageName)
                    SavedAppChoiceRow(
                        host = host,
                        route = route,
                        appLabel = app.first
                            ?: context.getString(R.string.saved_app_choice_label_unavailable),
                        installed = app.second,
                    )
                }
            adapter.submitList(rows)
            binding.textRememberedDestinationsEmpty.isVisible = rows.isEmpty()
            binding.recyclerViewSavedAppChoices.isVisible = rows.isNotEmpty()
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.isVisible = rows.isNotEmpty()
        }

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                confirmDeleteAll(context) {
                    preferencesManager.clearRememberedRoutes()
                    onChanged()
                    refresh()
                }
            }
            refresh()
        }
        refresh()
        dialog.show()
    }

    private fun confirmDeleteOne(context: Context, host: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.remembered_destinations_delete_one_title)
            .setMessage(context.getString(R.string.remembered_destinations_delete_one_message, host))
            .setPositiveButton(R.string.custom_rule_delete) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDeleteAll(context: Context, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.remembered_destinations_delete_all_title)
            .setMessage(R.string.remembered_destinations_delete_all_message)
            .setPositiveButton(R.string.custom_rule_delete) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun resolveApp(
        packageManager: PackageManager,
        packageName: String,
    ): Pair<String?, Boolean> =
        try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString() to true
        } catch (_: PackageManager.NameNotFoundException) {
            null to false
        }
}
