package com.natncompany.media

import android.net.Uri

data class Asset(
    val id: String,
    val sourceUri: String,
    val cachedPath: String,
    val displayName: String,
    val type: AssetType,
    val durationMs: Long? = null,
    val mimeType: String? = null,
    val fileSizeBytes: Long = 0L,
    val needsTranscode: Boolean = false
)

enum class AssetType {
    Video,
    Image,
    Audio,
    Unknown
}

enum class ImportStatus {
    Ok,
    Warning,
    Error
}

data class ImportRequest(
    val projectId: String,
    val uri: Uri? = null,
    val filePath: String? = null
)

data class ImportResult(
    val asset: Asset?,
    val status: ImportStatus,
    val warnings: List<String> = emptyList()
)

data class ImportFailure(
    val input: MediaImportInput,
    val error: MediaError
)

data class ImportBatchResult(
    val imported: List<Asset>,
    val failures: List<ImportFailure>
)

data class ImportBatchProgress(
    val total: Int,
    val completed: Int,
    val succeeded: Int,
    val failed: Int,
    val currentInput: MediaImportInput? = null,
    val latestAsset: Asset? = null,
    val latestError: MediaError? = null,
    val result: ImportBatchResult? = null
) {
    val progressPercent: Int
        get() = if (total <= 0) 100 else ((completed * 100) / total).coerceIn(0, 100)

    val isCompleted: Boolean
        get() = completed >= total
}

data class MediaMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val mimeType: String?,
    val bitrate: Int,
    val fps: Float?,
    val hasVideoTrack: Boolean,
    val hasAudioTrack: Boolean,
    val isVariableFrameRate: Boolean,
    val audioSampleRate: Int?,
    val audioChannels: Int? = null
)

data class Compatibility(
    val isSafe: Boolean,
    val needsTranscode: Boolean,
    val reasons: List<String> = emptyList()
)

typealias CompatibilityReport = Compatibility

data class MetadataResult(
    val metadata: MediaMetadata,
    val compatibility: Compatibility
)

data class VideoProject(
    val id: String,
    val name: String,
    val rootCachePath: String,
    val timeline: Timeline = Timeline(),
    val assets: List<Asset> = emptyList()
)

data class Timeline(
    val tracks: List<TimelineTrack> = listOf(
        TimelineTrack(id = "video-main", type = TrackType.Video, allowOverlap = false)
    ),
    val selectedClipIds: Set<String> = emptySet(),
    val clipGroups: List<ClipGroup> = emptyList(),
    val settings: TimelineSettings = TimelineSettings(),
    val history: EditHistory = EditHistory()
) {
    val durationMs: Long
        get() = TimelineCalculations.calculateTimelineDuration(this)
}

data class TimelineTrack(
    val id: String,
    val type: TrackType,
    val clips: List<TimelineClip> = emptyList(),
    val isEnabled: Boolean = true,
    val isLocked: Boolean = false,
    val isMuted: Boolean = false,
    val allowOverlap: Boolean = type != TrackType.Video
)

enum class TrackType {
    Video,
    Audio,
    Text,
    Sticker,
    Effect
}

data class TimelineClip(
    val id: String,
    val assetId: String,
    val assetType: AssetType,
    val timelineStartMs: Long,
    val sourceStartMs: Long = 0L,
    val sourceEndMs: Long,
    val sourceDurationMs: Long = sourceEndMs,
    val transform: ClipTransform = ClipTransform(),
    val audio: ClipAudio = ClipAudio(),
    val effect: ClipEffect = ClipEffect(),
    val metadata: ClipMetadata = ClipMetadata()
) {
    val visibleDurationMs: Long
        get() = sourceEndMs - sourceStartMs

    val timelineEndMs: Long
        get() = timelineStartMs + visibleDurationMs
}

data class ClipEffect(
    val intensity: Float = 1f,
    val parameters: Map<String, String> = emptyMap()
)

data class ClipMetadata(
    val label: String? = null,
    val groupId: String? = null,
    val createdFromSplit: Boolean = false,
    val defaultImageDurationMs: Long? = null
)

data class ClipGroup(
    val id: String,
    val clipIds: Set<String>
)

data class TimelineSettings(
    val snapEnabled: Boolean = true,
    val snapThresholdMs: Long = 120L,
    val defaultImageDurationMs: Long = 3_000L
)

data class EditHistory(
    val undoStack: List<EditOperation> = emptyList(),
    val redoStack: List<EditOperation> = emptyList()
)

data class EditOperation(
    val type: EditOperationType,
    val before: Timeline,
    val after: Timeline,
    val timestampMs: Long = System.currentTimeMillis(),
    val clipId: String? = null,
    val trackId: String? = null,
    val details: String? = null
)

enum class EditOperationType {
    Add,
    Remove,
    Trim,
    Split,
    Move,
    Duplicate,
    Adjust
}

data class ClipRange(
    val timelineStartMs: Long,
    val timelineEndMs: Long,
    val durationMs: Long
)

data class SourceRange(
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val durationMs: Long
)

data class SnapResult(
    val originalPositionMs: Long,
    val snappedPositionMs: Long,
    val didSnap: Boolean,
    val target: SnapTarget? = null
)

data class SnapTarget(
    val type: SnapTargetType,
    val positionMs: Long,
    val relatedTrackId: String? = null,
    val relatedClipId: String? = null
)

enum class SnapTargetType {
    Playhead,
    ClipStart,
    ClipEnd,
    TimelineStart,
    TrackNeighborStart,
    TrackNeighborEnd
}

internal object TimelineCalculations {
    fun calculateTimelineDuration(timeline: Timeline): Long {
        return timeline.tracks.maxOfOrNull { track ->
            calculateTrackDuration(track)
        } ?: 0L
    }

    fun calculateTrackDuration(track: TimelineTrack): Long {
        return track.clips.maxOfOrNull { clip -> clip.timelineEndMs } ?: 0L
    }
}

data class ClipTransform(
    val crop: Crop = Crop(),
    val rotationDegrees: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val blur: Float = 0f
)

data class Crop(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
)

data class ClipAudio(
    val volume: Float = 1f,
    val isMuted: Boolean = false,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val offsetMs: Long = 0L
)

data class PreviewState(
    val isPlaying: Boolean = false,
    val isPrepared: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentClipId: String? = null,
    val error: String? = null
)

data class TranscodeRequest(
    val asset: Asset,
    val projectId: String,
    val jobId: String = projectId,
    val maxWidth: Int = 1920,
    val maxHeight: Int = 1080,
    val maxFps: Int = 30
)

data class TranscodeUpdate(
    val progressPercent: Int,
    val asset: Asset? = null,
    val completed: Boolean = false,
    val error: MediaError? = null
)

data class RenderRequest(
    val project: VideoProject,
    val outputFileName: String,
    val timeline: Timeline = project.timeline,
    val jobId: String = project.id,
    val config: RenderConfig = RenderConfig(outputFileName)
)

data class RenderConfig(
    val outputFileName: String,
    val targetWidth: Int = 1920,
    val targetHeight: Int = 1080,
    val fps: Int = 30,
    val videoBitrate: Int = 8_000_000,
    val audioBitrate: Int = 192_000
)

data class RenderUpdate(
    val progressPercent: Int,
    val outputPath: String? = null,
    val completed: Boolean = false,
    val error: MediaError? = null
)

data class AudioInfo(
    val durationMs: Long,
    val sampleRateHz: Int?,
    val channels: Int?,
    val bitrate: Int?,
    val mimeType: String?
)

data class AudioSettings(
    val volume: Float? = null,
    val muted: Boolean? = null,
    val fadeInMs: Long? = null,
    val fadeOutMs: Long? = null,
    val offsetMs: Long? = null
)

data class AudioMixPlan(
    val items: List<AudioMixItem>,
    val timelineDurationMs: Long
)

data class AudioMixItem(
    val clipId: String,
    val assetId: String,
    val trackId: String,
    val startMs: Long,
    val durationMs: Long,
    val endMs: Long = startMs + durationMs,
    val sourceStartMs: Long,
    val sourceEndMs: Long = sourceStartMs + durationMs,
    val volume: Float,
    val muted: Boolean,
    val fadeInMs: Long,
    val fadeOutMs: Long,
    val audioOffsetMs: Long = 0L
)

data class WaveformPlaceholder(
    val bars: List<Float>,
    val durationMs: Long
)

data class WaveformConfig(
    val samples: Int = 64,
    val normalize: Boolean = true
)

data class WaveformProgress(
    val progressPercent: Int,
    val waveform: WaveformPlaceholder? = null,
    val completed: Boolean = false,
    val error: MediaError? = null
)

data class CacheLayout(
    val projectRoot: String,
    val assetsDir: String,
    val transcodedDir: String,
    val previewDir: String,
    val renderDir: String,
    val tempDir: String
)

data class CachedAssetFile(
    val projectId: String,
    val assetId: String,
    val displayName: String,
    val filePath: String
)

data class CacheStats(
    val projectId: String,
    val totalBytes: Long,
    val assetsBytes: Long,
    val transcodedBytes: Long,
    val previewBytes: Long,
    val renderBytes: Long,
    val tempBytes: Long
)
