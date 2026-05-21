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

data class PickAudioUiState(
    val audioList: List<MediaItemModel> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = ""
)

class PickAudioViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PickAudioUiState())
    val uiState: StateFlow<PickAudioUiState> = _uiState.asStateFlow()

    fun loadAudios(context: Context) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val audios = withContext(Dispatchers.IO) {
                MediaScanner.getAllAudio(context)
            }
            _uiState.update { it.copy(audioList = audios, isLoading = false) }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}
