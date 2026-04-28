package com.natncompany.media.render

import com.natncompany.media.Timeline
import com.natncompany.media.TimelineClip
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup

class GpuImageRenderBridge {
    fun requiresGpu(timeline: Timeline): Boolean {
        return timeline.tracks
            .flatMap { it.clips }
            .any { clip ->
                clip.effect.intensity != 1f ||
                    clip.effect.parameters.isNotEmpty() ||
                    clip.transform.brightness != 0f ||
                    clip.transform.contrast != 1f ||
                    clip.transform.blur != 0f
            }
    }

    fun buildFilter(clip: TimelineClip): GPUImageFilter {
        val group = GPUImageFilterGroup()
        var hasFilter = false
        if (clip.transform.brightness != 0f) {
            group.addFilter(GPUImageBrightnessFilter(clip.transform.brightness.coerceIn(-1f, 1f)))
            hasFilter = true
        }
        if (clip.transform.contrast != 1f) {
            group.addFilter(GPUImageContrastFilter(clip.transform.contrast.coerceIn(0f, 2f)))
            hasFilter = true
        }
        return if (hasFilter) group else GPUImageFilter()
    }
}
