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
fun DashboardScreen(
  appState: AppState,
  copy: Copy,
  onCreate: (CanvasRatio) -> Unit,
  onOpen: (String) -> Unit,
  onRename: (String, String) -> Unit,
  onDuplicate: (String) -> Unit,
  onDelete: (String) -> Unit,
  onSettings: () -> Unit,
  onExit: () -> Unit,
) {
  var ratio by remember { mutableStateOf(CanvasRatio.Portrait) }
  Box(
    Modifier
      .fillMaxSize()
      .background(Color(0xFF1F232A))
      .padding(horizontal = 24.dp, vertical = 24.dp)
  ) {
    Surface(
      onClick = onSettings,
      shape = RoundedCornerShape(18.dp),
      color = Color(0xFF313842),
      modifier = Modifier.align(Alignment.TopStart).size(40.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text("≡", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
      }
    }

    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(bottom = 52.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      ReferenceHomeActionCard(
        title = if (appState.projects.isNotEmpty()) appState.projects.first().name else "Video Edit",
        subtitle = if (appState.languageCode == LanguageCode.Vi) "Mo editor va bat dau cat, ghep, filter" else "Open editor and start trimming, arranging, and filtering",
        accent = Brush.linearGradient(listOf(Color(0xFF2B3240), Color(0xFF3C4D6B))),
        onClick = {
          if (appState.projects.isNotEmpty()) {
            onOpen(appState.projects.first().id)
          } else {
            onCreate(ratio)
          }
        }
      )
      ReferenceHomeActionCard(
        title = if (appState.languageCode == LanguageCode.Vi) "New Project" else "New Project",
        subtitle = if (appState.languageCode == LanguageCode.Vi) "Chon canvas va them media de tao project moi" else "Pick a canvas ratio and import media for a new edit",
        accent = Brush.linearGradient(listOf(Color(0xFF2E3A31), Color(0xFF556B5D))),
        onClick = { onCreate(ratio) }
      )
      Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CanvasRatio.entries.forEach { item ->
          FilterChip(selected = ratio == item, onClick = { ratio = item }, label = { Text(item.label) })
        }
      }
    }

    Text(
      text = "Clipy Studio 1.0",
      color = Color.White.copy(alpha = 0.82f),
      fontSize = 15.sp,
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
    )
  }
}

@Composable
private fun DashboardMetricCard(modifier: Modifier = Modifier, value: String, label: String) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = StudioSurface.copy(alpha = 0.88f)),
  ) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
      Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp)
      Text(label, color = StudioTextMuted, fontSize = 12.sp)
    }
  }
}

@Composable
private fun ReferenceHomeActionCard(
  title: String,
  subtitle: String,
  accent: Brush,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth().height(112.dp)
  ) {
    Box(Modifier.fillMaxSize().background(accent)) {
      Column(
        Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Color.White.copy(alpha = 0.86f), fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
      }
    }
  }
}


@Composable
fun ProjectCard(project: Project, onOpen: (String) -> Unit, onRename: (String, String) -> Unit, onDuplicate: (String) -> Unit, onDelete: (String) -> Unit) {
  var showActions by remember { mutableStateOf(false) }
  Card(
    onClick = { onOpen(project.id) },
    colors = CardDefaults.cardColors(containerColor = StudioSurface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(
        Modifier
          .size(92.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(
            Brush.linearGradient(
              listOf(
                StudioPrimary.copy(alpha = 0.95f),
                StudioSecondary.copy(alpha = 0.88f),
              )
            )
          )
      ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
          Text(project.canvasRatio.label, fontWeight = FontWeight.Black)
          Text(project.timeline.durationMs.asTimecode(), fontSize = 11.sp)
        }
      }
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
        Text(project.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text("${project.timeline.durationMs.asTimecode()} · autosave v${project.autosaveVersion}", color = StudioTextMuted, fontSize = 13.sp)
        Text("${project.importedAssets.size} assets · ${project.timeline.tracks.size} tracks", color = StudioTextMuted, fontSize = 13.sp)
      }
      TextButton(onClick = { showActions = !showActions }) { Text("More") }
    }
    AnimatedVisibility(showActions) {
      Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onRename(project.id, "${project.name} Draft") }) { Text("Rename") }
        OutlinedButton(onClick = { onDuplicate(project.id) }) { Text("Duplicate") }
        OutlinedButton(onClick = { onDelete(project.id) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioDanger)) { Text("Delete") }
      }
    }
  }
}


@Composable
fun EmptyState(onCreate: () -> Unit) {
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurface), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      OnboardingIllustration(StudioPrimary)
      Text("Create your first project", fontWeight = FontWeight.Black, fontSize = 22.sp)
      Text("A starter draft and editor timeline will be prepared immediately.", color = StudioTextMuted, textAlign = TextAlign.Center)
      Spacer(Modifier.height(14.dp))
      Button(onClick = onCreate, shape = RoundedCornerShape(999.dp)) { Text("Create Project") }
    }
  }
}
