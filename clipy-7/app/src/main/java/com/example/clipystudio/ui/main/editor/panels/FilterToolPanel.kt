package com.example.clipystudio.ui.main.editor.panels

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.*
import com.example.clipystudio.*
import com.example.clipystudio.data.*
import com.example.clipystudio.filter.*
import com.example.clipystudio.theme.*
import com.example.clipystudio.ui.main.*
import com.example.clipystudio.ui.main.editor.*
import com.example.clipystudio.ui.main.editor.components.*
import com.example.clipystudio.ui.main.editor.panels.*
import com.example.clipystudio.ui.main.editor.timeline.*
import com.example.clipystudio.ui.main.models.*
import com.example.clipystudio.ui.main.screens.*
import kotlin.math.*

@Composable
fun FilterAdjustPanel(selectedClip: TimelineClip?, viewModel: MainScreenViewModel) {
  var adjustments by remember(selectedClip?.id) { mutableStateOf(selectedClip?.filterAdjustments ?: FilterAdjustmentSet()) }
  val filters = FilterLibrary.presets
  val enabled = selectedClip?.clipType in setOf(ClipType.Video, ClipType.Image, ClipType.Overlay)
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Filter and adjust", fontWeight = FontWeight.Bold)
      Text(selectedClip?.let { "${it.title} · ${clipTypeBadge(it.clipType)}" } ?: "Select a video, image, or overlay layer to adjust.", color = StudioTextMuted, fontSize = 13.sp)
      LazyRow(
          Modifier.padding(top = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
          items(filters, key = { it.id ?: "original" }) { filter ->
              FilterPreviewChip(filter, selectedClip?.filterAdjustments?.filterId == filter.id) {
                  if (!enabled) return@FilterPreviewChip
                  val next = filter.defaultAdjustments.copy(
                      filterId = filter.id,
                      gpuImageFilterClass = filter.gpuImageFilterClass
                  )
                  adjustments = next
                  viewModel.updateSelectedAdjustments(next)
              }
          }
      }
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
          if (!enabled) return@OutlinedButton
          val next = FilterAdjustmentSet()
          adjustments = next
          viewModel.updateSelectedAdjustments(next)
        }, enabled = enabled) { Text("Reset") }
        OutlinedButton(onClick = {
          if (!enabled) return@OutlinedButton
          val next = adjustments.copy(filterId = null, gpuImageFilterClass = null)
          adjustments = next
          viewModel.updateSelectedAdjustments(next)
        }, enabled = enabled) { Text("Original") }
      }
      if (enabled) {
        AdjustmentControl("Brightness", adjustments.brightness, 0.5f, 1.5f) { val next = adjustments.copy(brightness = it); adjustments = next; viewModel.updateSelectedAdjustments(next) }
        AdjustmentControl("Contrast", adjustments.contrast, 0.5f, 1.6f) { val next = adjustments.copy(contrast = it); adjustments = next; viewModel.updateSelectedAdjustments(next) }
        AdjustmentControl("Saturation", adjustments.saturation, 0f, 2f) { val next = adjustments.copy(saturation = it); adjustments = next; viewModel.updateSelectedAdjustments(next) }
        AdjustmentControl("Exposure", adjustments.exposure, -1f, 1f) { val next = adjustments.copy(exposure = it); adjustments = next; viewModel.updateSelectedAdjustments(next) }
        AdjustmentControl("Temperature", adjustments.temperature, -1f, 1f) { val next = adjustments.copy(temperature = it); adjustments = next; viewModel.updateSelectedAdjustments(next) }
        AdjustmentControl("Sharpness", adjustments.sharpness, 0f, 1f) { val next = adjustments.copy(sharpness = it); adjustments = next; viewModel.updateSelectedAdjustments(next) }
      }
    }
  }
}


@Composable
fun FilterPreviewChip(filter: FilterPreset, selected: Boolean, onClick: () -> Unit) {
  val haptic = LocalHapticFeedback.current
  Card(
    onClick = {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      onClick()
    },
    colors = CardDefaults.cardColors(containerColor = if (selected) StudioPrimary.copy(alpha = 0.45f) else StudioSurface),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.size(92.dp, 66.dp)
  ) {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(StudioPrimary.copy(alpha = 0.4f), StudioSecondary.copy(alpha = 0.28f)))), contentAlignment = Alignment.Center) { Text(filter.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
  }
}
