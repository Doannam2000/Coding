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
import java.io.File

data class PickImagesUiState(
    val selectedMedia: List<MediaItemModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class PickImagesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PickImagesUiState())
    val uiState: StateFlow<PickImagesUiState> = _uiState.asStateFlow()

    fun loadImagesInfo(paths: List<String>) {
        if (paths.isEmpty()) {
            _uiState.update { it.copy(selectedMedia = emptyList(), isLoading = false) }
            return
        }

        // Check if we already have the info for all paths to avoid redundant loads
        val currentPaths = _uiState.value.selectedMedia.map { it.uri.path }
        if (currentPaths == paths) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    paths.map { path ->
                        ImageMetadataLoader.load(path)
                    }
                }
                _uiState.update { it.copy(selectedMedia = results, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load images info") }
            }
        }
    }
}
