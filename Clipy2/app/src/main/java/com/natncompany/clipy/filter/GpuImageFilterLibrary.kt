package com.natncompany.clipy.filter

import com.natncompany.clipy.editor.ClipAdjustments
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBilateralBlurFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBoxBlurFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBulgeDistortionFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageCGAColorspaceFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorInvertFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageCrosshatchFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageDirectionalSobelEdgeDetectionFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageEmbossFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageExposureFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFalseColorFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGammaFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGaussianBlurFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGlassSphereFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHalftoneFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHazeFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHighlightShadowFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageKuwaharaFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageLaplacianFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageLuminanceFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageLuminanceThresholdFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageMonochromeFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImagePixelationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImagePosterizeFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageRGBFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSketchFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSmoothToonFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSobelEdgeDetectionFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSobelThresholdFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSolarizeFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSphereRefractionFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSwirlFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageThresholdEdgeDetectionFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageToonFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageVignetteFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageWhiteBalanceFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageZoomBlurFilter

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
        GpuFilterPreset("monochrome", "Monochrome") { GPUImageMonochromeFilter() },
        GpuFilterPreset("luminance", "Luminance") { GPUImageLuminanceFilter() },
        GpuFilterPreset("invert", "Invert") { GPUImageColorInvertFilter() },
        GpuFilterPreset("solarize", "Solarize") { GPUImageSolarizeFilter() },
        GpuFilterPreset("posterize", "Posterize") { GPUImagePosterizeFilter(8) },
        GpuFilterPreset("cga", "CGA") { GPUImageCGAColorspaceFilter() },
        GpuFilterPreset("false_color", "False Color") { GPUImageFalseColorFilter() },
        GpuFilterPreset("rgb_warm", "RGB Warm") { GPUImageRGBFilter(1.08f, 1.0f, 0.92f) },
        GpuFilterPreset("rgb_cool", "RGB Cool") { GPUImageRGBFilter(0.9f, 1.0f, 1.12f) },
        GpuFilterPreset("hue_shift", "Hue Shift") { GPUImageHueFilter(90f) },
        GpuFilterPreset("gamma", "Gamma") { GPUImageGammaFilter(1.35f) },
        GpuFilterPreset("exposure", "Exposure") { GPUImageExposureFilter(0.45f) },
        GpuFilterPreset("white_balance", "White Balance") { GPUImageWhiteBalanceFilter() },
        GpuFilterPreset("highlight_shadow", "Highlight Shadow") { GPUImageHighlightShadowFilter() },
        GpuFilterPreset("sketch", "Sketch") { GPUImageSketchFilter() },
        GpuFilterPreset("toon", "Toon") { GPUImageToonFilter() },
        GpuFilterPreset("smooth_toon", "Smooth Toon") { GPUImageSmoothToonFilter() },
        GpuFilterPreset("sobel", "Sobel Edge") { GPUImageSobelEdgeDetectionFilter() },
        GpuFilterPreset("sobel_threshold", "Sobel Threshold") { GPUImageSobelThresholdFilter() },
        GpuFilterPreset("threshold_edge", "Threshold Edge") { GPUImageThresholdEdgeDetectionFilter() },
        GpuFilterPreset("directional_sobel", "Directional Edge") { GPUImageDirectionalSobelEdgeDetectionFilter() },
        GpuFilterPreset("laplacian", "Laplacian") { GPUImageLaplacianFilter() },
        GpuFilterPreset("luma_threshold", "Luma Threshold") { GPUImageLuminanceThresholdFilter() },
        GpuFilterPreset("crosshatch", "Crosshatch") { GPUImageCrosshatchFilter() },
        GpuFilterPreset("halftone", "Halftone") { GPUImageHalftoneFilter() },
        GpuFilterPreset("pixel", "Pixel") { GPUImagePixelationFilter() },
        GpuFilterPreset("emboss", "Emboss") { GPUImageEmbossFilter() },
        GpuFilterPreset("sharpen", "Sharpen") { GPUImageSharpenFilter(1.0f) },
        GpuFilterPreset("gaussian_blur", "Gaussian Blur") { GPUImageGaussianBlurFilter(0.8f) },
        GpuFilterPreset("box_blur", "Box Blur") { GPUImageBoxBlurFilter() },
        GpuFilterPreset("bilateral_blur", "Bilateral Blur") { GPUImageBilateralBlurFilter() },
        GpuFilterPreset("vignette", "Vignette") { GPUImageVignetteFilter() },
        GpuFilterPreset("haze", "Haze") { GPUImageHazeFilter() },
        GpuFilterPreset("kuwahara", "Kuwahara") { GPUImageKuwaharaFilter() },
        GpuFilterPreset("swirl", "Swirl") { GPUImageSwirlFilter() },
        GpuFilterPreset("bulge", "Bulge") { GPUImageBulgeDistortionFilter() },
        GpuFilterPreset("glass_sphere", "Glass Sphere") { GPUImageGlassSphereFilter() },
        GpuFilterPreset("sphere", "Sphere") { GPUImageSphereRefractionFilter() },
        GpuFilterPreset("zoom_blur", "Zoom Blur") { GPUImageZoomBlurFilter() }
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
