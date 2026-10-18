package com.whereami.locus.domain

object Heading {
    /**
     * @param degrees compass bearing 0–360 where 0 is North, clockwise
     */
    fun cardinal(degrees: Float): String {
        val normalized = ((degrees % 360f) + 360f) % 360f
        val index = ((normalized + 22.5f) / 45f).toInt() % 8
        return CARDINALS[index]
    }

    fun format(degrees: Float): String {
        val normalized = ((degrees % 360f) + 360f) % 360f
        return "${cardinal(normalized)} · ${normalized.toInt()}°"
    }

    private val CARDINALS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
}
