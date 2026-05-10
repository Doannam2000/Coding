package com.nantcompany.clipy.home

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.export.output.LocalOutputRepository
import com.nantcompany.clipy.export.output.OutputMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val recentExports: List<OutputMedia> = emptyList()
)

class HomeViewModel(
    private val repository: LocalOutputRepository = LocalOutputRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadRecentExports(limit: Int = 3) {
        _uiState.value = HomeUiState(recentExports = repository.getAll().take(limit))
    }
}
