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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.clipystudio.data.AppState
import com.example.clipystudio.data.AudioSource
import com.example.clipystudio.data.CanvasBackground
import com.example.clipystudio.data.CanvasRatio
import com.example.clipystudio.data.ClipAction
import com.example.clipystudio.data.ClipType
import com.example.clipystudio.data.EditorTool
import com.example.clipystudio.data.EffectCategory
import com.example.clipystudio.data.EffectLibrary
import com.example.clipystudio.data.EffectPreset
import com.example.clipystudio.data.ExportResolution
import com.example.clipystudio.data.ExportStatus
import com.example.clipystudio.data.FilterAdjustmentSet
import com.example.clipystudio.data.LanguageCode
import com.example.clipystudio.data.MediaAsset
import com.example.clipystudio.data.MediaType
import com.example.clipystudio.data.Project
import com.example.clipystudio.data.QualityPreset
import com.example.clipystudio.data.StickerAsset
import com.example.clipystudio.data.StickerCategory
import com.example.clipystudio.data.StickerLibrary
import com.example.clipystudio.data.Timeline
import com.example.clipystudio.data.TimelineThumbnailCache
import com.example.clipystudio.data.TimelineClip
import com.example.clipystudio.data.TimelineEngine
import com.example.clipystudio.data.TimelineTrack
import com.example.clipystudio.data.TrackType
import com.example.clipystudio.data.TransitionType
import com.example.clipystudio.data.TrimHandle
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
import kotlin.math.abs
import kotlin.math.roundToInt

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
  var showExitDialog by remember { mutableStateOf(false) }
  BackHandler { showExitDialog = true }
  StudioScreen(horizontalPadding = 10.dp) {
    EditorTopBar(
      title = project.name,
      version = project.autosaveVersion,
      canUndo = appState.undoStack.isNotEmpty(),
      canRedo = appState.redoStack.isNotEmpty(),
      onBack = { showExitDialog = true },
      onUndo = viewModel::undo,
      onRedo = viewModel::redo,
      onExport = onExport,
    )
    PreviewCanvas(project.canvasRatio, timeline, viewModel::selectClip, viewModel::deleteSelectedClip, viewModel::transformSelectedClip, viewModel::updateCanvasRatio)
    PlaybackControls(timeline, viewModel::togglePlayback, viewModel::seekBy)
    LaunchedEffect(timeline.isPlaying) {
      while (timeline.isPlaying) {
        delay(250)
        viewModel.tickPlayback(250)
      }
    }
    TimelineView(timeline, viewModel::selectClip, viewModel::seekTo, viewModel::scrollTimelineTo, viewModel::updateTimelineZoom, viewModel::trimSelectedClipEdge, viewModel::dragSelectedClip, viewModel::splitSelectedClip, viewModel::reorderSelectedVideoClip)
    Spacer(Modifier.height(8.dp))
    val selectedClip = timeline.tracks.flatMap { it.clips }.firstOrNull { it.id == timeline.selectedClipId }
    when {
      selectedClip != null && timeline.selectedTool == EditorTool.Edit -> ClipEditPanel(selectedClip, viewModel)
      timeline.selectedTool == EditorTool.Audio -> AudioToolPanel(selectedClip, viewModel)
      timeline.selectedTool == EditorTool.Text -> TextToolPanel(selectedClip, viewModel)
      timeline.selectedTool == EditorTool.Sticker -> StickerToolPanel(timeline, viewModel)
      timeline.selectedTool == EditorTool.Filter -> FilterAdjustPanel(selectedClip, viewModel)
      timeline.selectedTool == EditorTool.Effect -> EffectToolPanel(viewModel)
      timeline.selectedTool == EditorTool.Transition -> TransitionToolPanel(timeline, viewModel)
      timeline.selectedTool == EditorTool.Canvas -> CanvasToolPanel(project.canvasRatio, timeline.canvasBackground, viewModel)
      timeline.selectedTool == EditorTool.Speed -> SpeedToolPanel(selectedClip, viewModel)
      timeline.selectedTool == EditorTool.Overlay -> OverlayToolPanel(project.importedAssets, selectedClip, viewModel)
      else -> ToolPanel(timeline, viewModel)
    }
    Spacer(Modifier.height(8.dp))
    ToolRail(timeline.selectedTool, viewModel::updateSelectedTool, onImport, onExport)
  }
  if (showExitDialog) {
    AlertDialog(
      onDismissRequest = { showExitDialog = false },
      title = { Text(if (appState.languageCode == LanguageCode.Vi) "Luu truoc khi thoat?" else "Save before leaving?") },
      text = { Text(if (appState.languageCode == LanguageCode.Vi) "Du an da duoc tu dong luu. Ban co the luu va thoat, bo qua thay doi dang chon, hoac tiep tuc bien tap." else "Your project is autosaved. Save and exit, discard the current editor selection, or keep editing.") },
      confirmButton = { TextButton(onClick = onBack) { Text(if (appState.languageCode == LanguageCode.Vi) "Luu & thoat" else "Save & Exit") } },
      dismissButton = {
        Row {
          TextButton(onClick = onBack) { Text(if (appState.languageCode == LanguageCode.Vi) "Bo qua" else "Discard", color = StudioDanger) }
          TextButton(onClick = { showExitDialog = false }) { Text(if (appState.languageCode == LanguageCode.Vi) "Huy" else "Cancel") }
        }
      },
    )
  }
}

@Composable
private fun EditorTopBar(title: String, version: Long, canUndo: Boolean, canRedo: Boolean, onBack: () -> Unit, onUndo: () -> Unit, onRedo: () -> Unit, onExport: () -> Unit) {
  Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
    TextButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back from editor" }) { Text("Back") }
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
      Text("Autosaved v$version", color = StudioSecondary, fontSize = 11.sp)
    }
    TextButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.semantics { contentDescription = "Undo edit" }) { Text("Undo") }
    TextButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.semantics { contentDescription = "Redo edit" }) { Text("Redo") }
    Button(onClick = onExport, shape = RoundedCornerShape(999.dp), modifier = Modifier.height(44.dp).semantics { contentDescription = "Export project" }) { Text("Export") }
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
    if (appState.exportJob?.status == ExportStatus.Complete) {
      ExportSuccessPanel(appState, onBack, onDashboard)
      return@StudioScreen
    }
    if (appState.exportJob?.status == ExportStatus.Running || appState.exportJob?.status == ExportStatus.Cancelled || appState.exportJob?.status == ExportStatus.Failed) {
      ExportProgressPanel(appState, viewModel, onBack)
      return@StudioScreen
    }
    ExportOptionCard("Format", settings.format, "MP4 is the MVP target for Android compatibility.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf("MP4", "MOV").forEach { format -> FilterChip(selected = settings.format == format, onClick = { viewModel.updateExportSettings(settings.copy(format = format)) }, label = { Text(format) }) }
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
    Spacer(Modifier.weight(1f))
    Button(onClick = viewModel::startExport, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(999.dp)) { Text(if (appState.languageCode == LanguageCode.Vi) "Bat dau xuat" else "Start Export") }
  }
}

@Composable
private fun ExportProgressPanel(appState: AppState, viewModel: MainScreenViewModel, onBack: () -> Unit) {
  val job = appState.exportJob ?: return
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text("Export progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      Text(job.stageLabel, color = StudioTextMuted)
      LinearProgressIndicator(progress = { job.progressPercent / 100f }, modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp).height(10.dp), color = StudioSecondary)
      Text("${job.progressPercent}% · ${job.status}", color = if (job.status == ExportStatus.Failed) StudioDanger else StudioSecondary, fontWeight = FontWeight.Bold)
      job.errorMessage?.let { Text(it, color = StudioDanger, fontSize = 13.sp) }
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (job.status == ExportStatus.Running) OutlinedButton(onClick = viewModel::cancelExport) { Text("Cancel", color = StudioDanger) }
        if (job.status == ExportStatus.Cancelled || job.status == ExportStatus.Failed) Button(onClick = viewModel::startExport) { Text("Retry") }
        OutlinedButton(onClick = onBack) { Text("Return to editor") }
      }
    }
  }
}

@Composable
private fun ExportSuccessPanel(appState: AppState, onBack: () -> Unit, onDashboard: () -> Unit) {
  val job = appState.exportJob ?: return
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(StudioPrimary.copy(alpha = 0.45f), StudioSecondary.copy(alpha = 0.22f)))), contentAlignment = Alignment.Center) {
        Text("Final video preview\n${job.outputUri.orEmpty()}", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
      }
      Spacer(Modifier.height(14.dp))
      Text("Export complete", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      Text("${job.settings.format} · ${job.settings.resolution.label} · ${job.settings.fps} FPS · ${job.settings.qualityPreset.label}", color = StudioTextMuted)
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {}) { Text("Save to gallery") }
        Button(onClick = {}) { Text("Share") }
        OutlinedButton(onClick = onBack) { Text("Return to editor") }
        OutlinedButton(onClick = onDashboard) { Text("Dashboard") }
      }
    }
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
private fun PreviewCanvas(ratio: CanvasRatio, timeline: Timeline, onSelect: (String) -> Unit, onDelete: () -> Unit, onTransform: (Float, Float, Float, Float) -> Unit, onRatio: (CanvasRatio) -> Unit) {
  val ratioValue = when (ratio) { CanvasRatio.Portrait -> 9f / 16f; CanvasRatio.Square -> 1f; CanvasRatio.Landscape -> 16f / 9f; CanvasRatio.FourFive -> 4f / 5f; CanvasRatio.Original -> 3f / 4f }
  val glow by animateFloatAsState(if (timeline.isPlaying) 1f else 0.35f, label = "previewGlow")
  val composition = remember(timeline) { TimelineEngine.resolveActiveComposition(timeline) }
  val activeIds = buildSet { composition.video?.let { add(it.clipId) }; addAll(composition.audio.map { it.clipId }); addAll(composition.text.map { it.clipId }); addAll(composition.stickers.map { it.clipId }); addAll(composition.overlays.map { it.clipId }); addAll(composition.effects.map { it.clipId }) }
  val activeClips = timeline.tracks.flatMap { it.clips }.filter { it.id in activeIds }.sortedBy { it.zIndex }
  val selectedClip = activeClips.firstOrNull { it.id == timeline.selectedClipId }
  Column(Modifier.fillMaxWidth()) {
    Box(Modifier.fillMaxWidth().height(282.dp).clip(RoundedCornerShape(24.dp)).background(StudioSurfaceHigh), contentAlignment = Alignment.Center) {
      val backgroundColor = runCatching { Color(android.graphics.Color.parseColor(timeline.canvasBackground.color)) }.getOrDefault(StudioBackground)
      Box(Modifier.fillMaxHeight(0.88f).aspectRatio(ratioValue).clip(RoundedCornerShape(18.dp)).background(if (timeline.canvasBackground.blurEnabled) Brush.radialGradient(listOf(StudioPrimary.copy(alpha = 0.28f + timeline.canvasBackground.blurStrength * 0.24f), backgroundColor)) else Brush.radialGradient(listOf(StudioPrimary.copy(alpha = 0.55f * glow), backgroundColor)))) {
        Box(Modifier.fillMaxSize().pointerInput(timeline.selectedClipId) { detectTransformGestures { _, pan, zoom, rotation -> if (timeline.selectedClipId != null) onTransform(pan.x / 600f, pan.y / 900f, zoom, rotation) } })
        Canvas(Modifier.fillMaxSize()) {
          drawRect(Color.White.copy(alpha = 0.06f), style = Stroke(width = 2.dp.toPx()))
          drawCircle(StudioSecondary.copy(alpha = glow), radius = 18.dp.toPx(), center = center)
          if (selectedClip != null) {
            drawLine(StudioSecondary.copy(alpha = 0.35f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 1.dp.toPx())
            drawLine(StudioSecondary.copy(alpha = 0.35f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1.dp.toPx())
          }
        }
        activeClips.filter { it.clipType == ClipType.Text || it.clipType == ClipType.Sticker || it.clipType == ClipType.Overlay }.forEach { clip ->
          PreviewLayerChip(clip, selected = clip.id == timeline.selectedClipId, onSelect = { onSelect(clip.id) }, onDelete = onDelete)
        }
        composition.transition?.let { transition ->
          Text("${transition.type.label} transition", modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(999.dp)).background(StudioBackground.copy(alpha = 0.72f)).padding(horizontal = 12.dp, vertical = 8.dp), color = StudioSecondary, fontWeight = FontWeight.Bold)
        }
        Text("Preview ${timeline.playheadMs.asTimecode()}", modifier = Modifier.align(Alignment.TopCenter).padding(10.dp), fontWeight = FontWeight.Bold)
        Text("Tap overlays, drag/pinch/rotate selected layer", modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp), color = StudioTextMuted, fontSize = 12.sp)
      }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      CanvasRatio.entries.forEach { item -> FilterChip(selected = ratio == item, onClick = { onRatio(item) }, label = { Text(item.label, fontSize = 12.sp) }) }
    }
  }
}

@Composable
private fun PreviewLayerChip(clip: TimelineClip, selected: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
  val startPadding = ((clip.transform.positionX * 240).coerceIn(12f, 220f)).dp
  val topPadding = ((clip.transform.positionY * 180).coerceIn(18f, 170f)).dp
  Box(Modifier.fillMaxSize()) {
    Box(Modifier.align(Alignment.TopStart).padding(start = startPadding, top = topPadding).clip(RoundedCornerShape(14.dp)).background(StudioBackground.copy(alpha = 0.72f)).border(if (selected) 2.dp else 1.dp, if (selected) StudioPrimary else Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp)).clickable(onClick = onSelect).pointerInput(clip.id) { detectTapGestures(onDoubleTap = { onSelect() }, onTap = { onSelect() }) }.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
      Text(if (clip.clipType == ClipType.Text) clip.textProperties.content else clip.title, fontSize = clip.textProperties.fontSizeSp.coerceIn(14f, 34f).sp, maxLines = 2, textAlign = TextAlign.Center)
    }
    if (selected) {
      TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).semantics { contentDescription = "Delete selected overlay" }) { Text("Delete", color = StudioDanger) }
      Box(Modifier.align(Alignment.Center).size(10.dp).clip(CircleShape).background(StudioPrimary))
      Text("Rotate", modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp), color = StudioSecondary, fontSize = 11.sp)
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
private fun TimelineView(timeline: Timeline, onSelect: (String) -> Unit, onSeek: (Long) -> Unit, onScroll: (Float) -> Unit, onZoom: (Float) -> Unit, onTrim: (TrimHandle, Long) -> Unit, onMove: (Long) -> Unit, onSplit: () -> Unit, onReorder: (Int) -> Unit) {
  val projectTimeline = remember(timeline) { TimelineEngine.toProjectTimeline(timeline) }
  val pxPerSecond = timeline.pixelsPerSecond * timeline.zoomLevel
  val contentWidth = ((timeline.durationMs / 1_000f) * pxPerSecond).roundToInt().coerceAtLeast(640)
  val activeComposition = remember(timeline) { TimelineEngine.resolveActiveComposition(timeline) }
  val activeIds = remember(activeComposition) { buildSet { activeComposition.video?.let { add(it.clipId) }; addAll(activeComposition.audio.map { it.clipId }); addAll(activeComposition.text.map { it.clipId }); addAll(activeComposition.stickers.map { it.clipId }); addAll(activeComposition.overlays.map { it.clipId }); addAll(activeComposition.effects.map { it.clipId }) } }
  val viewportWidthPx = 440f
  val thumbnailCache = remember { TimelineThumbnailCache(maxEntries = 72) }
  val visibleRange = remember(timeline.scrollOffsetPx, timeline.zoomLevel, timeline.version) { TimelineEngine.visibleRange(timeline, viewportWidthPx) }
  val thumbnailRequests = remember(visibleRange, timeline.version) { TimelineEngine.planThumbnailRequests(timeline, visibleRange, thumbnailCache.snapshot()) }
  LaunchedEffect(thumbnailRequests) { thumbnailRequests.forEach { thumbnailCache.put(com.example.clipystudio.data.TimelineThumbnailState(it.clipId, it.cacheKey, com.example.clipystudio.data.ThumbnailStatus.Ready, System.currentTimeMillis(), System.currentTimeMillis())) } }
  var snapLabel by remember { mutableStateOf<String?>(null) }
  Box(Modifier.fillMaxWidth().height(312.dp).clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.30f))) {
    Column(Modifier.fillMaxSize().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      TimelineHeader(timeline, contentWidth, onSeek, onScroll, onZoom)
      timeline.tracks.sortedBy { it.orderIndex }.forEach { track ->
        EngineTrackLane(projectTimeline, timeline, track, contentWidth, activeIds, onSelect, onTrim, { delta ->
          onMove(delta)
          val selected = timeline.selectedClipId
          val target = selected?.let { track.clips.firstOrNull { clip -> clip.id == it } }?.let { TimelineEngine.resolveSnap(timeline, track.type, it.id, (it.startMs + delta).coerceAtLeast(0L)) }
          snapLabel = target?.takeIf { it.isSnapped }?.targetType?.name?.lowercase()?.replaceFirstChar { char -> char.uppercase() }
        }, onSplit, onReorder)
      }
    }
    TimelineGuides(timeline, contentWidth)
    snapLabel?.let { Text(it, modifier = Modifier.align(Alignment.TopCenter).padding(top = 30.dp).clip(RoundedCornerShape(999.dp)).background(StudioSurface.copy(alpha = 0.90f)).padding(horizontal = 9.dp, vertical = 3.dp), color = StudioAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    Text("${(timeline.zoomLevel * 100).roundToInt()}% · ${thumbnailRequests.size} visible thumbs · Saved v${timeline.version}", modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(999.dp)).background(StudioSurface.copy(alpha = 0.88f)).padding(horizontal = 8.dp, vertical = 3.dp), color = StudioTextMuted, fontSize = 10.sp)
    Box(Modifier.align(Alignment.TopCenter).width(2.dp).fillMaxHeight().background(StudioSecondary))
    Column(Modifier.align(Alignment.TopCenter).padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Box(Modifier.size(13.dp).clip(CircleShape).background(StudioSecondary))
      Text(timeline.playheadMs.asTimecode(), color = StudioSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(StudioSurface.copy(alpha = 0.82f), RoundedCornerShape(8.dp)).padding(horizontal = 5.dp, vertical = 2.dp))
    }
  }
}

@Composable
private fun TimelineHeader(timeline: Timeline, contentWidth: Int, onSeek: (Long) -> Unit, onScroll: (Float) -> Unit, onZoom: (Float) -> Unit) {
  val scrollState = rememberScrollState(timeline.scrollOffsetPx.roundToInt())
  LaunchedEffect(scrollState.value) { onScroll(scrollState.value.toFloat()) }
  Row(Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("Ruler", modifier = Modifier.width(58.dp), fontSize = 12.sp, color = StudioTextMuted)
    Box(Modifier.weight(1f).height(34.dp).pointerInput(timeline.id, timeline.zoomLevel) { detectTransformGestures { centroid, _, zoom, _ -> if (abs(zoom - 1f) > 0.02f) onZoom(zoom - 1f) } }.horizontalScroll(scrollState)) {
      Canvas(Modifier.width(contentWidth.dp).fillMaxHeight().semantics { contentDescription = "Scrollable timeline ruler at ${timeline.playheadMs.asTimecode()}" }) {
        val pxPerMs = timeline.pixelsPerSecond * timeline.zoomLevel / 1_000f
        for (tick in 0..timeline.durationMs step 1_000L) {
          val x = tick * pxPerMs
          drawLine(if (kotlin.math.abs(tick - timeline.playheadMs) < 550) StudioSecondary else StudioTextMuted.copy(alpha = 0.5f), Offset(x, 6f), Offset(x, size.height), strokeWidth = 2f)
        }
      }
      Row(Modifier.width(contentWidth.dp).fillMaxHeight(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        (0..timeline.durationMs step 2_000L).forEach { tick -> Text(tick.asTimecode(), color = StudioTextMuted, fontSize = 10.sp, modifier = Modifier.clickable { onSeek(tick) }) }
      }
      timeline.markers.forEach { marker ->
        val left = ((marker.timeMs / 1_000f) * timeline.pixelsPerSecond * timeline.zoomLevel).roundToInt()
        Text(marker.label, modifier = Modifier.offset { IntOffset(left, 0) }.clip(RoundedCornerShape(999.dp)).background(StudioAccent.copy(alpha = 0.24f)).padding(horizontal = 4.dp), color = StudioAccent, fontSize = 9.sp)
      }
    }
    TextButton(onClick = { onZoom(-0.2f) }, modifier = Modifier.size(42.dp)) { Text("-") }
    TextButton(onClick = { onZoom(0.2f) }, modifier = Modifier.size(42.dp)) { Text("+") }
  }
}

@Composable
private fun EngineTrackLane(projectTimeline: com.example.clipystudio.data.ProjectTimeline, timeline: Timeline, track: TimelineTrack, contentWidth: Int, activeIds: Set<String>, onSelect: (String) -> Unit, onTrim: (TrimHandle, Long) -> Unit, onMove: (Long) -> Unit, onSplit: () -> Unit, onReorder: (Int) -> Unit) {
  val scrollState = rememberScrollState(projectTimeline.scrollOffsetPx.roundToInt())
  Row(Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(track.type.label, modifier = Modifier.width(58.dp), fontSize = 12.sp, color = StudioTextMuted)
    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(StudioSurface.copy(alpha = 0.55f)).horizontalScroll(scrollState)) {
      Box(Modifier.width(contentWidth.dp).fillMaxHeight()) {
        track.clips.sortedBy { it.startMs }.forEachIndexed { index, clip ->
          EngineClipBlock(track.type, clip, index, selected = projectTimeline.selectedClipId == clip.id, active = clip.id in activeIds, zoom = projectTimeline.zoomScale, pixelsPerSecond = projectTimeline.pixelsPerSecond, transition = timeline.transitions.firstOrNull { it.fromClipId == clip.id || it.toClipId == clip.id }, onSelect, onTrim, onMove, onSplit, onReorder)
        }
      }
    }
  }
}

@Composable
private fun EngineClipBlock(trackType: TrackType, clip: TimelineClip, index: Int, selected: Boolean, active: Boolean, zoom: Float, pixelsPerSecond: Float, transition: com.example.clipystudio.data.Transition?, onSelect: (String) -> Unit, onTrim: (TrimHandle, Long) -> Unit, onMove: (Long) -> Unit, onSplit: () -> Unit, onReorder: (Int) -> Unit) {
  val color = when (trackType) { TrackType.Video -> StudioPrimary; TrackType.Audio -> StudioSecondary; TrackType.Text -> StudioAccent; TrackType.Sticker -> Color(0xFFFF65B3); TrackType.Effect -> Color(0xFF55A7FF); TrackType.Overlay -> Color(0xFF56E58A) }
  val left = ((clip.startMs / 1_000f) * pixelsPerSecond * zoom).roundToInt()
  val width = ((clip.durationMs / 1_000f) * pixelsPerSecond * zoom).roundToInt().coerceAtLeast(56)
  Box(
    Modifier.offset { IntOffset(left, 0) }.width(width.dp).fillMaxHeight().padding(vertical = 2.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = if (selected) 0.92f else if (active) 0.76f else 0.58f)).border(if (selected) 2.dp else 1.dp, if (selected) Color.White else if (active) StudioSecondary else Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)).clickable { onSelect(clip.id) }.pointerInput(clip.id, selected) {
      detectTapGestures(onDoubleTap = { onSelect(clip.id); onSplit() }, onLongPress = { onSelect(clip.id); if (trackType == TrackType.Video) onReorder(index + 1) else onMove(250) })
    }.pointerInput(clip.id) {
      detectHorizontalDragGestures(onDragStart = { onSelect(clip.id) }, onHorizontalDrag = { _, dragAmount -> onMove((dragAmount / (pixelsPerSecond * zoom) * 1_000f).roundToInt().toLong()) })
    }.semantics { contentDescription = "${clip.clipType} clip, ${trackType.label} track, starts at ${clip.startMs.asTimecode()}, duration ${clip.durationMs.asTimecode()}" },
    contentAlignment = Alignment.Center,
  ) {
    if (trackType == TrackType.Video || trackType == TrackType.Overlay) {
      Row(Modifier.matchParentSize().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat((width / 44).coerceIn(1, 8)) { Box(Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.10f))))) }
      }
    }
    if (active) Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(3.dp).background(StudioSecondary.copy(alpha = 0.82f)))
    transition?.let { Box(Modifier.align(if (it.fromClipId == clip.id) Alignment.CenterEnd else Alignment.CenterStart).width(18.dp).fillMaxHeight().background(StudioAccent.copy(alpha = 0.36f))) }
    clip.keyframes.distinctBy { it.timeMs }.forEach { keyframe ->
      val kx = ((keyframe.timeMs.toFloat() / clip.durationMs.coerceAtLeast(1L)) * width).roundToInt().coerceIn(8, width - 8)
      Box(Modifier.offset { IntOffset(kx - 4, 6) }.size(8.dp).background(StudioAccent, RoundedCornerShape(2.dp)))
    }
    Text(clip.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 16.dp))
    if (selected) {
      Box(Modifier.align(Alignment.CenterStart).width(12.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.82f)).clickable { onTrim(TrimHandle.Left, -250) }.semantics { contentDescription = "Trim left edge" })
      Box(Modifier.align(Alignment.CenterEnd).width(12.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.82f)).clickable { onTrim(TrimHandle.Right, 250) }.semantics { contentDescription = "Trim right edge" })
    }
  }
}

@Composable
private fun TimelineGuides(timeline: Timeline, contentWidth: Int) {
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
  }
}

@Composable
private fun ToolRail(selected: EditorTool, onSelect: (EditorTool) -> Unit, onImport: () -> Unit, onExport: () -> Unit) {
  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    item { FilterChip(selected = false, onClick = onImport, label = { Text("+ Media") }) }
    items(EditorTool.entries, key = { it.name }) { tool -> FilterChip(selected = selected == tool, onClick = { if (tool == EditorTool.Export) onExport() else onSelect(tool) }, label = { Text(tool.label) }) }
  }
}

@Composable
private fun ClipEditPanel(selectedClip: TimelineClip, viewModel: MainScreenViewModel) {
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Edit ${selectedClip.clipType}", fontWeight = FontWeight.Bold)
      Text("${selectedClip.startMs.asTimecode()} · ${selectedClip.durationMs.asTimecode()} · ${selectedClip.title}", color = StudioTextMuted, fontSize = 13.sp)
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::splitSelectedClip, modifier = Modifier.semantics { contentDescription = "Split selected clip" }) { Text("Split") }
        OutlinedButton(onClick = viewModel::deleteSelectedClip, modifier = Modifier.semantics { contentDescription = "Delete selected clip" }) { Text("Delete") }
        OutlinedButton(onClick = viewModel::duplicateSelectedClip) { Text("Duplicate") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.SpeedDown) }) { Text("Speed -") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.SpeedUp) }) { Text("Speed +") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.VolumeDown) }) { Text("Volume -") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.VolumeUp) }) { Text("Volume +") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.Replace) }) { Text("Replace") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.Mute) }) { Text("Mute") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.Crop) }) { Text("Crop") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.Rotate) }) { Text("Rotate") }
        OutlinedButton(onClick = { viewModel.adjustSelectedClip(ClipAction.Flip) }) { Text("Flip") }
      }
    }
  }
}

@Composable
private fun StickerToolPanel(timeline: Timeline, viewModel: MainScreenViewModel) {
  var category by remember { mutableStateOf(StickerCategory.Emoji) }
  val assets = if (category == StickerCategory.Recent) timeline.recentStickers.ifEmpty { StickerLibrary.take(4) } else StickerLibrary.filter { it.category == category }
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Sticker library", fontWeight = FontWeight.Bold)
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StickerCategory.entries.forEach { item -> FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item.label) }) } }
      LazyRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(assets, key = { it.id }) { asset -> StickerTile(asset) { viewModel.addStickerAtPlayhead(asset) } } }
      LayerActions(viewModel)
    }
  }
}

@Composable
private fun StickerTile(asset: StickerAsset, onClick: () -> Unit) {
  Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = StudioSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.size(86.dp).semantics { contentDescription = "Insert ${asset.label} sticker" }) {
    Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
      Text(asset.symbol, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
      Text(asset.label, color = StudioTextMuted, fontSize = 11.sp, maxLines = 1)
    }
  }
}

@Composable
private fun FilterAdjustPanel(selectedClip: TimelineClip?, viewModel: MainScreenViewModel) {
  var adjustments by remember(selectedClip?.id) { mutableStateOf(selectedClip?.filterAdjustments ?: FilterAdjustmentSet()) }
  val filters = listOf(null to "Original", "warm" to "Warm", "cool" to "Cool", "vintage" to "Vintage", "cinematic" to "Cinematic", "bw" to "B&W")
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Filter and adjust", fontWeight = FontWeight.Bold)
      if (selectedClip == null) Text("Select a video, image, overlay, or text layer to adjust.", color = StudioTextMuted, fontSize = 13.sp)
      LazyRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(filters) { pair -> FilterPreviewChip(pair.second, selectedClip?.filterAdjustments?.filterId == pair.first) { viewModel.updateSelectedFilter(pair.first) } } }
      AdjustmentControl("Brightness", adjustments.brightness, 0.5f, 1.5f) { adjustments = adjustments.copy(brightness = it); viewModel.updateSelectedAdjustments(adjustments) }
      AdjustmentControl("Contrast", adjustments.contrast, 0.5f, 1.6f) { adjustments = adjustments.copy(contrast = it); viewModel.updateSelectedAdjustments(adjustments) }
      AdjustmentControl("Saturation", adjustments.saturation, 0f, 2f) { adjustments = adjustments.copy(saturation = it); viewModel.updateSelectedAdjustments(adjustments) }
      AdjustmentControl("Exposure", adjustments.exposure, -1f, 1f) { adjustments = adjustments.copy(exposure = it); viewModel.updateSelectedAdjustments(adjustments) }
      AdjustmentControl("Temperature", adjustments.temperature, -1f, 1f) { adjustments = adjustments.copy(temperature = it); viewModel.updateSelectedAdjustments(adjustments) }
      AdjustmentControl("Sharpness", adjustments.sharpness, 0f, 1f) { adjustments = adjustments.copy(sharpness = it); viewModel.updateSelectedAdjustments(adjustments) }
    }
  }
}

@Composable
private fun FilterPreviewChip(label: String, selected: Boolean, onClick: () -> Unit) {
  Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = if (selected) StudioPrimary.copy(alpha = 0.45f) else StudioSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.size(92.dp, 66.dp)) {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(StudioPrimary.copy(alpha = 0.4f), StudioSecondary.copy(alpha = 0.28f)))), contentAlignment = Alignment.Center) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
  }
}

@Composable
private fun EffectToolPanel(viewModel: MainScreenViewModel) {
  var category by remember { mutableStateOf(EffectCategory.Basic) }
  val effects = EffectLibrary.filter { it.category == category }
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Effects", fontWeight = FontWeight.Bold)
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { EffectCategory.entries.forEach { item -> FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item.label) }) } }
      LazyRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(effects, key = { it.id }) { effect -> EffectTile(effect) { viewModel.addEffectAtPlayhead(effect) } } }
      LayerActions(viewModel)
    }
  }
}

@Composable
private fun EffectTile(effect: EffectPreset, onClick: () -> Unit) {
  Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = StudioSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.size(104.dp, 76.dp).semantics { contentDescription = "Apply ${effect.label} effect" }) {
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Center) { Text(effect.label, fontWeight = FontWeight.Bold); Text(effect.category.label, color = StudioTextMuted, fontSize = 12.sp) }
  }
}

@Composable
private fun TransitionToolPanel(timeline: Timeline, viewModel: MainScreenViewModel) {
  var duration by remember { mutableStateOf(800L) }
  val videoClips = timeline.tracks.firstOrNull { it.type == TrackType.Video }?.clips.orEmpty()
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Transitions", fontWeight = FontWeight.Bold)
      Text(if (videoClips.size >= 2) "Apply between adjacent video clips near the playhead." else "Add at least two video/image clips to enable transitions.", color = StudioTextMuted, fontSize = 13.sp)
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { TransitionType.entries.forEach { type -> Button(onClick = { viewModel.applyTransition(type, duration) }, enabled = videoClips.size >= 2, shape = RoundedCornerShape(999.dp)) { Text(type.label) } } }
      AdjustmentControl("Duration ms", duration.toFloat(), 300f, 2_000f) { duration = it.toLong() }
      OutlinedButton(onClick = viewModel::removeTransition, modifier = Modifier.padding(top = 8.dp)) { Text("Remove transition") }
    }
  }
}

@Composable
private fun SpeedToolPanel(selectedClip: TimelineClip?, viewModel: MainScreenViewModel) {
  val speed = selectedClip?.videoProperties?.speed ?: 1f
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Speed", fontWeight = FontWeight.Bold)
      Text(selectedClip?.let { "${it.title} · ${it.durationMs.asTimecode()} at ${"%.2f".format(speed)}x" } ?: "Select a compatible clip.", color = StudioTextMuted, fontSize = 13.sp)
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(0.5f, 1f, 1.5f, 2f).forEach { value -> FilterChip(selected = speed == value, onClick = { viewModel.updateSelectedSpeed(value) }, label = { Text("${value}x") }) } }
      AdjustmentControl("Speed", speed, 0.5f, 2f) { viewModel.updateSelectedSpeed(it) }
    }
  }
}

@Composable
private fun OverlayToolPanel(importedAssets: List<MediaAsset>, selectedClip: TimelineClip?, viewModel: MainScreenViewModel) {
  val overlayAssets = importedAssets.filter { it.type != MediaType.Audio }.ifEmpty { listOf(MediaAsset("sample-overlay", "local://overlay/sample", MediaType.Image, "Sample overlay", 4_000, 1_200_000)) }
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Overlay", fontWeight = FontWeight.Bold)
      LazyRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(overlayAssets, key = { it.id }) { asset -> MediaMiniCard(asset) { viewModel.addOverlayAtPlayhead(asset) } } }
      if (selectedClip?.clipType == ClipType.Overlay) AdjustmentControl("Opacity", selectedClip.transform.opacity, 0f, 1f) { viewModel.updateSelectedOpacity(it) }
      LayerActions(viewModel)
    }
  }
}

@Composable
private fun MediaMiniCard(asset: MediaAsset, onClick: () -> Unit) {
  Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = StudioSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.size(126.dp, 74.dp)) {
    Column(Modifier.padding(10.dp)) { Text(asset.displayName, maxLines = 1, fontWeight = FontWeight.Bold); Text("${asset.type.label} · ${asset.durationMs.asTimecode()}", color = StudioTextMuted, fontSize = 12.sp) }
  }
}

@Composable
private fun AudioToolPanel(selectedClip: TimelineClip?, viewModel: MainScreenViewModel) {
  var tab by remember { mutableStateOf(AudioSource.BuiltInMusic) }
  val items = when (tab) {
    AudioSource.DeviceMusic -> listOf("Device track placeholder" to "02:14", "Local song metadata" to "01:08")
    AudioSource.BuiltInMusic -> listOf("Neon pulse" to "00:18", "Lo-fi creator bed" to "00:30")
    AudioSource.ExtractedAudio -> listOf("Extract from selected video" to "linked", "Voice layer sample" to "00:11")
    AudioSource.SoundEffect -> listOf("Camera click" to "00:01", "Whoosh pop" to "00:02")
  }
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { AudioSource.entries.forEach { source -> FilterChip(selected = tab == source, onClick = { tab = source }, label = { Text(source.label) }) } }
      items.forEach { item ->
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) { Text(item.first, fontWeight = FontWeight.Bold); Text(item.second, color = StudioTextMuted, fontSize = 12.sp) }
          TextButton(onClick = { viewModel.updateSelectedAudio(selectedClip?.audioProperties?.volume ?: 0.72f, selectedClip?.audioProperties?.fadeInMs ?: 300, selectedClip?.audioProperties?.fadeOutMs ?: 500, selectedClip?.audioProperties?.loopEnabled?.not() ?: false) }) { Text("Play") }
          Button(onClick = { viewModel.addAudioClipAtPlayhead(item.first, tab) }, shape = RoundedCornerShape(999.dp)) { Text("Add") }
        }
      }
      selectedClip?.takeIf { it.clipType == ClipType.Audio }?.let { clip ->
        AdjustmentControl("Volume", clip.audioProperties.volume, 0f, 1f) { viewModel.updateSelectedAudio(it, clip.audioProperties.fadeInMs, clip.audioProperties.fadeOutMs, clip.audioProperties.loopEnabled) }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(selected = clip.audioProperties.fadeInMs > 0, onClick = { viewModel.updateSelectedAudio(clip.audioProperties.volume, if (clip.audioProperties.fadeInMs > 0) 0 else 600, clip.audioProperties.fadeOutMs, clip.audioProperties.loopEnabled) }, label = { Text("Fade in") })
          FilterChip(selected = clip.audioProperties.fadeOutMs > 0, onClick = { viewModel.updateSelectedAudio(clip.audioProperties.volume, clip.audioProperties.fadeInMs, if (clip.audioProperties.fadeOutMs > 0) 0 else 600, clip.audioProperties.loopEnabled) }, label = { Text("Fade out") })
          FilterChip(selected = clip.audioProperties.loopEnabled, onClick = { viewModel.updateSelectedAudio(clip.audioProperties.volume, clip.audioProperties.fadeInMs, clip.audioProperties.fadeOutMs, !clip.audioProperties.loopEnabled) }, label = { Text("Loop") })
        }
        LayerActions(viewModel)
      }
    }
  }
}

@Composable
private fun TextToolPanel(selectedClip: TimelineClip?, viewModel: MainScreenViewModel) {
  var text by remember(selectedClip?.id) { mutableStateOf(selectedClip?.textProperties?.content ?: "Make it pop") }
  var size by remember(selectedClip?.id) { mutableStateOf(selectedClip?.textProperties?.fontSizeSp ?: 28f) }
  var color by remember(selectedClip?.id) { mutableStateOf(selectedClip?.textProperties?.color ?: "#F4F6FF") }
  var background by remember(selectedClip?.id) { mutableStateOf(selectedClip?.textProperties?.backgroundColor != null) }
  var stroke by remember(selectedClip?.id) { mutableStateOf(selectedClip?.textProperties?.strokeEnabled ?: false) }
  var shadow by remember(selectedClip?.id) { mutableStateOf(selectedClip?.textProperties?.shadowEnabled ?: true) }
  var alignment by remember(selectedClip?.id) { mutableStateOf(selectedClip?.textProperties?.alignment ?: "Center") }
  var animation by remember(selectedClip?.id) { mutableStateOf(selectedClip?.textProperties?.animation ?: "Fade") }
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Text") }, singleLine = true, modifier = Modifier.fillMaxWidth())
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(22f, 28f, 34f, 42f).forEach { value -> FilterChip(selected = size == value, onClick = { size = value }, label = { Text("${value.toInt()}sp") }) }
        listOf("#F4F6FF", "#FACC15", "#22D3EE", "#FF65B3").forEach { value -> FilterChip(selected = color == value, onClick = { color = value }, label = { Text(value) }) }
      }
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = background, onClick = { background = !background }, label = { Text("Background") })
        FilterChip(selected = stroke, onClick = { stroke = !stroke }, label = { Text("Stroke") })
        FilterChip(selected = shadow, onClick = { shadow = !shadow }, label = { Text("Shadow") })
        listOf("Left", "Center", "Right").forEach { value -> FilterChip(selected = alignment == value, onClick = { alignment = value }, label = { Text(value) }) }
        listOf("Fade", "Slide", "Pop", "Typewriter").forEach { value -> FilterChip(selected = animation == value, onClick = { animation = value }, label = { Text(value) }) }
      }
      Button(onClick = { if (selectedClip?.clipType == ClipType.Text) viewModel.updateSelectedText(text, size, color, if (background) "#7C5CFF" else null, stroke, shadow, alignment, animation) else viewModel.addTextClipAtPlayhead(text, size, color, if (background) "#7C5CFF" else null, stroke, shadow, alignment, animation) }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp), shape = RoundedCornerShape(999.dp)) { Text(if (selectedClip?.clipType == ClipType.Text) "Update Text" else "Add Text") }
      LayerActions(viewModel)
    }
  }
}

@Composable
private fun CanvasToolPanel(selected: CanvasRatio, background: CanvasBackground, viewModel: MainScreenViewModel) {
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { CanvasRatio.entries.forEach { ratio -> FilterChip(selected = selected == ratio, onClick = { viewModel.updateCanvasRatio(ratio) }, label = { Text(ratio.label) }) } }
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("#09090B", "#18181B", "#7C3AED", "#22D3EE", "#F97316").forEach { color -> FilterChip(selected = background.color == color, onClick = { viewModel.updateCanvasBackground(background.copy(color = color)) }, label = { Text(color) }) } }
      FilterChip(selected = background.blurEnabled, onClick = { viewModel.updateCanvasBackground(background.copy(blurEnabled = !background.blurEnabled)) }, label = { Text("Background blur") }, modifier = Modifier.padding(top = 8.dp))
      AdjustmentControl("Blur strength", background.blurStrength, 0f, 1f) { viewModel.updateCanvasBackground(background.copy(blurStrength = it)) }
    }
  }
}

@Composable
private fun AdjustmentControl(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
  Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(label, modifier = Modifier.width(112.dp), fontSize = 12.sp, color = StudioTextMuted)
    TextButton(onClick = { onChange((value - ((max - min) / 12f)).coerceIn(min, max)) }, modifier = Modifier.size(48.dp)) { Text("-") }
    LinearProgressIndicator(progress = { ((value - min) / (max - min)).coerceIn(0f, 1f) }, modifier = Modifier.weight(1f).height(8.dp), color = StudioSecondary)
    TextButton(onClick = { onChange((value + ((max - min) / 12f)).coerceIn(min, max)) }, modifier = Modifier.size(48.dp)) { Text("+") }
    Text("%.2f".format(value), modifier = Modifier.width(46.dp), fontSize = 11.sp, textAlign = TextAlign.End)
  }
}

@Composable
private fun LayerActions(viewModel: MainScreenViewModel) {
  Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick = viewModel::duplicateSelectedClip) { Text("Duplicate") }
    OutlinedButton(onClick = viewModel::deleteSelectedClip) { Text("Delete") }
    OutlinedButton(onClick = { viewModel.trimSelectedClip(-500) }) { Text("Trim -") }
    OutlinedButton(onClick = { viewModel.trimSelectedClip(500) }) { Text("Trim +") }
    OutlinedButton(onClick = viewModel::toggleKeyframeAtPlayhead, modifier = Modifier.semantics { contentDescription = "Toggle keyframe at playhead" }) { Text("Keyframe") }
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
          EditorTool.Speed -> listOf(ClipAction.SpeedDown, ClipAction.SpeedUp)
          else -> listOf(ClipAction.OpacityDown, ClipAction.OpacityUp, ClipAction.Keyframe)
        }.forEach { action -> OutlinedButton(onClick = { viewModel.adjustSelectedClip(action) }) { Text(action.label) } }
      }
      Spacer(Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::splitSelectedClip) { Text("Split") }
        OutlinedButton(onClick = viewModel::deleteSelectedClip) { Text("Delete") }
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
