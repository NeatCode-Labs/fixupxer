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

package com.fixupxer.utils

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import com.fixupxer.R

/** Clipboard policy shared by every action that copies a processed URL. */
object UrlClipboard {
    val needsAppFeedback: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    /** Throws if copying fails so callers can display an error instead of success. */
    fun copy(context: Context, url: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: error("ClipboardManager not available")
        val clip = ClipData.newPlainText(context.getString(R.string.clipboard_label_url), url)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Even a cleaned URL can contain a private path or a functional token.
            // The compatible key hides that content from system copy previews.
            val sensitiveKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ClipDescription.EXTRA_IS_SENSITIVE
            } else {
                "android.content.extra.IS_SENSITIVE"
            }
            clip.description.extras = PersistableBundle().apply {
                putBoolean(sensitiveKey, true)
            }
        }
        clipboard.setPrimaryClip(clip)
    }
}
