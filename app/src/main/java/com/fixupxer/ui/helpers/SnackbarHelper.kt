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

import android.view.View
import com.google.android.material.snackbar.Snackbar

/**
 * Short-lived Snackbar feedback anchored to a visible view hierarchy.
 */
object SnackbarHelper {

    fun showShort(anchor: View, message: CharSequence) {
        Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT).show()
    }

    fun showShortWithAction(
        anchor: View,
        message: CharSequence,
        actionLabel: CharSequence,
        onAction: () -> Unit
    ) {
        Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT)
            .setAction(actionLabel) { onAction() }
            .show()
    }
}
