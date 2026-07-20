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

import com.fixupxer.utils.BrowserModeUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserAliasTransactionTest {

    @Test
    fun `successful transaction persists desired preference and alias`() {
        var preference = false
        var alias = false

        val result = BrowserModeUtils.executeBrowserAliasTransaction(
            desiredEnabled = true,
            readPreference = { preference },
            writePreference = {
                preference = it
                true
            },
            readAlias = { alias },
            writeAlias = {
                alias = it
                true
            },
        )

        assertTrue(result.success)
        assertTrue(result.rollbackSucceeded)
        assertTrue(preference)
        assertTrue(alias)
    }

    @Test
    fun `alias failure rolls preference and alias back`() {
        var preference = false
        var alias = false
        var rejectEnable = true

        val result = BrowserModeUtils.executeBrowserAliasTransaction(
            desiredEnabled = true,
            readPreference = { preference },
            writePreference = {
                preference = it
                true
            },
            readAlias = { alias },
            writeAlias = {
                if (it && rejectEnable) {
                    rejectEnable = false
                    false
                } else {
                    alias = it
                    true
                }
            },
        )

        assertFalse(result.success)
        assertTrue(result.rollbackSucceeded)
        assertFalse(result.needsAttention)
        assertFalse(preference)
        assertFalse(alias)
    }

    @Test
    fun `preference failure does not leave alias enabled`() {
        var preference = false
        var alias = false

        val result = BrowserModeUtils.executeBrowserAliasTransaction(
            desiredEnabled = true,
            readPreference = { preference },
            writePreference = { requested ->
                if (!requested) {
                    preference = false
                    true
                } else {
                    false
                }
            },
            readAlias = { alias },
            writeAlias = {
                alias = it
                true
            },
        )

        assertFalse(result.success)
        assertTrue(result.rollbackSucceeded)
        assertFalse(preference)
        assertFalse(alias)
    }

    @Test
    fun `failed rollback remains an attention state`() {
        var preference = false
        var alias = false

        val result = BrowserModeUtils.executeBrowserAliasTransaction(
            desiredEnabled = true,
            readPreference = { preference },
            writePreference = {
                preference = it
                it
            },
            readAlias = { alias },
            writeAlias = {
                if (it) {
                    false
                } else {
                    alias = true
                    false
                }
            },
        )

        assertFalse(result.success)
        assertFalse(result.rollbackSucceeded)
        assertTrue(result.needsAttention)
    }
}
