package com.example.clipystudio.filter

import com.example.clipystudio.data.FilterAdjustmentSet
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorInvertFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSketchFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageToonFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageWhiteBalanceFilter

object GpuImageFilterFactory {
  fun create(adjustments: FilterAdjustmentSet): GPUImageFilter = when (adjustments.filterId) {
    "warm" -> GPUImageWhiteBalanceFilter(5000f + adjustments.temperature * 2500f, 0f)
    "cool" -> GPUImageWhiteBalanceFilter(5000f + adjustments.temperature * 2500f, 0f)
    "vintage" -> GPUImageSepiaToneFilter()
    "cinematic" -> GPUImageContrastFilter(adjustments.contrast)
    "bw" -> GPUImageGrayscaleFilter()
    "invert" -> GPUImageColorInvertFilter()
    "sketch" -> GPUImageSketchFilter()
    "toon" -> GPUImageToonFilter()
    else -> if (adjustments.sharpness > 0f) GPUImageSharpenFilter(adjustments.sharpness) else GPUImageFilter()
  }
}
