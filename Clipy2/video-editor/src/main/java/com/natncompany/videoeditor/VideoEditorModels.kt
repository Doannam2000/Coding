package com.natncompany.videoeditor

data class VideoEditorSession(
    val projectName: String,
    val canvas: VideoCanvas,
    val timeline: VideoTimeline,
    val audioMix: AudioMix = AudioMix()
)

data class VideoCanvas(
    val label: String,
    val previewAspectRatio: Float
)

data class VideoTimeline(
    val clips: List<TimelineClip>
) {
    val durationMs: Long
        get() = clips.sumOf { it.outputDurationMs }
}

data class TimelineClip(
    val id: String,
    val sourceUri: String,
    val mediaType: MediaType,
    val displayName: String,
    val sourceDurationMs: Long,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val outputDurationMs: Long,
    val transform: ClipTransform,
    val visualEffect: VisualEffect,
    val backgroundHex: String,
    val volume: Float
)

enum class MediaType {
    Video,
    Image
}

data class ClipTransform(
    val speed: Float = 1f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f
)

data class VisualEffect(
    val filterName: String = "Original",
    val usesOpenGl: Boolean = false
)

data class AudioMix(
    val sourceVolume: Float = 1f,
    val musicVolume: Float = 0f,
    val voiceOverVolume: Float = 0f
)

data class ExportRequest(
    val outputFileName: String,
    val preferMediaCodec: Boolean = true,
    val allowFfmpegFallback: Boolean = true
)

data class PreviewPlan(
    val engine: PreviewEngine,
    val stages: List<PipelineStage>
)

data class ExportPlan(
    val primaryEngine: ExportEngineType,
    val fallbackEngine: ExportEngineType?,
    val stages: List<PipelineStage>
)

enum class PreviewEngine {
    MediaCodecRealtime
}

enum class ExportEngineType {
    MediaCodec,
    Ffmpeg
}

enum class PipelineStage {
    Timeline,
    MediaCodecPreview,
    OpenGlEffect,
    MediaCodecExport,
    FfmpegFallback
}

data class ExportResult(
    val engine: ExportEngineType,
    val usedFallback: Boolean,
    val outputFileName: String,
    val notes: String
)
