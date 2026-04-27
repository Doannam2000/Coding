package com.example.clipystudio.ui.main.models

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.BoxScope

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clipystudio.data.*
import com.example.clipystudio.filter.*
import com.example.clipystudio.theme.*
import com.example.clipystudio.*
import com.example.clipystudio.ui.main.*
import com.example.clipystudio.ui.main.models.*
import com.example.clipystudio.ui.main.screens.*
import com.example.clipystudio.ui.main.editor.*
import com.example.clipystudio.ui.main.editor.components.*
import com.example.clipystudio.ui.main.editor.panels.*
import com.example.clipystudio.ui.main.editor.timeline.*
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.*

data class TimelineClipPreviewState(
  val clipId: String,
  val startTimeMs: Long,
  val durationMs: Long,
  val snapLabel: String? = null,
  val isValid: Boolean = true,
  val trimHandle: TrimHandle? = null,
  val snapTimeMs: Long? = null,
)


data class TimelineGestureOverlayState(
  val zoomLabel: String? = null,
  val snapLabel: String? = null,
  val snapTimeMs: Long? = null,
  val resistanceFraction: Float = 0f,
  val mode: TimelineGestureMode = TimelineGestureMode.IDLE,
  val autoScrollDirection: com.example.clipystudio.data.AutoScrollDirection = com.example.clipystudio.data.AutoScrollDirection.NONE,
  val invalidFeedback: Boolean = false,
)


data class PreviewGestureFeedback(
  val owner: GestureOwner = GestureOwner.NONE,
  val showCenterXGuide: Boolean = false,
  val showCenterYGuide: Boolean = false,
  val showBoundaryGuide: Boolean = false,
  val angleLabel: String? = null,
  val chipLabel: String? = null,
  val pendingHaptic: HapticEvent? = null,
)


enum class PreviewSurfaceState {
  NoMedia,
  Loading,
  ImageReady,
  VideoReady,
  InvalidUri,
  LoadFailed,
}


enum class ClipVisualState {
  Selected,
  Active,
  Inactive,
  Invalid,
}


enum class VideoPreviewLoadState {
  Loading,
  Ready,
  Failed,
}


val EditorChromeBackground = Color(0xFF070B14)
val EditorChromeSurface = Color(0xFF101826)
val EditorChromeSurfaceAlt = Color(0xFF0B1220)
val EditorChromeSurfaceLow = Color(0xFF151F31)
val EditorChromeBorder = Color(0xFF22314A)
val EditorChromePrimary = Color(0xFF7AA2FF)
val EditorChromeAudio = Color(0xFF132A24)
val EditorChromeAudioAccent = Color(0xFF4FE0A6)
val EditorChromeMuted = Color(0xFFB7C4DD)
val EditorChromeText = Color(0xFFF3F6FF)
val EditorChromeTextMuted = Color(0xFF7E8CA6)

val EditorTimelineGrid = Color(0xFF162033)

val EditorChromeDanger = Color(0xFFFF6B6B)


data class BottomNavItem(val tool: EditorTool, val label: String, val glyph: String)


fun topBarChevronGlyph() = "‹"


fun toolbarGlyph(action: String): String = when (action) {
  "undo" -> "↶"
  "redo" -> "↷"
  "split" -> "✂"
  "speed" -> "◌"
  "anim" -> "◇"
  "volume" -> "∿"
  "delete" -> "⌫"
  else -> "•"
}


fun navGlyph(tool: EditorTool): String = when (tool) {
  EditorTool.Edit -> "▣"
  EditorTool.Audio -> "♪"
  EditorTool.Text -> "T"
  EditorTool.Effect -> "✦"
  EditorTool.Overlay -> "▤"
  EditorTool.Sticker -> "☺"
  EditorTool.Filter -> "◐"
  EditorTool.Transition -> "⇄"
  EditorTool.Canvas -> "□"
  EditorTool.Speed -> "⟲"
  EditorTool.Export -> "↑"
}


fun clipTypeBadge(clipType: ClipType): String = when (clipType) {
  ClipType.Image -> "IMG"
  ClipType.Video -> "VID"
  ClipType.Audio -> "AUD"
  ClipType.Text -> "TXT"
  ClipType.Sticker -> "STK"
  ClipType.Effect -> "FX"
  ClipType.Overlay -> "OVR"
}


@Composable
fun Timeline.findClip(clipId: String?): TimelineClip? =
  clipId?.let { id -> tracks.flatMap { it.clips }.firstOrNull { it.id == id } }


@Composable
fun Timeline.activePreviewClip(): TimelineClip? {
  val allClips = tracks.flatMap { it.clips }
  val lastFrameTimeMs = playheadMs.takeIf { it < durationMs }
    ?: (durationMs - 1L).coerceAtLeast(0L)
  return findClip(selectedClipId)?.takeIf { it.clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Overlay) }
    ?: TimelineEngine.resolveActiveComposition(this).video?.clipId?.let { activeId -> allClips.firstOrNull { it.id == activeId } }
    ?: allClips.firstOrNull { it.clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Overlay) && lastFrameTimeMs in it.startMs until (it.startMs + it.durationMs).coerceAtLeast(it.startMs + 1L) }
}


@Composable
fun Timeline.selectedRealClip(): TimelineClip? =
  findClip(selectedClipId)?.takeIf { it.clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Audio, ClipType.Overlay, ClipType.Text, ClipType.Sticker, ClipType.Effect) }


@Composable
fun TimelineClip.isVisualMediaClip(): Boolean = clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Overlay)


@Composable
fun TimelineClip.hasUsableMediaUri(): Boolean {
  if (!isVisualMediaClip()) return false
  val uri = mediaUri?.trim().orEmpty()
  if (uri.isBlank()) return false
  val parsedUri = runCatching { Uri.parse(uri) }.getOrNull() ?: return false
  return !parsedUri.scheme.isNullOrBlank()
}


data class ImportPermissionNotice(
  val title: String,
  val body: String,
  val confirmLabel: String,
  val dismissLabel: String,
  val openSettings: Boolean = false,
)


data class IntroPage(val title: String, val body: String, val color: Color)


data class ThumbnailFrame(val clipId: String, val bitmap: Bitmap?)


data class UriMetadata(val displayName: String?, val sizeBytes: Long?, val mimeType: String?, val durationMs: Long?)


fun stageColor(state: StageState): Color = when (state) {
  StageState.PENDING -> StudioSurface
  StageState.ACTIVE -> StudioSecondary
  StageState.COMPLETE -> StudioPrimary
  StageState.WARNING -> StudioAccent
  StageState.FAILED -> StudioDanger
  StageState.CANCELLED -> StudioTextMuted
}


fun Context.readUriMetadataSafely(uri: Uri): UriMetadata? = runCatching { readUriMetadata(uri) }.getOrNull()


fun Context.persistReadPermission(uri: Uri): Boolean {
  val grants = contentResolver.persistedUriPermissions
  if (grants.any { it.uri == uri && it.isReadPermission }) return true
  return runCatching {
    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }.isSuccess
}


fun Context.readUriMetadata(uri: Uri): UriMetadata {
  var displayName: String? = null
  var sizeBytes: Long? = null
  val mimeType = contentResolver.getType(uri)
  val durationMs = readMediaDurationMs(uri, mimeType)
  requireNotNull(contentResolver.openInputStream(uri)) { "Selected media is not readable." }.use { }
  contentResolver.query(uri, null, null, null, null)?.use { cursor ->
    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
    if (cursor.moveToFirst()) {
      if (nameIndex >= 0) displayName = cursor.getString(nameIndex)
      if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
    }
  }
  return UriMetadata(displayName = displayName, sizeBytes = sizeBytes, mimeType = mimeType, durationMs = durationMs)
}


fun Context.readMediaDurationMs(uri: Uri, mimeType: String?): Long? {
  if (mimeType?.startsWith("image") == true) return 3_000L
  if (mimeType?.startsWith("video") != true && mimeType?.startsWith("audio") != true) return null
  return runCatching {
    MediaMetadataRetriever().use { retriever ->
      retriever.setDataSource(this, uri)
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    }
  }.getOrNull()
}


fun Context.loadThumbnailBitmap(mediaUri: String, thumbnailTimeMs: Long, widthPx: Int, heightPx: Int): Bitmap? {
  if (mediaUri.startsWith("local://")) return null
  val uri = runCatching { Uri.parse(mediaUri) }.getOrNull() ?: return null
  return runCatching {
    val mimeType = resolveMimeType(uri)
    when {
      mimeType?.startsWith("image") == true -> {
        contentResolver.openInputStream(uri)?.use { input ->
          BitmapFactory.decodeStream(input)
        }
      }
      mimeType?.startsWith("video") == true -> {
        MediaMetadataRetriever().use { retriever ->
          retriever.setDataSource(this, uri)
          retriever.getFrameAtTime(thumbnailTimeMs * 1_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
      }
      else -> {
        // Some picker-backed content URIs do not expose a MIME type, so try image decode first and
        // fall back to video frame extraction before giving up.
        contentResolver.openInputStream(uri)?.use { input ->
          BitmapFactory.decodeStream(input)
        } ?: MediaMetadataRetriever().use { retriever ->
          retriever.setDataSource(this, uri)
          retriever.getFrameAtTime(thumbnailTimeMs * 1_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
      }
    }?.let { source -> Bitmap.createScaledBitmap(source, widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), true) }
  }.getOrNull()
}


fun Context.resolvePreviewSurfaceState(clip: TimelineClip?): PreviewSurfaceState {
  if (clip == null) return PreviewSurfaceState.NoMedia
  val uri = clip.mediaUri?.trim()
  if (uri.isNullOrEmpty()) return PreviewSurfaceState.InvalidUri
  val parsedUri = runCatching { Uri.parse(uri) }.getOrNull() ?: return PreviewSurfaceState.InvalidUri
  if (parsedUri.scheme.isNullOrBlank()) return PreviewSurfaceState.InvalidUri
  if (!canOpenPreviewUri(parsedUri)) return PreviewSurfaceState.LoadFailed
  val mimeType = resolveMimeType(parsedUri)
  return when {
    clip.clipType == ClipType.Video -> PreviewSurfaceState.VideoReady
    clip.clipType == ClipType.Image -> PreviewSurfaceState.ImageReady
    clip.clipType == ClipType.Overlay && mimeType?.startsWith("video") == true -> PreviewSurfaceState.VideoReady
    clip.clipType == ClipType.Overlay -> PreviewSurfaceState.ImageReady
    else -> PreviewSurfaceState.LoadFailed
  }
}


fun Context.canOpenPreviewUri(uri: Uri): Boolean {
  return when (uri.scheme?.lowercase()) {
    "content" -> runCatching {
      contentResolver.openAssetFileDescriptor(uri, "r")?.use { true }
        ?: contentResolver.openInputStream(uri)?.use { true }
        ?: false
    }.getOrDefault(false)
    "file" -> runCatching {
      val path = uri.path ?: return@runCatching false
      File(path).exists()
    }.getOrDefault(false)
    "android.resource" -> true
    else -> false
  }
}


fun Context.resolveMimeType(uri: Uri): String? {
  contentResolver.getType(uri)?.let { return it }
  val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())?.lowercase().orEmpty()
  return extension.takeIf { it.isNotBlank() }?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
}
