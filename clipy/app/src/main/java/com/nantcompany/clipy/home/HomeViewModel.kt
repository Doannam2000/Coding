package com.nantcompany.clipy.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nantcompany.clipy.export.output.LocalOutputRepository
import com.nantcompany.clipy.export.output.OutputMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val recentExports: List<OutputMedia> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val repository: LocalOutputRepository = LocalOutputRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadRecentExports(limit: Int = 3) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                repository.getAll().take(limit)
            }
            _uiState.update { it.copy(recentExports = results, isLoading = false) }
        }
    }
}
