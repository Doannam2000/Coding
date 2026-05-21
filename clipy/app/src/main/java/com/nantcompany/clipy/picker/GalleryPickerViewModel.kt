package com.nantcompany.clipy.picker

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GalleryPickerUiState(
    val mediaItems: List<MediaItemModel> = emptyList(),
    val selectedItems: List<MediaItemModel> = emptyList(),
    val isLoading: Boolean = false,
    val filterType: MediaItemType = MediaItemType.VIDEO,
    val isMultiSelect: Boolean = false,
    val error: String? = null
)

class GalleryPickerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryPickerUiState())
    val uiState: StateFlow<GalleryPickerUiState> = _uiState.asStateFlow()

    fun loadMedia(context: Context, type: MediaItemType, isMulti: Boolean) {
        _uiState.update { it.copy(isLoading = true, filterType = type, isMultiSelect = isMulti, error = null) }
        viewModelScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    when (type) {
                        MediaItemType.VIDEO -> MediaScanner.getAllVideos(context)
                        MediaItemType.IMAGE -> MediaScanner.getAllImages(context)
                        else -> emptyList()
                    }
                }
                _uiState.update { it.copy(mediaItems = items, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to scan media.") }
            }
        }
    }

    fun toggleSelection(item: MediaItemModel) {
        val currentSelected = _uiState.value.selectedItems.toMutableList()
        val isMulti = _uiState.value.isMultiSelect

        if (currentSelected.any { it.uri == item.uri }) {
            currentSelected.removeAll { it.uri == item.uri }
        } else {
            if (!isMulti) {
                currentSelected.clear()
            }
            currentSelected.add(item)
        }
        _uiState.update { it.copy(selectedItems = currentSelected) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedItems = emptyList()) }
    }
}
