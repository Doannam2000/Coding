package com.example.clipystudio.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioDarkScheme = darkColorScheme(
  primary = StudioPrimary,
  secondary = StudioSecondary,
  tertiary = StudioAccent,
  background = StudioBackground,
  surface = StudioSurface,
  surfaceVariant = StudioSurfaceHigh,
  onPrimary = Color.White,
  onSecondary = Color.Black,
  onBackground = StudioText,
  onSurface = StudioText,
  onSurfaceVariant = StudioTextMuted,
  error = StudioDanger,
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = StudioDarkScheme, typography = Typography, content = content)
}
