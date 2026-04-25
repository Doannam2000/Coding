package com.example.clipystudio.ui.main

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.clipystudio.data.AppState
import com.example.clipystudio.data.CanvasRatio
import com.example.clipystudio.data.ClipAction
import com.example.clipystudio.data.EditorTool
import com.example.clipystudio.data.ExportResolution
import com.example.clipystudio.data.ExportStatus
import com.example.clipystudio.data.LanguageCode
import com.example.clipystudio.data.MediaAsset
import com.example.clipystudio.data.MediaType
import com.example.clipystudio.data.Project
import com.example.clipystudio.data.QualityPreset
import com.example.clipystudio.data.Timeline
import com.example.clipystudio.data.TimelineClip
import com.example.clipystudio.data.TimelineTrack
import com.example.clipystudio.data.TrackType
import com.example.clipystudio.data.asSizeLabel
import com.example.clipystudio.data.asTimecode
import com.example.clipystudio.theme.MyApplicationTheme
import com.example.clipystudio.theme.StudioAccent
import com.example.clipystudio.theme.StudioBackground
import com.example.clipystudio.theme.StudioDanger
import com.example.clipystudio.theme.StudioPrimary
import com.example.clipystudio.theme.StudioSecondary
import com.example.clipystudio.theme.StudioSurface
import com.example.clipystudio.theme.StudioSurfaceHigh
import com.example.clipystudio.theme.StudioTextMuted
import kotlinx.coroutines.delay

private enum class Screen { Splash, Intro, Language, Dashboard, Import, Editor, Export, Settings }

private data class Copy(
  val create: String,
  val recent: String,
  val settings: String,
  val exit: String,
  val import: String,
  val export: String,
  val language: String,
  val continueAction: String,
  val dashboard: String,
  val editor: String,
)

private data class IntroPage(val title: String, val body: String, val color: Color)

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel { MainScreenViewModel() },
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  when (val state = uiState) {
    MainScreenUiState.Loading -> LoadingSurface(modifier)
    is MainScreenUiState.Error -> ErrorSurface(state.throwable.message.orEmpty(), modifier)
    is MainScreenUiState.Success -> ClipyStudioApp(state.appState, viewModel, modifier)
  }
}

@Composable
private fun ClipyStudioApp(appState: AppState, viewModel: MainScreenViewModel, modifier: Modifier = Modifier) {
  var screen by remember { mutableStateOf(Screen.Splash) }
  var languageFromSettings by remember { mutableStateOf(false) }
  var exitRequested by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  val copy = copyFor(appState.languageCode)

  LaunchedEffect(Unit) {
    delay(550)
    screen = if (appState.hasCompletedIntro) Screen.Dashboard else Screen.Intro
  }
  LaunchedEffect(appState.exportJob?.status) {
    if (appState.exportJob?.status == ExportStatus.Complete) snackbarHostState.showSnackbar("Export complete: ${appState.exportJob.outputUri}")
  }

  BackHandler(screen != Screen.Dashboard && screen != Screen.Intro) {
    screen = when (screen) {
      Screen.Editor, Screen.Import, Screen.Export, Screen.Settings -> Screen.Dashboard
      Screen.Language -> if (languageFromSettings) Screen.Settings else Screen.Intro
      else -> Screen.Dashboard
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().background(StudioBackground),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = StudioBackground,
  ) { padding ->
    Box(Modifier.fillMaxSize().padding(padding)) {
      when (screen) {
        Screen.Splash -> LoadingSurface(Modifier.fillMaxSize(), languageCode = appState.languageCode)
        Screen.Intro -> IntroScreen(
          copy = copy,
          onContinue = { viewModel.completeIntro(); screen = Screen.Language },
          onSkip = { screen = Screen.Language },
        )
        Screen.Language -> LanguageScreen(
          selected = appState.languageCode,
          copy = copy,
          showBack = true,
          onBack = { screen = if (languageFromSettings) Screen.Settings else Screen.Intro },
          onSave = { language ->
            viewModel.setLanguage(language)
            screen = if (languageFromSettings) Screen.Settings else Screen.Dashboard
            languageFromSettings = false
          },
        )
        Screen.Dashboard -> DashboardScreen(
          appState = appState,
          copy = copy,
          onCreate = { ratio -> viewModel.createProject(ratio); screen = Screen.Import },
          onOpen = { id -> viewModel.openProject(id); screen = Screen.Editor },
          onRename = viewModel::renameProject,
          onDuplicate = viewModel::duplicateProject,
          onDelete = viewModel::deleteProject,
          onSettings = { screen = Screen.Settings },
          onExit = { exitRequested = true },
        )
        Screen.Import -> ImportScreen(
          appState = appState,
          copy = copy,
          onBack = { screen = Screen.Dashboard },
          onAddAsset = viewModel::addImportedAsset,
          onRemove = viewModel::removeImportedAsset,
          onAddToProject = {
            viewModel.addImportsToProject()
            screen = Screen.Editor
          },
        )
        Screen.Editor -> EditorScreen(
          appState = appState,
          copy = copy,
          onBack = { screen = Screen.Dashboard },
          onImport = { screen = Screen.Import },
          onExport = { screen = Screen.Export },
          viewModel = viewModel,
        )
        Screen.Export -> ExportScreen(
          appState = appState,
          copy = copy,
          onBack = { screen = Screen.Editor },
          onDashboard = { viewModel.clearExportResult(); screen = Screen.Dashboard },
          viewModel = viewModel,
        )
        Screen.Settings -> SettingsScreen(
          appState = appState,
          copy = copy,
          onBack = { screen = Screen.Dashboard },
          onLanguage = { languageFromSettings = true; screen = Screen.Language },
          onClearCache = viewModel::clearCache,
          onExit = { exitRequested = true },
        )
      }

      if (exitRequested) {
        AlertDialog(
          onDismissRequest = { exitRequested = false },
          title = { Text(if (appState.languageCode == LanguageCode.Vi) "Thoat Clipy Studio?" else "Exit Clipy Studio?") },
          text = { Text(if (appState.languageCode == LanguageCode.Vi) "Ban co the quay lai du an da tu dong luu bat cu luc nao." else "Autosaved projects will be available when you return.") },
          confirmButton = { TextButton(onClick = { (context as? android.app.Activity)?.finish() }) { Text(copy.exit, color = StudioDanger) } },
          dismissButton = { TextButton(onClick = { exitRequested = false }) { Text(if (appState.languageCode == LanguageCode.Vi) "Huy" else "Cancel") } },
        )
      }
    }
  }
}

@Composable
private fun IntroScreen(copy: Copy, onContinue: () -> Unit, onSkip: () -> Unit) {
  var page by remember { mutableStateOf(0) }
  val pages = copy.onboardingPages()
  StudioScreen {
    Spacer(Modifier.height(28.dp))
    Text("Clipy Studio", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Text("A premium offline-friendly Android video editor", color = StudioTextMuted)
    Spacer(Modifier.height(28.dp))
    Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
      Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        OnboardingIllustration(pages[page].color)
        Spacer(Modifier.height(24.dp))
        Text(pages[page].title, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(pages[page].body, color = StudioTextMuted, textAlign = TextAlign.Center)
      }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.Center) {
      pages.indices.forEach { Dot(selected = it == page) }
    }
    Button(
      onClick = { if (page < pages.lastIndex) page++ else onContinue() },
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(999.dp),
    ) { Text(if (page == pages.lastIndex) "Get Started" else copy.continueAction) }
    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Skip") }
  }
}

@Composable
private fun LanguageScreen(selected: LanguageCode, copy: Copy, showBack: Boolean, onBack: () -> Unit, onSave: (LanguageCode) -> Unit) {
  var current by remember(selected) { mutableStateOf(selected) }
  StudioScreen {
    TopStrip(title = copy.language, onBack = if (showBack) onBack else null)
    Spacer(Modifier.height(24.dp))
    Text(if (selected == LanguageCode.Vi) "Chon ngon ngu" else "Choose your language", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(if (selected == LanguageCode.Vi) "Chon ngon ngu ung dung" else "Select the app language", color = StudioTextMuted)
    Spacer(Modifier.height(20.dp))
    LanguageCard("English", "English", current == LanguageCode.En) { current = LanguageCode.En }
    Spacer(Modifier.height(12.dp))
    LanguageCard("Tieng Viet", "Vietnamese", current == LanguageCode.Vi) { current = LanguageCode.Vi }
    Spacer(Modifier.weight(1f))
    Button(onClick = { onSave(current) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(999.dp)) { Text(copy.continueAction) }
  }
}

@Composable
private fun DashboardScreen(
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
  StudioScreen {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text("Clipy Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(if (appState.languageCode == LanguageCode.Vi) "Ban nhap tu dong luu, media cuc bo, xuat nhanh" else "Autosaved drafts, local media, fast exports", color = StudioTextMuted, fontSize = 13.sp)
      }
      TextButton(onClick = onSettings) { Text(copy.settings) }
      TextButton(onClick = onExit) { Text(copy.exit, color = StudioDanger) }
    }
    Spacer(Modifier.height(18.dp))
    Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.padding(20.dp)) {
        Text(copy.create, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(if (appState.languageCode == LanguageCode.Vi) "Chon preset, them media va bat dau bien tap." else "Pick a creator preset, import media, and start editing.", color = StudioTextMuted)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          CanvasRatio.entries.forEach { item -> FilterChip(selected = ratio == item, onClick = { ratio = item }, label = { Text(item.label) }) }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onCreate(ratio) }, shape = RoundedCornerShape(999.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(copy.create) }
      }
    }
    Spacer(Modifier.height(22.dp))
    Text(copy.recent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    if (appState.projects.isEmpty()) EmptyState(onCreate = { onCreate(ratio) }) else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      items(appState.projects, key = { it.id }) { project -> ProjectCard(project, onOpen, onRename, onDuplicate, onDelete) }
    }
  }
}

@Composable
private fun ProjectCard(project: Project, onOpen: (String) -> Unit, onRename: (String, String) -> Unit, onDuplicate: (String) -> Unit, onDelete: (String) -> Unit) {
  var showActions by remember { mutableStateOf(false) }
  Card(onClick = { onOpen(project.id) }, colors = CardDefaults.cardColors(containerColor = StudioSurface), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(86.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(StudioPrimary, StudioSecondary)))) {
        Text(project.canvasRatio.label, modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.Bold)
      }
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
        Text(project.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${project.timeline.durationMs.asTimecode()} duration · autosave v${project.autosaveVersion}", color = StudioTextMuted, fontSize = 13.sp)
        Text("${project.importedAssets.size} assets · ${project.timeline.tracks.size} tracks", color = StudioTextMuted, fontSize = 13.sp)
      }
      TextButton(onClick = { showActions = !showActions }) { Text("More") }
    }
    AnimatedVisibility(showActions) {
      Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onRename(project.id, "${project.name} Draft") }) { Text("Rename") }
        OutlinedButton(onClick = { onDuplicate(project.id) }) { Text("Duplicate") }
        OutlinedButton(onClick = { onDelete(project.id) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioDanger)) { Text("Delete") }
      }
    }
  }
}

@Composable
private fun ImportScreen(appState: AppState, copy: Copy, onBack: () -> Unit, onAddAsset: (MediaType, String?, String?, Long?) -> Unit, onRemove: (String) -> Unit, onAddToProject: () -> Unit) {
  val context = LocalContext.current
  val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
    uris.forEach { uri ->
      val metadata = context.readUriMetadata(uri)
      onAddAsset(if (metadata.mimeType?.startsWith("image") == true) MediaType.Image else MediaType.Video, uri.toString(), metadata.displayName, metadata.sizeBytes)
    }
  }
  val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
    uris.forEach { uri ->
      val metadata = context.readUriMetadata(uri)
      onAddAsset(MediaType.Audio, uri.toString(), metadata.displayName, metadata.sizeBytes)
    }
  }
  StudioScreen {
    TopStrip(title = copy.import, onBack = onBack)
    Text(if (appState.languageCode == LanguageCode.Vi) "Dung bo chon he thong de them video, anh va am thanh. URI duoc luu thanh metadata ban nhap." else "Use Android system pickers for visual and audio media. Selected URIs are autosaved as draft metadata.", color = StudioTextMuted)
    Spacer(Modifier.height(14.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Button(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, shape = RoundedCornerShape(999.dp)) { Text(if (appState.languageCode == LanguageCode.Vi) "Them anh/video" else "Import Images/Videos") }
      Button(onClick = { audioPicker.launch(arrayOf("audio/*")) }, shape = RoundedCornerShape(999.dp)) { Text(if (appState.languageCode == LanguageCode.Vi) "Them am thanh" else "Import Audio") }
      MediaType.entries.forEach { type -> OutlinedButton(onClick = { onAddAsset(type, null, null, null) }, shape = RoundedCornerShape(999.dp)) { Text("Sample ${type.label}") } }
    }
    Spacer(Modifier.height(18.dp))
    Text("Selected (${appState.selectedImports.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    if (appState.selectedImports.isEmpty()) {
      Text(if (appState.languageCode == LanguageCode.Vi) "Chon it nhat mot tep media de them vao timeline." else "Select at least one media item to add it to the timeline.", color = StudioTextMuted, fontSize = 13.sp)
    }
    Spacer(Modifier.height(8.dp))
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      items(appState.selectedImports, key = { it.id }) { asset -> MediaAssetCard(asset, onRemove) }
    }
    Button(onClick = onAddToProject, enabled = appState.selectedImports.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(999.dp)) { Text(if (appState.languageCode == LanguageCode.Vi) "Them vao du an" else "Add to Project") }
  }
}

@Composable
private fun EditorScreen(appState: AppState, copy: Copy, onBack: () -> Unit, onImport: () -> Unit, onExport: () -> Unit, viewModel: MainScreenViewModel) {
  val project = appState.activeProject
  if (project == null) {
    StudioScreen { EmptyState(onCreate = onImport) }
    return
  }
  val timeline = project.timeline
  StudioScreen(horizontalPadding = 10.dp) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      TextButton(onClick = onBack) { Text("Back") }
      Text("Saved v${project.autosaveVersion}", modifier = Modifier.weight(1f), color = StudioSecondary, textAlign = TextAlign.Center)
      TextButton(onClick = viewModel::undo) { Text("Undo") }
      TextButton(onClick = viewModel::redo) { Text("Redo") }
      Button(onClick = onExport, shape = RoundedCornerShape(999.dp)) { Text(copy.export) }
    }
    PreviewCanvas(project.canvasRatio, timeline)
    PlaybackControls(timeline, viewModel::togglePlayback, viewModel::seekBy)
    TimelineView(timeline, viewModel::selectClip)
    Spacer(Modifier.height(8.dp))
    ToolRail(timeline.selectedTool, viewModel::updateSelectedTool, onImport)
    ToolPanel(timeline, viewModel)
  }
}

@Composable
private fun ExportScreen(appState: AppState, copy: Copy, onBack: () -> Unit, onDashboard: () -> Unit, viewModel: MainScreenViewModel) {
  val settings = appState.defaultExportSettings
  LaunchedEffect(appState.exportJob?.status) {
    if (appState.exportJob?.status == ExportStatus.Running) {
      delay(900)
      viewModel.completeExport()
    }
  }
  StudioScreen {
    TopStrip(title = if (appState.languageCode == LanguageCode.Vi) "Xuat video" else "Export Video", onBack = onBack)
    ExportOptionCard("Format", settings.format, "MP4 is the MVP target for Android compatibility.")
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
    appState.exportJob?.let { job ->
      Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
          Text("Status: ${job.status}", fontWeight = FontWeight.Bold)
          LinearProgressIndicator(progress = { job.progressPercent / 100f }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), color = StudioSecondary)
          Text(job.outputUri ?: if (job.status == ExportStatus.Running) "Rendering MP4 in the background-safe MVP pipeline" else "Export cancelled", color = StudioTextMuted)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (job.status == ExportStatus.Running) OutlinedButton(onClick = viewModel::cancelExport) { Text("Cancel", color = StudioDanger) }
            Button(onClick = onDashboard) { Text(if (appState.languageCode == LanguageCode.Vi) "Ve bang du an" else "Back to Dashboard") }
          }
        }
      }
    }
    Spacer(Modifier.weight(1f))
    Button(onClick = viewModel::startExport, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(999.dp)) { Text(if (appState.languageCode == LanguageCode.Vi) "Bat dau xuat" else "Start Export") }
  }
}

@Composable
private fun SettingsScreen(appState: AppState, copy: Copy, onBack: () -> Unit, onLanguage: () -> Unit, onClearCache: () -> Unit, onExit: () -> Unit) {
  StudioScreen {
    TopStrip(title = copy.settings, onBack = onBack)
    SettingsRow(copy.language, if (appState.languageCode == LanguageCode.En) "English" else "Tieng Viet", onLanguage)
    SettingsRow(if (appState.languageCode == LanguageCode.Vi) "Mac dinh xuat" else "Export defaults", "${appState.defaultExportSettings.resolution.label}, ${appState.defaultExportSettings.fps} FPS, ${appState.defaultExportSettings.qualityPreset.label}", {})
    SettingsRow(if (appState.languageCode == LanguageCode.Vi) "Luu tru va cache" else "Storage & Cache", "${appState.cacheUsageMb} MB thumbnail/proxy cache", onClearCache, action = if (appState.languageCode == LanguageCode.Vi) "Xoa" else "Clear")
    SettingsRow(if (appState.languageCode == LanguageCode.Vi) "Thong tin ung dung" else "App Info", if (appState.languageCode == LanguageCode.Vi) "Bien tap cuc bo offline-friendly MVP - version 1.0" else "Offline-friendly local editing MVP - version 1.0", {})
    SettingsRow(copy.exit, "Close after autosave/export confirmation", onExit, danger = true)
  }
}

@Composable
private fun PreviewCanvas(ratio: CanvasRatio, timeline: Timeline) {
  val ratioValue = when (ratio) { CanvasRatio.Portrait -> 9f / 16f; CanvasRatio.Square -> 1f; CanvasRatio.Landscape -> 16f / 9f }
  val glow by animateFloatAsState(if (timeline.isPlaying) 1f else 0.35f, label = "previewGlow")
  Box(Modifier.fillMaxWidth().height(270.dp).clip(RoundedCornerShape(24.dp)).background(StudioSurfaceHigh), contentAlignment = Alignment.Center) {
    Box(Modifier.fillMaxHeight(0.88f).aspectRatio(ratioValue).clip(RoundedCornerShape(18.dp)).background(Brush.radialGradient(listOf(StudioPrimary.copy(alpha = 0.55f * glow), StudioBackground)))) {
      Canvas(Modifier.fillMaxSize()) {
        drawRect(Color.White.copy(alpha = 0.06f), style = Stroke(width = 2.dp.toPx()))
        drawCircle(StudioSecondary.copy(alpha = glow), radius = 18.dp.toPx(), center = center)
      }
      Text("Preview ${timeline.playheadMs.asTimecode()}", modifier = Modifier.align(Alignment.TopCenter).padding(10.dp), fontWeight = FontWeight.Bold)
      Text("Drag / pinch / rotate overlays", modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp), color = StudioTextMuted, fontSize = 12.sp)
    }
  }
}

@Composable
private fun PlaybackControls(timeline: Timeline, onPlay: () -> Unit, onSeek: (Long) -> Unit) {
  Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
    Text(timeline.playheadMs.asTimecode(), color = StudioSecondary, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(onClick = { onSeek(-1_000) }) { Text("-1s") }
      Button(onClick = onPlay, shape = CircleShape, modifier = Modifier.semantics { contentDescription = if (timeline.isPlaying) "Pause playback" else "Play playback" }) { Text(if (timeline.isPlaying) "Pause" else "Play") }
      OutlinedButton(onClick = { onSeek(1_000) }) { Text("+1s") }
    }
    Text(timeline.durationMs.asTimecode(), color = StudioTextMuted)
  }
}

@Composable
private fun TimelineView(timeline: Timeline, onSelect: (String) -> Unit) {
  Box(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.28f))) {
    LazyColumn(Modifier.fillMaxSize().padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(timeline.tracks, key = { it.id }) { track -> TrackLane(track, timeline.selectedClipId, onSelect) }
    }
    Box(Modifier.align(Alignment.TopCenter).width(2.dp).fillMaxHeight().background(StudioSecondary))
    Box(Modifier.align(Alignment.TopCenter).padding(top = 4.dp).size(12.dp).clip(CircleShape).background(StudioSecondary))
  }
}

@Composable
private fun TrackLane(track: TimelineTrack, selectedClipId: String?, onSelect: (String) -> Unit) {
  Row(Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(track.type.label, modifier = Modifier.width(58.dp), fontSize = 12.sp, color = StudioTextMuted)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(track.clips, key = { it.id }) { clip -> ClipBlock(track.type, clip, selectedClipId == clip.id, onSelect) }
    }
  }
}

@Composable
private fun ClipBlock(trackType: TrackType, clip: TimelineClip, selected: Boolean, onSelect: (String) -> Unit) {
  val color = when (trackType) { TrackType.Video -> StudioPrimary; TrackType.Audio -> StudioSecondary; TrackType.Text -> StudioAccent; TrackType.Sticker -> Color(0xFFFF65B3); TrackType.Effect -> Color(0xFF55A7FF); TrackType.Overlay -> Color(0xFF56E58A) }
  Box(
    Modifier.width((70 + (clip.durationMs / 120).toInt()).coerceAtMost(170).dp).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = if (selected) 0.9f else 0.55f)).border(if (selected) 2.dp else 0.dp, Color.White, RoundedCornerShape(12.dp)).clickable { onSelect(clip.id) }.semantics { contentDescription = "${clip.clipType} clip, starts at ${clip.startMs.asTimecode()}, duration ${clip.durationMs.asTimecode()}" },
    contentAlignment = Alignment.Center,
  ) { Text(clip.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp)) }
}

@Composable
private fun ToolRail(selected: EditorTool, onSelect: (EditorTool) -> Unit, onImport: () -> Unit) {
  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    item { FilterChip(selected = false, onClick = onImport, label = { Text("+ Media") }) }
    items(EditorTool.entries, key = { it.name }) { tool -> FilterChip(selected = selected == tool, onClick = { onSelect(tool) }, label = { Text(tool.label) }) }
  }
}

@Composable
private fun ToolPanel(timeline: Timeline, viewModel: MainScreenViewModel) {
  val selectedClip = timeline.tracks.flatMap { it.clips }.firstOrNull { it.id == timeline.selectedClipId }
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("${timeline.selectedTool.label} tools", fontWeight = FontWeight.Bold)
      Text(selectedClip?.title ?: "Select a clip to edit", color = StudioTextMuted, fontSize = 13.sp)
      Spacer(Modifier.height(10.dp))
      Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (timeline.selectedTool) {
          EditorTool.Edit -> listOf(ClipAction.Rotate, ClipAction.Flip, ClipAction.SpeedDown, ClipAction.SpeedUp, ClipAction.OpacityDown, ClipAction.OpacityUp, ClipAction.Mute)
          EditorTool.Filter, EditorTool.Effect -> listOf(ClipAction.Filter, ClipAction.OpacityDown, ClipAction.OpacityUp)
          EditorTool.Keyframe -> listOf(ClipAction.Keyframe)
          else -> listOf(ClipAction.OpacityDown, ClipAction.OpacityUp, ClipAction.Keyframe)
        }.forEach { action -> OutlinedButton(onClick = { viewModel.adjustSelectedClip(action) }) { Text(action.label) } }
      }
      Spacer(Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::splitSelectedClip) { Text("Split") }
        OutlinedButton(onClick = viewModel::duplicateSelectedClip) { Text("Duplicate") }
        OutlinedButton(onClick = { viewModel.trimSelectedClip(-500) }) { Text("Trim -") }
        OutlinedButton(onClick = { viewModel.trimSelectedClip(500) }) { Text("Trim +") }
      }
    }
  }
}

@Composable
private fun StudioScreen(horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
  Column(Modifier.fillMaxSize().background(StudioBackground).padding(horizontal = horizontalPadding, vertical = 14.dp), content = content)
}

@Composable
private fun LoadingSurface(modifier: Modifier = Modifier, languageCode: LanguageCode = LanguageCode.En) {
  Box(modifier.fillMaxSize().background(Brush.radialGradient(listOf(StudioSurfaceHigh, StudioBackground))), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      OnboardingIllustration(StudioSecondary)
      Text("Clipy Studio", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
      Text(if (languageCode == LanguageCode.Vi) "Dang tai studio" else "Loading studio", color = StudioTextMuted)
      LinearProgressIndicator(modifier = Modifier.width(160.dp).padding(top = 18.dp), color = StudioSecondary)
    }
  }
}

@Composable
private fun ErrorSurface(message: String, modifier: Modifier = Modifier) = Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error loading studio: $message", color = StudioDanger) }

@Composable
private fun OnboardingIllustration(color: Color) {
  Canvas(Modifier.size(170.dp)) {
    drawRoundRect(StudioSurfaceHigh, cornerRadius = androidx.compose.ui.geometry.CornerRadius(30f, 30f), size = size)
    drawRoundRect(color.copy(alpha = 0.24f), topLeft = Offset(26f, 34f), size = Size(size.width - 52f, 56f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f))
    drawLine(StudioSecondary, Offset(size.width / 2f, 22f), Offset(size.width / 2f, size.height - 22f), strokeWidth = 5f)
    drawCircle(color, 22f, Offset(size.width / 2f, 42f))
    drawRoundRect(StudioPrimary.copy(alpha = 0.65f), topLeft = Offset(26f, 112f), size = Size(62f, 24f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
    drawRoundRect(StudioAccent.copy(alpha = 0.8f), topLeft = Offset(94f, 112f), size = Size(48f, 24f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
  }
}

@Composable
private fun Dot(selected: Boolean) = Box(Modifier.padding(4.dp).size(if (selected) 20.dp else 8.dp, 8.dp).clip(CircleShape).background(if (selected) StudioPrimary else StudioTextMuted.copy(alpha = 0.35f)))

@Composable
private fun LanguageCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
  Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = if (selected) StudioPrimary.copy(alpha = 0.35f) else StudioSurface), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$title language option ${if (selected) "selected" else "not selected"}" }) {
    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = StudioTextMuted) }
      Text(if (selected) "Selected" else "Select", color = if (selected) StudioSecondary else StudioTextMuted)
    }
  }
}

@Composable
private fun TopStrip(title: String, onBack: (() -> Unit)?) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    if (onBack != null) TextButton(onClick = onBack) { Text("Back") }
    Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      OnboardingIllustration(StudioPrimary)
      Text("Create your first project", fontWeight = FontWeight.Bold, fontSize = 22.sp)
      Text("A starter timeline will be autosaved immediately.", color = StudioTextMuted, textAlign = TextAlign.Center)
      Spacer(Modifier.height(14.dp))
      Button(onClick = onCreate, shape = RoundedCornerShape(999.dp)) { Text("Create Project") }
    }
  }
}

@Composable
private fun MediaAssetCard(asset: MediaAsset, onRemove: (String) -> Unit) {
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(if (asset.type == MediaType.Audio) StudioSecondary else StudioPrimary), contentAlignment = Alignment.Center) { Text(asset.type.label.take(1), fontWeight = FontWeight.Bold) }
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)) {
        Text(asset.displayName, fontWeight = FontWeight.Bold)
        Text("${asset.durationMs.asTimecode()} · ${asset.sizeBytes.asSizeLabel()}${if (asset.sizeBytes > 40_000_000) " · Large file" else ""}", color = StudioTextMuted, fontSize = 13.sp)
      }
      TextButton(onClick = { onRemove(asset.id) }) { Text("Remove", color = StudioDanger) }
    }
  }
}

@Composable
private fun ExportOptionCard(title: String, value: String, subtitle: String) {
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = StudioTextMuted, fontSize = 13.sp) }
      Text(value, color = StudioSecondary, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit, action: String = "Open", danger: Boolean = false) {
  Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = StudioSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) { Text(title, color = if (danger) StudioDanger else Color.Unspecified, fontWeight = FontWeight.Bold); Text(subtitle, color = StudioTextMuted, fontSize = 13.sp) }
      Text(action, color = if (danger) StudioDanger else StudioSecondary)
    }
  }
}

private fun copyFor(languageCode: LanguageCode) = if (languageCode == LanguageCode.Vi) {
  Copy("Tao du an moi", "Du an gan day", "Cai dat", "Thoat", "Them media", "Xuat", "Ngon ngu", "Tiep tuc", "Bang du an", "Trinh bien tap")
} else {
  Copy("Create New Project", "Recent Projects", "Settings", "Exit", "Add Media", "Export", "Language", "Continue", "Dashboard", "Editor")
}

private fun Copy.onboardingPages() = if (language == "Ngon ngu") {
  listOf(
    IntroPage("Nhap media cuc bo", "Dua video, anh va am thanh vao du an offline-friendly.", StudioPrimary),
    IntroPage("Dong bo moi chinh sua", "Playhead trung tam giu preview, clip va timecode khop nhau.", StudioSecondary),
    IntroPage("Xuat va chia se", "Render preset MP4 cho Shorts, Reels, TikTok va clip ca nhan.", StudioAccent),
  )
} else {
  listOf(
    IntroPage("Import local media", "Bring videos, images, and audio into an offline-friendly project.", StudioPrimary),
    IntroPage("Sync every edit", "A centered playhead keeps preview, clips, overlays, and timecode aligned.", StudioSecondary),
    IntroPage("Export and share", "Render MP4 presets for Shorts, Reels, TikTok, and personal clips.", StudioAccent),
  )
}

private data class UriMetadata(val displayName: String?, val sizeBytes: Long?, val mimeType: String?)

private fun Context.readUriMetadata(uri: Uri): UriMetadata {
  var displayName: String? = null
  var sizeBytes: Long? = null
  contentResolver.query(uri, null, null, null, null)?.use { cursor ->
    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
    if (cursor.moveToFirst()) {
      if (nameIndex >= 0) displayName = cursor.getString(nameIndex)
      if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
    }
  }
  return UriMetadata(displayName = displayName, sizeBytes = sizeBytes, mimeType = contentResolver.getType(uri))
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  MyApplicationTheme { MainScreen(onItemClick = {}) }
}
