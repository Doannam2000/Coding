package com.nantcompany.clipy

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.collection.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nantcompany.clipy.data.ClipyRepository.AppSnapshot
import com.nantcompany.clipy.model.AppLanguage
import com.nantcompany.clipy.model.boundedTrimEndMs
import com.nantcompany.clipy.model.boundedTrimStartMs
import com.nantcompany.clipy.model.CropRatio
import com.nantcompany.clipy.model.ExportFormat
import com.nantcompany.clipy.model.ExportRecordUi
import com.nantcompany.clipy.model.Mp4Quality
import com.nantcompany.clipy.model.SaveBehavior
import com.nantcompany.clipy.model.editorTimelineUiState
import com.nantcompany.clipy.model.shouldDispatchTimelinePreviewSeek
import com.nantcompany.clipy.model.timelineFrameStepMs
import com.nantcompany.clipy.model.timelineMsToTrackPx
import com.nantcompany.clipy.model.timelinePrefetchRange
import com.nantcompany.clipy.model.timelineStripFrameCount
import com.nantcompany.clipy.model.timelineTrackPxToMs
import com.nantcompany.clipy.model.timelineVisibleWindowMs
import com.nantcompany.clipy.model.timelineSnapshot
import com.nantcompany.clipy.model.UserPreferences
import com.nantcompany.clipy.model.WatermarkPosition
import com.nantcompany.clipy.model.exportMimeType
import com.nantcompany.clipy.model.mimeType
import com.nantcompany.clipy.model.snapTimelineMs
import com.nantcompany.clipy.model.TimelineSnapshot
import com.nantcompany.clipy.ui.ClipyViewModel
import com.nantcompany.clipy.theme.ClipyAccent
import com.nantcompany.clipy.theme.ClipyBackground
import com.nantcompany.clipy.theme.ClipyMuted
import com.nantcompany.clipy.theme.ClipyOnDark
import com.nantcompany.clipy.theme.ClipyPrimary
import com.nantcompany.clipy.theme.ClipySecondary
import com.nantcompany.clipy.theme.ClipySuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val SPLASH = "splash"
private const val INTRO = "intro"
private const val HOME = "home"
private const val EDITOR = "editor"
private const val SETTINGS = "settings"
private const val HISTORY = "history"
private const val EXPORT = "export"
private const val LANGUAGE = "language"
private const val PERFORMANCE_CACHE_MB = 256
private const val MIN_TRIM_GAP_MS = 250L
private const val TIMELINE_PREVIEW_SEEK_THROTTLE_MS = 90L
private const val TIMELINE_SETTLE_DELAY_MS = 48L

private val timelineThumbnailCache = object : LruCache<String, Bitmap>(48) {}
private val timelineThumbnailRequests = mutableSetOf<String>()

@Stable
private data class TimelineGestureAnchor(
  val trimStartMs: Long,
  val trimEndMs: Long,
  val playheadMs: Long,
  val anchorTrackOffsetPx: Float = 0f,
)

@Stable
private data class TimelineStripCell(
  val index: Int,
  val captureTimeMs: Long,
) {
  val cacheKey: String = "frame-$index-$captureTimeMs"
}

private data class TimelineVisibleWindow(
  val firstVisibleIndex: Int,
  val lastVisibleIndex: Int,
)

@Stable
private data class TimelineChromeState(
  val trimStartMs: Long,
  val trimEndMs: Long,
  val playheadMs: Long,
  val isInteracting: Boolean,
)

@Stable
private data class TimelineDockState(
  val trimLabel: String,
  val playheadLabel: String,
  val durationLabel: String,
  val zoomLabel: String,
)

@Stable
private data class TimelineUiState(
  val clipDurationMs: Long,
  val trimStartMs: Long,
  val trimEndMs: Long,
  val playheadMs: Long,
  val selectedRangeLabel: String,
  val playheadLabel: String,
)

private enum class TimelineDragTarget {
  Start,
  End,
  Playhead,
}

private enum class EditorControlTab {
  Frame,
  Motion,
  Output,
}

@Composable
fun ClipyApp(finishApp: () -> Unit) {
  val context = LocalContext.current
  val app = context.applicationContext as Application
  val viewModel: ClipyViewModel = viewModel(factory = ClipyViewModel.factory(app))
  val state by viewModel.appState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  var splashResolved by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(state.preferences.languageCode) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(state.preferences.languageCode))
  }

  LaunchedEffect(state.preferences.onboardingCompleted, splashResolved) {
    if (splashResolved) {
      navController.navigate(if (state.preferences.onboardingCompleted) HOME else INTRO) {
        popUpTo(SPLASH) { inclusive = true }
      }
    }
  }

  NavHost(navController = navController, startDestination = SPLASH) {
    composable(SPLASH) {
      SplashScreen(onReady = { splashResolved = true })
    }
    composable(INTRO) {
      IntroScreen(
        selectedLanguage = state.preferences.languageCode,
        onOpenLanguage = { navController.navigate(LANGUAGE) },
        onContinue = {
          viewModel.completeOnboarding(it)
          navController.navigate(HOME) { popUpTo(INTRO) { inclusive = true } }
        },
      )
    }
    composable(HOME) {
      HomeScreen(
        state = state,
        finishApp = finishApp,
        onImportVideo = {
          viewModel.importVideo(it)
          navController.navigate(EDITOR)
        },
        onOpenHistory = { navController.navigate(HISTORY) },
        onOpenSettings = { navController.navigate(SETTINGS) },
      )
    }
    composable(EDITOR) {
      EditorScreen(
        state = state,
        onBack = navController::popBackStack,
        onTrimStartChange = viewModel::updateTrimStart,
        onTrimEndChange = viewModel::updateTrimEnd,
        onCropChange = viewModel::updateCropRatio,
        onSpeedChange = viewModel::updateSpeed,
        onPlayheadChange = viewModel::updatePlayhead,
        onStepBackward = viewModel::stepPlayheadBackward,
        onStepForward = viewModel::stepPlayheadForward,
        onTimelineZoomChange = viewModel::updateTimelineZoom,
        onToggleMute = viewModel::toggleMuted,
        onToggleReverse = viewModel::toggleReverse,
        onToggleBoomerang = viewModel::toggleBoomerang,
        onWatermarkChange = viewModel::updateWatermark,
        onWatermarkPositionChange = viewModel::updateWatermarkPosition,
        onFormatChange = viewModel::updateFormat,
        onGifFpsChange = viewModel::updateGifFps,
        onGifResolutionChange = viewModel::updateGifResolution,
        onMp4QualityChange = viewModel::updateMp4Quality,
        onOutputNameChange = viewModel::updateOutputName,
        onOpenHistory = { navController.navigate(HISTORY) },
        onOpenSettings = { navController.navigate(SETTINGS) },
        onExport = {
          if (viewModel.startExport()) {
            navController.navigate(EXPORT)
          }
        },
      )
    }
    composable(SETTINGS) {
      SettingsScreen(
        preferences = state.preferences,
        onBack = navController::popBackStack,
        onSave = viewModel::saveSettings,
        onOpenLanguage = { navController.navigate(LANGUAGE) },
        onClearHistory = viewModel::clearHistory,
        onExit = finishApp,
      )
    }
    composable(LANGUAGE) {
      LanguageScreen(
        selectedLanguage = state.preferences.languageCode,
        onBack = navController::popBackStack,
        onApply = {
          viewModel.saveSettings(state.preferences.copy(languageCode = it.code))
          navController.popBackStack()
        },
      )
    }
    composable(HISTORY) {
      HistoryScreen(
        state = state,
        onBack = navController::popBackStack,
        onClearHistory = viewModel::clearHistory,
        onReuse = {
          viewModel.reuseHistoryRecord(it)
          navController.popBackStack()
        },
      )
    }
    composable(EXPORT) {
      ExportScreen(state = state, onBack = navController::popBackStack, onCancel = viewModel::cancelExport)
    }
  }
}

@Composable
private fun SplashScreen(onReady: () -> Unit) {
  val transition = rememberInfiniteTransition(label = "glow")
  val alpha by transition.animateFloat(
    initialValue = 0.5f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
    label = "logoAlpha",
  )

  LaunchedEffect(Unit) {
    delay(1200)
    onReady()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ClipyBackground)
      .statusBarsPadding()
      .navigationBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
        modifier = Modifier
          .size(110.dp)
          .clip(RoundedCornerShape(32.dp))
          .background(
            Brush.radialGradient(
              listOf(
                ClipyPrimary.copy(alpha = alpha),
                ClipySecondary.copy(alpha = alpha * 0.4f),
                Color.Transparent,
              ),
            ),
          ),
        contentAlignment = Alignment.Center,
      ) {
        Text("C", style = MaterialTheme.typography.headlineLarge, color = ClipyOnDark)
      }
      Spacer(Modifier.height(18.dp))
      Text("Clipy", style = MaterialTheme.typography.headlineLarge, color = ClipyOnDark)
      Text(
        stringResource(R.string.tagline),
        modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
        textAlign = TextAlign.Center,
        color = ClipyMuted,
      )
      Spacer(Modifier.height(40.dp))
      CircularProgressIndicator(color = ClipyPrimary)
      Spacer(Modifier.height(12.dp))
      Text(stringResource(R.string.splash_loading), color = ClipyMuted)
    }
  }
}

@Composable
private fun IntroScreen(
  selectedLanguage: String,
  onOpenLanguage: () -> Unit,
  onContinue: (AppLanguage) -> Unit,
) {
  var page by rememberSaveable { mutableStateOf(0) }
  var language by rememberSaveable { mutableStateOf(AppLanguage.entries.first { it.code == selectedLanguage }) }
  val cards = listOf(
    stringResource(R.string.onboarding_trim_title) to stringResource(R.string.onboarding_trim_body),
    stringResource(R.string.onboarding_frame_title) to stringResource(R.string.onboarding_frame_body),
    stringResource(R.string.onboarding_export_title) to stringResource(R.string.onboarding_export_body),
  )

  LaunchedEffect(selectedLanguage) {
    language = AppLanguage.entries.first { it.code == selectedLanguage }
  }

  Scaffold(containerColor = ClipyBackground, contentWindowInsets = WindowInsets.safeDrawing) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(20.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(stringResource(R.string.intro_title), style = MaterialTheme.typography.headlineLarge)
        AnimatedContent(targetState = page, label = "introPage") { currentPage ->
          PremiumCard {
            Text(cards[currentPage].first, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(cards[currentPage].second, color = ClipyMuted)
            Spacer(Modifier.height(18.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                  Brush.linearGradient(
                    listOf(
                      ClipyPrimary.copy(alpha = 0.35f),
                      ClipySecondary.copy(alpha = 0.18f),
                      ClipyAccent.copy(alpha = 0.18f),
                    ),
                  ),
                ),
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(stringResource(R.string.intro_language_title), style = MaterialTheme.typography.titleLarge)
          TextButton(onClick = onOpenLanguage) {
            Text(stringResource(R.string.settings_language))
          }
        }
        AppLanguage.entries.forEach { option ->
          LanguageCard(language = option, selected = language == option, onClick = { language = option })
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          repeat(cards.size) { index ->
            Box(
              modifier = Modifier
                .height(6.dp)
                .width(if (index == page) 32.dp else 12.dp)
                .clip(CircleShape)
                .background(if (index == page) ClipyPrimary else ClipySurfaceVariant()),
            )
          }
        }
        Button(
          onClick = { if (page < cards.lastIndex) page += 1 else onContinue(language) },
          modifier = Modifier.fillMaxWidth().height(56.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary),
        ) {
          Text(stringResource(if (page < cards.lastIndex) R.string.intro_continue else R.string.intro_enter))
        }
        TextButton(onClick = { onContinue(language) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
          Text(stringResource(R.string.intro_skip))
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
  state: AppSnapshot,
  finishApp: () -> Unit,
  onImportVideo: (Uri) -> Unit,
  onOpenHistory: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val context = LocalContext.current
  var confirmExit by rememberSaveable { mutableStateOf(false) }
  val recentExports = remember(state.history) { state.history.take(3) }
  val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let {
      runCatching {
        context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      onImportVideo(it)
    }
  }

  Scaffold(
    containerColor = ClipyBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("Clipy") },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ClipyBackground),
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      PremiumCard {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.tagline), color = ClipyMuted)
        Spacer(Modifier.height(18.dp))
        Button(
          onClick = { picker.launch(arrayOf("video/*")) },
          modifier = Modifier.fillMaxWidth().height(56.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary),
        ) {
          Icon(Icons.Rounded.FolderOpen, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.home_pick_video))
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.home_pick_hint), color = ClipyMuted)
      }

      SectionCard(title = stringResource(R.string.home_recent_exports)) {
        if (recentExports.isEmpty()) {
          Text(stringResource(R.string.history_empty_body), color = ClipyMuted)
        } else {
          recentExports.forEach { item ->
            HistoryItemCard(item = item, onReuse = {}, showReuseAction = false)
            Spacer(Modifier.height(10.dp))
          }
        }
      }

      SectionCard(title = stringResource(R.string.home_tools)) {
        OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
          Icon(Icons.Rounded.History, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.nav_history))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
          Icon(Icons.Rounded.Settings, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.nav_settings))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { confirmExit = true }, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(R.string.nav_exit))
        }
      }
    }
  }

  if (confirmExit) {
    AlertDialog(
      onDismissRequest = { confirmExit = false },
      confirmButton = { TextButton(onClick = finishApp) { Text(stringResource(R.string.nav_exit)) } },
      dismissButton = { TextButton(onClick = { confirmExit = false }) { Text(stringResource(R.string.dialog_stay)) } },
      title = { Text(stringResource(R.string.dialog_exit_title)) },
      text = { Text(stringResource(R.string.dialog_exit_body)) },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(
  state: AppSnapshot,
  onBack: () -> Unit,
  onTrimStartChange: (Long) -> Unit,
  onTrimEndChange: (Long) -> Unit,
  onCropChange: (CropRatio) -> Unit,
  onSpeedChange: (Float) -> Unit,
  onPlayheadChange: (Long) -> Unit,
  onStepBackward: () -> Unit,
  onStepForward: () -> Unit,
  onTimelineZoomChange: (Float) -> Unit,
  onToggleMute: () -> Unit,
  onToggleReverse: () -> Unit,
  onToggleBoomerang: () -> Unit,
  onWatermarkChange: (String) -> Unit,
  onWatermarkPositionChange: (WatermarkPosition) -> Unit,
  onFormatChange: (ExportFormat) -> Unit,
  onGifFpsChange: (Int) -> Unit,
  onGifResolutionChange: (String) -> Unit,
  onMp4QualityChange: (Mp4Quality) -> Unit,
  onOutputNameChange: (String) -> Unit,
  onOpenHistory: () -> Unit,
  onOpenSettings: () -> Unit,
  onExport: () -> Unit,
) {
  val context = LocalContext.current
  val draft = state.draft
  val player = remember { ExoPlayer.Builder(context).build() }
  val timeline = remember(draft) { draft.timelineSnapshot() }
  var selectedControlTab by rememberSaveable { mutableStateOf(EditorControlTab.Frame) }
  var timelineInteracting by remember { mutableStateOf(false) }
  var visibleWindowStartMs by remember { mutableStateOf(0L) }
  var visibleWindowEndMs by remember { mutableStateOf(draft.sourceDurationMs) }
  var pendingSeekMs by remember { mutableStateOf<Long?>(null) }
  var syncedPlayerPositionMs by remember { mutableStateOf(-1L) }
  var isPlaying by remember { mutableStateOf(false) }
  val timelineChrome = remember(draft.trimStartMs, draft.trimEndMs, draft.playheadMs, timelineInteracting) {
    TimelineChromeState(
      trimStartMs = draft.trimStartMs,
      trimEndMs = draft.trimEndMs,
      playheadMs = draft.playheadMs,
      isInteracting = timelineInteracting,
    )
  }
  val timelineState = remember(
    draft.trimStartMs,
    draft.trimEndMs,
    draft.playheadMs,
    draft.sourceDurationMs,
    visibleWindowStartMs,
    visibleWindowEndMs,
    timelineInteracting,
    pendingSeekMs,
  ) {
    editorTimelineUiState(
      timeline = timeline,
      visibleWindowStartMs = visibleWindowStartMs,
      visibleWindowEndMs = visibleWindowEndMs,
      isScrubbingTimeline = timelineInteracting,
      pendingSeekMs = pendingSeekMs,
    )
  }
  val timelineUi = remember(timelineState) {
    TimelineUiState(
      clipDurationMs = timelineState.clipDurationMs,
      trimStartMs = timelineState.trimStartMs,
      trimEndMs = timelineState.trimEndMs,
      playheadMs = timelineState.pendingSeekMs ?: timelineState.playheadMs,
      selectedRangeLabel = formatTimelineWindow(timelineState.trimStartMs, timelineState.trimEndMs),
      playheadLabel = formatDurationMs(timelineState.pendingSeekMs ?: timelineState.playheadMs),
    )
  }
  val dockState = remember(timelineUi, draft.sourceDurationMs, timeline.zoom) {
    TimelineDockState(
      trimLabel = formatTimelineWindow(draft.trimStartMs, draft.trimEndMs),
      playheadLabel = timelineUi.playheadLabel,
      durationLabel = formatDurationMs(draft.sourceDurationMs),
      zoomLabel = String.format(java.util.Locale.US, "%.1fx", timeline.zoom),
    )
  }

  LaunchedEffect(draft.sourceUri) {
    if (draft.sourceUri.isBlank()) {
      player.stop()
      player.clearMediaItems()
    } else {
      player.setMediaItem(MediaItem.fromUri(draft.sourceUri))
      player.prepare()
      player.seekTo(draft.playheadMs)
    }
  }

  LaunchedEffect(draft.playheadMs, draft.sourceUri) {
    if (
      draft.sourceUri.isNotBlank() &&
      !timelineInteracting &&
      abs(player.currentPosition - draft.playheadMs) > timelineFrameStepMs(draft.sourceDurationMs)
    ) {
      player.seekTo(draft.playheadMs)
    }
  }

  LaunchedEffect(player, draft.trimStartMs, draft.trimEndMs, draft.sourceUri) {
    while (isActive) {
      delay(66)
      if (draft.sourceUri.isBlank()) continue
      isPlaying = player.isPlaying
      val currentPosition = player.currentPosition.coerceAtLeast(0L)
      val boundedPosition = currentPosition.coerceIn(draft.trimStartMs, draft.trimEndMs)
      if (player.isPlaying && currentPosition >= draft.trimEndMs) {
        player.seekTo(draft.trimStartMs)
      }
      val frameStep = timelineFrameStepMs(draft.sourceDurationMs)
      if (abs(boundedPosition - syncedPlayerPositionMs) >= frameStep || boundedPosition == draft.trimStartMs || boundedPosition == draft.trimEndMs) {
        syncedPlayerPositionMs = boundedPosition
        onPlayheadChange(boundedPosition)
      }
    }
  }

  DisposableEffect(player) {
    onDispose { player.release() }
  }

  Scaffold(
    containerColor = ClipyBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("Clipy") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
          }
        },
        actions = {
          IconButton(onClick = onOpenHistory) { Icon(Icons.Rounded.History, contentDescription = stringResource(R.string.nav_history)) }
          IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.nav_settings)) }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ClipyBackground),
      )
    },
    bottomBar = {
      Surface(color = Color(0xFF10141C), tonalElevation = 4.dp) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp).navigationBarsPadding(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(stringResource(R.string.editor_ready), style = MaterialTheme.typography.titleMedium)
              Text(
                stringResource(if (draft.exportFormat == ExportFormat.Gif) R.string.editor_gif_hint else R.string.editor_mp4_hint),
                color = ClipyMuted,
                style = MaterialTheme.typography.bodySmall,
              )
            }
            AssistChip(
              onClick = {},
              label = { Text(draft.exportFormat.name.uppercase()) },
              leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(ClipyAccent)) },
            )
          }
          if (draft.sourceUri.isBlank()) {
            Text(stringResource(R.string.editor_video_required), color = MaterialTheme.colorScheme.error)
          }
          Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary),
            enabled = draft.sourceUri.isNotBlank(),
          ) {
            Text(stringResource(if (draft.exportFormat == ExportFormat.Gif) R.string.editor_export_gif else R.string.editor_export_mp4))
          }
        }
      }
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 12.dp, vertical = 10.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF090C11),
        tonalElevation = 0.dp,
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(draft.displayName, color = ClipyOnDark, style = MaterialTheme.typography.titleMedium, maxLines = 1)
              Text(stringResource(R.string.editor_workspace_hint), color = ClipyMuted, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
              if (draft.isMuted) {
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.toggle_mute)) })
              }
              if (draft.isReversed) {
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.toggle_reverse)) })
              }
            }
          }

          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0D1117),
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .aspectRatio(9f / 16f)
                  .clip(RoundedCornerShape(24.dp))
                  .background(
                    Brush.verticalGradient(
                      listOf(
                        Color(0xFF010203),
                        Color(0xFF05080D),
                        Color(0xFF111723),
                      ),
                    ),
                  )
                  .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
              ) {
                if (draft.sourceUri.isBlank()) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(56.dp), tint = ClipyOnDark)
                    Spacer(Modifier.height(8.dp))
                    Text(draft.displayName)
                    Text(stringResource(R.string.editor_pick_video_hint), color = ClipyMuted)
                  }
                } else {
                  AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                      PlayerView(viewContext).apply {
                        useController = false
                        this.player = player
                      }
                    },
                    update = { it.player = player },
                  )
                  EditorOverlayBadge(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                    title = dockState.playheadLabel,
                    subtitle = dockState.trimLabel,
                  )
                  EditorStatusPill(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    isLive = timelineChrome.isInteracting || isPlaying,
                  )
                  Box(
                    modifier = Modifier
                      .align(Alignment.Center)
                      .fillMaxHeight()
                      .width(2.dp)
                      .background(ClipyOnDark.copy(alpha = 0.92f)),
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  TransportIconButton(
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.editor_transport_previous),
                    onClick = onStepBackward,
                  )
                  TransportIconButton(
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.editor_transport_play),
                    onClick = {
                      if (player.isPlaying) player.pause() else player.play()
                      isPlaying = player.isPlaying
                    },
                    highlighted = true,
                  )
                  TransportIconButton(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.editor_transport_next),
                    onClick = onStepForward,
                  )
                }
                Column(
                  modifier = Modifier.weight(1f),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  Text(dockState.playheadLabel, color = ClipyOnDark, style = MaterialTheme.typography.titleLarge)
                  Text(dockState.trimLabel, color = ClipyMuted, style = MaterialTheme.typography.bodySmall)
                }
                AssistChip(
                  onClick = {},
                  label = { Text(stringResource(R.string.editor_timeline_zoom, timeline.zoom)) },
                  leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(ClipyAccent)) },
                )
              }

              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF141923),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Text(stringResource(R.string.editor_trim_label), color = ClipyOnDark, style = MaterialTheme.typography.titleSmall)
                      Text(dockState.trimLabel, color = ClipyMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(dockState.zoomLabel, color = ClipySecondary, style = MaterialTheme.typography.labelLarge)
                  }
                  TimelineEditor(
                    sourceUri = draft.sourceUri,
                    timeline = timeline,
                    onTrimStartChange = onTrimStartChange,
                    onTrimEndChange = onTrimEndChange,
                    onPlayheadChange = onPlayheadChange,
                    onZoomChange = onTimelineZoomChange,
                    onInteractionChange = {
                      timelineInteracting = it
                      if (!it) pendingSeekMs = null
                    },
                    onPendingSeekChange = { pendingSeekMs = it },
                    onVisibleWindowChange = { startMs, endMs ->
                      visibleWindowStartMs = startMs
                      visibleWindowEndMs = endMs
                    },
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    EditorStatChip(label = stringResource(R.string.editor_trim_label), value = dockState.trimLabel, modifier = Modifier.weight(1f))
                    EditorStatChip(label = stringResource(R.string.editor_playhead_label), value = dockState.playheadLabel, modifier = Modifier.weight(1f))
                    EditorStatChip(label = stringResource(R.string.editor_duration_label), value = dockState.durationLabel, modifier = Modifier.weight(1f))
                  }
                }
              }
            }
          }
        }
      }

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF121720),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(stringResource(R.string.editor_tool_rail), style = MaterialTheme.typography.titleLarge)
          Text(stringResource(R.string.editor_tools_compact), color = ClipyMuted)
          EditorControlTabs(selected = selectedControlTab, onSelected = { selectedControlTab = it })
          when (selectedControlTab) {
            EditorControlTab.Frame -> {
              EditorControlGroup(title = stringResource(R.string.section_frame)) {
                ChipRow(items = CropRatio.entries.toList(), selected = draft.cropRatio, label = { Text(it.label) }, onSelected = onCropChange)
              }
            }
            EditorControlTab.Motion -> {
              EditorControlGroup(title = stringResource(R.string.section_motion)) {
                ChipRow(items = listOf(0.5f, 1f, 1.5f, 2f), selected = draft.speedMultiplier, label = { Text("${it}x") }, onSelected = onSpeedChange)
              }
            }
            EditorControlTab.Output -> {
              EditorControlGroup(title = stringResource(R.string.section_output)) {
                ChipRow(items = ExportFormat.entries.toList(), selected = draft.exportFormat, label = { Text(it.name.uppercase()) }, onSelected = onFormatChange)
                Spacer(Modifier.height(10.dp))
                if (draft.exportFormat == ExportFormat.Gif) {
                  ChipRow(items = listOf(12, 18, 24, 30), selected = draft.gifFps, label = { Text("${it} FPS") }, onSelected = onGifFpsChange)
                  Spacer(Modifier.height(10.dp))
                  ChipRow(items = listOf("480p", "720p", "1080p"), selected = draft.gifResolution, label = { Text(it) }, onSelected = onGifResolutionChange)
                } else {
                  ChipRow(items = Mp4Quality.entries.toList(), selected = draft.mp4Quality, label = { Text(mp4QualityLabel(it)) }, onSelected = onMp4QualityChange)
                }
              }
            }
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactToggleCard(title = stringResource(R.string.toggle_mute), checked = draft.isMuted, onToggle = onToggleMute, modifier = Modifier.weight(1f))
            CompactToggleCard(title = stringResource(R.string.toggle_reverse), checked = draft.isReversed, onToggle = onToggleReverse, modifier = Modifier.weight(1f))
            CompactToggleCard(title = stringResource(R.string.toggle_boomerang), checked = draft.isBoomerang, onToggle = onToggleBoomerang, modifier = Modifier.weight(1f))
          }
        }
      }

      SectionCard(title = stringResource(R.string.section_watermark)) {
        OutlinedTextField(
          value = draft.watermarkText,
          onValueChange = onWatermarkChange,
          modifier = Modifier.fillMaxWidth(),
          label = { Text(stringResource(R.string.watermark_label)) },
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(12.dp))
        ChipRow(items = WatermarkPosition.entries.toList(), selected = draft.watermarkPosition, label = { Text(watermarkPositionLabel(it)) }, onSelected = onWatermarkPositionChange)
      }

      SectionCard(title = stringResource(R.string.section_output)) {
        OutlinedTextField(
          value = draft.outputName,
          onValueChange = onOutputNameChange,
          modifier = Modifier.fillMaxWidth(),
          label = { Text(stringResource(R.string.output_name_label)) },
        )
        Spacer(Modifier.height(12.dp))
        Text(exportSummary(state), color = ClipyMuted)
      }
      Spacer(Modifier.height(12.dp))
    }
  }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
  preferences: UserPreferences,
  onBack: () -> Unit,
  onSave: (UserPreferences) -> Unit,
  onOpenLanguage: () -> Unit,
  onClearHistory: () -> Unit,
  onExit: () -> Unit,
) {
  val context = LocalContext.current
  var edited by remember(preferences) { mutableStateOf(preferences) }
  var confirmExit by rememberSaveable { mutableStateOf(false) }
  var confirmClearHistory by rememberSaveable { mutableStateOf(false) }
  var cacheLimitMb by rememberSaveable { mutableStateOf(PERFORMANCE_CACHE_MB) }

  Scaffold(
    containerColor = ClipyBackground,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.settings_title)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back)) } },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ClipyBackground),
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      SectionCard(title = stringResource(R.string.settings_language)) {
        Text(stringResource(R.string.settings_language_hint), color = ClipyMuted)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenLanguage, modifier = Modifier.fillMaxWidth()) {
          Text(
            stringResource(
              R.string.settings_language_current,
              when (AppLanguage.entries.first { it.code == edited.languageCode }) {
                AppLanguage.English -> context.getString(R.string.language_english)
                AppLanguage.Vietnamese -> context.getString(R.string.language_vietnamese)
              },
            ),
          )
        }
      }
      SectionCard(title = stringResource(R.string.settings_defaults)) {
        ChipRow(items = listOf(12, 18, 24, 30), selected = edited.defaultGifFps, label = { Text("${it} FPS") }, onSelected = { edited = edited.copy(defaultGifFps = it) })
        Spacer(Modifier.height(12.dp))
        ChipRow(items = listOf("480p", "720p", "1080p"), selected = edited.defaultGifResolution, label = { Text(it) }, onSelected = { edited = edited.copy(defaultGifResolution = it) })
        Spacer(Modifier.height(12.dp))
        ChipRow(items = Mp4Quality.entries.toList(), selected = edited.defaultMp4Quality, label = { Text(mp4QualityLabel(it)) }, onSelected = { edited = edited.copy(defaultMp4Quality = it) })
        Spacer(Modifier.height(12.dp))
        ChipRow(items = CropRatio.entries.toList(), selected = edited.defaultCropRatio, label = { Text(it.label) }, onSelected = { edited = edited.copy(defaultCropRatio = it) })
        Spacer(Modifier.height(12.dp))
        ToggleRow(title = stringResource(R.string.settings_mute_default), checked = edited.defaultMuteEnabled) {
          edited = edited.copy(defaultMuteEnabled = !edited.defaultMuteEnabled)
        }
      }
      SectionCard(title = stringResource(R.string.settings_storage)) {
        Text(stringResource(R.string.settings_storage_hint), color = ClipyMuted)
        Spacer(Modifier.height(12.dp))
        ChipRow(items = SaveBehavior.entries.toList(), selected = edited.saveBehavior, label = { Text(saveBehaviorLabel(it)) }, onSelected = { edited = edited.copy(saveBehavior = it) })
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.settings_uri_guidance), color = ClipyMuted)
      }
      SectionCard(title = stringResource(R.string.settings_performance)) {
        Text(stringResource(R.string.settings_performance_hint), color = ClipyMuted)
        Spacer(Modifier.height(12.dp))
        ChipRow(items = listOf(128, 256, 512), selected = cacheLimitMb, label = { Text(stringResource(R.string.settings_cache_limit_mb, it)) }, onSelected = { cacheLimitMb = it })
      }
      SectionCard(title = stringResource(R.string.settings_about)) {
        Text(stringResource(R.string.settings_about_body), color = ClipyMuted)
        Spacer(Modifier.height(8.dp))
        Text(settingsVersionLabel(), color = ClipyMuted, style = MaterialTheme.typography.bodyMedium)
      }
      SectionCard(title = stringResource(R.string.settings_clear_history)) {
        Text(stringResource(R.string.settings_clear_history_body), color = ClipyMuted)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { confirmClearHistory = true }, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(R.string.settings_clear_history_action))
        }
      }
      Button(
        onClick = { onSave(edited); onBack() },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary),
      ) {
        Text(stringResource(R.string.settings_save))
      }
      OutlinedButton(onClick = { confirmExit = true }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.settings_exit))
      }
    }
  }

  if (confirmExit) {
    AlertDialog(
      onDismissRequest = { confirmExit = false },
      confirmButton = { TextButton(onClick = onExit) { Text(stringResource(R.string.nav_exit)) } },
      dismissButton = { TextButton(onClick = { confirmExit = false }) { Text(stringResource(R.string.dialog_cancel)) } },
      title = { Text(stringResource(R.string.dialog_close_title)) },
      text = { Text(stringResource(R.string.dialog_close_body)) },
    )
  }

  if (confirmClearHistory) {
    AlertDialog(
      onDismissRequest = { confirmClearHistory = false },
      confirmButton = {
        TextButton(
          onClick = {
            onClearHistory()
            confirmClearHistory = false
          },
        ) { Text(stringResource(R.string.settings_clear_history_action)) }
      },
      dismissButton = { TextButton(onClick = { confirmClearHistory = false }) { Text(stringResource(R.string.dialog_cancel)) } },
      title = { Text(stringResource(R.string.settings_clear_history)) },
      text = { Text(stringResource(R.string.settings_clear_history_confirm)) },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageScreen(selectedLanguage: String, onBack: () -> Unit, onApply: (AppLanguage) -> Unit) {
  var language by rememberSaveable { mutableStateOf(AppLanguage.entries.first { it.code == selectedLanguage }) }

  Scaffold(
    containerColor = ClipyBackground,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.intro_language_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ClipyBackground),
      )
    },
    bottomBar = {
      Surface(color = ClipySurfaceVariant(), tonalElevation = 4.dp) {
        Button(
          onClick = { onApply(language) },
          modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding().height(56.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary),
        ) {
          Text(stringResource(R.string.language_apply))
        }
      }
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      PremiumCard {
        Text(stringResource(R.string.language_screen_body), color = ClipyMuted)
      }
      AppLanguage.entries.forEach { option ->
        LanguageCard(language = option, selected = language == option, onClick = { language = option })
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(state: AppSnapshot, onBack: () -> Unit, onClearHistory: () -> Unit, onReuse: (Long) -> Unit) {
  var filter by rememberSaveable { mutableStateOf(HistoryFilter.All) }
  var confirmClearHistory by rememberSaveable { mutableStateOf(false) }
  val filteredHistory = remember(state.history, filter) {
    when (filter) {
      HistoryFilter.All -> state.history
      HistoryFilter.Gif -> state.history.filter { it.formatLabel.equals("GIF", ignoreCase = true) }
      HistoryFilter.Mp4 -> state.history.filter { it.formatLabel.equals("MP4", ignoreCase = true) }
    }
  }

  Scaffold(
    containerColor = ClipyBackground,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.history_title)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back)) } },
        actions = {
          if (state.history.isNotEmpty()) {
            IconButton(onClick = { confirmClearHistory = true }) {
              Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.settings_clear_history_action))
            }
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ClipyBackground),
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (state.history.isNotEmpty()) {
        ChipRow(
          items = HistoryFilter.entries.toList(),
          selected = filter,
          label = { Text(historyFilterLabel(it)) },
          onSelected = { filter = it },
        )
      }
      if (filteredHistory.isEmpty()) {
        PremiumCard {
          Text(
            stringResource(if (state.history.isEmpty()) R.string.history_empty_title else R.string.history_filter_empty_title),
            style = MaterialTheme.typography.titleLarge,
          )
          Spacer(Modifier.height(8.dp))
          Text(
            stringResource(if (state.history.isEmpty()) R.string.history_empty_body else R.string.history_filter_empty_body),
            color = ClipyMuted,
          )
        }
      } else {
        filteredHistory.forEach { item ->
          HistoryItemCard(item = item, onReuse = { onReuse(item.id) })
        }
      }
    }
  }

  if (confirmClearHistory) {
    AlertDialog(
      onDismissRequest = { confirmClearHistory = false },
      confirmButton = {
        TextButton(
          onClick = {
            onClearHistory()
            confirmClearHistory = false
          },
        ) { Text(stringResource(R.string.settings_clear_history_action)) }
      },
      dismissButton = { TextButton(onClick = { confirmClearHistory = false }) { Text(stringResource(R.string.dialog_cancel)) } },
      title = { Text(stringResource(R.string.settings_clear_history)) },
      text = { Text(stringResource(R.string.settings_clear_history_confirm)) },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportScreen(state: AppSnapshot, onBack: () -> Unit, onCancel: () -> Unit) {
  val context = LocalContext.current
  val job = state.exportJobState
  val latestExport = latestExportRecord(state)
  val outputUri = job.outputUri ?: latestExport?.outputUri.orEmpty()
  val saveBehavior = saveBehaviorLabel(state.preferences.saveBehavior)
  var sharedOutputUri by rememberSaveable { mutableStateOf<String?>(null) }

  LaunchedEffect(job.status, outputUri, state.preferences.saveBehavior) {
    if (
      job.status == "Success" &&
      state.preferences.saveBehavior == SaveBehavior.ShareFirst &&
      outputUri.isNotBlank() &&
      sharedOutputUri != outputUri
    ) {
      sharedOutputUri = outputUri
      shareUri(context, Uri.parse(outputUri), state.draft.exportFormat.mimeType())
    }
  }

  Scaffold(
    containerColor = ClipyBackground,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.export_title)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back)) } },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ClipyBackground),
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Text(state.draft.outputName, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(exportSummary(state), color = ClipyMuted)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.export_save_behavior, saveBehavior), color = ClipyMuted)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.export_processing_note), color = ClipyMuted)
        Spacer(Modifier.height(18.dp))
        LinearProgressIndicator(
          progress = { job.progressPercent / 100f },
          modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
          color = ClipyPrimary,
          trackColor = ClipySurfaceVariant(),
        )
        Spacer(Modifier.height(12.dp))
        Text("${job.progressPercent}%", style = MaterialTheme.typography.titleLarge)
        Text(job.currentStep, color = ClipyMuted)
        Spacer(Modifier.height(20.dp))
        when {
          job.status == "Blocked" -> {
            Text(job.errorMessage ?: stringResource(R.string.export_source_missing), color = MaterialTheme.colorScheme.error)
          }
          job.status == "Success" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              AssistChip(
                 onClick = {},
                 label = { Text(stringResource(R.string.export_saved_locally)) },
                 leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(ClipySuccess)) },
               )
                AssistChip(
                  onClick = { shareUri(context, Uri.parse(outputUri), state.draft.exportFormat.mimeType()) },
                  label = { Text(stringResource(R.string.share)) },
                )
                AssistChip(
                  onClick = { openUri(context, Uri.parse(outputUri), state.draft.exportFormat.mimeType()) },
                  label = { Text(stringResource(R.string.open)) },
                )
             }
           }
           job.isCancellable -> {
             OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.export_cancel))
              }
            }
            job.status == "Cancelled" -> {
              Text(stringResource(R.string.export_cancelled_body), color = ClipyMuted)
            }
          }
      }
    }
  }
}

@Composable
private fun HistoryItemCard(item: ExportRecordUi, onReuse: () -> Unit, showReuseAction: Boolean = true) {
  val context = LocalContext.current
  PremiumCard {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Column(modifier = Modifier.weight(1f)) {
        Text(item.outputName, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(item.detailLabel, color = ClipyMuted)
        Text(item.timestampLabel, color = ClipyMuted)
      }
      AssistChip(onClick = {}, label = { Text(item.formatLabel) })
    }
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedButton(onClick = { shareUri(context, Uri.parse(item.outputUri), item.formatLabel.exportMimeType()) }, modifier = Modifier.weight(1f)) {
        Icon(Icons.Rounded.Share, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.share))
      }
      OutlinedButton(onClick = { openUri(context, Uri.parse(item.outputUri), item.formatLabel.exportMimeType()) }, modifier = Modifier.weight(1f)) {
         Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
         Spacer(Modifier.width(8.dp))
         Text(stringResource(R.string.open))
       }
    }
    if (showReuseAction) {
      Spacer(Modifier.height(10.dp))
      Button(onClick = onReuse, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary)) {
        Text(stringResource(R.string.history_reuse))
      }
    }
  }
}

@Composable
private fun PremiumCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = ClipySurfaceVariant()),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), content = content)
  }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
  PremiumCard {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(14.dp))
    content()
  }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onToggle: () -> Unit) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text(title)
    Switch(checked = checked, onCheckedChange = { onToggle() })
  }
}

@Composable
private fun TimelineEditor(
  sourceUri: String,
  timeline: TimelineSnapshot,
  onTrimStartChange: (Long) -> Unit,
  onTrimEndChange: (Long) -> Unit,
  onPlayheadChange: (Long) -> Unit,
  onZoomChange: (Float) -> Unit,
  onInteractionChange: (Boolean) -> Unit,
  onPendingSeekChange: (Long?) -> Unit,
  onVisibleWindowChange: (Long, Long) -> Unit,
) {
  val density = LocalDensity.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()
  var viewportWidthPx by remember { mutableStateOf(with(density) { 320.dp.roundToPx() }) }
  var activeDragTarget by remember { mutableStateOf<TimelineDragTarget?>(null) }
  var lastPreviewDispatchAt by remember { mutableStateOf(0L) }
  var lastPreviewTargetMs by remember(sourceUri) { mutableStateOf(Long.MIN_VALUE) }
  var pendingSettleSeekMs by remember(sourceUri) { mutableStateOf<Long?>(null) }
  var visibleWindow by remember { mutableStateOf(TimelineVisibleWindow(0, 0)) }
  var timelineBitmaps by remember(sourceUri, timelineFramesKey(sourceUri, timeline)) { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
  var gestureAnchor by remember(timeline) {
    mutableStateOf(
      TimelineGestureAnchor(
        trimStartMs = timeline.trimStartMs,
        trimEndMs = timeline.trimEndMs,
        playheadMs = timeline.playheadMs,
      ),
    )
  }
  val cellWidth = (44f + ((timeline.zoom - 1f).coerceAtLeast(0f) * 10f)).dp.coerceIn(44.dp, 94.dp)
  val cellWidthPx = with(density) { cellWidth.toPx() }.coerceAtLeast(1f)
  val handleTouchWidth = 48.dp
  val handleVisualWidth = 14.dp
  val duration = timeline.durationMs.coerceAtLeast(1L)
  val frameCount = remember(duration, timeline.zoom) { timelineStripFrameCount(duration, timeline.zoom) }
  val timelineFrames = remember(frameCount, duration) { buildTimelineStripCells(duration, frameCount) }
  val trackWidthPx = remember(frameCount, cellWidthPx) { frameCount * cellWidthPx }
  val edgePadding = remember(viewportWidthPx) { (viewportWidthPx / 2f).roundToInt().coerceAtLeast(0) }
  val prefetchRange = remember(visibleWindow, frameCount) {
    timelinePrefetchRange(visibleWindow.firstVisibleIndex, visibleWindow.lastVisibleIndex, frameCount, preloadCount = 6)
  }
  val frameStep = remember(duration) { timelineFrameStepMs(duration) }
  val isUserInteracting = activeDragTarget != null || listState.isScrollInProgress
  val visibleWindowMs = remember(visibleWindow, frameCount, duration) {
    timelineVisibleWindowMs(
      visibleStartIndex = visibleWindow.firstVisibleIndex,
      visibleEndIndex = visibleWindow.lastVisibleIndex,
      frameCount = frameCount,
      durationMs = duration,
    )
  }

  DisposableEffect(activeDragTarget, listState.isScrollInProgress) {
    val interacting = activeDragTarget != null || listState.isScrollInProgress
    onInteractionChange(interacting)
    onDispose {
      onInteractionChange(false)
      onPendingSeekChange(null)
    }
  }

  LaunchedEffect(visibleWindowMs) {
    onVisibleWindowChange(visibleWindowMs.first, visibleWindowMs.last)
  }

  LaunchedEffect(timeline.playheadMs, frameCount, viewportWidthPx, activeDragTarget) {
    if (activeDragTarget != null || listState.isScrollInProgress || frameCount <= 0) return@LaunchedEffect
    val targetScrollPx = timelineMsToTrackPx(timeline.playheadMs, duration, trackWidthPx)
    scrollTimelineTo(listState, targetScrollPx, cellWidthPx)
  }

  LaunchedEffect(listState, activeDragTarget, frameCount, trackWidthPx, duration) {
    snapshotFlow {
      Triple(currentTimelineScrollPx(listState, cellWidthPx), listState.isScrollInProgress, activeDragTarget)
    }
      .distinctUntilChanged()
      .collect { (scrollPx, isScrolling, dragTarget) ->
        if (frameCount <= 0) return@collect
        val targetMs = snapTimelineMs(timelineTrackPxToMs(scrollPx, duration, trackWidthPx)).coerceIn(timeline.trimStartMs, timeline.trimEndMs)
        val now = System.currentTimeMillis()
        val shouldDispatch = shouldDispatchTimelinePreviewSeek(
          targetMs = targetMs,
          lastDispatchedMs = lastPreviewTargetMs,
          isInteracting = isScrolling || dragTarget != null,
          elapsedSinceLastDispatchMs = now - lastPreviewDispatchAt,
          frameStepMs = frameStep,
          throttleMs = TIMELINE_PREVIEW_SEEK_THROTTLE_MS,
        )
        onPendingSeekChange(if (isScrolling || dragTarget != null) targetMs else null)
        pendingSettleSeekMs = targetMs
        if (shouldDispatch && targetMs != timeline.playheadMs) {
          lastPreviewDispatchAt = now
          lastPreviewTargetMs = targetMs
          onPlayheadChange(targetMs)
        }
      }
  }

  LaunchedEffect(isUserInteracting, pendingSettleSeekMs, timeline.trimStartMs, timeline.trimEndMs) {
    if (isUserInteracting) return@LaunchedEffect
    val settleTarget = pendingSettleSeekMs?.coerceIn(timeline.trimStartMs, timeline.trimEndMs) ?: return@LaunchedEffect
    delay(TIMELINE_SETTLE_DELAY_MS)
    if (!listState.isScrollInProgress && activeDragTarget == null && settleTarget != timeline.playheadMs) {
      lastPreviewTargetMs = settleTarget
      lastPreviewDispatchAt = System.currentTimeMillis()
      onPlayheadChange(settleTarget)
    }
  }

  LaunchedEffect(listState, frameCount) {
    snapshotFlow {
      val layoutInfo = listState.layoutInfo
      val visibleItems = layoutInfo.visibleItemsInfo
      if (visibleItems.isEmpty()) TimelineVisibleWindow(0, 0)
      else TimelineVisibleWindow(
        firstVisibleIndex = visibleItems.first().index.coerceAtLeast(0),
        lastVisibleIndex = visibleItems.last().index.coerceAtLeast(0),
      )
    }
      .distinctUntilChanged()
      .collect { window -> visibleWindow = window }
  }

  LaunchedEffect(sourceUri, prefetchRange, frameCount) {
    if (sourceUri.isBlank() || prefetchRange.isEmpty()) {
      timelineBitmaps = emptyMap()
      return@LaunchedEffect
    }
    val visibleKeys = prefetchRange.mapNotNull { index -> timelineFrames.getOrNull(index)?.cacheKey }.toSet()
    val updated = timelineBitmaps.filterKeys { it in visibleKeys }.toMutableMap()
    val missingFrames = prefetchRange.mapNotNull { index ->
      val frame = timelineFrames.getOrNull(index) ?: return@mapNotNull null
      val cacheKey = "$sourceUri@${frame.captureTimeMs}"
      val cached = timelineThumbnailCache.get(cacheKey)
      when {
        cached != null -> {
          updated[frame.cacheKey] = cached
          null
        }
        else -> {
          synchronized(timelineThumbnailRequests) {
            if (!timelineThumbnailRequests.add(cacheKey)) return@mapNotNull null
          }
          frame to cacheKey
        }
      }
    }
    val generatedFrames = withContext(Dispatchers.IO) {
      if (missingFrames.isEmpty()) return@withContext emptyList<Pair<TimelineStripCell, Bitmap>>()
      val retriever = MediaMetadataRetriever()
      try {
        retriever.setDataSource(context, Uri.parse(sourceUri))
        missingFrames.mapNotNull { (frame, cacheKey) ->
          val bitmap = runCatching {
            retriever.getFrameAtTime(frame.captureTimeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
          }.getOrNull()?.let { rawBitmap ->
            Bitmap.createScaledBitmap(rawBitmap, 128, 72, true).also {
              if (it != rawBitmap) rawBitmap.recycle()
            }
          }
          if (bitmap != null) frame to bitmap else null
        }
      } finally {
        missingFrames.forEach { (_, cacheKey) ->
          synchronized(timelineThumbnailRequests) {
            timelineThumbnailRequests.remove(cacheKey)
          }
        }
        runCatching { retriever.release() }
      }
    }
    generatedFrames.forEach { (frame, bitmap) ->
      val cacheKey = "$sourceUri@${frame.captureTimeMs}"
      timelineThumbnailCache.put(cacheKey, bitmap)
      updated[frame.cacheKey] = bitmap
    }
    timelineBitmaps = updated
  }

  val currentScrollPx by remember(listState, cellWidthPx) {
    derivedStateOf { currentTimelineScrollPx(listState, cellWidthPx) }
  }
  val startOffsetPx by remember(timeline.trimStartMs, duration, trackWidthPx, currentScrollPx, viewportWidthPx) {
    derivedStateOf { (viewportWidthPx / 2f) + timelineMsToTrackPx(timeline.trimStartMs, duration, trackWidthPx) - currentScrollPx }
  }
  val endOffsetPx by remember(timeline.trimEndMs, duration, trackWidthPx, currentScrollPx, viewportWidthPx) {
    derivedStateOf { (viewportWidthPx / 2f) + timelineMsToTrackPx(timeline.trimEndMs, duration, trackWidthPx) - currentScrollPx }
  }
  val activeRangeWidthPx by remember(startOffsetPx, endOffsetPx) {
    derivedStateOf { (endOffsetPx - startOffsetPx).coerceAtLeast(0f) }
  }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(196.dp)
        .onSizeChanged { viewportWidthPx = it.width }
        .clip(RoundedCornerShape(20.dp))
        .background(Color(0xFF0B1017))
        .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp))
        .pointerInput(timeline.zoom) {
          detectTransformGestures { _, _, zoom, _ ->
            onZoomChange((timeline.zoom * zoom).coerceIn(1f, 6f))
          }
        }
        .pointerInput(timeline, currentScrollPx, viewportWidthPx) {
          detectTapGestures { offset ->
            val targetTrackPx = (currentScrollPx + offset.x - (viewportWidthPx / 2f)).coerceIn(0f, trackWidthPx)
            val targetMs = snapTimelineMs(timelineTrackPxToMs(targetTrackPx, duration, trackWidthPx)).coerceIn(timeline.trimStartMs, timeline.trimEndMs)
            onPlayheadChange(targetMs)
            scope.launch { scrollTimelineTo(listState, targetTrackPx, cellWidthPx) }
          }
        },
    ) {
      LazyRow(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .padding(vertical = 18.dp),
        userScrollEnabled = activeDragTarget == null,
        contentPadding = PaddingValues(horizontal = with(density) { edgePadding.toDp() }),
      ) {
        items(
          items = timelineFrames,
          key = { it.cacheKey },
        ) { frame ->
          TimelineThumbnail(
            thumbnail = timelineBitmaps[frame.cacheKey],
            modifier = Modifier
              .width(cellWidth)
              .fillMaxHeight(),
          )
        }
      }
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .width(with(density) { startOffsetPx.coerceIn(0f, viewportWidthPx.toFloat()).toDp() })
          .background(Color.Black.copy(alpha = 0.54f)),
      )
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .width(with(density) { activeRangeWidthPx.toDp() })
          .offset { IntOffset(startOffsetPx.roundToInt(), 0) }
          .clip(RoundedCornerShape(16.dp))
          .background(
            Brush.verticalGradient(
              listOf(
                ClipyAccent.copy(alpha = if (isUserInteracting) 0.28f else 0.2f),
                ClipyAccent.copy(alpha = if (isUserInteracting) 0.18f else 0.12f),
              ),
            ),
          )
          .border(1.dp, ClipyAccent.copy(alpha = 0.95f), RoundedCornerShape(16.dp)),
      )
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .width(with(density) { (viewportWidthPx.toFloat() - endOffsetPx).coerceIn(0f, viewportWidthPx.toFloat()).toDp() })
          .offset { IntOffset(endOffsetPx.roundToInt(), 0) }
          .background(Color.Black.copy(alpha = 0.54f)),
      )
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .width(handleTouchWidth)
          .offset { IntOffset((startOffsetPx - with(density) { handleTouchWidth.toPx() / 2f }).roundToInt(), 0) }
          .pointerInput(timeline, currentScrollPx) {
            detectDragGestures(
              onDragStart = {
                activeDragTarget = TimelineDragTarget.Start
                gestureAnchor = TimelineGestureAnchor(
                  trimStartMs = timeline.trimStartMs,
                  trimEndMs = timeline.trimEndMs,
                  playheadMs = timeline.playheadMs,
                  anchorTrackOffsetPx = timelineMsToTrackPx(timeline.trimStartMs, duration, trackWidthPx),
                )
              },
              onDragEnd = {
                activeDragTarget = null
                pendingSettleSeekMs = timeline.playheadMs.coerceIn(timeline.trimStartMs, timeline.trimEndMs)
              },
              onDragCancel = { activeDragTarget = null },
            ) { change, dragAmount ->
              change.consume()
              val nextTrackOffset = (gestureAnchor.anchorTrackOffsetPx + dragAmount.x).coerceIn(
                0f,
                timelineMsToTrackPx(gestureAnchor.trimEndMs - MIN_TRIM_GAP_MS, duration, trackWidthPx),
              )
              gestureAnchor = gestureAnchor.copy(anchorTrackOffsetPx = nextTrackOffset)
              val target = boundedTrimStartMs(nextTrackOffset, duration, trackWidthPx, timeline.trimEndMs, MIN_TRIM_GAP_MS)
              onTrimStartChange(target)
              if (timeline.playheadMs < target) onPlayheadChange(target)
            }
          },
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier = Modifier
            .fillMaxHeight(0.74f)
            .width(handleVisualWidth)
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.verticalGradient(listOf(ClipyPrimary, if (activeDragTarget == TimelineDragTarget.Start) ClipyPrimary else ClipyAccent)))
        )
        Box(
          modifier = Modifier
            .width(4.dp)
            .fillMaxHeight(0.22f)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.65f)),
        )
      }
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .width(handleTouchWidth)
          .offset { IntOffset((endOffsetPx - with(density) { handleTouchWidth.toPx() / 2f }).roundToInt(), 0) }
          .pointerInput(timeline, currentScrollPx) {
            detectDragGestures(
              onDragStart = {
                activeDragTarget = TimelineDragTarget.End
                gestureAnchor = TimelineGestureAnchor(
                  trimStartMs = timeline.trimStartMs,
                  trimEndMs = timeline.trimEndMs,
                  playheadMs = timeline.playheadMs,
                  anchorTrackOffsetPx = timelineMsToTrackPx(timeline.trimEndMs, duration, trackWidthPx),
                )
              },
              onDragEnd = {
                activeDragTarget = null
                pendingSettleSeekMs = timeline.playheadMs.coerceIn(timeline.trimStartMs, timeline.trimEndMs)
              },
              onDragCancel = { activeDragTarget = null },
            ) { change, dragAmount ->
              change.consume()
              val nextTrackOffset = (gestureAnchor.anchorTrackOffsetPx + dragAmount.x).coerceIn(
                timelineMsToTrackPx(gestureAnchor.trimStartMs + MIN_TRIM_GAP_MS, duration, trackWidthPx),
                trackWidthPx,
              )
              gestureAnchor = gestureAnchor.copy(anchorTrackOffsetPx = nextTrackOffset)
              val target = boundedTrimEndMs(nextTrackOffset, duration, trackWidthPx, timeline.trimStartMs, MIN_TRIM_GAP_MS)
              onTrimEndChange(target)
              if (timeline.playheadMs > target) onPlayheadChange(target)
            }
          },
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier = Modifier
            .fillMaxHeight(0.74f)
            .width(handleVisualWidth)
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.verticalGradient(listOf(ClipyPrimary, if (activeDragTarget == TimelineDragTarget.End) ClipyPrimary else ClipyAccent)))
        )
        Box(
          modifier = Modifier
            .width(4.dp)
            .fillMaxHeight(0.22f)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.65f)),
        )
      }
      Box(
        modifier = Modifier
          .align(Alignment.Center)
          .fillMaxHeight(),
        contentAlignment = Alignment.Center,
        ) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .width(if (isUserInteracting) 4.dp else 2.dp)
            .background(ClipyOnDark),
        )
        Box(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 8.dp)
            .width(16.dp)
            .height(18.dp)
            .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
            .background(if (isUserInteracting) ClipyPrimary else ClipyAccent),
        )
      }
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 10.dp)
          .clip(RoundedCornerShape(999.dp))
          .background(Color.Black.copy(alpha = 0.34f))
          .padding(horizontal = 10.dp, vertical = 6.dp),
      ) {
        Text(
          text = formatDurationMs(timeline.playheadMs.coerceIn(timeline.trimStartMs, timeline.trimEndMs)),
          color = ClipyOnDark,
          style = MaterialTheme.typography.labelMedium,
        )
      }
      if (timelineBitmaps.isEmpty() && sourceUri.isNotBlank()) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 10.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.34f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
          Text(stringResource(R.string.editor_timeline_loading), color = ClipyOnDark, style = MaterialTheme.typography.labelSmall)
        }
      }
    }
    Text(
      text = stringResource(R.string.editor_timeline_hint),
      color = ClipyMuted,
      fontSize = 12.sp,
    )
  }
}

@Composable
private fun TimelineThumbnail(thumbnail: Bitmap?, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .padding(horizontal = 1.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF1A212C)),
    contentAlignment = Alignment.Center,
  ) {
    if (thumbnail != null) {
      Image(
        bitmap = thumbnail.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alpha = 0.96f,
      )
    } else {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                Color.White.copy(alpha = 0.02f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.08f),
              ),
            ),
          ),
      )
    }
  }
}

@Composable
private fun EditorOverlayBadge(modifier: Modifier = Modifier, title: String, subtitle: String) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(Color.Black.copy(alpha = 0.42f))
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(title, color = ClipyOnDark, style = MaterialTheme.typography.titleSmall)
    Text(subtitle, color = ClipyOnDark.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun EditorStatusPill(modifier: Modifier = Modifier, isLive: Boolean) {
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(Color.Black.copy(alpha = 0.36f))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(if (isLive) ClipyAccent else ClipyMuted))
    Text(
      text = if (isLive) stringResource(R.string.editor_status_live) else stringResource(R.string.editor_status_idle),
      color = ClipyOnDark,
      style = MaterialTheme.typography.labelMedium,
    )
  }
}

@Composable
private fun TransportIconButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  highlighted: Boolean = false,
) {
  Surface(
    modifier = Modifier.size(if (highlighted) 44.dp else 38.dp),
    shape = CircleShape,
    color = if (highlighted) ClipyAccent else Color(0xFF1C2230),
  ) {
    IconButton(onClick = onClick) {
      Icon(icon, contentDescription = contentDescription, tint = if (highlighted) Color.White else ClipyOnDark)
    }
  }
}

@Composable
private fun EditorControlTabs(selected: EditorControlTab, onSelected: (EditorControlTab) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    EditorControlTab.entries.forEach { tab ->
      val label = when (tab) {
        EditorControlTab.Frame -> stringResource(R.string.section_frame)
        EditorControlTab.Motion -> stringResource(R.string.section_motion)
        EditorControlTab.Output -> stringResource(R.string.section_output)
      }
      FilterChip(
        selected = tab == selected,
        onClick = { onSelected(tab) },
        label = { Text(label) },
      )
    }
  }
}

@Composable
private fun EditorStatChip(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    color = Color(0xFF151B24),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(label, color = ClipyMuted, style = MaterialTheme.typography.labelSmall)
      Text(value, color = ClipyOnDark, style = MaterialTheme.typography.titleSmall)
    }
  }
}

@Composable
private fun CompactToggleCard(
  title: String,
  checked: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  FilterChip(
    selected = checked,
    onClick = onToggle,
    modifier = modifier,
    label = {
      Text(
        text = title,
        maxLines = 1,
      )
    },
  )
}

@Composable
private fun EditorControlGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(title, color = ClipyMuted, style = MaterialTheme.typography.labelLarge)
    content()
  }
}

private fun formatTimelineWindow(startMs: Long, endMs: Long): String =
  "${formatDurationMs(startMs)} - ${formatDurationMs(endMs)}"

private fun formatDurationMs(durationMs: Long): String {
  val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L).toInt()
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}

private fun timelineFramesKey(sourceUri: String, timeline: TimelineSnapshot): String =
  "$sourceUri:${timeline.durationMs}:${timeline.zoom}"

private fun buildTimelineStripCells(durationMs: Long, frameCount: Int): List<TimelineStripCell> {
  if (frameCount <= 0) return emptyList()
  if (frameCount == 1) return listOf(TimelineStripCell(index = 0, captureTimeMs = 0L))
  val step = durationMs.coerceAtLeast(1L) / (frameCount - 1).toDouble()
  return List(frameCount) { index ->
    TimelineStripCell(
      index = index,
      captureTimeMs = (step * index).roundToLong().coerceIn(0L, durationMs),
    )
  }
}

private fun currentTimelineScrollPx(state: LazyListState, cellWidthPx: Float): Float =
  ((state.firstVisibleItemIndex * cellWidthPx) + state.firstVisibleItemScrollOffset).coerceAtLeast(0f)

private suspend fun scrollTimelineTo(state: LazyListState, targetScrollPx: Float, cellWidthPx: Float) {
  if (cellWidthPx <= 0f) return
  val boundedTarget = targetScrollPx.coerceAtLeast(0f)
  val targetIndex = floor(boundedTarget / cellWidthPx).toInt().coerceAtLeast(0)
  val targetOffset = (boundedTarget - (targetIndex * cellWidthPx)).roundToInt().coerceAtLeast(0)
  if (state.firstVisibleItemIndex != targetIndex || abs(state.firstVisibleItemScrollOffset - targetOffset) > 2) {
    state.scrollToItem(targetIndex, targetOffset)
  }
}

@Composable
private fun <T> ChipRow(items: List<T>, selected: T, label: @Composable (T) -> Unit, onSelected: (T) -> Unit) {
  Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items.forEach { item ->
      FilterChip(selected = item == selected, onClick = { onSelected(item) }, label = { label(item) })
    }
  }
}

private enum class HistoryFilter {
  All,
  Gif,
  Mp4,
}

@Composable
private fun LanguageCard(language: AppLanguage, selected: Boolean, onClick: () -> Unit) {
  val borderColor = if (selected) ClipyPrimary else Color.Transparent
  PremiumCard(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, borderColor, RoundedCornerShape(24.dp))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
      ),
  ) {
    Text(languageLabel(language), style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(6.dp))
    Text(languageHelper(language), color = ClipyMuted)
  }
}

@Composable
private fun ClipySurfaceVariant(): Color = MaterialTheme.colorScheme.surfaceVariant

private fun exportSummary(state: AppSnapshot): String {
  val draft = state.draft
  if (draft.sourceUri.isBlank()) {
    return ""
  }
  return if (draft.exportFormat == ExportFormat.Gif) {
    "GIF • ${draft.cropRatio.label} • ${draft.gifFps} FPS • ${draft.gifResolution}"
  } else {
    "MP4 • ${draft.cropRatio.label} • ${mp4QualitySummaryLabel(draft.mp4Quality)}"
  }
}

private fun latestExportRecord(state: AppSnapshot): ExportRecordUi? =
  state.history.firstOrNull { it.outputName == state.draft.outputName } ?: state.history.firstOrNull()

@Composable
private fun languageLabel(language: AppLanguage): String =
  stringResource(if (language == AppLanguage.English) R.string.language_english else R.string.language_vietnamese)

@Composable
private fun languageHelper(language: AppLanguage): String =
  stringResource(if (language == AppLanguage.English) R.string.language_english_helper else R.string.language_vietnamese_helper)

@Composable
private fun watermarkPositionLabel(position: WatermarkPosition): String =
  stringResource(
    when (position) {
      WatermarkPosition.TopLeft -> R.string.watermark_top_left
      WatermarkPosition.TopRight -> R.string.watermark_top_right
      WatermarkPosition.BottomLeft -> R.string.watermark_bottom_left
      WatermarkPosition.BottomRight -> R.string.watermark_bottom_right
      WatermarkPosition.Center -> R.string.watermark_center
    },
  )

@Composable
private fun mp4QualityLabel(quality: Mp4Quality): String =
  stringResource(
    when (quality) {
      Mp4Quality.Fast -> R.string.mp4_quality_fast
      Mp4Quality.Balanced -> R.string.mp4_quality_balanced
      Mp4Quality.Crisp -> R.string.mp4_quality_crisp
    },
  )

@Composable
private fun saveBehaviorLabel(saveBehavior: SaveBehavior): String =
  stringResource(
    when (saveBehavior) {
      SaveBehavior.AppFolder -> R.string.save_behavior_app_folder
      SaveBehavior.PromptEachTime -> R.string.save_behavior_prompt
      SaveBehavior.ShareFirst -> R.string.save_behavior_share_first
    },
  )

@Composable
private fun historyFilterLabel(filter: HistoryFilter): String =
  stringResource(
    when (filter) {
      HistoryFilter.All -> R.string.history_filter_all
      HistoryFilter.Gif -> R.string.history_filter_gif
      HistoryFilter.Mp4 -> R.string.history_filter_mp4
    },
  )

@Composable
private fun settingsVersionLabel(): String {
  val context = LocalContext.current
  val versionName = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
  }.getOrDefault("1.0") ?: "1.0"
  return stringResource(R.string.settings_version, versionName)
}

private fun mp4QualitySummaryLabel(quality: Mp4Quality): String =
  when (quality) {
    Mp4Quality.Fast -> "Fast 720p"
    Mp4Quality.Balanced -> "Balanced 1080p"
    Mp4Quality.Crisp -> "Crisp source"
  }

private fun shareUri(context: android.content.Context, uri: Uri, mimeType: String) {
  if (uri.toString().isBlank()) return
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = mimeType
    putExtra(Intent.EXTRA_STREAM, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  try {
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_export)))
  } catch (_: ActivityNotFoundException) {
    Toast.makeText(context, context.getString(R.string.share_error), Toast.LENGTH_SHORT).show()
  }
}

private fun openUri(context: android.content.Context, uri: Uri, mimeType: String) {
  if (uri.toString().isBlank()) return
  val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, mimeType)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  try {
    context.startActivity(intent)
  } catch (_: ActivityNotFoundException) {
    Toast.makeText(context, context.getString(R.string.open_error), Toast.LENGTH_SHORT).show()
  }
}
