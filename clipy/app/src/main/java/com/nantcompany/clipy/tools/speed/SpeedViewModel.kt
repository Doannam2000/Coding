package com.nantcompany.clipy.tools.speed

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.edit.tools.speed.SpeedRequest
import com.nantcompany.clipy.edit.tools.speed.SpeedValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SpeedUiState(
    val speedFactor: Float = 1f,
    val validationError: String? = null
)

class SpeedViewModel(
    private val validator: SpeedValidator = SpeedValidator()
) : ViewModel() {
    private val _uiState = MutableStateFlow(SpeedUiState())
    val uiState: StateFlow<SpeedUiState> = _uiState.asStateFlow()

    fun setSpeedFactor(factor: Float) {
        _uiState.value = _uiState.value.copy(speedFactor = factor.coerceIn(0.25f, 4.0f))
    }

    fun validate(request: SpeedRequest): Boolean {
        val result = validator.validate(request)
        _uiState.value = _uiState.value.copy(validationError = result.errorMessage)
        return result.isValid
    }
}