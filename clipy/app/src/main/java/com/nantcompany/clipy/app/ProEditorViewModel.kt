package com.nantcompany.clipy.app

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.nantcompany.clipy.filters.gpu.ClipyFilterType
import com.nantcompany.clipy.filters.gpu.ProPreset

class ProEditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProEditorState())
    val state: StateFlow<ProEditorState> = _state.asStateFlow()

    fun setVideoPath(path: String?) {
        _state.update { it.copy(videoPath = path) }
    }

    fun setDuration(duration: Long) {
        _state.update { it.copy(durationMs = duration, endMs = duration) }
    }

    fun setTrim(start: Long, end: Long) {
        _state.update { it.copy(startMs = start, endMs = end) }
    }

    fun setTransform(rotation: Int, flip: Boolean) {
        _state.update { it.copy(rotation = rotation, flipHorizontal = flip) }
    }

    fun setAdjustments(b: Float, c: Float, s: Float) {
        _state.update { it.copy(brightness = b, contrast = c, saturation = s) }
    }

    fun setFilter(f: ClipyFilterType) {
        _state.update { it.copy(selectedFilter = f) }
    }

    fun setSpeed(s: Float) {
        _state.update { it.copy(speedFactor = s) }
    }

    fun setOverlayText(text: String) {
        _state.update { it.copy(overlayText = text) }
    }

    fun setTextPos(x: Float, y: Float) {
        _state.update { it.copy(textX = x, textY = y) }
    }
}
