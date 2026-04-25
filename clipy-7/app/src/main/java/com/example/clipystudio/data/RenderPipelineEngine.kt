package com.example.clipystudio.data

import kotlin.math.ceil
import kotlin.math.roundToLong

data class RenderInput(
  val timeline: ProjectTimeline,
  val durationMs: Long,
  val tracks: TimelineTracks,
  val videoClips: List<ProjectTimelineClip>,
  val imageClips: List<ProjectTimelineClip>,
  val audioClips: List<ProjectTimelineClip>,
  val textClips: List<ProjectTimelineClip>,
  val stickerClips: List<ProjectTimelineClip>,
  val overlayClips: List<ProjectTimelineClip>,
  val effectClips: List<ProjectTimelineClip>,
  val transitions: List<RenderTransitionInput>,
  val settings: ProjectRenderSettings,
  val timelineVersion: Long,
)

data class ProjectRenderSettings(
  val projectId: String,
  val width: Int,
  val height: Int,
  val fps: Int,
  val backgroundColor: Long,
  val pixelAspectRatio: Float = 1f,
  val colorSpace: String = "sRGB",
)

data class ExportOptions(
  val resolution: RenderExportResolution = RenderExportResolution.FULL_HD_1080P,
  val fps: Int = 30,
  val format: ExportFormat = ExportFormat.MP4,
  val quality: ExportQuality = ExportQuality.MEDIUM,
)

enum class RenderExportResolution { PROJECT, HD_720P, FULL_HD_1080P, UHD_4K, CUSTOM }
enum class ExportFormat { MP4 }
enum class ExportQuality { LOW, MEDIUM, HIGH }

data class EncoderConfig(
  val width: Int,
  val height: Int,
  val fps: Int,
  val format: ExportFormat,
  val videoMimeType: String,
  val videoBitrate: Int,
  val audioMimeType: String,
  val audioBitrate: Int,
  val keyFrameIntervalSeconds: Int,
  val quality: ExportQuality,
  val outputExtension: String,
)

data class RenderGraph(
  val durationMs: Long,
  val encoderConfig: EncoderConfig,
  val layers: List<RenderLayerNode>,
  val transitions: List<RenderTransitionNode>,
  val audio: List<RenderAudioNode>,
  val totalFrames: Long,
)

data class RenderLayerNode(
  val id: String,
  val clipId: String,
  val type: RenderLayerType,
  val trackType: TrackType,
  val trackIndex: Int,
  val zIndex: Int,
  val startTimeMs: Long,
  val endTimeMs: Long,
  val sourceStartTimeMs: Long,
  val sourceEndTimeMs: Long,
  val mediaUri: String?,
  val transform: TransformState,
  val keyframes: List<Keyframe>,
  val opacity: Float,
  val filter: String?,
  val effect: String?,
  val text: String? = null,
  val textStyleRef: String? = null,
  val animationRef: String? = null,
)

enum class RenderLayerType { MAIN_VIDEO, IMAGE, OVERLAY, STICKER, TEXT, EFFECT }

data class RenderTransitionInput(
  val transitionId: String,
  val fromClipId: String,
  val toClipId: String,
  val type: TransitionType,
  val startTimeMs: Long,
  val endTimeMs: Long,
  val durationMs: Long,
)

data class RenderTransitionNode(
  val id: String,
  val fromClipId: String,
  val toClipId: String,
  val type: TransitionType,
  val startTimeMs: Long,
  val endTimeMs: Long,
  val durationMs: Long,
  val zIndex: Int,
)

data class RenderAudioNode(
  val id: String,
  val clipId: String,
  val mediaUri: String?,
  val startTimeMs: Long,
  val endTimeMs: Long,
  val sourceStartTimeMs: Long,
  val sourceEndTimeMs: Long,
  val volume: Float,
  val speed: Float,
  val trackIndex: Int,
)

data class ScheduledFrame(
  val index: Long,
  val timeMs: Long,
  val presentationTimeUs: Long,
  val durationMs: Long,
  val isLastFrame: Boolean,
)

data class FrameRenderPlan(
  val frame: ScheduledFrame,
  val mainLayer: RenderLayerNode?,
  val imageLayer: RenderLayerNode?,
  val transition: RenderTransitionFrame?,
  val overlays: List<RenderLayerNode>,
  val texts: List<RenderTextNode>,
  val effects: List<RenderLayerNode>,
)

data class RenderTransitionFrame(
  val transitionId: String,
  val fromClipId: String,
  val toClipId: String,
  val type: TransitionType,
  val progress: Float,
  val startTimeMs: Long,
  val endTimeMs: Long,
)

data class RenderTextNode(
  val id: String,
  val clipId: String,
  val text: String,
  val startTimeMs: Long,
  val endTimeMs: Long,
  val transform: TransformState,
  val opacity: Float,
  val styleRef: String?,
  val animationRef: String?,
)

data class RenderPipelineState(
  val status: RenderPipelineStatus = RenderPipelineStatus.IDLE,
  val options: ExportOptions? = null,
  val encoderConfig: EncoderConfig? = null,
  val graph: RenderGraph? = null,
  val totalFrames: Long = 0,
  val errorMessage: String? = null,
)

enum class RenderPipelineStatus { IDLE, PREPARING, READY, ERROR }

object ExportSettingsMapper {
  fun toExportOptions(settings: ExportSettings): ExportOptions? {
    val format = if (settings.format.equals("MP4", ignoreCase = true)) ExportFormat.MP4 else return null
    val resolution = when (settings.resolution) {
      ExportResolution.P720 -> RenderExportResolution.HD_720P
      ExportResolution.P1080 -> RenderExportResolution.FULL_HD_1080P
      ExportResolution.P2K -> RenderExportResolution.CUSTOM
      ExportResolution.P4K -> RenderExportResolution.UHD_4K
    }
    val quality = when (settings.qualityPreset) {
      QualityPreset.Balanced -> ExportQuality.MEDIUM
      QualityPreset.High, QualityPreset.Studio -> ExportQuality.HIGH
    }
    return ExportOptions(resolution, settings.fps, format, quality)
  }

  fun map(options: ExportOptions, projectSettings: ProjectRenderSettings, durationMs: Long): Result<EncoderConfig> {
    if (durationMs <= 0L) return Result.failure(IllegalArgumentException("Timeline duration must be greater than zero."))
    if (options.fps !in setOf(24, 30, 60)) return Result.failure(IllegalArgumentException("Unsupported FPS: ${options.fps}."))
    if (projectSettings.width <= 0 || projectSettings.height <= 0) return Result.failure(IllegalArgumentException("Project resolution is invalid."))

    val (width, height) = when (options.resolution) {
      RenderExportResolution.PROJECT -> projectSettings.width to projectSettings.height
      RenderExportResolution.HD_720P -> scaledSize(projectSettings.width, projectSettings.height, 720)
      RenderExportResolution.FULL_HD_1080P -> scaledSize(projectSettings.width, projectSettings.height, 1080)
      RenderExportResolution.UHD_4K -> scaledSize(projectSettings.width, projectSettings.height, 2160)
      RenderExportResolution.CUSTOM -> scaledSize(projectSettings.width, projectSettings.height, 1440)
    }
    if (width <= 0 || height <= 0) return Result.failure(IllegalArgumentException("Resolved export resolution is invalid."))

    val bitratePerPixel = when (options.quality) {
      ExportQuality.LOW -> 0.06f
      ExportQuality.MEDIUM -> 0.1f
      ExportQuality.HIGH -> 0.16f
    }
    val videoBitrate = (width * height * options.fps * bitratePerPixel).toInt().coerceAtLeast(2_000_000)
    return Result.success(
      EncoderConfig(
        width = width,
        height = height,
        fps = options.fps,
        format = options.format,
        videoMimeType = "video/avc",
        videoBitrate = videoBitrate,
        audioMimeType = "audio/mp4a-latm",
        audioBitrate = if (options.quality == ExportQuality.LOW) 96_000 else 192_000,
        keyFrameIntervalSeconds = 2,
        quality = options.quality,
        outputExtension = "mp4",
      ),
    )
  }

  private fun scaledSize(projectWidth: Int, projectHeight: Int, longEdge: Int): Pair<Int, Int> {
    val widthIsLong = projectWidth >= projectHeight
    val ratio = if (widthIsLong) projectHeight.toFloat() / projectWidth else projectWidth.toFloat() / projectHeight
    val rawWidth = if (widthIsLong) longEdge else (longEdge * ratio).roundToLong().toInt()
    val rawHeight = if (widthIsLong) (longEdge * ratio).roundToLong().toInt() else longEdge
    return rawWidth.evenAtLeastTwo() to rawHeight.evenAtLeastTwo()
  }

  private fun Int.evenAtLeastTwo(): Int = coerceAtLeast(2).let { if (it % 2 == 0) it else it - 1 }.coerceAtLeast(2)
}

object FrameScheduler {
  fun totalFrames(durationMs: Long, fps: Int): Long = ceil(durationMs.coerceAtLeast(0L) * fps / 1_000.0).toLong().coerceAtLeast(if (durationMs > 0L) 1L else 0L)

  fun schedule(durationMs: Long, fps: Int): List<ScheduledFrame> {
    val total = totalFrames(durationMs, fps)
    if (total == 0L) return emptyList()
    val frameDuration = (1_000.0 / fps).roundToLong().coerceAtLeast(1L)
    return List(total.toInt()) { index ->
      val frameIndex = index.toLong()
      val timeMs = (frameIndex * 1_000L / fps).coerceAtMost((durationMs - 1).coerceAtLeast(0L))
      ScheduledFrame(frameIndex, timeMs, timeMs * 1_000L, frameDuration, frameIndex == total - 1)
    }
  }
}

object RenderPipelineEngine {
  fun collectInput(timeline: Timeline, project: Project? = null): RenderInput {
    val projectTimeline = TimelineEngine.toProjectTimeline(timeline)
    val transitions = timeline.transitions.mapNotNull { transition ->
      TimelineEngine.transitionWindow(timeline, transition)?.let { window ->
        RenderTransitionInput(transition.id, transition.fromClipId, transition.toClipId, transition.type, window.first, window.last, (window.last - window.first).coerceAtLeast(1L))
      }
    }
    val ratio = project?.canvasRatio ?: CanvasRatio.Portrait
    return RenderInput(
      timeline = projectTimeline,
      durationMs = projectTimeline.durationMs,
      tracks = projectTimeline.tracks,
      videoClips = projectTimeline.tracks.video.filter { it.type == ClipType.Video },
      imageClips = projectTimeline.tracks.video.filter { it.type == ClipType.Image },
      audioClips = projectTimeline.tracks.audio,
      textClips = projectTimeline.tracks.text,
      stickerClips = projectTimeline.tracks.sticker,
      overlayClips = projectTimeline.tracks.overlay,
      effectClips = projectTimeline.tracks.effect,
      transitions = transitions,
      settings = ProjectRenderSettings(projectId = project?.id ?: projectTimeline.id, width = ratio.width(), height = ratio.height(), fps = 30, backgroundColor = timeline.canvasBackground.color.parseColorLong()),
      timelineVersion = projectTimeline.version,
    )
  }

  fun prepare(timeline: Timeline, project: Project?, options: ExportOptions): Result<RenderGraph> {
    val input = collectInput(timeline, project)
    val encoderConfig = ExportSettingsMapper.map(options, input.settings, input.durationMs).getOrElse { return Result.failure(it) }
    return buildRenderGraph(input, encoderConfig)
  }

  fun buildRenderGraph(input: RenderInput, encoderConfig: EncoderConfig): Result<RenderGraph> {
    if (input.durationMs <= 0L) return Result.failure(IllegalArgumentException("Timeline duration must be greater than zero."))
    if ((input.videoClips + input.imageClips + input.overlayClips + input.textClips + input.stickerClips + input.effectClips).isEmpty()) {
      return Result.failure(IllegalArgumentException("Timeline has no renderable visual layers."))
    }
    val layers = buildList {
      addAll(input.videoClips.map { it.toLayer(RenderLayerType.MAIN_VIDEO, 0) })
      addAll(input.imageClips.map { it.toLayer(RenderLayerType.IMAGE, 1) })
      addAll(input.effectClips.map { it.toLayer(RenderLayerType.EFFECT, 20) })
      addAll(input.overlayClips.map { it.toLayer(RenderLayerType.OVERLAY, 30) })
      addAll(input.stickerClips.map { it.toLayer(RenderLayerType.STICKER, 40) })
      addAll(input.textClips.map { it.toLayer(RenderLayerType.TEXT, 50) })
    }.sortedWith(compareBy<RenderLayerNode> { it.zIndex }.thenBy { it.trackIndex }.thenBy { it.startTimeMs }.thenBy { it.clipId })
    val transitions = input.transitions.mapIndexed { index, transition ->
      RenderTransitionNode(transition.transitionId, transition.fromClipId, transition.toClipId, transition.type, transition.startTimeMs, transition.endTimeMs, transition.durationMs, 10 + index)
    }.sortedWith(compareBy<RenderTransitionNode> { it.startTimeMs }.thenBy { it.id })
    val audio = input.audioClips.map { clip ->
      RenderAudioNode("audio-${clip.id}", clip.id, clip.mediaUri, clip.startTimeMs, clip.startTimeMs + clip.durationMs, clip.trimStartMs, clip.trimStartMs + ((clip.durationMs - clip.trimEndMs).coerceAtLeast(0L) * clip.speed.coerceAtLeast(0.1f)).roundToLong(), clip.volume, clip.speed, clip.trackIndex)
    }.sortedWith(compareBy<RenderAudioNode> { it.trackIndex }.thenBy { it.startTimeMs }.thenBy { it.clipId })
    return Result.success(RenderGraph(input.durationMs, encoderConfig, layers, transitions, audio, FrameScheduler.totalFrames(input.durationMs, encoderConfig.fps)))
  }

  fun scheduledFrame(graph: RenderGraph, index: Long): ScheduledFrame? = FrameScheduler.schedule(graph.durationMs, graph.encoderConfig.fps).getOrNull(index.toInt())

  fun planFrame(graph: RenderGraph, frame: ScheduledFrame): FrameRenderPlan {
    val active = graph.layers.filter { frame.timeMs >= it.startTimeMs && frame.timeMs < it.endTimeMs }
    val main = active.lastOrNull { it.type == RenderLayerType.MAIN_VIDEO }
    val image = active.lastOrNull { it.type == RenderLayerType.IMAGE }
    return FrameRenderPlan(
      frame = frame,
      mainLayer = main,
      imageLayer = image,
      transition = graph.transitions.firstOrNull { frame.timeMs >= it.startTimeMs && frame.timeMs <= it.endTimeMs }?.toFrame(frame.timeMs),
      overlays = active.filter { it.type == RenderLayerType.OVERLAY || it.type == RenderLayerType.STICKER },
      texts = active.filter { it.type == RenderLayerType.TEXT }.map { RenderTextNode(it.id, it.clipId, it.text ?: "Clipy Studio", it.startTimeMs, it.endTimeMs, it.transform, it.opacity, it.textStyleRef, it.animationRef) },
      effects = active.filter { it.type == RenderLayerType.EFFECT },
    )
  }

  fun sourceTimeFor(layer: RenderLayerNode, timelineTimeMs: Long): Long {
    val local = (timelineTimeMs - layer.startTimeMs).coerceIn(0L, (layer.endTimeMs - layer.startTimeMs).coerceAtLeast(0L))
    return (layer.sourceStartTimeMs + (local * speedFor(layer)).roundToLong()).coerceIn(layer.sourceStartTimeMs, layer.sourceEndTimeMs.coerceAtLeast(layer.sourceStartTimeMs))
  }

  private fun speedFor(layer: RenderLayerNode): Float {
    val visible = (layer.endTimeMs - layer.startTimeMs).coerceAtLeast(1L)
    val source = (layer.sourceEndTimeMs - layer.sourceStartTimeMs).coerceAtLeast(1L)
    return source.toFloat() / visible
  }

  private fun ProjectTimelineClip.toLayer(type: RenderLayerType, zBase: Int): RenderLayerNode {
    val sourceEnd = trimStartMs + ((durationMs - trimEndMs).coerceAtLeast(0L) * speed.coerceAtLeast(0.1f)).roundToLong()
    return RenderLayerNode(
      id = "${type.name.lowercase()}-$id",
      clipId = id,
      type = type,
      trackType = trackType,
      trackIndex = trackIndex,
      zIndex = zBase + trackIndex,
      startTimeMs = startTimeMs,
      endTimeMs = startTimeMs + durationMs,
      sourceStartTimeMs = trimStartMs,
      sourceEndTimeMs = sourceEnd,
      mediaUri = mediaUri,
      transform = transform,
      keyframes = keyframes,
      opacity = transform.opacity,
      filter = filter,
      effect = effect,
      text = text,
      textStyleRef = textStyleRef,
      animationRef = animationRef,
    )
  }

  private fun RenderTransitionNode.toFrame(timeMs: Long): RenderTransitionFrame {
    val span = (endTimeMs - startTimeMs).coerceAtLeast(1L)
    return RenderTransitionFrame(id, fromClipId, toClipId, type, ((timeMs - startTimeMs).toFloat() / span).coerceIn(0f, 1f), startTimeMs, endTimeMs)
  }

  private fun CanvasRatio.width(): Int = when (this) {
    CanvasRatio.Portrait -> 1080
    CanvasRatio.Square -> 1080
    CanvasRatio.Landscape -> 1920
    CanvasRatio.FourFive -> 1080
    CanvasRatio.Original -> 1080
  }

  private fun CanvasRatio.height(): Int = when (this) {
    CanvasRatio.Portrait -> 1920
    CanvasRatio.Square -> 1080
    CanvasRatio.Landscape -> 1080
    CanvasRatio.FourFive -> 1350
    CanvasRatio.Original -> 1920
  }

  private fun String.parseColorLong(): Long = removePrefix("#").toLongOrNull(16) ?: 0x09090BL
}
