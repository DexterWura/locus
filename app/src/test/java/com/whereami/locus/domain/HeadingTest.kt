package com.whereami.locus.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HeadingTest {
    @Test
    fun cardinal_mapsCardinalPoints() {
        assertEquals("N", Heading.cardinal(0f))
        assertEquals("NE", Heading.cardinal(45f))
        assertEquals("E", Heading.cardinal(90f))
        assertEquals("S", Heading.cardinal(180f))
        assertEquals("W", Heading.cardinal(270f))
        assertEquals("N", Heading.cardinal(359f))
    }

    @Test
    fun format_includesDegrees() {
        assertEquals("NE · 45°", Heading.format(45f))
    }
}
