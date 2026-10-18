package com.whereami.locus.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DayTime {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun startOfDayMillis(epochMillis: Long = System.currentTimeMillis()): Long {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    fun endOfDayMillis(dayStartMillis: Long): Long {
        return Instant.ofEpochMilli(dayStartMillis)
            .atZone(zone)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    fun localDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun formatDayTitle(dayStartMillis: Long): String {
        val date = localDate(dayStartMillis)
        val today = LocalDate.now(zone)
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
        }
    }

    fun formatClock(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(zone)
            .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

    fun formatDuration(millis: Long): String {
        val totalMinutes = (millis / 60_000L).coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    fun formatDistance(meters: Double): String {
        return if (meters >= 1000.0) {
            String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
        } else {
            String.format(Locale.getDefault(), "%.0f m", meters)
        }
    }
}
