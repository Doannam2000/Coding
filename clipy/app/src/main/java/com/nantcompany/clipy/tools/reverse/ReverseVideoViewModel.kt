package com.nantcompany.clipy.tools.reverse

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ReverseVideoUiState(
    val inputPath: String? = null
)

class ReverseVideoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ReverseVideoUiState())
    val uiState: StateFlow<ReverseVideoUiState> = _uiState.asStateFlow()

    fun setInputPath(path: String?) {
        _uiState.update { it.copy(inputPath = path) }
    }
}
