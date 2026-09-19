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

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.fixupxer.backup.SettingsSnapshot
import com.fixupxer.processing.BrowserFrontendPreference
import com.fixupxer.ui.SettingsActivity
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.InputValidator
import com.fixupxer.utils.ProxyPlatform
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 23) // Fixture observes RecentTaskInfo activity counts/topActivity.
@Suppress("DEPRECATION")
class BrowserTaskCompletionDeviceTest {
    private lateinit var context: Context
    private lateinit var preferences: PreferencesManager
    private lateinit var originalSettings: SettingsSnapshot
    private var originalAliasEnabled = false
    private val ownedTaskIds = mutableSetOf<Int>()
    private val manager get() = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val url = "https://example.com/recents?utm_source=test"

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        preferences = PreferencesManager(context)
        originalSettings = preferences.exportSettingsSnapshot()
        originalAliasEnabled = BrowserModeUtils.isBrowserAliasEnabled(context)
        preferences.setHistoryEnabled(false)
        preferences.setCustomRulesEnabled(false)
        preferences.clearRememberedRoutes()
        assertTrue(preferences.saveBrowserFrontendPreferences(
            ProxyPlatform.entries.associateWith { BrowserFrontendPreference.CLEAN_ONLY },
            preferences.getBrowserFrontendPreferences(),
        ))
        assertTrue(BrowserModeUtils.updateBrowserMode(context, preferences, true).success)
        preferences.setActionMode(PreferencesManager.ACTION_MODE_ASK)
        // These tests exercise task completion after valid input, not cold-start
        // validator timing. Assert the precondition before launching the UI.
        assertEquals(url, runBlocking { InputValidator.validateAndSanitizeInput(url) })
    }

    @After
    fun tearDown() {
        // Only tasks created and recorded by this fixture may be removed.
        manager.appTasks.filter { it.taskInfo.persistentId in ownedTaskIds }.forEach { it.finishAndRemoveTask() }
        assertTrue(preferences.replaceSettingsSnapshot(originalSettings))
        assertTrue(BrowserModeUtils.setBrowserAliasEnabled(context, originalAliasEnabled))
    }

    @Test
    fun completedBrowserAliasTaskIsRemovedFromRecents() {
        completeFreshBrowserTask()
    }

    @Test
    fun intentionalNewViewWithSameUrlStillCompletes() {
        val first = completeFreshBrowserTask()
        val second = completeFreshBrowserTask()
        assertTrue("A new VIEW must remain a new transaction", first != second)
    }

    @Test
    fun cancellationPreservesTaskAndAllowsExplicitRetry() {
        ActivityScenario.launch<MainActivity>(freshView()).use { scenario ->
            waitForPicker()
            var taskId = -1
            scenario.onActivity { taskId = rememberTask(it) }
            pressBack()
            onView(withId(R.id.buttonProcess)).check(matches(withText(R.string.browser_retry)))
            assertNotNull(taskInfo(taskId))
            onView(withId(R.id.buttonProcess)).perform(click())
            waitForPicker()
            completeWithClipboard()
            awaitAssertion { assertEquals(null, taskInfo(taskId)) }
        }
    }

    @Test
    fun viewDeliveredToLauncherTaskDoesNotRemoveThatTask() {
        val launcher = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        // ActivityScenario ignores destruction after MainActivity.setIntent(VIEW)
        // because its lifecycle matcher still expects the original MAIN intent.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(launcher) as MainActivity
        try {
            val taskId = rememberTask(activity)
            instrumentation.runOnMainSync {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setClass(context, MainActivity::class.java)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                })
            }
            waitForPicker()
            completeWithClipboard()
            awaitAssertion {
                val task = taskInfo(taskId)
                assertNotNull("The ordinary launcher task must remain", task)
                assertEquals(Intent.ACTION_MAIN, task!!.baseIntent.action)
                assertEquals(0, task.numActivities)
            }
        } finally {
            instrumentation.runOnMainSync { if (!activity.isFinishing) activity.finish() }
        }
    }

    @Test
    fun browserActivityInsideCallerTaskReturnsToCaller() {
        val caller = Intent(context, SettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        ActivityScenario.launch<SettingsActivity>(caller).use { scenario ->
            var taskId = -1
            scenario.onActivity {
                taskId = rememberTask(it)
                it.startActivity(viewIntent())
            }
            waitForPicker()
            assertEquals(2, taskInfo(taskId)?.numActivities)
            completeWithClipboard()
            awaitAssertion {
                assertEquals(ComponentName(context, SettingsActivity::class.java), taskInfo(taskId)?.topActivity)
            }
            scenario.onActivity { assertFalse(it.isFinishing) }
        }
    }

    private fun completeFreshBrowserTask(): Int {
        var taskId = -1
        ActivityScenario.launch<MainActivity>(freshView()).use { scenario ->
            waitForPicker()
            scenario.onActivity {
                assertTrue(it.isTaskRoot)
                taskId = rememberTask(it)
            }
            assertEquals(viewIntent().component, taskInfo(taskId)?.baseIntent?.component)
            completeWithClipboard()
            // Assert before ActivityScenario.close(), which also performs cleanup.
            awaitAssertion { assertEquals(null, taskInfo(taskId)) }
        }
        return taskId
    }

    private fun viewIntent() = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        component = ComponentName(context.packageName, "${context.packageName}.BrowserAlias")
        addCategory(Intent.CATEGORY_BROWSABLE)
    }

    private fun freshView() = viewIntent().addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
    )

    private fun taskInfo(taskId: Int) = manager.appTasks.map { it.taskInfo }.firstOrNull { it.persistentId == taskId }

    private fun rememberTask(activity: Activity): Int {
        // Resolve while running, then track the persistent ID so an empty retained
        // Recents entry is still found after its active ID becomes -1.
        val task = manager.appTasks.map { it.taskInfo }.first { it.id == activity.taskId }
        return task.persistentId.also { ownedTaskIds += it }
    }

    private fun waitForPicker() = awaitAssertion(timeoutMs = 10_000L) {
        // Keep failed validation bounded by awaitAssertion, rather than Espresso's
        // much longer wait for a dialog root which may never be created.
        onView(withText(R.string.post_clean_action_title)).check(matches(isDisplayed()))
    }

    private fun completeWithClipboard() {
        onView(withText(R.string.action_clipboard)).inRoot(isDialog()).perform(click())
    }
}
