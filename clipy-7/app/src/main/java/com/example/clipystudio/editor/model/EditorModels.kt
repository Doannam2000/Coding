package com.example.clipystudio.editor.model

data class Project(
  val id: String,
  val name: String,
  val createdAtMs: Long,
  val updatedAtMs: Long,
  val durationMs: Long,
  val tracks: List<Track>,
  val overlays: List<Overlay>,
  val transitions: List<Transition>,
  val exportSettings: ExportSettings,
  val metadata: Map<String, String> = emptyMap(),
)

data class Track(
  val id: String,
  val name: String,
  val type: TrackType,
  val order: Int,
  val clips: List<Clip>,
  val isMuted: Boolean = false,
  val isLocked: Boolean = false,
  val isVisible: Boolean = true,
)

data class Clip(
  val id: String,
  val trackId: String,
  val sourceUri: String?,
  val startMs: Long,
  val durationMs: Long,
  val trimStartMs: Long,
  val trimEndMs: Long,
  val zIndex: Float,
  val effects: List<EffectClip> = emptyList(),
  val keyframes: List<Keyframe> = emptyList(),
  val isSelected: Boolean = false,
  val isLocked: Boolean = false,
)

data class Overlay(
  val id: String,
  val clipId: String?,
  val type: OverlayType,
  val startMs: Long,
  val durationMs: Long,
  val x: Float,
  val y: Float,
  val width: Float,
  val height: Float,
  val rotationDegrees: Float,
  val scale: Float,
  val alpha: Float,
  val zIndex: Float,
  val isVisible: Boolean = true,
  val isLocked: Boolean = false,
  val keyframes: List<Keyframe> = emptyList(),
)

data class AudioClip(
  val id: String,
  val trackId: String,
  val sourceUri: String,
  val startMs: Long,
  val durationMs: Long,
  val trimStartMs: Long,
  val trimEndMs: Long,
  val volume: Float,
  val fadeInMs: Long,
  val fadeOutMs: Long,
  val isMuted: Boolean = false,
  val waveformCacheKey: String? = null,
)

data class TextClip(
  val id: String,
  val text: String,
  val fontFamily: String?,
  val fontSizeSp: Float,
  val color: Long,
  val backgroundColor: Long?,
  val alignment: TextAlignment,
  val style: TextStyleRef,
  val overlay: Overlay,
)

data class StickerClip(
  val id: String,
  val assetId: String,
  val sourceUri: String?,
  val category: String?,
  val tintColor: Long?,
  val overlay: Overlay,
)

data class EffectClip(
  val id: String,
  val type: EffectType,
  val startMs: Long,
  val durationMs: Long,
  val intensity: Float,
  val parameters: Map<String, String> = emptyMap(),
  val keyframes: List<Keyframe> = emptyList(),
  val isEnabled: Boolean = true,
)

data class Transition(
  val id: String,
  val fromClipId: String,
  val toClipId: String,
  val type: TransitionType,
  val durationMs: Long,
  val parameters: Map<String, String> = emptyMap(),
)

data class Keyframe(
  val id: String,
  val targetId: String,
  val property: KeyframeProperty,
  val timeMs: Long,
  val value: Float,
  val easing: EasingType = EasingType.Linear,
)

data class ExportSettings(
  val resolutionWidth: Int,
  val resolutionHeight: Int,
  val frameRate: Int,
  val videoBitrate: Long,
  val audioBitrate: Long,
  val format: ExportFormat,
  val codec: VideoCodec,
  val includeAudio: Boolean = true,
  val destinationUri: String? = null,
)

data class EditorUiState(
  val project: Project?,
  val currentTimeMs: Long,
  val isPlaying: Boolean,
  val selectedTool: EditorTool,
  val selectedClipId: String?,
  val selectedOverlayId: String?,
  val timelineState: TimelineUiState,
  val previewState: PreviewUiState,
  val panelState: PanelUiState,
  val exportSettings: ExportSettings,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
)

data class TimelineUiState(
  val tracks: List<Track>,
  val scrollOffsetPx: Float,
  val scalePxPerMs: Float,
  val gestureMode: TimelineGestureMode,
  val activeClipId: String?,
  val activeTrimHandle: TrimHandle?,
  val snapGuides: List<Long> = emptyList(),
  val boundaryFeedback: String? = null,
)

data class PreviewUiState(
  val overlays: List<Overlay>,
  val selectedOverlayId: String?,
  val canvasWidthPx: Float,
  val canvasHeightPx: Float,
  val showBoundaryGuide: Boolean,
  val previewSeekMs: Long,
  val isPreviewReady: Boolean,
)

data class PanelUiState(
  val activeTool: EditorTool,
  val isDirty: Boolean,
  val isPlaybackLocked: Boolean,
  val isExporting: Boolean,
  val message: String? = null,
)

enum class TrackType { Video, Audio, Text, Sticker, Effect, Overlay }
enum class OverlayType { Video, Image, Text, Sticker, Effect }
enum class EffectType { Blur, Glow, Shake, Zoom, Flash, Vhs, Glitch, Filter, Adjustment }
enum class TransitionType { Fade, Slide, Zoom, Blur }
enum class KeyframeProperty { PositionX, PositionY, Scale, Rotation, Opacity }
enum class EasingType { Linear }
enum class ExportFormat { Mp4, Mov }
enum class VideoCodec { H264, H265 }
enum class TextAlignment { Start, Center, End }
enum class EditorTool { Edit, Audio, Text, Sticker, Overlay, Filter, Effect, Transition, Canvas, Speed, Export }
enum class TimelineGestureMode { Idle, Scrolling, Flinging, DraggingClip, TrimmingClip, ScalingOverlay, RotatingOverlay, MovingOverlay, Playing }
enum class TrimHandle { Left, Right }

data class TextStyleRef(val id: String)
