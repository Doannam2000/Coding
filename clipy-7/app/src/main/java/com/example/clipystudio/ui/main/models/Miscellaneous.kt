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

enum class PreviewMediaLoadState {
  Idle,
  Failed,
}


suspend fun animateTimelineSettle(
  startOffsetPx: Float,
  timeline: Timeline,
  viewportWidthPx: Float,
  onScroll: (Float, Float) -> Unit,
  onPreviewSeek: (Long, PreviewSeekSource, Boolean) -> Unit,
  onResistance: (Float) -> Unit,
) {
  val frames = TimelineEngine.settleScrollFrames(
    scrollOffsetPx = startOffsetPx,
    durationMs = timeline.durationMs,
    zoomScale = timeline.zoomLevel,
    pixelsPerSecond = timeline.pixelsPerSecond,
    viewportWidthPx = viewportWidthPx,
  )
  frames.forEach { frame ->
    onScroll(frame.offsetPx, viewportWidthPx)
    onPreviewSeek(TimelineEngine.timeFromScroll(frame.offsetPx, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs, viewportWidthPx), PreviewSeekSource.TIMELINE_FLING, false)
    onResistance(frame.resistanceFraction)
    delay(16)
  }
  val exact = TimelineEngine.exactFrameSeekFromScroll(startOffsetPx, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs, viewportWidthPx, TimelineGestureMode.FLINGING, finalFrame = true)
  onPreviewSeek(exact.currentTimeMs, PreviewSeekSource.TIMELINE_FLING, true)
  onResistance(0f)
}


@Composable
fun String.toShareUri(context: Context): Uri {
  val parsed = Uri.parse(this)
  if (parsed.scheme == "content") return parsed
  if (parsed.scheme == "file") {
    val path = parsed.path ?: return parsed
    val file = java.io.File(path)
    return runCatching { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) }.getOrDefault(parsed)
  }
  return parsed
}


@Composable
fun BoxScope.TimelineGestureReadout(timecode: String, zoomLabel: String?, snapLabel: String?, resistanceFraction: Float) {
  val label = buildString {
    append(timecode)
    zoomLabel?.let {
      append(" · ")
      append(it)
    }
    snapLabel?.let {
      append(" · ")
      append(it)
    }
    if (resistanceFraction > 0.01f) {
      append(" · edge")
    }
  }
  Text(
    label,
    modifier = Modifier
      .align(Alignment.TopStart)
      .padding(start = 10.dp, top = 8.dp)
      .clip(RoundedCornerShape(999.dp))
      .background(StudioSurface.copy(alpha = 0.90f))
      .padding(horizontal = 10.dp, vertical = 4.dp),
    color = StudioSecondary,
    fontSize = 11.sp,
    fontWeight = FontWeight.Bold,
  )
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  MyApplicationTheme { MainScreen(onItemClick = {}) }
}