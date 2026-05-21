package com.nantcompany.clipy.tools.cut

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.edit.tools.cut.CutRequest
import com.nantcompany.clipy.edit.tools.cut.CutValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CutVideoUiState(
    val inputPath: String? = null,
    val startMs: Long = 0L,
    val endMs: Long = 5000L,
    val validationError: String? = null,
    val minDurationMs: Long = 300L
)

class CutVideoViewModel(
    private val validator: CutValidator = CutValidator()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CutVideoUiState())
    val uiState: StateFlow<CutVideoUiState> = _uiState.asStateFlow()

    fun setInputPath(path: String?) {
        _uiState.update { it.copy(inputPath = path) }
    }

    fun setStartMs(ms: Long) {
        _uiState.update { it.copy(startMs = ms.coerceAtLeast(0L)) }
        validateRange()
    }

    fun setEndMs(ms: Long) {
        _uiState.update { it.copy(endMs = ms.coerceAtLeast(0L)) }
        validateRange()
    }

    fun resetRange() {
        _uiState.update { it.copy(startMs = 0L, endMs = 5000L, validationError = null) }
    }

    private fun validateRange() {
        val state = _uiState.value
        val selectedDurationMs = state.endMs - state.startMs
        val error = when {
            state.endMs <= state.startMs -> "End must be greater than start."
            selectedDurationMs < state.minDurationMs -> "Minimum duration is ${state.minDurationMs} ms."
            else -> null
        }
        _uiState.update { it.copy(validationError = error) }
    }

    fun validate(request: CutRequest): Boolean {
        val result = validator.validate(request)
        _uiState.update { it.copy(validationError = result.errorMessage) }
        return result.isValid
    }
}
