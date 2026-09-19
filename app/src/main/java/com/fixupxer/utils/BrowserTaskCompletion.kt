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

package com.fixupxer.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import timber.log.Timber

/** Completes a delivered browser action without leaving its VIEW intent in Recents. */
object BrowserTaskCompletion {
    fun finish(activity: Activity) {
        if (ownsTransientTask(activity)) {
            try {
                activity.finishAndRemoveTask()
                return
            } catch (error: RuntimeException) {
                Timber.w(error, "Could not remove completed browser task")
            }
        }
        activity.finish()
    }

    @Suppress("DEPRECATION") // RecentTaskInfo.id is available on every supported API (21+).
    private fun ownsTransientTask(activity: Activity): Boolean {
        if (!activity.isTaskRoot) return false
        return try {
            val manager = activity.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return false
            val task = manager.appTasks.asSequence().map { it.taskInfo }
                .firstOrNull { it.id == activity.taskId } ?: return false
            // The latest activity intent may be a VIEW delivered to a launcher/caller
            // task. Only the task's original browser entry point establishes ownership.
            val browserAlias = ComponentName(activity.packageName, "${activity.packageName}.BrowserAlias")
            if (task.baseIntent.action != Intent.ACTION_VIEW || task.baseIntent.component != browserAlias) {
                return false
            }
            // RecentTaskInfo only gained numActivities in API 23. On Lollipop,
            // getRunningTasks still exposes our own tasks without GET_TASKS.
            val activityCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                task.numActivities
            } else {
                manager.getRunningTasks(Int.MAX_VALUE)
                    .firstOrNull { it.id == activity.taskId }?.numActivities
            }
            activityCount == 1
        } catch (error: RuntimeException) {
            Timber.w(error, "Could not determine browser task ownership")
            false
        }
    }
}
