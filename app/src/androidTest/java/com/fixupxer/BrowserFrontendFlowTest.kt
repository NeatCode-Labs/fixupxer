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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.backup.SettingsSnapshot
import com.fixupxer.processing.BrowserConversionMode
import com.fixupxer.processing.BrowserFrontendPolicy
import com.fixupxer.processing.BrowserFrontendPreference
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.FrontendTarget
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserFrontendFlowTest {

    private lateinit var context: Context
    private lateinit var preferences: PreferencesManager
    private lateinit var originalSettings: SettingsSnapshot
    private lateinit var clipboard: ClipboardManager
    private var originalAliasEnabled = false

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        preferences = PreferencesManager(context)
        originalSettings = preferences.exportSettingsSnapshot()
        originalAliasEnabled = BrowserModeUtils.isBrowserAliasEnabled(context)
        // API 21 constructs ClipboardManager with a Handler on the calling thread.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        }

        ProxyRoster.reset()
        ProxyPlatform.entries.forEach(preferences::restoreBuiltIns)
        assertTrue(
            preferences.saveBrowserFrontendPreferences(
                changes = ProxyPlatform.entries.associateWith { BrowserFrontendPreference.CLEAN_ONLY },
                expected = preferences.getBrowserFrontendPreferences(),
            )
        )
        preferences.clearRememberedRoutes()
        preferences.setCustomRulesEnabled(false)
        preferences.setHistoryEnabled(false)
        preferences.setBrowserModeEnabled(true)
        preferences.setActionMode(PreferencesManager.ACTION_MODE_PRIORITY)
        preferences.setActionPriority(listOf(PreferencesManager.ACTION_CLIPBOARD))
        BrowserModeUtils.setBrowserAliasEnabled(context, true)
    }

    @After
    fun tearDown() {
        BrowserModeUtils.setBrowserAliasEnabled(context, originalAliasEnabled)
        ProxyRoster.reset()
        assertTrue(preferences.replaceSettingsSnapshot(originalSettings))
        if (::clipboard.isInitialized) clearClipboardInForeground()
    }

    @Test
    fun xViewCopiesEveryAllowedBuiltInTarget() {
        assertBuiltIns(
            platform = ProxyPlatform.X,
            input = "https://x.com/alice/status/123",
            expectedFor = { target ->
                "https://${target.domain}${target.pathPrefix.orEmpty()}/alice/status/123"
            },
        )
    }

    @Test
    fun instagramViewCopiesEveryAllowedBuiltInTarget() {
        assertBuiltIns(
            platform = ProxyPlatform.INSTAGRAM,
            input = "https://www.instagram.com/p/ABC/",
            expectedFor = { target -> "https://${target.domain}/p/ABC/" },
        )
    }

    @Test
    fun tiktokViewCopiesEveryAllowedBuiltInTargetAndPreservesShortLinkSubdomains() {
        val targets = allowedBuiltIns(ProxyPlatform.TIKTOK)
        assertTrue(targets.isNotEmpty())
        targets.forEach { target ->
            select(ProxyPlatform.TIKTOK, target)
            assertViewCopies(
                "https://www.tiktok.com/@alice/video/123",
                "https://www.${target.domain}/@alice/video/123",
                target.id,
            )
        }

        val first = targets.first()
        select(ProxyPlatform.TIKTOK, first)
        assertViewCopies(
            "https://vm.tiktok.com/ZMabc/",
            "https://vm.${first.domain}/ZMabc/",
            "${first.id}:vm",
        )
        assertViewCopies(
            "https://vt.tiktok.com/ZSabcd/",
            "https://vt.${first.domain}/ZSabcd/",
            "${first.id}:vt",
        )
    }

    @Test
    fun blueskyViewCopiesEveryAllowedBuiltInTarget() {
        assertBuiltIns(
            platform = ProxyPlatform.BLUESKY,
            input = "https://bsky.app/profile/alice.test/post/abc",
            expectedFor = { target -> "https://${target.domain}/profile/alice.test/post/abc" },
        )
    }

    @Test
    fun redditAndPinterestViewsCopyEveryAllowedBuiltInTarget() {
        assertBuiltIns(
            platform = ProxyPlatform.REDDIT,
            input = "https://www.reddit.com/r/test/comments/abc/title/",
            expectedFor = { target -> "https://${target.domain}/r/test/comments/abc/title/" },
        )
        assertBuiltIns(
            platform = ProxyPlatform.PINTEREST,
            input = "https://www.pinterest.com/pin/123456/",
            expectedFor = { target -> "https://${target.domain}/pin/123456/" },
        )
    }

    @Test
    fun customTargetsCopyForAllSevenSupportedPlatforms() {
        val inputs = mapOf(
            ProxyPlatform.X to "https://x.com/alice/status/123",
            ProxyPlatform.INSTAGRAM to "https://www.instagram.com/p/ABC/",
            ProxyPlatform.TIKTOK to "https://www.tiktok.com/@alice/video/123",
            ProxyPlatform.FACEBOOK to "https://www.facebook.com/alice/posts/123",
            ProxyPlatform.BLUESKY to "https://bsky.app/profile/alice.test/post/abc",
            ProxyPlatform.REDDIT to "https://www.reddit.com/r/test/comments/abc/title/",
            ProxyPlatform.PINTEREST to "https://www.pinterest.com/pin/123456/",
        )
        val expectedPaths = mapOf(
            ProxyPlatform.X to "/alice/status/123",
            ProxyPlatform.INSTAGRAM to "/p/ABC/",
            ProxyPlatform.TIKTOK to "/@alice/video/123",
            ProxyPlatform.FACEBOOK to "/alice/posts/123",
            ProxyPlatform.BLUESKY to "/profile/alice.test/post/abc",
            ProxyPlatform.REDDIT to "/r/test/comments/abc/title/",
            ProxyPlatform.PINTEREST to "/pin/123456/",
        )

        BrowserFrontendPolicy.supportedPlatforms().forEach { platform ->
            val domain = "browser-${platform.name.lowercase()}.example"
            preferences.addCustomProxy(platform, domain)
            val expected = preferences.getBrowserFrontendPreferences()
            assertTrue(
                preferences.saveBrowserFrontendPreferences(
                    changes = mapOf(
                        platform to BrowserFrontendPreference(
                            BrowserConversionMode.CUSTOM,
                            "custom:$domain",
                        )
                    ),
                    expected = expected,
                )
            )
            val prefix = if (platform == ProxyPlatform.TIKTOK) "www." else ""
            assertViewCopies(
                input = inputs.getValue(platform),
                expected = "https://$prefix$domain${expectedPaths.getValue(platform)}",
                caseName = "custom:$platform",
            )
        }
    }

    @Test
    fun cleanOnlyCopiesSourceUrlForAllNinePlatforms() {
        val inputs = mapOf(
            ProxyPlatform.X to "https://x.com/alice/status/123",
            ProxyPlatform.INSTAGRAM to "https://instagram.com/p/ABC/",
            ProxyPlatform.TIKTOK to "https://www.tiktok.com/@alice/video/123",
            ProxyPlatform.FACEBOOK to "https://facebook.com/alice/posts/123",
            ProxyPlatform.BLUESKY to "https://bsky.app/profile/alice.test/post/abc",
            ProxyPlatform.REDDIT to "https://reddit.com/r/test/comments/abc/title/",
            ProxyPlatform.YOUTUBE to "https://youtube.com/watch?v=abc123",
            ProxyPlatform.PINTEREST to "https://pinterest.com/pin/123456/",
            ProxyPlatform.THREADS to "https://threads.net/@alice/post/ABC",
        )

        inputs.forEach { (platform, input) ->
            select(platform, BrowserFrontendPreference.CLEAN_ONLY)
            assertViewCopies(input, input, "clean:$platform")
        }
    }

    @Test
    fun settingsChangeWhileAskDialogIsOpenCannotCopyStaleResult() {
        preferences.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        val target = allowedBuiltIns(ProxyPlatform.X).first()
        select(ProxyPlatform.X, target)
        val sentinel = "unchanged-before-stale-browser-action"
        clipboard.setPrimaryClip(ClipData.newPlainText("test", sentinel))

        ActivityScenario.launch<MainActivity>(viewIntent("https://x.com/alice/status/123")).use {
            awaitAssertion {
                onView(withText(R.string.post_clean_action_title))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
            }

            select(ProxyPlatform.X, BrowserFrontendPreference.CLEAN_ONLY)
            onView(withText(R.string.action_clipboard)).inRoot(isDialog()).perform(click())

            awaitAssertion {
                assertEquals(sentinel, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
                onView(withText(R.string.browser_retry)).check(matches(isDisplayed()))
            }
        }
    }

    @Test
    fun settingsChangeWhileAskDialogIsOpenThenBackKeepsProcessedResultForRetry() {
        preferences.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        val target = allowedBuiltIns(ProxyPlatform.X).first()
        select(ProxyPlatform.X, target)
        val input = "https://x.com/alice/status/123"
        val processed = "https://${target.domain}${target.pathPrefix.orEmpty()}/alice/status/123"
        val sentinel = "unchanged-before-stale-browser-cancel"
        clipboard.setPrimaryClip(ClipData.newPlainText("test", sentinel))

        ActivityScenario.launch<MainActivity>(viewIntent(input)).use {
            awaitAskDialog()

            select(ProxyPlatform.X, BrowserFrontendPreference.CLEAN_ONLY)
            pressBack()

            waitForAttention(processed, sentinel)
            onView(withText(R.string.post_clean_action_title)).check(doesNotExist())
        }
    }

    @Test
    fun cancellingAskDialogKeepsProcessedResultForRetry() {
        preferences.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        val target = allowedBuiltIns(ProxyPlatform.X).first()
        select(ProxyPlatform.X, target)
        val input = "https://x.com/alice/status/123"
        val processed = "https://${target.domain}${target.pathPrefix.orEmpty()}/alice/status/123"
        val sentinel = "unchanged-before-browser-cancel"
        clipboard.setPrimaryClip(ClipData.newPlainText("test", sentinel))

        ActivityScenario.launch<MainActivity>(viewIntent(input)).use {
            awaitAskDialog()
            pressBack()

            waitForAttention(processed, sentinel)
            onView(withText(R.string.post_clean_action_title)).check(doesNotExist())
        }
    }

    @Test
    fun cancellingRememberedDestinationPickerKeepsProcessedResultForRetry() {
        preferences.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        val target = allowedBuiltIns(ProxyPlatform.X).first()
        select(ProxyPlatform.X, target)
        val input = "https://x.com/alice/status/123"
        val processed = "https://${target.domain}${target.pathPrefix.orEmpty()}/alice/status/123"
        val sentinel = "unchanged-before-destination-picker-cancel"
        clipboard.setPrimaryClip(ClipData.newPlainText("test", sentinel))

        ActivityScenario.launch<MainActivity>(viewIntent(input)).use {
            awaitAskDialog()
            onView(withText(R.string.action_remember_for_host)).inRoot(isDialog()).perform(click())
            awaitAssertion {
                onView(withText(R.string.remembered_route_picker_title))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
            }

            pressBack()

            waitForAttention(processed, sentinel)
            assertEquals(0, preferences.getRememberedRouteCount())
            onView(withText(R.string.remembered_route_picker_title)).check(doesNotExist())
        }
    }

    @Test
    fun failedActionAttentionAndProcessedUrlSurviveRecreation() {
        val reader = allowedBuiltIns(ProxyPlatform.X).first {
            BrowserFrontendPolicy.modeFor(it) == BrowserConversionMode.READER
        }
        select(ProxyPlatform.X, reader)
        preferences.setActionPriority(listOf(PreferencesManager.ACTION_NATIVE_APP))
        val input = "https://x.com/alice/status/123"
        val processed = "https://${reader.domain}/alice/status/123"
        val sentinel = "unchanged-before-failed-browser-action"
        clipboard.setPrimaryClip(ClipData.newPlainText("test", sentinel))

        ActivityScenario.launch<MainActivity>(viewIntent(input)).use { scenario ->
            waitForAttention(processed, sentinel)
            scenario.recreate()
            waitForAttention(processed, sentinel)
            onView(withText(R.string.post_clean_action_title)).check(doesNotExist())
        }
    }

    @Test
    fun unavailableEmbedStaysLocalAndRetryUsesTheRestoredBrowserChoice() {
        val target = allowedBuiltIns(ProxyPlatform.TIKTOK).first()
        select(ProxyPlatform.TIKTOK, target)
        preferences.disableBuiltIn(ProxyPlatform.TIKTOK, target.id)
        val input = "https://vm.tiktok.com/ZMabc/"
        val sentinel = "not-dispatched-when-target-is-unavailable"
        clipboard.setPrimaryClip(ClipData.newPlainText("test", sentinel))
        ActivityScenario.launch<MainActivity>(viewIntent(input)).use { scenario ->
            waitForAttention(input, sentinel)
            onView(withText(R.string.pipeline_status_frontend_unavailable)).check(matches(isDisplayed()))
            preferences.restoreBuiltIns(ProxyPlatform.TIKTOK)
            onView(withText(R.string.browser_retry)).perform(click())
            awaitAssertion {
                assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED, scenario.state)
            }
        }
        assertEquals("https://vm.${target.domain}/ZMabc/", readClipboardInForeground())
    }

    @Test
    fun unsupportedAuthorityDoesNotDispatchTheOriginalUrl() {
        select(ProxyPlatform.TIKTOK, allowedBuiltIns(ProxyPlatform.TIKTOK).first())
        val input = "https://www.tiktok.com:8443/@alice/video/123"
        val sentinel = "not-dispatched-for-unsupported-port"
        clipboard.setPrimaryClip(ClipData.newPlainText("test", sentinel))
        ActivityScenario.launch<MainActivity>(viewIntent(input)).use {
            waitForAttention(input, sentinel)
            onView(withText(R.string.pipeline_status_unsupported_conversion)).check(matches(isDisplayed()))
        }
    }

    private fun assertBuiltIns(
        platform: ProxyPlatform,
        input: String,
        expectedFor: (FrontendTarget) -> String,
    ) {
        val targets = allowedBuiltIns(platform)
        assertTrue("No Browser built-ins for $platform", targets.isNotEmpty())
        targets.forEach { target ->
            select(platform, target)
            assertViewCopies(input, expectedFor(target), target.id)
        }
    }

    private fun allowedBuiltIns(platform: ProxyPlatform): List<FrontendTarget> =
        BrowserFrontendPolicy.allowedTargets(platform, AlternativeFrontendCatalog.builtIn(platform))

    private fun select(platform: ProxyPlatform, target: FrontendTarget) {
        select(
            platform,
            BrowserFrontendPreference(BrowserFrontendPolicy.modeFor(target), target.id),
        )
    }

    private fun select(platform: ProxyPlatform, preference: BrowserFrontendPreference) {
        val expected = preferences.getBrowserFrontendPreferences()
        assertTrue(
            "Failed to save Browser frontend for $platform",
            preferences.saveBrowserFrontendPreferences(
                changes = mapOf(platform to preference),
                expected = expected,
                restoreBuiltInIds = preference.targetId
                    ?.takeUnless { it.startsWith("custom:") }
                    ?.let { mapOf(platform to setOf(it)) }
                    .orEmpty(),
            )
        )
    }

    private fun assertViewCopies(input: String, expected: String, caseName: String) {
        val sentinel = "pending:$caseName"
        clipboard.setPrimaryClip(ClipData.newPlainText("test", sentinel))
        ActivityScenario.launch<MainActivity>(viewIntent(input)).use {
            awaitAssertion(timeoutMs = 15_000L, pollMs = 100L) {
                assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED, it.state)
            }
        }
        assertEquals(
            "Unexpected Browser clipboard result for $caseName",
            expected,
            readClipboardInForeground(),
        )
    }

    private fun waitForAttention(processed: String, clipboardSentinel: String) {
        awaitAssertion(timeoutMs = 15_000L, pollMs = 100L) {
            onView(withText(R.string.browser_retry)).check(matches(isDisplayed()))
            onView(withId(R.id.editTextUrl)).check(matches(withText(processed)))
            assertEquals(
                clipboardSentinel,
                clipboard.primaryClip?.getItemAt(0)?.text?.toString(),
            )
        }
    }

    private fun awaitAskDialog() {
        awaitAssertion {
            onView(withText(R.string.post_clean_action_title))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    private fun viewIntent(url: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            setClass(context, MainActivity::class.java)
        }

    private fun readClipboardInForeground(): String? {
        var value: String? = null
        ActivityScenario.launch<MainActivity>(mainIntent()).use { scenario ->
            scenario.onActivity {
                value = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            }
        }
        return value
    }

    private fun clearClipboardInForeground() {
        ActivityScenario.launch<MainActivity>(mainIntent()).use { scenario ->
            scenario.onActivity {
                if (android.os.Build.VERSION.SDK_INT >= 28) clipboard.clearPrimaryClip()
                else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }

    private fun mainIntent(): Intent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_MAIN
    }
}
