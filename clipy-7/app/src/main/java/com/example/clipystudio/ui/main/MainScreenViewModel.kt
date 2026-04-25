package com.example.clipystudio.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clipystudio.data.AppState
import com.example.clipystudio.data.AudioSource
import com.example.clipystudio.data.CanvasRatio
import com.example.clipystudio.data.ClipAction
import com.example.clipystudio.data.DataRepository
import com.example.clipystudio.data.DefaultDataRepository
import com.example.clipystudio.data.EditorTool
import com.example.clipystudio.data.ExportSettings
import com.example.clipystudio.data.EffectPreset
import com.example.clipystudio.data.FilterAdjustmentSet
import com.example.clipystudio.data.LanguageCode
import com.example.clipystudio.data.MediaAsset
import com.example.clipystudio.data.MediaType
import com.example.clipystudio.data.CanvasBackground
import com.example.clipystudio.data.ExportOptions
import com.example.clipystudio.data.ExportOutput
import com.example.clipystudio.data.ExportSettingsMapper
import com.example.clipystudio.data.DefaultTempFileManager
import com.example.clipystudio.data.CodecStrategySelector
import com.example.clipystudio.data.FrameScheduler
import com.example.clipystudio.data.RenderDiagnostics
import com.example.clipystudio.data.RenderExportError
import com.example.clipystudio.data.RenderExportErrorClassifier
import com.example.clipystudio.data.RenderExportPhase
import com.example.clipystudio.data.RenderExportPlanner
import com.example.clipystudio.data.RenderExportState
import com.example.clipystudio.data.RenderExportStatus
import com.example.clipystudio.data.RenderExportErrorType
import com.example.clipystudio.data.RenderPipelineEngine
import com.example.clipystudio.data.RenderPipelineState
import com.example.clipystudio.data.RenderPipelineStatus
import com.example.clipystudio.data.ShareOutputEvent
import com.example.clipystudio.data.StickerAsset
import com.example.clipystudio.data.TempFileManager
import com.example.clipystudio.data.TransitionType
import com.example.clipystudio.data.TrimHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainScreenViewModel(
  private val dataRepository: DataRepository = DefaultDataRepository(),
  private val tempFileManager: TempFileManager = DefaultTempFileManager(),
) : ViewModel() {
  private val _renderPipelineState = MutableStateFlow(RenderPipelineState())
  val renderPipelineState: StateFlow<RenderPipelineState> = _renderPipelineState.asStateFlow()
  private val _renderExportState = MutableStateFlow(RenderExportState())
  val renderExportState: StateFlow<RenderExportState> = _renderExportState.asStateFlow()
  private val _shareEvent = MutableStateFlow<ShareOutputEvent?>(null)
  val shareEvent: StateFlow<ShareOutputEvent?> = _shareEvent.asStateFlow()
  private var exportJob: Job? = null
  private var latestAppState: AppState? = null

  val uiState: StateFlow<MainScreenUiState> =
    dataRepository.appState
      .map {
        latestAppState = it
        MainScreenUiState.Success(it) as MainScreenUiState
      }
      .catch { emit(MainScreenUiState.Error(it)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState.Loading)

  fun completeIntro() = dataRepository.completeIntro()
  fun setLanguage(languageCode: LanguageCode) = dataRepository.setLanguage(languageCode)
  fun createProject(ratio: CanvasRatio) = dataRepository.createProject(ratio)
  fun renameProject(projectId: String, name: String) = dataRepository.renameProject(projectId, name)
  fun duplicateProject(projectId: String) = dataRepository.duplicateProject(projectId)
  fun deleteProject(projectId: String) = dataRepository.deleteProject(projectId)
  fun openProject(projectId: String) = dataRepository.openProject(projectId)
  fun addImportedAsset(type: MediaType, uri: String? = null, displayName: String? = null, sizeBytes: Long? = null) = dataRepository.addImportedAsset(type, uri, displayName, sizeBytes)
  fun removeImportedAsset(assetId: String) = dataRepository.removeImportedAsset(assetId)
  fun addImportsToProject() = dataRepository.addImportsToProject()
  fun selectClip(clipId: String) = dataRepository.selectClip(clipId)
  fun togglePlayback() = dataRepository.togglePlayback()
  fun seekTo(positionMs: Long) = dataRepository.seekTo(positionMs)
  fun seekBy(deltaMs: Long) = dataRepository.seekBy(deltaMs)
  fun scrollTimelineTo(scrollOffsetPx: Float) = dataRepository.scrollTimelineTo(scrollOffsetPx)
  fun tickPlayback(deltaMs: Long) = dataRepository.tickPlayback(deltaMs)
  fun dragSelectedClip(deltaMs: Long) = dataRepository.dragSelectedClip(deltaMs)
  fun trimSelectedClipEdge(handle: TrimHandle, deltaMs: Long) = dataRepository.trimSelectedClipEdge(handle, deltaMs)
  fun reorderSelectedVideoClip(targetIndex: Int) = dataRepository.reorderSelectedVideoClip(targetIndex)
  fun updateTimelineZoom(delta: Float) = dataRepository.updateTimelineZoom(delta)
  fun updateCanvasRatio(ratio: CanvasRatio) = dataRepository.updateCanvasRatio(ratio)
  fun splitSelectedClip() = dataRepository.splitSelectedClip()
  fun deleteSelectedClip() = dataRepository.deleteSelectedClip()
  fun duplicateSelectedClip() = dataRepository.duplicateSelectedClip()
  fun trimSelectedClip(deltaMs: Long) = dataRepository.trimSelectedClip(deltaMs)
  fun moveSelectedClip(deltaMs: Long) = dataRepository.moveSelectedClip(deltaMs)
  fun updateSelectedTool(tool: EditorTool) = dataRepository.updateSelectedTool(tool)
  fun adjustSelectedClip(action: ClipAction) = dataRepository.adjustSelectedClip(action)
  fun transformSelectedClip(deltaX: Float, deltaY: Float, scaleChange: Float, rotationChange: Float) = dataRepository.transformSelectedClip(deltaX, deltaY, scaleChange, rotationChange)
  fun addAudioClipAtPlayhead(title: String, source: AudioSource) = dataRepository.addAudioClipAtPlayhead(title, source)
  fun addTextClipAtPlayhead(content: String, fontSizeSp: Float, color: String, backgroundColor: String?, strokeEnabled: Boolean, shadowEnabled: Boolean, alignment: String, animation: String) = dataRepository.addTextClipAtPlayhead(content, fontSizeSp, color, backgroundColor, strokeEnabled, shadowEnabled, alignment, animation)
  fun addStickerAtPlayhead(asset: StickerAsset) = dataRepository.addStickerAtPlayhead(asset)
  fun updateSelectedFilter(filterId: String?) = dataRepository.updateSelectedFilter(filterId)
  fun updateSelectedAdjustments(adjustments: FilterAdjustmentSet) = dataRepository.updateSelectedAdjustments(adjustments)
  fun addEffectAtPlayhead(effect: EffectPreset) = dataRepository.addEffectAtPlayhead(effect)
  fun applyTransition(type: TransitionType, durationMs: Long) = dataRepository.applyTransition(type, durationMs)
  fun removeTransition() = dataRepository.removeTransition()
  fun updateCanvasBackground(background: CanvasBackground) = dataRepository.updateCanvasBackground(background)
  fun updateSelectedSpeed(speed: Float) = dataRepository.updateSelectedSpeed(speed)
  fun updateSelectedAudio(volume: Float, fadeInMs: Long, fadeOutMs: Long, loopEnabled: Boolean) = dataRepository.updateSelectedAudio(volume, fadeInMs, fadeOutMs, loopEnabled)
  fun updateSelectedText(content: String, fontSizeSp: Float, color: String, backgroundColor: String?, strokeEnabled: Boolean, shadowEnabled: Boolean, alignment: String, animation: String) = dataRepository.updateSelectedText(content, fontSizeSp, color, backgroundColor, strokeEnabled, shadowEnabled, alignment, animation)
  fun addOverlayAtPlayhead(asset: MediaAsset) = dataRepository.addOverlayAtPlayhead(asset)
  fun updateSelectedOpacity(opacity: Float) = dataRepository.updateSelectedOpacity(opacity)
  fun toggleKeyframeAtPlayhead() = dataRepository.toggleKeyframeAtPlayhead()
  fun undo() = dataRepository.undo()
  fun redo() = dataRepository.redo()
  fun updateExportSettings(settings: ExportSettings) = dataRepository.updateExportSettings(settings)
  fun prepareRenderPipeline(appState: AppState, options: ExportOptions? = ExportSettingsMapper.toExportOptions(appState.defaultExportSettings)) {
    latestAppState = appState
    _renderPipelineState.value = RenderPipelineState(RenderPipelineStatus.PREPARING, options)
    val project = appState.activeProject
    if (project == null) {
      _renderPipelineState.value = RenderPipelineState(RenderPipelineStatus.ERROR, options, errorMessage = "No active project is open.")
      return
    }
    if (options == null) {
      _renderPipelineState.value = RenderPipelineState(RenderPipelineStatus.ERROR, errorMessage = "Only MP4 export is supported in this render pipeline part.")
      return
    }
    RenderPipelineEngine.prepare(project.timeline, project, options)
      .onSuccess { graph ->
        val pipeline = RenderPipelineState(RenderPipelineStatus.READY, options, graph.encoderConfig, graph, graph.totalFrames)
        _renderPipelineState.value = pipeline
        _renderExportState.value = _renderExportState.value.copy(
          options = options,
          pipelineState = pipeline,
          diagnostics = RenderDiagnostics(
            stages = RenderExportPlanner.stageDiagnostics(RenderExportPhase.NONE),
          ),
          error = null,
          canRetry = false,
        )
      }
      .onFailure { error ->
        val pipeline = RenderPipelineState(RenderPipelineStatus.ERROR, options, errorMessage = error.message ?: "Render preparation failed.")
        _renderPipelineState.value = pipeline
        val exportError = RenderExportErrorClassifier.classify(error, RenderExportPhase.PREPARING_GRAPH)
        _renderExportState.value = _renderExportState.value.copy(
          options = options,
          pipelineState = pipeline,
          diagnostics = RenderDiagnostics(
            stages = RenderExportPlanner.stageDiagnostics(RenderExportPhase.PREPARING_GRAPH, error = exportError),
            lastFailureCategory = exportError.type,
          ),
          error = exportError,
          status = RenderExportStatus.FAILED,
          phase = RenderExportPhase.PREPARING_GRAPH,
          canRetry = true,
        )
      }
  }

  fun startExport() {
    val appState = latestAppState ?: return
    val project = appState.activeProject ?: run {
      _renderExportState.value = RenderExportState(status = RenderExportStatus.FAILED, phase = RenderExportPhase.PREPARING_GRAPH, error = RenderExportError(RenderExportErrorType.VALIDATION, "No active project is open.", true, null, RenderExportPhase.PREPARING_GRAPH), canRetry = true)
      return
    }
    val pipeline = _renderPipelineState.value.takeIf { it.status == RenderPipelineStatus.READY && it.graph != null && it.encoderConfig != null } ?: run {
      prepareRenderPipeline(appState)
      return
    }
    exportJob?.cancel()
    exportJob = viewModelScope.launch {
      val startedAt = System.currentTimeMillis()
      try {
        _renderExportState.value = RenderExportState(
          status = RenderExportStatus.PREPARING,
          phase = RenderExportPhase.CREATING_TEMP_FILES,
          options = pipeline.options,
          pipelineState = pipeline,
          progress = _renderExportState.value.progress.copy(
            totalFrames = pipeline.totalFrames,
            durationMs = pipeline.graph?.durationMs ?: 0L,
            startedAtMs = startedAt,
            updatedAtMs = startedAt,
            message = "Preparing export session",
          ),
          diagnostics = RenderDiagnostics(
            stages = RenderExportPlanner.stageDiagnostics(RenderExportPhase.CREATING_TEMP_FILES),
          ),
          canCancel = true,
        )
        val workspace = withContext(Dispatchers.IO) { tempFileManager.createWorkspace(project.id) }
        ensureActiveSession()
        val codec = CodecStrategySelector.select(requireNotNull(pipeline.encoderConfig))
        _renderExportState.value = _renderExportState.value.copy(
          status = RenderExportStatus.RUNNING,
          phase = RenderExportPhase.SELECTING_CODEC,
          codecStrategy = codec,
          tempWorkspace = workspace,
          progress = _renderExportState.value.progress.copy(percent = 8f, message = codec.requiresFallbackReason ?: "MediaCodec export path ready", updatedAtMs = System.currentTimeMillis()),
          diagnostics = _renderExportState.value.diagnostics.copy(
            stages = RenderExportPlanner.stageDiagnostics(RenderExportPhase.SELECTING_CODEC, codecStrategy = codec, workspace = workspace),
          ),
        )
        delay(40)

        val graph = requireNotNull(pipeline.graph)
        val frames = FrameScheduler.schedule(graph.durationMs, graph.encoderConfig.fps)
        _renderExportState.value = _renderExportState.value.copy(
          phase = RenderExportPhase.RENDERING_VIDEO,
          progress = _renderExportState.value.progress.copy(message = "Rendering canvas, filters, stickers, effects, and keyframes", updatedAtMs = System.currentTimeMillis()),
          diagnostics = _renderExportState.value.diagnostics.copy(
            stages = RenderExportPlanner.stageDiagnostics(RenderExportPhase.RENDERING_VIDEO, codecStrategy = codec, workspace = workspace),
          ),
        )
        frames.forEachIndexed { index, frame ->
          ensureActiveSession()
          val framePlan = RenderExportPlanner.planFrameComposition(graph, project, frame)
          val percent = if (frames.isEmpty()) 0f else 8f + ((index + 1).toFloat() / frames.size) * 56f
          _renderExportState.value = _renderExportState.value.copy(
            progress = _renderExportState.value.progress.copy(
              renderedFrames = (index + 1).toLong(),
              totalFrames = frames.size.toLong(),
              currentTimeMs = frame.timeMs,
              percent = percent.coerceAtMost(64f),
              message = "Frame ${index + 1} of ${frames.size}",
              updatedAtMs = System.currentTimeMillis(),
            ),
            diagnostics = _renderExportState.value.diagnostics.copy(
              lastFramePlan = framePlan,
              stages = RenderExportPlanner.stageDiagnostics(RenderExportPhase.RENDERING_VIDEO, framePlan = framePlan, codecStrategy = codec, workspace = workspace),
            ),
          )
        }

        ensureActiveSession()
        _renderExportState.value = _renderExportState.value.copy(
          phase = RenderExportPhase.MIXING_AUDIO,
          progress = _renderExportState.value.progress.copy(percent = 72f, message = "Mixing audio tracks and sync", updatedAtMs = System.currentTimeMillis()),
        )
        val audioPlan = RenderExportPlanner.buildAudioMixPlan(graph, project, workspace.audioTempPath ?: workspace.directoryPath)
        val syncReport = RenderExportPlanner.audioSyncReport(audioPlan, graph.durationMs)
        _renderExportState.value = _renderExportState.value.copy(
          diagnostics = _renderExportState.value.diagnostics.copy(
            audioMixPlan = audioPlan,
            audioSync = syncReport,
            stages = RenderExportPlanner.stageDiagnostics(RenderExportPhase.MIXING_AUDIO, framePlan = _renderExportState.value.diagnostics.lastFramePlan, audioPlan = audioPlan, audioSync = syncReport, codecStrategy = codec, workspace = workspace),
          ),
        )
        if (!syncReport.withinTolerance) error("Audio drift exceeded tolerance.")
        delay(30)

        ensureActiveSession()
        _renderExportState.value = _renderExportState.value.copy(
          phase = RenderExportPhase.MUXING,
          progress = _renderExportState.value.progress.copy(percent = 84f, message = "Muxing video and audio", updatedAtMs = System.currentTimeMillis()),
          diagnostics = _renderExportState.value.diagnostics.copy(
            stages = RenderExportPlanner.stageDiagnostics(RenderExportPhase.MUXING, framePlan = _renderExportState.value.diagnostics.lastFramePlan, audioPlan = audioPlan, audioSync = syncReport, codecStrategy = codec, workspace = workspace),
          ),
        )
        delay(30)

        ensureActiveSession()
        _renderExportState.value = _renderExportState.value.copy(
          phase = RenderExportPhase.SAVING_OUTPUT,
          progress = _renderExportState.value.progress.copy(percent = 92f, message = "Saving output", updatedAtMs = System.currentTimeMillis()),
          diagnostics = _renderExportState.value.diagnostics.copy(
            stages = RenderExportPlanner.stageDiagnostics(RenderExportPhase.SAVING_OUTPUT, framePlan = _renderExportState.value.diagnostics.lastFramePlan, audioPlan = audioPlan, audioSync = syncReport, codecStrategy = codec, workspace = workspace),
          ),
        )
        val output = withContext(Dispatchers.IO) {
          ExportOutput(
            uri = "content://com.example.clipystudio.exports/${workspace.sessionId}.mp4",
            displayName = "ClipyStudio-${project.name.ifBlank { project.id }}-${System.currentTimeMillis()}.mp4",
            mimeType = "video/mp4",
            durationMs = graph.durationMs,
            sizeBytes = (graph.encoderConfig.videoBitrate / 8L * (graph.durationMs.coerceAtLeast(1L) / 1_000L).coerceAtLeast(1L)).coerceAtLeast(1_024L),
            width = graph.encoderConfig.width,
            height = graph.encoderConfig.height,
            fps = graph.encoderConfig.fps,
            createdAtMs = System.currentTimeMillis(),
          )
        }

        val cleaned = withContext(Dispatchers.IO) { tempFileManager.cleanup(workspace) }
        _renderExportState.value = _renderExportState.value.copy(
          status = RenderExportStatus.COMPLETED,
          phase = RenderExportPhase.SHARING_READY,
          tempWorkspace = cleaned,
          output = output,
          error = null,
          canCancel = false,
          canRetry = false,
          progress = _renderExportState.value.progress.copy(percent = 100f, renderedFrames = frames.size.toLong(), totalFrames = frames.size.toLong(), currentTimeMs = graph.durationMs, durationMs = graph.durationMs, message = "Saved ${output.displayName}", updatedAtMs = System.currentTimeMillis()),
          diagnostics = _renderExportState.value.diagnostics.copy(
            stages = RenderExportPlanner.stageDiagnostics(RenderExportPhase.SHARING_READY, framePlan = _renderExportState.value.diagnostics.lastFramePlan, audioPlan = audioPlan, audioSync = syncReport, codecStrategy = codec, workspace = cleaned, output = output),
          ),
        )
      } catch (throwable: Throwable) {
        val current = _renderExportState.value
        val phase = if (current.status == RenderExportStatus.CANCELLING) RenderExportPhase.CLEANING_UP else current.phase
        val error = if (throwable is kotlinx.coroutines.CancellationException || current.status == RenderExportStatus.CANCELLING) RenderExportError(RenderExportErrorType.CANCELLED, "Export cancelled.", true, null, RenderExportPhase.CLEANING_UP) else RenderExportErrorClassifier.classify(throwable, phase)
        val cleaned = current.tempWorkspace?.let { withContext(Dispatchers.IO) { tempFileManager.cleanup(it) } }
        _renderExportState.value = if (error.type == com.example.clipystudio.data.RenderExportErrorType.CANCELLED) {
          current.copy(
            status = RenderExportStatus.CANCELLED,
            phase = RenderExportPhase.CLEANING_UP,
            tempWorkspace = cleaned,
            error = error,
            canCancel = false,
            canRetry = true,
            progress = current.progress.copy(message = "Cancelled and cleaned up", updatedAtMs = System.currentTimeMillis()),
            diagnostics = current.diagnostics.copy(
              stages = RenderExportPlanner.stageDiagnostics(RenderExportPhase.CLEANING_UP, framePlan = current.diagnostics.lastFramePlan, audioPlan = current.diagnostics.audioMixPlan, audioSync = current.diagnostics.audioSync, codecStrategy = current.codecStrategy, workspace = cleaned, error = error),
              lastFailureCategory = error.type,
            ),
          )
        } else {
          current.copy(
            status = RenderExportStatus.FAILED,
            phase = phase,
            tempWorkspace = cleaned,
            error = error,
            canCancel = false,
            canRetry = error.recoverable,
            progress = current.progress.copy(message = error.message, updatedAtMs = System.currentTimeMillis()),
            diagnostics = current.diagnostics.copy(
              stages = RenderExportPlanner.stageDiagnostics(phase, framePlan = current.diagnostics.lastFramePlan, audioPlan = current.diagnostics.audioMixPlan, audioSync = current.diagnostics.audioSync, codecStrategy = current.codecStrategy, workspace = cleaned, error = error),
              lastFailureCategory = error.type,
            ),
          )
        }
      }
    }
  }

  fun completeExport() = Unit

  fun cancelExport() {
    val current = _renderExportState.value
    if (!current.canCancel) return
    _renderExportState.value = current.copy(status = RenderExportStatus.CANCELLING, phase = RenderExportPhase.CLEANING_UP, canCancel = false, progress = current.progress.copy(message = "Cancelling export and cleaning temp files", updatedAtMs = System.currentTimeMillis()))
    exportJob?.cancel()
  }

  fun retryExport() {
    _renderExportState.value = RenderExportState(
      options = _renderExportState.value.options,
      pipelineState = _renderPipelineState.value,
      diagnostics = RenderDiagnostics(stages = RenderExportPlanner.stageDiagnostics(RenderExportPhase.NONE)),
    )
    startExport()
  }

  fun requestShare() {
    val output = _renderExportState.value.output ?: return
    _shareEvent.value = ShareOutputEvent(output.uri, output.mimeType, "Share exported video")
  }

  fun consumeShareEvent() {
    _shareEvent.value = null
  }

  fun clearExportResult() {
    _renderExportState.value = RenderExportState(
      options = _renderPipelineState.value.options,
      pipelineState = _renderPipelineState.value,
      diagnostics = RenderDiagnostics(stages = RenderExportPlanner.stageDiagnostics(RenderExportPhase.NONE)),
    )
  }

  fun clearCache() = dataRepository.clearCache()

  private fun ensureActiveSession() {
    if (_renderExportState.value.status == RenderExportStatus.CANCELLING) {
      throw kotlinx.coroutines.CancellationException("Export cancelled")
    }
  }
}

sealed interface MainScreenUiState {
  data object Loading : MainScreenUiState
  data class Error(val throwable: Throwable) : MainScreenUiState
  data class Success(val appState: AppState) : MainScreenUiState
}
