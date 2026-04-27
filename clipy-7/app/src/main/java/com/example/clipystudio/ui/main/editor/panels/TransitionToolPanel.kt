package com.example.clipystudio.ui.main.editor.panels

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
fun TransitionToolPanel(timeline: Timeline, viewModel: MainScreenViewModel) {
  var duration by remember { mutableStateOf(800L) }
  val videoClips = timeline.tracks.firstOrNull { it.type == TrackType.Video }?.clips.orEmpty()
  val nearestPair = remember(timeline.playheadMs, videoClips, timeline.transitions) {
    videoClips.sortedBy { it.startMs }.zipWithNext().minByOrNull { (_, next) -> kotlin.math.abs((next.startMs - timeline.playheadMs).toInt()) }
  }
  val activeTransition = remember(nearestPair, timeline.transitions) {
    val pair = nearestPair ?: return@remember null
    timeline.transitions.firstOrNull { it.fromClipId == pair.first.id && it.toClipId == pair.second.id }
  }
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Transitions", fontWeight = FontWeight.Bold)
      Text(if (videoClips.size >= 2) "Apply between the video clips nearest the current playhead boundary." else "Add at least two video/image clips to enable transitions.", color = StudioTextMuted, fontSize = 13.sp)
      activeTransition?.let {
        Text("Active: ${it.type.label} · ${it.durationMs}ms", color = StudioSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
      }
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { TransitionType.entries.forEach { type -> Button(onClick = { viewModel.applyTransition(type, duration) }, enabled = videoClips.size >= 2, shape = RoundedCornerShape(999.dp)) { Text(type.label) } } }
      AdjustmentControl("Duration ms", duration.toFloat(), 300f, 2_000f) { duration = it.toLong() }
      OutlinedButton(onClick = viewModel::removeTransition, enabled = activeTransition != null, modifier = Modifier.padding(top = 8.dp)) { Text("Remove transition") }
    }
  }
}
