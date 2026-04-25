package com.example.clipystudio.editor.model

import com.example.clipystudio.data.AppState
import com.example.clipystudio.data.ClipType
import com.example.clipystudio.data.ExportResolution
import com.example.clipystudio.data.Timeline
import com.example.clipystudio.data.TimelineClip
import com.example.clipystudio.data.TimelineTrack

fun AppState.toEditorUiState(): EditorUiState {
  val timeline = activeProject?.timeline ?: Timeline.defaultTimeline()
  val exportSettings = defaultExportSettings.toDomainExportSettings()
  val project = activeProject?.toDomainProject(exportSettings)
  val overlays = timeline.tracks.flatMap { track -> track.clips.mapNotNull { clip -> clip.toDomainOverlayOrNull() } }
  return EditorUiState(
    project = project,
    currentTimeMs = timeline.playheadMs,
    isPlaying = timeline.isPlaying,
    selectedTool = timeline.selectedTool.toDomainEditorTool(),
    selectedClipId = timeline.selectedClipId,
    selectedOverlayId = timeline.selectedClipId?.takeIf { id -> overlays.any { it.id == id } },
    timelineState = TimelineUiState(
      tracks = timeline.tracks.map { it.toDomainTrack(timeline.selectedClipId) },
      scrollOffsetPx = timeline.scrollOffsetPx,
      scalePxPerMs = timeline.pixelsPerSecond * timeline.zoomLevel / 1_000f,
      gestureMode = if (timeline.isPlaying) TimelineGestureMode.Playing else TimelineGestureMode.Idle,
      activeClipId = timeline.selectedClipId,
      activeTrimHandle = null,
      snapGuides = timeline.markers.map { it.timeMs },
    ),
    previewState = PreviewUiState(
      overlays = overlays,
      selectedOverlayId = timeline.selectedClipId?.takeIf { id -> overlays.any { it.id == id } },
      canvasWidthPx = 0f,
      canvasHeightPx = 0f,
      showBoundaryGuide = false,
      previewSeekMs = timeline.playheadMs,
      isPreviewReady = activeProject != null,
    ),
    panelState = PanelUiState(
      activeTool = timeline.selectedTool.toDomainEditorTool(),
      isDirty = activeProject?.autosaveVersion?.let { it > 1 } == true,
      isPlaybackLocked = timeline.isPlaying,
      isExporting = exportJob?.status == com.example.clipystudio.data.ExportStatus.Running,
    ),
    exportSettings = exportSettings,
  )
}

fun com.example.clipystudio.data.Project.toDomainProject(exportSettings: ExportSettings): Project = Project(
  id = id,
  name = name,
  createdAtMs = createdAt,
  updatedAtMs = updatedAt,
  durationMs = timeline.durationMs.takeIf { it > 0 } ?: durationMs,
  tracks = timeline.tracks.map { it.toDomainTrack(timeline.selectedClipId) },
  overlays = timeline.tracks.flatMap { track -> track.clips.mapNotNull { clip -> clip.toDomainOverlayOrNull() } },
  transitions = timeline.transitions.map { transition ->
    Transition(
      id = transition.id,
      fromClipId = transition.fromClipId,
      toClipId = transition.toClipId,
      type = transition.type.toDomainTransitionType(),
      durationMs = transition.durationMs,
      parameters = mapOf("boundaryMs" to transition.boundaryMs.toString()),
    )
  },
  exportSettings = exportSettings,
  metadata = mapOf(
    "canvasRatio" to canvasRatio.label,
    "thumbnailUri" to thumbnailUri.orEmpty(),
    "lastPlaybackPositionMs" to lastPlaybackPositionMs.toString(),
    "autosaveVersion" to autosaveVersion.toString(),
  ),
)

private fun TimelineTrack.toDomainTrack(selectedClipId: String?): Track = Track(
  id = id,
  name = name,
  type = type.toDomainTrackType(),
  order = orderIndex,
  clips = clips.map { it.toDomainClip(id, selectedClipId) },
  isMuted = isMuted,
  isLocked = isLocked,
  isVisible = true,
)

private fun TimelineClip.toDomainClip(trackId: String, selectedClipId: String?): Clip = Clip(
  id = id,
  trackId = trackId,
  sourceUri = assetId,
  startMs = startMs,
  durationMs = durationMs,
  trimStartMs = sourceInMs,
  trimEndMs = 0L,
  zIndex = zIndex.toFloat(),
  effects = if (clipType == ClipType.Effect) listOf(toDomainEffectClip()) else emptyList(),
  keyframes = keyframes.map { keyframe ->
    Keyframe(
      id = keyframe.id,
      targetId = id,
      property = keyframe.property.toDomainKeyframeProperty(),
      timeMs = keyframe.timeMs,
      value = keyframe.value,
    )
  },
  isSelected = id == selectedClipId,
  isLocked = false,
)

private fun TimelineClip.toDomainOverlayOrNull(): Overlay? {
  val type = when (clipType) {
    ClipType.Text -> OverlayType.Text
    ClipType.Sticker -> OverlayType.Sticker
    ClipType.Overlay -> OverlayType.Image
    ClipType.Effect -> OverlayType.Effect
    else -> return null
  }
  return Overlay(
    id = id,
    clipId = id,
    type = type,
    startMs = startMs,
    durationMs = durationMs,
    x = transform.positionX,
    y = transform.positionY,
    width = transform.scale,
    height = transform.scale,
    rotationDegrees = transform.rotationDegrees,
    scale = transform.scale,
    alpha = transform.opacity,
    zIndex = zIndex.toFloat(),
    keyframes = keyframes.map { keyframe ->
      Keyframe(keyframe.id, id, keyframe.property.toDomainKeyframeProperty(), keyframe.timeMs, keyframe.value)
    },
  )
}

private fun TimelineClip.toDomainEffectClip(): EffectClip = EffectClip(
  id = id,
  type = EffectType.Glow,
  startMs = startMs,
  durationMs = durationMs,
  intensity = 1f,
  parameters = mapOf("title" to title),
)

private fun com.example.clipystudio.data.ExportSettings.toDomainExportSettings(): ExportSettings {
  val (width, height) = when (resolution) {
    ExportResolution.P720 -> 720 to 1280
    ExportResolution.P1080 -> 1080 to 1920
    ExportResolution.P2K -> 1440 to 2560
    ExportResolution.P4K -> 2160 to 3840
  }
  return ExportSettings(
    resolutionWidth = width,
    resolutionHeight = height,
    frameRate = fps,
    videoBitrate = (bitrateMbps * 1_000_000).toLong(),
    audioBitrate = 192_000,
    format = if (format.equals("MOV", ignoreCase = true)) ExportFormat.Mov else ExportFormat.Mp4,
    codec = VideoCodec.H264,
    includeAudio = true,
  )
}

private fun com.example.clipystudio.data.TrackType.toDomainTrackType(): TrackType = when (this) {
  com.example.clipystudio.data.TrackType.Video -> TrackType.Video
  com.example.clipystudio.data.TrackType.Audio -> TrackType.Audio
  com.example.clipystudio.data.TrackType.Text -> TrackType.Text
  com.example.clipystudio.data.TrackType.Sticker -> TrackType.Sticker
  com.example.clipystudio.data.TrackType.Effect -> TrackType.Effect
  com.example.clipystudio.data.TrackType.Overlay -> TrackType.Overlay
}

private fun com.example.clipystudio.data.EditorTool.toDomainEditorTool(): EditorTool = when (this) {
  com.example.clipystudio.data.EditorTool.Edit -> EditorTool.Edit
  com.example.clipystudio.data.EditorTool.Audio -> EditorTool.Audio
  com.example.clipystudio.data.EditorTool.Text -> EditorTool.Text
  com.example.clipystudio.data.EditorTool.Sticker -> EditorTool.Sticker
  com.example.clipystudio.data.EditorTool.Overlay -> EditorTool.Overlay
  com.example.clipystudio.data.EditorTool.Filter -> EditorTool.Filter
  com.example.clipystudio.data.EditorTool.Effect -> EditorTool.Effect
  com.example.clipystudio.data.EditorTool.Transition -> EditorTool.Transition
  com.example.clipystudio.data.EditorTool.Canvas -> EditorTool.Canvas
  com.example.clipystudio.data.EditorTool.Speed -> EditorTool.Speed
  com.example.clipystudio.data.EditorTool.Export -> EditorTool.Export
}

private fun com.example.clipystudio.data.TransitionType.toDomainTransitionType(): TransitionType = when (this) {
  com.example.clipystudio.data.TransitionType.Fade -> TransitionType.Fade
  com.example.clipystudio.data.TransitionType.Slide -> TransitionType.Slide
  com.example.clipystudio.data.TransitionType.Zoom -> TransitionType.Zoom
  com.example.clipystudio.data.TransitionType.Blur -> TransitionType.Blur
}

private fun com.example.clipystudio.data.KeyframeProperty.toDomainKeyframeProperty(): KeyframeProperty = when (this) {
  com.example.clipystudio.data.KeyframeProperty.PositionX -> KeyframeProperty.PositionX
  com.example.clipystudio.data.KeyframeProperty.PositionY -> KeyframeProperty.PositionY
  com.example.clipystudio.data.KeyframeProperty.Scale -> KeyframeProperty.Scale
  com.example.clipystudio.data.KeyframeProperty.Rotation -> KeyframeProperty.Rotation
  com.example.clipystudio.data.KeyframeProperty.Opacity -> KeyframeProperty.Opacity
}
