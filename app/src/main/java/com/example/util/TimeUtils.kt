package com.example.util

import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {
    fun formatMs(ms: Long): String {
        if (ms <= 0L) return "0:00"
        val totalSecs = ms / 1000
        val minutes = totalSecs / 60
        val seconds = totalSecs % 60
        val hours = minutes / 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes % 60, seconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
