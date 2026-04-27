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
fun AutoScrollEdgeMask(direction: com.example.clipystudio.data.AutoScrollDirection) {
  if (direction == com.example.clipystudio.data.AutoScrollDirection.NONE) return
  Row(Modifier.fillMaxSize()) {
    Box(Modifier.width(56.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(StudioPrimary.copy(alpha = if (direction == com.example.clipystudio.data.AutoScrollDirection.LEFT) 0.24f else 0.05f), Color.Transparent))))
    Spacer(Modifier.weight(1f))
    Box(Modifier.width(56.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color.Transparent, StudioPrimary.copy(alpha = if (direction == com.example.clipystudio.data.AutoScrollDirection.RIGHT) 0.24f else 0.05f)))))
  }
}


@Composable
fun EdgeResistanceMask(fraction: Float) {
  if (fraction <= 0f) return
  val alpha = (0.12f + fraction * 0.20f).coerceIn(0f, 0.32f)
  Row(Modifier.fillMaxSize()) {
    Box(Modifier.width(42.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(StudioSecondary.copy(alpha = alpha), Color.Transparent))))
    Spacer(Modifier.weight(1f))
    Box(Modifier.width(42.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color.Transparent, StudioSecondary.copy(alpha = alpha)))))
  }
}


@Composable
fun TimelineGuides(timeline: Timeline, contentWidth: Int, snapTimeMs: Long?) {
  val pxPerMs = timeline.pixelsPerSecond * timeline.zoomLevel / 1_000f
  Canvas(Modifier.fillMaxSize().padding(start = 66.dp, top = 52.dp, end = 8.dp, bottom = 8.dp).semantics { contentDescription = "Timeline snap guides and marker positions" }) {
    timeline.markers.forEach { marker ->
      val x = (marker.timeMs * pxPerMs - timeline.scrollOffsetPx).toFloat().coerceIn(0f, contentWidth.toFloat())
      drawLine(StudioAccent.copy(alpha = 0.42f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
    }
    timeline.transitions.forEach { transition ->
      TimelineEngine.transitionWindow(timeline, transition)?.let { window ->
        val start = (window.first * pxPerMs - timeline.scrollOffsetPx).toFloat()
        val end = (window.last * pxPerMs - timeline.scrollOffsetPx).toFloat()
        drawRect(StudioAccent.copy(alpha = 0.12f), Offset(start, 0f), Size((end - start).coerceAtLeast(1f), size.height))
      }
    }
    snapTimeMs?.let { snappedMs ->
      val x = (snappedMs * pxPerMs - timeline.scrollOffsetPx).toFloat().coerceIn(0f, contentWidth.toFloat())
      drawLine(StudioAccent.copy(alpha = 0.95f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
    }
  }
}