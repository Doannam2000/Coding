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
    FilterPreset("warm", "Warm", "GPUImageWhiteBalanceFilter", FilterAdjustmentSet(filterId = "warm", temperature = 0.28f, saturation = 1.08f)),
    FilterPreset("cool", "Cool", "GPUImageWhiteBalanceFilter", FilterAdjustmentSet(filterId = "cool", temperature = -0.22f, saturation = 1.04f)),
    FilterPreset("vintage", "Vintage", "GPUImageSepiaToneFilter", FilterAdjustmentSet(filterId = "vintage", contrast = 1.06f, saturation = 0.86f, vignette = 0.18f)),
    FilterPreset("cinematic", "Cinematic", "GPUImageContrastFilter", FilterAdjustmentSet(filterId = "cinematic", contrast = 1.12f, saturation = 0.92f, exposure = -0.04f)),
    FilterPreset("bw", "B&W", "GPUImageGrayscaleFilter", FilterAdjustmentSet(filterId = "bw", contrast = 1.08f, saturation = 0f)),
    FilterPreset("invert", "Invert", "GPUImageColorInvertFilter", FilterAdjustmentSet(filterId = "invert")),
    FilterPreset("sketch", "Sketch", "GPUImageSketchFilter", FilterAdjustmentSet(filterId = "sketch", contrast = 1.15f)),
    FilterPreset("toon", "Toon", "GPUImageToonFilter", FilterAdjustmentSet(filterId = "toon", saturation = 1.18f)),
  )

  fun byId(id: String?): FilterPreset = presets.firstOrNull { it.id == id } ?: presets.first()
}
