package com.example.clipystudio.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

interface DataRepository {
  val appState: Flow<AppState>

  fun completeIntro()
  fun setLanguage(languageCode: LanguageCode)
  fun createProject(ratio: CanvasRatio = CanvasRatio.Portrait)
  fun renameProject(projectId: String, name: String)
  fun duplicateProject(projectId: String)
  fun deleteProject(projectId: String)
  fun openProject(projectId: String)
  fun addImportedAsset(type: MediaType, uri: String? = null, displayName: String? = null, sizeBytes: Long? = null)
  fun removeImportedAsset(assetId: String)
  fun addImportsToProject()
  fun selectClip(clipId: String)
  fun togglePlayback()
  fun seekTo(positionMs: Long)
  fun seekBy(deltaMs: Long)
  fun updateTimelineZoom(delta: Float)
  fun updateCanvasRatio(ratio: CanvasRatio)
  fun splitSelectedClip()
  fun deleteSelectedClip()
  fun duplicateSelectedClip()
  fun trimSelectedClip(deltaMs: Long)
  fun moveSelectedClip(deltaMs: Long)
  fun updateSelectedTool(tool: EditorTool)
  fun adjustSelectedClip(action: ClipAction)
  fun transformSelectedClip(deltaX: Float, deltaY: Float, scaleChange: Float, rotationChange: Float)
  fun addAudioClipAtPlayhead(title: String, source: AudioSource)
  fun addTextClipAtPlayhead(content: String, fontSizeSp: Float, color: String, backgroundColor: String?, strokeEnabled: Boolean, shadowEnabled: Boolean, alignment: String, animation: String)
  fun undo()
  fun redo()
  fun updateExportSettings(settings: ExportSettings)
  fun startExport()
  fun completeExport()
  fun cancelExport()
  fun clearExportResult()
  fun clearCache()
}

class DefaultDataRepository(context: Context? = null) : DataRepository {
  private val preferences = context?.getSharedPreferences("clipy_state", Context.MODE_PRIVATE)
  private val state = MutableStateFlow(loadInitialState())
  override val appState: Flow<AppState> = state

  override fun completeIntro() = persistUpdate { it.copy(hasCompletedIntro = true) }

  override fun setLanguage(languageCode: LanguageCode) = persistUpdate { it.copy(languageCode = languageCode, hasCompletedIntro = true) }

  override fun createProject(ratio: CanvasRatio) {
    val now = System.currentTimeMillis()
    val project = Project(
      id = UUID.randomUUID().toString(),
      name = "Social cut ${state.value.projects.size + 1}",
      createdAt = now,
      updatedAt = now,
      canvasRatio = ratio,
      timeline = Timeline.defaultTimeline(),
    )
    persistUpdate { it.copy(projects = listOf(project) + it.projects, activeProjectId = project.id, selectedImports = emptyList()) }
  }

  override fun renameProject(projectId: String, name: String) = mutateProject(projectId) { it.copy(name = name.ifBlank { it.name }) }

  override fun duplicateProject(projectId: String) {
    val original = state.value.projects.firstOrNull { it.id == projectId } ?: return
    val now = System.currentTimeMillis()
    val copy = original.copy(
      id = UUID.randomUUID().toString(),
      name = "${original.name} duplicate",
      createdAt = now,
      updatedAt = now,
      timeline = original.timeline.copy(
        tracks = original.timeline.tracks.map { track ->
          track.copy(id = UUID.randomUUID().toString(), clips = track.clips.map { it.copy(id = UUID.randomUUID().toString()) })
        },
      ),
    )
    persistUpdate { it.copy(projects = listOf(copy) + it.projects, activeProjectId = copy.id) }
  }

  override fun deleteProject(projectId: String) = persistUpdate { app ->
    app.copy(projects = app.projects.filterNot { it.id == projectId }, activeProjectId = app.activeProjectId.takeUnless { it == projectId })
  }

  override fun openProject(projectId: String) = persistUpdate { it.copy(activeProjectId = projectId) }

  override fun addImportedAsset(type: MediaType, uri: String?, displayName: String?, sizeBytes: Long?) = persistUpdate { app ->
    val index = app.selectedImports.count { it.type == type } + 1
    val asset = MediaAsset(
      id = UUID.randomUUID().toString(),
      uri = uri ?: "local://${type.name.lowercase()}/$index",
      type = type,
      displayName = displayName?.ifBlank { null } ?: "${type.label} sample $index",
      durationMs = when (type) {
        MediaType.Image -> 4_000
        MediaType.Video -> 8_000
        MediaType.Audio -> 12_000
      },
      sizeBytes = sizeBytes ?: when (type) {
        MediaType.Image -> 2_400_000
        MediaType.Video -> 48_000_000
        MediaType.Audio -> 6_800_000
      },
    )
    app.copy(selectedImports = app.selectedImports + asset)
  }

  override fun removeImportedAsset(assetId: String) = persistUpdate { it.copy(selectedImports = it.selectedImports.filterNot { asset -> asset.id == assetId }) }

  override fun addImportsToProject() {
    val app = state.value
    val projectId = app.activeProjectId ?: run {
      createProject(CanvasRatio.Portrait)
      state.value.activeProjectId ?: return
    }
    val imports = app.selectedImports.ifEmpty { emptyList() }
    if (imports.isEmpty()) return
    mutateProject(projectId) { project ->
      val baseTimeline = project.timeline
      val videoTrack = baseTimeline.tracks.first { it.type == TrackType.Video }
      val audioTrack = baseTimeline.tracks.first { it.type == TrackType.Audio }
      val textTrack = baseTimeline.tracks.first { it.type == TrackType.Text }
      var videoCursor = videoTrack.clips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
      var audioCursor = audioTrack.clips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
      val updatedTracks = baseTimeline.tracks.map { track ->
        when (track.type) {
          TrackType.Video -> track.copy(clips = track.clips + imports.filter { it.type != MediaType.Audio }.map { asset ->
            TimelineClip(
              id = UUID.randomUUID().toString(),
              assetId = asset.id,
              clipType = if (asset.type == MediaType.Image) ClipType.Image else ClipType.Video,
              title = asset.displayName,
              startMs = videoCursor.also { videoCursor += asset.durationMs },
              durationMs = asset.durationMs,
            )
          })
          TrackType.Audio -> track.copy(clips = track.clips + imports.filter { it.type == MediaType.Audio }.map { asset ->
            TimelineClip(
              id = UUID.randomUUID().toString(),
              assetId = asset.id,
              clipType = ClipType.Audio,
              title = asset.displayName,
              startMs = audioCursor.also { audioCursor += asset.durationMs },
              durationMs = asset.durationMs,
              transform = TransformState(opacity = 1f),
              audioProperties = AudioProperties(volume = 0.8f),
            )
          })
          TrackType.Text -> if (textTrack.clips.isEmpty()) track.copy(clips = listOf(TimelineClip.textClip())) else track
          else -> track
        }
      }
      project.copy(importedAssets = (project.importedAssets + imports).distinctBy { it.id }, timeline = baseTimeline.copy(tracks = updatedTracks).recalculateDuration())
    }
    persistUpdate { it.copy(selectedImports = emptyList()) }
  }

  override fun selectClip(clipId: String) = updateTimeline { it.copy(selectedClipId = clipId) }

  override fun togglePlayback() = updateTimeline { it.copy(isPlaying = !it.isPlaying, playheadMs = if (it.playheadMs >= it.durationMs) 0L else it.playheadMs) }

  override fun seekTo(positionMs: Long) = updateTimeline { timeline -> timeline.copy(playheadMs = positionMs.coerceIn(0L, timeline.durationMs)) }

  override fun seekBy(deltaMs: Long) = updateTimeline { timeline -> timeline.copy(playheadMs = (timeline.playheadMs + deltaMs).coerceIn(0L, timeline.durationMs)) }

  override fun updateTimelineZoom(delta: Float) = updateTimeline { timeline -> timeline.copy(zoomLevel = (timeline.zoomLevel + delta).coerceIn(0.65f, 3f)) }

  override fun updateCanvasRatio(ratio: CanvasRatio) {
    val projectId = state.value.activeProjectId ?: return
    mutateProject(projectId) { it.copy(canvasRatio = ratio) }
  }

  override fun splitSelectedClip() = withUndo("Split clip") { project ->
    val selectedId = project.timeline.selectedClipId ?: return@withUndo project
    val playhead = project.timeline.playheadMs
    project.copy(timeline = project.timeline.copy(tracks = project.timeline.tracks.map { track ->
      val clip = track.clips.firstOrNull { it.id == selectedId } ?: return@map track
      val splitPoint = playhead - clip.startMs
      if (splitPoint <= 500 || splitPoint >= clip.durationMs - 500) return@map track
      val first = clip.copy(durationMs = splitPoint)
      val second = clip.copy(id = UUID.randomUUID().toString(), startMs = playhead, durationMs = clip.durationMs - splitPoint, sourceInMs = clip.sourceInMs + splitPoint)
      track.copy(clips = track.clips.flatMap { if (it.id == selectedId) listOf(first, second) else listOf(it) })
    }).recalculateDuration())
  }

  override fun duplicateSelectedClip() = withUndo("Duplicate clip") { project ->
    val selectedId = project.timeline.selectedClipId ?: return@withUndo project
    project.copy(timeline = project.timeline.copy(tracks = project.timeline.tracks.map { track ->
      val clip = track.clips.firstOrNull { it.id == selectedId } ?: return@map track
      track.copy(clips = track.clips + clip.copy(id = UUID.randomUUID().toString(), startMs = clip.startMs + clip.durationMs, title = "${clip.title} copy"))
    }).recalculateDuration())
  }

  override fun deleteSelectedClip() = withUndo("Delete clip") { project ->
    val selectedId = project.timeline.selectedClipId ?: return@withUndo project
    project.copy(timeline = project.timeline.copy(
      selectedClipId = null,
      tracks = project.timeline.tracks.map { track -> track.copy(clips = track.clips.filterNot { it.id == selectedId }) },
    ).recalculateDuration())
  }

  override fun trimSelectedClip(deltaMs: Long) = withUndo("Trim clip") { project ->
    mapSelectedClip(project) { clip -> clip.copy(durationMs = (clip.durationMs + deltaMs).coerceAtLeast(1_000L)) }.copyTimelineDuration()
  }

  override fun moveSelectedClip(deltaMs: Long) = withUndo("Move clip") { project ->
    mapSelectedClip(project) { clip -> clip.copy(startMs = (clip.startMs + deltaMs).coerceAtLeast(0L)) }.copyTimelineDuration()
  }

  override fun updateSelectedTool(tool: EditorTool) = updateTimeline { it.copy(selectedTool = tool) }

  override fun adjustSelectedClip(action: ClipAction) = withUndo(action.label) { project ->
    mapSelectedClip(project) { clip ->
      when (action) {
        ClipAction.Rotate -> clip.copy(transform = clip.transform.copy(rotationDegrees = (clip.transform.rotationDegrees + 90f) % 360f))
        ClipAction.Flip -> clip.copy(transform = clip.transform.copy(flipHorizontal = !clip.transform.flipHorizontal))
        ClipAction.OpacityDown -> clip.copy(transform = clip.transform.copy(opacity = (clip.transform.opacity - 0.1f).coerceAtLeast(0.2f)))
        ClipAction.OpacityUp -> clip.copy(transform = clip.transform.copy(opacity = (clip.transform.opacity + 0.1f).coerceAtMost(1f)))
        ClipAction.SpeedUp -> clip.copy(videoProperties = clip.videoProperties.copy(speed = (clip.videoProperties.speed + 0.25f).coerceAtMost(2f)))
        ClipAction.SpeedDown -> clip.copy(videoProperties = clip.videoProperties.copy(speed = (clip.videoProperties.speed - 0.25f).coerceAtLeast(0.5f)))
        ClipAction.Mute -> clip.copy(videoProperties = clip.videoProperties.copy(sourceAudioMuted = !clip.videoProperties.sourceAudioMuted))
        ClipAction.Filter -> clip.copy(filterAdjustments = clip.filterAdjustments.copy(filterId = if (clip.filterAdjustments.filterId == "cinematic") null else "cinematic", contrast = 1.1f, saturation = 0.92f))
        ClipAction.Keyframe -> clip.copy(keyframes = clip.keyframes + Keyframe(timeMs = project.timeline.playheadMs, property = KeyframeProperty.Opacity, value = clip.transform.opacity))
        ClipAction.VolumeDown -> clip.copy(audioProperties = clip.audioProperties.copy(volume = (clip.audioProperties.volume - 0.1f).coerceAtLeast(0f)))
        ClipAction.VolumeUp -> clip.copy(audioProperties = clip.audioProperties.copy(volume = (clip.audioProperties.volume + 0.1f).coerceAtMost(1f)))
        ClipAction.Fade -> clip.copy(audioProperties = clip.audioProperties.copy(fadeInMs = if (clip.audioProperties.fadeInMs == 0L) 600 else 0, fadeOutMs = if (clip.audioProperties.fadeOutMs == 0L) 600 else 0))
        ClipAction.Loop -> clip.copy(audioProperties = clip.audioProperties.copy(loopEnabled = !clip.audioProperties.loopEnabled))
        ClipAction.Replace -> clip.copy(title = "${clip.title} replaced", assetId = clip.assetId ?: "replacement://${UUID.randomUUID()}")
        ClipAction.Crop -> clip.copy(filterAdjustments = clip.filterAdjustments.copy(vignette = if (clip.filterAdjustments.vignette == 0f) 0.28f else 0f))
      }
    }
  }

  override fun transformSelectedClip(deltaX: Float, deltaY: Float, scaleChange: Float, rotationChange: Float) = withUndo("Transform overlay") { project ->
    mapSelectedClip(project) { clip ->
      clip.copy(transform = clip.transform.copy(
        positionX = (clip.transform.positionX + deltaX).coerceIn(0.05f, 0.95f),
        positionY = (clip.transform.positionY + deltaY).coerceIn(0.05f, 0.95f),
        scale = (clip.transform.scale * scaleChange).coerceIn(0.35f, 3f),
        rotationDegrees = (clip.transform.rotationDegrees + rotationChange) % 360f,
      ))
    }
  }

  override fun addAudioClipAtPlayhead(title: String, source: AudioSource) = withUndo("Add audio") { project ->
    val clip = TimelineClip(
      id = UUID.randomUUID().toString(),
      clipType = ClipType.Audio,
      title = title,
      startMs = project.timeline.playheadMs,
      durationMs = if (source == AudioSource.SoundEffect) 1_500 else 8_000,
      audioProperties = AudioProperties(volume = if (source == AudioSource.SoundEffect) 0.9f else 0.72f, fadeInMs = 300, fadeOutMs = 500),
    )
    project.copy(timeline = project.timeline.copy(
      selectedClipId = clip.id,
      selectedTool = EditorTool.Audio,
      tracks = project.timeline.tracks.map { if (it.type == TrackType.Audio) it.copy(clips = it.clips + clip) else it },
    ).recalculateDuration())
  }

  override fun addTextClipAtPlayhead(content: String, fontSizeSp: Float, color: String, backgroundColor: String?, strokeEnabled: Boolean, shadowEnabled: Boolean, alignment: String, animation: String) = withUndo("Add text") { project ->
    val text = content.ifBlank { "New title" }
    val clip = TimelineClip(
      id = UUID.randomUUID().toString(),
      clipType = ClipType.Text,
      title = text.take(18),
      startMs = project.timeline.playheadMs,
      durationMs = 3_500,
      zIndex = 10 + project.timeline.tracks.flatMap { it.clips }.count { it.clipType == ClipType.Text },
      textProperties = TextProperties(content = text, fontSizeSp = fontSizeSp, color = color, backgroundColor = backgroundColor, strokeEnabled = strokeEnabled, shadowEnabled = shadowEnabled, alignment = alignment, animation = animation),
      transform = TransformState(positionY = 0.42f),
    )
    project.copy(timeline = project.timeline.copy(
      selectedClipId = clip.id,
      selectedTool = EditorTool.Text,
      tracks = project.timeline.tracks.map { if (it.type == TrackType.Text) it.copy(clips = it.clips + clip) else it },
    ).recalculateDuration())
  }

  override fun undo() = persistUpdate { app ->
    val activeId = app.activeProjectId
    val command = app.undoStack.lastOrNull()
    if (activeId == null || command == null) app else app.copy(
      projects = app.projects.map { if (it.id == activeId) command.before else it },
      undoStack = app.undoStack.dropLast(1),
      redoStack = app.redoStack + command,
    )
  }

  override fun redo() = persistUpdate { app ->
    val activeId = app.activeProjectId
    val command = app.redoStack.lastOrNull()
    if (activeId == null || command == null) app else app.copy(
      projects = app.projects.map { if (it.id == activeId) command.after else it },
      undoStack = app.undoStack + command,
      redoStack = app.redoStack.dropLast(1),
    )
  }

  override fun updateExportSettings(settings: ExportSettings) = persistUpdate { it.copy(defaultExportSettings = settings) }

  override fun startExport() = persistUpdate { app ->
    app.copy(exportJob = ExportJob(projectId = app.activeProjectId.orEmpty(), settings = app.defaultExportSettings, status = ExportStatus.Running, progressPercent = 42))
  }

  override fun completeExport() = persistUpdate { app ->
    val job = app.exportJob ?: return@persistUpdate app
    app.copy(exportJob = job.copy(status = ExportStatus.Complete, progressPercent = 100, outputUri = "gallery://ClipyStudio/export-${System.currentTimeMillis()}.mp4"))
  }

  override fun cancelExport() = persistUpdate { it.copy(exportJob = it.exportJob?.copy(status = ExportStatus.Cancelled, progressPercent = 0)) }

  override fun clearExportResult() = persistUpdate { it.copy(exportJob = null) }

  override fun clearCache() = persistUpdate { it.copy(cacheUsageMb = 0) }

  private fun mutateProject(projectId: String, transform: (Project) -> Project) = persistUpdate { app ->
    app.copy(projects = app.projects.map { project -> if (project.id == projectId) transform(project).copy(updatedAt = System.currentTimeMillis(), autosaveVersion = project.autosaveVersion + 1) else project })
  }

  private fun updateTimeline(transform: (Timeline) -> Timeline) {
    val projectId = state.value.activeProjectId ?: return
    mutateProject(projectId) { it.copy(timeline = transform(it.timeline)) }
  }

  private fun withUndo(description: String, transform: (Project) -> Project) {
    val app = state.value
    val project = app.activeProject ?: return
    val after = transform(project).copy(updatedAt = System.currentTimeMillis(), autosaveVersion = project.autosaveVersion + 1)
    if (after == project) return
    persistUpdate { current ->
      current.copy(projects = current.projects.map { if (it.id == project.id) after else it }, undoStack = current.undoStack + UndoRedoCommand(description, project, after), redoStack = emptyList())
    }
  }

  private fun loadInitialState(): AppState = AppState(
    languageCode = LanguageCode.valueOf(preferences?.getString("language", LanguageCode.En.name) ?: LanguageCode.En.name),
    hasCompletedIntro = preferences?.getBoolean("intro", false) ?: false,
    projects = loadProjects(),
    activeProjectId = preferences?.getString("lastProjectId", null),
    defaultExportSettings = loadExportSettings(),
    cacheUsageMb = preferences?.getInt("cacheUsageMb", 128) ?: 128,
  )

  private fun persistUpdate(transform: (AppState) -> AppState) = state.update { app -> transform(app).also(::persistSettings) }

  private fun persistSettings(appState: AppState) {
    preferences?.edit()
      ?.putString("language", appState.languageCode.name)
      ?.putBoolean("intro", appState.hasCompletedIntro)
      ?.putString("lastProjectId", appState.activeProjectId)
      ?.putInt("cacheUsageMb", appState.cacheUsageMb)
      ?.putString("exportSettings", appState.defaultExportSettings.toJson().toString())
      ?.putString("projects", JSONArray(appState.projects.map { it.toJson() }).toString())
      ?.apply()
  }

  private fun loadProjects(): List<Project> {
    val raw = preferences?.getString("projects", null) ?: return emptyList()
    return runCatching {
      val array = JSONArray(raw)
      List(array.length()) { index -> array.getJSONObject(index).toProject() }
    }.getOrDefault(emptyList())
  }

  private fun loadExportSettings(): ExportSettings {
    val raw = preferences?.getString("exportSettings", null) ?: return ExportSettings()
    return runCatching { JSONObject(raw).toExportSettings() }.getOrDefault(ExportSettings())
  }

  private fun mapSelectedClip(project: Project, transform: (TimelineClip) -> TimelineClip): Project {
    val selectedId = project.timeline.selectedClipId ?: return project
    return project.copy(timeline = project.timeline.copy(tracks = project.timeline.tracks.map { track ->
      track.copy(clips = track.clips.map { if (it.id == selectedId) transform(it) else it })
    }))
  }

  private fun Project.copyTimelineDuration() = copy(timeline = timeline.recalculateDuration())

}

private fun Project.toJson() = JSONObject()
  .put("id", id)
  .put("name", name)
  .put("createdAt", createdAt)
  .put("updatedAt", updatedAt)
  .put("thumbnailUri", thumbnailUri)
  .put("durationMs", durationMs)
  .put("canvasRatio", canvasRatio.name)
  .put("timeline", timeline.toJson())
  .put("importedAssets", JSONArray(importedAssets.map { it.toJson() }))
  .put("lastPlaybackPositionMs", lastPlaybackPositionMs)
  .put("autosaveVersion", autosaveVersion)

private fun JSONObject.toProject() = Project(
  id = getString("id"),
  name = getString("name"),
  createdAt = optLong("createdAt"),
  updatedAt = optLong("updatedAt"),
  thumbnailUri = optNullableString("thumbnailUri"),
  durationMs = optLong("durationMs"),
  canvasRatio = enumValueOf(optString("canvasRatio", CanvasRatio.Portrait.name)),
  timeline = getJSONObject("timeline").toTimeline(),
  importedAssets = optJSONArray("importedAssets").toList { it.toMediaAsset() },
  lastPlaybackPositionMs = optLong("lastPlaybackPositionMs"),
  autosaveVersion = optLong("autosaveVersion", 1L),
)

private fun MediaAsset.toJson() = JSONObject()
  .put("id", id)
  .put("uri", uri)
  .put("type", type.name)
  .put("displayName", displayName)
  .put("durationMs", durationMs)
  .put("sizeBytes", sizeBytes)

private fun JSONObject.toMediaAsset() = MediaAsset(
  id = getString("id"),
  uri = getString("uri"),
  type = enumValueOf(getString("type")),
  displayName = getString("displayName"),
  durationMs = optLong("durationMs"),
  sizeBytes = optLong("sizeBytes"),
)

private fun Timeline.toJson() = JSONObject()
  .put("durationMs", durationMs)
  .put("tracks", JSONArray(tracks.map { it.toJson() }))
  .put("playheadMs", playheadMs)
  .put("zoomLevel", zoomLevel.toDouble())
  .put("selectedClipId", selectedClipId)
  .put("selectedTool", selectedTool.name)
  .put("isPlaying", isPlaying)

private fun JSONObject.toTimeline() = Timeline(
  durationMs = optLong("durationMs"),
  tracks = optJSONArray("tracks").toList { it.toTimelineTrack() },
  playheadMs = optLong("playheadMs"),
  zoomLevel = optDouble("zoomLevel", 1.0).toFloat(),
  selectedClipId = optNullableString("selectedClipId"),
  selectedTool = enumValueOf(optString("selectedTool", EditorTool.Edit.name)),
  isPlaying = optBoolean("isPlaying"),
)

private fun TimelineTrack.toJson() = JSONObject()
  .put("id", id)
  .put("type", type.name)
  .put("name", name)
  .put("orderIndex", orderIndex)
  .put("clips", JSONArray(clips.map { it.toJson() }))
  .put("isMuted", isMuted)
  .put("isLocked", isLocked)

private fun JSONObject.toTimelineTrack() = TimelineTrack(
  id = getString("id"),
  type = enumValueOf(getString("type")),
  name = getString("name"),
  orderIndex = optInt("orderIndex"),
  clips = optJSONArray("clips").toList { it.toTimelineClip() },
  isMuted = optBoolean("isMuted"),
  isLocked = optBoolean("isLocked"),
)

private fun TimelineClip.toJson() = JSONObject()
  .put("id", id)
  .put("assetId", assetId)
  .put("clipType", clipType.name)
  .put("title", title)
  .put("startMs", startMs)
  .put("durationMs", durationMs)
  .put("sourceInMs", sourceInMs)
  .put("zIndex", zIndex)
  .put("transform", transform.toJson())
  .put("videoProperties", videoProperties.toJson())
  .put("audioProperties", audioProperties.toJson())
  .put("textProperties", textProperties.toJson())
  .put("filterAdjustments", filterAdjustments.toJson())
  .put("keyframes", JSONArray(keyframes.map { it.toJson() }))

private fun JSONObject.toTimelineClip() = TimelineClip(
  id = getString("id"),
  assetId = optNullableString("assetId"),
  clipType = enumValueOf(getString("clipType")),
  title = getString("title"),
  startMs = optLong("startMs"),
  durationMs = optLong("durationMs"),
  sourceInMs = optLong("sourceInMs"),
  zIndex = optInt("zIndex"),
  transform = optJSONObject("transform")?.toTransformState() ?: TransformState(),
  videoProperties = optJSONObject("videoProperties")?.toVideoProperties() ?: VideoProperties(),
  audioProperties = optJSONObject("audioProperties")?.toAudioProperties() ?: AudioProperties(),
  textProperties = optJSONObject("textProperties")?.toTextProperties() ?: TextProperties(),
  filterAdjustments = optJSONObject("filterAdjustments")?.toFilterAdjustmentSet() ?: FilterAdjustmentSet(),
  keyframes = optJSONArray("keyframes").toList { it.toKeyframe() },
)

private fun TransformState.toJson() = JSONObject().put("positionX", positionX.toDouble()).put("positionY", positionY.toDouble()).put("scale", scale.toDouble()).put("rotationDegrees", rotationDegrees.toDouble()).put("opacity", opacity.toDouble()).put("flipHorizontal", flipHorizontal).put("flipVertical", flipVertical)
private fun JSONObject.toTransformState() = TransformState(optDouble("positionX", 0.5).toFloat(), optDouble("positionY", 0.5).toFloat(), optDouble("scale", 1.0).toFloat(), optDouble("rotationDegrees").toFloat(), optDouble("opacity", 1.0).toFloat(), optBoolean("flipHorizontal"), optBoolean("flipVertical"))
private fun VideoProperties.toJson() = JSONObject().put("speed", speed.toDouble()).put("isFreezeFrame", isFreezeFrame).put("sourceAudioMuted", sourceAudioMuted)
private fun JSONObject.toVideoProperties() = VideoProperties(optDouble("speed", 1.0).toFloat(), optBoolean("isFreezeFrame"), optBoolean("sourceAudioMuted"))
private fun AudioProperties.toJson() = JSONObject().put("volume", volume.toDouble()).put("fadeInMs", fadeInMs).put("fadeOutMs", fadeOutMs).put("loopEnabled", loopEnabled)
private fun JSONObject.toAudioProperties() = AudioProperties(optDouble("volume", 1.0).toFloat(), optLong("fadeInMs"), optLong("fadeOutMs"), optBoolean("loopEnabled"))
private fun TextProperties.toJson() = JSONObject().put("content", content).put("fontSizeSp", fontSizeSp.toDouble()).put("color", color).put("backgroundColor", backgroundColor).put("strokeEnabled", strokeEnabled).put("shadowEnabled", shadowEnabled).put("alignment", alignment).put("animation", animation)
private fun JSONObject.toTextProperties() = TextProperties(optString("content", "Clipy Studio"), optDouble("fontSizeSp", 28.0).toFloat(), optString("color", "#F4F6FF"), optNullableString("backgroundColor"), optBoolean("strokeEnabled"), optBoolean("shadowEnabled"), optString("alignment", "Center"), optString("animation", "Fade"))
private fun FilterAdjustmentSet.toJson() = JSONObject().put("filterId", filterId).put("brightness", brightness.toDouble()).put("contrast", contrast.toDouble()).put("saturation", saturation.toDouble()).put("exposure", exposure.toDouble()).put("temperature", temperature.toDouble()).put("sharpness", sharpness.toDouble()).put("vignette", vignette.toDouble())
private fun JSONObject.toFilterAdjustmentSet() = FilterAdjustmentSet(optNullableString("filterId"), optDouble("brightness", 1.0).toFloat(), optDouble("contrast", 1.0).toFloat(), optDouble("saturation", 1.0).toFloat(), optDouble("exposure").toFloat(), optDouble("temperature").toFloat(), optDouble("sharpness").toFloat(), optDouble("vignette").toFloat())
private fun Keyframe.toJson() = JSONObject().put("id", id).put("timeMs", timeMs).put("property", property.name).put("value", value.toDouble())
private fun JSONObject.toKeyframe() = Keyframe(optString("id", UUID.randomUUID().toString()), optLong("timeMs"), enumValueOf(optString("property", KeyframeProperty.Opacity.name)), optDouble("value").toFloat())
private fun ExportSettings.toJson() = JSONObject().put("format", format).put("resolution", resolution.name).put("fps", fps).put("bitrateMbps", bitrateMbps.toDouble()).put("qualityPreset", qualityPreset.name).put("saveToGallery", saveToGallery)
private fun JSONObject.toExportSettings() = ExportSettings(optString("format", "MP4"), enumValueOf(optString("resolution", ExportResolution.P1080.name)), optInt("fps", 30), optDouble("bitrateMbps", 12.0).toFloat(), enumValueOf(optString("qualityPreset", QualityPreset.Balanced.name)), optBoolean("saveToGallery", true))
private fun JSONObject.optNullableString(name: String): String? = if (isNull(name)) null else optString(name)
private fun <T> JSONArray?.toList(transform: (JSONObject) -> T): List<T> = if (this == null) emptyList() else List(length()) { transform(getJSONObject(it)) }

data class AppState(
  val languageCode: LanguageCode = LanguageCode.En,
  val hasCompletedIntro: Boolean = false,
  val projects: List<Project> = emptyList(),
  val activeProjectId: String? = null,
  val selectedImports: List<MediaAsset> = emptyList(),
  val defaultExportSettings: ExportSettings = ExportSettings(),
  val exportJob: ExportJob? = null,
  val cacheUsageMb: Int = 128,
  val undoStack: List<UndoRedoCommand> = emptyList(),
  val redoStack: List<UndoRedoCommand> = emptyList(),
) {
  val activeProject: Project? get() = projects.firstOrNull { it.id == activeProjectId }
}

enum class LanguageCode { En, Vi }
enum class CanvasRatio(val label: String) { Portrait("9:16"), Square("1:1"), Landscape("16:9"), FourFive("4:5"), Original("Original") }
enum class MediaType(val label: String) { Video("Video"), Image("Image"), Audio("Audio") }
enum class TrackType(val label: String) { Video("Video"), Audio("Audio"), Text("Text"), Sticker("Sticker"), Effect("Effect"), Overlay("Overlay") }
enum class ClipType { Video, Image, Audio, Text, Sticker, Effect, Overlay }
enum class EditorTool(val label: String) { Edit("Edit"), Audio("Audio"), Text("Text"), Sticker("Sticker"), Overlay("Overlay"), Filter("Filter"), Effect("Effect"), Transition("Transition"), Canvas("Canvas"), Speed("Speed"), Export("Export") }
enum class AudioSource(val label: String) { DeviceMusic("Device Music"), BuiltInMusic("Built-in Music"), ExtractedAudio("Extracted Audio"), SoundEffect("Sound Effects") }
enum class KeyframeProperty { PositionX, PositionY, Scale, Rotation, Opacity }
enum class ExportResolution(val label: String) { P720("720p"), P1080("1080p"), P2K("2K"), P4K("4K") }
enum class QualityPreset(val label: String) { Balanced("Balanced"), High("High"), Studio("Studio") }
enum class ExportStatus { Idle, Running, Complete, Cancelled, Failed }

enum class ClipAction(val label: String) {
  Rotate("Rotate"), Flip("Flip"), OpacityDown("Opacity -"), OpacityUp("Opacity +"), SpeedUp("Speed +"), SpeedDown("Speed -"), Mute("Mute"), Filter("Cinematic"), Keyframe("Add keyframe"), VolumeDown("Volume -"), VolumeUp("Volume +"), Fade("Fade"), Loop("Loop"), Replace("Replace"), Crop("Crop")
}

data class Project(
  val id: String,
  val name: String,
  val createdAt: Long,
  val updatedAt: Long,
  val thumbnailUri: String? = null,
  val durationMs: Long = 0,
  val canvasRatio: CanvasRatio = CanvasRatio.Portrait,
  val timeline: Timeline = Timeline.defaultTimeline(),
  val importedAssets: List<MediaAsset> = emptyList(),
  val lastPlaybackPositionMs: Long = 0,
  val autosaveVersion: Long = 1,
)

data class MediaAsset(
  val id: String,
  val uri: String,
  val type: MediaType,
  val displayName: String,
  val durationMs: Long,
  val sizeBytes: Long,
)

data class Timeline(
  val durationMs: Long,
  val tracks: List<TimelineTrack>,
  val playheadMs: Long = 0,
  val zoomLevel: Float = 1f,
  val selectedClipId: String? = null,
  val selectedTool: EditorTool = EditorTool.Edit,
  val isPlaying: Boolean = false,
) {
  fun recalculateDuration() = copy(durationMs = tracks.flatMap { it.clips }.maxOfOrNull { it.startMs + it.durationMs } ?: 0L)

  companion object {
    fun defaultTimeline(): Timeline {
      val videoClip = TimelineClip(id = UUID.randomUUID().toString(), title = "Starter video", clipType = ClipType.Video, durationMs = 6_000)
      return Timeline(
        durationMs = 10_000,
        selectedClipId = videoClip.id,
        tracks = listOf(
          TimelineTrack(UUID.randomUUID().toString(), TrackType.Video, "Video 1", 0, listOf(videoClip)),
          TimelineTrack(UUID.randomUUID().toString(), TrackType.Audio, "Music", 1, listOf(TimelineClip.audioClip())),
          TimelineTrack(UUID.randomUUID().toString(), TrackType.Text, "Text", 2, listOf(TimelineClip.textClip())),
          TimelineTrack(UUID.randomUUID().toString(), TrackType.Sticker, "Stickers", 3, emptyList()),
          TimelineTrack(UUID.randomUUID().toString(), TrackType.Overlay, "Overlay", 4, emptyList()),
          TimelineTrack(UUID.randomUUID().toString(), TrackType.Effect, "Effects", 5, listOf(TimelineClip.effectClip())),
        ),
      )
    }
  }
}

data class TimelineTrack(val id: String, val type: TrackType, val name: String, val orderIndex: Int, val clips: List<TimelineClip>, val isMuted: Boolean = false, val isLocked: Boolean = false)

data class TimelineClip(
  val id: String,
  val assetId: String? = null,
  val clipType: ClipType,
  val title: String,
  val startMs: Long = 0,
  val durationMs: Long,
  val sourceInMs: Long = 0,
  val zIndex: Int = 0,
  val transform: TransformState = TransformState(),
  val videoProperties: VideoProperties = VideoProperties(),
  val audioProperties: AudioProperties = AudioProperties(),
  val textProperties: TextProperties = TextProperties(),
  val filterAdjustments: FilterAdjustmentSet = FilterAdjustmentSet(),
  val keyframes: List<Keyframe> = emptyList(),
) {
  companion object {
    fun audioClip() = TimelineClip(UUID.randomUUID().toString(), clipType = ClipType.Audio, title = "Lo-fi beat", durationMs = 10_000, audioProperties = AudioProperties(volume = 0.72f, fadeInMs = 500, fadeOutMs = 800))
    fun textClip() = TimelineClip(UUID.randomUUID().toString(), clipType = ClipType.Text, title = "Title overlay", startMs = 1_000, durationMs = 4_000, textProperties = TextProperties(content = "Make it pop"))
    fun effectClip() = TimelineClip(UUID.randomUUID().toString(), clipType = ClipType.Effect, title = "Soft glow", startMs = 2_000, durationMs = 3_000)
  }
}

data class TransformState(val positionX: Float = 0.5f, val positionY: Float = 0.5f, val scale: Float = 1f, val rotationDegrees: Float = 0f, val opacity: Float = 1f, val flipHorizontal: Boolean = false, val flipVertical: Boolean = false)
data class VideoProperties(val speed: Float = 1f, val isFreezeFrame: Boolean = false, val sourceAudioMuted: Boolean = false)
data class AudioProperties(val volume: Float = 1f, val fadeInMs: Long = 0, val fadeOutMs: Long = 0, val loopEnabled: Boolean = false)
data class TextProperties(val content: String = "Clipy Studio", val fontSizeSp: Float = 28f, val color: String = "#F4F6FF", val backgroundColor: String? = "#7C5CFF", val strokeEnabled: Boolean = false, val shadowEnabled: Boolean = true, val alignment: String = "Center", val animation: String = "Fade")
data class FilterAdjustmentSet(val filterId: String? = null, val brightness: Float = 1f, val contrast: Float = 1f, val saturation: Float = 1f, val exposure: Float = 0f, val temperature: Float = 0f, val sharpness: Float = 0f, val vignette: Float = 0f)
data class Keyframe(val id: String = UUID.randomUUID().toString(), val timeMs: Long, val property: KeyframeProperty, val value: Float)
data class UndoRedoCommand(val description: String, val before: Project, val after: Project)
data class ExportSettings(val format: String = "MP4", val resolution: ExportResolution = ExportResolution.P1080, val fps: Int = 30, val bitrateMbps: Float = 12f, val qualityPreset: QualityPreset = QualityPreset.Balanced, val saveToGallery: Boolean = true)
data class ExportJob(val id: String = UUID.randomUUID().toString(), val projectId: String, val settings: ExportSettings, val status: ExportStatus = ExportStatus.Idle, val progressPercent: Int = 0, val outputUri: String? = null, val errorMessage: String? = null)

fun Long.asTimecode(): String {
  val totalSeconds = this / 1_000
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return "%02d:%02d".format(minutes, seconds)
}

fun Long.asSizeLabel(): String = if (this >= 1_000_000) "%.1f MB".format(this / 1_000_000f) else "${this / 1_000} KB"
