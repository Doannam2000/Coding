package com.nantcompany.clipy.tools.slideshow

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.edit.tools.slideshow.SlideshowRequest
import com.nantcompany.clipy.edit.tools.slideshow.SlideshowValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SlideshowUiState(
    val validationError: String? = null
)

class SlideshowViewModel(
    private val validator: SlideshowValidator = SlideshowValidator()
) : ViewModel() {
    private val _uiState = MutableStateFlow(SlideshowUiState())
    val uiState: StateFlow<SlideshowUiState> = _uiState.asStateFlow()

    fun validate(request: SlideshowRequest): Boolean {
        val result = validator.validate(request)
        _uiState.value = _uiState.value.copy(validationError = result.errorMessage)
        return result.isValid
    }
}
