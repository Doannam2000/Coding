package com.example.clipy.clipy.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.example.clipy.clipy.model.CropRatio
import com.example.clipy.clipy.model.ExportFormat
import com.example.clipy.clipy.model.Mp4Quality
import com.example.clipy.clipy.model.ProjectDraft
import com.example.clipy.clipy.model.SaveBehavior
import com.example.clipy.clipy.model.WatermarkPosition
import com.example.clipy.clipy.model.resolutionPreset
import com.example.clipy.clipy.model.sanitizeOutputName
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidVideoExporter(private val context: Context) {
  private var activeSession: FFmpegSession? = null

  suspend fun export(
    draft: ProjectDraft,
    saveBehavior: SaveBehavior,
    onProgress: (Int, String) -> Unit,
  ): ExportResult {
    val sourceUri = Uri.parse(draft.sourceUri)
    val sourcePath = context.copySourceToCache(sourceUri)
    val tempOutput = File.createTempFile(sanitizeOutputName(draft.outputName), if (draft.exportFormat == ExportFormat.Gif) ".gif" else ".mp4", context.cacheDir)
    val command = buildCommand(draft, sourcePath.absolutePath, tempOutput.absolutePath)
    try {
      executeCommand(command, draft, onProgress)
      val outputUri = context.copyExportToMediaStore(tempOutput, draft, saveBehavior)
      return ExportResult(outputUri = outputUri.toString(), fileSizeBytes = tempOutput.length())
    } finally {
      activeSession = null
      runCatching { sourcePath.delete() }
      runCatching { tempOutput.delete() }
    }
  }

  fun cancel() {
    activeSession?.cancel()
  }

  private suspend fun executeCommand(
    command: String,
    draft: ProjectDraft,
    onProgress: (Int, String) -> Unit,
  ) {
    val durationMs = (draft.trimEndMs - draft.trimStartMs).coerceAtLeast(250L)
    suspendCancellableCoroutine<Unit> { continuation ->
      onProgress(5, "Opening source")
      val session = FFmpegKit.executeAsync(
        command,
        { completedSession ->
          activeSession = null
          val state = completedSession.state
          val returnCode = completedSession.returnCode
          when {
            ReturnCode.isSuccess(returnCode) -> {
              if (continuation.isActive) continuation.resume(Unit)
            }
            ReturnCode.isCancel(returnCode) || state.name.equals("FAILED", ignoreCase = true) && !continuation.isActive -> {
              if (continuation.isActive) continuation.resumeWithException(CancellationException("Export cancelled"))
            }
            else -> {
              val message = completedSession.allLogsAsString.ifBlank { "FFmpeg export failed." }
              if (continuation.isActive) continuation.resumeWithException(IllegalStateException(message))
            }
          }
        },
        { log ->
          val message = log.message.lowercase(Locale.getDefault())
          when {
            "palette" in message -> onProgress(66, "Rendering palette")
            "frame=" in message -> Unit
            "video:" in message -> onProgress(92, "Finalizing file")
          }
        },
        { statistics ->
          val processedMs = statistics.time.toLong().coerceAtLeast(0L)
          val fraction = (processedMs.toDouble() / durationMs.toDouble()).coerceIn(0.0, 1.0)
          val progress = (8 + (fraction * 84)).toInt().coerceIn(8, 92)
          onProgress(progress, if (draft.exportFormat == ExportFormat.Gif) "Encoding GIF" else "Encoding MP4")
        },
      )
      activeSession = session
      continuation.invokeOnCancellation { session.cancel() }
    }
  }

  private fun buildCommand(draft: ProjectDraft, inputPath: String, outputPath: String): String {
    val resolution = if (draft.exportFormat == ExportFormat.Gif) {
      resolutionPreset(draft.gifResolution, draft.cropRatio)
    } else {
      when (draft.mp4Quality) {
        Mp4Quality.Fast -> resolutionPreset("720p", draft.cropRatio)
        Mp4Quality.Balanced -> resolutionPreset("1080p", draft.cropRatio)
        Mp4Quality.Crisp -> resolutionPreset("1080p", draft.cropRatio)
      }
    }
    val videoFilter = buildVideoFilter(draft, resolution.width, resolution.height)
    val audioArgs = buildAudioArgs(draft)
    return if (draft.exportFormat == ExportFormat.Gif) {
      listOf(
        "-y",
        "-ss", formatSeconds(draft.trimStartMs),
        "-to", formatSeconds(draft.trimEndMs),
        "-i", quote(inputPath),
        "-vf", quote("$videoFilter,fps=${draft.gifFps},split[s0][s1];[s0]palettegen=stats_mode=single[p];[s1][p]paletteuse=dither=bayer:bayer_scale=3"),
        "-loop", "0",
        quote(outputPath),
      ).joinToString(" ", prefix = "-hide_banner ")
    } else {
      listOf(
        "-y",
        "-ss", formatSeconds(draft.trimStartMs),
        "-to", formatSeconds(draft.trimEndMs),
        "-i", quote(inputPath),
        "-vf", quote(videoFilter),
        *audioArgs.toTypedArray(),
        "-movflags", "+faststart",
        *mp4QualityArgs(draft.mp4Quality).toTypedArray(),
        quote(outputPath),
      ).joinToString(" ", prefix = "-hide_banner ")
    }
  }

  private fun buildVideoFilter(draft: ProjectDraft, width: Int, height: Int): String {
    val filters = mutableListOf<String>()
    if (draft.isReversed) {
      filters += "reverse"
    }
    val speed = (1f / draft.speedMultiplier).coerceAtLeast(0.1f)
    filters += "setpts=${"%.4f".format(Locale.US, speed)}*PTS"
    filters += cropFilter(draft.cropRatio)
    filters += "scale=$width:$height:force_original_aspect_ratio=decrease"
    filters += "pad=$width:$height:(ow-iw)/2:(oh-ih)/2:color=0x0B0D12"
    if (draft.watermarkText.isNotBlank()) {
      filters += drawTextFilter(draft)
    }
    return if (draft.isBoomerang) {
      "${filters.joinToString(",")},split[a][b];[b]reverse[br];[a][br]concat=n=2:v=1:a=0"
    } else {
      filters.joinToString(",")
    }
  }

  private fun cropFilter(cropRatio: CropRatio): String =
    when (cropRatio) {
      CropRatio.Square -> "crop='min(iw,ih)':'min(iw,ih)'"
      CropRatio.Portrait -> "crop='min(iw,ih*4/5)':'min(ih,iw*5/4)'"
      CropRatio.Story -> "crop='min(iw,ih*9/16)':'min(ih,iw*16/9)'"
      CropRatio.Landscape -> "crop='min(iw,ih*16/9)':'min(ih,iw*9/16)'"
    }

  private fun drawTextFilter(draft: ProjectDraft): String {
    val escaped = draft.watermarkText.replace("\\", "\\\\").replace(":", "\\:").replace("'", "\\\\'")
    val (x, y) = when (draft.watermarkPosition) {
      WatermarkPosition.TopLeft -> "24" to "24"
      WatermarkPosition.TopRight -> "w-text_w-24" to "24"
      WatermarkPosition.BottomLeft -> "24" to "h-text_h-24"
      WatermarkPosition.BottomRight -> "w-text_w-24" to "h-text_h-24"
      WatermarkPosition.Center -> "(w-text_w)/2" to "(h-text_h)/2"
    }
    return "drawtext=text='$escaped':x=$x:y=$y:fontsize=28:fontcolor=white:box=1:boxcolor=black@0.32:boxborderw=14"
  }

  private fun buildAudioArgs(draft: ProjectDraft): List<String> {
    if (draft.isMuted) return listOf("-an")
    val filters = mutableListOf<String>()
    if (draft.isReversed) filters += "areverse"
    if (draft.speedMultiplier != 1f) {
      filters += atempoChain(draft.speedMultiplier)
    }
    val args = mutableListOf<String>()
    if (filters.isNotEmpty()) {
      args += listOf("-af", quote(filters.joinToString(",")))
    }
    args += listOf("-c:a", "aac", "-b:a", "192k")
    return args
  }

  private fun mp4QualityArgs(quality: Mp4Quality): List<String> =
    when (quality) {
      Mp4Quality.Fast -> listOf("-preset", "veryfast", "-crf", "28")
      Mp4Quality.Balanced -> listOf("-preset", "medium", "-crf", "23")
      Mp4Quality.Crisp -> listOf("-preset", "slow", "-crf", "18")
    }

  private fun atempoChain(speedMultiplier: Float): String {
    var remaining = speedMultiplier.coerceIn(0.5f, 2f).toDouble()
    val parts = mutableListOf<String>()
    while (remaining > 2.0) {
      parts += "atempo=2.0"
      remaining /= 2.0
    }
    while (remaining < 0.5) {
      parts += "atempo=0.5"
      remaining /= 0.5
    }
    parts += "atempo=${"%.3f".format(Locale.US, remaining)}"
    return parts.joinToString(",")
  }

  private fun quote(value: String): String = "\"${value.replace("\"", "\\\"")}\""

  private fun formatSeconds(valueMs: Long): String = "%.3f".format(Locale.US, valueMs / 1000f)
}

data class ExportResult(
  val outputUri: String,
  val fileSizeBytes: Long,
)

private fun Context.copySourceToCache(sourceUri: Uri): File {
  val extension = contentResolver.getType(sourceUri)?.substringAfterLast('/')?.ifBlank { "mp4" } ?: "mp4"
  val target = File.createTempFile("clipy_source_", ".${extension}", cacheDir)
  contentResolver.openInputStream(sourceUri)?.use { input ->
    target.outputStream().use { output -> input.copyTo(output) }
  } ?: throw IllegalStateException("Unable to read source media.")
  return target
}

private fun Context.copyExportToMediaStore(file: File, draft: ProjectDraft, saveBehavior: SaveBehavior): Uri {
  val resolver = contentResolver
  val relativePath = when (draft.exportFormat) {
    ExportFormat.Gif -> Environment.DIRECTORY_PICTURES + "/Clipy"
    ExportFormat.Mp4 -> Environment.DIRECTORY_MOVIES + "/Clipy"
  }
  val mimeType = if (draft.exportFormat == ExportFormat.Gif) "image/gif" else "video/mp4"
  val values = ContentValues().apply {
    put(MediaStore.MediaColumns.DISPLAY_NAME, sanitizeOutputName(draft.outputName) + if (draft.exportFormat == ExportFormat.Gif) ".gif" else ".mp4")
    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
      put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    if (saveBehavior == SaveBehavior.ShareFirst) {
      put(MediaStore.MediaColumns.TITLE, "Clipy share export")
    }
  }
  val collection = if (draft.exportFormat == ExportFormat.Gif) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
  val targetUri = requireNotNull(resolver.insert(collection, values))
  resolver.openOutputStream(targetUri)?.use { output -> file.inputStream().use { it.copyTo(output) } }
    ?: throw IllegalStateException("Unable to write export destination.")
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    resolver.update(targetUri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
  }
  return targetUri
}
