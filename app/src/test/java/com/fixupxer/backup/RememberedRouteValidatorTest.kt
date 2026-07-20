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

package com.fixupxer.backup

import android.net.Uri
import com.fixupxer.utils.ProxyRoster
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RememberedRouteValidatorTest {

    @Before
    fun setup() = ProxyRoster.reset()

    @After
    fun tearDown() = ProxyRoster.reset()

    @Test
    fun `normalizes case and trailing dot`() {
        assertEquals("example.com", RememberedRouteValidator.normalizeHost("EXAMPLE.COM."))
        assertEquals("example.com", RememberedRouteValidator.normalizeHost("https://Example.COM/path"))
    }

    @Test
    fun `normalizes IDN host to punycode`() {
        assertEquals("xn--mnchen-3ya.de", RememberedRouteValidator.normalizeHost("münchen.de"))
        assertEquals(
            "xn--mnchen-3ya.example",
            RememberedRouteValidator.normalizeHost("https://MÜNCHEN.example./post"),
        )
    }

    @Test
    fun `blank host is rejected`() {
        assertNull(RememberedRouteValidator.normalizeHost("   "))
    }

    @Test
    fun `reader-only final uri skips native without delete`() {
        assertTrue(
            RememberedRouteValidator.shouldSkipNativeWithoutDelete(
                Uri.parse("https://xcancel.com/user/status/1"),
            ),
        )
    }

    @Test
    fun `farside nitter path counts as reader`() {
        assertTrue(
            RememberedRouteValidator.shouldSkipNativeWithoutDelete(
                Uri.parse("https://farside.link/nitter/user/status/1"),
            ),
        )
        assertFalse(
            RememberedRouteValidator.shouldSkipNativeWithoutDelete(
                Uri.parse("https://farside.link/redlib/r/privacy"),
            ),
        )
    }

    @Test
    fun `embed final uri is not reader-only`() {
        assertFalse(
            RememberedRouteValidator.shouldSkipNativeWithoutDelete(
                Uri.parse("https://fixupx.com/user/status/1"),
            ),
        )
    }

    @Test
    fun `valid snapshot routes pass strict validation`() {
        RememberedRouteValidator.requireValidSnapshotRoutes(
            mapOf(
                "instagram.com" to RememberedRoute(RememberedRouteKind.NATIVE, "com.instagram.android"),
                "example.com" to RememberedRoute(RememberedRouteKind.BROWSER, "org.mozilla.firefox"),
            ),
            ownPackageName = "com.fixupxer.debug",
        )
    }

    @Test
    fun `non-normalized route host is rejected`() {
        assertTrue(
            runCatching {
                RememberedRouteValidator.requireValidSnapshotRoutes(
                    mapOf("Example.COM" to RememberedRoute(RememberedRouteKind.BROWSER, "org.mozilla.firefox")),
                    ownPackageName = null,
                )
            }.isFailure,
        )
    }

    @Test
    fun `native route package is checked at runtime not during cross-device restore`() {
        RememberedRouteValidator.requireValidSnapshotRoutes(
            mapOf(
                "google.com" to RememberedRoute(
                    RememberedRouteKind.NATIVE,
                    "com.google.android.googlequicksearchbox",
                ),
            ),
            ownPackageName = null,
        )
    }

    @Test
    fun `own package route is rejected when own package known`() {
        assertTrue(
            runCatching {
                RememberedRouteValidator.requireValidSnapshotRoutes(
                    mapOf("example.com" to RememberedRoute(RememberedRouteKind.BROWSER, "com.fixupxer.debug")),
                    ownPackageName = "com.fixupxer.debug",
                )
            }.isFailure,
        )
    }

    @Test
    fun `invalid package characters are rejected`() {
        assertTrue(
            runCatching {
                RememberedRouteValidator.requireValidSnapshotRoutes(
                    mapOf("example.com" to RememberedRoute(RememberedRouteKind.BROWSER, "bad package!")),
                    ownPackageName = null,
                )
            }.isFailure,
        )
    }
}
