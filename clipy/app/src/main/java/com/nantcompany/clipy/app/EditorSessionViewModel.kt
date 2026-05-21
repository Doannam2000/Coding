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

    fun setToolTarget(target: ToolTarget?) {
        _state.value = _state.value.copy(toolTarget = target)
    }

    fun setSingleVideoPath(path: String?) {
        _state.value = _state.value.copy(singleVideoPath = path)
    }

    fun removeMultipleVideoAt(index: Int) {
        val current = _state.value.multipleVideoPaths
        if (index !in current.indices) return
        _state.value = _state.value.copy(multipleVideoPaths = current.filterIndexed { i, _ -> i != index })
    }

    fun moveMultipleVideo(fromIndex: Int, toIndex: Int) {
        val current = _state.value.multipleVideoPaths.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _state.value = _state.value.copy(multipleVideoPaths = current)
    }

    fun removeImageAt(index: Int) {
        val current = _state.value.imagePaths
        if (index !in current.indices) return
        _state.value = _state.value.copy(imagePaths = current.filterIndexed { i, _ -> i != index })
    }

    fun moveImage(fromIndex: Int, toIndex: Int) {
        val current = _state.value.imagePaths.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _state.value = _state.value.copy(imagePaths = current)
    }

    fun clearSingleVideo() {
        _state.value = _state.value.copy(singleVideoPath = null)
    }

    fun clearMultipleVideos() {
        _state.value = _state.value.copy(multipleVideoPaths = emptyList())
    }

    fun clearImages() {
        _state.value = _state.value.copy(imagePaths = emptyList())
    }

    fun setMultipleVideoPaths(paths: List<String>) {
        _state.value = _state.value.copy(multipleVideoPaths = paths)
    }

    fun appendMultipleVideoPaths(paths: List<String>) {
        if (paths.isEmpty()) return
        val current = _state.value.multipleVideoPaths
        _state.value = _state.value.copy(multipleVideoPaths = current + paths)
    }

    fun setImagePaths(paths: List<String>) {
        _state.value = _state.value.copy(imagePaths = paths)
    }

    fun setSlideshowAudioPath(path: String?) {
        _state.value = _state.value.copy(slideshowAudioPath = path)
    }

    fun clearSlideshowAudioPath() {
        _state.value = _state.value.copy(slideshowAudioPath = null)
    }

    fun setPendingRequest(request: ProcessingRequest) {
        _state.value = _state.value.copy(pendingRequest = request)
    }

    fun consumePendingRequest(): ProcessingRequest? {
        val request = _state.value.pendingRequest
        if (request != null) {
            _state.value = _state.value.copy(pendingRequest = null)
        }
        return request
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
