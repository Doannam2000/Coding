package com.example.clipystudio.gpuimagecore

data class GpuImageAdjustments(
    val filterId: String? = null,
    val gpuImageFilterClass: String? = null,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val exposure: Float = 0f,
    val temperature: Float = 0f,
    val sharpness: Float = 0f,
    val vignette: Float = 0f
)
