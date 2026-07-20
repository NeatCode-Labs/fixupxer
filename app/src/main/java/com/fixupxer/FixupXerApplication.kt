// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2025  NeatCode Labs
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

import android.app.Application
import com.fixupxer.backup.LocalBackupManager
import com.fixupxer.ui.helpers.ThemeHelper
import com.fixupxer.utils.BrowserModeUtils
import com.fixupxer.utils.BrowserViewGate
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for FixupXer
 * Handles app-wide initialization and dependency injection setup
 */
@HiltAndroidApp
class FixupXerApplication : Application() {

    // Injected at app start so its init block seeds InstagramProxyStore with the
    // persisted custom proxies before any URL processing can happen.
    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var localBackupManager: LocalBackupManager

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Plant a tree that only logs errors in release builds
            Timber.plant(ReleaseTree())
        }

        if (localBackupManager.hasInterruptedRestore()) {
            BrowserViewGate.pause()
            try {
                // This rare startup-only path must finish before an Activity can read a
                // partially restored settings/rules pair. Room work stays on IO.
                runBlocking(Dispatchers.IO) {
                    localBackupManager.recoverInterruptedRestore()
                        .onFailure { Timber.e(it, "Interrupted restore recovery failed") }
                }
            } finally {
                BrowserViewGate.resume()
            }
        }

        ThemeHelper.apply(preferencesManager.getThemeMode())
        // Deliberately NOT applying DynamicColors: on Android 12+ wallpaper-based
        // dynamic colors would override the hand-tuned M3 brand palette
        // (colors.xml + values-night) that the redesign is built around.

        // Cloud/local preference restore can outlive PackageManager component state.
        // Reconcile before any Activity can accept a Browser VIEW intent.
        BrowserModeUtils.reconcileBrowserAlias(this, preferencesManager)
        
        Timber.d("FixupXer Application initialized")
    }
    
    /**
     * Custom Timber tree for release builds that only logs warnings and errors
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.WARN || priority == android.util.Log.ERROR) {
                // In a real app, you might want to send these to a crash reporting service
                super.log(priority, tag, message, t)
            }
        }
    }
} 