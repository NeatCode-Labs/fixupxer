// SPDX-License-Identifier: GPL-3.0-or-later
/* Copyright (C) 2026 NeatCode Labs */
package com.fixupxer

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.ui.BrowserSettingsActivity
import com.fixupxer.ui.FrontendSettingsActivity
import com.fixupxer.ui.RuleEditorActivity
import com.fixupxer.ui.SettingsActivity
import com.fixupxer.ui.ShareActivity
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.Constants
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Checks real UI wiring and Android intent dispatch, blocking external network effects. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class HelpNavigationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val outgoing = mutableListOf<Intent>()
    private var previousBrowserMode = false
    private var previousAlias = false
    private val monitor = object : Instrumentation.ActivityMonitor() {
        override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
            if (intent.action !in setOf(Intent.ACTION_VIEW, Intent.ACTION_CHOOSER)) return null
            outgoing.add(Intent(intent))
            return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
        }
    }

    @Before
    fun setup() {
        val preferences = PreferencesManager(context)
        previousBrowserMode = preferences.isBrowserModeEnabled()
        previousAlias = BrowserModeUtils.isBrowserAliasEnabled(context)
        preferences.setBrowserModeEnabled(true)
        BrowserModeUtils.setBrowserAliasEnabled(context, true)
        instrumentation.addMonitor(monitor)
    }

    @After
    fun cleanup() {
        instrumentation.removeMonitor(monitor)
        PreferencesManager(context).setBrowserModeEnabled(previousBrowserMode)
        BrowserModeUtils.setBrowserAliasEnabled(context, previousAlias)
    }

    @Suppress("DEPRECATION")
    private fun assertExternalDestination(expectedUrl: String) {
        assertEquals("One external handoff per tap", 1, outgoing.size)
        val launched = outgoing.single()
        val candidates = if (launched.action == Intent.ACTION_CHOOSER) {
            listOfNotNull(launched.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)) +
                launched.getParcelableArrayExtra(Intent.EXTRA_INITIAL_INTENTS)
                    .orEmpty().filterIsInstance<Intent>()
        } else listOf(launched)
        assertFalse("A browser must be offered", candidates.isEmpty())
        candidates.forEach { intent ->
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals(expectedUrl, intent.dataString)
            assertTrue("Help must target an external browser explicitly", intent.component != null)
            assertNotEquals(context.packageName, intent.component!!.packageName)
        }
        outgoing.clear()
    }

    private fun assertDestination(fragment: String?) =
        assertExternalDestination(Constants.HELP_URL + (fragment?.let { "#$it" } ?: ""))

    private fun follow(id: Int, fragment: String) {
        onView(withId(id)).perform(nestedScrollTo(), click())
        assertDestination(fragment)
    }

    @Test
    fun mainAndShareMenusOpenHelpContentsOutsideFixupxer() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openActionBarOverflowOrOptionsMenu(context)
            onView(withText(R.string.help_menu)).perform(click())
            assertDestination(null)
        }
        val share = Intent(context, ShareActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://example.com/help-test?utm_source=test")
        }
        ActivityScenario.launch<ShareActivity>(share).use {
            openActionBarOverflowOrOptionsMenu(context)
            onView(withText(R.string.help_menu)).perform(click())
            assertDestination(null)
        }
    }

    @Test
    fun markedWebsiteLinksOpenOutsideFixupxerFromMainAndShare() {
        val checks: List<() -> Unit> = listOf(
            {
                openActionBarOverflowOrOptionsMenu(context)
                onView(withText(R.string.whats_new)).perform(click())
                assertExternalDestination(Constants.RELEASE_NOTES_URL)
            },
            {
                onView(withId(R.id.footerTextView)).perform(click())
                assertExternalDestination(Constants.WEBSITE_URL)
            },
            {
                openActionBarOverflowOrOptionsMenu(context)
                onView(withText(R.string.donate)).perform(click())
                assertTrue("Donate menu opens an internal dialog", outgoing.isEmpty())
                onView(withId(R.id.buttonDonate)).check(matches(withText(R.string.donate_browser)))
                    .perform(click())
                assertExternalDestination(Constants.DONATION_URL)
            },
        )
        val share = Intent(context, ShareActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://example.com/help-test")
        }
        // Share is intentionally noHistory: each external exit needs a fresh entry.
        checks.forEach { check ->
            ActivityScenario.launch(MainActivity::class.java).use { check() }
            ActivityScenario.launch<ShareActivity>(share).use { check() }
        }
    }

    @Test
    fun disclaimerSourceLinkHasExternalIndicatorAndBrowserHandoff() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openActionBarOverflowOrOptionsMenu(context)
            onView(withText(R.string.disclaimer)).perform(click())
            onView(withId(R.id.textViewDisclaimerContent)).check { view, error ->
                if (error != null) throw error
                val textView = view as android.widget.TextView
                val text = textView.text as android.text.Spanned
                val links = text.getSpans(0, text.length, android.text.style.ClickableSpan::class.java)
                assertEquals(1, links.size)
                val link = links.single()
                assertTrue(text.subSequence(text.getSpanStart(link), text.getSpanEnd(link)).contains('↗'))
                link.onClick(textView)
            }
            assertExternalDestination(Constants.GITHUB_REPOSITORY_URL)
        }
    }

    @Test
    fun browserLinksAnnounceExternalNavigationAndReachSpecificTopics() {
        ActivityScenario.launch(BrowserSettingsActivity::class.java).use {
            onView(withId(R.id.buttonBrowserModeGuide))
                .perform(nestedScrollTo())
                .check(matches(hasDescendant(withText(R.string.help_opens_in_browser))))
                .check(matches(withContentDescription(context.getString(
                    R.string.help_link_description, context.getString(R.string.browser_mode_guide),
                ))))
            follow(R.id.buttonBrowserModeGuide, "browser-mode")
            follow(R.id.browserFrontendsHelp, "browser-frontends")
            follow(R.id.afterCleanHelp, "after-clean")
        }
    }

    @Test
    fun settingsAndFrontendHelpReachSpecificTopics() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            follow(R.id.appSettingsHelp, "app-settings")
            follow(R.id.linkCleaningHelp, "link-cleaning")
            follow(R.id.buttonCustomRulesHowTo, "custom-rules")
            follow(R.id.historyBackupHelp, "history-and-backup")
        }
        ActivityScenario.launch(FrontendSettingsActivity::class.java).use {
            follow(R.id.frontendsHelp, "alternative-frontends")
        }
    }

    @Test
    fun editorHelpKeepsUnsavedInputAcrossBackgroundAndReturn() {
        ActivityScenario.launch(RuleEditorActivity::class.java).use { scenario ->
            onView(withId(R.id.editName)).perform(replaceText("Help draft"), closeSoftKeyboard())
            follow(R.id.customRulesHelp, "custom-rules")
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(withId(R.id.editName)).check(matches(withText("Help draft")))
            onView(withId(R.id.buttonTeachToggle)).perform(nestedScrollTo(), click())
            follow(R.id.teachExampleHelp, "teach-from-example")
            follow(R.id.testLabHelp, "test-lab")
        }
    }
}
