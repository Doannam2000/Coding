package com.nantcompany.clipy.tools.crop

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.edit.tools.crop.CropRequest
import com.nantcompany.clipy.edit.tools.crop.CropValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CropUiState(
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val validationError: String? = null
)

class CropViewModel(
    private val validator: CropValidator = CropValidator()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CropUiState())
    val uiState: StateFlow<CropUiState> = _uiState.asStateFlow()

    fun setVideoDimensions(w: Int, h: Int) {
        val current = _uiState.value
        _uiState.value = current.copy(
            videoWidth = w,
            videoHeight = h,
            width = if (current.width == 0) w else current.width,
            height = if (current.height == 0) h else current.height
        )
    }

    fun updateX(value: Int) {
        _uiState.value = _uiState.value.copy(x = value.coerceAtLeast(0))
    }

    fun updateY(value: Int) {
        _uiState.value = _uiState.value.copy(y = value.coerceAtLeast(0))
    }

    fun updateWidth(value: Int) {
        _uiState.value = _uiState.value.copy(width = value.coerceAtLeast(0))
    }

    fun updateHeight(value: Int) {
        _uiState.value = _uiState.value.copy(height = value.coerceAtLeast(0))
    }

    fun autoCrop() {
        val state = _uiState.value
        _uiState.value = state.copy(x = 0, y = 0, width = state.videoWidth, height = state.videoHeight)
    }

    fun validate(request: CropRequest): Boolean {
        val result = validator.validate(request)
        _uiState.value = _uiState.value.copy(validationError = result.errorMessage)
        return result.isValid
    }
}