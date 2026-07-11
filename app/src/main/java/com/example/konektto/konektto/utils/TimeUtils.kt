package com.example.konektto.konektto.utils

import java.util.concurrent.TimeUnit

/**
 * Formats a past timestamp (millis since epoch) as a short, human-friendly
 * relative string -- "Just now", "5m ago", "3h ago", "2d ago".
 */
object TimeUtils {

    /** "0:07", "1:23" -- for recording timers and voice note playback. */
    fun formatDuration(millis: Long): String {

        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return String.format("%d:%02d", minutes, seconds)

    }

    fun formatLastSeen(timestampMillis: Long): String {

        if (timestampMillis <= 0L) return "a while ago"

        val diffMillis = System.currentTimeMillis() - timestampMillis

        if (diffMillis < 0L) return "just now"

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
        val days = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> "a while ago"
        }

    }

}
