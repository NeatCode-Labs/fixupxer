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
    enum class Outcome { SUCCESS, CANCELLED, FAILED, STALE }
    private var isCurrent: () -> Boolean = { true }
    private var outcomeCallback: (Outcome) -> Unit = {}
    private var reported = false

    private fun report(outcome: Outcome) {
        if (reported) return
        reported = true
        // No delivery has happened for a cancellation or failure. Recheck even
        // these callbacks: the configuration may have changed while a dialog was open.
        outcomeCallback(if (outcome != Outcome.SUCCESS && !isCurrent()) Outcome.STALE else outcome)
    }

    private fun checkCurrent(): Boolean {
        if (reported) return false
        if (isCurrent()) return true
        report(Outcome.STALE)
        return false
    }

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
        runGuarded(cleanedUri, routingHost, { true }) { outcome ->
            if (outcome == Outcome.SUCCESS) onComplete?.invoke()
        }
    }

    fun runGuarded(cleanedUri: Uri, routingHost: String?, isCurrent: () -> Boolean, onOutcome: (Outcome) -> Unit) {
        this.isCurrent = isCurrent
        outcomeCallback = onOutcome
        reported = false
        if (!checkCurrent()) return
        val onComplete: () -> Unit = { report(Outcome.SUCCESS) }
        Timber.d(
            "PostCleanRunner.run called (host=${cleanedUri.host ?: "unknown"}, " +
                "length=${cleanedUri.toString().length})"
        )

        if (preferencesManager == null) {
            Timber.e("PreferencesManager is null")
            report(Outcome.FAILED)
            return
        }

        val actionMode = preferencesManager.getActionMode()
        Timber.d("Action mode: $actionMode")

        if (preferencesManager.isBrowserModeEnabled() &&
            actionMode == PreferencesManager.ACTION_MODE_ASK &&
            tryRememberedRoute(cleanedUri, routingHost)
        ) {
            onComplete()
            return
        }

        when (actionMode) {
            PreferencesManager.ACTION_MODE_ASK -> {
                showAskEveryTimeDialog(cleanedUri, routingHost, onComplete)
            }
            PreferencesManager.ACTION_MODE_PRIORITY -> {
                runPriorityMode(cleanedUri)
            }
            else -> report(Outcome.FAILED)
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
            // A different final frontend URI may be temporarily incompatible.
            // Preserve the user's mapping; malformed stored routes are validated by prefs.
            Timber.d("Remembered route cannot handle this destination")
            return false
        }

        return if (launchPackage(uri, route.packageName)) {
            Timber.d("Launched remembered route for host")
            true
        } else {
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
            report(Outcome.FAILED)
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
                if (!checkCurrent()) return@setItems
                if (which == 4) {
                    showRememberCandidatePicker(uri, routingHost, onComplete)
                    return@setItems
                }
                when (which) {
                    0 -> if (launchNativeApp(uri)) report(Outcome.SUCCESS) else launchBrowser(uri, ::report)
                    1 -> launchBrowser(uri, ::report)
                    2 -> share(uri, ::report)
                    3 -> report(if (copyToClipboard(uri)) Outcome.SUCCESS else Outcome.FAILED)
                    else -> report(Outcome.FAILED)
                }
            }
            .setOnCancelListener {
                report(Outcome.CANCELLED)
            }
        )
    }

    private fun showRememberCandidatePicker(
        uri: Uri,
        routingHost: String?,
        onComplete: (() -> Unit)?,
    ) {
        if (!checkCurrent()) return
        val activity = context as? Activity
        if (activity == null || activity.isFinishing) {
            report(Outcome.FAILED)
            return
        }
        val host = preferencesManager?.normalizeRoutingHost(routingHost ?: uri.host)
        if (host.isNullOrBlank()) {
            Toast.makeText(activity, R.string.remembered_route_host_invalid, Toast.LENGTH_SHORT).show()
            report(Outcome.FAILED)
            return
        }

        val candidates = buildRememberCandidates(uri)
        if (candidates.isEmpty()) {
            showTrackedDialog(
                MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.remembered_route_picker_title)
                .setMessage(R.string.remembered_route_picker_empty)
                .setPositiveButton(android.R.string.ok) { _, _ -> report(Outcome.FAILED) }
                .setOnCancelListener { report(Outcome.CANCELLED) }
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
                if (!checkCurrent()) return@setItems
                val candidate = candidates[index]
                if (!launchPackage(uri, candidate.packageName)) {
                    report(Outcome.FAILED)
                    return@setItems
                }
                val saved = preferencesManager?.setRememberedRoute(
                    host,
                    RememberedRoute(candidate.kind, candidate.packageName),
                ) == true
                if (saved) {
                    Timber.d("Saved and launched remembered route")
                    onComplete?.invoke()
                } else {
                    Toast.makeText(activity, R.string.remembered_route_save_failed, Toast.LENGTH_SHORT).show()
                    onComplete?.invoke()
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> report(Outcome.CANCELLED) }
            .setOnCancelListener { report(Outcome.CANCELLED) }
        )
    }

    private fun showTrackedDialog(builder: MaterialAlertDialogBuilder) {
        if (!checkCurrent()) return
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
        val actions = preferencesManager?.getActionPriority().orEmpty()
        fun attempt(index: Int) {
            if (!checkCurrent()) return
            if (index >= actions.size) {
                report(Outcome.FAILED)
                return
            }
            val onResult: (Outcome) -> Unit = { outcome ->
                // A failed destination may fall through to the next action. An
                // explicit cancellation must never trigger an unexpected action.
                if (outcome == Outcome.FAILED && checkCurrent()) attempt(index + 1)
                else report(outcome)
            }
            when (actions[index]) {
                PreferencesManager.ACTION_NATIVE_APP -> onResult(if (launchNativeApp(uri)) Outcome.SUCCESS else Outcome.FAILED)
                PreferencesManager.ACTION_BROWSER -> launchBrowser(uri, onResult)
                PreferencesManager.ACTION_SHARE_MENU -> share(uri, onResult)
                PreferencesManager.ACTION_CLIPBOARD -> onResult(if (copyToClipboard(uri)) Outcome.SUCCESS else Outcome.FAILED)
                else -> onResult(Outcome.FAILED)
            }
        }
        attempt(0)
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
        if (!checkCurrent() || packageName == context.packageName) return false
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
     * Own the destination dialog on every supported Android version. Starting a
     * system chooser is not delivery: cancellation must retain the local result.
     * The selected target is resolved again before launching the exact final URI.
     */
    private fun chooseDestination(
        title: Int,
        alwaysAsk: Boolean,
        resolveTargets: () -> List<Intent>,
        onResult: (Outcome) -> Unit,
    ) {
        if (!checkCurrent()) return
        val targets = runCatching(resolveTargets).getOrElse {
            Timber.w(it, "Could not resolve Browser action destinations")
            onResult(Outcome.FAILED)
            return
        }
        if (targets.isEmpty()) {
            onResult(Outcome.FAILED)
            return
        }
        fun launch(target: Intent) {
            if (!checkCurrent()) return
            // Re-resolve from the original query, not an explicit intent whose
            // component could bypass a changed intent filter or disabled app.
            val available = runCatching { resolveTargets().any { it.filterEquals(target) } }.getOrDefault(false)
            if (!available) {
                onResult(Outcome.FAILED)
                return
            }
            if (!checkCurrent()) return
            val success = try {
                context.startActivity(target)
                true
            } catch (error: RuntimeException) {
                Timber.w(error, "Could not launch Browser action destination")
                false
            }
            onResult(if (success) Outcome.SUCCESS else Outcome.FAILED)
        }
        if (!alwaysAsk && targets.size == 1) {
            launch(targets.single())
            return
        }
        val activity = context as? Activity
        if (activity == null || activity.isFinishing) {
            onResult(Outcome.FAILED)
            return
        }
        val labels = targets.map { target ->
            val packageName = target.component?.packageName ?: target.`package`.orEmpty()
            activity.getString(R.string.remembered_route_candidate_label, appLabel(context.packageManager, packageName), packageName)
        }.toTypedArray()
        showTrackedDialog(
            MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setItems(labels) { _, index -> launch(targets[index]) }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    if (checkCurrent()) onResult(Outcome.CANCELLED)
                }
                .setOnCancelListener { if (checkCurrent()) onResult(Outcome.CANCELLED) }
        )
    }

    private fun launchBrowser(uri: Uri, onResult: (Outcome) -> Unit) {
        val preferredPackage = preferencesManager?.getPreferredBrowserPackage()
        if (preferredPackage != null &&
            RememberedRouteValidator.isBrowserRouteValid(context, uri, preferredPackage) &&
            launchPackage(uri, preferredPackage)
        ) {
            onResult(Outcome.SUCCESS)
            return
        }
        chooseBrowserDestination(
            uri = uri,
            preferredWasUnavailable = preferredPackage != null,
            onResult = onResult,
        )
    }

    private fun chooseBrowserDestination(
        uri: Uri,
        preferredWasUnavailable: Boolean,
        onResult: (Outcome) -> Unit,
    ) {
        if (!checkCurrent()) return
        val targets = runCatching { resolveExternalBrowserIntents(uri) }.getOrElse {
            Timber.w(it, "Could not resolve Browser action destinations")
            onResult(Outcome.FAILED)
            return
        }
        if (targets.isEmpty()) {
            onResult(Outcome.FAILED)
            return
        }
        val activity = context as? Activity
        if (activity == null || activity.isFinishing) {
            onResult(Outcome.FAILED)
            return
        }
        var selectedIndex = 0
        val labels = targets.map { target ->
            val packageName = target.component?.packageName ?: target.`package`.orEmpty()
            activity.getString(
                R.string.remembered_route_candidate_label,
                appLabel(context.packageManager, packageName),
                packageName,
            )
        }.toTypedArray()

        fun launch(target: Intent, remember: Boolean) {
            if (!checkCurrent()) return
            val available = runCatching {
                resolveExternalBrowserIntents(uri).any { it.filterEquals(target) }
            }.getOrDefault(false)
            if (!available) {
                onResult(Outcome.FAILED)
                return
            }
            if (!checkCurrent()) return
            val success = try {
                context.startActivity(target)
                true
            } catch (error: RuntimeException) {
                Timber.w(error, "Could not launch selected browser")
                false
            }
            if (success && remember) {
                val packageName = target.component?.packageName
                if (packageName != null &&
                    preferencesManager?.setPreferredBrowserPackage(packageName) == false
                ) {
                    Timber.w("Could not save preferred browser package")
                }
            }
            onResult(if (success) Outcome.SUCCESS else Outcome.FAILED)
        }

        val builder = MaterialAlertDialogBuilder(activity)
            .setTitle(
                if (preferredWasUnavailable) {
                    R.string.preferred_browser_picker_title_unavailable
                } else {
                    R.string.chooser_open_with_browser
                }
            )
            .setSingleChoiceItems(labels, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(R.string.browser_use_once) { _, _ ->
                launch(targets[selectedIndex], remember = false)
            }
            .setNeutralButton(R.string.browser_always_use) { _, _ ->
                launch(targets[selectedIndex], remember = true)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                if (checkCurrent()) onResult(Outcome.CANCELLED)
            }
            .setOnCancelListener { if (checkCurrent()) onResult(Outcome.CANCELLED) }
        showTrackedDialog(builder)
    }

    private fun resolveExternalBrowserIntents(uri: Uri): List<Intent> =
        RememberedRouteValidator.browserPackages(context).flatMap { packageName ->
            val viewIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(packageName)
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            queryExternalTargets(viewIntent)
        }.distinctBy { it.component }

    private fun share(uri: Uri, onResult: (Outcome) -> Unit) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, uri.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        chooseDestination(R.string.share_via, true, { resolveExternalShareIntents(shareIntent) }, onResult)
    }

    /**
     * Copy URL to clipboard
     */
    internal fun resolveExternalShareIntents(shareIntent: Intent): List<Intent> = queryExternalTargets(shareIntent)

    private fun queryExternalTargets(intent: Intent): List<Intent> {
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, 0)
        }
        return activities.filter {
            it.activityInfo.packageName != context.packageName &&
                it.activityInfo.exported &&
                it.activityInfo.enabled &&
                it.activityInfo.applicationInfo?.enabled != false
        }
            .distinctBy { "${it.activityInfo.packageName}/${it.activityInfo.name}" }
            .map { resolved -> Intent(intent).setClassName(resolved.activityInfo.packageName, resolved.activityInfo.name) }
    }

    private fun copyToClipboard(uri: Uri): Boolean {
        if (!checkCurrent()) return false
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
