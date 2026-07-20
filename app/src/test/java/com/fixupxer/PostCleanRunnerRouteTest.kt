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

package com.fixupxer

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import com.fixupxer.backup.RememberedRoute
import com.fixupxer.backup.RememberedRouteKind
import com.fixupxer.utils.PostCleanRunner
import com.fixupxer.utils.ProxyRoster
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
class PostCleanRunnerRouteTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var runner: PostCleanRunner

    @Before
    fun setup() {
        ProxyRoster.reset()
        context = RuntimeEnvironment.getApplication().applicationContext
        preferencesManager = PreferencesManager(context)
        preferencesManager.clearRememberedRoutes()
        runner = PostCleanRunner(context, preferencesManager)
    }

    @After
    fun tearDown() = ProxyRoster.reset()

    @Test
    fun `reader final uri skips native route without deleting it`() {
        preferencesManager.setRememberedRoute(
            "twitter.com",
            RememberedRoute(RememberedRouteKind.NATIVE, "com.twitter.android"),
        )

        val handled = runner.tryRememberedRoute(
            Uri.parse("https://xcancel.com/user/status/1"),
            "twitter.com",
        )

        assertFalse(handled)
        assertNotNull(preferencesManager.getRememberedRoute("twitter.com"))
    }

    @Test
    fun `farside nitter final uri skips native route without deleting it`() {
        preferencesManager.setRememberedRoute(
            "twitter.com",
            RememberedRoute(RememberedRouteKind.NATIVE, "com.twitter.android"),
        )

        val handled = runner.tryRememberedRoute(
            Uri.parse("https://farside.link/nitter/user/status/1"),
            "twitter.com",
        )

        assertFalse(handled)
        assertNotNull(preferencesManager.getRememberedRoute("twitter.com"))
    }

    @Test
    fun `incompatible native route is deleted and falls back`() {
        preferencesManager.setRememberedRoute(
            "twitter.com",
            RememberedRoute(RememberedRouteKind.NATIVE, "com.twitter.android"),
        )

        // No installed app can open the final uri in bare Robolectric.
        val handled = runner.tryRememberedRoute(
            Uri.parse("https://fixupx.com/user/status/1"),
            "twitter.com",
        )

        assertFalse(handled)
        assertNull(preferencesManager.getRememberedRoute("twitter.com"))
    }

    @Test
    fun `run falls back exactly once after route deletion`() {
        preferencesManager.setBrowserModeEnabled(true)
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        preferencesManager.setRememberedRoute(
            "twitter.com",
            RememberedRoute(RememberedRouteKind.NATIVE, "com.twitter.android"),
        )
        var completions = 0

        runner.run(Uri.parse("https://fixupx.com/user/status/1"), "twitter.com") {
            completions++
        }

        assertEquals(1, completions)
        assertNull(preferencesManager.getRememberedRoute("twitter.com"))
    }

    @Test
    fun `programmatic dialog dismissal does not complete the browser flow`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        val activity = controller.get()
        activity.setTheme(R.style.Theme_FixupXer)
        controller.setup()
        preferencesManager.setBrowserModeEnabled(false)
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        val activityRunner = PostCleanRunner(activity, preferencesManager)
        var completions = 0

        activityRunner.run(Uri.parse("https://example.com/")) {
            completions++
        }
        assertTrue(ShadowDialog.getLatestDialog().isShowing)

        activityRunner.dismissActiveDialog()

        assertFalse(ShadowDialog.getLatestDialog().isShowing)
        assertEquals(0, completions)
        controller.destroy()
    }

    @Test
    fun `priority mode ignores remembered route and follows action order`() {
        preferencesManager.setBrowserModeEnabled(true)
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        preferencesManager.setActionPriority(listOf(PreferencesManager.ACTION_CLIPBOARD))
        preferencesManager.setRememberedRoute(
            "twitter.com",
            RememberedRoute(RememberedRouteKind.NATIVE, "com.twitter.android"),
        )
        var completions = 0

        runner.run(Uri.parse("https://fixupx.com/user/status/1"), "twitter.com") {
            completions++
        }

        assertEquals(1, completions)
        assertNotNull(preferencesManager.getRememberedRoute("twitter.com"))
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
        assertEquals(
            "https://fixupx.com/user/status/1",
            clipboard.primaryClip?.getItemAt(0)?.text?.toString(),
        )
    }

    @Test
    fun `browser mode off ignores remembered route`() {
        val uri = Uri.parse("https://fixupx.com/user/status/1")
        registerBrowser("com.android.chrome", uri)
        preferencesManager.setBrowserModeEnabled(false)
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        preferencesManager.setRememberedRoute(
            "twitter.com",
            RememberedRoute(RememberedRouteKind.BROWSER, "com.android.chrome"),
        )
        var completions = 0

        runner.run(uri, "twitter.com") {
            completions++
        }

        assertEquals(1, completions)
        assertNotNull(preferencesManager.getRememberedRoute("twitter.com"))
        assertFalse(
            shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity?.`package` ==
                "com.android.chrome"
        )
    }

    @Test
    fun `valid browser route launches using lookup key host`() {
        val uri = Uri.parse("https://fixupx.com/user/status/1")
        registerBrowser("com.android.chrome", uri)
        preferencesManager.setRememberedRoute(
            "twitter.com",
            RememberedRoute(RememberedRouteKind.BROWSER, "com.android.chrome"),
        )

        assertTrue(runner.tryRememberedRoute(uri, "twitter.com"))
        assertNotNull(preferencesManager.getRememberedRoute("twitter.com"))
        assertEquals(
            "com.android.chrome",
            shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity?.`package`,
        )
    }

    @Test
    fun `candidates are empty when nothing can open the final uri`() {
        val candidates = runner.buildRememberCandidates(
            Uri.parse("https://fixupx.com/user/status/1"),
        )
        assertTrue(candidates.isEmpty())
    }

    @Test
    fun `candidates use final uri and include only launchable packages`() {
        val uri = Uri.parse("https://fixupx.com/user/status/1")
        registerBrowser("com.android.chrome", uri)
        // Second browser installed but unable to open the final uri.
        registerBrowserWithoutViewSupport("org.example.browser")
        registerViewTarget("com.twitter.android", uri)

        val candidates = runner.buildRememberCandidates(uri)

        val byPackage = candidates.associateBy { it.packageName }
        assertEquals(setOf("com.twitter.android", "com.android.chrome"), byPackage.keys)
        assertEquals(RememberedRouteKind.NATIVE, byPackage["com.twitter.android"]?.kind)
        assertEquals(RememberedRouteKind.BROWSER, byPackage["com.android.chrome"]?.kind)
    }

    @Test
    fun `own app is excluded from browser candidates`() {
        val uri = Uri.parse("https://fixupx.com/user/status/1")
        registerBrowser(context.packageName, uri)

        val candidates = runner.buildRememberCandidates(uri)

        assertTrue(candidates.none { it.packageName == context.packageName })
    }

    @Test
    fun `reader final uri offers no native candidates`() {
        val uri = Uri.parse("https://xcancel.com/user/status/1")
        registerViewTarget("com.twitter.android", uri)

        val candidates = runner.buildRememberCandidates(uri)

        assertTrue(candidates.none { it.kind == RememberedRouteKind.NATIVE })
    }

    private fun registerBrowser(packageName: String, uri: Uri) {
        registerBrowserWithoutViewSupport(packageName)
        registerViewTarget(packageName, uri)
    }

    @Suppress("DEPRECATION")
    private fun registerBrowserWithoutViewSupport(packageName: String) {
        val browserIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_BROWSER)
        }
        shadowOf(context.packageManager)
            .addResolveInfoForIntent(browserIntent, resolveInfoFor(packageName))
    }

    @Suppress("DEPRECATION")
    private fun registerViewTarget(packageName: String, uri: Uri) {
        val viewIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(packageName)
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        shadowOf(context.packageManager)
            .addResolveInfoForIntent(viewIntent, resolveInfoFor(packageName))
    }

    private fun resolveInfoFor(packageName: String): ResolveInfo {
        val activity = ActivityInfo().apply {
            this.packageName = packageName
            name = "$packageName.MainActivity"
            applicationInfo = ApplicationInfo().apply { this.packageName = packageName }
        }
        return ResolveInfo().apply { activityInfo = activity }
    }
}
