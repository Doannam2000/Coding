package com.nantcompany.clipy.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ClipyDesignTokens {
    val screenPadding = 16.dp
    val sectionSpacing = 14.dp
    val cardCorner = 16.dp
    val toolCardCorner = 20.dp
    val heroCorner = 22.dp

    val primaryAccent = Color(0xFF87B5FF)
    val secondaryText = Color(0xFFBAC6D7)
    val success = Color(0xFF5ED6A8)
    val error = Color(0xFFFF7A8A)

    val heroTitle = Color.White
    val heroSubtitle = Color(0xFFD8E3F2)
    val subtleText = Color(0xFFADB8C7)
    val linkText = Color(0xFF8DB7FF)
    val cardSurface = Color(0xFF171E27)

    val heroBrush = Brush.linearGradient(listOf(Color(0xFF2B3E57), Color(0xFF4A3D73)))
}
