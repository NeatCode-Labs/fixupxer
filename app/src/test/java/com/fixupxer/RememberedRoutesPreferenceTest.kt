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

import com.fixupxer.backup.RememberedRoute
import com.fixupxer.backup.RememberedRouteKind
import com.fixupxer.utils.BrowserViewGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RememberedRoutesPreferenceTest {
    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setup() {
        preferencesManager = PreferencesManager(RuntimeEnvironment.getApplication().applicationContext)
        preferencesManager.clearRememberedRoutes()
    }

    @Test
    fun `routes are isolated by exact normalized host`() {
        assertTrue(
            preferencesManager.setRememberedRoute(
                "instagram.com",
                RememberedRoute(RememberedRouteKind.NATIVE, "com.instagram.android"),
            )
        )
        assertTrue(
            preferencesManager.setRememberedRoute(
                "www.instagram.com",
                RememberedRoute(RememberedRouteKind.BROWSER, "com.android.chrome"),
            )
        )
        assertEquals(2, preferencesManager.getRememberedRouteCount())
        assertEquals(
            RememberedRouteKind.NATIVE,
            preferencesManager.getRememberedRoute("instagram.com")?.kind,
        )
    }

    @Test
    fun `delete one route keeps others`() {
        preferencesManager.setRememberedRoute(
            "a.example",
            RememberedRoute(RememberedRouteKind.BROWSER, "com.android.chrome"),
        )
        preferencesManager.setRememberedRoute(
            "b.example",
            RememberedRoute(RememberedRouteKind.BROWSER, "com.android.chrome"),
        )
        preferencesManager.removeRememberedRoute("a.example")
        assertEquals(1, preferencesManager.getRememberedRouteCount())
    }

    @Test
    fun `route mutators invalidate in-flight browser work`() {
        val route = RememberedRoute(RememberedRouteKind.BROWSER, "com.android.chrome")

        val beforeSet = BrowserViewGate.begin(true, true)!!
        assertTrue(preferencesManager.setRememberedRoute("a.example", route))
        assertFalse(BrowserViewGate.isValid(beforeSet, true, true))

        val beforeRemove = BrowserViewGate.begin(true, true)!!
        preferencesManager.removeRememberedRoute("a.example")
        assertFalse(BrowserViewGate.isValid(beforeRemove, true, true))

        assertTrue(preferencesManager.setRememberedRoute("b.example", route))
        val beforeClear = BrowserViewGate.begin(true, true)!!
        preferencesManager.clearRememberedRoutes()
        assertFalse(BrowserViewGate.isValid(beforeClear, true, true))
    }

    @Test
    fun `settings snapshot round trip includes remembered routes`() {
        preferencesManager.setRememberedRoute(
            "reddit.com",
            RememberedRoute(RememberedRouteKind.NATIVE, "com.reddit.frontpage"),
        )
        val exported = preferencesManager.exportSettingsSnapshot()
        preferencesManager.clearRememberedRoutes()
        assertTrue(preferencesManager.replaceSettingsSnapshot(exported))
        assertEquals(1, preferencesManager.getRememberedRouteCount())
    }

    @Test
    fun `replace snapshot throws on invalid action priority without writing`() {
        preferencesManager.setBrowserModeEnabled(true)
        val valid = preferencesManager.exportSettingsSnapshot()
        val invalid = valid.copy(actionPriority = listOf(PreferencesManager.ACTION_NATIVE_APP))

        assertTrue(runCatching { preferencesManager.replaceSettingsSnapshot(invalid) }.isFailure)
        assertEquals(valid, preferencesManager.exportSettingsSnapshot())
    }

    @Test
    fun `replace snapshot throws on own package route`() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        val valid = preferencesManager.exportSettingsSnapshot()
        val invalid = valid.copy(
            rememberedRoutes = mapOf(
                "example.com" to RememberedRoute(RememberedRouteKind.BROWSER, context.packageName),
            ),
        )

        assertTrue(runCatching { preferencesManager.replaceSettingsSnapshot(invalid) }.isFailure)
        assertEquals(0, preferencesManager.getRememberedRouteCount())
    }

    @Test
    fun `replace snapshot keeps uninstalled browser route instead of filtering`() {
        val valid = preferencesManager.exportSettingsSnapshot()
        val foreign = valid.copy(
            rememberedRoutes = mapOf(
                "example.com" to RememberedRoute(RememberedRouteKind.BROWSER, "org.mozilla.firefox"),
            ),
        )

        assertTrue(preferencesManager.replaceSettingsSnapshot(foreign))
        assertEquals(
            "org.mozilla.firefox",
            preferencesManager.getRememberedRoute("example.com")?.packageName,
        )
    }
}
