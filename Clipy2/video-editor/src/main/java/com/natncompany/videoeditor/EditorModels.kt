package com.natncompany.videoeditor

import android.net.Uri
import com.natncompany.media.MediaExportConfig
import com.natncompany.media.Crop
import com.natncompany.media.RenderConfig
import com.natncompany.media.Timeline
import com.natncompany.media.TimelineClip
import com.natncompany.media.TimelineTrack
import com.natncompany.media.VideoProject

data class EditorUiState(
    val project: VideoProject? = null,
    val timeline: Timeline = Timeline(),
    val selectedClipId: String? = null,
    val isPlaying: Boolean = false,
    val isPreviewPrepared: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val importProgress: Int? = null,
    val exportProgress: Int? = null,
    val activeTool: EditorTool? = null,
    val previewError: String? = null,
    val snackbarErrorMessage: String? = null,
    val criticalErrorMessage: String? = null,
    val exportResultPath: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
) {
    val selectedClip: TimelineClip?
        get() = timeline.findClip(selectedClipId)
}

enum class ExportQuality(
    val label: String,
    val width: Int,
    val height: Int,
    val videoBitrate: Int,
    val audioBitrate: Int
) {
    Low("Low", 854, 480, 2_000_000, 128_000),
    Medium("Medium", 1280, 720, 5_000_000, 160_000),
    High("High", 1920, 1080, 8_000_000, 192_000)
}

enum class EditorTool(val label: String) {
    Import("Import"),
    Split("Split"),
    Delete("Delete"),
    Duplicate("Duplicate"),
    Trim("Trim"),
    Crop("Crop"),
    Rotate("Rotate"),
    Filter("Filter"),
    Speed("Speed"),
    Volume("Volume"),
    Text("Text"),
    Sticker("Sticker"),
    Music("Music"),
    Background("Background"),
    Canvas("Canvas"),
    Effects("Effects")
}

sealed class EditorAction {
    data object PlayPause : EditorAction()
    data class Seek(val positionMs: Long) : EditorAction()
    data class SelectClip(val clipId: String?) : EditorAction()
    data class MoveClip(val trackId: String, val clipId: String, val newStartMs: Long) : EditorAction()
    data class TrimClip(
        val trackId: String,
        val clipId: String,
        val newSourceStartMs: Long? = null,
        val newSourceEndMs: Long? = null
    ) : EditorAction()
    data object Split : EditorAction()
    data object Undo : EditorAction()
    data object Redo : EditorAction()
    data object Delete : EditorAction()
    data class Export(val quality: ExportQuality = ExportQuality.High) : EditorAction()
    data class Import(val uris: List<Uri>) : EditorAction()
    data class SelectTool(val tool: EditorTool) : EditorAction()
    data class ApplyFilter(val filter: ClipFilter) : EditorAction()
    data class ApplyNamedFilter(val filterName: String) : EditorAction()
    data class SetBrightness(val brightness: Float) : EditorAction()
    data class SetContrast(val contrast: Float) : EditorAction()
    data class SetSaturation(val saturation: Float) : EditorAction()
    data object RotateLeft : EditorAction()
    data object RotateRight : EditorAction()
    data object FlipHorizontal : EditorAction()
    data object FlipVertical : EditorAction()
    data class SetVolume(val volume: Float) : EditorAction()
    data class SetMuted(val muted: Boolean) : EditorAction()
    data class SetCrop(val crop: Crop, val label: String) : EditorAction()
    data class SetSpeed(val speed: Float) : EditorAction()
}

enum class ClipFilter(val label: String) {
    Brightness("Brightness"),
    Contrast("Contrast"),
    Blur("Blur")
}

val legacyFilterOptions = listOf(
    "Original",
    "Sepia",
    "Mono",
    "Monochrome",
    "Luminance",
    "Invert",
    "Solarize",
    "Posterize",
    "CGA",
    "False Color",
    "RGB Warm",
    "RGB Cool",
    "Hue Shift",
    "Gamma",
    "Exposure",
    "White Balance",
    "Highlight Shadow",
    "Sketch",
    "Toon",
    "Smooth Toon",
    "Sobel Edge",
    "Sobel Threshold",
    "Threshold Edge",
    "Directional Edge",
    "Laplacian",
    "Luma Threshold",
    "Crosshatch",
    "Halftone",
    "Pixel",
    "Emboss",
    "Sharpen",
    "Gaussian Blur",
    "Box Blur",
    "Bilateral Blur",
    "Vignette",
    "Haze",
    "Kuwahara",
    "Swirl",
    "Bulge",
    "Glass Sphere",
    "Sphere",
    "Zoom Blur"
)

internal fun Timeline.findClipTrack(clipId: String?): TimelineTrack? {
    if (clipId == null) return null
    return tracks.firstOrNull { track -> track.clips.any { it.id == clipId } }
}

internal fun Timeline.findClip(clipId: String?): TimelineClip? {
    if (clipId == null) return null
    return tracks.asSequence().flatMap { it.clips.asSequence() }.firstOrNull { it.id == clipId }
}

internal fun Timeline.toDefaultExportConfig(
    outputFileName: String,
    quality: ExportQuality = ExportQuality.High
): MediaExportConfig {
    return MediaExportConfig(
        outputFileName = outputFileName,
        timeline = this,
        renderConfig = RenderConfig(
            outputFileName = outputFileName,
            targetWidth = quality.width,
            targetHeight = quality.height,
            videoBitrate = quality.videoBitrate,
            audioBitrate = quality.audioBitrate
        )
    )
}
