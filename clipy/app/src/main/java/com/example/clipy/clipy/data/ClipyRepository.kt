package com.example.clipy.clipy.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import com.example.clipy.clipy.model.CropRatio
import com.example.clipy.clipy.model.ExportFormat
import com.example.clipy.clipy.model.ExportJobState
import com.example.clipy.clipy.model.ExportRecord
import com.example.clipy.clipy.model.ExportRecordUi
import com.example.clipy.clipy.model.Mp4Quality
import com.example.clipy.clipy.model.ProjectDraft
import com.example.clipy.clipy.model.SaveBehavior
import com.example.clipy.clipy.model.UserPreferences
import com.example.clipy.clipy.model.WatermarkPosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
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
    updateDraft {
      it.copy(
        sourceUri = uri.toString(),
        displayName = name,
        trimStartMs = 0L,
        trimEndMs = 12000L,
        outputName = name.substringBeforeLast('.').ifBlank { "clipy_export" },
        exportFormat = ExportFormat.Gif,
      )
    }
  }

  fun canExport(): Boolean = draftState.value.sourceUri.isNotBlank()

  fun blockExport() {
    exportState.value = ExportJobState(
      jobId = draftState.value.outputName,
      projectId = draftState.value.id,
      currentStep = "Import required",
      status = "Blocked",
      errorMessage = "Import a video before exporting.",
    )
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

  suspend fun startMockExport() {
    val current = draftState.value
    exportState.value = ExportJobState(jobId = current.outputName, projectId = current.id, progressPercent = 0, currentStep = "Preparing timeline", isCancellable = true, status = "Running")
    val steps = listOf("Preparing timeline", "Applying crop and speed", "Rendering local output", "Saving result")
    steps.forEachIndexed { index, step ->
      delay(450)
      exportState.value = exportState.value.copy(progressPercent = ((index + 1) * 25), currentStep = step)
    }
    val outputUri = current.sourceUri.ifBlank { "content://clipy/exports/${current.outputName}" }
    val statusLabel = if (current.sourceUri.isBlank()) "Preview only" else "Saved"
    val estimatedFileSize = estimateFileSize(current)
    dao.insertRecord(
      ExportRecord(
        sourceUri = current.sourceUri,
        outputUri = outputUri,
        outputName = current.outputName,
        format = current.exportFormat.name.uppercase(Locale.getDefault()),
        durationMs = current.trimEndMs - current.trimStartMs,
        cropRatio = current.cropRatio.label,
        speedMultiplier = current.speedMultiplier,
        isMuted = current.isMuted,
        isReversed = current.isReversed,
        isBoomerang = current.isBoomerang,
        watermarkText = current.watermarkText,
        gifFps = current.gifFps.takeIf { current.exportFormat == ExportFormat.Gif },
        gifResolution = current.gifResolution.takeIf { current.exportFormat == ExportFormat.Gif },
        mp4Quality = current.mp4Quality.name.takeIf { current.exportFormat == ExportFormat.Mp4 },
        status = statusLabel,
        fileSizeBytes = estimatedFileSize,
        createdAt = System.currentTimeMillis(),
      )
    )
    exportState.value = exportState.value.copy(
      progressPercent = 100,
      currentStep = exportCompletionStep(current.sourceUri.isNotBlank()),
      status = "Success",
      isCancellable = false,
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

  private suspend fun exportCompletionStep(hasOutputUri: Boolean): String {
    val saveLabel = when (preferences.first().saveBehavior) {
      SaveBehavior.AppFolder -> "Saved to Clipy folder"
      SaveBehavior.PromptEachTime -> "Ready to choose save location"
      SaveBehavior.ShareFirst -> "Ready to share"
    }
    return if (hasOutputUri) saveLabel else "Preview ready"
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
