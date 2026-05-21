package com.nantcompany.clipy.filters.gpu

import androidx.media3.common.util.UnstableApi

@UnstableApi
data class ProPreset(
    val id: String,
    val name: String,
    val filter: ClipyFilterType,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 1f,
    val description: String = ""
)

@UnstableApi
object ClipyPresets {
    val ALL = listOf(
        ProPreset("original", "Original", ClipyFilterType.NONE, description = "No effects applied"),
        ProPreset("magic", "✨ Magic", ClipyFilterType.NONE, brightness = 0.05f, contrast = 0.1f, saturation = 1.15f, description = "Auto-balanced levels"),
        ProPreset("cyber", "Cyberpunk", ClipyFilterType.CYBERPUNK, saturation = 1.4f, contrast = 0.15f, description = "Neon nights aesthetic"),
        ProPreset("noir", "Film Noir", ClipyFilterType.GRAYSCALE, contrast = 0.3f, brightness = -0.05f, description = "High contrast B&W"),
        ProPreset("sunny", "Warm Day", ClipyFilterType.WARM, brightness = 0.08f, saturation = 1.1f, description = "Golden hour glow"),
        ProPreset("cold", "Ice Cave", ClipyFilterType.COOL, brightness = -0.05f, saturation = 1.2f, description = "Deep blue tones"),
        ProPreset("retro", "Vintage", ClipyFilterType.VINTAGE, contrast = -0.1f, saturation = 0.8f, description = "Classic film look"),
        ProPreset("dramatic", "Cinema", ClipyFilterType.DRAMATIC, contrast = 0.25f, saturation = 0.9f, description = "Moody cinematic feel")
    )
}
