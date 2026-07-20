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

import com.fixupxer.utils.BrowserViewGate
import com.fixupxer.utils.BrowserViewHandoffPolicy
import com.fixupxer.ui.helpers.UrlActionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserViewGateTest {

    @Test
    fun `gate requires preference and alias at startup`() {
        assertNull(BrowserViewGate.begin(preferenceEnabled = false, aliasEnabled = true))
        assertNull(BrowserViewGate.begin(preferenceEnabled = true, aliasEnabled = false))
        assertNotNull(BrowserViewGate.begin(preferenceEnabled = true, aliasEnabled = true))
    }

    @Test
    fun `revision change invalidates in-flight view processing`() {
        val snapshot = BrowserViewGate.begin(true, true)!!
        assertTrue(BrowserViewGate.isValid(snapshot, true, true))

        BrowserViewGate.invalidate()

        assertFalse(BrowserViewGate.isValid(snapshot, true, true))
    }

    @Test
    fun `fresh state recheck rejects disabled browser mode`() {
        val snapshot = BrowserViewGate.begin(true, true)!!

        assertFalse(BrowserViewGate.isValid(snapshot, false, true))
        assertFalse(BrowserViewGate.isValid(snapshot, true, false))
    }

    @Test
    fun `paused gate rejects new and in-flight view processing`() {
        val snapshot = BrowserViewGate.begin(true, true)!!

        BrowserViewGate.pause()
        try {
            assertNull(BrowserViewGate.begin(true, true))
            assertFalse(BrowserViewGate.isValid(snapshot, true, true))
        } finally {
            BrowserViewGate.resume()
        }

        assertNotNull(BrowserViewGate.begin(true, true))
    }

    @Test
    fun `unmatched resume does not leave gate paused`() {
        assertThrows(IllegalStateException::class.java) {
            BrowserViewGate.resume()
        }

        assertNotNull(BrowserViewGate.begin(true, true))
        BrowserViewGate.pause()
        BrowserViewGate.resume()
        assertNotNull(BrowserViewGate.begin(true, true))
    }

    @Test
    fun `activity finishes only after successful external handoff`() {
        assertTrue(BrowserViewHandoffPolicy.shouldFinish(externalBrowserOpened = true))
        assertFalse(BrowserViewHandoffPolicy.shouldFinish(externalBrowserOpened = false))
    }

    @Test
    fun `external browser candidates include action view fallback and exclude self`() {
        assertEquals(
            linkedSetOf("org.standard.browser", "com.oem.browser"),
            UrlActionHelper.mergeExternalBrowserPackages(
                selectorPackages = listOf("org.standard.browser", "com.fixupxer"),
                actionViewPackages = listOf("com.oem.browser", "com.fixupxer"),
                ownPackage = "com.fixupxer",
            ),
        )
    }
}
