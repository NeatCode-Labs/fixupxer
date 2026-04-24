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

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.fixupxer.PreferencesManager
import com.fixupxer.data.model.AfterCleanAction
import com.fixupxer.utils.Constants
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
    fun run(cleanedUri: Uri) {
        Timber.d("PostCleanRunner.run called with URI: $cleanedUri")
        
        if (preferencesManager == null) {
            Timber.e("PreferencesManager is null")
            return
        }
        
        val actionMode = preferencesManager.getActionMode()
        Timber.d("Action mode: $actionMode")
        
        when (actionMode) {
            PreferencesManager.ACTION_MODE_ASK -> {
                showAppChooser(cleanedUri)
            }
            PreferencesManager.ACTION_MODE_PRIORITY -> {
                runPriorityMode(cleanedUri)
            }
        }
    }
    
    /**
     * Show the system chooser for the URL
     */
    private fun showSystemChooser(uri: Uri) {
        Timber.d("showSystemChooser: URI=$uri")
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            
            // Create chooser that excludes FixupXer itself
            val chooser = Intent.createChooser(intent, "Open with").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                
                // Exclude FixupXer from the chooser
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val excludedComponents = arrayOf(
                        ComponentName(context, "${context.packageName}.BrowserAlias"),
                        ComponentName(context, "${context.packageName}.MainActivity")
                    )
                    putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedComponents)
                }
            }
            
            context.startActivity(chooser)
        } catch (e: Exception) {
            Timber.e(e, "Failed to show chooser")
            Toast.makeText(context, "Failed to show chooser", Toast.LENGTH_SHORT).show()
            // Fallback to share
            share(uri)
        }
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
     * Execute actions in priority order until one succeeds
     */
    @Suppress("UNUSED_PARAMETER")
    fun runPriority(
        originalUrl: String,
        cleanedUrl: String,
        actionPriority: List<AfterCleanAction>
    ) {
        Timber.d("runPriorityMode: Running actions in order: ${actionPriority.map { it.name }} for URI: $cleanedUrl")
        
        val cleanedUri = Uri.parse(cleanedUrl)
        
        for (action in actionPriority) {
            Timber.d("Trying action: ${action.name}")
            
            val success = when (action.type) {
                AfterCleanAction.Type.NATIVE_APP -> {
                    launchNativeApp(cleanedUri)
                }
                AfterCleanAction.Type.BROWSER -> {
                    launchBrowser(cleanedUri)
                }
                AfterCleanAction.Type.SHARE -> {
                    share(cleanedUri)
                    true // Share always succeeds via chooser
                }
                AfterCleanAction.Type.CLIPBOARD -> {
                    copyToClipboard(cleanedUri)
                    true // Clipboard always succeeds
                }
            }
            
            if (success) {
                Timber.d("Action ${action.name} handled the URL successfully")
                return
            } else {
                Timber.d("Action ${action.name} could not handle the URL, trying next...")
            }
        }
        
        Timber.w("No actions could handle the URL: $cleanedUrl")
    }
    
    /**
     * Execute actions using follow mode (all enabled actions)
     */
    fun runFollow(
        originalUrl: String,
        cleanedUrl: String,
        enabledActions: List<AfterCleanAction>
    ) {
        Timber.d("runFollow: Starting with original=$originalUrl, cleaned=$cleanedUrl")
        
        val cleanedUri = Uri.parse(cleanedUrl)
        
        for (action in enabledActions) {
            Timber.d("runFollow: Executing action: ${action.name}")
            
            try {
                when (action.type) {
                    AfterCleanAction.Type.NATIVE_APP -> {
                        launchNativeApp(cleanedUri)
                    }
                    AfterCleanAction.Type.BROWSER -> {
                        launchBrowser(cleanedUri)
                    }
                    AfterCleanAction.Type.SHARE -> {
                        share(cleanedUri)
                    }
                    AfterCleanAction.Type.CLIPBOARD -> {
                        copyToClipboard(cleanedUri)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "runFollow: Failed to execute action: ${action.name}")
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
        
        // If that fails, show system chooser
        return showAppChooser(uri)
    }
    
    /**
     * Try to launch known native apps for cleaned URLs
     */
    private fun tryLaunchKnownNativeApp(uri: Uri): Boolean {
        val url = uri.toString().lowercase()
        
        // For YouTube URLs, try ReVanced YouTube first, then official YouTube
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            // Try ReVanced YouTube first
            val revancedIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("app.revanced.android.youtube")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            try {
                context.startActivity(revancedIntent)
                Timber.d("Launched ReVanced YouTube")
                return true
            } catch (e: ActivityNotFoundException) {
                Timber.d("ReVanced YouTube not found, trying official YouTube")
                
                // Try official YouTube
                val youtubeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.youtube")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                try {
                    context.startActivity(youtubeIntent)
                    Timber.d("Launched official YouTube")
                    return true
                } catch (e2: ActivityNotFoundException) {
                    Timber.d("Official YouTube not found either")
                }
            }
            
            return false
        }
        
        // Map domains to their native app packages
        val nativeAppPackage = when {
            url.contains(Constants.INSTAGRAM_DOMAIN) ||
                Constants.INSTAGRAM_PROXY_DOMAINS.any { url.contains(it) } -> "com.instagram.android"
            url.contains("x.com") || url.contains("twitter.com") || url.contains("fixupx.com") -> "com.twitter.android"
            url.contains("facebook.com") || url.contains("facebookez.com") -> "com.facebook.katana"
            url.contains("reddit.com") -> "com.reddit.frontpage"
            url.contains("linkedin.com") -> "com.linkedin.android"
            url.contains("amazon.com") || url.contains("amzn.to") -> "com.amazon.mShop.android.shopping"
            url.contains("google.com/search") || url.contains("google.com/url") -> "com.google.android.googlequicksearchbox"
            url.contains("tiktok.com") -> "com.zhiliaoapp.musically"  // Most common TikTok package
            url.contains("substack.com") -> "com.substack.app"
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
                url.contains("youtube.com") || url.contains("youtu.be") -> {
                    Timber.d("URL contains youtube.com or youtu.be, trying to add YouTube apps")
                    // Try ReVanced first, then official YouTube
                    tryAddManualApp("app.revanced.android.youtube", uri, targetIntents, manuallyAddedApps)
                    tryAddManualApp("com.google.android.youtube", uri, targetIntents, manuallyAddedApps)
                }
                url.contains(Constants.INSTAGRAM_DOMAIN) ||
                    Constants.INSTAGRAM_PROXY_DOMAINS.any { url.contains(it) } -> {
                    Timber.d("URL contains Instagram or one of its proxies, trying to add Instagram app")
                    tryAddManualApp("com.instagram.android", uri, targetIntents, manuallyAddedApps)
                }
                url.contains("x.com") || url.contains("twitter.com") || url.contains("fixupx.com") -> {
                    Timber.d("URL contains x.com/twitter.com/fixupx.com, trying to add Twitter app")
                    tryAddManualApp("com.twitter.android", uri, targetIntents, manuallyAddedApps)
                }
                url.contains("facebook.com") || url.contains("facebookez.com") -> {
                    Timber.d("URL contains facebook.com or facebookez.com, trying to add Facebook app")
                    tryAddManualApp("com.facebook.katana", uri, targetIntents, manuallyAddedApps)
                }
                url.contains("reddit.com") -> {
                    Timber.d("URL contains reddit.com, trying to add Reddit app")
                    tryAddManualApp("com.reddit.frontpage", uri, targetIntents, manuallyAddedApps)
                }
                url.contains("linkedin.com") -> {
                    Timber.d("URL contains linkedin.com, trying to add LinkedIn app")
                    tryAddManualApp("com.linkedin.android", uri, targetIntents, manuallyAddedApps)
                }
                url.contains("google.com/search") || url.contains("google.com/url") -> {
                    Timber.d("URL contains google.com search/url, trying to add Google app")
                    tryAddManualApp("com.google.android.googlequicksearchbox", uri, targetIntents, manuallyAddedApps)
                }
                url.contains("substack.com") -> {
                    Timber.d("URL contains substack.com, trying to add Substack app")
                    tryAddManualApp("com.substack.app", uri, targetIntents, manuallyAddedApps)
                }
                url.contains("tiktok.com") -> {
                    Timber.d("URL contains tiktok.com, trying to add TikTok app")
                    tryAddManualApp("com.zhiliaoapp.musically", uri, targetIntents, manuallyAddedApps)
                }
                url.contains("amazon.com") || url.contains("amzn.to") -> {
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
                Intent.createChooser(firstIntent, "Open with").apply {
                    if (targetIntents.isNotEmpty()) {
                        putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toTypedArray())
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                // Fallback to original intent if something goes wrong
                Intent.createChooser(viewIntent, "Open with").apply {
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
            // First, try to launch Chrome directly
            val chromeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.android.chrome")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            try {
                context.startActivity(chromeIntent)
                Timber.d("Launched Chrome directly")
                return true
            } catch (e: ActivityNotFoundException) {
                Timber.d("Chrome not found or can't handle URL, trying chooser")
            }
            
            // If Chrome fails, show the system chooser
            // Don't try to filter or query - just let Android handle it
            val viewIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(viewIntent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(chooser)
            Timber.d("Showed system chooser")
            return true
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch browser")
            Toast.makeText(context, "Failed to open browser", Toast.LENGTH_SHORT).show()
            return false
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
            val chooser = Intent.createChooser(shareIntent, "Share URL")
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
        val clip = ClipData.newPlainText("URL", uri.toString()).apply {
            // Only set extras on API 24+ to maintain compatibility with minSdk 21
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                description.extras = android.os.PersistableBundle().apply {
                    putString(ClipDescription.MIMETYPE_TEXT_URILIST, uri.toString())
                }
            }
        }
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Check if a package is a known browser
     */
    private fun isBrowserPackage(packageName: String): Boolean {
        return packageName in setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.brave.browser",
            "com.duckduckgo.mobile.android",
            "com.kiwibrowser.browser",
            "com.sec.android.app.sbrowser",
            "com.UCMobile.intl",
            "org.mozilla.fenix",
            "org.mozilla.focus",
            "com.vivaldi.browser",
            "com.android.browser",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary"
        )
    }
} 