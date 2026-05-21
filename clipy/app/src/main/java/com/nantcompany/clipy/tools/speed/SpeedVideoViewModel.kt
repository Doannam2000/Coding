package com.nantcompany.clipy.tools.speed

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SpeedVideoUiState(
    val inputPath: String? = null,
    val speedFactor: Float = 1.0f
)

class SpeedVideoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SpeedVideoUiState())
    val uiState: StateFlow<SpeedVideoUiState> = _uiState.asStateFlow()

    fun setInputPath(path: String?) {
        _uiState.update { it.copy(inputPath = path) }
    }

    fun setSpeedFactor(factor: Float) {
        _uiState.update { it.copy(speedFactor = factor.coerceIn(0.25f, 4.0f)) }
    }

    fun reset() {
        _uiState.update { it.copy(speedFactor = 1.0f) }
    }
}
