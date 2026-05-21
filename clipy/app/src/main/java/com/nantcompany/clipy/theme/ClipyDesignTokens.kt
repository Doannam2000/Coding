package com.nantcompany.clipy.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ClipyDesignTokens {
    // Spacing
    val screenPadding = 20.dp
    val sectionSpacing = 24.dp
    
    // Corners
    val cardCorner = 24.dp
    val toolCardCorner = 28.dp
    val heroCorner = 32.dp
    val buttonCorner = 18.dp

    // Brand Colors - High End Cinematic
    val primaryAccent = Color(0xFFC084FC) // Vibrant Soft Purple
    val secondaryAccent = Color(0xFF38BDF8) // Sky Blue
    val tertiaryAccent = Color(0xFFFB7185) // Rose Pink
    val success = Color(0xFF4ADE80)
    val error = Color(0xFFF87171)
    
    // Background Hierarchy
    val bgMain = Color(0xFF030712) // Near Black
    val bgCard = Color(0x14FFFFFF) // Ultra-thin glass
    val bgNav = Color(0xF20B0F1A) // Solid navy-black for nav

    // UI Elements (Main Names)
    val cardSurface = Color(0x0DFFFFFF)
    val cardBorder = Color(0x12FFFFFF)
    val secondaryText = Color(0xFF94A3B8)
    val textSecondary = Color(0xFF94A3B8)
    val textMuted = Color(0xFF64748B)
    val textPrimary = Color(0xFFF8FAFC)
    
    // Compatibility Legacy
    val LegacyTextPrimary = Color(0xFFFFFFFF)
    val LegacyTextMuted = Color(0xFF64748B)
    val ErrorRed = Color(0xFFF87171)
    val NeonPurple = Color(0xFFC084FC)
    val NeonCyan = Color(0xFF38BDF8)
    
    // Borders
    val borderLight = Color(0x1AFFFFFF)
    val borderAccent = Color(0x33B76DFF)

    val premiumBrush = Brush.linearGradient(
        colors = listOf(primaryAccent, secondaryAccent)
    )

    val premiumGlowBrush = Brush.radialGradient(
        colors = listOf(primaryAccent.copy(alpha = 0.15f), Color.Transparent)
    )

    val heroBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF2E1065), Color(0xFF581C87), Color(0xFF701A75))
    )
    
    val glassBrush = Brush.verticalGradient(
        colors = listOf(Color(0x1FFFFFFF), Color(0x0AFFFFFF))
    )

    val surfaceGradient = Brush.verticalGradient(
        colors = listOf(Color(0x1AFFFFFF), Color(0x00FFFFFF))
    )
}
