package com.nantcompany.clipy.app

import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.export.output.OutputMedia

enum class ToolTarget {
    CUT,
    COMPRESS,
    EXTRACT_AUDIO,
    MERGE,
    SLIDESHOW,
    ROTATE,
    SPEED,
    CROP,
    FILTERS,
    REVERSE,
    STICKERS,
    TEXT_OVERLAY
}

data class EditorSessionState(
    val singleVideoPath: String? = null,
    val multipleVideoPaths: List<String> = emptyList(),
    val pendingMergeInsertIndex: Int? = null,
    val imagePaths: List<String> = emptyList(),
    val slideshowAudioPath: String? = null,
    val toolTarget: ToolTarget? = null,
    val pendingRequest: ProcessingRequest? = null,
    val lastOutput: OutputMedia? = null,
    val selectedHistoryOutput: OutputMedia? = null
)
