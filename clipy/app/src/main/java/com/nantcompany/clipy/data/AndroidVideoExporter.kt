package com.nantcompany.clipy.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.nantcompany.clipy.model.ExportFormat
import com.nantcompany.clipy.model.OutputFormat
import com.nantcompany.clipy.model.ProjectDraft
import com.nantcompany.clipy.model.SaveBehavior
import com.nantcompany.clipy.model.sanitizeOutputName
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

class AndroidVideoExporter(private val context: Context) {
  private val cancelled = AtomicBoolean(false)

  suspend fun export(
    draft: ProjectDraft,
    saveBehavior: SaveBehavior,
    onProgress: (Int, String) -> Unit,
  ): ExportResult {
    cancelled.set(false)
    val sourceUri = Uri.parse(draft.sourceUri)
    val tempOutput =
      File.createTempFile(
        sanitizeOutputName(draft.outputName),
        when {
          draft.exportFormat == ExportFormat.Gif -> ".gif"
          draft.outputFormat == OutputFormat.MOV -> ".mov"
          else -> ".mp4"
        },
        context.cacheDir,
      )

    try {
      writeExportStub(sourceUri, draft, tempOutput, onProgress)
      val outputUri = context.copyExportToMediaStore(tempOutput, draft, saveBehavior)
      return ExportResult(outputUri = outputUri.toString(), fileSizeBytes = tempOutput.length())
    } finally {
      runCatching { tempOutput.delete() }
    }
  }

  fun cancel() {
    cancelled.set(true)
  }

  private suspend fun writeExportStub(
    sourceUri: Uri,
    draft: ProjectDraft,
    outputFile: File,
    onProgress: (Int, String) -> Unit,
  ) {
    val steps =
      if (draft.exportFormat == ExportFormat.Gif) {
        listOf(
          8 to "Opening source",
          32 to "Preparing GIF export",
          68 to "Rendering GIF placeholder",
          92 to "Saving export",
        )
      } else {
        val containerLabel = if (draft.outputFormat == OutputFormat.MOV) "MOV" else "MP4"
        listOf(
          8 to "Opening source",
          28 to "Preparing $containerLabel export",
          72 to "Copying source media",
          92 to "Saving export",
        )
      }

    for ((progress, label) in steps) {
      currentCoroutineContext().ensureActive()
      if (cancelled.get()) throw CancellationException("Export cancelled")
      onProgress(progress, label)
      delay(180)
    }

    if (draft.exportFormat == ExportFormat.Gif) {
      outputFile.outputStream().use { stream ->
        stream.write(MINIMAL_GIF_1X1)
      }
      return
    }

    context.contentResolver.openInputStream(sourceUri)?.use { input ->
      outputFile.outputStream().use { output ->
        input.copyTo(output)
      }
    } ?: throw IllegalStateException("Unable to read source media.")
  }

  companion object {
    private val MINIMAL_GIF_1X1 =
      byteArrayOf(
        71,
        73,
        70,
        56,
        57,
        97,
        1,
        0,
        1,
        0,
        -128,
        0,
        0,
        0,
        0,
        0,
        -1,
        -1,
        -1,
        33,
        -7,
        4,
        1,
        0,
        0,
        1,
        0,
        44,
        0,
        0,
        0,
        0,
        1,
        0,
        1,
        0,
        0,
        2,
        2,
        68,
        1,
        0,
        59,
      )
  }
}

data class ExportResult(
  val outputUri: String,
  val fileSizeBytes: Long,
)

private fun Context.copyExportToMediaStore(file: File, draft: ProjectDraft, saveBehavior: SaveBehavior): Uri {
  val resolver = contentResolver
  val relativePath =
    when (draft.exportFormat) {
      ExportFormat.Gif -> Environment.DIRECTORY_PICTURES + "/Clipy"
      ExportFormat.Mp4 -> Environment.DIRECTORY_MOVIES + "/Clipy"
    }
  val mimeType = when {
    draft.exportFormat == ExportFormat.Gif -> "image/gif"
    draft.outputFormat == OutputFormat.MOV -> "video/quicktime"
    else -> "video/mp4"
  }
  val values =
    ContentValues().apply {
      put(
        MediaStore.MediaColumns.DISPLAY_NAME,
        sanitizeOutputName(draft.outputName) + when {
          draft.exportFormat == ExportFormat.Gif -> ".gif"
          draft.outputFormat == OutputFormat.MOV -> ".mov"
          else -> ".mp4"
        },
      )
      put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
      }
      if (saveBehavior == SaveBehavior.ShareFirst) {
        put(MediaStore.MediaColumns.TITLE, "Clipy share export")
      }
    }
  val collection =
    if (draft.exportFormat == ExportFormat.Gif) {
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    } else {
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }
  val targetUri = requireNotNull(resolver.insert(collection, values))
  resolver.openOutputStream(targetUri)?.use { output ->
    file.inputStream().use { it.copyTo(output) }
  } ?: throw IllegalStateException("Unable to write export destination.")
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    resolver.update(targetUri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
  }
  return targetUri
}
