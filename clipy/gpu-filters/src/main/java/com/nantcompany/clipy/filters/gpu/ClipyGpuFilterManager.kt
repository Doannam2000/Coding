package com.nantcompany.clipy.filters.gpu

import android.graphics.ColorMatrix
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.RgbMatrix

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

        // Keep preview on stable Media3 effects. The GPUImage bridge is useful for
        // offline experiments, but it can render black frames on some devices.
        when (filterType) {
            ClipyFilterType.SEPIA -> {
                effects.add(StaticRgbMatrix(SEPIA_MATRIX))
                effects.add(Contrast(0.18f))
                effects.add(Brightness(0.04f))
            }
            ClipyFilterType.GRAYSCALE -> {
                effects.add(RgbFilter.createGrayscaleFilter())
                effects.add(Contrast(0.22f))
            }
            ClipyFilterType.INVERT -> {
                effects.add(RgbFilter.createInvertedFilter())
            }
            ClipyFilterType.TOON -> {
                effects.add(Contrast(0.95f))
                effects.add(HslAdjustment.Builder().adjustSaturation(80f).build())
            }
            ClipyFilterType.VIGNETTE -> {
                effects.add(Brightness(-0.28f))
                effects.add(Contrast(0.42f))
            }
            ClipyFilterType.SKETCH -> {
                effects.add(HslAdjustment.Builder().adjustSaturation(-100f).build())
                effects.add(Contrast(1.0f))
                effects.add(Brightness(0.08f))
            }
            ClipyFilterType.KUWAHARA -> {
                effects.add(HslAdjustment.Builder().adjustSaturation(45f).build())
                effects.add(Contrast(-0.45f))
                effects.add(Brightness(0.08f))
            }
            ClipyFilterType.PIXEL -> {
                effects.add(Contrast(0.7f))
                effects.add(HslAdjustment.Builder().adjustSaturation(55f).build())
            }
            ClipyFilterType.POSTER -> {
                effects.add(Contrast(0.92f))
                effects.add(HslAdjustment.Builder().adjustSaturation(75f).build())
            }
            ClipyFilterType.WARM -> {
                effects.add(RgbAdjustment.Builder().setRedScale(1.22f).setGreenScale(1.05f).setBlueScale(0.78f).build())
                effects.add(HslAdjustment.Builder().adjustHue(14f).adjustSaturation(24f).build())
                effects.add(Brightness(0.05f))
            }
            ClipyFilterType.COOL -> {
                effects.add(RgbAdjustment.Builder().setRedScale(0.78f).setGreenScale(1.02f).setBlueScale(1.25f).build())
                effects.add(HslAdjustment.Builder().adjustHue(-18f).adjustSaturation(22f).build())
                effects.add(Contrast(0.16f))
            }
            ClipyFilterType.VINTAGE -> {
                effects.add(RgbAdjustment.Builder().setRedScale(1.14f).setGreenScale(0.96f).setBlueScale(0.72f).build())
                effects.add(Contrast(-0.24f))
                effects.add(HslAdjustment.Builder().adjustSaturation(-45f).adjustHue(18f).build())
                effects.add(Brightness(0.05f))
            }
            ClipyFilterType.DRAMATIC -> {
                effects.add(Contrast(0.95f))
                effects.add(HslAdjustment.Builder().adjustSaturation(-42f).build())
                effects.add(Brightness(-0.12f))
            }
            ClipyFilterType.LOMO -> {
                effects.add(Contrast(0.75f))
                effects.add(HslAdjustment.Builder().adjustSaturation(58f).adjustHue(10f).build())
                effects.add(RgbAdjustment.Builder().setRedScale(1.12f).setGreenScale(0.95f).setBlueScale(1.08f).build())
            }
            ClipyFilterType.CYBERPUNK -> {
                effects.add(RgbAdjustment.Builder().setRedScale(1.22f).setGreenScale(0.68f).setBlueScale(1.35f).build())
                effects.add(HslAdjustment.Builder().adjustHue(-140f).adjustSaturation(95f).build())
                effects.add(Contrast(0.7f))
                effects.add(Brightness(0.04f))
            }
            else -> {}
        }

        // 2. Fine Adjustments (User Sliders) - Applied on top
        if (brightness != 0f) effects.add(Brightness(brightness))
        if (contrast != 0f) effects.add(Contrast(contrast))
        if (saturation != 1f) {
            effects.add(HslAdjustment.Builder().adjustSaturation((saturation - 1f) * 100f).build())
        }

        return effects
    }

    fun createPreviewColorMatrix(
        brightness: Float,
        contrast: Float,
        saturation: Float,
        filterType: ClipyFilterType = ClipyFilterType.NONE,
        filterIntensity: Float = 1f
    ): ColorMatrix? {
        val intensity = filterIntensity.coerceIn(0f, 1f)
        val hasStyle = filterType != ClipyFilterType.NONE && intensity > 0f
        val hasAdjustment = brightness != 0f || contrast != 0f || saturation != 1f || hasStyle
        if (!hasAdjustment) return null

        return ColorMatrix().apply {
            previewStyleMatrix(filterType)?.let { postConcat(blendWithIdentity(it, intensity)) }
            if (saturation != 1f) postConcat(ColorMatrix().apply { setSaturation(saturation.coerceIn(0f, 3f)) })
            if (contrast != 0f || brightness != 0f) {
                postConcat(contrastBrightnessMatrix(contrast.coerceIn(-1f, 1f), brightness.coerceIn(-1f, 1f)))
            }
        }
    }

    private fun previewStyleMatrix(filterType: ClipyFilterType): ColorMatrix? {
        return when (filterType) {
            ClipyFilterType.SEPIA -> ColorMatrix(SEPIA_PREVIEW_MATRIX)
            ClipyFilterType.GRAYSCALE -> ColorMatrix().apply { setSaturation(0f) }
            ClipyFilterType.INVERT -> ColorMatrix(INVERT_PREVIEW_MATRIX)
            ClipyFilterType.WARM -> channelScaleMatrix(red = 1.22f, green = 1.05f, blue = 0.78f)
            ClipyFilterType.COOL -> channelScaleMatrix(red = 0.78f, green = 1.02f, blue = 1.25f)
            ClipyFilterType.VINTAGE -> ColorMatrix().apply {
                postConcat(channelScaleMatrix(red = 1.14f, green = 0.96f, blue = 0.72f))
                postConcat(ColorMatrix().apply { setSaturation(0.55f) })
            }
            ClipyFilterType.DRAMATIC -> ColorMatrix().apply {
                postConcat(ColorMatrix().apply { setSaturation(0.58f) })
                postConcat(contrastBrightnessMatrix(0.95f, -0.12f))
            }
            ClipyFilterType.TOON -> contrastBrightnessMatrix(0.9f, 0.03f)
            ClipyFilterType.SKETCH -> ColorMatrix().apply {
                postConcat(ColorMatrix().apply { setSaturation(0f) })
                postConcat(contrastBrightnessMatrix(1.0f, 0.08f))
            }
            ClipyFilterType.VIGNETTE -> contrastBrightnessMatrix(0.42f, -0.28f)
            ClipyFilterType.KUWAHARA -> ColorMatrix().apply {
                postConcat(ColorMatrix().apply { setSaturation(1.45f) })
                postConcat(contrastBrightnessMatrix(-0.45f, 0.08f))
            }
            ClipyFilterType.PIXEL -> contrastBrightnessMatrix(0.7f, 0f)
            ClipyFilterType.POSTER -> contrastBrightnessMatrix(0.92f, 0.02f)
            ClipyFilterType.LOMO -> ColorMatrix().apply {
                postConcat(channelScaleMatrix(red = 1.12f, green = 0.95f, blue = 1.08f))
                postConcat(contrastBrightnessMatrix(0.75f, 0f))
            }
            ClipyFilterType.CYBERPUNK -> ColorMatrix().apply {
                postConcat(channelScaleMatrix(red = 1.22f, green = 0.68f, blue = 1.35f))
                postConcat(contrastBrightnessMatrix(0.7f, 0.04f))
            }
            ClipyFilterType.NONE -> null
        }
    }

    private fun blendWithIdentity(matrix: ColorMatrix, intensity: Float): ColorMatrix {
        val clamped = intensity.coerceIn(0f, 1f)
        if (clamped >= 0.999f) return matrix

        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        val source = matrix.array
        return ColorMatrix(FloatArray(20) { index ->
            identity[index] + (source[index] - identity[index]) * clamped
        })
    }

    private class StaticRgbMatrix(private val matrix: FloatArray) : RgbMatrix {
        override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = matrix
    }

    private fun channelScaleMatrix(red: Float, green: Float, blue: Float): ColorMatrix {
        return ColorMatrix(
            floatArrayOf(
                red, 0f, 0f, 0f, 0f,
                0f, green, 0f, 0f, 0f,
                0f, 0f, blue, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private fun contrastBrightnessMatrix(contrast: Float, brightness: Float): ColorMatrix {
        val scale = (contrast + 1f).coerceIn(0.01f, 3f)
        val offset = brightness.coerceIn(-1f, 1f) * 255f
        return ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, offset,
                0f, scale, 0f, 0f, offset,
                0f, 0f, scale, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private val SEPIA_MATRIX = floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f,
        0.349f, 0.686f, 0.168f, 0f,
        0.272f, 0.534f, 0.131f, 0f,
        0f, 0f, 0f, 1f
    )

    private val SEPIA_PREVIEW_MATRIX = floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    private val INVERT_PREVIEW_MATRIX = floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    )
}
