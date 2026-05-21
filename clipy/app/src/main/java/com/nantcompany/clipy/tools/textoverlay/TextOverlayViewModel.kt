package com.nantcompany.clipy.tools.textoverlay

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.edit.tools.textoverlay.TextOverlayRequest
import com.nantcompany.clipy.edit.tools.textoverlay.TextOverlayValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TextOverlayUiState(
    val text: String = "",
    val fontSize: Int = 48,
    val fontColor: String = "white",
    val x: Int = 10,
    val y: Int = 10,
    val validationError: String? = null
)

class TextOverlayViewModel(
    private val validator: TextOverlayValidator = TextOverlayValidator()
) : ViewModel() {
    private val _uiState = MutableStateFlow(TextOverlayUiState())
    val uiState: StateFlow<TextOverlayUiState> = _uiState.asStateFlow()

    fun updateText(value: String) {
        _uiState.value = _uiState.value.copy(text = value)
    }

    fun updateFontSize(value: Int) {
        _uiState.value = _uiState.value.copy(fontSize = value.coerceIn(12, 200))
    }

    fun updateFontColor(value: String) {
        _uiState.value = _uiState.value.copy(fontColor = value)
    }

    fun updateX(value: Int) {
        _uiState.value = _uiState.value.copy(x = value.coerceAtLeast(0))
    }

    fun updateY(value: Int) {
        _uiState.value = _uiState.value.copy(y = value.coerceAtLeast(0))
    }

    fun validate(request: TextOverlayRequest): Boolean {
        val result = validator.validate(request)
        _uiState.value = _uiState.value.copy(validationError = result.errorMessage)
        return result.isValid
    }
}