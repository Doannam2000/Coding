package com.nantcompany.clipy.tools.compress

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.edit.tools.compress.CompressRequest
import com.nantcompany.clipy.edit.tools.compress.CompressValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CompressVideoUiState(
    val validationError: String? = null
)

class CompressVideoViewModel(
    private val validator: CompressValidator = CompressValidator()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CompressVideoUiState())
    val uiState: StateFlow<CompressVideoUiState> = _uiState.asStateFlow()

    fun validate(request: CompressRequest): Boolean {
        val result = validator.validate(request)
        _uiState.value = _uiState.value.copy(validationError = result.errorMessage)
        return result.isValid
    }
}
