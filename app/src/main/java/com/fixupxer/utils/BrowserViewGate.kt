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

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

data class BrowserViewGateSnapshot internal constructor(
    internal val revision: Long,
)

/**
 * Invalidates in-flight Browser VIEW work whenever browser routing state changes.
 * Callers still pass freshly-read preference and alias values on every check.
 */
object BrowserViewGate {
    private val revision = AtomicLong(0L)
    private val pauseDepth = AtomicInteger(0)

    fun invalidate() {
        revision.incrementAndGet()
    }

    fun pause() {
        pauseDepth.incrementAndGet()
        invalidate()
    }

    fun resume() {
        while (true) {
            val current = pauseDepth.get()
            check(current > 0) { "Browser VIEW gate resumed without a matching pause" }
            if (pauseDepth.compareAndSet(current, current - 1)) break
        }
        invalidate()
    }

    fun begin(preferenceEnabled: Boolean, aliasEnabled: Boolean): BrowserViewGateSnapshot? {
        if (pauseDepth.get() != 0 ||
            !BrowserSettingsStateResolver.canProcessViewIntent(preferenceEnabled, aliasEnabled)
        ) {
            return null
        }
        val snapshot = BrowserViewGateSnapshot(revision.get())
        return snapshot.takeIf { pauseDepth.get() == 0 }
    }

    fun isValid(
        snapshot: BrowserViewGateSnapshot,
        preferenceEnabled: Boolean,
        aliasEnabled: Boolean,
    ): Boolean =
        pauseDepth.get() == 0 &&
            snapshot.revision == revision.get() &&
            BrowserSettingsStateResolver.canProcessViewIntent(preferenceEnabled, aliasEnabled)
}

object BrowserViewHandoffPolicy {
    fun shouldFinish(externalBrowserOpened: Boolean): Boolean = externalBrowserOpened
}
