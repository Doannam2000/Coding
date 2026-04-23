package com.nantcompany.clipy.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Offline-safe local fallback keeps the intended modern sans tone without runtime font downloads.
private val ClipyFontFamily = FontFamily.SansSerif

val Typography =
  Typography(
    headlineLarge =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp),
    headlineMedium =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    titleLarge =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
    titleMedium =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = (-0.1).sp),
    titleSmall =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.sp),
    bodySmall =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp),
    labelMedium =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp),
    labelSmall =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 0.45.sp),
  )
