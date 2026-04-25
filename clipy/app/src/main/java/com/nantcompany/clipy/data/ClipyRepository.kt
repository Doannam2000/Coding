package com.nantcompany.clipy.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.room.Room
import com.nantcompany.clipy.model.AppLanguage
import com.nantcompany.clipy.model.CropRatio
import com.nantcompany.clipy.model.ExportFormat
import com.nantcompany.clipy.model.ExportJobState
import com.nantcompany.clipy.model.ExportRecord
import com.nantcompany.clipy.model.ExportRecordUi
import com.nantcompany.clipy.model.Mp4Quality
import com.nantcompany.clipy.model.OutputFormat
import com.nantcompany.clipy.model.OutputResolution
import com.nantcompany.clipy.model.ProjectDraft
import com.nantcompany.clipy.model.SaveBehavior
import com.nantcompany.clipy.model.sanitizeTimeline
import com.nantcompany.clipy.model.sanitizeOutputName
import com.nantcompany.clipy.model.snapToNearestKeyframe
import com.nantcompany.clipy.model.shouldPersistUri
import com.nantcompany.clipy.model.UserPreferences
import com.nantcompany.clipy.model.validateExport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ClipyRepository private constructor(context: Context) {
  private val appContext = context.applicationContext
  private val preferenceRepository = PreferenceRepository(appContext)
  private val database = Room.databaseBuilder(appContext, ClipyDatabase::class.java, "clipy.db").fallbackToDestructiveMigration().build()
  private val dao = database.clipyDao()
  private val exporter = AndroidVideoExporter(appContext)
  private val draftState = MutableStateFlow(ProjectDraft())
  private val exportState = MutableStateFlow(ExportJobState())

  val preferences: Flow<UserPreferences> = preferenceRepository.preferences
  val draft: StateFlow<ProjectDraft> = draftState.asStateFlow()
  val exportJob: StateFlow<ExportJobState> = exportState.asStateFlow()
  val history =
    dao.observeHistory().map { items ->
      val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
      items.map {
        ExportRecordUi(
          id = it.id,
          sourceUri = it.sourceUri,
          outputName = it.outputName,
          formatLabel = it.format,
          timestampLabel = formatter.format(Date(it.createdAt)),
          detailLabel = "${it.durationMs / 1000}s • ${it.cropRatio} • x${it.speedMultiplier}",
          outputUri = it.outputUri,
          cropRatio = CropRatio.entries.firstOrNull { ratio -> ratio.label == it.cropRatio } ?: CropRatio.Story,
          speedMultiplier = it.speedMultiplier,
          isMuted = it.isMuted,
          isReversed = it.isReversed,
          isBoomerang = it.isBoomerang,
          watermarkText = it.watermarkText,
          gifFps = it.gifFps,
          gifResolution = it.gifResolution,
          mp4Quality = it.mp4Quality?.let { quality -> Mp4Quality.entries.firstOrNull { entry -> entry.name == quality } },
        )
      }
    }

  val appSnapshot = combine(preferences, draft, exportJob, history) { prefs, currentDraft, currentJob, exportHistory ->
    AppSnapshot(preferences = prefs, draft = currentDraft, exportJobState = currentJob, history = exportHistory)
  }

  fun updateDraft(transform: (ProjectDraft) -> ProjectDraft) {
    draftState.value = transform(draftState.value).copy(lastUpdatedAt = System.currentTimeMillis())
  }

  fun loadVideo(uri: Uri) {
    loadSelectedMedia(uri = uri, mediaType = "video")
  }

  fun loadSelectedMedia(uri: Uri, mediaType: String, displayNameHint: String? = null) {
    val name = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "Selected clip" } ?: "Selected clip"
    val safeMediaType = mediaType.lowercase(Locale.getDefault())
    val resolvedName = displayNameHint?.ifBlank { null } ?: name
    val durationMs = if (safeMediaType == "video") readDurationMs(uri) ?: 12_000L else 12_000L
    val keyframeTimes = readKeyframeTimes(durationMs)
    if (shouldPersistUri(uri.toString())) {
      runCatching {
        appContext.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    }
    updateDraft {
      it.copy(
        sourceUri = uri.toString(),
        sourceMediaType = safeMediaType,
        displayName = resolvedName,
        sourceDurationMs = durationMs,
        keyframeTimesMs = keyframeTimes,
        trimStartMs = 0L,
        trimEndMs = durationMs.coerceAtMost(12_000L),
        playheadMs = 0L,
        timelineZoom = 1f,
        outputName = sanitizeOutputName(resolvedName.substringBeforeLast('.')),
        exportFormat = ExportFormat.Gif,
        outputFormat = OutputFormat.MP4,
        outputResolution = OutputResolution.P1080,
        outputFps = 30,
      )
    }
  }

  fun canExport(): Boolean = draftState.value.validateExport().isValid

  fun blockExport() {
    val validation = draftState.value.validateExport()
    exportState.value = ExportJobState(
      jobId = draftState.value.outputName,
      projectId = draftState.value.id,
      currentStep = "Import required",
      status = "Blocked",
      errorMessage = validation.message ?: "Import a video before exporting.",
    )
  }

  fun setPlayhead(positionMs: Long) {
    updateDraft {
      val timeline = sanitizeTimeline(it.sourceDurationMs, it.trimStartMs, it.trimEndMs, positionMs, it.timelineZoom)
      val snapped = snapToNearestKeyframe(timeline.playheadMs, it.keyframeTimesMs, fallbackStepMs = 33L)
      it.copy(playheadMs = snapped.coerceIn(timeline.trimStartMs, timeline.trimEndMs))
    }
  }

  fun stepPlayhead(direction: Int) {
    val delta = if (direction >= 0) 33L else -33L
    setPlayhead(draftState.value.playheadMs + delta)
  }

  fun updateTimelineZoom(zoom: Float) {
    updateDraft {
      val timeline = sanitizeTimeline(it.sourceDurationMs, it.trimStartMs, it.trimEndMs, it.playheadMs, zoom)
      it.copy(timelineZoom = timeline.zoom)
    }
  }

  fun updateTrimWindow(startMs: Long = draftState.value.trimStartMs, endMs: Long = draftState.value.trimEndMs) {
    updateDraft {
      val timeline = sanitizeTimeline(it.sourceDurationMs, startMs, endMs, it.playheadMs, it.timelineZoom)
      val snappedStart = snapToNearestKeyframe(timeline.trimStartMs, it.keyframeTimesMs)
      val snappedEnd = snapToNearestKeyframe(timeline.trimEndMs, it.keyframeTimesMs).coerceAtLeast(snappedStart + 250L)
      it.copy(
        trimStartMs = snappedStart,
        trimEndMs = snappedEnd.coerceAtMost(it.sourceDurationMs),
        playheadMs = timeline.playheadMs.coerceIn(snappedStart, snappedEnd.coerceAtMost(it.sourceDurationMs)),
      )
    }
  }

  suspend fun completeOnboarding(languageCode: String) {
    preferenceRepository.setLanguage(AppLanguage.entries.first { it.code == languageCode })
    preferenceRepository.setOnboardingCompleted(true)
  }

  suspend fun updateSettings(prefs: UserPreferences) {
    preferenceRepository.setLanguage(AppLanguage.entries.first { it.code == prefs.languageCode })
      preferenceRepository.updateDefaults(
        gifFps = prefs.defaultGifFps,
        gifResolution = prefs.defaultGifResolution,
        quality = prefs.defaultMp4Quality,
        outputFormat = prefs.defaultOutputFormat,
        outputResolution = prefs.defaultOutputResolution,
        outputFps = prefs.defaultOutputFps,
        cropRatio = prefs.defaultCropRatio,
        saveBehavior = prefs.saveBehavior,
        defaultMuteEnabled = prefs.defaultMuteEnabled,
    )
    applyDefaultSettingsToDraft()
  }

  suspend fun reuseHistoryRecord(recordId: Long) {
    val record = dao.getRecordById(recordId) ?: return
    updateDraft {
      it.copy(
        sourceUri = record.sourceUri,
        sourceMediaType = "video",
        displayName = record.outputName,
        sourceDurationMs = record.durationMs.coerceAtLeast(1_000L),
        keyframeTimesMs = readKeyframeTimes(record.durationMs.coerceAtLeast(1_000L)),
        trimStartMs = 0L,
        trimEndMs = record.durationMs.coerceAtLeast(1_000L),
        playheadMs = 0L,
        cropRatio = CropRatio.entries.firstOrNull { ratio -> ratio.label == record.cropRatio } ?: it.cropRatio,
        speedMultiplier = record.speedMultiplier,
        isMuted = record.isMuted,
        isReversed = record.isReversed,
        isBoomerang = record.isBoomerang,
        watermarkText = record.watermarkText,
        exportFormat = if (record.format.equals("GIF", ignoreCase = true)) ExportFormat.Gif else ExportFormat.Mp4,
        outputFormat = if (record.format.equals("MOV", ignoreCase = true)) OutputFormat.MOV else OutputFormat.MP4,
        gifFps = record.gifFps ?: it.gifFps,
        gifResolution = record.gifResolution ?: it.gifResolution,
        mp4Quality = record.mp4Quality?.let { quality -> Mp4Quality.entries.firstOrNull { entry -> entry.name == quality } } ?: it.mp4Quality,
        outputResolution = it.outputResolution,
        outputFps = it.outputFps,
        outputName = "${record.outputName}_redo",
      )
    }
  }

  suspend fun clearHistory() {
    dao.clearHistory()
  }

  suspend fun startExport() {
    val current = draftState.value
    val validation = current.validateExport()
    if (!validation.isValid) {
      blockExport()
      return
    }

    val sanitizedDraft = current.copy(outputName = sanitizeOutputName(current.outputName))
    val saveBehavior = preferences.first().saveBehavior
    exportState.value = ExportJobState(jobId = sanitizedDraft.outputName, projectId = sanitizedDraft.id, progressPercent = 0, currentStep = "Preparing timeline", isCancellable = true, status = "Running")
    runCatching {
      exporter.export(sanitizedDraft, saveBehavior) { progress, step ->
        exportState.value = exportState.value.copy(progressPercent = progress, currentStep = step, status = "Running", isCancellable = true)
      }
    }.onSuccess { result ->
      dao.insertRecord(
        ExportRecord(
          sourceUri = sanitizedDraft.sourceUri,
          outputUri = result.outputUri,
          outputName = sanitizedDraft.outputName,
          format = if (sanitizedDraft.exportFormat == ExportFormat.Gif) {
            "GIF"
          } else {
            sanitizedDraft.outputFormat.label
          },
          durationMs = sanitizedDraft.trimEndMs - sanitizedDraft.trimStartMs,
          cropRatio = sanitizedDraft.cropRatio.label,
          speedMultiplier = sanitizedDraft.speedMultiplier,
          isMuted = sanitizedDraft.isMuted,
          isReversed = sanitizedDraft.isReversed,
          isBoomerang = sanitizedDraft.isBoomerang,
          watermarkText = sanitizedDraft.watermarkText,
          gifFps = sanitizedDraft.gifFps.takeIf { sanitizedDraft.exportFormat == ExportFormat.Gif },
          gifResolution = sanitizedDraft.gifResolution.takeIf { sanitizedDraft.exportFormat == ExportFormat.Gif },
          mp4Quality = sanitizedDraft.mp4Quality.name.takeIf { sanitizedDraft.exportFormat == ExportFormat.Mp4 },
          status = "Saved",
          fileSizeBytes = result.fileSizeBytes,
          createdAt = System.currentTimeMillis(),
        )
      )
      exportState.value = exportState.value.copy(
        progressPercent = 100,
        currentStep = exportCompletionStep(saveBehavior),
        status = "Success",
        isCancellable = false,
        outputUri = result.outputUri,
        errorMessage = null,
      )
    }.onFailure { error ->
      val cancelled = error is kotlinx.coroutines.CancellationException
      exportState.value = exportState.value.copy(
        status = if (cancelled) "Cancelled" else "Failed",
        currentStep = if (cancelled) "Export cancelled" else "Export failed",
        isCancellable = false,
        errorMessage = if (cancelled) null else (error.message ?: "Unable to finish export."),
      )
    }
  }

  fun cancelExport() {
    exporter.cancel()
    exportState.value = exportState.value.copy(status = "Cancelled", currentStep = "Export cancelled", isCancellable = false, errorMessage = null)
  }

  suspend fun applyDefaultSettingsToDraft() {
    val prefs = preferences.first()
    updateDraft {
      it.copy(
        cropRatio = prefs.defaultCropRatio,
        gifFps = prefs.defaultGifFps,
        gifResolution = prefs.defaultGifResolution,
        mp4Quality = prefs.defaultMp4Quality,
        outputFormat = prefs.defaultOutputFormat,
        outputResolution = prefs.defaultOutputResolution,
        outputFps = prefs.defaultOutputFps,
        isMuted = prefs.defaultMuteEnabled,
      )
    }
  }

  private fun exportCompletionStep(saveBehavior: SaveBehavior): String {
    return when (saveBehavior) {
      SaveBehavior.AppFolder -> "Saved to Clipy folder"
      SaveBehavior.PromptEachTime -> "Ready to choose save location"
      SaveBehavior.ShareFirst -> "Ready to share"
    }
  }

  private fun readDurationMs(uri: Uri): Long? {
    val retriever = MediaMetadataRetriever()
    return runCatching {
      retriever.setDataSource(appContext, uri)
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    }.getOrNull().also {
      runCatching { retriever.release() }
    }
  }

  private fun readKeyframeTimes(durationMs: Long): List<Long> {
    val stepMs = when {
      durationMs <= 6_000L -> 250L
      durationMs <= 20_000L -> 500L
      else -> 1_000L
    }
    return buildList {
      var cursor = 0L
      while (cursor < durationMs) {
        add(cursor)
        cursor += stepMs
      }
      add(durationMs)
    }
  }

  data class AppSnapshot(
    val preferences: UserPreferences,
    val draft: ProjectDraft,
    val exportJobState: ExportJobState,
    val history: List<ExportRecordUi>,
  )

  companion object {
    @Volatile private var instance: ClipyRepository? = null

    fun getInstance(context: Context): ClipyRepository =
      instance ?: synchronized(this) { instance ?: ClipyRepository(context).also { instance = it } }
  }
}
