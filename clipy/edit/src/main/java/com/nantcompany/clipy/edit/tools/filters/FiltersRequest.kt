package com.nantcompany.clipy.edit.tools.filters

data class FiltersRequest(
    val inputPath: String,
    val outputPath: String,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 1f,
    val filterIntensity: Float = 1f,
    val filterName: String = "Normal"
)
