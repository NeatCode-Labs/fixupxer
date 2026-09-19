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
 */

package com.fixupxer

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.fixupxer.utils.BrowserTaskCompletion
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.never
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BrowserTaskCompletionTest {

    @Test
    @Config(sdk = [21])
    fun `API 21 browser task uses own running task count`() {
        val taskId = 51
        val manager = activityManager(appTask(taskId, Intent.ACTION_VIEW, browserAlias(), 1))
        whenever(manager.getRunningTasks(Int.MAX_VALUE)).thenReturn(listOf(
            ActivityManager.RunningTaskInfo().apply { id = taskId; numActivities = 1 },
        ))
        val activity = activity(manager, taskId, isTaskRoot = true)

        BrowserTaskCompletion.finish(activity)

        verify(activity).finishAndRemoveTask()
        verify(activity, never()).finish()
    }

    @Test
    @Config(sdk = [21])
    fun `API 21 resolved alias keeps its original Browser entry point`() {
        val taskId = 53
        val manager = activityManager(appTask(
            taskId, Intent.ACTION_VIEW, ComponentName(appPackage, "$appPackage.MainActivity"),
            numActivities = 1, originalComponent = browserAlias(),
        ))
        whenever(manager.getRunningTasks(Int.MAX_VALUE)).thenReturn(listOf(
            ActivityManager.RunningTaskInfo().apply { id = taskId; numActivities = 1 },
        ))
        val activity = activity(manager, taskId, isTaskRoot = true)

        BrowserTaskCompletion.finish(activity)

        verify(activity).finishAndRemoveTask()
        verify(activity, never()).finish()
    }

    @Test
    fun `different original entry point is not treated as browser-owned`() {
        val taskId = 54
        val manager = activityManager(appTask(
            taskId, Intent.ACTION_VIEW, browserAlias(), numActivities = 1,
            originalComponent = ComponentName(appPackage, "$appPackage.OtherAlias"),
        ))
        val activity = activity(manager, taskId, isTaskRoot = true)

        BrowserTaskCompletion.finish(activity)

        verify(activity).finish()
        verify(activity, never()).finishAndRemoveTask()
    }

    @Test
    @Config(sdk = [21])
    fun `API 21 task containing another activity is preserved`() {
        val taskId = 52
        val manager = activityManager(appTask(taskId, Intent.ACTION_VIEW, browserAlias(), 2))
        whenever(manager.getRunningTasks(Int.MAX_VALUE)).thenReturn(listOf(
            ActivityManager.RunningTaskInfo().apply { id = taskId; numActivities = 2 },
        ))
        val activity = activity(manager, taskId, isTaskRoot = true)

        BrowserTaskCompletion.finish(activity)

        verify(activity).finish()
        verify(activity, never()).finishAndRemoveTask()
    }

    @Test
    fun `browser-owned root task is removed after completion`() {
        val taskId = 41
        val manager = activityManager(
            appTask(taskId, Intent.ACTION_VIEW, browserAlias(), numActivities = 1),
        )
        val activity = activity(manager, taskId, isTaskRoot = true)

        BrowserTaskCompletion.finish(activity)

        verify(activity).finishAndRemoveTask()
        verify(activity, never()).finish()
    }

    @Test
    fun `caller task root false is finished without removing its task`() {
        val taskId = 42
        val manager = activityManager(
            appTask(taskId, Intent.ACTION_VIEW, browserAlias(), numActivities = 1),
        )
        val activity = activity(manager, taskId, isTaskRoot = false)

        BrowserTaskCompletion.finish(activity)

        verify(activity).finish()
        verify(activity, never()).finishAndRemoveTask()
    }

    @Test
    fun `launcher task stays intact when latest activity intent is VIEW`() {
        val taskId = 43
        val manager = activityManager(
            appTask(taskId, Intent.ACTION_MAIN, ComponentName(appPackage, "$appPackage.MainActivity"),
                numActivities = 1, originalComponent = browserAlias()),
        )
        val activity = activity(manager, taskId, isTaskRoot = true)
        whenever(activity.intent).thenReturn(Intent(Intent.ACTION_VIEW, Uri.parse(testUri)))

        BrowserTaskCompletion.finish(activity)

        verify(activity).finish()
        verify(activity, never()).finishAndRemoveTask()
    }

    @Test
    fun `different base component is not treated as browser-owned`() {
        val taskId = 44
        val manager = activityManager(
            appTask(taskId, Intent.ACTION_VIEW, ComponentName(appPackage, "$appPackage.MainActivity"), numActivities = 1),
        )
        val activity = activity(manager, taskId, isTaskRoot = true)

        BrowserTaskCompletion.finish(activity)

        verify(activity).finish()
        verify(activity, never()).finishAndRemoveTask()
    }

    @Test
    fun `task with multiple activities is not removed`() {
        val taskId = 45
        val manager = activityManager(
            appTask(taskId, Intent.ACTION_VIEW, browserAlias(), numActivities = 2),
        )
        val activity = activity(manager, taskId, isTaskRoot = true)

        BrowserTaskCompletion.finish(activity)

        verify(activity).finish()
        verify(activity, never()).finishAndRemoveTask()
    }

    @Test
    fun `task lookup failure falls back to ordinary finish`() {
        val taskId = 46
        val manager = mock<ActivityManager>().also {
            whenever(it.appTasks).thenThrow(IllegalStateException("task list unavailable"))
        }
        val activity = activity(manager, taskId, isTaskRoot = true)

        BrowserTaskCompletion.finish(activity)

        verify(activity).finish()
        verify(activity, never()).finishAndRemoveTask()
    }

    private fun activity(
        manager: ActivityManager,
        taskId: Int,
        isTaskRoot: Boolean,
    ): Activity = mock<Activity>().also {
        whenever(it.isTaskRoot).thenReturn(isTaskRoot)
        whenever(it.taskId).thenReturn(taskId)
        whenever(it.packageName).thenReturn(appPackage)
        whenever(it.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(manager)
    }

    private fun activityManager(vararg tasks: ActivityManager.AppTask): ActivityManager =
        mock<ActivityManager>().also {
            whenever(it.appTasks).thenReturn(tasks.toList())
        }

    private fun appTask(
        taskId: Int,
        action: String,
        component: ComponentName,
        numActivities: Int,
        originalComponent: ComponentName? = null,
    ): ActivityManager.AppTask {
        val taskInfo = ActivityManager.RecentTaskInfo().apply {
            id = taskId
            baseIntent = Intent(action, Uri.parse(testUri)).setComponent(component)
            origActivity = originalComponent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) this.numActivities = numActivities
        }
        return mock<ActivityManager.AppTask>().also {
            whenever(it.taskInfo).thenReturn(taskInfo)
        }
    }

    private fun browserAlias(): ComponentName =
        ComponentName(appPackage, "$appPackage.BrowserAlias")

    private companion object {
        const val appPackage = "com.fixupxer.debug"
        const val testUri = "https://example.com/browser-task"
    }
}
