package com.whereami.locus.domain

enum class TrackingMode {
    /** Record only while the app is visible; no sticky notification. */
    Foreground,

    /** All-day background tracking; Android requires a quiet ongoing notice. */
    Background
}
