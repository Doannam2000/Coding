package com.nantcompany.clipy.app

import com.nantcompany.clipy.filters.gpu.ClipyFilterType
import com.nantcompany.clipy.export.model.TextLayer
import com.nantcompany.clipy.export.model.AudioTrack

data class ProEditorState(
    val videoPath: String? = null,
    val durationMs: Long = 0,
    val startMs: Long = 0,
    val endMs: Long = 0,
    
    // Transforms
    val rotation: Int = 0,
    val flipHorizontal: Boolean = false,
    
    // Looks
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 1f,
    val selectedFilter: ClipyFilterType = ClipyFilterType.NONE,
    
    // Control
    val speedFactor: Float = 1.0f,
    val volume: Float = 1.0f,
    val overlayText: String = "",
    val textX: Float = 15f,
    val textY: Float = 15f
)
