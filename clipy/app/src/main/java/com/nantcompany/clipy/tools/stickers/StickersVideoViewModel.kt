package com.nantcompany.clipy.tools.stickers

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class StickersVideoUiState(
    val inputPath: String? = null,
    val stickerPath: String? = null,
    val x: Float = 50f,
    val y: Float = 50f,
    val startTimeMs: Long = 0,
    val endTimeMs: Long = 0
)

class StickersVideoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StickersVideoUiState())
    val uiState: StateFlow<StickersVideoUiState> = _uiState.asStateFlow()

    fun setInputPath(path: String?) {
        _uiState.update { it.copy(inputPath = path) }
    }

    fun setStickerPath(path: String?) {
        _uiState.update { it.copy(stickerPath = path) }
    }

    fun setPosition(x: Float, y: Float) {
        _uiState.update { it.copy(x = x, y = y) }
    }

    fun setRange(start: Long, end: Long) {
        _uiState.update { it.copy(startTimeMs = start, endTimeMs = end) }
    }
}
