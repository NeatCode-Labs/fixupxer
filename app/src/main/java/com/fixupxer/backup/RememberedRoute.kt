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

enum class RememberedRouteKind(val wireName: String) {
    NATIVE("NATIVE"),
    BROWSER("BROWSER");

    companion object {
        fun fromWire(value: String): RememberedRouteKind =
            entries.first { it.wireName == value }
    }
}

data class RememberedRoute(
    val kind: RememberedRouteKind,
    val packageName: String,
)
