package com.nantcompany.clipy.app

import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.export.output.OutputMedia

data class EditorSessionState(
    val singleVideoPath: String? = null,
    val multipleVideoPaths: List<String> = emptyList(),
    val imagePaths: List<String> = emptyList(),
    val pendingRequest: ProcessingRequest? = null,
    val lastOutput: OutputMedia? = null,
    val selectedHistoryOutput: OutputMedia? = null
)
