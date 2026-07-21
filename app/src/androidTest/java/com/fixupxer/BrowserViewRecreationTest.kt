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

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.View
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingRootException
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.data.database.FixupXerDatabase
import com.fixupxer.data.database.MIGRATION_1_2
import com.fixupxer.utils.BrowserModeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserViewRecreationTest {

    private lateinit var context: android.content.Context
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var database: FixupXerDatabase
    private var previousBrowserModeEnabled = false
    private var previousActionMode = PreferencesManager.ACTION_MODE_ASK
    private var previousHistoryEnabled = true

    private val testUrl = "https://www.instagram.com/p/ABC/?igsh=xyz"

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        preferencesManager = PreferencesManager(context)
        database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FixupXerDatabase::class.java,
            "fixupxer_database",
        )
            .addMigrations(MIGRATION_1_2)
            .build()

        previousBrowserModeEnabled = preferencesManager.isBrowserModeEnabled()
        previousActionMode = preferencesManager.getActionMode()
        previousHistoryEnabled = preferencesManager.isHistoryEnabled()

        preferencesManager.setBrowserModeEnabled(true)
        BrowserModeUtils.setBrowserAliasEnabled(context, true)
        preferencesManager.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        preferencesManager.setHistoryEnabled(true)

        runBlocking {
            database.urlHistoryDao().deleteAll()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            database.urlHistoryDao().deleteAll()
        }
        database.close()
        preferencesManager.setBrowserModeEnabled(previousBrowserModeEnabled)
        preferencesManager.setActionMode(previousActionMode)
        preferencesManager.setHistoryEnabled(previousHistoryEnabled)
        BrowserModeUtils.setBrowserAliasEnabled(context, false)
    }

    @Test
    fun testBrowserViewRecreationDoesNotDuplicateHistory() {
        // Explicit component: an implicit VIEW intent would resolve to the
        // emulator's default browser instead of MainActivity under test.
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(testUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            setClass(context, MainActivity::class.java)
        }

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            waitForPostCleanDialog()

            scenario.recreate()

            waitForPostCleanDialog()

            val matchingEntries = runBlocking {
                database.urlHistoryDao().getAllHistory().first()
                    .filter { it.originalUrl == testUrl }
            }
            assertEquals(1, matchingEntries.size)
        }
    }

    private fun waitForPostCleanDialog() {
        val deadline = SystemClock.uptimeMillis() + DIALOG_TIMEOUT_MS
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                onView(withText(R.string.post_clean_action_title))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                return
            } catch (_: NoMatchingViewException) {
                // Dialog is not attached yet.
            } catch (_: NoMatchingRootException) {
                // Dialog window is not attached yet.
            }
            onView(isRoot()).perform(waitFor(POLL_INTERVAL_MS))
        }

        onView(withText(R.string.post_clean_action_title))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    private fun waitFor(delay: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints() = isRoot()
            override fun getDescription() = "Wait for $delay milliseconds."
            override fun perform(uiController: UiController, view: View?) {
                uiController.loopMainThreadForAtLeast(delay)
            }
        }
    }

    private companion object {
        const val DIALOG_TIMEOUT_MS = 10_000L
        const val POLL_INTERVAL_MS = 200L
    }
}
