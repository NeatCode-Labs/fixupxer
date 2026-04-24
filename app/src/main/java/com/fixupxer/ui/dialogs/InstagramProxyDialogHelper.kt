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

package com.fixupxer.ui.dialogs

import android.app.AlertDialog
import android.content.Context
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.utils.Constants

/**
 * Helper that shows a single-choice dialog letting the user pick
 * which Instagram embed proxy the app should use.
 *
 * Used as a fallback for builds where [com.fixupxer.ui.SettingsActivity]
 * is not registered in the manifest (F-Droid variant).
 */
object InstagramProxyDialogHelper {

    fun show(
        context: Context,
        prefs: PreferencesManager,
        onChanged: (String) -> Unit
    ) {
        val options = arrayOf(
            Constants.KKINSTAGRAM_DOMAIN,
            Constants.EEINSTAGRAM_DOMAIN,
            Constants.INSTAGRAM7_DOMAIN
        )
        val current = prefs.getInstagramProxy()
        val checked = options.indexOf(current).coerceAtLeast(0)

        AlertDialog.Builder(context)
            .setTitle(R.string.instagram_proxy_dialog_title)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val selected = options[which]
                prefs.setInstagramProxy(selected)
                onChanged(selected)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
