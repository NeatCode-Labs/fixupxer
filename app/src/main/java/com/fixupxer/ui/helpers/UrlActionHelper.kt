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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.core.net.toUri
import com.fixupxer.R
import timber.log.Timber

/**
 * Shared copy / share / open actions for Main and Share screens.
 */
object UrlActionHelper {

    @SuppressLint("NewApi")
    fun copyToClipboard(anchor: View, activity: Activity, url: String) {
        if (url.isEmpty()) {
            SnackbarHelper.showShort(anchor, activity.getString(R.string.no_url_to_copy))
            return
        }
        try {
            val clipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboardManager == null) {
                Timber.e("ClipboardManager not available")
                SnackbarHelper.showShort(anchor, activity.getString(R.string.error_processing_url))
                return
            }
            val clipData = ClipData.newPlainText(activity.getString(R.string.clipboard_label_url), url)
            clipboardManager.setPrimaryClip(clipData)

            // Manual feedback only below Android 10 (API 29); Android 10+ shows a system notification.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                SnackbarHelper.showShort(anchor, activity.getString(R.string.url_copied))
            } else {
                Timber.d("URL copied to clipboard (Android 10+)")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error copying to clipboard")
            SnackbarHelper.showShort(anchor, activity.getString(R.string.error_copying_url))
        }
    }

    fun shareUrl(anchor: View, activity: Activity, url: String) {
        if (url.isEmpty()) {
            SnackbarHelper.showShort(anchor, activity.getString(R.string.no_url_to_share))
            return
        }
        try {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
                type = "text/plain"
            }
            activity.startActivity(Intent.createChooser(shareIntent, activity.getString(R.string.share_via)))
        } catch (e: Exception) {
            Timber.e(e, "Error sharing URL")
            SnackbarHelper.showShort(anchor, activity.getString(R.string.error_sharing_url))
        }
    }

    fun openUrl(anchor: View, activity: Activity, url: String) {
        if (url.isEmpty()) {
            SnackbarHelper.showShort(anchor, activity.getString(R.string.no_url_to_open))
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            activity.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Error opening URL")
            SnackbarHelper.showShort(anchor, activity.getString(R.string.error_browser))
        }
    }
}
