package com.example.clipystudio.data

import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

data class RenderExportState(
  val status: RenderExportStatus = RenderExportStatus.IDLE,
  val phase: RenderExportPhase = RenderExportPhase.NONE,
  val options: ExportOptions? = null,
  val pipelineState: RenderPipelineState? = null,
  val progress: RenderProgress = RenderProgress(),
  val codecStrategy: CodecStrategy? = null,
  val diagnostics: RenderDiagnostics = RenderDiagnostics(),
  val tempWorkspace: TempRenderWorkspace? = null,
  val output: ExportOutput? = null,
  val error: RenderExportError? = null,
  val canCancel: Boolean = false,
  val canRetry: Boolean = false,
)

enum class RenderExportStatus { IDLE, PREPARING, RUNNING, CANCELLING, CANCELLED, COMPLETED, FAILED }

enum class RenderExportPhase {
  NONE,
  PREPARING_GRAPH,
  CREATING_TEMP_FILES,
  SELECTING_CODEC,
  RENDERING_VIDEO,
  MIXING_AUDIO,
  MUXING,
  SAVING_OUTPUT,
  SHARING_READY,
  CLEANING_UP,
}

data class RenderProgress(
  val renderedFrames: Long = 0,
  val totalFrames: Long = 0,
  val percent: Float = 0f,
  val currentTimeMs: Long = 0,
  val durationMs: Long = 0,
  val message: String? = null,
  val startedAtMs: Long = 0,
  val updatedAtMs: Long = 0,
)

data class CodecStrategy(
  val primary: CodecBackend,
  val fallback: CodecBackend?,
  val selected: CodecBackend,
  val videoMimeType: String,
  val audioMimeType: String,
  val requiresFallbackReason: String? = null,
)

enum class CodecBackend { MEDIA_CODEC, FFMPEG }

data class TempRenderWorkspace(
  val sessionId: String,
  val directoryPath: String,
  val videoTempPath: String? = null,
  val audioTempPath: String? = null,
  val muxedTempPath: String? = null,
  val finalOutputPath: String? = null,
  val createdAtMs: Long,
  val isCleaned: Boolean = false,
)

data class ExportOutput(
  val uri: String,
  val displayName: String,
  val mimeType: String,
  val durationMs: Long,
  val sizeBytes: Long,
  val width: Int,
  val height: Int,
  val fps: Int,
  val createdAtMs: Long,
)

data class RenderExportError(
  val type: RenderExportErrorType,
  val message: String,
  val recoverable: Boolean,
  val causeMessage: String? = null,
  val failedPhase: RenderExportPhase,
)

enum class RenderExportErrorType {
  VALIDATION,
  CODEC_UNAVAILABLE,
  DECODER_FAILURE,
  FRAME_RENDER_FAILURE,
  AUDIO_MIX_FAILURE,
  MUXER_FAILURE,
  STORAGE_FAILURE,
  CANCELLED,
  OUT_OF_MEMORY,
  UNKNOWN,
}

data class CanvasRenderConfig(
  val width: Int,
  val height: Int,
  val aspectRatio: Float,
  val backgroundColor: Long,
  val blurBackground: Boolean,
  val fitMode: CanvasFitMode,
  val colorSpace: String,
)

enum class CanvasFitMode { FIT, FILL, CROP }

data class RenderDiagnostics(
  val stages: List<RenderStageStatus> = emptyList(),
  val lastFramePlan: FrameCompositionPlan? = null,
  val audioMixPlan: AudioMixPlan? = null,
  val audioSync: AudioSyncReport? = null,
  val lastFailureCategory: RenderExportErrorType? = null,
)

data class RenderStageStatus(
  val label: String,
  val state: StageState,
  val detail: String? = null,
)

enum class StageState { PENDING, ACTIVE, COMPLETE, WARNING, FAILED, CANCELLED }

data class FrameCompositionPlan(
  val frame: ScheduledFrame,
  val canvas: CanvasRenderConfig,
  val mainLayer: RenderLayerNode?,
  val imageLayer: RenderLayerNode?,
  val transition: RenderTransitionFrame?,
  val stickers: List<RenderLayerNode>,
  val overlays: List<RenderLayerNode>,
  val texts: List<RenderTextNode>,
  val effects: List<RenderEffectFrame>,
  val filters: List<RenderFilterFrame>,
  val animatedProperties: List<InterpolatedProperty>,
)

data class RenderFilterFrame(
  val clipId: String,
  val filter: String,
  val adjustments: Map<String, Float>,
  val startTimeMs: Long,
  val endTimeMs: Long,
  val strength: Float,
)

data class RenderEffectFrame(
  val clipId: String,
  val effect: String,
  val startTimeMs: Long,
  val endTimeMs: Long,
  val progress: Float,
  val parameters: Map<String, Float>,
)

data class InterpolatedProperty(
  val clipId: String,
  val property: String,
  val value: Float,
  val timeMs: Long,
  val sourceKeyframeIds: List<String>,
)

data class AudioMixPlan(
  val durationMs: Long,
  val sampleRate: Int,
  val channelCount: Int,
  val tracks: List<AudioMixTrackPlan>,
  val outputTempPath: String,
)

data class AudioSyncReport(
  val videoDurationMs: Long,
  val mixedAudioDurationMs: Long,
  val driftMs: Long,
  val withinTolerance: Boolean,
)

data class AudioMixTrackPlan(
  val nodeId: String,
  val clipId: String,
  val mediaUri: String?,
  val timelineStartMs: Long,
  val timelineEndMs: Long,
  val sourceStartTimeMs: Long,
  val sourceEndTimeMs: Long,
  val volume: Float,
  val speed: Float,
  val fadeInMs: Long,
  val fadeOutMs: Long,
  val syncOffsetUs: Long,
)

data class ShareOutputEvent(
  val uri: String,
  val mimeType: String,
  val chooserTitle: String,
)

object RenderExportPlanner {
  fun canvasConfig(project: Project, encoder: EncoderConfig): CanvasRenderConfig = CanvasRenderConfig(
    width = encoder.width,
    height = encoder.height,
    aspectRatio = encoder.width.toFloat() / encoder.height.coerceAtLeast(1),
    backgroundColor = project.timeline.canvasBackground.color.removePrefix("#").toLongOrNull(16) ?: 0x09090BL,
    blurBackground = project.timeline.canvasBackground.blurEnabled,
    fitMode = when (project.canvasRatio) {
      CanvasRatio.Original -> CanvasFitMode.FIT
      CanvasRatio.Landscape -> CanvasFitMode.FILL
      else -> CanvasFitMode.CROP
    },
    colorSpace = "sRGB",
  )

  fun keyframeProperties(layer: RenderLayerNode, timelineTimeMs: Long): List<InterpolatedProperty> {
    val localTimeMs = (timelineTimeMs - layer.startTimeMs).coerceIn(0L, (layer.endTimeMs - layer.startTimeMs).coerceAtLeast(0L))
    return KeyframeProperty.entries.mapNotNull { property ->
      val frames = layer.keyframes.filter { it.property == property }.sortedBy { it.timeMs }
      if (frames.isEmpty()) return@mapNotNull null
      val before = frames.lastOrNull { it.timeMs <= localTimeMs } ?: frames.first()
      val after = frames.firstOrNull { it.timeMs >= localTimeMs } ?: frames.last()
      val value = if (before.timeMs == after.timeMs) before.value else {
        val span = (after.timeMs - before.timeMs).coerceAtLeast(1L)
        val progress = ((localTimeMs - before.timeMs).toFloat() / span).coerceIn(0f, 1f)
        before.value + (after.value - before.value) * progress
      }
      InterpolatedProperty(layer.clipId, property.name, value, timelineTimeMs, listOf(before.id, after.id).distinct())
    }
  }

  fun planFrameComposition(graph: RenderGraph, project: Project, frame: ScheduledFrame): FrameCompositionPlan {
    val framePlan = RenderPipelineEngine.planFrame(graph, frame)
    val active = graph.layers.filter { frame.timeMs in it.startTimeMs until it.endTimeMs }
    return FrameCompositionPlan(
      frame = frame,
      canvas = canvasConfig(project, graph.encoderConfig),
      mainLayer = framePlan.mainLayer,
      imageLayer = framePlan.imageLayer,
      transition = framePlan.transition,
      stickers = active.filter { it.type == RenderLayerType.STICKER }.sortedWith(compareBy<RenderLayerNode> { it.zIndex }.thenBy { it.trackIndex }),
      overlays = active.filter { it.type == RenderLayerType.OVERLAY }.sortedWith(compareBy<RenderLayerNode> { it.zIndex }.thenBy { it.trackIndex }),
      texts = framePlan.texts,
      effects = active.filter { it.type == RenderLayerType.EFFECT && !it.effect.isNullOrBlank() }.map { layer ->
        val span = (layer.endTimeMs - layer.startTimeMs).coerceAtLeast(1L)
        RenderEffectFrame(layer.clipId, layer.effect.orEmpty(), layer.startTimeMs, layer.endTimeMs, ((frame.timeMs - layer.startTimeMs).toFloat() / span).coerceIn(0f, 1f), mapOf("opacity" to layer.opacity))
      },
      filters = active.filter { !it.filter.isNullOrBlank() && it.type != RenderLayerType.EFFECT }.map { layer ->
        RenderFilterFrame(
          clipId = layer.clipId,
          filter = layer.filter.orEmpty(),
          adjustments = mapOf("opacity" to layer.opacity, "scale" to layer.transform.scale),
          startTimeMs = layer.startTimeMs,
          endTimeMs = layer.endTimeMs,
          strength = 1f,
        )
      },
      animatedProperties = active.flatMap { keyframeProperties(it, frame.timeMs) },
    )
  }

  fun buildAudioMixPlan(graph: RenderGraph, project: Project, outputTempPath: String): AudioMixPlan {
    val clipsById = project.timeline.tracks.flatMap { it.clips }.associateBy { it.id }
    val tracks = graph.audio.map { node ->
      val clip = clipsById[node.clipId]
      AudioMixTrackPlan(
        nodeId = node.id,
        clipId = node.clipId,
        mediaUri = node.mediaUri,
        timelineStartMs = node.startTimeMs,
        timelineEndMs = node.endTimeMs,
        sourceStartTimeMs = node.sourceStartTimeMs,
        sourceEndTimeMs = node.sourceEndTimeMs,
        volume = node.volume,
        speed = node.speed,
        fadeInMs = clip?.audioProperties?.fadeInMs ?: 0L,
        fadeOutMs = clip?.audioProperties?.fadeOutMs ?: 0L,
        syncOffsetUs = node.startTimeMs * 1_000L,
      )
    }
    return AudioMixPlan(graph.durationMs, 48_000, 2, tracks, outputTempPath)
  }

  fun audioSyncReport(plan: AudioMixPlan, videoDurationMs: Long, toleranceMs: Long = 120L): AudioSyncReport {
    val drift = abs(plan.durationMs - videoDurationMs)
    return AudioSyncReport(
      videoDurationMs = videoDurationMs,
      mixedAudioDurationMs = plan.durationMs,
      driftMs = drift,
      withinTolerance = drift <= toleranceMs,
    )
  }

  fun audioDriftMs(plan: AudioMixPlan, videoDurationMs: Long): Long = abs(plan.durationMs - videoDurationMs)

  fun stageDiagnostics(
    phase: RenderExportPhase,
    framePlan: FrameCompositionPlan? = null,
    audioPlan: AudioMixPlan? = null,
    audioSync: AudioSyncReport? = null,
    codecStrategy: CodecStrategy? = null,
    workspace: TempRenderWorkspace? = null,
    output: ExportOutput? = null,
    error: RenderExportError? = null,
  ): List<RenderStageStatus> {
    val errorPhase = error?.failedPhase
    return listOf(
      RenderStageStatus("Canvas", stageState(RenderExportPhase.RENDERING_VIDEO, phase, errorPhase), framePlan?.canvas?.let { "${it.width}x${it.height} ${it.fitMode.name.lowercase()} bg ${it.backgroundColor.toString(16)}" }),
      RenderStageStatus("Keyframes", stageState(RenderExportPhase.RENDERING_VIDEO, phase, errorPhase), framePlan?.animatedProperties?.takeIf { it.isNotEmpty() }?.let { "${it.size} animated values" } ?: "No animated properties"),
      RenderStageStatus("Stickers", stageState(RenderExportPhase.RENDERING_VIDEO, phase, errorPhase), framePlan?.stickers?.let { "${it.size} active layers" }),
      RenderStageStatus("Filters", stageState(RenderExportPhase.RENDERING_VIDEO, phase, errorPhase), framePlan?.filters?.let { "${it.size} active filters" }),
      RenderStageStatus("Effects", stageState(RenderExportPhase.RENDERING_VIDEO, phase, errorPhase), framePlan?.effects?.let { "${it.size} active effects" }),
      RenderStageStatus("Audio Mix", stageState(RenderExportPhase.MIXING_AUDIO, phase, errorPhase), audioPlan?.let { "${it.tracks.size} tracks @ ${it.sampleRate} Hz" }),
      RenderStageStatus("Audio Sync", stageState(RenderExportPhase.MIXING_AUDIO, phase, errorPhase), audioSync?.let { "drift ${it.driftMs} ms" }),
      RenderStageStatus("Codec", stageState(RenderExportPhase.SELECTING_CODEC, phase, errorPhase), codecStrategy?.let { "${it.selected.name.replace('_', ' ')}" }),
      RenderStageStatus("Temp Files", stageState(RenderExportPhase.CREATING_TEMP_FILES, phase, errorPhase), workspace?.let { if (it.isCleaned) "Cleaned" else "Active ${it.sessionId}" }),
      RenderStageStatus("Save", stageState(RenderExportPhase.SAVING_OUTPUT, phase, errorPhase), output?.displayName),
      RenderStageStatus("Share", stageState(RenderExportPhase.SHARING_READY, phase, errorPhase), output?.mimeType),
    )
  }

  private fun stageState(target: RenderExportPhase, current: RenderExportPhase, errorPhase: RenderExportPhase?): StageState {
    return when {
      errorPhase == target -> StageState.FAILED
      current == RenderExportPhase.CLEANING_UP && target == RenderExportPhase.CREATING_TEMP_FILES -> StageState.CANCELLED
      current == target -> StageState.ACTIVE
      current > target && current != RenderExportPhase.NONE -> StageState.COMPLETE
      else -> StageState.PENDING
    }
  }
}

object CodecStrategySelector {
  fun select(encoder: EncoderConfig): CodecStrategy {
    val requiresFallbackReason = when {
      encoder.format != ExportFormat.MP4 -> "Only MP4 is supported by the primary codec path."
      encoder.width >= 3840 || encoder.height >= 3840 -> "4K export uses the fallback path on this MVP pipeline."
      encoder.fps == 60 && encoder.width * encoder.height > 1920 * 1080 -> "High frame-rate large exports use the fallback path."
      else -> null
    }
    val selected = if (requiresFallbackReason == null) CodecBackend.MEDIA_CODEC else CodecBackend.FFMPEG
    return CodecStrategy(CodecBackend.MEDIA_CODEC, CodecBackend.FFMPEG, selected, encoder.videoMimeType, encoder.audioMimeType, requiresFallbackReason)
  }
}

interface TempFileManager {
  fun createWorkspace(projectId: String): TempRenderWorkspace
  fun createShareableOutput(workspace: TempRenderWorkspace, displayName: String): File
  fun cleanup(workspace: TempRenderWorkspace): TempRenderWorkspace
  fun cleanupStale(maxAgeMs: Long = 86_400_000L): Int
  fun availableStorageBytes(): Long
  fun owns(file: File): Boolean
}

class DefaultTempFileManager(private val root: File = File(System.getProperty("java.io.tmpdir"), "clipy-studio-exports")) : TempFileManager {
  override fun createWorkspace(projectId: String): TempRenderWorkspace {
    cleanupStale()
    root.mkdirs()
    val sessionId = "$projectId-${UUID.randomUUID()}"
    val directory = File(root, sessionId).apply { mkdirs() }
    val video = File(directory, "video.tmp")
    val audio = File(directory, "audio.tmp")
    val muxed = File(directory, "muxed.mp4")
    video.writeText("")
    audio.writeText("")
    muxed.writeText("")
    return TempRenderWorkspace(sessionId, directory.absolutePath, video.absolutePath, audio.absolutePath, muxed.absolutePath, null, System.currentTimeMillis(), false)
  }

  override fun createShareableOutput(workspace: TempRenderWorkspace, displayName: String): File {
    val rootPath = root.canonicalFile.toPath()
    val workspacePath = File(workspace.directoryPath).canonicalFile.toPath()
    require(workspacePath != rootPath && workspacePath.startsWith(rootPath)) { "Workspace must stay inside app-owned export cache." }
    val completedDirectory = File(root, "completed").apply { mkdirs() }
    val safeDisplayName = displayName.replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-', '.').ifBlank { "clipy-export.mp4" }
    val outputFile = File(completedDirectory, safeDisplayName)
    require(outputFile.canonicalFile.toPath().startsWith(rootPath)) { "Output must stay inside app-owned export cache." }
    return outputFile.apply { writeText("Clipy Studio export placeholder for ${workspace.sessionId}") }
  }

  override fun cleanup(workspace: TempRenderWorkspace): TempRenderWorkspace {
    val directory = File(workspace.directoryPath)
    val rootPath = root.canonicalFile.toPath()
    val directoryPath = directory.canonicalFile.toPath()
    if (directoryPath != rootPath && directoryPath.startsWith(rootPath)) {
      val finalOutputPath = workspace.finalOutputPath
      if (finalOutputPath == null) {
        directory.deleteRecursively()
      } else {
        File(finalOutputPath).canonicalFile.takeIf { it.toPath().startsWith(rootPath) }
        directory.deleteRecursively()
      }
    }
    return workspace.copy(isCleaned = true)
  }

  override fun cleanupStale(maxAgeMs: Long): Int {
    root.mkdirs()
    val now = System.currentTimeMillis()
    return root.listFiles()?.count { file ->
      val canonical = runCatching { file.canonicalFile }.getOrNull() ?: return@count false
      val isOwned = canonical.toPath().startsWith(root.canonicalFile.toPath())
      val isExpired = now - canonical.lastModified() > maxAgeMs
      when {
        !isOwned || !isExpired -> false
        file.isDirectory && file.name.contains('-') -> runCatching { canonical.deleteRecursively() }.getOrDefault(false)
        file.isDirectory && file.name == "completed" -> false
        else -> false
      }
    } ?: 0
  }

  override fun availableStorageBytes(): Long = root.usableSpace.coerceAtLeast(0L)

  override fun owns(file: File): Boolean = runCatching {
    file.canonicalFile.toPath().startsWith(root.canonicalFile.toPath())
  }.getOrDefault(false)
}

object RenderExportErrorClassifier {
  fun classify(throwable: Throwable, phase: RenderExportPhase): RenderExportError {
    val type = when {
      throwable is IllegalArgumentException -> RenderExportErrorType.VALIDATION
      throwable is OutOfMemoryError -> RenderExportErrorType.OUT_OF_MEMORY
      phase == RenderExportPhase.SELECTING_CODEC -> RenderExportErrorType.CODEC_UNAVAILABLE
      phase == RenderExportPhase.RENDERING_VIDEO -> RenderExportErrorType.FRAME_RENDER_FAILURE
      phase == RenderExportPhase.MIXING_AUDIO -> RenderExportErrorType.AUDIO_MIX_FAILURE
      phase == RenderExportPhase.MUXING -> RenderExportErrorType.MUXER_FAILURE
      phase == RenderExportPhase.SAVING_OUTPUT -> RenderExportErrorType.STORAGE_FAILURE
      else -> RenderExportErrorType.UNKNOWN
    }
    return RenderExportError(type, friendlyMessage(type), type != RenderExportErrorType.OUT_OF_MEMORY, throwable::class.simpleName, phase)
  }

  private fun friendlyMessage(type: RenderExportErrorType): String = when (type) {
    RenderExportErrorType.VALIDATION -> "Check your timeline and export settings, then try again."
    RenderExportErrorType.CODEC_UNAVAILABLE -> "This device cannot use the selected codec settings. Try 1080p or 30 FPS."
    RenderExportErrorType.DECODER_FAILURE -> "One media item could not be decoded. Replace it or choose another file."
    RenderExportErrorType.FRAME_RENDER_FAILURE -> "The preview frame could not be rendered. Retry after closing other apps."
    RenderExportErrorType.AUDIO_MIX_FAILURE -> "Audio mixing failed. Try muting or replacing the audio layer."
    RenderExportErrorType.MUXER_FAILURE -> "Video packaging failed. Retry export with MP4 settings."
    RenderExportErrorType.STORAGE_FAILURE -> "Clipy Studio could not save the export. Free space and retry."
    RenderExportErrorType.CANCELLED -> "Export cancelled. Temporary files were cleaned."
    RenderExportErrorType.OUT_OF_MEMORY -> "This export needs more memory. Try fewer layers or a lower resolution."
    RenderExportErrorType.UNKNOWN -> "Export failed safely. Your project was not changed."
  }
}
