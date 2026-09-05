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

package com.fixupxer.utils

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.backup.RememberedRoute
import com.fixupxer.backup.RememberedRouteKind
import com.fixupxer.backup.RememberedRouteValidator
import timber.log.Timber

/**
 * Handles post-cleaning actions for URLs
 */
class PostCleanRunner(
    private val context: Context,
    private val preferencesManager: PreferencesManager? = null
) {
    private var activeDialog: AlertDialog? = null

    data class RouteCandidate(
        val packageName: String,
        val label: String,
        val kind: RememberedRouteKind,
    )

    /**
     * Backward-compatible run method for the existing system
     */
    fun run(cleanedUri: Uri, onComplete: (() -> Unit)? = null) {
        run(cleanedUri, routingHost = null, onComplete = onComplete)
    }

    fun run(cleanedUri: Uri, routingHost: String?, onComplete: (() -> Unit)? = null) {
        Timber.d(
            "PostCleanRunner.run called (host=${cleanedUri.host ?: "unknown"}, " +
                "length=${cleanedUri.toString().length})"
        )

        if (preferencesManager == null) {
            Timber.e("PreferencesManager is null")
            onComplete?.invoke()
            return
        }

        val actionMode = preferencesManager.getActionMode()
        Timber.d("Action mode: $actionMode")

        if (preferencesManager.isBrowserModeEnabled() &&
            actionMode == PreferencesManager.ACTION_MODE_ASK &&
            tryRememberedRoute(cleanedUri, routingHost)
        ) {
            onComplete?.invoke()
            return
        }

        when (actionMode) {
            PreferencesManager.ACTION_MODE_ASK -> {
                showAskEveryTimeDialog(cleanedUri, routingHost, onComplete)
            }
            PreferencesManager.ACTION_MODE_PRIORITY -> {
                runPriorityMode(cleanedUri)
                onComplete?.invoke()
            }
        }
    }

    fun dismissActiveDialog() {
        activeDialog?.dismiss()
        activeDialog = null
    }

    // Lookup key is the pre-conversion routing host; reader/native policy and
    // launch compatibility are evaluated against the FINAL uri. Internal for tests.
    internal fun tryRememberedRoute(uri: Uri, routingHost: String?): Boolean {
        val pm = preferencesManager ?: return false
        val host = pm.normalizeRoutingHost(routingHost ?: uri.host) ?: return false
        val route = pm.getRememberedRoute(host) ?: return false

        if (route.kind == RememberedRouteKind.NATIVE &&
            RememberedRouteValidator.shouldSkipNativeWithoutDelete(uri)
        ) {
            Timber.d("Skipping remembered native route for reader-only destination")
            return false
        }

        val valid = when (route.kind) {
            RememberedRouteKind.NATIVE ->
                RememberedRouteValidator.isNativeRouteValid(context, uri, route.packageName)
            RememberedRouteKind.BROWSER ->
                RememberedRouteValidator.isBrowserRouteValid(context, uri, route.packageName)
        }

        if (!valid) {
            Timber.d("Remembered route invalid; removing host mapping")
            pm.removeRememberedRoute(host)
            return false
        }

        return if (launchPackage(uri, route.packageName)) {
            Timber.d("Launched remembered route for host")
            true
        } else {
            pm.removeRememberedRoute(host)
            false
        }
    }

    /**
     * Show a FixupXer-owned action picker so "Ask every time" always asks,
     * even when Android's system chooser would auto-select the only target.
     */
    private fun showAskEveryTimeDialog(
        uri: Uri,
        routingHost: String?,
        onComplete: (() -> Unit)?,
    ) {
        val activity = context as? Activity
        if (activity == null || activity.isFinishing) {
            showAppChooser(uri)
            onComplete?.invoke()
            return
        }

        val rememberLabel = activity.getString(R.string.action_remember_for_host)
        val actionNames = arrayOf(
            activity.getString(R.string.action_native_app),
            activity.getString(R.string.action_browser),
            activity.getString(R.string.action_share_menu),
            activity.getString(R.string.action_clipboard),
            rememberLabel,
        )

        showTrackedDialog(
            MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.post_clean_action_title)
            .setItems(actionNames) { _, which ->
                when (which) {
                    0 -> {
                        if (!launchNativeApp(uri)) {
                            launchBrowser(uri)
                        }
                    }
                    1 -> launchBrowser(uri)
                    2 -> share(uri)
                    3 -> copyToClipboard(uri)
                    4 -> showRememberCandidatePicker(uri, routingHost, onComplete)
                    else -> share(uri)
                }
                if (which != 4) {
                    onComplete?.invoke()
                }
            }
            .setOnCancelListener {
                onComplete?.invoke()
            }
        )
    }

    private fun showRememberCandidatePicker(
        uri: Uri,
        routingHost: String?,
        onComplete: (() -> Unit)?,
    ) {
        val activity = context as? Activity
        if (activity == null || activity.isFinishing) {
            onComplete?.invoke()
            return
        }
        val host = preferencesManager?.normalizeRoutingHost(routingHost ?: uri.host)
        if (host.isNullOrBlank()) {
            Toast.makeText(activity, R.string.remembered_route_host_invalid, Toast.LENGTH_SHORT).show()
            onComplete?.invoke()
            return
        }

        val candidates = buildRememberCandidates(uri)
        if (candidates.isEmpty()) {
            showTrackedDialog(
                MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.remembered_route_picker_title)
                .setMessage(R.string.remembered_route_picker_empty)
                .setPositiveButton(android.R.string.ok) { _, _ -> onComplete?.invoke() }
                .setOnCancelListener { onComplete?.invoke() }
            )
            return
        }

        val labels = candidates.map { candidate ->
            activity.getString(
                R.string.remembered_route_candidate_label,
                candidate.label,
                candidate.packageName,
            )
        }.toTypedArray()

        showTrackedDialog(
            MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.remembered_route_picker_title)
            .setItems(labels) { _, index ->
                val candidate = candidates[index]
                val saved = preferencesManager?.setRememberedRoute(
                    host,
                    RememberedRoute(candidate.kind, candidate.packageName),
                ) == true
                if (saved && launchPackage(uri, candidate.packageName)) {
                    Timber.d("Saved and launched remembered route")
                    onComplete?.invoke()
                } else if (!saved) {
                    Toast.makeText(activity, R.string.remembered_route_save_failed, Toast.LENGTH_SHORT).show()
                    onComplete?.invoke()
                } else {
                    // Launch failed right after saving: delete the route and fall
                    // back to the Ask dialog directly — no recursive route lookup.
                    preferencesManager?.removeRememberedRoute(host)
                    Toast.makeText(activity, R.string.remembered_route_launch_failed, Toast.LENGTH_SHORT).show()
                    showAskEveryTimeDialog(uri, routingHost, onComplete)
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> onComplete?.invoke() }
            .setOnCancelListener { onComplete?.invoke() }
        )
    }

    private fun showTrackedDialog(builder: MaterialAlertDialogBuilder) {
        val dialog = builder.create()
        activeDialog = dialog
        dialog.setOnDismissListener {
            if (activeDialog === dialog) {
                activeDialog = null
            }
        }
        dialog.show()
    }

    // Candidates are resolved against the FINAL uri: only installed packages that
    // can actually open it are offered, FixupXer itself excluded.
    internal fun buildRememberCandidates(uri: Uri): List<RouteCandidate> {
        val seen = linkedSetOf<String>()
        val candidates = mutableListOf<RouteCandidate>()
        val packageManager = context.packageManager
        val finalHost = uri.host?.lowercase()

        if (finalHost != null && !RememberedRouteValidator.shouldSkipNativeWithoutDelete(uri)) {
            RememberedRouteValidator.nativePackagesFor(uri.toString(), finalHost).forEach { packageName ->
                if (!seen.add(packageName)) return@forEach
                if (!RememberedRouteValidator.canSaveRoute(context, packageName)) return@forEach
                if (!RememberedRouteValidator.canLaunchPackage(context, uri, packageName)) return@forEach
                candidates += RouteCandidate(
                    packageName = packageName,
                    label = appLabel(packageManager, packageName),
                    kind = RememberedRouteKind.NATIVE,
                )
            }
        }

        RememberedRouteValidator.browserPackages(context).forEach { packageName ->
            if (!seen.add(packageName)) return@forEach
            if (!RememberedRouteValidator.canLaunchPackage(context, uri, packageName)) return@forEach
            candidates += RouteCandidate(
                packageName = packageName,
                label = appLabel(packageManager, packageName),
                kind = RememberedRouteKind.BROWSER,
            )
        }
        return candidates
    }

    private fun appLabel(packageManager: PackageManager, packageName: String): String =
        runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        }.getOrDefault(packageName)

    /**
     * Run actions in priority mode based on preferences
     */
    private fun runPriorityMode(uri: Uri) {
        val actionStrings = preferencesManager?.getActionPriority() ?: listOf(
            "native_app",
            "browser",
            "share_menu",
            "clipboard"
        )

        Timber.d("Running actions in priority: $actionStrings")

        for (actionName in actionStrings) {
            val success = when (actionName) {
                "native_app" -> launchNativeApp(uri)
                "browser" -> launchBrowser(uri)
                "share_menu" -> share(uri)
                "clipboard" -> copyToClipboard(uri)
                else -> false
            }

            if (success) {
                Timber.d("Action $actionName succeeded")
                break
            } else {
                Timber.d("Action $actionName failed, trying next")
            }
        }
    }

    /**
     * Try to launch native app
     */
    private fun launchNativeApp(uri: Uri): Boolean {
        Timber.d("launchNativeApp: trying to find native app (host=${uri.host ?: "unknown"})")

        val nativeAppLaunchResult = tryLaunchKnownNativeApp(uri)
        if (nativeAppLaunchResult) {
            return true
        }

        return false
    }

    /**
     * Try to launch known native apps for cleaned URLs
     */
    private fun tryLaunchKnownNativeApp(uri: Uri): Boolean {
        val host = uri.host?.lowercase()
        for (packageName in NativeAppMapping.packagesFor(uri.toString(), host)) {
            if (launchPackage(uri, packageName)) return true
        }
        return false
    }

    private fun launchPackage(uri: Uri, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(packageName)
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            Timber.d("Launched app package")
            true
        } catch (_: RuntimeException) {
            Timber.d("App not found or cannot handle URL")
            false
        }
    }

    /**
     * Show system app chooser
     */
    private fun showAppChooser(uri: Uri): Boolean {
        return try {
            val viewIntent = Intent(Intent.ACTION_VIEW, uri)

            val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    viewIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            val filteredActivities = activities.filter {
                it.activityInfo.packageName != context.packageName
            }

            val targetIntents = mutableListOf<Intent>()

            filteredActivities.forEach { resolveInfo ->
                val targetIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setClassName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                targetIntents.add(targetIntent)
            }

            val url = uri.toString()
            val manuallyAddedApps = mutableListOf<String>()

            Timber.d("Checking URL host for manual app addition: ${uri.host ?: "unknown"}")

            if (url.startsWith("http://") || url.startsWith("https://")) {
                tryAddManualApp("com.android.chrome", uri, targetIntents, manuallyAddedApps)
            }

            NativeAppMapping.packagesFor(url, uri.host?.lowercase()).forEach { packageName ->
                Timber.d("Native app mapping selected package for manual chooser addition")
                tryAddManualApp(packageName, uri, targetIntents, manuallyAddedApps)
            }

            if (targetIntents.isEmpty()) {
                Timber.d("No apps available to handle URL host=${uri.host ?: "unknown"}")
                return false
            }

            val chooser = if (targetIntents.isNotEmpty()) {
                val firstIntent = targetIntents.removeAt(0)
                Intent.createChooser(firstIntent, context.getString(R.string.chooser_open_with)).apply {
                    if (targetIntents.isNotEmpty()) {
                        putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toTypedArray())
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent.createChooser(viewIntent, context.getString(R.string.chooser_open_with)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            context.startActivity(chooser)
            val totalApps = filteredActivities.size + manuallyAddedApps.size
            Timber.d("Showed system chooser with $totalApps apps (excluding FixupXer)")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to show app chooser")
            false
        }
    }

    /**
     * Try to manually add an app to the chooser if it's installed
     */
    private fun tryAddManualApp(
        packageName: String,
        uri: Uri,
        targetIntents: MutableList<Intent>,
        addedApps: MutableList<String>
    ) {
        Timber.d("tryAddManualApp called for package")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val canHandle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0)
                ).isNotEmpty()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
            }

            if (canHandle) {
                targetIntents.add(intent)
                addedApps.add(packageName)
                Timber.d("Manually added package to chooser")
            }
        } catch (_: PackageManager.NameNotFoundException) {
            Timber.d("App not installed")
        }
    }

    /**
     * Launch in browser
     */
    private fun launchBrowser(uri: Uri): Boolean {
        Timber.d("launchBrowser: trying to launch browser (host=${uri.host ?: "unknown"})")
        try {
            val browserIntents = resolveExternalBrowserIntents(uri)
            if (browserIntents.isEmpty()) {
                Timber.d("No browser target available after excluding FixupXer")
                return false
            }

            if (browserIntents.size == 1) {
                context.startActivity(browserIntents.first())
                Timber.d("Launched the only external browser")
                return true
            }

            val firstIntent = browserIntents.first()
            val extraIntents = browserIntents.drop(1).toTypedArray()
            val chooser = Intent.createChooser(firstIntent, context.getString(R.string.chooser_open_with_browser)).apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Timber.d("Showed browser-only chooser with ${browserIntents.size} browsers")
            return true

        } catch (e: Exception) {
            Timber.e(e, "Failed to launch browser")
            Toast.makeText(context, context.getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun resolveExternalBrowserIntents(uri: Uri): List<Intent> {
        val browserIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_BROWSER)
        }
        val browsers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                browserIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }

        return browsers
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { resolveInfo ->
                Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(resolveInfo.activityInfo.packageName)
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
    }

    /**
     * Show share menu
     */
    private fun share(uri: Uri): Boolean {
        return try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, uri.toString())
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_via))
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Timber.d("Showed share menu")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to show share menu")
            false
        }
    }

    /**
     * Copy URL to clipboard
     */
    private fun copyToClipboard(uri: Uri): Boolean {
        return try {
            UrlClipboard.copy(context, uri.toString())
            if (UrlClipboard.needsAppFeedback) {
                Toast.makeText(context, R.string.url_copied, Toast.LENGTH_SHORT).show()
            }
            true
        } catch (error: Exception) {
            Timber.e(error, "Failed to copy URL")
            Toast.makeText(context, R.string.error_copying_url, Toast.LENGTH_SHORT).show()
            false
        }
    }
}
