// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
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

package com.fixupxer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.ui.SettingsActivity
import com.fixupxer.ui.ShareActivity
import com.fixupxer.ui.helpers.DominantHandLayoutHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DominantHandLayoutInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun mainActivityUsesPhysicalOrderForBothHandsAndDirections() {
        handDirectionCases().forEach { case ->
            PreferencesManager(context).setDominantHand(case.hand)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                applyAndLayout(scenario, case.hand, case.layoutDirection)
                scenario.onActivity { activity ->
                    assertPhysicalLayout(activity, case.expectedPhysicalOrder, case.historyOnLeft)
                }
            }
        }
    }

    @Test
    fun shareActivityUsesPhysicalOrderForBothHandsAndDirections() {
        handDirectionCases().forEach { case ->
            PreferencesManager(context).setDominantHand(case.hand)
            launchShareActivity().use { scenario ->
                applyAndLayout(scenario, case.hand, case.layoutDirection)
                scenario.onActivity { activity ->
                    assertPhysicalLayout(activity, case.expectedPhysicalOrder, case.historyOnLeft)
                }
            }
        }
    }

    @Test
    fun historyActionKeepsTouchTargetDescriptionEnabledStateAndClickFlow() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity(::assertHistoryAction)
            onView(withId(R.id.buttonHistory)).perform(click())
            onView(withText(R.string.history_title))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            pressBack()
        }

        launchShareActivity().use { scenario ->
            scenario.onActivity(::assertHistoryAction)
            onView(withId(R.id.buttonHistory)).perform(click())
            onView(withText(R.string.history_title))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            pressBack()
        }
    }

    @Test
    fun mainActivityAppliesHandChangeAfterSettingsRoundTrip() {
        PreferencesManager(context).setDominantHand(PreferencesManager.DOMINANT_HAND_RIGHT)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val row = activity.findViewById<LinearLayout>(R.id.actionRow)
                row.layoutDirection = View.LAYOUT_DIRECTION_LTR
                applyLayout(activity, PreferencesManager.DOMINANT_HAND_RIGHT)
                activity.startActivity(Intent(activity, SettingsActivity::class.java))
            }

            onView(withId(R.id.buttonHandLeft)).perform(scrollTo(), click())
            pressBack()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onActivity { activity ->
                assertEquals(
                    PreferencesManager.DOMINANT_HAND_LEFT,
                    PreferencesManager(activity).getDominantHand(),
                )
                assertPhysicalLayout(
                    activity,
                    listOf(R.id.buttonShare, R.id.buttonCopy, R.id.buttonOpen),
                    historyOnLeft = false,
                )
            }
        }
    }

    @Test
    fun settingsInitialSelectionDoesNotPersistTheDefault() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var dominantHandChanged = false
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_DOMINANT_HAND) dominantHandChanged = true
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        try {
            ActivityScenario.launch(SettingsActivity::class.java).use {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                assertFalse(dominantHandChanged)
                assertFalse(prefs.contains(KEY_DOMINANT_HAND))
            }
        } finally {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    @Test
    fun mainAndShareReapplyPersistedHandAfterConfigurationRecreation() {
        PreferencesManager(context).setDominantHand(PreferencesManager.DOMINANT_HAND_LEFT)

        // ActivityScenario recreation exercises the same destroy/create path as rotation
        // without mutating device-wide orientation or locale for neighboring tests.
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.recreate()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertPhysicalLayout(
                    activity,
                    listOf(R.id.buttonShare, R.id.buttonCopy, R.id.buttonOpen),
                    historyOnLeft = false,
                )
            }
        }

        launchShareActivity().use { scenario ->
            scenario.recreate()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertPhysicalLayout(
                    activity,
                    listOf(R.id.buttonShare, R.id.buttonCopy, R.id.buttonOpen),
                    historyOnLeft = false,
                )
            }
        }
    }

    private fun <T : Activity> applyAndLayout(
        scenario: ActivityScenario<T>,
        hand: String,
        layoutDirection: Int,
    ) {
        scenario.onActivity { activity ->
            activity.findViewById<LinearLayout>(R.id.actionRow).layoutDirection = layoutDirection
            applyLayout(activity, hand)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun applyLayout(activity: Activity, hand: String) {
        DominantHandLayoutHelper.apply(
            actionRow = activity.findViewById(R.id.actionRow),
            openButton = activity.findViewById(R.id.buttonOpen),
            copyButton = activity.findViewById(R.id.buttonCopy),
            shareButton = activity.findViewById(R.id.buttonShare),
            historyButton = activity.findViewById(R.id.buttonHistory),
            hand = hand,
            actionGapPx = activity.resources.getDimensionPixelSize(R.dimen.margin_small),
            historyEdgeMarginPx = activity.resources.getDimensionPixelSize(R.dimen.margin_medium),
        )
    }

    private fun assertPhysicalLayout(
        activity: Activity,
        expectedPhysicalOrder: List<Int>,
        historyOnLeft: Boolean,
    ) {
        val actualPhysicalOrder = listOf(
            activity.findViewById<View>(R.id.buttonOpen),
            activity.findViewById(R.id.buttonCopy),
            activity.findViewById(R.id.buttonShare),
        ).sortedBy { (it.left + it.right) / 2 }.map { it.id }
        assertEquals(expectedPhysicalOrder, actualPhysicalOrder)

        val history = activity.findViewById<View>(R.id.buttonHistory)
        val historyParent = history.parent as View
        val historyCenter = (history.left + history.right) / 2
        if (historyOnLeft) {
            assertTrue(historyCenter < historyParent.width / 2)
        } else {
            assertTrue(historyCenter > historyParent.width / 2)
        }
    }

    private fun assertHistoryAction(activity: Activity) {
        val history = activity.findViewById<View>(R.id.buttonHistory)
        val minimumSize =
            activity.resources.getDimensionPixelSize(R.dimen.min_touch_target_size)
        assertTrue(history.width >= minimumSize)
        assertTrue(history.height >= minimumSize)
        assertEquals(activity.getString(R.string.history_title), history.contentDescription)
        assertTrue(history.isEnabled)
    }

    private fun launchShareActivity(): ActivityScenario<ShareActivity> {
        val intent = Intent(context, ShareActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://example.com/?utm_source=test")
        }
        return ActivityScenario.launch(intent)
    }

    private fun handDirectionCases(): List<Case> = listOf(
        Case(
            PreferencesManager.DOMINANT_HAND_RIGHT,
            View.LAYOUT_DIRECTION_LTR,
            listOf(R.id.buttonOpen, R.id.buttonCopy, R.id.buttonShare),
            historyOnLeft = true,
        ),
        Case(
            PreferencesManager.DOMINANT_HAND_RIGHT,
            View.LAYOUT_DIRECTION_RTL,
            listOf(R.id.buttonOpen, R.id.buttonCopy, R.id.buttonShare),
            historyOnLeft = true,
        ),
        Case(
            PreferencesManager.DOMINANT_HAND_LEFT,
            View.LAYOUT_DIRECTION_LTR,
            listOf(R.id.buttonShare, R.id.buttonCopy, R.id.buttonOpen),
            historyOnLeft = false,
        ),
        Case(
            PreferencesManager.DOMINANT_HAND_LEFT,
            View.LAYOUT_DIRECTION_RTL,
            listOf(R.id.buttonShare, R.id.buttonCopy, R.id.buttonOpen),
            historyOnLeft = false,
        ),
    )

    private data class Case(
        val hand: String,
        val layoutDirection: Int,
        val expectedPhysicalOrder: List<Int>,
        val historyOnLeft: Boolean,
    )

    private companion object {
        const val PREFS_NAME = "FixupXerPrefs"
        const val KEY_DOMINANT_HAND = "dominant_hand"
    }
}
