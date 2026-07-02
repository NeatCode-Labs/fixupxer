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
import dagger.hilt.android.HiltAndroidApp
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

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Plant a tree that only logs errors in release builds
            Timber.plant(ReleaseTree())
        }
        
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