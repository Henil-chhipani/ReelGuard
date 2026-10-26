package com.reelguard.app

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object StreakFormatter {

    fun duration(elapsedMs: Long): String {
        val minutes = elapsedMs.coerceAtLeast(0L) / 60_000L
        if (minutes < 60L) return "<1h"

        val hours = minutes / 60L
        if (hours < 24L) return "${hours}h"

        val days = hours / 24L
        if (days < 7L) return "${days}d"

        if (days < 30L) {
            val weeks = days / 7L
            val remainder = days % 7L
            return if (remainder == 0L) "${weeks}w" else "${weeks}w ${remainder}d"
        }

        if (days < 365L) {
            val months = days / 30L
            val afterMonths = days % 30L
            val weeks = afterMonths / 7L
            val remainder = afterMonths % 7L
            return buildString {
                append("${months}M")
                if (weeks > 0L) append(" ${weeks}W")
                if (remainder > 0L) append(" ${remainder}d")
            }
        }

        val years = days / 365L
        val afterYears = days % 365L
        val months = afterYears / 30L
        val remainder = afterYears % 30L
        return buildString {
            append("${years}Y")
            if (months > 0L) append(" ${months}M")
            if (remainder > 0L) append(" ${remainder}d")
        }
    }

    fun since(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val current = Instant.ofEpochMilli(now).atZone(zone)
        val pattern = if (start.year == current.year) "d MMM" else "d MMM yyyy"
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(start)
    }
}
