package com.nantcompany.clipy.app

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.export.output.OutputMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditorSessionViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorSessionState())
    val state: StateFlow<EditorSessionState> = _state.asStateFlow()

    fun setSingleVideoPath(path: String) {
        _state.value = _state.value.copy(singleVideoPath = path)
    }

    fun setMultipleVideoPaths(paths: List<String>) {
        _state.value = _state.value.copy(multipleVideoPaths = paths)
    }

    fun setImagePaths(paths: List<String>) {
        _state.value = _state.value.copy(imagePaths = paths)
    }

    fun setPendingRequest(request: ProcessingRequest) {
        _state.value = _state.value.copy(pendingRequest = request)
    }

    fun clearPendingRequest() {
        _state.value = _state.value.copy(pendingRequest = null)
    }

    fun setLastOutput(output: OutputMedia) {
        _state.value = _state.value.copy(lastOutput = output)
    }

    fun setSelectedHistoryOutput(output: OutputMedia?) {
        _state.value = _state.value.copy(selectedHistoryOutput = output)
    }
}
