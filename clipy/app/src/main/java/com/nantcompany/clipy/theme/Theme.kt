package com.nantcompany.clipy.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ClipyDarkScheme =
  darkColorScheme(
    primary = ClipyPrimary,
    onPrimary = ClipyOnDark,
    secondary = ClipySecondary,
    onSecondary = ClipyBackground,
    tertiary = ClipyAccent,
    background = ClipyBackground,
    onBackground = ClipyOnDark,
    surface = ClipySurface,
    onSurface = ClipyOnDark,
    surfaceVariant = ClipySurfaceVariant,
    onSurfaceVariant = ClipyMuted,
    error = ClipyError,
  )

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = ClipyDarkScheme, typography = Typography, content = content)
}
