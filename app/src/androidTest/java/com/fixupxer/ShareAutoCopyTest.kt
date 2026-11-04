// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2026 NeatCode Labs
 */
package com.fixupxer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.backup.SettingsSnapshot
import com.fixupxer.ui.ShareActivity
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyPlatform
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareAutoCopyTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var preferences: PreferencesManager
    private lateinit var original: SettingsSnapshot
    private val sentinel = "auto-copy-test-sentinel"

    @Before
    fun setUp() {
        preferences = PreferencesManager(context)
        original = preferences.exportSettingsSnapshot()
        preferences.setCustomRulesEnabled(false)
        preferences.setHistoryEnabled(false)
        preferences.setAutoCopySharedLinksEnabled(true)
    }

    @After
    fun tearDown() {
        assertTrue(preferences.replaceSettingsSnapshot(original))
    }

    private fun send(text: String, clipOnly: Boolean = false) = Intent(context, ShareActivity::class.java).apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        if (clipOnly) clipData = ClipData.newPlainText("fixture", text)
        else putExtra(Intent.EXTRA_TEXT, text)
    }

    private fun launch(): ActivityScenario<ShareActivity> = ActivityScenario.launch(send(""))

    private fun setClipboard(scenario: ActivityScenario<ShareActivity>, value: String = sentinel) {
        scenario.onActivity { activity ->
            (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("fixture", value))
        }
    }

    private fun assertClipboard(scenario: ActivityScenario<ShareActivity>, expected: String, sensitive: Boolean = false) {
        awaitAssertion {
            scenario.onActivity { activity ->
                val clip = (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip!!
                assertEquals(expected, clip.getItemAt(0).text.toString())
                if (sensitive) assertTrue(clip.description.extras!!.getBoolean("android.content.extra.IS_SENSITIVE"))
            }
        }
    }

    private fun awaitFinished(scenario: ActivityScenario<ShareActivity>) {
        awaitAssertion {
            scenario.onActivity { activity ->
                assertTrue(activity.findViewById<android.view.View>(R.id.progressIndicator).visibility != android.view.View.VISIBLE)
            }
        }
    }

    @Test
    fun cleansAndCopiesClipDataInputAndDoesNotReplayAfterRecreation() {
        launch().use { scenario ->
            setClipboard(scenario)
            scenario.onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, send("https://example.com/article?utm_source=fixture", clipOnly = true)) }
            assertClipboard(scenario, "https://example.com/article", sensitive = true)
            setClipboard(scenario)
            scenario.recreate()
            awaitFinished(scenario)
            assertClipboard(scenario, sentinel)
            // A genuinely new Share intent of the same text can copy again.
            scenario.onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, send("https://example.com/article?utm_source=fixture", clipOnly = true)) }
            assertClipboard(scenario, "https://example.com/article")
        }
    }

    @Test
    fun offKeepsClipboardAndManualCopyStillWorks() {
        preferences.setAutoCopySharedLinksEnabled(false)
        launch().use { scenario ->
            setClipboard(scenario)
            scenario.onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, send("https://example.com/article?utm_source=fixture")) }
            awaitFinished(scenario)
            assertClipboard(scenario, sentinel)
            onView(withId(R.id.buttonCopy)).perform(click())
            assertClipboard(scenario, "https://example.com/article")
        }
    }

    @Test
    fun alreadyCleanUrlIsCopied() {
        launch().use { scenario ->
            setClipboard(scenario)
            scenario.onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, send("https://example.com/article")) }
            assertClipboard(scenario, "https://example.com/article")
        }
    }

    @Test
    fun conversionToggleCopiesNewFinalResult() {
        preferences.setConvertTwitterEnabled(true)
        preferences.setSelectedProxyDomain(ProxyPlatform.X, Constants.FIXUPX_DOMAIN)
        launch().use { scenario ->
            scenario.onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, send("https://x.com/person/status/123?utm_source=fixture")) }
            assertClipboard(scenario, "https://fixupx.com/person/status/123")
            onView(withId(R.id.switchPlatform)).perform(click())
            assertClipboard(scenario, "https://x.com/person/status/123")
        }
    }

    @Test
    fun linkGuardPausesAutoCopyAndKeepsManualCopyAvailable() {
        val url = "https://example.com/?email=person%40example.com"
        launch().use { scenario ->
            setClipboard(scenario)
            scenario.onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, send(url)) }
            awaitFinished(scenario)
            onView(withId(R.id.autoCopyWarning)).check(matches(isDisplayed()))
            assertClipboard(scenario, sentinel)
            onView(withId(R.id.buttonCopy)).perform(click())
            assertClipboard(scenario, url, sensitive = true)
        }
    }

    @Test
    fun invalidAndMultipleUrlsLeaveClipboardAlone() {
        launch().use { scenario ->
            for (input in listOf("plain text", "https://example.com/a https://example.com/b")) {
                setClipboard(scenario)
                scenario.onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, send(input)) }
                awaitFinished(scenario)
                assertClipboard(scenario, sentinel)
            }
        }
    }
}
