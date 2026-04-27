package com.example.clipystudio.filter

import com.example.clipystudio.data.FilterAdjustmentSet

data class FilterPreset(
  val id: String?,
  val label: String,
  val gpuImageFilterClass: String?,
  val defaultAdjustments: FilterAdjustmentSet = FilterAdjustmentSet(),
)

object FilterLibrary {
  val presets = listOf(
    FilterPreset(null, "Original", null),
    FilterPreset("sepia", "Sepia", "jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter"),
    FilterPreset("grayscale", "Grayscale", "jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter"),
    FilterPreset("invert", "Invert", "jp.co.cyberagent.android.gpuimage.filter.GPUImageColorInvertFilter"),
    FilterPreset("sketch", "Sketch", "jp.co.cyberagent.android.gpuimage.filter.GPUImageSketchFilter"),
    FilterPreset("toon", "Toon", "jp.co.cyberagent.android.gpuimage.filter.GPUImageToonFilter"),
    FilterPreset("posterize", "Posterize", "jp.co.cyberagent.android.gpuimage.filter.GPUImagePosterizeFilter"),
    FilterPreset("vignette", "Vignette", "jp.co.cyberagent.android.gpuimage.filter.GPUImageVignetteFilter"),
    FilterPreset("kuwahara", "Kuwahara", "jp.co.cyberagent.android.gpuimage.filter.GPUImageKuwaharaFilter"),
    FilterPreset("pixelation", "Pixel", "jp.co.cyberagent.android.gpuimage.filter.GPUImagePixelationFilter"),
    FilterPreset("bulge_distortion", "Bulge", "jp.co.cyberagent.android.gpuimage.filter.GPUImageBulgeDistortionFilter"),
    FilterPreset("glass_sphere", "Glass", "jp.co.cyberagent.android.gpuimage.filter.GPUImageGlassSphereFilter"),
    FilterPreset("haze", "Haze", "jp.co.cyberagent.android.gpuimage.filter.GPUImageHazeFilter"),
    FilterPreset("solarize", "Solarize", "jp.co.cyberagent.android.gpuimage.filter.GPUImageSolarizeFilter"),
    FilterPreset("emboss", "Emboss", "jp.co.cyberagent.android.gpuimage.filter.GPUImageEmbossFilter"),
    FilterPreset("false_color", "FalseColor", "jp.co.cyberagent.android.gpuimage.filter.GPUImageFalseColorFilter"),
    FilterPreset("halftone", "Halftone", "jp.co.cyberagent.android.gpuimage.filter.GPUImageHalftoneFilter"),
    FilterPreset("crosshatch", "Crosshatch", "jp.co.cyberagent.android.gpuimage.filter.GPUImageCrosshatchFilter"),
    FilterPreset("cga", "CGA", "jp.co.cyberagent.android.gpuimage.filter.GPUImageCGAColorspaceFilter"),
  )
}
