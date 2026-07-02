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
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import timber.log.Timber

/**
 * Handles post-cleaning actions for URLs
 */
class PostCleanRunner(
    private val context: Context, 
    private val preferencesManager: PreferencesManager? = null
) {
    
    /**
     * Backward-compatible run method for the existing system
     */
    fun run(cleanedUri: Uri, onComplete: (() -> Unit)? = null) {
        Timber.d("PostCleanRunner.run called with URI: $cleanedUri")
        
        if (preferencesManager == null) {
            Timber.e("PreferencesManager is null")
            onComplete?.invoke()
            return
        }
        
        val actionMode = preferencesManager.getActionMode()
        Timber.d("Action mode: $actionMode")
        
        when (actionMode) {
            PreferencesManager.ACTION_MODE_ASK -> {
                showAskEveryTimeDialog(cleanedUri, onComplete)
            }
            PreferencesManager.ACTION_MODE_PRIORITY -> {
                runPriorityMode(cleanedUri)
                onComplete?.invoke()
            }
        }
    }
    
    /**
     * Show a FixupXer-owned action picker so "Ask every time" always asks,
     * even when Android's system chooser would auto-select the only target.
     */
    private fun showAskEveryTimeDialog(uri: Uri, onComplete: (() -> Unit)?) {
        val activity = context as? Activity
        if (activity == null || activity.isFinishing) {
            showAppChooser(uri)
            onComplete?.invoke()
            return
        }
        
        val actionNames = arrayOf(
            activity.getString(R.string.action_native_app),
            activity.getString(R.string.action_browser),
            activity.getString(R.string.action_share_menu),
            activity.getString(R.string.action_clipboard)
        )
        
        AlertDialog.Builder(activity)
            .setTitle(R.string.post_clean_action_title)
            .setItems(actionNames) { _, which ->
                val actionName = when (which) {
                    0 -> PreferencesManager.ACTION_NATIVE_APP
                    1 -> PreferencesManager.ACTION_BROWSER
                    2 -> PreferencesManager.ACTION_SHARE_MENU
                    3 -> PreferencesManager.ACTION_CLIPBOARD
                    else -> PreferencesManager.ACTION_SHARE_MENU
                }
                when (actionName) {
                    PreferencesManager.ACTION_NATIVE_APP -> {
                        if (!launchNativeApp(uri)) {
                            launchBrowser(uri)
                        }
                    }
                    PreferencesManager.ACTION_BROWSER -> launchBrowser(uri)
                    PreferencesManager.ACTION_SHARE_MENU -> share(uri)
                    PreferencesManager.ACTION_CLIPBOARD -> {
                        copyToClipboard(uri)
                    }
                }
                onComplete?.invoke()
            }
            .setOnCancelListener {
                onComplete?.invoke()
            }
            .show()
    }
    
    /**
     * Run actions in priority mode based on preferences
     */
    private fun runPriorityMode(uri: Uri) {
        // Get action priority from preferences as strings
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
                "clipboard" -> {
                    copyToClipboard(uri)
                    true
                }
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
        Timber.d("launchNativeApp: Trying to find native app for $uri")
        
        // First, try to launch known native apps directly
        val nativeAppLaunchResult = tryLaunchKnownNativeApp(uri)
        if (nativeAppLaunchResult) {
            return true
        }
        
        // Let priority mode continue to the browser/share fallback when no native app exists.
        return false
    }
    
    /**
     * Try to launch known native apps for cleaned URLs
     */
    private fun tryLaunchKnownNativeApp(uri: Uri): Boolean {
        val url = uri.toString().lowercase()
        
        // For YouTube URLs, try ReVanced YouTube first, then official YouTube
        if (url.contains(Constants.YOUTUBE_DOMAIN) || url.contains(Constants.YOUTUBE_SHORT_DOMAIN)) {
            for (packageName in listOf(
                "app.revanced.android.youtube",
                "app.morphe.android.youtube",
                "com.google.android.youtube"
            )) {
                val youtubeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(packageName)
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(youtubeIntent)
                    Timber.d("Launched YouTube package: $packageName")
                    return true
                } catch (e2: ActivityNotFoundException) {
                    Timber.d("YouTube package not found or cannot handle URL: $packageName")
                }
            }
            
            return false
        }
        
        // Map domains to their native app packages
        val nativeAppPackage = when {
            url.contains(Constants.INSTAGRAM_DOMAIN) ||
                InstagramProxyStore.allKnownProxies().any { url.contains(it) } -> "com.instagram.android"
            url.contains(Constants.X_DOMAIN) || url.contains(Constants.TWITTER_DOMAIN) ||
                url.contains(Constants.FIXUPX_DOMAIN) -> "com.twitter.android"
            url.contains(Constants.FACEBOOK_DOMAIN) || url.contains(Constants.FACEBOOKEZ_DOMAIN) -> "com.facebook.katana"
            url.contains(Constants.REDDIT_DOMAIN) -> "com.reddit.frontpage"
            url.contains(Constants.LINKEDIN_DOMAIN) -> "com.linkedin.android"
            url.contains(Constants.AMAZON_DOMAIN) || url.contains(Constants.AMAZON_SHORT_DOMAIN) -> "com.amazon.mShop.android.shopping"
            url.contains("${Constants.GOOGLE_DOMAIN}/search") || url.contains("${Constants.GOOGLE_DOMAIN}/url") -> "com.google.android.googlequicksearchbox"
            url.contains(Constants.TIKTOK_DOMAIN) -> "com.zhiliaoapp.musically"  // Most common TikTok package
            url.contains(Constants.SUBSTACK_DOMAIN) -> "com.substack.app"
            else -> null
        }
        
        if (nativeAppPackage != null) {
            try {
                // Try to launch the app directly
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(nativeAppPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                context.startActivity(intent)
                Timber.d("Launched known native app: $nativeAppPackage")
                return true
            } catch (e: ActivityNotFoundException) {
                Timber.d("Native app not found: $nativeAppPackage")
            }
        }
        
        return false
    }
    
    /**
     * Show system app chooser
     */
    private fun showAppChooser(uri: Uri): Boolean {
        return try {
            val viewIntent = Intent(Intent.ACTION_VIEW, uri)
            
            // Query for apps that can handle this URL
            val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    viewIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            
            // Filter out our own app
            val filteredActivities = activities.filter { 
                it.activityInfo.packageName != context.packageName 
            }
            
            // Build list of explicit intents for each app
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
            
            // Manually add native apps that should handle this URL but aren't detected
            val url = uri.toString()
            val manuallyAddedApps = mutableListOf<String>()
            
            Timber.d("Checking URL for manual app addition: $url")
            
            // Always try to add Chrome for http/https URLs to ensure chooser has options
            if (url.startsWith("http://") || url.startsWith("https://")) {
                tryAddManualApp("com.android.chrome", uri, targetIntents, manuallyAddedApps)
            }
            
            when {
                url.contains(Constants.YOUTUBE_DOMAIN) || url.contains(Constants.YOUTUBE_SHORT_DOMAIN) -> {
                    Timber.d("URL contains youtube.com or youtu.be, trying to add YouTube apps")
                    // Try common YouTube variants first, then official YouTube
                    tryAddManualApp("app.revanced.android.youtube", uri, targetIntents, manuallyAddedApps)
                    tryAddManualApp("app.morphe.android.youtube", uri, targetIntents, manuallyAddedApps)
                    tryAddManualApp("com.google.android.youtube", uri, targetIntents, manuallyAddedApps)
                }
                url.contains(Constants.INSTAGRAM_DOMAIN) ||
                    InstagramProxyStore.allKnownProxies().any { url.contains(it) } -> {
                    Timber.d("URL contains Instagram or one of its proxies, trying to add Instagram app")
                    tryAddManualApp("com.instagram.android", uri, targetIntents, manuallyAddedApps)
                }
                url.contains(Constants.X_DOMAIN) || url.contains(Constants.TWITTER_DOMAIN) ||
                    url.contains(Constants.FIXUPX_DOMAIN) -> {
                    Timber.d("URL contains x.com/twitter.com/fixupx.com, trying to add Twitter app")
                    tryAddManualApp("com.twitter.android", uri, targetIntents, manuallyAddedApps)
                }
                url.contains(Constants.FACEBOOK_DOMAIN) || url.contains(Constants.FACEBOOKEZ_DOMAIN) -> {
                    Timber.d("URL contains facebook.com or facebookez.com, trying to add Facebook app")
                    tryAddManualApp("com.facebook.katana", uri, targetIntents, manuallyAddedApps)
                }
                url.contains(Constants.REDDIT_DOMAIN) -> {
                    Timber.d("URL contains reddit.com, trying to add Reddit app")
                    tryAddManualApp("com.reddit.frontpage", uri, targetIntents, manuallyAddedApps)
                }
                url.contains(Constants.LINKEDIN_DOMAIN) -> {
                    Timber.d("URL contains linkedin.com, trying to add LinkedIn app")
                    tryAddManualApp("com.linkedin.android", uri, targetIntents, manuallyAddedApps)
                }
                url.contains("${Constants.GOOGLE_DOMAIN}/search") || url.contains("${Constants.GOOGLE_DOMAIN}/url") -> {
                    Timber.d("URL contains google.com search/url, trying to add Google app")
                    tryAddManualApp("com.google.android.googlequicksearchbox", uri, targetIntents, manuallyAddedApps)
                }
                url.contains(Constants.SUBSTACK_DOMAIN) -> {
                    Timber.d("URL contains substack.com, trying to add Substack app")
                    tryAddManualApp("com.substack.app", uri, targetIntents, manuallyAddedApps)
                }
                url.contains(Constants.TIKTOK_DOMAIN) -> {
                    Timber.d("URL contains tiktok.com, trying to add TikTok app")
                    tryAddManualApp("com.zhiliaoapp.musically", uri, targetIntents, manuallyAddedApps)
                }
                url.contains(Constants.AMAZON_DOMAIN) || url.contains(Constants.AMAZON_SHORT_DOMAIN) -> {
                    Timber.d("URL contains amazon.com or amzn.to, trying to add Amazon app")
                    tryAddManualApp("com.amazon.mShop.android.shopping", uri, targetIntents, manuallyAddedApps)
                }
                // Add more apps as needed
            }
            
            // If no other apps can handle this, return false
            if (targetIntents.isEmpty()) {
                Timber.d("No apps available to handle URL: $uri")
                return false
            }
            
            // Always show chooser, even if only one app is available
            // This ensures "Ask every time" actually asks every time
            
            // Multiple apps available, create chooser with explicit intents
            val chooser = if (targetIntents.isNotEmpty()) {
                val firstIntent = targetIntents.removeAt(0)
                Intent.createChooser(firstIntent, context.getString(R.string.chooser_open_with)).apply {
                    if (targetIntents.isNotEmpty()) {
                        putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toTypedArray())
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                // Fallback to original intent if something goes wrong
                Intent.createChooser(viewIntent, context.getString(R.string.chooser_open_with)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            
            context.startActivity(chooser)
            val totalApps = filteredActivities.size + manuallyAddedApps.size
            Timber.d("Showed system chooser with $totalApps apps (excluding FixupXer)" + 
                     if (manuallyAddedApps.isNotEmpty()) " - manually added: ${manuallyAddedApps.joinToString()}" else "")
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
        Timber.d("tryAddManualApp called for package: $packageName, uri: $uri")
        try {
            // Check if app is installed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            
            Timber.d("App $packageName is installed")
            
            // App is installed, create intent for it
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            // Verify the app can actually handle this intent
            val canHandle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0)
                ).isNotEmpty()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
            }
            
            Timber.d("App $packageName canHandle: $canHandle")
            
            if (canHandle) {
                targetIntents.add(intent)
                addedApps.add(packageName)
                Timber.d("Manually added $packageName to chooser")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // App not installed, skip
            Timber.d("App $packageName not installed")
        }
    }
    
    /**
     * Launch in browser
     */
    private fun launchBrowser(uri: Uri): Boolean {
        Timber.d("launchBrowser: Trying to launch browser for $uri")
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
    private fun copyToClipboard(uri: Uri) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(context.getString(R.string.clipboard_label_url), uri.toString()).apply {
            // Only set extras on API 24+ to maintain compatibility with minSdk 21
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                description.extras = android.os.PersistableBundle().apply {
                    putString(ClipDescription.MIMETYPE_TEXT_URILIST, uri.toString())
                }
            }
        }
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.url_copied), Toast.LENGTH_SHORT).show()
    }
} 