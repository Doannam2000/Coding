package com.nantcompany.clipy.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val enableHardwareAcceleration: Boolean = true,
    val keepOriginalFiles: Boolean = true
)

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleHardwareAcceleration() {
        _uiState.value = _uiState.value.copy(
            enableHardwareAcceleration = !_uiState.value.enableHardwareAcceleration
        )
    }

    fun toggleKeepOriginal() {
        _uiState.value = _uiState.value.copy(
            keepOriginalFiles = !_uiState.value.keepOriginalFiles
        )
    }
}
