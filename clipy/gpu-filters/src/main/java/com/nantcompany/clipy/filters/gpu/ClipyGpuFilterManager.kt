package com.nantcompany.clipy.filters.gpu

import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbFilter
import jp.co.cyberagent.android.gpuimage.filter.*

@UnstableApi
enum class ClipyFilterType(val displayName: String) {
    NONE("Normal"),
    SEPIA("Sepia"),
    GRAYSCALE("B&W"),
    INVERT("Invert"),
    WARM("Warm"),
    COOL("Cool"),
    VINTAGE("Vintage"),
    DRAMATIC("Dramatic"),
    TOON("Toon"),
    SKETCH("Sketch"),
    VIGNETTE("Vignette"),
    KUWAHARA("Artistic"),
    PIXEL("Pixel"),
    POSTER("Poster"),
    LOMO("Lomo"),
    CYBERPUNK("Cyber")
}

@UnstableApi
object ClipyGpuFilterManager {
    fun createEffects(
        brightness: Float,
        contrast: Float,
        saturation: Float,
        filterType: ClipyFilterType = ClipyFilterType.NONE
    ): List<Effect> {
        val effects = mutableListOf<Effect>()

        // 1. Style Filters (Using full GPUImage library sources)
        when (filterType) {
            ClipyFilterType.SEPIA -> {
                effects.add(GPUImageEffect(GPUImageSepiaToneFilter()))
            }
            ClipyFilterType.GRAYSCALE -> {
                effects.add(GPUImageEffect(GPUImageGrayscaleFilter()))
            }
            ClipyFilterType.INVERT -> {
                effects.add(GPUImageEffect(GPUImageColorInvertFilter()))
            }
            ClipyFilterType.TOON -> {
                effects.add(GPUImageEffect(GPUImageToonFilter()))
            }
            ClipyFilterType.VIGNETTE -> {
                effects.add(GPUImageEffect(GPUImageVignetteFilter()))
            }
            ClipyFilterType.SKETCH -> {
                effects.add(GPUImageEffect(GPUImageSketchFilter()))
            }
            ClipyFilterType.KUWAHARA -> {
                effects.add(GPUImageEffect(GPUImageKuwaharaFilter()))
            }
            ClipyFilterType.PIXEL -> {
                effects.add(GPUImageEffect(GPUImagePixelationFilter()))
            }
            ClipyFilterType.POSTER -> {
                effects.add(GPUImageEffect(GPUImagePosterizeFilter()))
            }
            ClipyFilterType.WARM -> {
                effects.add(HslAdjustment.Builder().adjustHue(12f).adjustSaturation(0.05f).build())
            }
            ClipyFilterType.COOL -> {
                effects.add(HslAdjustment.Builder().adjustHue(-12f).adjustSaturation(0.05f).build())
            }
            ClipyFilterType.VINTAGE -> {
                effects.add(Contrast(-0.15f))
                effects.add(HslAdjustment.Builder().adjustSaturation(-0.4f).adjustHue(15f).build())
            }
            ClipyFilterType.DRAMATIC -> {
                effects.add(Contrast(0.35f))
                effects.add(HslAdjustment.Builder().adjustSaturation(-0.2f).build())
            }
            ClipyFilterType.LOMO -> {
                effects.add(Contrast(0.25f))
                effects.add(HslAdjustment.Builder().adjustSaturation(0.2f).build())
            }
            ClipyFilterType.CYBERPUNK -> {
                effects.add(HslAdjustment.Builder().adjustHue(-150f).adjustSaturation(0.4f).build())
                effects.add(Contrast(0.15f))
            }
            else -> {}
        }

        // 2. Fine Adjustments (User Sliders) - Applied on top
        if (brightness != 0f) effects.add(Brightness(brightness))
        if (contrast != 0f) effects.add(Contrast(contrast))
        if (saturation != 1f) effects.add(HslAdjustment.Builder().adjustSaturation(saturation - 1f).build())

        return effects
    }
}
