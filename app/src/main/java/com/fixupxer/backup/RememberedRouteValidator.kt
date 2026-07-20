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

package com.fixupxer.backup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.fixupxer.utils.NativeAppMapping

object RememberedRouteValidator {

    fun normalizeHost(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val candidate = if (raw.contains("://")) raw else "https://${raw.trim()}"
        return com.fixupxer.processing.UrlNormalizer.extractAsciiHost(candidate)
    }

    fun isOwnPackage(context: Context, packageName: String): Boolean =
        packageName == context.packageName

    private val PACKAGE_NAME = Regex(
        "^[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)+$"
    )

    fun isValidPackageName(packageName: String): Boolean =
        packageName.length <= 255 && PACKAGE_NAME.matches(packageName)

    fun canSaveRoute(context: Context, packageName: String): Boolean =
        isValidPackageName(packageName) && !isOwnPackage(context, packageName)

    /**
     * Reader-only policy is decided on the FINAL (post-conversion) URI, including
     * the Farside path form. Reader destinations skip a remembered NATIVE route
     * without deleting it — the route stays valid for future non-reader launches.
     */
    fun shouldSkipNativeWithoutDelete(finalUri: Uri): Boolean {
        val finalHost = finalUri.host?.lowercase() ?: return false
        return NativeAppMapping.isReaderOnlyUrl(finalUri.toString(), finalHost)
    }

    /**
     * Runtime validity of a remembered NATIVE route. Mapping and launch
     * compatibility are evaluated against the FINAL uri, not the routing key.
     */
    fun isNativeRouteValid(context: Context, finalUri: Uri, packageName: String): Boolean {
        if (!canSaveRoute(context, packageName)) return false
        if (shouldSkipNativeWithoutDelete(finalUri)) return false
        val finalHost = finalUri.host?.lowercase() ?: return false
        if (packageName !in NativeAppMapping.packagesFor(finalUri.toString(), finalHost)) return false
        return canLaunchPackage(context, finalUri, packageName)
    }

    fun isBrowserRouteValid(context: Context, finalUri: Uri, packageName: String): Boolean {
        if (!canSaveRoute(context, packageName)) return false
        if (packageName !in browserPackages(context)) return false
        return canLaunchPackage(context, finalUri, packageName)
    }

    fun browserPackages(context: Context): Set<String> {
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
            .map { it.activityInfo.packageName }
            .filter { it != context.packageName }
            .toSet()
    }

    fun nativePackagesFor(url: String, host: String): List<String> =
        NativeAppMapping.packagesFor(url, host)

    /**
     * Strict structural validation of snapshot routes: any invalid entry rejects
     * the whole restore (no silent filtering). Deliberately does NOT check package
     * installation or launchability — backups travel across devices, availability
     * is a runtime concern. Runtime validation still restricts NATIVE routes to
     * [NativeAppMapping], but the backup cannot reproduce path-dependent mappings
     * (for example Google Search) from an exact-host key alone.
     */
    fun requireValidSnapshotRoutes(
        routes: Map<String, RememberedRoute>,
        ownPackageName: String?,
    ) {
        routes.forEach { (host, route) ->
            require(host.isNotBlank() && normalizeHost(host) == host) {
                "Invalid remembered route host"
            }
            require(isValidPackageName(route.packageName)) {
                "Invalid remembered route package"
            }
            require(ownPackageName == null || route.packageName != ownPackageName) {
                "Remembered route must not target FixupXer itself"
            }
        }
    }

    fun canLaunchPackage(context: Context, uri: Uri, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(packageName)
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0)
            ).isNotEmpty()
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
        }
    }
}
