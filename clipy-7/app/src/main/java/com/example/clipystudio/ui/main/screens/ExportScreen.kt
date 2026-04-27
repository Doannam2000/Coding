package com.example.clipystudio.ui.main.screens

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
fun ExportScreen(appState: AppState, copy: Copy, onBack: () -> Unit, onDashboard: () -> Unit, viewModel: MainScreenViewModel) {
  val settings = appState.defaultExportSettings
  val renderState by viewModel.renderPipelineState.collectAsStateWithLifecycle()
  val exportState by viewModel.renderExportState.collectAsStateWithLifecycle()
  val hasExportableContent = appState.activeProject?.timeline?.durationMs?.let { it > 0L } == true
  LaunchedEffect(settings, appState.activeProject?.timeline?.version, appState.activeProjectId) {
    viewModel.prepareRenderPipeline(appState)
  }
  StudioScreen(horizontalPadding = 18.dp) {
    TopStrip(title = if (appState.languageCode == LanguageCode.Vi) "Xuat video" else "Export Video", onBack = onBack)
    if (exportState.status == RenderExportStatus.COMPLETED) {
      ExportSuccessPanel(exportState, onBack, onDashboard, viewModel)
      return@StudioScreen
    }
    if (exportState.status in setOf(RenderExportStatus.PREPARING, RenderExportStatus.RUNNING, RenderExportStatus.CANCELLING, RenderExportStatus.CANCELLED, RenderExportStatus.FAILED)) {
      ExportProgressPanel(exportState, viewModel, onBack)
      return@StudioScreen
    }
    Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
      Column(Modifier.padding(18.dp)) {
        Text("Export workspace", fontWeight = FontWeight.Black, fontSize = 24.sp)
        Text("Review output settings, render readiness, and storage safety before creating a shareable MP4.", color = StudioTextMuted, fontSize = 13.sp)
      }
    }
    ExportOptionCard("Format", settings.format, "MP4 is the MVP target for Android compatibility.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(selected = settings.format == "MP4", onClick = { viewModel.updateExportSettings(settings.copy(format = "MP4")) }, label = { Text("MP4") })
      FilterChip(selected = false, onClick = {}, enabled = false, label = { Text("MOV later") })
    }
    Spacer(Modifier.height(10.dp))
    ExportOptionCard("Resolution", settings.resolution.label, "720p and 1080p are primary; 2K/4K are device-gated.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ExportResolution.entries.forEach { res -> FilterChip(selected = settings.resolution == res, onClick = { viewModel.updateExportSettings(settings.copy(resolution = res)) }, label = { Text(res.label) }) }
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf(24, 30, 60).forEach { fps -> FilterChip(selected = settings.fps == fps, onClick = { viewModel.updateExportSettings(settings.copy(fps = fps)) }, label = { Text("$fps FPS") }) }
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      QualityPreset.entries.forEach { quality -> FilterChip(selected = settings.qualityPreset == quality, onClick = { viewModel.updateExportSettings(settings.copy(qualityPreset = quality)) }, label = { Text(quality.label) }) }
    }
    Spacer(Modifier.height(18.dp))
    ExportOptionCard("Estimated output", "${settings.bitrateMbps.toInt()} Mbps", "Snapshot includes timeline clips, overlays, text, stickers, effects, transitions, speed, canvas, and audio state.")
    Spacer(Modifier.height(12.dp))
    Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (appState.languageCode == LanguageCode.Vi) "Luu tru an toan" else "Storage safety", fontWeight = FontWeight.Bold)
        Text(
          if (appState.languageCode == LanguageCode.Vi) {
            "Clipy Studio chi tao tep tam trong cache cua ung dung trong luc xuat. Chia se chi bat dau sau khi video MP4 hop le duoc tao thanh cong."
          } else {
            "Clipy Studio keeps temporary render files inside app cache and only enables sharing after a valid MP4 output exists."
          },
          color = StudioTextMuted,
          fontSize = 13.sp,
        )
      }
    }
    Spacer(Modifier.height(12.dp))
    RenderPipelineSummary(renderState, exportState, appState.activeProject?.timeline?.durationMs ?: 0L)
    if (!hasExportableContent) {
      Text("Import media or add a visible clip before export.", color = StudioDanger, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    }
    Spacer(Modifier.weight(1f))
    Button(onClick = viewModel::startExport, enabled = hasExportableContent && renderState.status == RenderPipelineStatus.READY, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(999.dp)) { Text(if (appState.languageCode == LanguageCode.Vi) "Bat dau xuat" else "Start Export") }
  }
}


@Composable
fun RenderPipelineSummary(renderState: RenderPipelineState, exportState: RenderExportState, durationMs: Long) {
  val graph = renderState.graph
  val encoder = renderState.encoderConfig
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Render pipeline readiness summary" }) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Render readiness", fontWeight = FontWeight.Bold)
        StatusPill(renderState.status.name.lowercase().replaceFirstChar { it.uppercase() }, if (renderState.status == RenderPipelineStatus.ERROR) StudioDanger else StudioSecondary)
      }
      if (renderState.status == RenderPipelineStatus.ERROR) {
        Text(renderState.errorMessage.orEmpty(), color = StudioDanger, fontSize = 13.sp)
      } else {
        Text("${durationMs.asTimecode()} · ${renderState.totalFrames} frames · ${encoder?.fps ?: 0} FPS", color = StudioTextMuted, fontSize = 13.sp)
        Text("Encoder ${encoder?.width ?: 0}x${encoder?.height ?: 0} · ${encoder?.videoMimeType ?: "pending"} · ${((encoder?.videoBitrate ?: 0) / 1_000_000f).let { "%.1f".format(it) }} Mbps", color = StudioTextMuted, fontSize = 13.sp)
        exportState.codecStrategy?.let { codec ->
          Text("Codec ${codec.selected.name.replace('_', ' ')}${codec.requiresFallbackReason?.let { reason -> " · $reason" } ?: ""}", color = StudioTextMuted, fontSize = 13.sp)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          StatusPill("Layers ${graph?.layers?.size ?: 0}", StudioPrimary)
          StatusPill("Audio ${graph?.audio?.size ?: 0}", StudioSecondary)
          StatusPill("Transitions ${graph?.transitions?.size ?: 0}", StudioAccent)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          exportState.diagnostics.stages.ifEmpty {
            listOf(
              RenderStageStatus("Canvas", StageState.PENDING),
              RenderStageStatus("Keyframes", StageState.PENDING),
              RenderStageStatus("Stickers", StageState.PENDING),
              RenderStageStatus("Filters", StageState.PENDING),
              RenderStageStatus("Effects", StageState.PENDING),
              RenderStageStatus("Audio Mix", StageState.PENDING),
              RenderStageStatus("Audio Sync", StageState.PENDING),
              RenderStageStatus("Codec", StageState.PENDING),
              RenderStageStatus("Temp Files", StageState.PENDING),
              RenderStageStatus("Save", StageState.PENDING),
              RenderStageStatus("Share", StageState.PENDING),
            )
          }.forEach { stage -> StatusPill(stage.label, stageColor(stage.state)) }
        }
        graph?.layers?.take(5)?.let { nodes ->
          Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            nodes.forEach { node -> StatusPill("${node.type.name.lowercase()} ${node.startTimeMs.asTimecode()}", StudioSurface) }
          }
        }
      }
    }
  }
}


@Composable
fun StatusPill(label: String, color: Color) {
  Surface(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(999.dp), modifier = Modifier.height(32.dp)) {
    Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
      Text(label, color = if (color == StudioSurface) StudioTextMuted else color, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
  }
}


@Composable
fun ExportProgressPanel(exportState: RenderExportState, viewModel: MainScreenViewModel, onBack: () -> Unit) {
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text("Export progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      Text(exportState.progress.message ?: exportState.phase.name.replace('_', ' '), color = StudioTextMuted, textAlign = TextAlign.Center)
      LinearProgressIndicator(progress = { (exportState.progress.percent / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp).height(10.dp), color = StudioSecondary)
      Text("${exportState.progress.percent.roundToInt()}% · ${exportState.status.name.lowercase().replaceFirstChar { it.uppercase() }}", color = if (exportState.status == RenderExportStatus.FAILED) StudioDanger else StudioSecondary, fontWeight = FontWeight.Bold)
      Text("Frame ${exportState.progress.renderedFrames} of ${exportState.progress.totalFrames} · ${exportState.progress.currentTimeMs.asTimecode()}", color = StudioTextMuted, fontSize = 13.sp)
      exportState.codecStrategy?.let { Text("${it.selected.name.replace('_', ' ')} · ${it.videoMimeType}", color = StudioTextMuted, fontSize = 13.sp) }
      exportState.tempWorkspace?.let { Text(if (it.isCleaned) "Temporary files cleaned. Final export kept." else "Temp workspace active", color = StudioTextMuted, fontSize = 13.sp) }
      exportState.diagnostics.audioSync?.let { Text("Audio sync drift ${it.driftMs} ms", color = if (it.withinTolerance) StudioTextMuted else StudioAccent, fontSize = 13.sp) }
      exportState.error?.let { Text(it.message, color = StudioDanger, fontSize = 13.sp) }
      if (exportState.status == RenderExportStatus.CANCELLED) {
        Text("You can retry export when you're ready. The project and completed exports were kept.", color = StudioTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
      }
      Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        exportState.diagnostics.stages.forEach { stage ->
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stage.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            StatusPill(stage.state.name.lowercase().replaceFirstChar { it.uppercase() }, stageColor(stage.state))
          }
          stage.detail?.takeIf { it.isNotBlank() }?.let { Text(it, color = StudioTextMuted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth()) }
        }
      }
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (exportState.canCancel) OutlinedButton(onClick = viewModel::cancelExport, modifier = Modifier.height(48.dp)) { Text("Cancel", color = StudioDanger) }
        if (exportState.canRetry) Button(onClick = viewModel::retryExport, modifier = Modifier.height(48.dp)) { Text("Retry") }
        OutlinedButton(onClick = onBack) { Text("Return to editor") }
      }
    }
  }
}


@Composable
fun ExportSuccessPanel(exportState: RenderExportState, onBack: () -> Unit, onDashboard: () -> Unit, viewModel: MainScreenViewModel) {
  val output = exportState.output ?: return
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(StudioPrimary.copy(alpha = 0.45f), StudioSecondary.copy(alpha = 0.22f)))), contentAlignment = Alignment.Center) {
        Text("Saved video\n${output.displayName}", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
      }
      Spacer(Modifier.height(14.dp))
      Text("Export complete", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      Text("${output.width}x${output.height} · ${output.fps} FPS · ${output.durationMs.asTimecode()} · ${output.sizeBytes.asSizeLabel()}", color = StudioTextMuted, textAlign = TextAlign.Center)
      exportState.codecStrategy?.let { Text("${it.selected.name.replace('_', ' ')} save path ready for sharing", color = StudioTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center) }
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::requestShare, enabled = exportState.status == RenderExportStatus.COMPLETED && output.uri.isNotBlank(), modifier = Modifier.height(48.dp)) { Text("Share") }
        Button(onClick = viewModel::clearExportResult, modifier = Modifier.height(48.dp)) { Text("New Export") }
        OutlinedButton(onClick = onBack) { Text("Return to editor") }
        OutlinedButton(onClick = onDashboard) { Text("Dashboard") }
      }
    }
  }
}


@Composable
fun ExportOptionCard(title: String, value: String, subtitle: String) {
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = StudioTextMuted, fontSize = 13.sp) }
      Text(value, color = StudioSecondary, fontWeight = FontWeight.Bold)
    }
  }
}
