package com.nantcompany.clipy.tools.merge

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.edit.tools.merge.MergeRequest
import com.nantcompany.clipy.edit.tools.merge.MergeValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MergeVideoUiState(
    val validationError: String? = null
)

class MergeVideoViewModel(
    private val validator: MergeValidator = MergeValidator()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MergeVideoUiState())
    val uiState: StateFlow<MergeVideoUiState> = _uiState.asStateFlow()

    fun validate(request: MergeRequest): Boolean {
        val result = validator.validate(request)
        _uiState.value = _uiState.value.copy(validationError = result.errorMessage)
        return result.isValid
    }
}
