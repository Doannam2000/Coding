package com.example.clipystudio.gpuimagecore

data class GpuVideoFrameRequest(
    val mediaUri: String,
    val playheadMs: Long,
    val clipStartMs: Long,
    val clipDurationMs: Long,
    val sourceInMs: Long,
    val sourceDurationMs: Long?,
    val speed: Float,
    val adjustments: GpuImageAdjustments,
    val targetLongEdgePx: Int
)
