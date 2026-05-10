package com.nantcompany.clipy.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ClipyDarkPrimary,
    secondary = ClipyDarkSecondary,
    background = ClipyDarkBackground,
    surface = ClipyDarkSurface,
    onSurface = ClipyDarkOnSurface,
    error = ClipyDesignTokens.error
)

private val LightColorScheme = lightColorScheme(
    primary = ClipyLightPrimary,
    secondary = ClipyLightSecondary,
    background = ClipyLightBackground,
    surface = ClipyLightSurface,
    onSurface = ClipyLightOnSurface
)

@Composable
fun ClipyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = ClipyTypography,
        content = content
    )
}
