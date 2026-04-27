package com.example.clipystudio.ui.main.editor.timeline

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

@Composable
fun TimelineHeader(timeline: Timeline, contentWidth: Int, viewportWidthPx: Float, onSeek: (Long) -> Unit, onScroll: (Float, Float) -> Unit, onZoom: (Float, Float, Float) -> Unit, onGestureZoomLabel: (String?) -> Unit, onGestureTimecode: (String?) -> Unit, onTransformStart: () -> Unit, onTransformFrame: (Float) -> Unit, physics: TimelinePhysicsConfig = TimelineEngine.DefaultPhysics) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(28.dp)
      .background(EditorChromeSurface)
      .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text("TIME", modifier = Modifier.width(58.dp), fontSize = 10.sp, color = EditorChromeMuted.copy(alpha = 0.74f), fontWeight = FontWeight.Bold)
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .pointerInput(timeline.id, timeline.zoomLevel, viewportWidthPx, timeline.scrollOffsetPx) {
          var gestureScroll = timeline.scrollOffsetPx
          var gestureZoom = timeline.zoomLevel
          detectTransformGestures { centroid, pan, zoom, _ ->
            onTransformStart()
            val focalX = centroid.x.coerceIn(0f, viewportWidthPx)
            if (abs(zoom - 1f) > 0.01f) {
              val previousGestureZoom = gestureZoom
              val zoomResult = TimelineEngine.zoomAroundFocal(
                timeline = timeline.copy(scrollOffsetPx = gestureScroll, zoomLevel = gestureZoom),
                zoomDelta = zoom,
                focalXpx = focalX,
                viewportWidthPx = viewportWidthPx,
              )
              gestureZoom = zoomResult.newZoomScale
              gestureScroll = zoomResult.newScrollOffsetPx
              onZoom((zoomResult.newZoomScale / previousGestureZoom.coerceAtLeast(0.001f)) - 1f, focalX, viewportWidthPx)
              onGestureZoomLabel("${(zoomResult.newZoomScale * 100).roundToInt()}%")
              onGestureTimecode(zoomResult.currentTimeMs.asTimecode())
            }
            if (abs(pan.x) > 0.2f) {
              val update = TimelineEngine.dragTimeline(gestureScroll, pan.x, timeline.durationMs, gestureZoom, timeline.pixelsPerSecond, viewportWidthPx, physics)
              gestureScroll = update.nextOffsetPx
              onTransformFrame(update.resistanceFraction)
              onGestureTimecode(update.currentTimeMs.asTimecode())
              onScroll(gestureScroll, viewportWidthPx)
            } else {
              onTransformFrame(0f)
            }
          }
        }
        .drawBehind {
          val pxPerMs = timeline.pixelsPerSecond * timeline.zoomLevel / 1_000f
          val secondaryColor = Color(0xFF333333)
          
          // Small ticks
          for (tick in 0..(timeline.durationMs + 2000L) step 100L) {
              val x = tick * pxPerMs - timeline.scrollOffsetPx
              if (x in 0f..size.width) {
                  val height = if (tick % 1000L == 0L) 8.dp.toPx() else 4.dp.toPx()
                  drawLine(secondaryColor, Offset(x, size.height - height), Offset(x, size.height), 1.dp.toPx())
              }
          }
        }
    ) {
      val pxPerMs = timeline.pixelsPerSecond * timeline.zoomLevel / 1_000f
      (0..timeline.durationMs step 5_000L).forEach { tick ->
          val x = (tick * pxPerMs - timeline.scrollOffsetPx).roundToInt()
          if (x in 0..viewportWidthPx.toInt()) {
              Text(
                  tick.asTimecode(),
                  color = Color.Gray,
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.offset { IntOffset(x + 4, 4) }
              )
          }
      }
    }
  }
}
