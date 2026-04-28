package com.natncompany.media

import android.net.Uri

data class MediaImportInput(
    val uri: Uri? = null,
    val filePath: String? = null
)

data class MediaExportConfig(
    val outputFileName: String,
    val timeline: Timeline? = null,
    val renderConfig: RenderConfig = RenderConfig(outputFileName)
)

data class MediaSessionState(
    val currentProject: VideoProject? = null,
    val currentTimeline: Timeline? = null,
    val previewState: PreviewState = PreviewState(),
    val activeImportJobs: List<ActiveMediaJob> = emptyList(),
    val activeTranscodeJobs: List<ActiveMediaJob> = emptyList(),
    val activeRenderJobs: List<ActiveMediaJob> = emptyList(),
    val lastError: MediaError? = null
) {
    val isProjectOpen: Boolean
        get() = currentProject != null
}

data class ActiveMediaJob(
    val id: String,
    val type: MediaJobType,
    val assetId: String? = null,
    val progressPercent: Int? = null
)

enum class MediaJobType {
    Import,
    Transcode,
    Render
}

sealed interface MediaSessionEvent {
    data class ProjectOpened(val project: VideoProject) : MediaSessionEvent
    data class ProjectClosed(val projectId: String?) : MediaSessionEvent
    data class ImportStarted(val job: ActiveMediaJob, val input: MediaImportInput) : MediaSessionEvent
    data class ImportCompleted(val job: ActiveMediaJob, val asset: Asset) : MediaSessionEvent
    data class ImportFailed(val job: ActiveMediaJob, val error: MediaError) : MediaSessionEvent
    data class TranscodeStarted(val job: ActiveMediaJob, val asset: Asset) : MediaSessionEvent
    data class TranscodeCompleted(val job: ActiveMediaJob, val asset: Asset) : MediaSessionEvent
    data class TranscodeFailed(val job: ActiveMediaJob, val error: MediaError) : MediaSessionEvent
    data object PreviewReady : MediaSessionEvent
    data class PreviewError(val error: MediaError) : MediaSessionEvent
    data class RenderStarted(val job: ActiveMediaJob, val request: RenderRequest) : MediaSessionEvent
    data class RenderProgress(val job: ActiveMediaJob, val progressPercent: Int) : MediaSessionEvent
    data class RenderCompleted(val job: ActiveMediaJob, val outputPath: String) : MediaSessionEvent
    data class RenderFailed(val job: ActiveMediaJob, val error: MediaError) : MediaSessionEvent
    data class JobCancelled(val job: ActiveMediaJob) : MediaSessionEvent
}

data class CodecDescriptor(
    val name: String,
    val mimeTypes: List<String>,
    val isEncoder: Boolean,
    val isHardwareAccelerated: Boolean,
    val isSoftwareOnly: Boolean,
    val isVendor: Boolean,
    val canonicalName: String?
)

data class AssetDebugReport(
    val assetId: String,
    val displayName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val mimeType: String?,
    val metadata: MediaMetadata?,
    val compatibility: Compatibility?,
    val availableDecoders: List<CodecDescriptor>,
    val availableEncoders: List<CodecDescriptor>,
    val compatibilityReasons: List<String>,
    val codecInfo: List<String>,
    val summary: String
)
