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


package com.fixupxer.utils

import java.util.concurrent.TimeUnit

/**
 * Convert a timestamp to a relative time string (e.g., "2 minutes ago", "Yesterday")
 */
fun Long.timeAgo(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            if (hours == 1L) "1 hour ago" else "$hours hours ago"
        }
        diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            "$days days ago"
        }
        else -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val weeks = days / 7
            if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
        }
    }
} 