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
fun ToolRail(selected: EditorTool, onSelect: (EditorTool) -> Unit, onImport: () -> Unit, onExport: () -> Unit) {
  val haptic = LocalHapticFeedback.current
  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    item { 
      FilterChip(
        selected = false, 
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onImport()
        }, 
        label = { Text("+ Media") }
      ) 
    }
    items(EditorTool.entries, key = { it.name }) { tool -> 
      FilterChip(
        selected = selected == tool, 
        onClick = { 
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (tool == EditorTool.Export) onExport() else onSelect(tool) 
        }, 
        label = { Text(tool.label) }
      ) 
    }
  }
}


@Composable
fun ClipEditPanel(selectedClip: TimelineClip, viewModel: MainScreenViewModel) {
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Edit ${selectedClip.clipType}", fontWeight = FontWeight.Bold)
      Text("${selectedClip.startMs.asTimecode()} · ${selectedClip.durationMs.asTimecode()} · ${selectedClip.title}", color = StudioTextMuted, fontSize = 13.sp)
      if (selectedClip.isVisualMediaClip() && !selectedClip.hasUsableMediaUri()) {
        Text(
          "This clip is still on the timeline, but its media URI is invalid. Re-import or replace the source to recover preview and thumbnails.",
          color = StudioDanger,
          fontSize = 12.sp,
          modifier = Modifier.padding(top = 6.dp),
        )
      }
      if (selectedClip.clipType == ClipType.Video && selectedClip.sourceDurationMs != null) {
        Text(
          "Source ${selectedClip.sourceDurationMs.asTimecode()} · In ${selectedClip.sourceInMs.asTimecode()}",
          color = StudioTextMuted,
          fontSize = 12.sp,
          modifier = Modifier.padding(top = 4.dp),
        )
      }
      val supportsDurationAdjust = selectedClip.clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Overlay, ClipType.Audio)
      val durationStepMs = if (selectedClip.clipType == ClipType.Image) 500L else 1_000L
      if (supportsDurationAdjust) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = { viewModel.trimSelectedClip(-durationStepMs) }, modifier = Modifier.semantics { contentDescription = "Shorten selected clip duration" }) { Text(if (selectedClip.clipType == ClipType.Image) "Duration -0.5s" else "Trim -1s") }
          OutlinedButton(onClick = { viewModel.trimSelectedClip(durationStepMs) }, modifier = Modifier.semantics { contentDescription = "Extend selected clip duration" }) { Text(if (selectedClip.clipType == ClipType.Image) "Duration +0.5s" else "Trim +1s") }
        }
      }
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::splitSelectedClip, modifier = Modifier.semantics { contentDescription = "Split selected clip" }) { Text("Split") }
        OutlinedButton(onClick = viewModel::deleteSelectedClip, modifier = Modifier.semantics { contentDescription = "Delete selected clip" }) { Text("Delete") }
        OutlinedButton(onClick = viewModel::duplicateSelectedClip) { Text("Duplicate") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.SpeedDown) }) { Text("Speed -") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.SpeedUp) }) { Text("Speed +") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.VolumeDown) }) { Text("Volume -") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.VolumeUp) }) { Text("Volume +") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.Mute) }) { Text("Mute") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.Crop) }) { Text("Crop") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.Rotate) }) { Text("Rotate") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.Flip) }) { Text("Flip") }
      }
    }
  }
}


@Composable
fun AdjustmentControl(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
  val haptic = LocalHapticFeedback.current
  Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(label, fontSize = 11.sp, color = StudioTextMuted, fontWeight = FontWeight.SemiBold)
      Text("%.2f".format(value), fontSize = 11.sp, color = StudioPrimary, fontWeight = FontWeight.Bold)
    }
    Slider(
      value = value,
      onValueChange = {
        if (it != value) {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onChange(it)
        }
      },
      valueRange = min..max,
      modifier = Modifier.fillMaxWidth().height(32.dp),
      colors = SliderDefaults.colors(
        thumbColor = StudioPrimary,
        activeTrackColor = StudioPrimary,
        inactiveTrackColor = Color.White.copy(alpha = 0.12f)
      )
    )
  }
}


@Composable
fun LayerActions(viewModel: MainScreenViewModel, enabled: Boolean = true) {
  Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick = viewModel::duplicateSelectedClip, enabled = enabled) { Text("Duplicate") }
    OutlinedButton(onClick = viewModel::deleteSelectedClip, enabled = enabled) { Text("Delete") }
    OutlinedButton(onClick = { viewModel.trimSelectedClip(-500) }, enabled = enabled) { Text("Trim -") }
    OutlinedButton(onClick = { viewModel.trimSelectedClip(500) }, enabled = enabled) { Text("Trim +") }
    OutlinedButton(onClick = viewModel::toggleKeyframeAtPlayhead, enabled = enabled, modifier = Modifier.semantics { contentDescription = "Toggle keyframe at playhead" }) { Text("Keyframe") }
  }
}


@Composable
fun ToolPanel(timeline: Timeline, viewModel: MainScreenViewModel) {
  val selectedClip = timeline.tracks.flatMap { it.clips }.firstOrNull { it.id == timeline.selectedClipId }
  val hasSelection = selectedClip != null
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consumePositionChange() } } }) {
    Column(Modifier.padding(14.dp)) {
      Text("${timeline.selectedTool.label} tools", fontWeight = FontWeight.Bold)
      Text(selectedClip?.title ?: "Select a clip to edit", color = StudioTextMuted, fontSize = 13.sp)
      Spacer(Modifier.height(10.dp))
      Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (timeline.selectedTool) {
          EditorTool.Edit -> listOf(ClipAction.Rotate, ClipAction.Flip, ClipAction.SpeedDown, ClipAction.SpeedUp, ClipAction.OpacityDown, ClipAction.OpacityUp, ClipAction.Mute)
          EditorTool.Filter, EditorTool.Effect -> listOf(ClipAction.Filter, ClipAction.OpacityDown, ClipAction.OpacityUp)
          EditorTool.Speed -> listOf(ClipAction.SpeedDown, ClipAction.SpeedUp)
          else -> listOf(ClipAction.OpacityDown, ClipAction.OpacityUp, ClipAction.Keyframe)
        }.forEach { action -> OutlinedButton(onClick = { viewModel.adjustSelectedClip(action) }, enabled = hasSelection) { Text(action.label) } }
      }
      Spacer(Modifier.height(8.dp))
      Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::splitSelectedClip, enabled = hasSelection) { Text("Split") }
        OutlinedButton(onClick = viewModel::deleteSelectedClip, enabled = hasSelection) { Text("Delete") }
        OutlinedButton(onClick = viewModel::duplicateSelectedClip, enabled = hasSelection) { Text("Duplicate") }
        OutlinedButton(onClick = { viewModel.trimSelectedClip(-500) }, enabled = hasSelection) { Text("Trim -") }
        OutlinedButton(onClick = { viewModel.trimSelectedClip(500) }, enabled = hasSelection) { Text("Trim +") }
      }
    }
  }
}