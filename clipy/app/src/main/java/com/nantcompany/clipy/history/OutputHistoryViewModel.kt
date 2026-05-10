package com.nantcompany.clipy.history

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.export.output.LocalOutputRepository
import com.nantcompany.clipy.export.output.OutputMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OutputHistoryUiState(
    val outputs: List<OutputMedia> = emptyList(),
    val message: String? = null
)

class OutputHistoryViewModel(
    private val repository: LocalOutputRepository = LocalOutputRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(OutputHistoryUiState())
    val uiState: StateFlow<OutputHistoryUiState> = _uiState.asStateFlow()

    fun loadHistory() {
        _uiState.value = OutputHistoryUiState(outputs = repository.getAll(), message = _uiState.value.message)
    }

    fun removeHistoryItem(output: OutputMedia) {
        repository.removeById(output.id)
        repository.removeByPath(output.path)
        _uiState.value = OutputHistoryUiState(
            outputs = repository.getAll(),
            message = "Removed from history."
        )
    }

    fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
