package com.nantcompany.clipy.tools.filters

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.edit.tools.filters.FiltersRequest
import com.nantcompany.clipy.edit.tools.filters.FiltersValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FiltersUiState(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 1f,
    val validationError: String? = null
)

class FiltersViewModel(
    private val validator: FiltersValidator = FiltersValidator()
) : ViewModel() {
    private val _uiState = MutableStateFlow(FiltersUiState())
    val uiState: StateFlow<FiltersUiState> = _uiState.asStateFlow()

    fun updateBrightness(value: Float) {
        _uiState.value = _uiState.value.copy(brightness = value)
    }

    fun updateContrast(value: Float) {
        _uiState.value = _uiState.value.copy(contrast = value)
    }

    fun updateSaturation(value: Float) {
        _uiState.value = _uiState.value.copy(saturation = value)
    }

    fun reset() {
        _uiState.value = FiltersUiState()
    }

    fun validate(request: FiltersRequest): Boolean {
        val result = validator.validate(request)
        _uiState.value = _uiState.value.copy(validationError = result.errorMessage)
        return result.isValid
    }
}