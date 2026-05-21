package com.nantcompany.clipy.tools.textoverlay

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TextOverlayVideoUiState(
    val inputPath: String? = null,
    val text: String = "Clipy",
    val x: Float = 10f,
    val y: Float = 10f,
    val fontSize: Int = 24,
    val fontColor: String = "white"
)

class TextOverlayVideoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TextOverlayVideoUiState())
    val uiState: StateFlow<TextOverlayVideoUiState> = _uiState.asStateFlow()

    fun setInputPath(path: String?) {
        _uiState.update { it.copy(inputPath = path) }
    }

    fun setText(text: String) {
        _uiState.update { it.copy(text = text) }
    }

    fun setPosition(x: Float, y: Float) {
        _uiState.update { it.copy(x = x, y = y) }
    }

    fun setFontSize(size: Int) {
        _uiState.update { it.copy(fontSize = size.coerceIn(10, 100)) }
    }

    fun setFontColor(color: String) {
        _uiState.update { it.copy(fontColor = color) }
    }
}
