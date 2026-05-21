package com.nantcompany.clipy.picker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class MediaPreviewUiState(
    val videoInfo: MediaItemModel? = null,
    val imagePreviews: List<ImagePreviewData> = emptyList(),
    val isLoading: Boolean = false
)

data class ImagePreviewData(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val thumbnail: Bitmap? = null
)

class MediaPreviewViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MediaPreviewUiState())
    val uiState: StateFlow<MediaPreviewUiState> = _uiState.asStateFlow()

    fun loadVideoPreview(path: String?) {
        if (path.isNullOrBlank()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                VideoMetadataLoader.load(path)
            }
            _uiState.update { it.copy(videoInfo = result, isLoading = false) }
        }
    }

    fun loadImagesPreview(paths: List<String>) {
        if (paths.isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                paths.map { path ->
                    val file = File(path)
                    val thumb = runCatching {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(path, options)
                        val sample = maxOf(1, minOf(options.outWidth / 512, options.outHeight / 512))
                        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
                        BitmapFactory.decodeFile(path, decodeOptions)
                    }.getOrNull()
                    ImagePreviewData(
                        path = path,
                        name = file.name,
                        sizeBytes = file.length(),
                        thumbnail = thumb
                    )
                }
            }
            _uiState.update { it.copy(imagePreviews = results, isLoading = false) }
        }
    }
}
