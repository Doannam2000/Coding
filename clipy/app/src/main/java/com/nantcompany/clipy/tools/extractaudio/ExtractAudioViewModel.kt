package com.nantcompany.clipy.tools.extractaudio

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.edit.tools.extractaudio.ExtractAudioRequest
import com.nantcompany.clipy.edit.tools.extractaudio.ExtractAudioValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExtractAudioUiState(
    val validationError: String? = null
)

class ExtractAudioViewModel(
    private val validator: ExtractAudioValidator = ExtractAudioValidator()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExtractAudioUiState())
    val uiState: StateFlow<ExtractAudioUiState> = _uiState.asStateFlow()

    fun validate(request: ExtractAudioRequest): Boolean {
        val result = validator.validate(request)
        _uiState.value = _uiState.value.copy(validationError = result.errorMessage)
        return result.isValid
    }
}
