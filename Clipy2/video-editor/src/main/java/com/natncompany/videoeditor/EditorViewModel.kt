package com.natncompany.videoeditor

import android.net.Uri
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.natncompany.media.MediaError
import com.natncompany.media.MediaImportInput
import com.natncompany.media.MediaResult
import com.natncompany.media.MediaSessionEvent
import com.natncompany.media.MediaSessionManager
import com.natncompany.media.Timeline
import com.natncompany.media.TimelineClip
import com.natncompany.media.TimelineEditor
import com.natncompany.media.VideoProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

class EditorViewModel(
    private val mediaSessionManager: MediaSessionManager,
    private val timelineEditor: TimelineEditor
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mediaSessionManager.state.collect { state ->
                val timeline = state.currentTimeline ?: state.currentProject?.timeline ?: Timeline()
                val preview = state.previewState
                _uiState.update { current ->
                    current.copy(
                        project = state.currentProject,
                        timeline = timeline,
                        selectedClipId = timeline.selectedClipIds.firstOrNull()
                            ?: current.selectedClipId?.takeIf { timeline.findClip(it) != null },
                        isPlaying = preview.isPlaying,
                        isPreviewPrepared = preview.isPrepared,
                        position = preview.positionMs,
                        duration = max(preview.durationMs, timeline.durationMs),
                        importProgress = state.activeImportJobs.lastOrNull()?.progressPercent,
                        exportProgress = state.activeRenderJobs.lastOrNull()?.progressPercent,
                        previewError = preview.error,
                        criticalErrorMessage = preview.error ?: current.criticalErrorMessage
                    )
                }
            }
        }
        viewModelScope.launch {
            mediaSessionManager.events.collect { event ->
                when (event) {
                    is MediaSessionEvent.ImportStarted -> _uiState.update { it.copy(importProgress = 0, snackbarErrorMessage = null, criticalErrorMessage = null) }
                    is MediaSessionEvent.ImportCompleted -> _uiState.update { it.copy(importProgress = null) }
                    is MediaSessionEvent.ImportFailed -> reportError(event.error) { it.copy(importProgress = null) }
                    is MediaSessionEvent.RenderStarted -> _uiState.update { it.copy(exportProgress = 0, exportResultPath = null, snackbarErrorMessage = null, criticalErrorMessage = null) }
                    is MediaSessionEvent.RenderProgress -> _uiState.update { it.copy(exportProgress = event.progressPercent) }
                    is MediaSessionEvent.RenderCompleted -> _uiState.update { it.copy(exportProgress = null, exportResultPath = event.outputPath) }
                    is MediaSessionEvent.RenderFailed -> reportError(event.error) { it.copy(exportProgress = null) }
                    is MediaSessionEvent.PreviewError -> reportError(event.error)
                    else -> Unit
                }
            }
        }
    }

    fun openProject(project: VideoProject) = launchMediaCall { mediaSessionManager.openProject(project) }
    fun importMedia(uris: List<Uri>) = launchMediaCall { mediaSessionManager.importBatch(uris.map { MediaImportInput(uri = it) }) }
    fun importMedia(uri: Uri) = importMedia(listOf(uri))
    fun play() = launchMediaCall { mediaSessionManager.play() }
    fun pause() = launchMediaCall { mediaSessionManager.pause() }
    fun seekTo(positionMs: Long) = launchMediaCall { mediaSessionManager.seekTo(positionMs.coerceAtLeast(0L)) }
    fun scrubTo(positionMs: Long) = launchMediaCall { mediaSessionManager.scrubTo(positionMs.coerceAtLeast(0L)) }
    fun setSurface(surface: Surface?) = launchMediaCall { mediaSessionManager.preparePreview(surface) }
    fun clearSnackbarError() = _uiState.update { it.copy(snackbarErrorMessage = null) }
    fun clearCriticalError() = _uiState.update { it.copy(criticalErrorMessage = null) }

    fun selectClip(clipId: String?) {
        val timeline = _uiState.value.timeline
        val result = if (clipId == null) {
            timelineEditor.clearSelection(timeline)
        } else {
            timelineEditor.selectClip(timeline, clipId)
        }
        applyTimelineResult(result)
    }

    fun moveClip(trackId: String, clipId: String, newStartMs: Long) {
        val timeline = _uiState.value.timeline
        applyTimelineResult(timelineEditor.moveClip(timeline, trackId, trackId, clipId, newStartMs.coerceAtLeast(0L)))
    }

    fun trimClipStart(trackId: String, clipId: String, newSourceStartMs: Long) {
        applyTimelineResult(timelineEditor.trimClipStart(_uiState.value.timeline, trackId, clipId, newSourceStartMs.coerceAtLeast(0L)))
    }

    fun trimClipEnd(trackId: String, clipId: String, newSourceEndMs: Long) {
        applyTimelineResult(timelineEditor.trimClipEnd(_uiState.value.timeline, trackId, clipId, newSourceEndMs.coerceAtLeast(0L)))
    }

    fun splitSelectedClip() {
        val state = _uiState.value
        val clipId = state.selectedClipId ?: return
        val track = state.timeline.findClipTrack(clipId) ?: return
        applyTimelineResult(timelineEditor.splitClip(state.timeline, track.id, clipId, state.position))
    }

    fun trimSelectedClip() = selectTool(EditorTool.Trim)

    fun cropSelectedClip() = selectTool(EditorTool.Crop)

    fun rotateSelectedClip() = selectTool(EditorTool.Rotate)

    fun filterSelectedClip() = selectTool(EditorTool.Filter)

    fun speedSelectedClip() = selectTool(EditorTool.Speed)

    fun volumeSelectedClip() = selectTool(EditorTool.Volume)

    fun onAction(action: EditorAction) {
        when (action) {
            EditorAction.PlayPause -> if (_uiState.value.isPlaying) pause() else play()
            is EditorAction.Seek -> seekTo(action.positionMs)
            is EditorAction.SelectClip -> selectClip(action.clipId)
            is EditorAction.MoveClip -> moveClip(action.trackId, action.clipId, action.newStartMs)
            is EditorAction.TrimClip -> {
                action.newSourceStartMs?.let { trimClipStart(action.trackId, action.clipId, it) }
                action.newSourceEndMs?.let { trimClipEnd(action.trackId, action.clipId, it) }
            }
            EditorAction.Split -> splitSelectedClip()
            EditorAction.Delete -> deleteSelectedClip()
            is EditorAction.Export -> exportVideo(action.quality)
            is EditorAction.Import -> importMedia(action.uris)
            is EditorAction.SelectTool -> selectTool(action.tool)
            is EditorAction.ApplyFilter -> updateSelectedClip { clip ->
                clip.copy(
                    transform = when (action.filter) {
                        ClipFilter.Brightness -> clip.transform.copy(brightness = 0.25f)
                        ClipFilter.Contrast -> clip.transform.copy(contrast = 1.25f)
                        ClipFilter.Blur -> clip.transform.copy(blur = 0.25f)
                    }
                )
            }
            EditorAction.RotateLeft -> updateSelectedClip { clip ->
                clip.copy(transform = clip.transform.copy(rotationDegrees = normalizeRotation(clip.transform.rotationDegrees - 90f)))
            }
            EditorAction.RotateRight -> updateSelectedClip { clip ->
                clip.copy(transform = clip.transform.copy(rotationDegrees = normalizeRotation(clip.transform.rotationDegrees + 90f)))
            }
            EditorAction.FlipHorizontal -> updateSelectedClip { clip ->
                clip.copy(transform = clip.transform.copy(flipHorizontal = !clip.transform.flipHorizontal))
            }
            EditorAction.FlipVertical -> updateSelectedClip { clip ->
                clip.copy(transform = clip.transform.copy(flipVertical = !clip.transform.flipVertical))
            }
            is EditorAction.SetVolume -> updateSelectedClip { clip ->
                clip.copy(audio = clip.audio.copy(volume = action.volume.coerceIn(0f, 1f)))
            }
            is EditorAction.SetMuted -> updateSelectedClip { clip ->
                clip.copy(audio = clip.audio.copy(isMuted = action.muted))
            }
        }
    }

    fun deleteSelectedClip() {
        val state = _uiState.value
        val clipId = state.selectedClipId ?: return
        val track = state.timeline.findClipTrack(clipId) ?: return
        applyTimelineResult(timelineEditor.removeClip(state.timeline, track.id, clipId))
    }

    fun duplicateSelectedClip() {
        val state = _uiState.value
        val clipId = state.selectedClipId ?: return
        val track = state.timeline.findClipTrack(clipId) ?: return
        applyTimelineResult(timelineEditor.duplicateClip(state.timeline, track.id, clipId, "${clipId}-copy-${System.currentTimeMillis()}"))
    }

    fun exportVideo(outputFileName: String = defaultExportName()) = launchMediaCall {
        mediaSessionManager.export(_uiState.value.timeline.toDefaultExportConfig(outputFileName))
    }

    fun exportVideo(quality: ExportQuality, outputFileName: String = defaultExportName()) = launchMediaCall {
        mediaSessionManager.export(_uiState.value.timeline.toDefaultExportConfig(outputFileName, quality))
    }

    fun cancelExport() = launchMediaCall { mediaSessionManager.cancelAllJobs() }

    private fun applyTimelineResult(result: MediaResult<Timeline>) {
        when (result) {
            is MediaResult.Success -> launchMediaCall { mediaSessionManager.updateTimeline(result.value) }
            is MediaResult.Failure -> reportError(result.error)
        }
    }

    private fun launchMediaCall(call: suspend () -> MediaResult<*>) {
        viewModelScope.launch {
            when (val result = call()) {
                is MediaResult.Success -> Unit
                is MediaResult.Failure -> reportError(result.error)
            }
        }
    }

    private fun reportError(error: MediaError, transform: (EditorUiState) -> EditorUiState = { it }) {
        _uiState.update { state ->
            val updated = transform(state)
            if (error.isCritical) {
                updated.copy(criticalErrorMessage = error.message)
            } else {
                updated.copy(snackbarErrorMessage = error.message)
            }
        }
    }

    private val MediaError.isCritical: Boolean
        get() = when (this) {
            is MediaError.FileAccess,
            is MediaError.UnsupportedFormat,
            is MediaError.CorruptMedia,
            is MediaError.BackendUnavailable,
            is MediaError.ExceptionError -> true
            is MediaError.InvalidInput,
            is MediaError.Validation,
            is MediaError.Cancelled -> false
        }

    private fun selectTool(tool: EditorTool) {
        if (_uiState.value.selectedClipId == null) return
        _uiState.update { it.copy(activeTool = tool) }
    }

    private fun updateSelectedClip(transform: (TimelineClip) -> TimelineClip) {
        val state = _uiState.value
        val clipId = state.selectedClipId ?: return
        val track = state.timeline.findClipTrack(clipId) ?: return
        val clip = track.clips.firstOrNull { it.id == clipId } ?: return
        val updatedTimeline = state.timeline.copy(
            tracks = state.timeline.tracks.map { timelineTrack ->
                if (timelineTrack.id == track.id) {
                    timelineTrack.copy(clips = timelineTrack.clips.map { existing ->
                        if (existing.id == clipId) transform(clip) else existing
                    })
                } else {
                    timelineTrack
                }
            }
        )
        launchMediaCall { mediaSessionManager.updateTimeline(updatedTimeline) }
    }

    private fun normalizeRotation(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

    private fun defaultExportName(): String = "clipy-export-${System.currentTimeMillis()}.mp4"
}
