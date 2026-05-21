package com.nantcompany.clipy.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PickVideoUiState(
    val selectedMedia: MediaItemModel? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class PickVideoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PickVideoUiState())
    val uiState: StateFlow<PickVideoUiState> = _uiState.asStateFlow()

    fun loadVideoInfo(path: String?) {
        if (path.isNullOrBlank()) {
            _uiState.update { it.copy(selectedMedia = null, isLoading = false, error = null) }
            return
        }

        if (_uiState.value.selectedMedia?.uri?.path == path) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    VideoMetadataLoader.load(path)
                }
                _uiState.update { it.copy(selectedMedia = result, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load video info") }
            }
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedMedia = null, error = null) }
    }
}
