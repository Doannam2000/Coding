package com.nantcompany.clipy.tools.rotate

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.edit.tools.rotate.RotateRequest
import com.nantcompany.clipy.edit.tools.rotate.RotateValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RotateUiState(
    val degrees: Int = 90,
    val validationError: String? = null
)

class RotateViewModel(
    private val validator: RotateValidator = RotateValidator()
) : ViewModel() {
    private val _uiState = MutableStateFlow(RotateUiState())
    val uiState: StateFlow<RotateUiState> = _uiState.asStateFlow()

    fun setDegrees(degrees: Int) {
        _uiState.value = _uiState.value.copy(degrees = degrees)
    }

    fun validate(request: RotateRequest): Boolean {
        val result = validator.validate(request)
        _uiState.value = _uiState.value.copy(validationError = result.errorMessage)
        return result.isValid
    }
}