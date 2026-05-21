package com.nantcompany.clipy.tools.rotate

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RotateVideoUiState(
    val inputPath: String? = null,
    val rotation: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
)

class RotateVideoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RotateVideoUiState())
    val uiState: StateFlow<RotateVideoUiState> = _uiState.asStateFlow()

    fun setInputPath(path: String?) {
        _uiState.update { it.copy(inputPath = path) }
    }

    fun rotateClockwise() {
        _uiState.update { it.copy(rotation = (it.rotation + 90) % 360) }
    }

    fun rotateCounterClockwise() {
        _uiState.update { 
            var newRotation = it.rotation - 90
            if (newRotation < 0) newRotation += 360
            it.copy(rotation = newRotation)
        }
    }

    fun toggleFlipHorizontal() {
        _uiState.update { it.copy(flipHorizontal = !it.flipHorizontal) }
    }

    fun toggleFlipVertical() {
        _uiState.update { it.copy(flipVertical = !it.flipVertical) }
    }

    fun reset() {
        _uiState.update { it.copy(rotation = 0, flipHorizontal = false, flipVertical = false) }
    }
}
