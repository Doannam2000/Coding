package com.example.clipy.clipy.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.room.Room
import com.example.clipy.clipy.model.CropRatio
import com.example.clipy.clipy.model.ExportFormat
import com.example.clipy.clipy.model.ExportJobState
import com.example.clipy.clipy.model.ExportPlan
import com.example.clipy.clipy.model.ExportRecord
import com.example.clipy.clipy.model.ExportRecordUi
import com.example.clipy.clipy.model.Mp4Quality
import com.example.clipy.clipy.model.ProjectDraft
import com.example.clipy.clipy.model.SaveBehavior
import com.example.clipy.clipy.model.buildExportPlan
import com.example.clipy.clipy.model.sanitizeTimeline
import com.example.clipy.clipy.model.sanitizeOutputName
import com.example.clipy.clipy.model.snapTimelineMs
import com.example.clipy.clipy.model.shouldPersistUri
import com.example.clipy.clipy.model.UserPreferences
import com.example.clipy.clipy.model.WatermarkPosition
import com.example.clipy.clipy.model.validateExport
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
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
    val name = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "Selected clip" } ?: "Selected clip"
    val durationMs = readDurationMs(uri) ?: 12_000L
    if (shouldPersistUri(uri.toString())) {
      runCatching {
        appContext.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    }
    updateDraft {
      it.copy(
        sourceUri = uri.toString(),
        displayName = name,
        sourceDurationMs = durationMs,
        trimStartMs = 0L,
        trimEndMs = durationMs.coerceAtMost(12_000L),
        playheadMs = 0L,
        timelineZoom = 1f,
        outputName = sanitizeOutputName(name.substringBeforeLast('.')),
        exportFormat = ExportFormat.Gif,
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
      it.copy(playheadMs = snapTimelineMs(timeline.playheadMs).coerceIn(timeline.trimStartMs, timeline.trimEndMs))
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
      it.copy(
        trimStartMs = snapTimelineMs(timeline.trimStartMs),
        trimEndMs = snapTimelineMs(timeline.trimEndMs).coerceAtLeast(timeline.trimStartMs + 250L),
        playheadMs = timeline.playheadMs.coerceIn(timeline.trimStartMs, timeline.trimEndMs),
      )
    }
  }

  suspend fun completeOnboarding(languageCode: String) {
    preferenceRepository.setLanguage(com.example.clipy.clipy.model.AppLanguage.entries.first { it.code == languageCode })
    preferenceRepository.setOnboardingCompleted(true)
  }

  suspend fun updateSettings(prefs: UserPreferences) {
    preferenceRepository.setLanguage(com.example.clipy.clipy.model.AppLanguage.entries.first { it.code == prefs.languageCode })
    preferenceRepository.updateDefaults(
      gifFps = prefs.defaultGifFps,
      gifResolution = prefs.defaultGifResolution,
      quality = prefs.defaultMp4Quality,
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
        displayName = record.outputName,
        sourceDurationMs = record.durationMs.coerceAtLeast(1_000L),
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
        gifFps = record.gifFps ?: it.gifFps,
        gifResolution = record.gifResolution ?: it.gifResolution,
        mp4Quality = record.mp4Quality?.let { quality -> Mp4Quality.entries.firstOrNull { entry -> entry.name == quality } } ?: it.mp4Quality,
        outputName = "${record.outputName}_redo",
      )
    }
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
    val plan = buildExportPlan(sanitizedDraft)
    exportState.value = ExportJobState(jobId = sanitizedDraft.outputName, projectId = sanitizedDraft.id, progressPercent = 0, currentStep = "Preparing timeline", isCancellable = true, status = "Running")
    val progressMarks = listOf(10, 26, 46, 70, 90, 100)
    val steps = progressMarks.zip(plan.progressSteps)
    steps.forEach { (progress, step) ->
      currentCoroutineContext().ensureActive()
      exportState.value = exportState.value.copy(progressPercent = progress, currentStep = step)
      delay(220)
    }

    val outputUri = saveExportArtifact(sanitizedDraft, saveBehavior, plan)
    val estimatedFileSize = estimateFileSize(sanitizedDraft)
    dao.insertRecord(
      ExportRecord(
        sourceUri = sanitizedDraft.sourceUri,
        outputUri = outputUri,
        outputName = sanitizedDraft.outputName,
        format = sanitizedDraft.exportFormat.name.uppercase(Locale.getDefault()),
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
        fileSizeBytes = estimatedFileSize,
        createdAt = System.currentTimeMillis(),
      )
    )
    exportState.value = exportState.value.copy(
      progressPercent = 100,
      currentStep = exportCompletionStep(saveBehavior),
      status = "Success",
      isCancellable = false,
      outputUri = outputUri,
    )
  }

  fun cancelExport() {
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
        isMuted = prefs.defaultMuteEnabled,
      )
    }
  }

  private fun estimateFileSize(draft: ProjectDraft): Long {
    val durationSeconds = ((draft.trimEndMs - draft.trimStartMs).coerceAtLeast(500L) / 1000f)
    return if (draft.exportFormat == ExportFormat.Gif) {
      (durationSeconds * draft.gifFps * 22_000L).toLong().coerceAtLeast(900_000L)
    } else {
      val qualityMultiplier = when (draft.mp4Quality) {
        Mp4Quality.Fast -> 1L
        Mp4Quality.Balanced -> 2L
        Mp4Quality.Crisp -> 3L
      }
      (durationSeconds * qualityMultiplier * 1_700_000L).toLong().coerceAtLeast(2_400_000L)
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

  private suspend fun saveExportArtifact(draft: ProjectDraft, saveBehavior: SaveBehavior, plan: ExportPlan): String {
    val targetUri = createExportUri(draft, saveBehavior)
    val payload = buildExportPayload(draft, plan)
    val resolver = appContext.contentResolver
    resolver.openOutputStream(targetUri)?.use { stream ->
      stream.write(payload.toByteArray())
      stream.flush()
    } ?: throw FileNotFoundException("Unable to open export destination.")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
      resolver.update(targetUri, values, null, null)
    }
    return targetUri.toString()
  }

  private fun createExportUri(draft: ProjectDraft, saveBehavior: SaveBehavior): Uri {
    val resolver = appContext.contentResolver
    val fileName = buildOutputFileName(draft)
    val relativePath = when (draft.exportFormat) {
      ExportFormat.Gif -> Environment.DIRECTORY_PICTURES + "/Clipy"
      ExportFormat.Mp4 -> Environment.DIRECTORY_MOVIES + "/Clipy"
    }
    val values = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
      put(MediaStore.MediaColumns.MIME_TYPE, if (draft.exportFormat == ExportFormat.Gif) "image/gif" else "video/mp4")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
      }
      if (saveBehavior == SaveBehavior.ShareFirst) {
        put(MediaStore.MediaColumns.TITLE, "Clipy share export")
      }
    }
    val collection = if (draft.exportFormat == ExportFormat.Gif) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    return requireNotNull(resolver.insert(collection, values))
  }

  private fun buildExportPayload(draft: ProjectDraft, plan: ExportPlan): String {
    val safeWatermark = draft.watermarkText.ifBlank { "none" }
    val fpsOrQuality = if (draft.exportFormat == ExportFormat.Gif) {
      "gif_fps=${draft.gifFps};gif_resolution=${draft.gifResolution}"
    } else {
      "mp4_quality=${draft.mp4Quality.name}"
    }
    return buildString {
      appendLine("clipy_export=1")
      appendLine("source=${draft.sourceUri}")
      appendLine("trim=${draft.trimStartMs}-${draft.trimEndMs}")
      appendLine("crop=${draft.cropRatio.label}")
      appendLine("speed=${draft.speedMultiplier}")
      appendLine("muted=${draft.isMuted}")
      appendLine("reverse=${draft.isReversed}")
      appendLine("boomerang=${draft.isBoomerang}")
      appendLine("watermark=${safeWatermark}")
      appendLine("watermark_position=${draft.watermarkPosition.name}")
      appendLine(fpsOrQuality)
      appendLine("ffmpeg=${plan.ffmpegCommand}")
      appendLine("warnings=${plan.warnings.joinToString(" | ").ifBlank { "none" }}")
    }
  }

  private fun buildOutputFileName(draft: ProjectDraft): String {
    val extension = if (draft.exportFormat == ExportFormat.Gif) ".gif" else ".mp4"
    return sanitizeOutputName(draft.outputName) + extension
  }

  data class AppSnapshot(
    val preferences: UserPreferences,
    val draft: ProjectDraft,
    val exportJobState: ExportJobState,
    val history: List<com.example.clipy.clipy.model.ExportRecordUi>,
  )

  companion object {
    @Volatile private var instance: ClipyRepository? = null

    fun getInstance(context: Context): ClipyRepository =
      instance ?: synchronized(this) { instance ?: ClipyRepository(context).also { instance = it } }
  }
}
