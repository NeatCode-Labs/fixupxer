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
        context.getSharedPreferences("FixupXerPrefs", Context.MODE_PRIVATE).edit().clear().commit()
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
    fun `incompatible native route is retained and falls back`() {
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
        assertNotNull(preferencesManager.getRememberedRoute("twitter.com"))
    }

    @Test
    fun `failed fallback retains route and does not complete`() {
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

        assertEquals(0, completions)
        assertNotNull(preferencesManager.getRememberedRoute("twitter.com"))
    }

    @Test
    fun `exhausted priority reports failure instead of success`() {
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        preferencesManager.setActionPriority(listOf(PreferencesManager.ACTION_NATIVE_APP))
        val outcomes = mutableListOf<PostCleanRunner.Outcome>()
        runner.runGuarded(Uri.parse("https://example.com/"), null, { true }, outcomes::add)
        assertEquals(listOf(PostCleanRunner.Outcome.FAILED), outcomes)
    }

    @Test
    fun `configuration changed while dialog open prevents clipboard dispatch`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        val activity = controller.get()
        activity.setTheme(R.style.Theme_FixupXer)
        controller.setup()
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        val activityRunner = PostCleanRunner(activity, preferencesManager)
        var current = true
        val outcomes = mutableListOf<PostCleanRunner.Outcome>()
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("old", "unchanged"))
        activityRunner.runGuarded(Uri.parse("https://example.com/new"), null, { current }, outcomes::add)
        current = false
        val dialog = ShadowDialog.getLatestDialog() as androidx.appcompat.app.AlertDialog
        dialog.listView.performItemClick(null, 3, 3)
        assertEquals(listOf(PostCleanRunner.Outcome.STALE), outcomes)
        assertEquals("unchanged", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        controller.destroy()
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

        assertEquals(0, completions)
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
    fun `preferred browser launches exact uri directly without showing a dialog`() = withActivity { activity ->
        val uri = Uri.parse("https://vm.tnktok.com/preferred/?keep=%2B#fragment")
        registerBrowser("preferred.browser", uri)
        preferencesManager.setPreferredBrowserPackage("preferred.browser")
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        preferencesManager.setActionPriority(listOf(PreferencesManager.ACTION_BROWSER))
        val outcomes = mutableListOf<PostCleanRunner.Outcome>()

        PostCleanRunner(activity, preferencesManager).runGuarded(uri, null, { true }, outcomes::add)

        assertEquals(listOf(PostCleanRunner.Outcome.SUCCESS), outcomes)
        val sent = shadowOf(activity).nextStartedActivity
        assertEquals(uri, sent?.data)
        assertEquals("preferred.browser", sent?.`package`)
        assertTrue(ShadowDialog.getShownDialogs().none { it.isShowing })
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

    @Test
    fun `stale before dialog creation reports once without showing a dialog`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.Theme_FixupXer)
        val activity = controller.setup().get()
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        var checks = 0
        val outcomes = mutableListOf<PostCleanRunner.Outcome>()
        PostCleanRunner(activity, preferencesManager).runGuarded(
            Uri.parse("https://example.org/a"), "example.org", { ++checks == 1 }, outcomes::add,
        )
        assertEquals(listOf(PostCleanRunner.Outcome.STALE), outcomes)
        assertTrue(ShadowDialog.getShownDialogs().none { it.isShowing })
        controller.pause().stop().destroy()
    }

    @Test
    @Suppress("DEPRECATION")
    fun `share targets exclude self and preserve exact final text`() {
        val uri = "https://vm.tnktok.com/Z123/?keep=%2B#part"
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, uri)
        val pm = shadowOf(context.packageManager)
        pm.addResolveInfoForIntent(intent, resolveInfoFor(context.packageName).apply { activityInfo.exported = true })
        pm.addResolveInfoForIntent(intent, resolveInfoFor("test.receiver").apply { activityInfo.exported = true })
        val targets = runner.resolveExternalShareIntents(intent)
        assertEquals(listOf("test.receiver"), targets.map { it.component?.packageName })
        assertEquals(uri, targets.single().getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun `share with no external destination fails and does not launch empty chooser`() {
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        preferencesManager.setActionPriority(listOf(PreferencesManager.ACTION_SHARE_MENU))
        var outcome: PostCleanRunner.Outcome? = null
        runner.runGuarded(Uri.parse("https://example.org/a"), "example.org", { true }) { outcome = it }
        assertEquals(PostCleanRunner.Outcome.FAILED, outcome)
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity)
    }

    @Test
    fun `cancelling stale ask dialog reports stale once`() = withActivity { activity ->
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        var current = true
        val outcomes = mutableListOf<PostCleanRunner.Outcome>()
        PostCleanRunner(activity, preferencesManager).runGuarded(
            Uri.parse("https://example.org/a"), null, { current }, outcomes::add,
        )
        current = false
        ShadowDialog.getLatestDialog().cancel()
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(listOf(PostCleanRunner.Outcome.STALE), outcomes)
        assertNull(shadowOf(activity).nextStartedActivity)
    }

    @Test
    fun `cancelling stale remembered destination dialog reports stale`() = withActivity { activity ->
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        var current = true
        val outcomes = mutableListOf<PostCleanRunner.Outcome>()
        PostCleanRunner(activity, preferencesManager).runGuarded(
            Uri.parse("https://example.org/a"), "example.org", { current }, outcomes::add,
        )
        latestDialog().listView.performItemClick(null, 4, 4)
        current = false
        latestDialog().cancel()
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(listOf(PostCleanRunner.Outcome.STALE), outcomes)
        assertNull(preferencesManager.getRememberedRoute("example.org"))
    }

    @Test
    fun `browser chooser is pending until selection and cancel does not fall through`() = withActivity { activity ->
        val uri = Uri.parse("https://vm.tnktok.com/a/?keep=%2B#fragment")
        registerBrowser("first.browser", uri)
        registerBrowser("second.browser", uri)
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        preferencesManager.setActionPriority(listOf(PreferencesManager.ACTION_BROWSER, PreferencesManager.ACTION_CLIPBOARD))
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("old", "unchanged"))
        val outcomes = mutableListOf<PostCleanRunner.Outcome>()
        PostCleanRunner(activity, preferencesManager).runGuarded(uri, null, { true }, outcomes::add)
        assertTrue(outcomes.isEmpty())
        assertNull(shadowOf(activity).nextStartedActivity)
        latestDialog().cancel()
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(listOf(PostCleanRunner.Outcome.CANCELLED), outcomes)
        assertEquals("unchanged", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun `unavailable preferred browser shows replacement list and cancel does not dispatch`() = withActivity { activity ->
        val uri = Uri.parse("https://vm.tnktok.com/a/?keep=%2B#fragment")
        registerBrowser("first.browser", uri)
        registerBrowser("second.browser", uri)
        preferencesManager.setPreferredBrowserPackage("missing.browser")
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        preferencesManager.setActionPriority(listOf(PreferencesManager.ACTION_BROWSER))
        val outcomes = mutableListOf<PostCleanRunner.Outcome>()

        PostCleanRunner(activity, preferencesManager).runGuarded(uri, null, { true }, outcomes::add)

        val dialog = latestDialog()
        assertTrue(dialog.isShowing)
        assertTrue(dialog.listView.isShown)
        assertEquals(2, dialog.listView.adapter?.count ?: 0)
        assertTrue(outcomes.isEmpty())
        dialog.cancel()
        shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(listOf(PostCleanRunner.Outcome.CANCELLED), outcomes)
        assertNull(shadowOf(activity).nextStartedActivity)
        assertEquals("missing.browser", preferencesManager.getPreferredBrowserPackage())
    }

    @Test
    fun `single browser still asks and saves Always use only after launch`() = withActivity { activity ->
        val uri = Uri.parse("https://vm.tnktok.com/a/?keep=%2B#fragment")
        registerBrowser("only.browser", uri)
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        preferencesManager.setActionPriority(listOf(PreferencesManager.ACTION_BROWSER))
        val initialFingerprint = preferencesManager.browserFrontendFingerprint()
        val outcomes = mutableListOf<PostCleanRunner.Outcome>()

        PostCleanRunner(activity, preferencesManager).runGuarded(uri, null, { true }, outcomes::add)

        assertTrue(outcomes.isEmpty())
        assertNull(preferencesManager.getPreferredBrowserPackage())
        latestDialog()
            .getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)
            .performClick()
        shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(listOf(PostCleanRunner.Outcome.SUCCESS), outcomes)
        assertEquals("only.browser", preferencesManager.getPreferredBrowserPackage())
        assertNotNull(preferencesManager.getPreferredBrowserPackage())
        assertFalse(initialFingerprint == preferencesManager.browserFrontendFingerprint())
    }

    @Test
    fun `browser destination selection launches exact uri and then succeeds`() = withActivity { activity ->
        val uri = Uri.parse("https://vm.tnktok.com/a/?keep=%2B#fragment")
        registerBrowser("first.browser", uri)
        registerBrowser("second.browser", uri)
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        preferencesManager.setActionPriority(listOf(PreferencesManager.ACTION_BROWSER))
        val outcomes = mutableListOf<PostCleanRunner.Outcome>()
        PostCleanRunner(activity, preferencesManager).runGuarded(uri, null, { true }, outcomes::add)
        assertTrue(outcomes.isEmpty())
        latestDialog().listView.performItemClick(null, 1, 1)
        latestDialog()
            .getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            .performClick()
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(listOf(PostCleanRunner.Outcome.SUCCESS), outcomes)
        assertNull(preferencesManager.getPreferredBrowserPackage())
        val sent = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, sent.action)
        assertEquals(uri, sent.data)
        assertEquals("second.browser", sent.component?.packageName)
    }

    @Test
    fun `share selection launches exact text and never launches system chooser`() = withActivity { activity ->
        val uri = Uri.parse("https://vm.tnktok.com/a/?keep=%2B#fragment")
        registerShare("test.receiver")
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        preferencesManager.setActionPriority(listOf(PreferencesManager.ACTION_SHARE_MENU))
        val outcomes = mutableListOf<PostCleanRunner.Outcome>()
        PostCleanRunner(activity, preferencesManager).runGuarded(uri, null, { true }, outcomes::add)
        assertTrue(outcomes.isEmpty())
        assertNull(shadowOf(activity).nextStartedActivity)
        latestDialog().listView.performItemClick(null, 0, 0)
        assertEquals(listOf(PostCleanRunner.Outcome.SUCCESS), outcomes)
        val sent = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_SEND, sent.action)
        assertEquals(uri.toString(), sent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals("test.receiver", sent.component?.packageName)
    }

    @Test
    fun `share dialog checks stale context on cancel and never falls through`() = withActivity { activity ->
        registerShare("test.receiver")
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        preferencesManager.setActionPriority(listOf(PreferencesManager.ACTION_SHARE_MENU, PreferencesManager.ACTION_CLIPBOARD))
        var current = true
        val outcomes = mutableListOf<PostCleanRunner.Outcome>()
        PostCleanRunner(activity, preferencesManager).runGuarded(Uri.parse("https://example.org/a"), null, { current }, outcomes::add)
        current = false
        latestDialog().cancel()
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(listOf(PostCleanRunner.Outcome.STALE), outcomes)
        assertNull(shadowOf(activity).nextStartedActivity)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `removed share target falls through with same processed uri`() = withActivity { activity ->
        val uri = Uri.parse("https://vm.tnktok.com/a/?keep=%2B#fragment")
        registerShare("test.receiver")
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        preferencesManager.setActionPriority(listOf(PreferencesManager.ACTION_SHARE_MENU, PreferencesManager.ACTION_CLIPBOARD))
        val outcomes = mutableListOf<PostCleanRunner.Outcome>()
        PostCleanRunner(activity, preferencesManager).runGuarded(uri, null, { true }, outcomes::add)
        shadowOf(context.packageManager).removeResolveInfosForIntent(Intent(Intent.ACTION_SEND).setType("text/plain"), "test.receiver")
        latestDialog().listView.performItemClick(null, 0, 0)
        assertEquals(listOf(PostCleanRunner.Outcome.SUCCESS), outcomes)
        assertNull(shadowOf(activity).nextStartedActivity)
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        assertEquals(uri.toString(), clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }

    private fun latestDialog() = ShadowDialog.getLatestDialog() as androidx.appcompat.app.AlertDialog

    private fun withActivity(test: (AppCompatActivity) -> Unit) {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.Theme_FixupXer)
        val activity = controller.setup().get()
        try { test(activity) } finally {
            ShadowDialog.getShownDialogs().forEach { it.dismiss() }
            controller.pause().stop().destroy()
        }
    }

    @Suppress("DEPRECATION")
    private fun registerShare(packageName: String) {
        shadowOf(context.packageManager).addResolveInfoForIntent(
            Intent(Intent.ACTION_SEND).setType("text/plain"), resolveInfoFor(packageName),
        )
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
            exported = true
            enabled = true
            applicationInfo = ApplicationInfo().apply {
                this.packageName = packageName
                enabled = true
            }
        }
        return ResolveInfo().apply { activityInfo = activity }
    }
}
