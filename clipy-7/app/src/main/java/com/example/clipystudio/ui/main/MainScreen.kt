package com.example.clipystudio.ui.main

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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.foundation.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
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
import com.example.clipystudio.data.GestureOwner
import com.example.clipystudio.data.HapticEvent
import com.example.clipystudio.data.ExportResolution
import com.example.clipystudio.data.FilterAdjustmentSet
import com.example.clipystudio.data.LanguageCode
import com.example.clipystudio.data.MediaAsset
import com.example.clipystudio.data.MediaType
import com.example.clipystudio.data.Project
import com.example.clipystudio.data.DefaultTempFileManager
import com.example.clipystudio.data.RenderExportState
import com.example.clipystudio.data.RenderExportStatus
import com.example.clipystudio.data.RenderPipelineState
import com.example.clipystudio.data.RenderPipelineStatus
import com.example.clipystudio.data.RenderStageStatus
import com.example.clipystudio.data.StageState
import com.example.clipystudio.data.QualityPreset
import com.example.clipystudio.data.StickerAsset
import com.example.clipystudio.data.StickerCategory
import com.example.clipystudio.data.StickerLibrary
import com.example.clipystudio.data.Timeline
import com.example.clipystudio.data.TimelineClip
import com.example.clipystudio.data.TimelineEngine
import com.example.clipystudio.data.TimelineGestureMode
import com.example.clipystudio.data.TimelineTrack
import com.example.clipystudio.data.TrackType
import com.example.clipystudio.data.TransitionType
import com.example.clipystudio.data.TrimHandle
import com.example.clipystudio.data.PreviewSeekSource
import com.example.clipystudio.data.asSizeLabel
import com.example.clipystudio.data.asTimecode
import com.example.clipystudio.filter.FilterLibrary
import com.example.clipystudio.filter.FilterPreset
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private enum class Screen { Splash, Intro, Language, Dashboard, Import, Editor, Export, Settings }

private data class TimelineClipPreviewState(
  val clipId: String,
  val startTimeMs: Long,
  val durationMs: Long,
  val snapLabel: String? = null,
  val isValid: Boolean = true,
  val trimHandle: TrimHandle? = null,
  val snapTimeMs: Long? = null,
)

private data class TimelineGestureOverlayState(
  val zoomLabel: String? = null,
  val snapLabel: String? = null,
  val snapTimeMs: Long? = null,
  val resistanceFraction: Float = 0f,
  val mode: TimelineGestureMode = TimelineGestureMode.IDLE,
  val autoScrollDirection: com.example.clipystudio.data.AutoScrollDirection = com.example.clipystudio.data.AutoScrollDirection.NONE,
  val invalidFeedback: Boolean = false,
)

private data class PreviewGestureFeedback(
  val owner: GestureOwner = GestureOwner.NONE,
  val showCenterXGuide: Boolean = false,
  val showCenterYGuide: Boolean = false,
  val showBoundaryGuide: Boolean = false,
  val angleLabel: String? = null,
  val chipLabel: String? = null,
  val pendingHaptic: HapticEvent? = null,
)

private enum class PreviewSurfaceState {
  NoMedia,
  Loading,
  ImageReady,
  VideoReady,
  InvalidUri,
  LoadFailed,
}

private enum class ClipVisualState {
  Selected,
  Active,
  Inactive,
  Invalid,
}

private enum class PreviewMediaLoadState {
  Idle,
  Failed,
}

private enum class VideoPreviewLoadState {
  Loading,
  Ready,
  Failed,
}

private val EditorChromeBackground = Color(0xFF0A0A0A)
private val EditorChromeSurface = Color(0xFF1A1A1A)
private val EditorChromeSurfaceAlt = Color(0xFF0F0F0F)
private val EditorChromeSurfaceLow = Color(0xFF141414)
private val EditorChromeBorder = Color(0xFF262626)
private val EditorChromePrimary = Color(0xFF0084FF)
private val EditorChromeAudio = Color(0xFF003919)
private val EditorChromeAudioAccent = Color(0xFF00A657)
private val EditorChromeMuted = Color(0xFFC6C6C7)
private val EditorTimelineGrid = Color(0xFF1A1A1A)
private val EditorChromeDanger = Color(0xFFFF6B6B)

private data class BottomNavItem(val tool: EditorTool, val label: String, val glyph: String)

private fun topBarChevronGlyph() = "‹"

private fun toolbarGlyph(action: String): String = when (action) {
  "undo" -> "↶"
  "redo" -> "↷"
  "split" -> "✂"
  "speed" -> "◌"
  "anim" -> "◇"
  "volume" -> "∿"
  "delete" -> "⌫"
  else -> "•"
}

private fun navGlyph(tool: EditorTool): String = when (tool) {
  EditorTool.Edit -> "▣"
  EditorTool.Audio -> "♪"
  EditorTool.Text -> "T"
  EditorTool.Effect -> "✦"
  EditorTool.Overlay -> "▤"
  EditorTool.Sticker -> "☺"
  EditorTool.Filter -> "◐"
  EditorTool.Transition -> "⇄"
  EditorTool.Canvas -> "□"
  EditorTool.Speed -> "⟲"
  EditorTool.Export -> "↑"
}

private fun clipTypeBadge(clipType: ClipType): String = when (clipType) {
  ClipType.Image -> "IMG"
  ClipType.Video -> "VID"
  ClipType.Audio -> "AUD"
  ClipType.Text -> "TXT"
  ClipType.Sticker -> "STK"
  ClipType.Effect -> "FX"
  ClipType.Overlay -> "OVR"
}

private fun Timeline.findClip(clipId: String?): TimelineClip? =
  clipId?.let { id -> tracks.flatMap { it.clips }.firstOrNull { it.id == id } }

private fun Timeline.activePreviewClip(): TimelineClip? {
  val allClips = tracks.flatMap { it.clips }
  val lastFrameTimeMs = playheadMs.takeIf { it < durationMs }
    ?: (durationMs - 1L).coerceAtLeast(0L)
  return findClip(selectedClipId)?.takeIf { it.clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Overlay) }
    ?: TimelineEngine.resolveActiveComposition(this).video?.clipId?.let { activeId -> allClips.firstOrNull { it.id == activeId } }
    ?: allClips.firstOrNull { it.clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Overlay) && lastFrameTimeMs in it.startMs until (it.startMs + it.durationMs).coerceAtLeast(it.startMs + 1L) }
}

private fun Timeline.selectedRealClip(): TimelineClip? =
  findClip(selectedClipId)?.takeIf { it.clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Audio, ClipType.Overlay, ClipType.Text, ClipType.Sticker, ClipType.Effect) }

private fun TimelineClip.isVisualMediaClip(): Boolean = clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Overlay)

private fun TimelineClip.hasUsableMediaUri(): Boolean {
  if (!isVisualMediaClip()) return false
  val uri = mediaUri?.trim().orEmpty()
  if (uri.isBlank()) return false
  val parsedUri = runCatching { Uri.parse(uri) }.getOrNull() ?: return false
  return !parsedUri.scheme.isNullOrBlank()
}

private suspend fun animateTimelineSettle(
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

private data class ImportPermissionNotice(
  val title: String,
  val body: String,
  val confirmLabel: String,
  val dismissLabel: String,
  val openSettings: Boolean = false,
)

private data class IntroPage(val title: String, val body: String, val color: Color)

private data class ThumbnailFrame(val clipId: String, val bitmap: Bitmap?)

private data class UriMetadata(val displayName: String?, val sizeBytes: Long?, val mimeType: String?, val durationMs: Long?)

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel? = null,
) {
  val context = LocalContext.current
  val resolvedViewModel = viewModel ?: viewModel {
    MainScreenViewModel(tempFileManager = DefaultTempFileManager(File(context.cacheDir, "exports")))
  }
  val uiState by resolvedViewModel.uiState.collectAsStateWithLifecycle()
  when (val state = uiState) {
    MainScreenUiState.Loading -> LoadingSurface(modifier)
    is MainScreenUiState.Error -> ErrorSurface(state.throwable.message.orEmpty(), modifier)
    is MainScreenUiState.Success -> ClipyStudioApp(state.appState, state.editorUiState, resolvedViewModel, modifier)
  }
}

@Composable
private fun ClipyStudioApp(appState: AppState, editorUiState: com.example.clipystudio.editor.model.EditorUiState, viewModel: MainScreenViewModel, modifier: Modifier = Modifier) {
  var screen by remember { mutableStateOf(Screen.Splash) }
  var languageFromSettings by remember { mutableStateOf(false) }
  var exitRequested by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val copy = copyFor(appState.languageCode)
  val shareEvent by viewModel.shareEvent.collectAsStateWithLifecycle()
  val isPlaybackLocked = editorUiState.panelState.isPlaybackLocked

  LaunchedEffect(Unit) {
    delay(550)
    screen = if (appState.hasCompletedIntro) Screen.Dashboard else Screen.Intro
  }
  LaunchedEffect(shareEvent) {
    val event = shareEvent ?: return@LaunchedEffect
    val shareUri = event.uri.toShareUri(context)
    if (shareUri.scheme != "content" || event.mimeType != "video/mp4") {
      snackbarHostState.showSnackbar(if (appState.languageCode == LanguageCode.Vi) "Video xuat chua san sang de chia se an toan." else "The exported video is not ready to share safely.")
      viewModel.consumeShareEvent()
      return@LaunchedEffect
    }
    runCatching {
      context.startActivity(
        Intent.createChooser(
          Intent(Intent.ACTION_SEND).apply {
            type = event.mimeType
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          },
          event.chooserTitle,
        ),
      )
    }.onFailure {
      snackbarHostState.showSnackbar(if (appState.languageCode == LanguageCode.Vi) "Khong the mo bang chia se luc nay." else "Unable to open the share sheet right now.")
    }
    viewModel.consumeShareEvent()
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
          snackbarHostState = snackbarHostState,
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
          isPlaybackLocked = isPlaybackLocked,
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
          onClearCache = {
            viewModel.clearCache()
            scope.launch { snackbarHostState.showSnackbar(if (appState.languageCode == LanguageCode.Vi) "Da xoa tep tam. Media goc va video xuat khong bi xoa." else "Temporary files cleared. Original media and exported videos were not deleted.") }
          },
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

private fun String.toShareUri(context: Context): Uri {
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
private fun ImportScreen(appState: AppState, copy: Copy, snackbarHostState: SnackbarHostState, onBack: () -> Unit, onAddAsset: (MediaType, String?, String?, Long?, Long?) -> Unit, onRemove: (String) -> Unit, onAddToProject: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var pendingPicker by remember { mutableStateOf<MediaType?>(null) }
  var permissionNotice by remember { mutableStateOf<ImportPermissionNotice?>(null) }
  val largeFileLimitBytes = 512L * 1024L * 1024L
  fun importMessage(en: String, vi: String) = if (appState.languageCode == LanguageCode.Vi) vi else en
  fun permissionNotice(titleEn: String, titleVi: String, bodyEn: String, bodyVi: String, openSettings: Boolean = false) =
    ImportPermissionNotice(
      title = importMessage(titleEn, titleVi),
      body = importMessage(bodyEn, bodyVi),
      confirmLabel = if (openSettings) importMessage("Open settings", "Mo cai dat") else importMessage("Choose media", "Chon media"),
      dismissLabel = importMessage("Cancel", "Huy"),
      openSettings = openSettings,
    )
  val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
    if (uris.isEmpty()) {
      scope.launch { snackbarHostState.showSnackbar(importMessage("Media selection cancelled. Your project was not changed.", "Da huy chon media. Du an khong thay doi.")) }
      return@rememberLauncherForActivityResult
    }
    uris.forEach { uri ->
      context.persistReadPermission(uri)
      val metadata = context.readUriMetadataSafely(uri)
      if (metadata == null) {
        scope.launch { snackbarHostState.showSnackbar(importMessage("This media item could not be read. Try another file.", "Khong the doc media nay. Hay thu tep khac.")) }
      } else if (metadata.mimeType?.startsWith("image") != true && metadata.mimeType?.startsWith("video") != true) {
        scope.launch { snackbarHostState.showSnackbar(importMessage("This file type is not supported for image/video import.", "Loai tep nay khong duoc ho tro cho anh/video.")) }
      } else if ((metadata.sizeBytes ?: 0L) > largeFileLimitBytes) {
        scope.launch { snackbarHostState.showSnackbar(importMessage("This file is too large to import safely in this MVP build.", "Tep qua lon de nhap an toan trong ban MVP.")) }
      } else {
        onAddAsset(if (metadata.mimeType?.startsWith("image") == true) MediaType.Image else MediaType.Video, uri.toString(), metadata.displayName, metadata.sizeBytes, metadata.durationMs)
      }
    }
  }
  val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
    if (uris.isEmpty()) {
      scope.launch { snackbarHostState.showSnackbar(importMessage("Audio selection cancelled. Your project was not changed.", "Da huy chon am thanh. Du an khong thay doi.")) }
      return@rememberLauncherForActivityResult
    }
    uris.forEach { uri ->
      if (!context.persistReadPermission(uri)) {
        permissionNotice = permissionNotice(
          titleEn = "Limited audio access",
          titleVi = "Quyen truy cap am thanh bi gioi han",
          bodyEn = "Clipy Studio can still use this audio in the current session, but Android did not grant long-term access. Re-pick it if it is missing later.",
          bodyVi = "Clipy Studio van co the dung tep am thanh nay trong phien hien tai, nhung Android khong cap quyen truy cap lau dai. Hay chon lai neu tep bi mat sau do.",
        )
      }
      val metadata = context.readUriMetadataSafely(uri)
      if (metadata == null || metadata.mimeType?.startsWith("audio") != true) {
        scope.launch { snackbarHostState.showSnackbar(importMessage("This audio file is not supported.", "Tep am thanh khong duoc ho tro.")) }
      } else if ((metadata.sizeBytes ?: 0L) > largeFileLimitBytes) {
        scope.launch { snackbarHostState.showSnackbar(importMessage("This audio file is too large to import safely.", "Tep am thanh qua lon de nhap an toan.")) }
      } else {
        onAddAsset(MediaType.Audio, uri.toString(), metadata.displayName, metadata.sizeBytes, metadata.durationMs)
      }
    }
  }
  StudioScreen {
    TopStrip(title = copy.import, onBack = onBack)
    Text(if (appState.languageCode == LanguageCode.Vi) "Dung bo chon he thong de them media. Android 13+ chi cap quyen cho tep ban chon; Clipy Studio khong yeu cau truy cap bo nho rong." else "Use Android system pickers. On Android 13+, Clipy Studio works with media you choose instead of requesting broad storage access.", color = StudioTextMuted)
    Spacer(Modifier.height(14.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Button(onClick = { pendingPicker = MediaType.Video }, shape = RoundedCornerShape(999.dp)) { Text(if (appState.languageCode == LanguageCode.Vi) "Them anh/video" else "Import Images/Videos") }
      Button(onClick = { pendingPicker = MediaType.Audio }, shape = RoundedCornerShape(999.dp)) { Text(if (appState.languageCode == LanguageCode.Vi) "Them am thanh" else "Import Audio") }
    }
    permissionNotice?.let { notice ->
      Spacer(Modifier.height(12.dp))
      OutlinedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(notice.title, color = StudioAccent, fontWeight = FontWeight.Bold)
          Text(notice.body, color = StudioTextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
          TextButton(onClick = { permissionNotice = null }) { Text(if (appState.languageCode == LanguageCode.Vi) "Dong" else "Dismiss") }
        }
      }
    }
    if (pendingPicker != null) {
      val isAudio = pendingPicker == MediaType.Audio
      AlertDialog(
        onDismissRequest = { pendingPicker = null },
        title = { Text(if (isAudio) importMessage("Choose audio safely", "Chon am thanh an toan") else importMessage("Choose media safely", "Chon media an toan")) },
        text = {
          Text(
            if (isAudio) {
              importMessage(
                "Clipy Studio opens the Android document picker for audio and only reads files you choose. It does not need broad Music and audio permission for this import path.",
                "Clipy Studio mo bo chon tai lieu Android cho am thanh va chi doc cac tep ban chon. Duong nhap nay khong can quyen Nhac va am thanh rong.",
              )
            } else {
              importMessage(
                "Clipy Studio only reads the files you choose for editing and export. Clearing temporary files will not delete original media.",
                "Clipy Studio chi doc nhung tep ban chon de tao va xuat video. Media goc khong bi xoa khi xoa cache.",
              )
            },
          )
        },
        confirmButton = {
          TextButton(onClick = {
            pendingPicker = null
            if (isAudio) {
              audioPicker.launch(arrayOf("audio/*"))
            } else {
              photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            }
          }) { Text(if (appState.languageCode == LanguageCode.Vi) "Chon media" else "Choose media") }
        },
        dismissButton = { TextButton(onClick = { pendingPicker = null }) { Text(if (appState.languageCode == LanguageCode.Vi) "Huy" else "Cancel") } },
      )
    }
    Spacer(Modifier.height(18.dp))
    Text("Selected (${appState.selectedImports.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    if (appState.selectedImports.isEmpty()) {
      Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(if (appState.languageCode == LanguageCode.Vi) "Chua co media nao" else "No media selected", fontWeight = FontWeight.Bold)
          Text(if (appState.languageCode == LanguageCode.Vi) "Chon tep anh, video hoac am thanh tu bo chon he thong de them vao timeline. Ung dung khong them media mau gia." else "Pick images, videos, or audio from Android system pickers to add real local media to the timeline. Sample media shortcuts have been removed.", color = StudioTextMuted, fontSize = 13.sp)
        }
      }
    }
    Spacer(Modifier.height(8.dp))
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      items(appState.selectedImports, key = { it.id }) { asset -> MediaAssetCard(asset, onRemove) }
    }
    Button(onClick = onAddToProject, enabled = appState.selectedImports.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(999.dp)) { Text(if (appState.languageCode == LanguageCode.Vi) "Them vao du an" else "Add to Project") }
  }
}

@Composable
private fun EditorScreen(appState: AppState, copy: Copy, onBack: () -> Unit, onImport: () -> Unit, onExport: () -> Unit, viewModel: MainScreenViewModel, isPlaybackLocked: Boolean) {
  val project = appState.activeProject
  if (project == null) {
    StudioScreen { EmptyState(onCreate = onImport) }
    return
  }
  val timeline = project.timeline
  val selectedClip = timeline.selectedRealClip()
  var showExitDialog by remember { mutableStateOf(false) }
  BackHandler { showExitDialog = true }
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(EditorChromeBackground),
  ) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
      val bottomBarHeight = 72.dp
      val previewHeight = (maxHeight * 0.40f).coerceIn(248.dp, 360.dp)
      val panelHeight = if (maxHeight < 780.dp) 132.dp else 160.dp
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(EditorChromeBackground),
      ) {
        EditorTopBar(
          resolution = appState.defaultExportSettings.resolution.label.uppercase(),
          onBack = { showExitDialog = true },
          onExport = onExport,
        )
        EditorPreviewSection(
          modifier = Modifier
            .fillMaxWidth()
            .height(previewHeight),
          ratio = project.canvasRatio,
          timeline = timeline,
          onSelect = viewModel::selectClip,
          onClearSelection = viewModel::clearSelection,
          onDelete = viewModel::deleteSelectedClip,
          onTransform = viewModel::transformSelectedClipAbsolute,
          onEditText = viewModel::updateSelectedTool,
          onRatio = viewModel::updateCanvasRatio,
          onSeek = viewModel::seekTo,
          onPlay = viewModel::togglePlayback,
          onSeekBy = viewModel::seekBy,
        )
        if (isPlaybackLocked) {
          Text(
            if (appState.languageCode == LanguageCode.Vi) "Dang phat: cac thay doi truc tiep duoc khoa de giu dong bo preview." else "Playback lock: direct edits are held to keep preview and timeline synced.",
            color = EditorChromeMuted.copy(alpha = 0.78f),
            fontSize = 11.sp,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 4.dp)
              .semantics { contentDescription = "Playback editing lock active" },
          )
        }
        LaunchedEffect(timeline.isPlaying) {
          while (timeline.isPlaying) {
            delay(50)
            viewModel.tickPlayback(50)
          }
        }
        EditorTimelineSection(
          modifier = Modifier.weight(1f),
          timeline = timeline,
          canUndo = appState.undoStack.isNotEmpty(),
          canRedo = appState.redoStack.isNotEmpty(),
          hasSelection = selectedClip != null,
          onUndo = viewModel::undo,
          onRedo = viewModel::redo,
          onSplit = viewModel::splitSelectedClip,
          onSpeed = { viewModel.updateSelectedTool(EditorTool.Speed) },
          onAnimation = { viewModel.updateSelectedTool(EditorTool.Effect) },
          onVolume = { viewModel.updateSelectedTool(EditorTool.Audio) },
          onDelete = viewModel::deleteSelectedClip,
          timelineContent = {
            TimelineView(
              timeline = timeline,
              onSelect = viewModel::selectClip,
              onSeek = viewModel::seekTo,
              onScroll = viewModel::scrollTimelineTo,
              onZoom = viewModel::updateTimelineZoom,
              onTrim = viewModel::trimSelectedClipEdge,
              onMove = viewModel::dragSelectedClip,
              onSplit = viewModel::splitSelectedClip,
              onReorder = viewModel::reorderSelectedVideoClip,
            )
          },
        )
        EditorPanelHost(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .height(panelHeight),
          timeline = timeline,
          project = project,
          selectedClip = selectedClip,
          viewModel = viewModel,
        )
        Spacer(Modifier.height(bottomBarHeight + 16.dp))
      }
      EditorBottomBar(
        modifier = Modifier.align(Alignment.BottomCenter),
        selected = timeline.selectedTool,
        onSelect = viewModel::updateSelectedTool,
      )
      AddMediaFab(
        modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-24).dp, y = (-92).dp),
        onClick = onImport,
      )
    }
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
private fun EditorTopBar(resolution: String, onBack: () -> Unit, onExport: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(56.dp)
      .background(EditorChromeBackground)
      .border(1.dp, EditorChromeBorder)
      .padding(horizontal = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(Color.Transparent)
          .semantics { contentDescription = "Back from editor" },
      ) {
        Text(topBarChevronGlyph(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
      }
      Surface(
        color = EditorChromeSurface,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EditorChromeBorder),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(resolution, color = Color(0xFF9CA3AF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          Text("▾", color = Color(0xFF9CA3AF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
    Button(
      onClick = onExport,
      shape = RoundedCornerShape(8.dp),
      colors = ButtonDefaults.buttonColors(containerColor = EditorChromePrimary, contentColor = Color.White),
      modifier = Modifier.height(36.dp).semantics { contentDescription = "Export project" },
      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
    ) {
      Text("↑", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      Spacer(Modifier.width(6.dp))
      Text("Export", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
  }
}

@Composable
private fun EditorPreviewSection(
  modifier: Modifier,
  ratio: CanvasRatio,
  timeline: Timeline,
  onSelect: (String) -> Unit,
  onClearSelection: () -> Unit,
  onDelete: () -> Unit,
  onTransform: (Float, Float, Float, Float) -> Unit,
  onEditText: (EditorTool) -> Unit,
  onRatio: (CanvasRatio) -> Unit,
  onSeek: (Long) -> Unit,
  onPlay: () -> Unit,
  onSeekBy: (Long) -> Unit,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(EditorChromeBackground)
      .padding(horizontal = 20.dp, vertical = 12.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      PreviewCanvas(ratio, timeline, onSelect, onClearSelection, onDelete, onTransform, onEditText, onRatio, onSeek)
      PlaybackControls(timeline, onPlay, onSeekBy)
    }
  }
}

@Composable
private fun EditorTimelineSection(
  modifier: Modifier,
  timeline: Timeline,
  canUndo: Boolean,
  canRedo: Boolean,
  hasSelection: Boolean,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onSplit: () -> Unit,
  onSpeed: () -> Unit,
  onAnimation: () -> Unit,
  onVolume: () -> Unit,
  onDelete: () -> Unit,
  timelineContent: @Composable () -> Unit,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(EditorChromeSurfaceAlt)
      .border(1.dp, EditorChromeBorder),
  ) {
    EditorTimelineToolbar(
      timeline = timeline,
      canUndo = canUndo,
      canRedo = canRedo,
      hasSelection = hasSelection,
      onUndo = onUndo,
      onRedo = onRedo,
      onSplit = onSplit,
      onSpeed = onSpeed,
      onAnimation = onAnimation,
      onVolume = onVolume,
      onDelete = onDelete,
    )
    Box(Modifier.weight(1f).fillMaxWidth()) {
      timelineContent()
    }
  }
}

@Composable
private fun EditorTimelineToolbar(
  timeline: Timeline,
  canUndo: Boolean,
  canRedo: Boolean,
  hasSelection: Boolean,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onSplit: () -> Unit,
  onSpeed: () -> Unit,
  onAnimation: () -> Unit,
  onVolume: () -> Unit,
  onDelete: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(44.dp)
      .background(EditorChromeSurface)
      .border(1.dp, EditorChromeBorder)
      .padding(horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
      CompactToolbarIconButton(toolbarGlyph("undo"), "Undo edit", canUndo, onUndo)
      CompactToolbarIconButton(toolbarGlyph("redo"), "Redo edit", canRedo, onRedo)
      Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(16.dp).background(EditorChromeBorder))
      CompactToolbarAction(toolbarGlyph("split"), "Split", hasSelection, onSplit)
      CompactToolbarAction(toolbarGlyph("speed"), "Speed", hasSelection, onSpeed)
    }
    Surface(
      color = EditorChromePrimary.copy(alpha = 0.10f),
      shape = RoundedCornerShape(6.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, EditorChromePrimary.copy(alpha = 0.20f)),
    ) {
      Text(
        timeline.playheadMs.asTimecode(),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        color = EditorChromePrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
      CompactToolbarAction(toolbarGlyph("anim"), "Anim", true, onAnimation)
      CompactToolbarAction(toolbarGlyph("volume"), "Vol", hasSelection, onVolume)
      CompactToolbarIconButton(toolbarGlyph("delete"), "Delete selected clip", hasSelection, onDelete, tint = EditorChromeDanger)
    }
  }
}

@Composable
private fun CompactToolbarIconButton(
  glyph: String,
  description: String,
  enabled: Boolean,
  onClick: () -> Unit,
  tint: Color = EditorChromeMuted,
) {
  IconButton(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.size(36.dp).semantics { contentDescription = description },
  ) {
    Text(glyph, color = if (enabled) tint else tint.copy(alpha = 0.35f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun CompactToolbarAction(glyph: String, label: String, enabled: Boolean, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(Color.Transparent)
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 8.dp)
      .semantics { contentDescription = label },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(glyph, color = if (enabled) EditorChromeMuted else EditorChromeMuted.copy(alpha = 0.35f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    Text(label, color = if (enabled) EditorChromeMuted else EditorChromeMuted.copy(alpha = 0.35f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun EditorPanelHost(modifier: Modifier, timeline: Timeline, project: Project, selectedClip: TimelineClip?, viewModel: MainScreenViewModel) {
  Surface(
    modifier = modifier,
    color = EditorChromeSurface,
    shape = RectangleShape,
    border = androidx.compose.foundation.BorderStroke(1.dp, EditorChromeBorder),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
      when {
        selectedClip != null && timeline.selectedTool == EditorTool.Edit -> ClipEditPanel(selectedClip, viewModel)
        timeline.selectedTool == EditorTool.Audio -> AudioToolPanel(selectedClip, viewModel)
        timeline.selectedTool == EditorTool.Text -> TextToolPanel(selectedClip, viewModel)
        timeline.selectedTool == EditorTool.Sticker -> StickerToolPanel(timeline, viewModel)
        timeline.selectedTool == EditorTool.Filter -> FilterAdjustPanel(selectedClip, viewModel)
        timeline.selectedTool == EditorTool.Effect -> EffectToolPanel(selectedClip, viewModel)
        timeline.selectedTool == EditorTool.Transition -> TransitionToolPanel(timeline, viewModel)
        timeline.selectedTool == EditorTool.Canvas -> CanvasToolPanel(project.canvasRatio, timeline.canvasBackground, viewModel)
        timeline.selectedTool == EditorTool.Speed -> SpeedToolPanel(selectedClip, viewModel)
        timeline.selectedTool == EditorTool.Overlay -> OverlayToolPanel(project.importedAssets, selectedClip, viewModel)
        else -> ToolPanel(timeline, viewModel)
      }
    }
  }
}

@Composable
private fun EditorBottomBar(modifier: Modifier = Modifier, selected: EditorTool, onSelect: (EditorTool) -> Unit) {
  val items = listOf(
    BottomNavItem(EditorTool.Edit, "Edit", navGlyph(EditorTool.Edit)),
    BottomNavItem(EditorTool.Audio, "Audio", navGlyph(EditorTool.Audio)),
    BottomNavItem(EditorTool.Text, "Text", navGlyph(EditorTool.Text)),
    BottomNavItem(EditorTool.Effect, "Effects", navGlyph(EditorTool.Effect)),
    BottomNavItem(EditorTool.Overlay, "Overlay", navGlyph(EditorTool.Overlay)),
  )
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(72.dp)
      .navigationBarsPadding()
      .background(EditorChromeSurface)
      .border(1.dp, Color(0xFF333333))
      .padding(horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceEvenly,
  ) {
    items.forEach { item ->
      val active = selected == item.tool
      val tint = if (active) EditorChromePrimary else Color(0xFF737373)
      Column(
        modifier = Modifier
          .weight(1f)
          .clickable(onClick = { onSelect(item.tool) })
          .padding(vertical = 6.dp)
          .semantics { contentDescription = "${item.label} tool" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) EditorChromePrimary.copy(alpha = 0.10f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        ) {
          Text(item.glyph, color = tint, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text(item.label.uppercase(), color = tint, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
      }
    }
  }
}

@Composable
private fun AddMediaFab(modifier: Modifier = Modifier, onClick: () -> Unit) {
  Surface(
    modifier = modifier.size(56.dp),
    shape = CircleShape,
    color = EditorChromePrimary,
    shadowElevation = 12.dp,
    border = androidx.compose.foundation.BorderStroke(4.dp, EditorChromeBackground),
    onClick = onClick,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun ExportScreen(appState: AppState, copy: Copy, onBack: () -> Unit, onDashboard: () -> Unit, viewModel: MainScreenViewModel) {
  val settings = appState.defaultExportSettings
  val renderState by viewModel.renderPipelineState.collectAsStateWithLifecycle()
  val exportState by viewModel.renderExportState.collectAsStateWithLifecycle()
  val hasExportableContent = appState.activeProject?.timeline?.durationMs?.let { it > 0L } == true
  LaunchedEffect(settings, appState.activeProject?.timeline?.version, appState.activeProjectId) {
    viewModel.prepareRenderPipeline(appState)
  }
  StudioScreen {
    TopStrip(title = if (appState.languageCode == LanguageCode.Vi) "Xuat video" else "Export Video", onBack = onBack)
    if (exportState.status == RenderExportStatus.COMPLETED) {
      ExportSuccessPanel(exportState, onBack, onDashboard, viewModel)
      return@StudioScreen
    }
    if (exportState.status in setOf(RenderExportStatus.PREPARING, RenderExportStatus.RUNNING, RenderExportStatus.CANCELLING, RenderExportStatus.CANCELLED, RenderExportStatus.FAILED)) {
      ExportProgressPanel(exportState, viewModel, onBack)
      return@StudioScreen
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
private fun RenderPipelineSummary(renderState: RenderPipelineState, exportState: RenderExportState, durationMs: Long) {
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
private fun StatusPill(label: String, color: Color) {
  Surface(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(999.dp), modifier = Modifier.height(32.dp)) {
    Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
      Text(label, color = if (color == StudioSurface) StudioTextMuted else color, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
  }
}

private fun stageColor(state: StageState): Color = when (state) {
  StageState.PENDING -> StudioSurface
  StageState.ACTIVE -> StudioSecondary
  StageState.COMPLETE -> StudioPrimary
  StageState.WARNING -> StudioAccent
  StageState.FAILED -> StudioDanger
  StageState.CANCELLED -> StudioTextMuted
}

@Composable
private fun ExportProgressPanel(exportState: RenderExportState, viewModel: MainScreenViewModel, onBack: () -> Unit) {
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
private fun ExportSuccessPanel(exportState: RenderExportState, onBack: () -> Unit, onDashboard: () -> Unit, viewModel: MainScreenViewModel) {
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
private fun SettingsScreen(appState: AppState, copy: Copy, onBack: () -> Unit, onLanguage: () -> Unit, onClearCache: () -> Unit, onExit: () -> Unit) {
  StudioScreen {
    TopStrip(title = copy.settings, onBack = onBack)
    SettingsRow(copy.language, if (appState.languageCode == LanguageCode.En) "English" else "Tieng Viet", onLanguage)
    SettingsRow(if (appState.languageCode == LanguageCode.Vi) "Mac dinh xuat" else "Export defaults", "${appState.defaultExportSettings.resolution.label}, ${appState.defaultExportSettings.fps} FPS, ${appState.defaultExportSettings.qualityPreset.label}", {})
    SettingsRow(if (appState.languageCode == LanguageCode.Vi) "Luu tru va cache" else "Storage & Cache", "${appState.cacheUsageMb} MB thumbnail/proxy cache", onClearCache, action = if (appState.languageCode == LanguageCode.Vi) "Xoa" else "Clear")
    Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (appState.languageCode == LanguageCode.Vi) "Bao mat quyen rieng tu" else "Privacy-safe storage", fontWeight = FontWeight.Bold)
        Text(
          if (appState.languageCode == LanguageCode.Vi) {
            "Media nhap vao van nam o vi tri ban chon. Xoa tep tam chi xoa cache cua ung dung va khong xoa video da xuat."
          } else {
            "Imported media stays in the location you selected. Clearing temporary files only removes app cache and does not delete exported videos."
          },
          color = StudioTextMuted,
          fontSize = 13.sp,
        )
      }
    }
    Spacer(Modifier.height(12.dp))
    SettingsRow(if (appState.languageCode == LanguageCode.Vi) "Thong tin ung dung" else "App Info", if (appState.languageCode == LanguageCode.Vi) "Bien tap cuc bo offline-friendly MVP - version 1.0" else "Offline-friendly local editing MVP - version 1.0", {})
    SettingsRow(copy.exit, "Close after autosave/export confirmation", onExit, danger = true)
  }
}

@Composable
private fun PreviewCanvas(ratio: CanvasRatio, timeline: Timeline, onSelect: (String) -> Unit, onClearSelection: () -> Unit, onDelete: () -> Unit, onTransform: (Float, Float, Float, Float) -> Unit, onEditText: (EditorTool) -> Unit, onRatio: (CanvasRatio) -> Unit, onSeek: (Long) -> Unit) {
  val ratioValue = when (ratio) { CanvasRatio.Portrait -> 9f / 16f; CanvasRatio.Square -> 1f; CanvasRatio.Landscape -> 16f / 9f; CanvasRatio.FourFive -> 4f / 5f; CanvasRatio.Original -> 3f / 4f }
  val glow by animateFloatAsState(if (timeline.isPlaying) 1f else 0.35f, label = "previewGlow")
  val composition = remember(timeline) { TimelineEngine.resolveActiveComposition(timeline) }
  val activeIds = buildSet { composition.video?.let { add(it.clipId) }; addAll(composition.audio.map { it.clipId }); addAll(composition.text.map { it.clipId }); addAll(composition.stickers.map { it.clipId }); addAll(composition.overlays.map { it.clipId }); addAll(composition.effects.map { it.clipId }) }
  val allClips = timeline.tracks.flatMap { it.clips }
  val activeClips = allClips.filter { it.id in activeIds }.sortedBy { it.zIndex }
  val selectedClip = timeline.findClip(timeline.selectedClipId)
  val selectedVisualClip = selectedClip?.takeIf { it.clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Overlay) }
  val primaryVisualClip = selectedVisualClip ?: timeline.activePreviewClip()?.takeIf { it.clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Overlay) }
  val context = LocalContext.current
  val previewState = remember(primaryVisualClip?.id, primaryVisualClip?.mediaUri, primaryVisualClip?.clipType) { context.resolvePreviewSurfaceState(primaryVisualClip) }
  var feedback by remember { mutableStateOf(PreviewGestureFeedback()) }
  val haptic = LocalHapticFeedback.current
  val density = LocalDensity.current
  val touchSlopPx = with(density) { 8.dp.toPx() }
  LaunchedEffect(feedback.pendingHaptic) {
    val event = feedback.pendingHaptic ?: return@LaunchedEffect
    haptic.performHapticFeedback(if (event == HapticEvent.INVALID_ACTION) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
    feedback = feedback.copy(pendingHaptic = null)
  }
  BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    val previewHeight = maxHeight.coerceAtLeast(180.dp)
    Box(
      Modifier
        .fillMaxHeight(0.92f)
        .height(previewHeight)
        .aspectRatio(ratioValue)
        .clip(RoundedCornerShape(14.dp))
        .background(Color.Black)
        .border(1.dp, EditorChromeBorder, RoundedCornerShape(14.dp)),
      contentAlignment = Alignment.Center,
    ) {
      val backgroundColor = runCatching { Color(android.graphics.Color.parseColor(timeline.canvasBackground.color)) }.getOrDefault(StudioBackground)
      var previewWidthPx by remember { mutableStateOf(1f) }
      var previewHeightPx by remember { mutableStateOf(1f) }
      Box(Modifier.fillMaxSize().background(if (timeline.canvasBackground.blurEnabled) Brush.radialGradient(listOf(StudioPrimary.copy(alpha = 0.20f + timeline.canvasBackground.blurStrength * 0.20f), backgroundColor)) else Brush.radialGradient(listOf(StudioPrimary.copy(alpha = 0.16f * glow), backgroundColor))).onSizeChanged { previewWidthPx = it.width.toFloat().coerceAtLeast(1f); previewHeightPx = it.height.toFloat().coerceAtLeast(1f) }.pointerInput(activeClips.map { Triple(it.id, it.transform, it.zIndex) }, timeline.selectedClipId, previewWidthPx, previewHeightPx) {
        detectTapGestures(onTap = { tap ->
          val hit = TimelineEngine.overlayHitTest(TimelineEngine.overlayHitTargets(activeClips.filter { it.clipType == ClipType.Text || it.clipType == ClipType.Sticker || it.clipType == ClipType.Overlay }, tap.x, tap.y, previewWidthPx, previewHeightPx), tap.x, tap.y)
          when {
            hit.selectedOverlayId != null -> onSelect(hit.selectedOverlayId)
            else -> onClearSelection()
          }
          feedback = feedback.copy(owner = GestureOwner.PREVIEW_TAP)
        }, onDoubleTap = { tap ->
          val hitId = TimelineEngine.overlayHitTest(TimelineEngine.overlayHitTargets(activeClips.filter { it.clipType == ClipType.Text }, tap.x, tap.y, previewWidthPx, previewHeightPx, 144f, 88f), tap.x, tap.y).selectedOverlayId
          val hit = activeClips.firstOrNull { it.id == hitId }
          if (hit != null) {
            onSelect(hit.id)
            onEditText(EditorTool.Text)
            feedback = feedback.copy(owner = GestureOwner.TEXT_DOUBLE_TAP, pendingHaptic = HapticEvent.SNAP)
          }
        })
  }.pointerInput(selectedClip?.id, selectedClip?.transform, previewWidthPx, previewHeightPx) {
        val clip = selectedClip ?: return@pointerInput
        var gestureStarted = false
        var gesturePositionX = clip.transform.positionX
        var gesturePositionY = clip.transform.positionY
        var gestureScale = clip.transform.scale
        var gestureRotationDegrees = clip.transform.rotationDegrees
        var pointerStartX = 0f
        var pointerStartY = 0f
        detectTransformGestures { centroid, pan, zoom, rotation ->
          val owner = if (abs(zoom - 1f) > 0.01f || abs(rotation) > 0.25f) GestureOwner.OVERLAY_TRANSFORM else GestureOwner.OVERLAY_DRAG
          val slop = TimelineEngine.touchSlopGate(0f, 0f, pan.x, pan.y, touchSlopPx, if (owner == GestureOwner.OVERLAY_TRANSFORM) TimelineGestureMode.SCALING_OVERLAY else TimelineGestureMode.MOVING_OVERLAY)
          if (!gestureStarted && !slop.hasExceededTouchSlop && abs(zoom - 1f) <= 0.01f && abs(rotation) <= 0.25f) return@detectTransformGestures
          if (!gestureStarted) {
            gestureStarted = true
            pointerStartX = centroid.x
            pointerStartY = centroid.y
          }
          val lock = TimelineEngine.resolvePlaybackEditLock(timeline.isPlaying, slop.confirmedGestureMode.takeUnless { it == TimelineGestureMode.IDLE } ?: TimelineGestureMode.MOVING_OVERLAY)
          if (lock.shouldPauseBeforeEdit) {
            onSeek(timeline.playheadMs)
          } else if (lock.shouldBlockEditGesture) {
            feedback = feedback.copy(chipLabel = lock.lockReason, pendingHaptic = HapticEvent.INVALID_ACTION)
            return@detectTransformGestures
          }
          val startCenterX = gesturePositionX * previewWidthPx
          val startCenterY = gesturePositionY * previewHeightPx
          val drag = TimelineEngine.resolveOverlayDrag(clip.id, startCenterX, startCenterY, pointerStartX, pointerStartY, centroid.x, centroid.y, previewWidthPx, previewHeightPx)
          val transformed = TimelineEngine.resolveOverlayTransform(clip.id, drag.resolvedCenterX, drag.resolvedCenterY, 112f, 48f, centroid.x, centroid.y, gestureScale, zoom, gestureRotationDegrees, rotation)
          val boundary = TimelineEngine.resolveOverlayCanvasBoundary(clip.id, transformed.boundingBox.centerX, transformed.boundingBox.centerY, 112f, 48f, transformed.resolvedScale, transformed.resolvedRotationDegrees, previewWidthPx, previewHeightPx)
          gesturePositionX = boundary.resolvedCenterX / previewWidthPx
          gesturePositionY = boundary.resolvedCenterY / previewHeightPx
          gestureScale = transformed.resolvedScale
          gestureRotationDegrees = transformed.resolvedRotationDegrees
          onTransform(gesturePositionX, gesturePositionY, gestureScale, gestureRotationDegrees)
          val snap = transformed.snapResolution
          val guide = drag.snapResolution
          feedback = feedback.copy(
            owner = owner,
            showCenterXGuide = guide?.showVerticalCenterGuide == true,
            showCenterYGuide = guide?.showHorizontalCenterGuide == true,
            showBoundaryGuide = boundary.showBoundaryGuide,
            angleLabel = snap?.snappedRotationDegrees?.let { "${it.roundToInt()} deg" },
            chipLabel = if (boundary.showBoundaryGuide) "Edge limit" else null,
            pendingHaptic = if ((guide?.feedbackIntensity ?: 0f) > 0.85f || (snap?.feedbackIntensity ?: 0f) > 0.85f) HapticEvent.SNAP else feedback.pendingHaptic,
          )
        }
      }) {
        PreviewMediaSurface(primaryVisualClip, previewState, timeline.playheadMs, timeline.isPlaying, onSeek)
        Canvas(Modifier.fillMaxSize()) {
          drawRect(Color.White.copy(alpha = 0.05f), style = Stroke(width = 1.dp.toPx()))
          if (feedback.showCenterXGuide || selectedClip != null && feedback.owner == GestureOwner.OVERLAY_DRAG) {
            drawLine(EditorChromePrimary.copy(alpha = 0.35f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 1.dp.toPx())
          }
          if (feedback.showCenterYGuide || selectedClip != null && feedback.owner == GestureOwner.OVERLAY_DRAG) {
            drawLine(EditorChromePrimary.copy(alpha = 0.35f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1.dp.toPx())
          }
          if (feedback.showBoundaryGuide) {
            drawRect(EditorChromeAudioAccent.copy(alpha = 0.42f), style = Stroke(width = 2.dp.toPx()))
          }
        }
        activeClips.filter { it.clipType == ClipType.Text || it.clipType == ClipType.Sticker || it.clipType == ClipType.Overlay }.forEach { clip ->
          PreviewLayerChip(clip, selected = clip.id == timeline.selectedClipId, previewWidthPx = previewWidthPx, previewHeightPx = previewHeightPx, onSelect = { onSelect(clip.id) }, onDelete = onDelete)
        }
        feedback.angleLabel?.let { Text(it, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.42f)).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp), color = EditorChromeAudioAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        feedback.chipLabel?.let { Text(it, modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.42f)).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp), color = EditorChromeAudioAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        composition.transition?.let { transition ->
          Text("${transition.type.label} transition", modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(999.dp)).background(StudioBackground.copy(alpha = 0.72f)).padding(horizontal = 12.dp, vertical = 8.dp), color = StudioSecondary, fontWeight = FontWeight.Bold)
        }
        Text(
          timeline.playheadMs.asTimecode(),
          modifier = Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.40f)).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
        )
        Text(
          navGlyph(EditorTool.Canvas),
          modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.40f)).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
          color = Color.White,
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
        )
      }
    }
  }
}

@Composable
private fun PreviewLayerChip(clip: TimelineClip, selected: Boolean, previewWidthPx: Float, previewHeightPx: Float, onSelect: () -> Unit, onDelete: () -> Unit) {
  val x = (clip.transform.positionX * previewWidthPx).roundToInt()
  val y = (clip.transform.positionY * previewHeightPx).roundToInt()
  Box(Modifier.fillMaxSize()) {
    Box(Modifier.align(Alignment.TopStart).offset { IntOffset(x - 56, y - 24) }.graphicsLayer { scaleX = clip.transform.scale; scaleY = clip.transform.scale; rotationZ = clip.transform.rotationDegrees }.size(width = 112.dp, height = 48.dp).clip(RoundedCornerShape(14.dp)).background(StudioBackground.copy(alpha = 0.72f)).border(if (selected) 2.dp else 1.dp, if (selected) StudioPrimary else Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp)).clickable(onClick = onSelect).pointerInput(clip.id) { detectTapGestures(onDoubleTap = { onSelect() }, onTap = { onSelect() }) }.padding(horizontal = 12.dp, vertical = 8.dp).semantics { contentDescription = if (selected) "Selected overlay ${clip.title}" else "Overlay ${clip.title}" }, contentAlignment = Alignment.Center) {
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
private fun PreviewMediaSurface(clip: TimelineClip?, previewState: PreviewSurfaceState, playheadMs: Long, isPlaying: Boolean, onSeek: (Long) -> Unit) {
  when (previewState) {
    PreviewSurfaceState.NoMedia -> PreviewStatusCard("No media selected", "Import an image or video to start previewing and editing.", StudioTextMuted)
    PreviewSurfaceState.Loading -> PreviewStatusCard("Loading media", "Clipy Studio is preparing the selected preview.", StudioSecondary)
    PreviewSurfaceState.InvalidUri -> PreviewStatusCard("Invalid media", "This clip does not have a usable URI.", StudioDanger)
    PreviewSurfaceState.LoadFailed -> PreviewStatusCard("Media failed to load", "Clipy Studio could not open this file for preview.", StudioDanger)
    PreviewSurfaceState.ImageReady -> {
      val model = clip?.mediaUri ?: return PreviewStatusCard("Image unavailable", "The selected image is missing.", StudioDanger)
      var loadState by rememberSaveable(model) { mutableStateOf(PreviewMediaLoadState.Idle) }
      LaunchedEffect(model) { loadState = PreviewMediaLoadState.Idle }
      if (loadState == PreviewMediaLoadState.Failed) {
        PreviewStatusCard("Image failed to load", "Clipy Studio could not decode this image for preview.", StudioDanger)
        return
      }
      Box(Modifier.fillMaxSize()) {
        AsyncImage(
          model = Uri.parse(model),
          contentDescription = "Image preview for ${clip.title}",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Fit,
          onSuccess = { loadState = PreviewMediaLoadState.Idle },
          onError = { loadState = PreviewMediaLoadState.Failed },
        )
        Column(Modifier.align(Alignment.TopEnd).padding(10.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            "${(clip.durationMs / 1000f).let { "%.1fs".format(it) }} still",
            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(StudioBackground.copy(alpha = 0.8f)).padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
          )
          clip.mediaUri?.let {
            Text(
              clip.title,
              modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(StudioBackground.copy(alpha = 0.76f)).padding(horizontal = 8.dp, vertical = 4.dp),
              color = Color.White.copy(alpha = 0.92f),
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
            )
          }
        }
      }
    }
    PreviewSurfaceState.VideoReady -> clip?.let { VideoPreviewPlayer(clip = it, isPlaying = isPlaying, playheadMs = playheadMs, onSeek = onSeek) }
      ?: PreviewStatusCard("Video unavailable", "The selected video is missing.", StudioDanger)
  }
}

@Composable
private fun VideoPreviewPlayer(clip: TimelineClip, isPlaying: Boolean, playheadMs: Long, onSeek: (Long) -> Unit) {
  val context = LocalContext.current
  val mediaUri = clip.mediaUri
  if (mediaUri.isNullOrBlank()) {
    PreviewStatusCard("Video unavailable", "The selected video is missing.", StudioDanger)
    return
  }
  var loadState by remember(mediaUri) { mutableStateOf(VideoPreviewLoadState.Loading) }
  val player = remember(mediaUri) {
    ExoPlayer.Builder(context).build().apply {
      repeatMode = Player.REPEAT_MODE_OFF
      setMediaItem(MediaItem.fromUri(mediaUri))
      prepare()
    }
  }
  DisposableEffect(player) {
    onDispose { player.release() }
  }
  LaunchedEffect(isPlaying, mediaUri) {
    player.playWhenReady = isPlaying
    if (!isPlaying) player.pause()
  }
  LaunchedEffect(playheadMs, mediaUri, clip.id, clip.startMs, clip.durationMs, clip.sourceInMs, clip.sourceDurationMs, clip.videoProperties.speed, isPlaying) {
    val localPlayhead = (playheadMs - clip.startMs).coerceIn(0L, clip.durationMs)
    val maxSourcePosition = clip.sourceDurationMs?.coerceAtLeast(clip.sourceInMs + 1L)
    val unclampedTarget = (clip.sourceInMs + localPlayhead * clip.videoProperties.speed).toLong().coerceAtLeast(0L)
    val targetPosition = maxSourcePosition?.let { unclampedTarget.coerceAtMost(it - 1L) } ?: unclampedTarget
    if (!isPlaying || kotlin.math.abs(player.currentPosition - targetPosition) > 250L) {
      player.seekTo(targetPosition)
    }
  }
  DisposableEffect(player, onSeek) {
    val listener = object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        loadState = when (playbackState) {
          Player.STATE_READY -> VideoPreviewLoadState.Ready
          Player.STATE_IDLE -> VideoPreviewLoadState.Loading
          Player.STATE_BUFFERING -> if (loadState == VideoPreviewLoadState.Failed) VideoPreviewLoadState.Failed else VideoPreviewLoadState.Loading
          Player.STATE_ENDED -> VideoPreviewLoadState.Ready
          else -> loadState
        }
      }

      override fun onPlayerError(error: PlaybackException) {
        loadState = VideoPreviewLoadState.Failed
      }

      override fun onIsPlayingChanged(playing: Boolean) {
        if (!playing && loadState == VideoPreviewLoadState.Ready && player.playbackState == Player.STATE_ENDED) {
          onSeek((clip.startMs + clip.durationMs).coerceAtMost(playheadMs.coerceAtLeast(clip.startMs + clip.durationMs)))
        }
      }
    }
    player.addListener(listener)
    onDispose { player.removeListener(listener) }
  }
  Box(Modifier.fillMaxSize()) {
    AndroidView(
      factory = {
        PlayerView(it).apply {
          useController = false
          this.player = player
        }
      },
      modifier = Modifier.fillMaxSize(),
    )
    when (loadState) {
      VideoPreviewLoadState.Loading -> {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(color = StudioSecondary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            Text("Loading video preview", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
          }
        }
      }
      VideoPreviewLoadState.Failed -> PreviewStatusCard("Video failed to load", "Clipy Studio could not prepare this video for preview.", StudioDanger)
      VideoPreviewLoadState.Ready -> Unit
    }
  }
}

@Composable
private fun PreviewStatusCard(title: String, body: String, tint: Color) {
  Box(
    Modifier.fillMaxSize().padding(18.dp).clip(RoundedCornerShape(16.dp)).background(EditorChromeSurfaceLow.copy(alpha = 0.72f)).border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
      Text(title, color = tint, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
      Text(body, color = StudioTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
  }
}

@Composable
private fun PlaybackControls(timeline: Timeline, onPlay: () -> Unit, onSeek: (Long) -> Unit) {
  val hasContent = timeline.durationMs > 0L
  Row(
    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = { onSeek(-1_000) }, enabled = hasContent, modifier = Modifier.size(44.dp).semantics { contentDescription = "Seek backward" }) {
        Text("⏮", color = EditorChromeMuted, fontSize = 20.sp, fontWeight = FontWeight.Medium)
      }
      Surface(
        onClick = onPlay,
        enabled = hasContent,
        shape = CircleShape,
        color = Color.White,
        modifier = Modifier.size(40.dp).semantics { contentDescription = if (timeline.isPlaying) "Pause playback" else "Play playback" },
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(if (timeline.isPlaying) "❚❚" else "▶", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
      }
      IconButton(onClick = { onSeek(1_000) }, enabled = hasContent, modifier = Modifier.size(44.dp).semantics { contentDescription = "Seek forward" }) {
        Text("⏭", color = EditorChromeMuted, fontSize = 20.sp, fontWeight = FontWeight.Medium)
      }
    }
  }
}

@Composable
private fun TimelineView(timeline: Timeline, onSelect: (String) -> Unit, onSeek: (Long) -> Unit, onScroll: (Float, Float) -> Unit, onZoom: (Float, Float, Float) -> Unit, onTrim: (TrimHandle, Long) -> Unit, onMove: (Long) -> Unit, onSplit: () -> Unit, onReorder: (Int) -> Unit) {
  val projectTimeline = remember(timeline) { TimelineEngine.toProjectTimeline(timeline) }
  val pxPerSecond = timeline.pixelsPerSecond * timeline.zoomLevel
  val contentWidth = ((timeline.durationMs / 1_000f) * pxPerSecond).roundToInt().coerceAtLeast(640)
  val activeComposition = remember(timeline) { TimelineEngine.resolveActiveComposition(timeline) }
  val activeIds = remember(activeComposition) { buildSet { activeComposition.video?.let { add(it.clipId) }; addAll(activeComposition.audio.map { it.clipId }); addAll(activeComposition.text.map { it.clipId }); addAll(activeComposition.stickers.map { it.clipId }); addAll(activeComposition.overlays.map { it.clipId }); addAll(activeComposition.effects.map { it.clipId }) } }
  var viewportWidthPx by remember { mutableStateOf(TimelineEngine.DefaultViewportWidthPx) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val visibleRange = remember(timeline.scrollOffsetPx, timeline.zoomLevel, timeline.version) { TimelineEngine.visibleRange(timeline, viewportWidthPx) }
  val thumbnailRequests = remember(visibleRange, timeline.version) { TimelineEngine.planThumbnailRequests(timeline, visibleRange) }
  val thumbnailFrames by produceState(initialValue = emptyMap<String, Bitmap?>(), thumbnailRequests, context) {
    value = thumbnailRequests.associate { request -> request.clipId to context.loadThumbnailBitmap(request.mediaUri, request.thumbnailTimeMs, request.widthPx, request.heightPx) }
  }
  var gestureOverlay by remember { mutableStateOf(TimelineGestureOverlayState()) }
  var activePreview by remember { mutableStateOf<TimelineClipPreviewState?>(null) }
  var isEditGestureActive by remember { mutableStateOf(false) }
  var gestureTimecode by remember { mutableStateOf<String?>(null) }
  var flingNonce by remember { mutableLongStateOf(0L) }
  var flingJob by remember { mutableStateOf<Job?>(null) }
  var seekThrottle by remember { mutableStateOf(com.example.clipystudio.data.PreviewSeekThrottleState(minIntervalMs = 48L)) }
  val density = LocalDensity.current
  val touchSlopPx = with(density) { 8.dp.toPx() }
  val previewSeek: (Long, PreviewSeekSource, Boolean) -> Unit = { timeMs, source, forceFinal ->
    val decision = TimelineEngine.previewSeekDecision(seekThrottle, timeMs.coerceIn(0L, timeline.durationMs), System.currentTimeMillis(), source, forceFinal)
    seekThrottle = decision.state
    decision.seekTimeMs?.let(onSeek)
  }
  val cancelFling = {
    flingNonce += 1L
    flingJob?.cancel()
    flingJob = null
  }
  Box(
    Modifier
      .fillMaxWidth()
      .fillMaxHeight()
      .onSizeChanged { viewportWidthPx = (it.width.toFloat() - 74f).coerceAtLeast(180f) }
      .background(EditorChromeSurfaceAlt)
      .then(
        if (isEditGestureActive) {
          Modifier
        } else {
          Modifier.pointerInput(timeline.id, timeline.version, viewportWidthPx, timeline.scrollOffsetPx, timeline.zoomLevel, flingNonce) {
            var offset = timeline.scrollOffsetPx
            var velocityPxPerSec = 0f
            var lastDragAtMs = 0L
            var acceptedDrag = false
            detectHorizontalDragGestures(
              onDragStart = { start ->
                val bounds = TimelineEngine.timelinePointerBounds(66f, 0f, viewportWidthPx + 66f, size.height.toFloat(), start.x, start.y)
                acceptedDrag = bounds.shouldAcceptTimelineGesture
                if (!acceptedDrag) return@detectHorizontalDragGestures
                val interruption = TimelineEngine.interruptTimelineGesture(gestureOverlay.mode, start.x, timeline.scrollOffsetPx, timeline.playheadMs)
                cancelFling()
                offset = interruption.scrollOffsetAtTouchDownPx
                velocityPxPerSec = 0f
                lastDragAtMs = System.currentTimeMillis()
                gestureTimecode = timeline.playheadMs.asTimecode()
                if (timeline.isPlaying) onSeek(timeline.playheadMs)
                gestureOverlay = gestureOverlay.copy(mode = TimelineGestureMode.SCROLLING, snapLabel = null, snapTimeMs = null, zoomLabel = null)
              },
              onHorizontalDrag = { change, dragAmount ->
                if (!acceptedDrag) return@detectHorizontalDragGestures
                val update = TimelineEngine.dragTimeline(offset, dragAmount, timeline.durationMs, timeline.zoomLevel, timeline.pixelsPerSecond, viewportWidthPx)
                offset = update.nextOffsetPx
                gestureTimecode = update.currentTimeMs.asTimecode()
                gestureOverlay = gestureOverlay.copy(resistanceFraction = update.resistanceFraction, snapLabel = null, snapTimeMs = null)
                onScroll(offset, viewportWidthPx)
                previewSeek(update.currentTimeMs, PreviewSeekSource.TIMELINE_SCROLL, false)
                val now = System.currentTimeMillis()
                val elapsed = (now - lastDragAtMs).coerceAtLeast(1L)
                velocityPxPerSec = TimelineEngine.updateDragVelocity(velocityPxPerSec, dragAmount, elapsed)
                lastDragAtMs = now
                change.consumePositionChange()
              },
              onDragEnd = {
                if (!acceptedDrag) return@detectHorizontalDragGestures
                val finalSeek = TimelineEngine.exactFrameSeekFromScroll(offset, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs, viewportWidthPx, TimelineGestureMode.SCROLLING, finalFrame = true)
                previewSeek(finalSeek.currentTimeMs, PreviewSeekSource.TIMELINE_SCROLL, true)
                val token = flingNonce + 1L
                flingNonce = token
                if (abs(velocityPxPerSec) > TimelineEngine.DefaultPhysics.minFlingVelocityPxPerSec) {
                  flingJob = scope.launch {
                    gestureOverlay = gestureOverlay.copy(mode = TimelineGestureMode.FLINGING)
                    var velocity = velocityPxPerSec
                    var flingOffset = offset
                    while (abs(velocity) > TimelineEngine.DefaultPhysics.stopVelocityThresholdPxPerSec && flingNonce == token) {
                      val frame = TimelineEngine.advanceFling(flingOffset, velocity, 16, timeline.durationMs, timeline.zoomLevel, timeline.pixelsPerSecond, viewportWidthPx)
                      flingOffset = frame.nextOffsetPx
                      velocity = frame.nextVelocityPxPerSec
                      gestureTimecode = TimelineEngine.timeFromScroll(flingOffset, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs, viewportWidthPx).asTimecode()
                      gestureOverlay = gestureOverlay.copy(resistanceFraction = frame.resistanceFraction)
                      onScroll(flingOffset, viewportWidthPx)
                      previewSeek(TimelineEngine.timeFromScroll(flingOffset, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs, viewportWidthPx), PreviewSeekSource.TIMELINE_FLING, false)
                      if (frame.isFinished) break
                      delay(16)
                    }
                    if (flingNonce != token) return@launch
                    animateTimelineSettle(flingOffset, timeline, viewportWidthPx, onScroll, previewSeek) { resistance ->
                      gestureOverlay = gestureOverlay.copy(resistanceFraction = resistance)
                    }
                    gestureTimecode = null
                    gestureOverlay = gestureOverlay.copy(mode = TimelineGestureMode.IDLE, resistanceFraction = 0f)
                    flingJob = null
                  }
                } else {
                  flingJob = scope.launch {
                    gestureOverlay = gestureOverlay.copy(mode = TimelineGestureMode.IDLE)
                    animateTimelineSettle(offset, timeline, viewportWidthPx, onScroll, previewSeek) { resistance ->
                      gestureOverlay = gestureOverlay.copy(resistanceFraction = resistance)
                    }
                    gestureTimecode = null
                    flingJob = null
                  }
                }
              },
              onDragCancel = {
                if (!acceptedDrag) return@detectHorizontalDragGestures
                flingJob = scope.launch {
                  animateTimelineSettle(offset, timeline, viewportWidthPx, onScroll, previewSeek) { resistance ->
                    gestureOverlay = gestureOverlay.copy(resistanceFraction = resistance)
                  }
                  gestureTimecode = null
                  gestureOverlay = gestureOverlay.copy(mode = TimelineGestureMode.IDLE, resistanceFraction = 0f)
                  flingJob = null
                }
              },
            )
          }
        },
      ),
  ) {
    Canvas(Modifier.matchParentSize()) {
      val stepPx = 40.dp.toPx()
      var x = 66.dp.toPx() - (timeline.scrollOffsetPx % stepPx)
      while (x < size.width) {
        drawLine(EditorTimelineGrid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
        x += stepPx
      }
    }
    Column(Modifier.fillMaxSize().padding(top = 10.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      TimelineHeader(
        timeline = timeline,
        contentWidth = contentWidth,
        viewportWidthPx = viewportWidthPx,
        onSeek = onSeek,
        onScroll = onScroll,
        onZoom = onZoom,
        onGestureZoomLabel = { gestureOverlay = gestureOverlay.copy(zoomLabel = it) },
        onGestureTimecode = { gestureTimecode = it },
        onTransformStart = {
          cancelFling()
          isEditGestureActive = false
          gestureTimecode = timeline.playheadMs.asTimecode()
          gestureOverlay = gestureOverlay.copy(mode = TimelineGestureMode.SCROLLING, snapLabel = null, snapTimeMs = null)
        },
        onTransformFrame = { resistanceFraction ->
          gestureOverlay = gestureOverlay.copy(resistanceFraction = resistanceFraction)
        },
      )
      timeline.tracks.sortedBy { it.orderIndex }.forEach { track ->
        EngineTrackLane(projectTimeline, timeline, track, contentWidth, viewportWidthPx, activeIds, touchSlopPx, thumbnailFrames, onSelect, onTrim, { delta ->
          val lock = TimelineEngine.resolvePlaybackEditLock(timeline.isPlaying, TimelineGestureMode.DRAGGING_CLIP)
          if (lock.shouldPauseBeforeEdit) onSeek(timeline.playheadMs)
          if (!lock.shouldBlockEditGesture) onMove(delta)
          val selected = timeline.selectedClipId
          val target = selected?.let { track.clips.firstOrNull { clip -> clip.id == it } }?.let { TimelineEngine.resolveSnap(timeline, track.type, it.id, (it.startMs + delta).coerceAtLeast(0L)) }
          gestureOverlay = gestureOverlay.copy(
            snapLabel = target?.takeIf { it.isSnapped }?.targetType?.name?.lowercase()?.replaceFirstChar { char -> char.uppercase() },
            snapTimeMs = target?.takeIf { it.isSnapped }?.targetTimeMs,
          )
        }, onSplit, onReorder, activePreview, { preview ->
          cancelFling()
          activePreview = preview
          isEditGestureActive = preview != null
          gestureOverlay = gestureOverlay.copy(snapLabel = preview?.snapLabel, snapTimeMs = preview?.snapTimeMs, mode = if (preview?.trimHandle != null) TimelineGestureMode.TRIMMING_CLIP else if (preview != null) TimelineGestureMode.DRAGGING_CLIP else TimelineGestureMode.IDLE, invalidFeedback = preview?.isValid == false)
        }, { scrollDeltaPx, direction ->
          val update = TimelineEngine.advanceAutoScroll(timeline.scrollOffsetPx, TimelineEngine.resolveAutoScroll(if (direction == com.example.clipystudio.data.AutoScrollDirection.LEFT) 0f else viewportWidthPx, viewportWidthPx, timeline.selectedClipId, timeline.playheadMs), 16, timeline.durationMs, timeline.zoomLevel, timeline.pixelsPerSecond, viewportWidthPx)
          gestureOverlay = gestureOverlay.copy(autoScrollDirection = direction, mode = TimelineGestureMode.DRAGGING_CLIP)
          onScroll(update.nextOffsetPx + scrollDeltaPx, viewportWidthPx)
        }, { previewTimeMs ->
          previewSeek(previewTimeMs, PreviewSeekSource.CLIP_TRIM_LEFT, false)
        }, {
          activePreview?.trimHandle?.let { handle ->
            val seekTime = if (handle == TrimHandle.Left) activePreview?.startTimeMs else activePreview?.let { it.startTimeMs + it.durationMs }
            seekTime?.let { previewSeek(it, if (handle == TrimHandle.Left) PreviewSeekSource.CLIP_TRIM_LEFT else PreviewSeekSource.CLIP_TRIM_RIGHT, true) }
          }
          activePreview = null
          isEditGestureActive = false
          gestureTimecode = null
          gestureOverlay = gestureOverlay.copy(mode = TimelineGestureMode.IDLE, snapLabel = null, snapTimeMs = null, autoScrollDirection = com.example.clipystudio.data.AutoScrollDirection.NONE, invalidFeedback = false)
        })
      }
    }
    TimelineGuides(timeline, contentWidth, gestureOverlay.snapTimeMs)
    EdgeResistanceMask(gestureOverlay.resistanceFraction)
    AutoScrollEdgeMask(gestureOverlay.autoScrollDirection)
    gestureOverlay.snapLabel?.let { Text(it, modifier = Modifier.align(Alignment.TopCenter).padding(top = 30.dp).clip(RoundedCornerShape(999.dp)).background(EditorChromeSurface.copy(alpha = 0.92f)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(999.dp)).padding(horizontal = 9.dp, vertical = 3.dp), color = EditorChromeAudioAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    TimelineGestureReadout(gestureTimecode ?: timeline.playheadMs.asTimecode(), gestureOverlay.zoomLabel, gestureOverlay.snapLabel, gestureOverlay.resistanceFraction)
    Text("${(timeline.zoomLevel * 100).roundToInt()}% · ${thumbnailFrames.count { it.value != null }} thumbs · Saved v${timeline.version}", modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(999.dp)).background(EditorChromeSurface.copy(alpha = 0.88f)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 3.dp), color = EditorChromeMuted, fontSize = 10.sp)
    Box(Modifier.align(Alignment.TopCenter).width(2.dp).fillMaxHeight().background(EditorChromePrimary))
    Column(Modifier.align(Alignment.TopCenter).padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Box(Modifier.size(10.dp).graphicsLayer { rotationZ = 45f }.background(EditorChromePrimary))
      Text(timeline.playheadMs.asTimecode(), color = EditorChromePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(EditorChromeSurface.copy(alpha = 0.82f), RoundedCornerShape(8.dp)).padding(horizontal = 5.dp, vertical = 2.dp))
    }
  }
}

@Composable
private fun TimelineHeader(timeline: Timeline, contentWidth: Int, viewportWidthPx: Float, onSeek: (Long) -> Unit, onScroll: (Float, Float) -> Unit, onZoom: (Float, Float, Float) -> Unit, onGestureZoomLabel: (String?) -> Unit, onGestureTimecode: (String?) -> Unit, onTransformStart: () -> Unit, onTransformFrame: (Float) -> Unit) {
  Row(Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("SYNC", modifier = Modifier.width(58.dp), fontSize = 10.sp, color = EditorChromeMuted.copy(alpha = 0.74f), fontWeight = FontWeight.Bold)
    Box(
      Modifier
        .weight(1f)
        .height(28.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(EditorChromeSurface)
        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
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
              val update = TimelineEngine.dragTimeline(gestureScroll, pan.x, timeline.durationMs, gestureZoom, timeline.pixelsPerSecond, viewportWidthPx)
              gestureScroll = update.nextOffsetPx
              onTransformFrame(update.resistanceFraction)
              onGestureTimecode(update.currentTimeMs.asTimecode())
              onScroll(gestureScroll, viewportWidthPx)
            } else {
              onTransformFrame(0f)
            }
          }
        },
    ) {
      LaunchedEffect(timeline.zoomLevel) {
        onGestureZoomLabel(null)
        onGestureTimecode(null)
      }
      Canvas(Modifier.fillMaxWidth().fillMaxHeight().semantics { contentDescription = "Scrollable timeline ruler at ${timeline.playheadMs.asTimecode()}" }) {
        val pxPerMs = timeline.pixelsPerSecond * timeline.zoomLevel / 1_000f
        for (tick in 0..timeline.durationMs step 1_000L) {
          val x = tick * pxPerMs - timeline.scrollOffsetPx
          drawLine(if (kotlin.math.abs(tick - timeline.playheadMs) < 550) EditorChromePrimary else EditorChromeMuted.copy(alpha = 0.28f), Offset(x, 4f), Offset(x, size.height), strokeWidth = 1.5f)
        }
      }
      Box(Modifier.fillMaxSize()) {
        (0..timeline.durationMs step 2_000L).forEach { tick ->
          val x = (tick * (timeline.pixelsPerSecond * timeline.zoomLevel / 1_000f) - timeline.scrollOffsetPx).roundToInt()
          Text(tick.asTimecode(), color = EditorChromeMuted.copy(alpha = 0.85f), fontSize = 10.sp, modifier = Modifier.offset { IntOffset(x, 0) }.clickable { onSeek(tick) })
        }
      }
      timeline.markers.forEach { marker ->
        val left = ((marker.timeMs / 1_000f) * timeline.pixelsPerSecond * timeline.zoomLevel - timeline.scrollOffsetPx).roundToInt()
        Text(marker.label, modifier = Modifier.offset { IntOffset(left, 0) }.clip(RoundedCornerShape(999.dp)).background(EditorChromePrimary.copy(alpha = 0.18f)).padding(horizontal = 4.dp), color = EditorChromePrimary, fontSize = 9.sp)
      }
    }
    TextButton(onClick = { onZoom(-0.2f, viewportWidthPx / 2f, viewportWidthPx) }, modifier = Modifier.size(34.dp)) { Text("-", color = EditorChromeMuted, fontWeight = FontWeight.Bold) }
    TextButton(onClick = { onZoom(0.2f, viewportWidthPx / 2f, viewportWidthPx) }, modifier = Modifier.size(34.dp)) { Text("+", color = EditorChromeMuted, fontWeight = FontWeight.Bold) }
  }
}

@Composable
private fun BoxScope.TimelineGestureReadout(timecode: String, zoomLabel: String?, snapLabel: String?, resistanceFraction: Float) {
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

@Composable
private fun AutoScrollEdgeMask(direction: com.example.clipystudio.data.AutoScrollDirection) {
  if (direction == com.example.clipystudio.data.AutoScrollDirection.NONE) return
  Row(Modifier.fillMaxSize()) {
    Box(Modifier.width(56.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(StudioPrimary.copy(alpha = if (direction == com.example.clipystudio.data.AutoScrollDirection.LEFT) 0.24f else 0.05f), Color.Transparent))))
    Spacer(Modifier.weight(1f))
    Box(Modifier.width(56.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color.Transparent, StudioPrimary.copy(alpha = if (direction == com.example.clipystudio.data.AutoScrollDirection.RIGHT) 0.24f else 0.05f)))))
  }
}

@Composable
private fun EngineTrackLane(projectTimeline: com.example.clipystudio.data.ProjectTimeline, timeline: Timeline, track: TimelineTrack, contentWidth: Int, viewportWidthPx: Float, activeIds: Set<String>, touchSlopPx: Float, thumbnailFrames: Map<String, Bitmap?>, onSelect: (String) -> Unit, onTrim: (TrimHandle, Long) -> Unit, onMove: (Long) -> Unit, onSplit: () -> Unit, onReorder: (Int) -> Unit, activePreview: TimelineClipPreviewState?, onPreview: (TimelineClipPreviewState?) -> Unit, onAutoScroll: (Float, com.example.clipystudio.data.AutoScrollDirection) -> Unit, onPreviewSeek: (Long) -> Unit, onPreviewEnd: () -> Unit) {
  val laneHeight = when (track.type) {
    TrackType.Text, TrackType.Sticker, TrackType.Overlay, TrackType.Effect -> 28.dp
    TrackType.Video -> 80.dp
    TrackType.Audio -> 40.dp
  }
  Row(Modifier.fillMaxWidth().height(laneHeight).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(track.type.label.uppercase(), modifier = Modifier.width(58.dp), fontSize = 10.sp, color = EditorChromeMuted.copy(alpha = 0.74f), fontWeight = FontWeight.Bold)
    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(EditorChromeSurface.copy(alpha = 0.66f)).border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))) {
      Box(Modifier.fillMaxWidth().fillMaxHeight()) {
        track.clips.sortedBy { it.startMs }.forEachIndexed { index, clip ->
          EngineClipBlock(track.type, clip, index, selected = projectTimeline.selectedClipId == clip.id, active = clip.id in activeIds, zoom = projectTimeline.zoomScale, pixelsPerSecond = projectTimeline.pixelsPerSecond, scrollOffsetPx = timeline.scrollOffsetPx, transition = timeline.transitions.firstOrNull { it.fromClipId == clip.id || it.toClipId == clip.id }, timeline = timeline, viewportWidthPx = viewportWidthPx, touchSlopPx = touchSlopPx, thumbnailBitmap = thumbnailFrames[clip.id], preview = activePreview?.takeIf { it.clipId == clip.id }, onSelect = onSelect, onTrim = onTrim, onMove = onMove, onSplit = onSplit, onReorder = onReorder, onPreview = onPreview, onPreviewEnd = onPreviewEnd, onAutoScroll = onAutoScroll, onPreviewSeek = onPreviewSeek)
        }
      }
    }
  }
}

@Composable
private fun EngineClipBlock(trackType: TrackType, clip: TimelineClip, index: Int, selected: Boolean, active: Boolean, zoom: Float, pixelsPerSecond: Float, scrollOffsetPx: Float, transition: com.example.clipystudio.data.Transition?, timeline: Timeline, viewportWidthPx: Float, touchSlopPx: Float, thumbnailBitmap: Bitmap?, preview: TimelineClipPreviewState?, onSelect: (String) -> Unit, onTrim: (TrimHandle, Long) -> Unit, onMove: (Long) -> Unit, onSplit: () -> Unit, onReorder: (Int) -> Unit, onPreview: (TimelineClipPreviewState?) -> Unit, onPreviewEnd: () -> Unit, onAutoScroll: (Float, com.example.clipystudio.data.AutoScrollDirection) -> Unit, onPreviewSeek: (Long) -> Unit) {
  val color = when (trackType) { TrackType.Video -> EditorChromeSurface; TrackType.Audio -> EditorChromeAudio; TrackType.Text -> EditorChromePrimary.copy(alpha = 0.30f); TrackType.Sticker -> EditorChromeAudioAccent.copy(alpha = 0.24f); TrackType.Effect -> EditorChromePrimary.copy(alpha = 0.18f); TrackType.Overlay -> EditorChromePrimary.copy(alpha = 0.24f) }
  val selectedOutline by animateFloatAsState(if (selected) 1f else 0f, tween(140), label = "selectedOutline")
  val liftFraction by animateFloatAsState(if (preview != null) 1f else 0f, tween(120), label = "clipLift")
  var longPressReordering by remember { mutableStateOf(false) }
  val density = LocalDensity.current
  val reorderLift by animateFloatAsState(if (longPressReordering) 1f else 0f, tween(110), label = "reorderLift")
  val visualLift = max(liftFraction, reorderLift)
  val displayStartMs = preview?.startTimeMs ?: clip.startMs
  val displayDurationMs = preview?.durationMs ?: clip.durationMs
  val left = ((displayStartMs / 1_000f) * pixelsPerSecond * zoom - scrollOffsetPx).roundToInt()
  val width = ((displayDurationMs / 1_000f) * pixelsPerSecond * zoom).roundToInt().coerceAtLeast(56)
  val widthDp = with(density) { width.toDp() }
  val pxPerMs = TimelineEngine.pixelsPerMs(zoom, pixelsPerSecond)
  val isPreviewing = preview != null
  val visualState = when {
    clip.isVisualMediaClip() && !clip.hasUsableMediaUri() -> ClipVisualState.Invalid
    selected || isPreviewing || longPressReordering -> ClipVisualState.Selected
    active -> ClipVisualState.Active
    else -> ClipVisualState.Inactive
  }
  val backgroundColor = when (visualState) {
    ClipVisualState.Invalid -> StudioDanger.copy(alpha = 0.28f)
    ClipVisualState.Selected -> if (trackType == TrackType.Audio) EditorChromeAudio else Color(0xFF1A1A1A)
    ClipVisualState.Active -> if (trackType == TrackType.Audio) EditorChromeAudio.copy(alpha = 0.92f) else Color(0xFF222222)
    ClipVisualState.Inactive -> if (trackType == TrackType.Audio) EditorChromeAudio.copy(alpha = 0.78f) else Color(0xFF262626)
  }
  val outlineColor = when {
    preview?.isValid == false -> StudioDanger
    visualState == ClipVisualState.Invalid -> StudioDanger.copy(alpha = 0.92f)
    selected -> EditorChromePrimary.copy(alpha = 0.92f * selectedOutline)
    active -> if (trackType == TrackType.Audio) EditorChromeAudioAccent else Color.White.copy(alpha = 0.26f)
    else -> Color.White.copy(alpha = 0.18f)
  }
  Box(
    Modifier.offset { IntOffset(left, (-3 * visualLift).roundToInt()) }.graphicsLayer { scaleX = 1f + visualLift * 0.025f; scaleY = 1f + visualLift * 0.055f; shadowElevation = visualLift * 14f }.width(widthDp).fillMaxHeight().padding(vertical = 1.dp).clip(RoundedCornerShape(if (trackType == TrackType.Video) 8.dp else 6.dp)).background(backgroundColor).border(if (selected || isPreviewing || longPressReordering || visualState == ClipVisualState.Invalid) 2.dp else 1.dp, outlineColor, RoundedCornerShape(if (trackType == TrackType.Video) 8.dp else 6.dp)).clickable { onSelect(clip.id) }.pointerInput(clip.id, selected) {
      detectTapGestures(onDoubleTap = { onSelect(clip.id); onSplit() }, onLongPress = { onSelect(clip.id); longPressReordering = true })
    }.pointerInput(clip.id, trackType, timeline.version) {
      var dragPx = 0f
      detectDragGesturesAfterLongPress(
        onDragStart = { dragPx = 0f; longPressReordering = true; onSelect(clip.id); onPreview(TimelineClipPreviewState(clip.id, clip.startMs, clip.durationMs)) },
        onDrag = { change, dragAmount ->
          dragPx += dragAmount.x
          val targetIndex = (index + (dragPx / width.coerceAtLeast(1)).roundToInt()).coerceAtLeast(0)
          if (trackType == TrackType.Video) onPreview(TimelineClipPreviewState(clip.id, clip.startMs, clip.durationMs, snapLabel = "Reorder ${targetIndex + 1}"))
          change.consumePositionChange()
        },
        onDragEnd = {
          val targetIndex = (index + (dragPx / width.coerceAtLeast(1)).roundToInt()).coerceAtLeast(0)
          if (trackType == TrackType.Video) onReorder(targetIndex)
          longPressReordering = false
          onPreviewEnd()
        },
        onDragCancel = { longPressReordering = false; onPreviewEnd() },
      )
    }.pointerInput(clip.id, timeline.version, zoom, scrollOffsetPx, viewportWidthPx) {
      var accumulatedDragPx = 0f
      var previewState: TimelineClipPreviewState? = null
      var lastValidStartMs = clip.startMs
      detectHorizontalDragGestures(
        onDragStart = {
          accumulatedDragPx = 0f
          lastValidStartMs = clip.startMs
          onSelect(clip.id)
          previewState = null
        },
        onHorizontalDrag = { change, dragAmount ->
          accumulatedDragPx += dragAmount
          val slop = TimelineEngine.touchSlopGate(0f, 0f, accumulatedDragPx, 0f, touchSlopPx, TimelineGestureMode.DRAGGING_CLIP)
          if (!slop.hasExceededTouchSlop) return@detectHorizontalDragGestures
          val deltaMs = (accumulatedDragPx / pxPerMs).roundToLong()
          val proposedStartMs = clip.startMs + deltaMs
          val boundary = TimelineEngine.resolveClipBoundaryState(timeline, clip.id, proposedStartMs, lastValidStartMs)
          val autoScroll = TimelineEngine.resolveAutoScroll(change.position.x, viewportWidthPx, clip.id, proposedStartMs)
          if (autoScroll.isAutoScrolling) onAutoScroll(0f, autoScroll.direction)
          val resolution = TimelineEngine.resolveDraggedClip(timeline, clip.id, proposedStartMs)
          if (resolution.isValid) lastValidStartMs = resolution.resolvedStartTimeMs
          previewState = TimelineClipPreviewState(
            clipId = clip.id,
            startTimeMs = if (boundary.isBeyondStart || boundary.isBeyondEnd) (resolution.resolvedStartTimeMs + (boundary.resistanceOffsetPx / pxPerMs).roundToLong()).coerceAtLeast(0L) else resolution.resolvedStartTimeMs,
            durationMs = clip.durationMs,
            snapLabel = resolution.snapResolution.target?.label,
            isValid = resolution.isValid,
            snapTimeMs = resolution.snapResolution.snappedTimeMs,
          )
          onPreview(previewState)
          change.consumePositionChange()
        },
        onDragEnd = {
          val finalPreview = previewState
          if (finalPreview != null && finalPreview.isValid) onMove(finalPreview.startTimeMs - clip.startMs) else if (finalPreview != null) onPreview(TimelineClipPreviewState(clip.id, lastValidStartMs, clip.durationMs, snapLabel = "Invalid", isValid = false))
          previewState = null
          onPreviewEnd()
        },
        onDragCancel = {
          previewState = null
          onPreviewEnd()
        },
      )
    }.semantics { contentDescription = "${clip.clipType} clip, ${trackType.label} track, starts at ${clip.startMs.asTimecode()}, duration ${clip.durationMs.asTimecode()}" },
    contentAlignment = Alignment.Center,
  ) {
    if (selected && trackType == TrackType.Video) {
      Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().width(4.dp).background(EditorChromePrimary))
    }
    val shouldRenderBitmapThumbnail = clip.clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Overlay)
    if (shouldRenderBitmapThumbnail && thumbnailBitmap != null) {
      Image(bitmap = thumbnailBitmap.asImageBitmap(), contentDescription = "${clip.title} thumbnail", modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
    } else if (shouldRenderBitmapThumbnail || trackType == TrackType.Audio) {
      Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.16f))))) {
        Row(Modifier.matchParentSize().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
          repeat((width / 44).coerceIn(1, 8)) { Box(Modifier.weight(1f).height(if (trackType == TrackType.Audio) 22.dp else 18.dp).clip(RoundedCornerShape(6.dp)).background(if (trackType == TrackType.Audio) Brush.verticalGradient(listOf(EditorChromeAudioAccent.copy(alpha = 0.50f), EditorChromeAudioAccent.copy(alpha = 0.18f))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.10f))))) }
        }
        Text(
          clipTypeBadge(clip.clipType),
          modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(999.dp)).background(Color.Black.copy(alpha = 0.52f)).padding(horizontal = 6.dp, vertical = 2.dp),
          color = Color.White,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
        )
      if (shouldRenderBitmapThumbnail) {
        Text(
          if (!clip.hasUsableMediaUri()) "Invalid media" else "Thumbnail unavailable",
          modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 4.dp),
          color = Color.White.copy(alpha = 0.92f),
          fontSize = 9.sp,
          fontWeight = FontWeight.Medium,
        )
      }
    }
    }
    if (active && trackType != TrackType.Audio) Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.30f)))
    transition?.let { Box(Modifier.align(if (it.fromClipId == clip.id) Alignment.CenterEnd else Alignment.CenterStart).width(18.dp).fillMaxHeight().background(EditorChromeAudioAccent.copy(alpha = 0.22f))) }
    clip.keyframes.distinctBy { it.timeMs }.forEach { keyframe ->
      val kx = ((keyframe.timeMs.toFloat() / clip.durationMs.coerceAtLeast(1L)) * width).roundToInt().coerceIn(8, width - 8)
      Box(Modifier.offset { IntOffset(kx - 4, 6) }.size(8.dp).background(StudioAccent, RoundedCornerShape(2.dp)))
    }
    Text(clip.title.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = if (trackType == TrackType.Video) 10.sp else 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = if (trackType == TrackType.Video) 16.dp else 10.dp), color = if (trackType == TrackType.Audio) EditorChromeAudioAccent else if (selected) Color.White else Color.White.copy(alpha = 0.96f))
    if (trackType == TrackType.Video && clip.clipType == ClipType.Video && clip.sourceDurationMs != null) {
      Text(
        clip.sourceDurationMs.asTimecode(),
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 4.dp).clip(RoundedCornerShape(999.dp)).background(StudioBackground.copy(alpha = 0.72f)).padding(horizontal = 6.dp, vertical = 2.dp),
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
      )
    }
    if (trackType == TrackType.Video && clip.clipType == ClipType.Image) {
      Text(
        "${(displayDurationMs / 1000f).let { "%.1fs".format(it) }}",
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 4.dp).clip(RoundedCornerShape(999.dp)).background(StudioBackground.copy(alpha = 0.72f)).padding(horizontal = 6.dp, vertical = 2.dp),
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
      )
    }
    if (selected) {
      TrimHandleGrip(Modifier.align(Alignment.CenterStart), color, TrimHandle.Left, clip, timeline, zoom, pixelsPerSecond, touchSlopPx, preview, onTrim, onPreview, onPreviewEnd, onPreviewSeek)
      TrimHandleGrip(Modifier.align(Alignment.CenterEnd), color, TrimHandle.Right, clip, timeline, zoom, pixelsPerSecond, touchSlopPx, preview, onTrim, onPreview, onPreviewEnd, onPreviewSeek)
    }
    if (preview?.snapLabel != null) {
      Text(preview.snapLabel, modifier = Modifier.align(Alignment.TopCenter).padding(top = 3.dp).clip(RoundedCornerShape(999.dp)).background(StudioBackground.copy(alpha = 0.82f)).padding(horizontal = 7.dp, vertical = 2.dp), color = StudioAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun TrimHandleGrip(modifier: Modifier, color: Color, handle: TrimHandle, clip: TimelineClip, timeline: Timeline, zoom: Float, pixelsPerSecond: Float, touchSlopPx: Float, preview: TimelineClipPreviewState?, onTrim: (TrimHandle, Long) -> Unit, onPreview: (TimelineClipPreviewState?) -> Unit, onPreviewEnd: () -> Unit, onPreviewSeek: (Long) -> Unit) {
  val pxPerMs = TimelineEngine.pixelsPerMs(zoom, pixelsPerSecond)
  Box(
    modifier.requiredWidth(48.dp).fillMaxHeight().background(if (preview?.trimHandle == handle) StudioAccent.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.82f)).padding(horizontal = 10.dp).pointerInput(clip.id, handle, timeline.version, zoom) {
      var accumulatedDragPx = 0f
      var previewState: TimelineClipPreviewState? = null
      var slopExceeded = false
      detectHorizontalDragGestures(
        onDragStart = {
          accumulatedDragPx = 0f
          slopExceeded = false
        },
        onHorizontalDrag = { change, dragAmount ->
          accumulatedDragPx += dragAmount
          val slop = TimelineEngine.touchSlopGate(0f, 0f, accumulatedDragPx, 0f, touchSlopPx, TimelineGestureMode.TRIMMING_CLIP)
          if (!slop.hasExceededTouchSlop) return@detectHorizontalDragGestures
          if (!slopExceeded) {
            slopExceeded = true
            previewState = TimelineClipPreviewState(clip.id, clip.startMs, clip.durationMs, trimHandle = handle)
            onPreview(previewState)
          }
          val deltaMs = (accumulatedDragPx / pxPerMs).roundToLong()
          val proposedTime = if (handle == TrimHandle.Left) clip.startMs + deltaMs else clip.startMs + clip.durationMs + deltaMs
          val resolution = TimelineEngine.resolveTrimGesture(timeline, clip.id, handle, proposedTime)
          val scrub = TimelineEngine.resolveTrimPreviewScrub(timeline, clip.id, handle, proposedTime)
          val nextStart = if (handle == TrimHandle.Left) resolution.resolvedTimeMs else clip.startMs
          val nextEnd = if (handle == TrimHandle.Right) resolution.resolvedTimeMs else clip.startMs + clip.durationMs
          previewState = TimelineClipPreviewState(
            clipId = clip.id,
            startTimeMs = nextStart,
            durationMs = (nextEnd - nextStart).coerceAtLeast(TimelineEngine.MinClipDurationMs),
            snapLabel = resolution.snapResolution.target?.label,
            isValid = resolution.isValid,
            trimHandle = handle,
            snapTimeMs = resolution.snapResolution.snappedTimeMs,
          )
          onPreview(previewState)
          onPreviewSeek(scrub.previewTimeMs)
          change.consumePositionChange()
        },
        onDragEnd = {
          val state = previewState?.takeIf { it.trimHandle == handle }
          if (state != null) {
            val deltaMs = if (handle == TrimHandle.Left) state.startTimeMs - clip.startMs else (state.startTimeMs + state.durationMs) - (clip.startMs + clip.durationMs)
            onTrim(handle, deltaMs)
            onPreviewSeek(if (handle == TrimHandle.Left) state.startTimeMs else state.startTimeMs + state.durationMs)
          }
          previewState = null
          onPreviewEnd()
        },
        onDragCancel = {
          previewState = null
          onPreviewEnd()
        },
      )
    }.semantics { contentDescription = if (handle == TrimHandle.Left) "Trim left edge" else "Trim right edge" },
  ) {
    Box(Modifier.align(Alignment.Center).width(4.dp).fillMaxHeight(0.68f).clip(RoundedCornerShape(999.dp)).background(color))
  }
}

@Composable
private fun EdgeResistanceMask(fraction: Float) {
  if (fraction <= 0f) return
  val alpha = (0.12f + fraction * 0.20f).coerceIn(0f, 0.32f)
  Row(Modifier.fillMaxSize()) {
    Box(Modifier.width(42.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(StudioSecondary.copy(alpha = alpha), Color.Transparent))))
    Spacer(Modifier.weight(1f))
    Box(Modifier.width(42.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color.Transparent, StudioSecondary.copy(alpha = alpha)))))
  }
}

@Composable
private fun TimelineGuides(timeline: Timeline, contentWidth: Int, snapTimeMs: Long?) {
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
private fun StickerToolPanel(timeline: Timeline, viewModel: MainScreenViewModel) {
  var category by remember { mutableStateOf(StickerCategory.Emoji) }
  val assets = if (category == StickerCategory.Recent) timeline.recentStickers.ifEmpty { StickerLibrary.take(4) } else StickerLibrary.filter { it.category == category }
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Sticker library", fontWeight = FontWeight.Bold)
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StickerCategory.entries.forEach { item -> FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item.label) }) } }
      LazyRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(assets, key = { it.id }) { asset -> StickerTile(asset) { viewModel.addStickerAtPlayhead(asset) } } }
      LayerActions(viewModel, enabled = timeline.selectedRealClip()?.clipType == ClipType.Sticker)
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
  val filters = FilterLibrary.presets
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Filter and adjust", fontWeight = FontWeight.Bold)
      if (selectedClip == null) Text("Select a video, image, overlay, or text layer to adjust.", color = StudioTextMuted, fontSize = 13.sp)
      LazyRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(filters, key = { it.id ?: "original" }) { filter -> FilterPreviewChip(filter, selectedClip?.filterAdjustments?.filterId == filter.id) { adjustments = filter.defaultAdjustments; viewModel.updateSelectedAdjustments(filter.defaultAdjustments) } } }
      AdjustmentControl("Brightness", adjustments.brightness, 0.5f, 1.5f) { val next = adjustments.copy(brightness = it); adjustments = next; viewModel.updateSelectedAdjustments(next) }
      AdjustmentControl("Contrast", adjustments.contrast, 0.5f, 1.6f) { val next = adjustments.copy(contrast = it); adjustments = next; viewModel.updateSelectedAdjustments(next) }
      AdjustmentControl("Saturation", adjustments.saturation, 0f, 2f) { val next = adjustments.copy(saturation = it); adjustments = next; viewModel.updateSelectedAdjustments(next) }
      AdjustmentControl("Exposure", adjustments.exposure, -1f, 1f) { val next = adjustments.copy(exposure = it); adjustments = next; viewModel.updateSelectedAdjustments(next) }
      AdjustmentControl("Temperature", adjustments.temperature, -1f, 1f) { val next = adjustments.copy(temperature = it); adjustments = next; viewModel.updateSelectedAdjustments(next) }
      AdjustmentControl("Sharpness", adjustments.sharpness, 0f, 1f) { val next = adjustments.copy(sharpness = it); adjustments = next; viewModel.updateSelectedAdjustments(next) }
    }
  }
}

@Composable
private fun FilterPreviewChip(filter: FilterPreset, selected: Boolean, onClick: () -> Unit) {
  Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = if (selected) StudioPrimary.copy(alpha = 0.45f) else StudioSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.size(92.dp, 66.dp)) {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(StudioPrimary.copy(alpha = 0.4f), StudioSecondary.copy(alpha = 0.28f)))), contentAlignment = Alignment.Center) { Text(filter.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
  }
}

@Composable
private fun EffectToolPanel(selectedClip: TimelineClip?, viewModel: MainScreenViewModel) {
  var category by remember { mutableStateOf(EffectCategory.Basic) }
  val effects = EffectLibrary.filter { it.category == category }
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Effects", fontWeight = FontWeight.Bold)
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { EffectCategory.entries.forEach { item -> FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item.label) }) } }
      LazyRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(effects, key = { it.id }) { effect -> EffectTile(effect) { viewModel.addEffectAtPlayhead(effect) } } }
      LayerActions(viewModel, enabled = selectedClip?.clipType == ClipType.Effect)
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
  val enabled = selectedClip?.clipType in setOf(ClipType.Video, ClipType.Image, ClipType.Overlay)
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Speed", fontWeight = FontWeight.Bold)
      Text(selectedClip?.let { "${it.title} · ${it.durationMs.asTimecode()} at ${"%.2f".format(speed)}x" } ?: "Select a compatible clip.", color = StudioTextMuted, fontSize = 13.sp)
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(0.5f, 1f, 1.5f, 2f).forEach { value -> FilterChip(selected = speed == value, enabled = enabled, onClick = { viewModel.updateSelectedSpeed(value) }, label = { Text("${value}x") }) } }
      if (enabled) AdjustmentControl("Speed", speed, 0.5f, 2f) { viewModel.updateSelectedSpeed(it) }
    }
  }
}

@Composable
private fun OverlayToolPanel(importedAssets: List<MediaAsset>, selectedClip: TimelineClip?, viewModel: MainScreenViewModel) {
  val overlayAssets = importedAssets.filter { it.type != MediaType.Audio }
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Overlay", fontWeight = FontWeight.Bold)
      if (overlayAssets.isEmpty()) {
        Text("Import image or video media before adding overlay layers.", color = StudioTextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
      } else {
        LazyRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(overlayAssets, key = { it.id }) { asset -> MediaMiniCard(asset) { viewModel.addOverlayAtPlayhead(asset) } } }
      }
      if (selectedClip?.clipType == ClipType.Overlay) AdjustmentControl("Opacity", selectedClip.transform.opacity, 0f, 1f) { viewModel.updateSelectedOpacity(it) }
      LayerActions(viewModel, enabled = selectedClip?.clipType == ClipType.Overlay)
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
    AudioSource.DeviceMusic -> emptyList()
    AudioSource.BuiltInMusic -> listOf("Neon pulse" to "00:18", "Lo-fi creator bed" to "00:30")
    AudioSource.ExtractedAudio -> if (selectedClip?.clipType == ClipType.Video) listOf("Extract from ${selectedClip.title}" to "linked") else emptyList()
    AudioSource.SoundEffect -> listOf("Camera click" to "00:01", "Whoosh pop" to "00:02")
  }
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { AudioSource.entries.forEach { source -> FilterChip(selected = tab == source, onClick = { tab = source }, label = { Text(source.label) }) } }
      if (items.isEmpty()) {
        Text(
          when (tab) {
            AudioSource.DeviceMusic -> "Use Import Audio to choose real device audio from the system picker."
            AudioSource.ExtractedAudio -> "Select a video clip before extracting source audio."
            else -> "No audio items are available for this source."
          },
          color = StudioTextMuted,
          fontSize = 13.sp,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
      items.forEach { item ->
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) { Text(item.first, fontWeight = FontWeight.Bold); Text(item.second, color = StudioTextMuted, fontSize = 12.sp) }
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
        LayerActions(viewModel, enabled = true)
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
       LayerActions(viewModel, enabled = selectedClip?.clipType == ClipType.Text)
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
private fun LayerActions(viewModel: MainScreenViewModel, enabled: Boolean = true) {
  Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick = viewModel::duplicateSelectedClip, enabled = enabled) { Text("Duplicate") }
    OutlinedButton(onClick = viewModel::deleteSelectedClip, enabled = enabled) { Text("Delete") }
    OutlinedButton(onClick = { viewModel.trimSelectedClip(-500) }, enabled = enabled) { Text("Trim -") }
    OutlinedButton(onClick = { viewModel.trimSelectedClip(500) }, enabled = enabled) { Text("Trim +") }
    OutlinedButton(onClick = viewModel::toggleKeyframeAtPlayhead, enabled = enabled, modifier = Modifier.semantics { contentDescription = "Toggle keyframe at playhead" }) { Text("Keyframe") }
  }
}

@Composable
private fun ToolPanel(timeline: Timeline, viewModel: MainScreenViewModel) {
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

private fun Context.readUriMetadataSafely(uri: Uri): UriMetadata? = runCatching { readUriMetadata(uri) }.getOrNull()

private fun Context.persistReadPermission(uri: Uri): Boolean {
  val grants = contentResolver.persistedUriPermissions
  if (grants.any { it.uri == uri && it.isReadPermission }) return true
  return runCatching {
    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }.isSuccess
}

private fun Context.readUriMetadata(uri: Uri): UriMetadata {
  var displayName: String? = null
  var sizeBytes: Long? = null
  val mimeType = contentResolver.getType(uri)
  val durationMs = readMediaDurationMs(uri, mimeType)
  requireNotNull(contentResolver.openInputStream(uri)) { "Selected media is not readable." }.use { }
  contentResolver.query(uri, null, null, null, null)?.use { cursor ->
    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
    if (cursor.moveToFirst()) {
      if (nameIndex >= 0) displayName = cursor.getString(nameIndex)
      if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
    }
  }
  return UriMetadata(displayName = displayName, sizeBytes = sizeBytes, mimeType = mimeType, durationMs = durationMs)
}

private fun Context.readMediaDurationMs(uri: Uri, mimeType: String?): Long? {
  if (mimeType?.startsWith("image") == true) return 3_000L
  if (mimeType?.startsWith("video") != true && mimeType?.startsWith("audio") != true) return null
  return runCatching {
    MediaMetadataRetriever().use { retriever ->
      retriever.setDataSource(this, uri)
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    }
  }.getOrNull()
}

private fun Context.loadThumbnailBitmap(mediaUri: String, thumbnailTimeMs: Long, widthPx: Int, heightPx: Int): Bitmap? {
  if (mediaUri.startsWith("local://")) return null
  val uri = runCatching { Uri.parse(mediaUri) }.getOrNull() ?: return null
  return runCatching {
    val mimeType = resolveMimeType(uri)
    when {
      mimeType?.startsWith("image") == true -> {
        contentResolver.openInputStream(uri)?.use { input ->
          BitmapFactory.decodeStream(input)
        }
      }
      mimeType?.startsWith("video") == true -> {
        MediaMetadataRetriever().use { retriever ->
          retriever.setDataSource(this, uri)
          retriever.getFrameAtTime(thumbnailTimeMs * 1_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
      }
      else -> {
        // Some picker-backed content URIs do not expose a MIME type, so try image decode first and
        // fall back to video frame extraction before giving up.
        contentResolver.openInputStream(uri)?.use { input ->
          BitmapFactory.decodeStream(input)
        } ?: MediaMetadataRetriever().use { retriever ->
          retriever.setDataSource(this, uri)
          retriever.getFrameAtTime(thumbnailTimeMs * 1_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
      }
    }?.let { source -> Bitmap.createScaledBitmap(source, widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), true) }
  }.getOrNull()
}

private fun Context.resolvePreviewSurfaceState(clip: TimelineClip?): PreviewSurfaceState {
  if (clip == null) return PreviewSurfaceState.NoMedia
  val uri = clip.mediaUri?.trim()
  if (uri.isNullOrEmpty()) return PreviewSurfaceState.InvalidUri
  val parsedUri = runCatching { Uri.parse(uri) }.getOrNull() ?: return PreviewSurfaceState.InvalidUri
  if (parsedUri.scheme.isNullOrBlank()) return PreviewSurfaceState.InvalidUri
  if (!canOpenPreviewUri(parsedUri)) return PreviewSurfaceState.LoadFailed
  val mimeType = resolveMimeType(parsedUri)
  return when {
    clip.clipType == ClipType.Video -> PreviewSurfaceState.VideoReady
    clip.clipType == ClipType.Image -> PreviewSurfaceState.ImageReady
    clip.clipType == ClipType.Overlay && mimeType?.startsWith("video") == true -> PreviewSurfaceState.VideoReady
    clip.clipType == ClipType.Overlay -> PreviewSurfaceState.ImageReady
    else -> PreviewSurfaceState.LoadFailed
  }
}

private fun Context.canOpenPreviewUri(uri: Uri): Boolean {
  return when (uri.scheme?.lowercase()) {
    "content" -> runCatching {
      contentResolver.openAssetFileDescriptor(uri, "r")?.use { true }
        ?: contentResolver.openInputStream(uri)?.use { true }
        ?: false
    }.getOrDefault(false)
    "file" -> runCatching {
      val path = uri.path ?: return@runCatching false
      File(path).exists()
    }.getOrDefault(false)
    "android.resource" -> true
    else -> false
  }
}

private fun Context.resolveMimeType(uri: Uri): String? {
  contentResolver.getType(uri)?.let { return it }
  val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())?.lowercase().orEmpty()
  return extension.takeIf { it.isNotBlank() }?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  MyApplicationTheme { MainScreen(onItemClick = {}) }
}
