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

package com.fixupxer.ui.dialogs

import android.content.Context
import com.fixupxer.R
import com.fixupxer.processing.LeakCategory
import com.fixupxer.processing.LeakComponent
import com.fixupxer.processing.LeakFinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Shows an informational summary of sensitive data which remains in a processed link. */
object LinkGuardDialogHelper {

    fun show(
        context: Context,
        findings: List<LeakFinding>,
        onRemoveParameter: (Set<String>) -> Unit,
        onBack: () -> Unit
    ) {
        if (findings.isEmpty()) return

        val removableNames = findings
            .asSequence()
            .filter { it.component == LeakComponent.QUERY }
            .mapNotNull { it.parameterName }
            .toSet()
        val message = listOf(
            context.getString(R.string.leak_warning_body),
            formatFindings(context, findings)
        ).filter { it.isNotBlank() }.joinToString(separator = "\n\n")

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.leak_dialog_title)
            .setMessage(message)
            .setPositiveButton(R.string.leak_continue_anyway, null)
            .setNegativeButton(R.string.leak_back) { _, _ -> onBack() }
            .apply {
                if (removableNames.isNotEmpty()) {
                    setNeutralButton(R.string.leak_remove_parameter) { _, _ ->
                        confirmRemoval(context, removableNames, onRemoveParameter)
                    }
                }
            }
            .show()
    }

    private fun confirmRemoval(
        context: Context,
        names: Set<String>,
        onRemoveParameter: (Set<String>) -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.leak_dialog_title)
            .setMessage(R.string.leak_remove_param_warning)
            .setPositiveButton(R.string.leak_remove_parameter) { _, _ -> onRemoveParameter(names) }
            .setNegativeButton(R.string.leak_back, null)
            .show()
    }

    private fun formatFindings(context: Context, findings: List<LeakFinding>): String {
        val coordinatesByComponent = findings
            .filter { it.category == LeakCategory.COORDINATES }
            .groupBy { it.component }

        return findings
            .asSequence()
            .filterNot { finding ->
                finding.category == LeakCategory.COORDINATES &&
                    coordinatesByComponent[finding.component]?.firstOrNull() != finding
            }
            .mapNotNull { finding ->
                when (finding.category) {
                    LeakCategory.CREDENTIALS ->
                        context.getString(R.string.leak_finding_credentials)
                    LeakCategory.EMAIL -> when (finding.component) {
                        LeakComponent.PATH ->
                            context.getString(R.string.leak_finding_email_path)
                        LeakComponent.QUERY ->
                            finding.parameterName?.let {
                                context.getString(R.string.leak_finding_email_parameter, it)
                            } ?: context.getString(R.string.leak_finding_email_query)
                        LeakComponent.FRAGMENT ->
                            context.getString(R.string.leak_finding_email_fragment)
                        LeakComponent.USERINFO -> null
                    }
                    LeakCategory.JWT -> context.getString(R.string.leak_finding_jwt)
                    LeakCategory.TOKEN_PARAM ->
                        finding.parameterName?.let {
                            context.getString(R.string.leak_finding_token_parameter, it)
                        }
                    LeakCategory.COORDINATES -> {
                        val names = coordinatesByComponent[finding.component]
                            .orEmpty()
                            .mapNotNull { it.parameterName }
                        when {
                            names.size >= 2 -> context.getString(
                                R.string.leak_finding_coordinates,
                                names[0],
                                names[1]
                            )
                            names.size == 1 ->
                                context.getString(R.string.leak_finding_coordinate, names.single())
                            else -> null
                        }
                    }
                }
            }
            .joinToString(separator = "\n")
    }
}
