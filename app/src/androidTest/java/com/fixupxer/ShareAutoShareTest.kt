// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2026 NeatCode Labs
 */
package com.fixupxer

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.fixupxer.backup.SettingsSnapshot
import com.fixupxer.ui.ShareActivity
import com.fixupxer.ui.helpers.UrlActionHelper
import com.fixupxer.utils.Constants
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ShareAfterCleanAction
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

/** Intent interception verifies dispatch without sending test links to installed apps. */
@RunWith(AndroidJUnit4::class)
class ShareAutoShareTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var preferences: PreferencesManager
    private lateinit var original: SettingsSnapshot
    private val choosers = CopyOnWriteArrayList<Intent>()
    private val resultInflatedAtLaunch = CopyOnWriteArrayList<Boolean>()
    private val monitor = object : Instrumentation.ActivityMonitor() {
        override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
            if (intent.action != Intent.ACTION_CHOOSER) return null
            val current = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<ShareActivity>().single()
            resultInflatedAtLaunch.add(current.findViewById<View>(R.id.textViewProcessedUrl) != null)
            choosers.add(intent)
            return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
        }
    }

    @Before
    fun setUp() {
        preferences = PreferencesManager(context)
        original = preferences.exportSettingsSnapshot()
        preferences.setCustomRulesEnabled(false)
        preferences.setHistoryEnabled(false)
        preferences.setAutoCopySharedLinksEnabled(false)
        preferences.setShareAfterCleanAction(ShareAfterCleanAction.SHARE)
        instrumentation.addMonitor(monitor)
    }

    @After
    fun tearDown() {
        instrumentation.removeMonitor(monitor)
        assertTrue(preferences.replaceSettingsSnapshot(original))
    }

    private fun send(text: String, clipOnly: Boolean = false, preview: Boolean = false) =
        Intent(context, ShareActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            if (clipOnly) clipData = ClipData.newPlainText("fixture", text)
            else putExtra(Intent.EXTRA_TEXT, text)
            if (preview) putExtra(UrlActionHelper.EXTRA_SKIP_AUTO_SHARE, true)
        }

    @Suppress("DEPRECATION")
    private fun sharedUrl(index: Int = 0): String? {
        val send = choosers[index].getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/plain", send.type)
        assertTrue(send.getBooleanExtra(UrlActionHelper.EXTRA_SKIP_AUTO_SHARE, false))
        return send.getStringExtra(Intent.EXTRA_TEXT)
    }

    private fun awaitResult(scenario: ActivityScenario<ShareActivity>, text: String) {
        awaitAssertion {
            scenario.onActivity { activity ->
                assertEquals(text, activity.findViewById<TextView>(R.id.textViewProcessedUrl)?.text?.toString())
                assertTrue(activity.findViewById<View>(R.id.progressIndicator).visibility != View.VISIBLE)
            }
        }
    }

    @Test
    fun clipDataCleansWithoutInflatingResultAndEachNewRequestSharesOnce() {
        val input = "https://example.com/article?utm_source=fixture"
        // The monitor's synthetic result pauses the Activity just like external
        // handoff. Inspect the view at dispatch, before the one-shot screen closes.
        for (count in 1..2) {
            ActivityScenario.launch<ShareActivity>(send(input, clipOnly = true)).use {
                awaitAssertion { assertEquals(count, choosers.size) }
                assertEquals("https://example.com/article", sharedUrl(count - 1))
                assertFalse(resultInflatedAtLaunch[count - 1])
            }
            assertEquals(count, choosers.size)
        }
    }

    @Test
    fun autoCopyCompletesBeforeChooserAndFinalFrontendIsShared() {
        preferences.setAutoCopySharedLinksEnabled(true)
        preferences.setConvertTwitterEnabled(true)
        preferences.setSelectedProxyDomain(ProxyPlatform.X, Constants.FIXUPX_DOMAIN)
        ActivityScenario.launch<ShareActivity>(send("https://x.com/person/status/123?utm_source=fixture")).use {
            awaitAssertion { assertEquals(1, choosers.size) }
            assertEquals("https://fixupx.com/person/status/123", sharedUrl())
            assertFalse(resultInflatedAtLaunch.single())
        }
        // A transparent one-shot entry may dispatch before gaining window focus.
        // Android denies clipboard reads without focus, so inspect the completed
        // write from a focused app window after the intercepted handoff.
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitAssertion {
                scenario.onActivity { activity ->
                    assertTrue(activity.hasWindowFocus())
                    val clip = (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                    assertEquals(sharedUrl(), clip?.getItemAt(0)?.text?.toString())
                }
            }
        }
    }

    @Test
    fun alreadyCleanLinkAlsoOpensChooser() {
        ActivityScenario.launch<ShareActivity>(send("https://example.com/clean")).use {
            awaitAssertion { assertEquals(1, choosers.size) }
            assertEquals("https://example.com/clean", sharedUrl())
        }
    }

    @Test
    fun disabledPreferenceAndInternalPreviewKeepResult() {
        preferences.setShareAfterCleanAction(ShareAfterCleanAction.SHOW_RESULT)
        ActivityScenario.launch<ShareActivity>(send("https://example.com/clean")).use {
            awaitResult(it, "https://example.com/clean")
            assertTrue(choosers.isEmpty())
        }
        preferences.setShareAfterCleanAction(ShareAfterCleanAction.SHARE)
        ActivityScenario.launch<ShareActivity>(send("https://example.com/clean", preview = true)).use {
            awaitResult(it, "https://example.com/clean")
            assertTrue(choosers.isEmpty())
        }
    }

    @Test
    fun linkGuardShowsReviewAndDoesNotLaunchChooser() {
        val url = "https://example.com/?email=person%40example.com"
        ActivityScenario.launch<ShareActivity>(send(url)).use { scenario ->
            awaitResult(scenario, url)
            scenario.onActivity { activity ->
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.leakWarningRow).visibility)
                assertEquals(context.getString(R.string.share_after_clean_blocked), activity.findViewById<TextView>(R.id.autoCopyWarning).text.toString())
            }
            assertTrue(choosers.isEmpty())
            scenario.recreate()
            awaitResult(scenario, url)
            assertTrue(choosers.isEmpty())
        }
    }

    @Test
    fun malformedAndMultipleLinksShowErrorWithoutSharing() {
        for ((input, error) in listOf(
            "plain text" to R.string.error_no_url_found_in_shared_text,
            "https://example.com/a https://example.com/b" to R.string.error_multiple_urls,
        )) {
            ActivityScenario.launch<ShareActivity>(send(input)).use { scenario ->
                awaitResult(scenario, context.getString(error))
                scenario.onActivity { assertFalse(it.findViewById<View>(R.id.buttonShare).isEnabled) }
                assertTrue(choosers.isEmpty())
            }
        }
    }
}
