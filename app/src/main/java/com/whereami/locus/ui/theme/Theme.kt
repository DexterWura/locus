package com.whereami.locus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LocatorBlue = Color(0xFF1A73E8)
val LocatorBlueDark = Color(0xFF8AB4F8)
val Paper = Color(0xFFF8FAFC)
val Ink = Color(0xFF0F172A)
val InkMuted = Color(0xFF64748B)
val SheetDark = Color(0xFF111827)

private val LightColors = lightColorScheme(
    primary = LocatorBlue,
    onPrimary = Color.White,
    secondary = LocatorBlue,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8EEF7),
    onSurfaceVariant = InkMuted,
    outline = Color(0xFFCBD5E1)
)

private val DarkColors = darkColorScheme(
    primary = LocatorBlueDark,
    onPrimary = Ink,
    secondary = LocatorBlueDark,
    onSecondary = Ink,
    background = Ink,
    onBackground = Paper,
    surface = SheetDark,
    onSurface = Paper,
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

@Composable
fun LocusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
