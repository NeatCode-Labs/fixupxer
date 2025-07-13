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