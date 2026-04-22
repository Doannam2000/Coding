package com.example.clipy.clipy.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppLanguage(val code: String) {
  English("en"),
  Vietnamese("vi"),
}

enum class CropRatio(val label: String) {
  Square("1:1"),
  Portrait("4:5"),
  Story("9:16"),
  Landscape("16:9"),
}

enum class WatermarkPosition(val label: String) {
  TopLeft("Top Left"),
  TopRight("Top Right"),
  BottomLeft("Bottom Left"),
  BottomRight("Bottom Right"),
  Center("Center"),
}

enum class ExportFormat { Gif, Mp4 }

enum class Mp4Quality(val label: String) {
  Fast("Fast 720p"),
  Balanced("Balanced 1080p"),
  Crisp("Crisp 4K source"),
}

enum class SaveBehavior(val label: String) {
  AppFolder("Save to Clipy folder"),
  PromptEachTime("Ask every export"),
  ShareFirst("Open share sheet first"),
}

data class ProjectDraft(
  val id: String = "draft",
  val sourceUri: String = "",
  val displayName: String = "No clip selected",
  val trimStartMs: Long = 0L,
  val trimEndMs: Long = 12000L,
  val cropRatio: CropRatio = CropRatio.Story,
  val speedMultiplier: Float = 1f,
  val isMuted: Boolean = false,
  val isReversed: Boolean = false,
  val isBoomerang: Boolean = false,
  val watermarkText: String = "",
  val watermarkPosition: WatermarkPosition = WatermarkPosition.BottomRight,
  val exportFormat: ExportFormat = ExportFormat.Gif,
  val gifFps: Int = 18,
  val gifResolution: String = "720p",
  val mp4Quality: Mp4Quality = Mp4Quality.Balanced,
  val outputName: String = "clipy_export",
  val lastUpdatedAt: Long = System.currentTimeMillis(),
)

data class UserPreferences(
  val languageCode: String = AppLanguage.English.code,
  val defaultGifFps: Int = 18,
  val defaultGifResolution: String = "720p",
  val defaultMp4Quality: Mp4Quality = Mp4Quality.Balanced,
  val defaultMuteEnabled: Boolean = false,
  val defaultCropRatio: CropRatio = CropRatio.Story,
  val saveBehavior: SaveBehavior = SaveBehavior.AppFolder,
  val onboardingCompleted: Boolean = false,
)

data class ExportJobState(
  val jobId: String = "",
  val projectId: String = "draft",
  val progressPercent: Int = 0,
  val currentStep: String = "Preparing",
  val isCancellable: Boolean = false,
  val status: String = "Idle",
  val errorMessage: String? = null,
)

@Entity(tableName = "export_records")
data class ExportRecord(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val sourceUri: String,
  val outputUri: String,
  val outputName: String,
  val format: String,
  val durationMs: Long,
  val cropRatio: String,
  val speedMultiplier: Float,
  val isMuted: Boolean,
  val isReversed: Boolean,
  val isBoomerang: Boolean,
  val watermarkText: String,
  val gifFps: Int?,
  val gifResolution: String?,
  val mp4Quality: String?,
  val status: String,
  val fileSizeBytes: Long,
  val createdAt: Long,
)

data class ExportRecordUi(
  val id: Long,
  val sourceUri: String,
  val outputName: String,
  val formatLabel: String,
  val timestampLabel: String,
  val detailLabel: String,
  val outputUri: String,
  val cropRatio: CropRatio,
  val speedMultiplier: Float,
  val isMuted: Boolean,
  val isReversed: Boolean,
  val isBoomerang: Boolean,
  val watermarkText: String,
  val gifFps: Int?,
  val gifResolution: String?,
  val mp4Quality: Mp4Quality?,
)
