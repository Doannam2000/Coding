package com.nantcompany.clipy.tools.stickers

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.edit.tools.stickers.StickersRequest
import com.nantcompany.clipy.edit.tools.stickers.StickersValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StickersUiState(
    val stickerPath: String = "",
    val x: Int = 0,
    val y: Int = 0,
    val width: Int? = null,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val videoDurationMs: Long = 0L,
    val validationError: String? = null
)

class StickersViewModel(
    private val validator: StickersValidator = StickersValidator()
) : ViewModel() {
    private val _uiState = MutableStateFlow(StickersUiState())
    val uiState: StateFlow<StickersUiState> = _uiState.asStateFlow()

    fun setStickerPath(path: String) {
        _uiState.value = _uiState.value.copy(stickerPath = path)
    }

    fun setVideoDuration(ms: Long) {
        _uiState.value = _uiState.value.copy(
            videoDurationMs = ms,
            endTimeMs = if (_uiState.value.endTimeMs == 0L) ms else _uiState.value.endTimeMs
        )
    }

    fun updateX(value: Int) {
        _uiState.value = _uiState.value.copy(x = value.coerceAtLeast(0))
    }

    fun updateY(value: Int) {
        _uiState.value = _uiState.value.copy(y = value.coerceAtLeast(0))
    }

    fun updateWidth(value: Int?) {
        _uiState.value = _uiState.value.copy(width = value?.coerceAtLeast(0))
    }

    fun updateStartTime(ms: Long) {
        _uiState.value = _uiState.value.copy(startTimeMs = ms.coerceAtLeast(0L))
    }

    fun updateEndTime(ms: Long) {
        _uiState.value = _uiState.value.copy(endTimeMs = ms.coerceAtLeast(0L))
    }

    fun validate(request: StickersRequest): Boolean {
        val result = validator.validate(request)
        _uiState.value = _uiState.value.copy(validationError = result.errorMessage)
        return result.isValid
    }
}