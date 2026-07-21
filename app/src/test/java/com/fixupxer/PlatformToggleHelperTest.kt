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

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.TextView
import com.fixupxer.ui.helpers.PlatformToggleHelper
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import com.google.android.material.materialswitch.MaterialSwitch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PlatformToggleHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        ProxyRoster.reset()
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            R.style.Theme_FixupXer,
        )
    }

    @After
    fun tearDown() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        ProxyRoster.reset()
    }

    @Test
    fun `empty frontend state renders neutral disabled toggle without callback`() {
        val root = LayoutInflater.from(context).inflate(R.layout.include_platform_toggles, null)
        val manager = PreferencesManager(context)
        var toggled: Boolean? = null

        PlatformToggleHelper.bindPlatformToggle(
            context = context,
            container = root.findViewById(R.id.platformToggleContainer),
            monogram = root.findViewById(R.id.platformMonogram),
            title = root.findViewById(R.id.platformTitle),
            proxyRow = root.findViewById(R.id.platformProxyRow),
            proxyStatus = root.findViewById(R.id.textViewPlatformProxyStatus),
            changeProxy = root.findViewById(R.id.textViewChangeProxy),
            platformSwitch = root.findViewById(R.id.switchPlatform),
            platform = ProxyPlatform.FACEBOOK,
            preferencesManager = manager,
            conversionEnabled = true,
            proxySelectionRevision = 0,
            onToggle = { toggled = it },
            onChangeProxy = {},
        )

        val title = root.findViewById<TextView>(R.id.platformTitle)
        val platformSwitch = root.findViewById<MaterialSwitch>(R.id.switchPlatform)
        assertEquals(context.getString(R.string.no_frontend_active_title), title.text.toString())
        assertFalse(platformSwitch.isEnabled)
        assertFalse(platformSwitch.isChecked)
        assertNull(toggled)
        assertTrue(manager.isConvertFacebookEnabled())
    }

    private companion object {
        const val PREFS_NAME = "FixupXerPrefs"
    }
}
