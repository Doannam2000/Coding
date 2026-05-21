package com.nantcompany.clipy.tools.crop

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CropVideoUiState(
    val inputPath: String? = null,
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val aspectRatio: String = "original"
)

class CropVideoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CropVideoUiState())
    val uiState: StateFlow<CropVideoUiState> = _uiState.asStateFlow()

    fun setInputPath(path: String?) {
        _uiState.update { it.copy(inputPath = path) }
    }

    fun setCropArea(x: Int, y: Int, width: Int, height: Int) {
        _uiState.update { it.copy(x = x, y = y, width = width, height = height) }
    }

    fun setAspectRatio(ratio: String) {
        _uiState.update { it.copy(aspectRatio = ratio) }
    }
}
