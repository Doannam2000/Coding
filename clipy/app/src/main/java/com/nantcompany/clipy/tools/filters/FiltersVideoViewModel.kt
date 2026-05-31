package com.nantcompany.clipy.tools.filters

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import com.nantcompany.clipy.filters.gpu.ClipyFilterType

data class FiltersVideoUiState(
    val inputPath: String? = null,
    val brightness: Float = 0.0f,
    val contrast: Float = 0.0f,
    val saturation: Float = 1.0f,
    val filterIntensity: Float = 1.0f,
    val selectedFilter: ClipyFilterType = ClipyFilterType.NONE
)

class FiltersVideoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FiltersVideoUiState())
    val uiState: StateFlow<FiltersVideoUiState> = _uiState.asStateFlow()

    fun setInputPath(path: String?) {
        _uiState.update { current ->
            if (current.inputPath == path) current.copy(inputPath = path)
            else FiltersVideoUiState(inputPath = path)
        }
    }

    fun setBrightness(value: Float) {
        _uiState.update { it.copy(brightness = value.coerceIn(-1.0f, 1.0f)) }
    }

    fun setContrast(value: Float) {
        _uiState.update { it.copy(contrast = value.coerceIn(-1.0f, 1.0f)) }
    }

    fun setSaturation(value: Float) {
        _uiState.update { it.copy(saturation = value.coerceIn(0.0f, 3.0f)) }
    }

    fun setFilterIntensity(value: Float) {
        _uiState.update { it.copy(filterIntensity = value.coerceIn(0.0f, 1.0f)) }
    }

    fun setFilter(filter: ClipyFilterType) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun reset() {
        _uiState.update {
            it.copy(
                brightness = 0.0f,
                contrast = 0.0f,
                saturation = 1.0f,
                filterIntensity = 1.0f,
                selectedFilter = ClipyFilterType.NONE
            )
        }
    }
}
