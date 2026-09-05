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

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.core.net.toUri
import com.fixupxer.R
import com.fixupxer.utils.Constants
import com.fixupxer.utils.UrlClipboard
import timber.log.Timber

/**
 * Shared copy / share / open actions for Main and Share screens.
 */
object UrlActionHelper {

    fun copyToClipboard(anchor: View, activity: Context, url: String) {
        if (url.isEmpty()) {
            SnackbarHelper.showShort(anchor, activity.getString(R.string.no_url_to_copy))
            return
        }
        try {
            UrlClipboard.copy(activity, url)
            if (UrlClipboard.needsAppFeedback) {
                SnackbarHelper.showShort(anchor, activity.getString(R.string.url_copied))
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
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.packageManager.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                activity.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            val resolvedPackage = resolveInfo?.activityInfo?.packageName
            if (shouldRedirectSelfOpen(resolvedPackage, activity.packageName)) {
                openUrlInExternalBrowser(anchor, activity, url)
                return
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Error opening URL")
            SnackbarHelper.showShort(anchor, activity.getString(R.string.error_browser))
        }
    }

    /**
     * Opens an HTTP(S) URL outside FixupXer. Explicit external components prevent
     * BrowserAlias from intercepting guides or defensive VIEW-intent handoffs.
     */
    fun openUrlInExternalBrowser(anchor: View, activity: Activity, url: String): Boolean {
        if (url.isEmpty()) {
            SnackbarHelper.showShort(anchor, activity.getString(R.string.no_url_to_open))
            return false
        }

        return try {
            val baseIntent = Intent(Intent.ACTION_VIEW, url.toUri())
                .addCategory(Intent.CATEGORY_BROWSABLE)
            val packageManager = activity.packageManager
            val selectorPackages = packageManager.queryIntentActivities(
                Intent.makeMainSelectorActivity(
                    Intent.ACTION_MAIN,
                    Intent.CATEGORY_APP_BROWSER,
                ),
                0,
            ).mapNotNull { it.activityInfo?.packageName }
            // Some OEM browsers do not advertise CATEGORY_APP_BROWSER. A neutral
            // browsable HTTP VIEW probe safely discovers those packages.
            val actionViewPackages = packageManager.queryIntentActivities(
                Intent(Intent.ACTION_VIEW, Constants.BROWSER_PROBE_URL.toUri())
                    .addCategory(Intent.CATEGORY_BROWSABLE),
                0,
            ).mapNotNull { it.activityInfo?.packageName }
            val browserPackages = mergeExternalBrowserPackages(
                selectorPackages,
                actionViewPackages,
                activity.packageName,
            )

            val directCandidates = packageManager.queryIntentActivities(baseIntent, 0)
                .mapNotNull { resolveInfo ->
                    val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                    if (activityInfo.packageName == activity.packageName) return@mapNotNull null
                    if (activityInfo.packageName !in browserPackages) return@mapNotNull null
                    Intent(baseIntent).setComponent(
                        ComponentName(activityInfo.packageName, activityInfo.name)
                    )
                }
            val packageFallbackCandidates = browserPackages.mapNotNull { packageName ->
                val fallback = Intent(baseIntent).setPackage(packageName)
                val activityInfo = packageManager.resolveActivity(fallback, 0)?.activityInfo
                    ?: return@mapNotNull null
                if (activityInfo.packageName == activity.packageName ||
                    activityInfo.packageName !in browserPackages
                ) {
                    return@mapNotNull null
                }
                Intent(baseIntent).setComponent(
                    ComponentName(activityInfo.packageName, activityInfo.name)
                )
            }
            val candidates = (directCandidates + packageFallbackCandidates)
                .distinctBy { it.component }

            if (candidates.isEmpty()) {
                SnackbarHelper.showShort(anchor, activity.getString(R.string.error_browser))
                return false
            }

            val resolvedDefault = packageManager.resolveActivity(baseIntent, 0)
                ?.activityInfo
                ?.let { ComponentName(it.packageName, it.name) }
            val ordered = candidates.sortedByDescending { it.component == resolvedDefault }

            if (ordered.size == 1 || ordered.first().component == resolvedDefault) {
                activity.startActivity(ordered.first())
            } else {
                val chooser = Intent.createChooser(
                    ordered.first(),
                    activity.getString(R.string.chooser_open_with_browser),
                )
                if (ordered.size > 1) {
                    chooser.putExtra(
                        Intent.EXTRA_INITIAL_INTENTS,
                        ordered.drop(1).toTypedArray(),
                    )
                }
                activity.startActivity(chooser)
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Error opening URL in an external browser")
            SnackbarHelper.showShort(anchor, activity.getString(R.string.error_browser))
            false
        }
    }

    internal fun shouldRedirectSelfOpen(resolvedPackage: String?, ownPackage: String): Boolean =
        resolvedPackage != null && resolvedPackage == ownPackage

    internal fun mergeExternalBrowserPackages(
        selectorPackages: Collection<String>,
        actionViewPackages: Collection<String>,
        ownPackage: String,
    ): Set<String> = (selectorPackages + actionViewPackages)
        .filterNotTo(linkedSetOf()) { it == ownPackage }
}
