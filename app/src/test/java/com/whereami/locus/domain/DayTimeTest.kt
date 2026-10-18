package com.whereami.locus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DayTimeTest {

    @Test
    fun startOfDay_isMidnightLocal() {
        val zone = ZoneId.systemDefault()
        val noon = LocalDate.now(zone)
            .atTime(12, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val start = DayTime.startOfDayMillis(noon)
        val expected = LocalDate.now(zone)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, start)
    }

    @Test
    fun crossingMidnight_splitsIntoDifferentDays() {
        val zone = ZoneId.systemDefault()
        val beforeMidnight = LocalDate.now(zone)
            .atTime(23, 59)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val afterMidnight = LocalDate.now(zone)
            .plusDays(1)
            .atTime(0, 1)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        val dayA = DayTime.startOfDayMillis(beforeMidnight)
        val dayB = DayTime.startOfDayMillis(afterMidnight)
        assertNotEquals(dayA, dayB)
        assertEquals(DayTime.endOfDayMillis(dayA), dayB)
    }

    @Test
    fun endOfDay_isExclusiveNextMidnight() {
        val start = DayTime.startOfDayMillis()
        val end = DayTime.endOfDayMillis(start)
        assertTrue(end > start)
        assertEquals(24 * 60 * 60 * 1000L, end - start)
    }
}
