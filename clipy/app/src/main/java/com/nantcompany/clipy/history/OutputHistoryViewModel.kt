package com.nantcompany.clipy.history

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.export.output.LocalOutputRepository
import com.nantcompany.clipy.export.output.OutputMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OutputHistoryUiState(
    val outputs: List<OutputMedia> = emptyList()
)

class OutputHistoryViewModel(
    private val repository: LocalOutputRepository = LocalOutputRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(OutputHistoryUiState())
    val uiState: StateFlow<OutputHistoryUiState> = _uiState.asStateFlow()

    fun loadHistory() {
        _uiState.value = OutputHistoryUiState(repository.getAll())
    }
}
