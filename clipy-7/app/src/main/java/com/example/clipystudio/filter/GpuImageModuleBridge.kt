package com.example.clipystudio.filter

import com.example.clipystudio.data.FilterAdjustmentSet
import com.example.clipystudio.data.TimelineClip
import com.example.clipystudio.gpuimagecore.GpuImageAdjustments
import com.example.clipystudio.gpuimagecore.GpuVideoFrameRequest

fun GpuImageAdjustments.toFilterAdjustmentSet(): FilterAdjustmentSet = FilterAdjustmentSet(
  filterId = filterId,
  gpuImageFilterClass = gpuImageFilterClass,
  brightness = brightness,
  contrast = contrast,
  saturation = saturation,
  exposure = exposure,
  temperature = temperature,
  sharpness = sharpness,
  vignette = vignette,
)

fun FilterAdjustmentSet.toGpuImageAdjustments(): GpuImageAdjustments = GpuImageAdjustments(
  filterId = filterId,
  gpuImageFilterClass = gpuImageFilterClass,
  brightness = brightness,
  contrast = contrast,
  saturation = saturation,
  exposure = exposure,
  temperature = temperature,
  sharpness = sharpness,
  vignette = vignette,
)

fun TimelineClip.toGpuVideoFrameRequest(playheadMs: Long, targetLongEdgePx: Int = 1_280): GpuVideoFrameRequest {
  return GpuVideoFrameRequest(
    mediaUri = mediaUri.orEmpty(),
    playheadMs = playheadMs,
    clipStartMs = startMs,
    clipDurationMs = durationMs,
    sourceInMs = sourceInMs,
    sourceDurationMs = sourceDurationMs,
    speed = videoProperties.speed,
    adjustments = filterAdjustments.toGpuImageAdjustments(),
    targetLongEdgePx = targetLongEdgePx,
  )
}
