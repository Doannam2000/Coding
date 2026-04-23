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
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge =
      TextStyle(fontFamily = ClipyFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
  )
