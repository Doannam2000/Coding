package com.example.clipy.clipy.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class AppLanguage(val code: String) {
  English("en"),
  Vietnamese("vi"),
}

enum class CropRatio(val label: String) {
  Square("1:1"),
  Portrait("4:5"),
  Story("9:16"),
  Landscape("16:9"),
}

enum class WatermarkPosition {
  TopLeft,
  TopRight,
  BottomLeft,
  BottomRight,
  Center,
}

enum class ExportFormat { Gif, Mp4 }

enum class Mp4Quality {
  Fast,
  Balanced,
  Crisp,
}

enum class SaveBehavior {
  AppFolder,
  PromptEachTime,
  ShareFirst,
}

data class ProjectDraft(
  val id: String = "draft",
  val sourceUri: String = "",
  val displayName: String = "No clip selected",
  val sourceDurationMs: Long = 12000L,
  val trimStartMs: Long = 0L,
  val trimEndMs: Long = 12000L,
  val playheadMs: Long = 0L,
  val timelineZoom: Float = 1f,
  val cropRatio: CropRatio = CropRatio.Story,
  val speedMultiplier: Float = 1f,
  val isMuted: Boolean = false,
  val isReversed: Boolean = false,
  val isBoomerang: Boolean = false,
  val watermarkText: String = "",
  val watermarkPosition: WatermarkPosition = WatermarkPosition.BottomRight,
  val exportFormat: ExportFormat = ExportFormat.Gif,
  val gifFps: Int = 18,
  val gifResolution: String = "720p",
  val mp4Quality: Mp4Quality = Mp4Quality.Balanced,
  val outputName: String = "clipy_export",
  val lastUpdatedAt: Long = System.currentTimeMillis(),
)

data class UserPreferences(
  val languageCode: String = AppLanguage.English.code,
  val defaultGifFps: Int = 18,
  val defaultGifResolution: String = "720p",
  val defaultMp4Quality: Mp4Quality = Mp4Quality.Balanced,
  val defaultMuteEnabled: Boolean = false,
  val defaultCropRatio: CropRatio = CropRatio.Story,
  val saveBehavior: SaveBehavior = SaveBehavior.AppFolder,
  val onboardingCompleted: Boolean = false,
)

data class ExportJobState(
  val jobId: String = "",
  val projectId: String = "draft",
  val progressPercent: Int = 0,
  val currentStep: String = "Preparing",
  val isCancellable: Boolean = false,
  val status: String = "Idle",
  val outputUri: String? = null,
  val errorMessage: String? = null,
)

@Entity(tableName = "export_records")
data class ExportRecord(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val sourceUri: String,
  val outputUri: String,
  val outputName: String,
  val format: String,
  val durationMs: Long,
  val cropRatio: String,
  val speedMultiplier: Float,
  val isMuted: Boolean,
  val isReversed: Boolean,
  val isBoomerang: Boolean,
  val watermarkText: String,
  val gifFps: Int?,
  val gifResolution: String?,
  val mp4Quality: String?,
  val status: String,
  val fileSizeBytes: Long,
  val createdAt: Long,
)

data class ExportRecordUi(
  val id: Long,
  val sourceUri: String,
  val outputName: String,
  val formatLabel: String,
  val timestampLabel: String,
  val detailLabel: String,
  val outputUri: String,
  val cropRatio: CropRatio,
  val speedMultiplier: Float,
  val isMuted: Boolean,
  val isReversed: Boolean,
  val isBoomerang: Boolean,
  val watermarkText: String,
  val gifFps: Int?,
  val gifResolution: String?,
  val mp4Quality: Mp4Quality?,
)

data class TimelineSnapshot(
  val durationMs: Long,
  val trimStartMs: Long,
  val trimEndMs: Long,
  val playheadMs: Long,
  val zoom: Float,
)

data class ExportValidation(
  val isValid: Boolean,
  val message: String? = null,
)

data class ResolutionPreset(
  val label: String,
  val width: Int,
  val height: Int,
)

data class ExportPlan(
  val extension: String,
  val mimeType: String,
  val ffmpegCommand: String,
  val progressSteps: List<String>,
  val warnings: List<String>,
)

private const val MIN_TRIM_GAP_MS = 250L
private const val FRAME_STEP_MS = 33L
private const val MAX_GIF_DURATION_MS = 15_000L

val SupportedGifFps = listOf(12, 18, 24, 30)
val SupportedGifResolutions = listOf("480p", "720p", "1080p")

fun ProjectDraft.timelineSnapshot(): TimelineSnapshot =
  TimelineSnapshot(
    durationMs = sourceDurationMs.coerceAtLeast(trimEndMs.coerceAtLeast(1L)),
    trimStartMs = trimStartMs,
    trimEndMs = trimEndMs,
    playheadMs = playheadMs,
    zoom = timelineZoom,
  )

fun sanitizeTimeline(
  durationMs: Long,
  trimStartMs: Long,
  trimEndMs: Long,
  playheadMs: Long,
  zoom: Float,
): TimelineSnapshot {
  val boundedDuration = durationMs.coerceAtLeast(MIN_TRIM_GAP_MS * 2)
  val safeStart = trimStartMs.coerceIn(0L, boundedDuration - MIN_TRIM_GAP_MS)
  val safeEnd = trimEndMs.coerceIn(safeStart + MIN_TRIM_GAP_MS, boundedDuration)
  return TimelineSnapshot(
    durationMs = boundedDuration,
    trimStartMs = safeStart,
    trimEndMs = safeEnd,
    playheadMs = playheadMs.coerceIn(safeStart, safeEnd),
    zoom = zoom.coerceIn(1f, 6f),
  )
}

fun snapTimelineMs(valueMs: Long, stepMs: Long = FRAME_STEP_MS): Long {
  if (stepMs <= 0L) return valueMs
  val snapped = ((valueMs + (stepMs / 2)) / stepMs) * stepMs
  return snapped.coerceAtLeast(0L)
}

fun sanitizeOutputName(name: String): String {
  val sanitized = name
    .trim()
    .replace(Regex("[^A-Za-z0-9._-]+"), "_")
    .replace(Regex("_+"), "_")
    .trim('_', '.')
  return sanitized.ifBlank { "clipy_export" }
}

fun shouldPersistUri(uriString: String): Boolean = uriString.startsWith("content://")

fun timelineFrameStepMs(durationMs: Long): Long =
  when {
    durationMs <= 6_000L -> FRAME_STEP_MS
    durationMs <= 20_000L -> 50L
    else -> 100L
  }

fun thumbnailCaptureTimesMs(timeline: TimelineSnapshot, frameCount: Int): List<Long> {
  if (frameCount <= 1) return listOf(timeline.trimStartMs)
  val span = (timeline.trimEndMs - timeline.trimStartMs).coerceAtLeast(1L)
  val step = span / (frameCount - 1).toDouble()
  return List(frameCount) { index ->
    (timeline.trimStartMs + (step * index)).toLong().coerceIn(timeline.trimStartMs, timeline.trimEndMs)
  }
}

fun timelineThumbnailCount(zoom: Float, viewportWidthPx: Int): Int {
  val baseCount = ceil(viewportWidthPx.coerceAtLeast(240) / 64.0).toInt()
  return (baseCount + (zoom - 1f).coerceAtLeast(0f).roundToInt() * 3).coerceIn(6, 36)
}

fun timelineScrollForPlayhead(playheadFraction: Float, contentWidthPx: Int, viewportWidthPx: Int): Int {
  val rawTarget = (contentWidthPx * playheadFraction) - (viewportWidthPx / 2f)
  return rawTarget.roundToInt().coerceAtLeast(0)
}

fun resolutionPreset(label: String, cropRatio: CropRatio): ResolutionPreset {
  val shortEdge = when (label) {
    "480p" -> 480
    "720p" -> 720
    else -> 1080
  }
  return when (cropRatio) {
    CropRatio.Square -> ResolutionPreset(label, shortEdge, shortEdge)
    CropRatio.Portrait -> ResolutionPreset(label, shortEdge, (shortEdge * 1.25f).roundToInt())
    CropRatio.Story -> ResolutionPreset(label, shortEdge, (shortEdge * (16f / 9f)).roundToInt())
    CropRatio.Landscape -> ResolutionPreset(label, (shortEdge * (16f / 9f)).roundToInt(), shortEdge)
  }
}

fun buildExportPlan(draft: ProjectDraft): ExportPlan {
  val durationMs = (draft.trimEndMs - draft.trimStartMs).coerceAtLeast(MIN_TRIM_GAP_MS)
  val resolution = resolutionPreset(draft.gifResolution, draft.cropRatio)
  val filterParts = buildList {
    add("trim=start=${draft.trimStartMs / 1000f}:end=${draft.trimEndMs / 1000f}")
    add("setpts=${1f / draft.speedMultiplier}*PTS")
    if (draft.isReversed) add("reverse")
    if (draft.isBoomerang) add("split[a][b];[b]reverse[br];[a][br]concat=n=2:v=1:a=0")
    add("scale=${resolution.width}:${resolution.height}:force_original_aspect_ratio=decrease")
    if (draft.watermarkText.isNotBlank()) {
      add("drawtext=text='${draft.watermarkText.replace("'", "\\'")}':x=(w-text_w-24):y=(h-text_h-24):fontsize=28:fontcolor=white")
    }
  }
  val audioParts = buildList {
    if (draft.isMuted || draft.exportFormat == ExportFormat.Gif) add("-an")
    else add("-c:a aac")
  }
  val qualityArgs = when (draft.mp4Quality) {
    Mp4Quality.Fast -> "-preset veryfast -crf 28"
    Mp4Quality.Balanced -> "-preset medium -crf 23"
    Mp4Quality.Crisp -> "-preset slow -crf 18"
  }
  val warnings = buildList {
    if (draft.exportFormat == ExportFormat.Gif && durationMs > MAX_GIF_DURATION_MS) add("Long GIF exports should be shortened for stable encoding.")
    if (draft.exportFormat == ExportFormat.Gif && draft.gifFps >= 24 && draft.gifResolution == "1080p") add("1080p GIF at 24+ FPS may be heavy on mid-range devices.")
    if (draft.sourceDurationMs >= 120_000L) add("Large source video detected; keep the trim range focused for faster processing.")
  }
  val command = if (draft.exportFormat == ExportFormat.Gif) {
    "ffmpeg -i INPUT -vf \"${filterParts.joinToString(",")} ,fps=${draft.gifFps},split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse\" -loop 0 OUTPUT.gif"
      .replace(" ,fps", ",fps")
  } else {
    "ffmpeg -i INPUT -vf \"${filterParts.joinToString(",")}\" ${audioParts.joinToString(" ")} $qualityArgs -movflags +faststart OUTPUT.mp4"
  }
  val steps = if (draft.exportFormat == ExportFormat.Gif) {
    listOf("Opening source URI", "Sampling timeline frames", "Rendering GIF palette", "Writing GIF export", "Saving to MediaStore")
  } else {
    listOf("Opening source URI", "Sampling timeline frames", "Building MP4 filters", "Writing MP4 export", "Saving to MediaStore")
  }
  return ExportPlan(
    extension = if (draft.exportFormat == ExportFormat.Gif) ".gif" else ".mp4",
    mimeType = if (draft.exportFormat == ExportFormat.Gif) "image/gif" else "video/mp4",
    ffmpegCommand = command,
    progressSteps = steps,
    warnings = warnings,
  )
}

fun ProjectDraft.validateExport(): ExportValidation {
  if (sourceUri.isBlank()) return ExportValidation(false, "Import a video before exporting.")
  if (trimEndMs - trimStartMs < MIN_TRIM_GAP_MS) return ExportValidation(false, "Trim range must be at least 250 ms.")
  if (sanitizeOutputName(outputName).isBlank()) return ExportValidation(false, "Output name is invalid.")
  if (exportFormat == ExportFormat.Gif && gifFps !in SupportedGifFps) {
    return ExportValidation(false, "GIF FPS must be one of the supported presets.")
  }
  if (exportFormat == ExportFormat.Gif && gifResolution !in SupportedGifResolutions) {
    return ExportValidation(false, "GIF resolution must be 480p, 720p, or 1080p.")
  }
  if (exportFormat == ExportFormat.Gif && trimEndMs - trimStartMs > MAX_GIF_DURATION_MS) {
    return ExportValidation(false, "GIF export must stay within 15 seconds for reliable output.")
  }
  return ExportValidation(true)
}
