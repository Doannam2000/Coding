package com.nantcompany.clipy.tools.cut

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.edit.tools.cut.CutRequest
import com.nantcompany.clipy.edit.tools.cut.CutValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CutVideoUiState(
    val validationError: String? = null
)

class CutVideoViewModel(
    private val validator: CutValidator = CutValidator()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CutVideoUiState())
    val uiState: StateFlow<CutVideoUiState> = _uiState.asStateFlow()

    fun validate(request: CutRequest): Boolean {
        val result = validator.validate(request)
        _uiState.value = _uiState.value.copy(validationError = result.errorMessage)
        return result.isValid
    }
}
