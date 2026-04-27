package com.natncompany.clipy.filter

import com.natncompany.clipy.editor.ClipAdjustments
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorInvertFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageEmbossFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImagePixelationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSketchFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageToonFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageVignetteFilter

data class GpuFilterPreset(
    val id: String,
    val label: String,
    val buildFilter: () -> GPUImageFilter
)

object GpuImageFilterLibrary {
    private val original = GpuFilterPreset(
        id = "original",
        label = "Original",
        buildFilter = { GPUImageFilter() }
    )

    val presets = listOf(
        original,
        GpuFilterPreset("sepia", "Sepia") { GPUImageSepiaToneFilter() },
        GpuFilterPreset("mono", "Mono") { GPUImageGrayscaleFilter() },
        GpuFilterPreset("invert", "Invert") { GPUImageColorInvertFilter() },
        GpuFilterPreset("sketch", "Sketch") { GPUImageSketchFilter() },
        GpuFilterPreset("toon", "Toon") { GPUImageToonFilter() },
        GpuFilterPreset("pixel", "Pixel") { GPUImagePixelationFilter() },
        GpuFilterPreset("emboss", "Emboss") { GPUImageEmbossFilter() },
        GpuFilterPreset("vignette", "Vignette") { GPUImageVignetteFilter() }
    )

    fun presetForLabel(label: String?): GpuFilterPreset {
        return presets.firstOrNull { it.label == label } ?: original
    }

    fun hasPreviewAdjustments(adjustments: ClipAdjustments): Boolean {
        return adjustments.filterName != original.label ||
            adjustments.brightness != 0f ||
            adjustments.contrast != 0f ||
            adjustments.saturation != 0f
    }

    fun buildFilter(adjustments: ClipAdjustments): GPUImageFilter {
        val preset = presetForLabel(adjustments.filterName)
        val group = GPUImageFilterGroup()
        var hasCustomStep = false

        if (preset.id != original.id) {
            group.addFilter(preset.buildFilter())
            hasCustomStep = true
        }

        if (adjustments.brightness != 0f) {
            group.addFilter(GPUImageBrightnessFilter(adjustments.brightness.coerceIn(-1f, 1f)))
            hasCustomStep = true
        }

        if (adjustments.contrast != 0f) {
            group.addFilter(
                GPUImageContrastFilter(
                    (1f + adjustments.contrast).coerceIn(0f, 2f)
                )
            )
            hasCustomStep = true
        }

        if (adjustments.saturation != 0f) {
            group.addFilter(
                GPUImageSaturationFilter(
                    (1f + adjustments.saturation).coerceIn(0f, 2f)
                )
            )
            hasCustomStep = true
        }

        return if (hasCustomStep) {
            group
        } else {
            original.buildFilter()
        }
    }
}
