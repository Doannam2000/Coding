package com.natncompany.clipy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Mint500,
    onPrimary = Fog100,
    secondary = Sky500,
    tertiary = Sand500,
    background = Ink900,
    surface = Ink800,
    surfaceVariant = Ink700,
    surfaceContainer = Ink800,
    surfaceContainerLow = Ink800,
    surfaceContainerHighest = Ink700,
    onBackground = Fog100,
    onSurface = Fog100,
    onSurfaceVariant = Fog200,
    outline = Slate500,
    outlineVariant = Stone700,
    error = Rose400
)

private val LightColorScheme = lightColorScheme(
    primary = Mint500,
    onPrimary = Fog100,
    secondary = Sky500,
    tertiary = Sand500,
    background = Slate50,
    surface = Fog100,
    surfaceVariant = Slate100,
    surfaceContainer = Fog100,
    surfaceContainerLow = Slate50,
    surfaceContainerHighest = Slate100,
    onBackground = Ink900,
    onSurface = Ink900,
    onSurfaceVariant = Stone700,
    outline = Slate500,
    outlineVariant = Slate100,
    error = Rose400
)

@Composable
fun ClipyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
