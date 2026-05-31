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
    val durationMs: Long = 0L,
    val validationError: String? = null,
    val minDurationMs: Long = 500L
)

class CutVideoViewModel(
    private val validator: CutValidator = CutValidator()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CutVideoUiState())
    val uiState: StateFlow<CutVideoUiState> = _uiState.asStateFlow()

    fun setInputPath(path: String?) {
        _uiState.update { state ->
            if (state.inputPath == path) {
                state
            } else {
                CutVideoUiState(inputPath = path)
            }
        }
    }

    fun setDurationMs(durationMs: Long) {
        if (durationMs <= 0L) return
        _uiState.update { state ->
            val endMs = when {
                state.endMs > durationMs -> durationMs
                state.endMs == DEFAULT_END_MS -> durationMs
                else -> state.endMs
            }
            val startMs = state.startMs.coerceIn(0L, (endMs - state.minDurationMs).coerceAtLeast(0L))
            state.copy(durationMs = durationMs, startMs = startMs, endMs = endMs)
        }
        validateRange()
    }

    fun setStartMs(ms: Long) {
        _uiState.update { state ->
            val maxStart = (state.endMs - state.minDurationMs).coerceAtLeast(0L)
            state.copy(startMs = ms.coerceIn(0L, maxStart))
        }
        validateRange()
    }

    fun setEndMs(ms: Long) {
        _uiState.update { state ->
            val maxEnd = state.durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
            val minEnd = state.startMs + state.minDurationMs
            val endMs = if (maxEnd < minEnd) maxEnd else ms.coerceIn(minEnd, maxEnd)
            state.copy(endMs = endMs)
        }
        validateRange()
    }

    fun resetRange() {
        _uiState.update { state ->
            state.copy(
                startMs = 0L,
                endMs = state.durationMs.takeIf { it > 0L } ?: DEFAULT_END_MS,
                validationError = null
            )
        }
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

    private companion object {
        const val DEFAULT_END_MS = 5000L
    }
}
