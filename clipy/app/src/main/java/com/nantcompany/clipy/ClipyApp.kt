package com.nantcompany.clipy

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.collection.LruCache
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nantcompany.clipy.data.ClipyRepository.AppSnapshot
import com.nantcompany.clipy.model.AppLanguage
import com.nantcompany.clipy.model.AudioSegmentUi
import com.nantcompany.clipy.model.buildWaveformSamples
import com.nantcompany.clipy.model.buildTimelineTicks
import com.nantcompany.clipy.model.boundedTrimEndMs
import com.nantcompany.clipy.model.boundedTrimStartMs
import com.nantcompany.clipy.model.CropRatio
import com.nantcompany.clipy.model.ExportFormat
import com.nantcompany.clipy.model.ExportRecordUi
import com.nantcompany.clipy.model.Mp4Quality
import com.nantcompany.clipy.model.SaveBehavior
import com.nantcompany.clipy.model.editorTimelineUiState
import com.nantcompany.clipy.model.shouldDispatchTimelinePreviewSeek
import com.nantcompany.clipy.model.splitAudioSegments
import com.nantcompany.clipy.model.timelineFrameStepMs
import com.nantcompany.clipy.model.timelineMsToTrackPx
import com.nantcompany.clipy.model.timelinePrefetchRange
import com.nantcompany.clipy.model.timelineStripFrameCount
import com.nantcompany.clipy.model.timelineTrackPxToMs
import com.nantcompany.clipy.model.timelineVisibleWindowMs
import com.nantcompany.clipy.model.timelineSnapshot
import com.nantcompany.clipy.model.TimelineTickUiModel
import com.nantcompany.clipy.model.UserPreferences
import com.nantcompany.clipy.model.WatermarkPosition
import com.nantcompany.clipy.model.exportMimeType
import com.nantcompany.clipy.model.mimeType
import com.nantcompany.clipy.model.snapTimelineMs
import com.nantcompany.clipy.model.TimelineSnapshot
import com.nantcompany.clipy.ui.ClipyViewModel
import com.nantcompany.clipy.ui.MediaPickerScreen
import com.nantcompany.clipy.ui.MediaTab
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
private const val MEDIA_PICKER = "media_picker"
private const val PERFORMANCE_CACHE_MB = 256
private const val MIN_TRIM_GAP_MS = 250L
private const val TIMELINE_PREVIEW_SEEK_THROTTLE_MS = 90L
private const val TIMELINE_SETTLE_DELAY_MS = 48L

internal fun shouldLoadVideoTimelineFrames(sourceUri: String, isVideoSource: Boolean): Boolean {
  return sourceUri.isNotBlank() && isVideoSource
}

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
  val visibleWindowLabel: String,
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

private enum class EditorTrack {
  Video,
  Audio,
  Text,
}

private enum class TimelineTool {
  Trim,
  Split,
  Gain,
}

private enum class EditorPrimaryTool {
  Edit,
  Audio,
  Text,
  Effects,
  Filters,
}

private data class TextClipUi(
  val id: String,
  val label: String,
  val startMs: Long,
  val endMs: Long,
)

private data class UndoRedoState(
  val undoStack: List<String> = emptyList(),
  val redoStack: List<String> = emptyList(),
) {
  val canUndo: Boolean get() = undoStack.isNotEmpty()
  val canRedo: Boolean get() = redoStack.isNotEmpty()
  val lastActionLabel: String? get() = undoStack.lastOrNull()
}

@Composable
fun ClipyApp(finishApp: () -> Unit) {
  val context = LocalContext.current
  val app = context.applicationContext as Application
  val viewModel: ClipyViewModel = viewModel(factory = ClipyViewModel.factory(app))
  val state by viewModel.appState.collectAsStateWithLifecycle()
  val pickerState by viewModel.pickerState.collectAsStateWithLifecycle()
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
      AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220)),
        exit = fadeOut(tween(140)),
      ) {
        HomeScreen(
          state = state,
          finishApp = finishApp,
          onOpenMediaPicker = {
            viewModel.openMediaPicker(MediaTab.Videos)
            navController.navigate(MEDIA_PICKER)
          },
          onOpenProject = {
            viewModel.reuseHistoryRecord(it)
            navController.navigate(EDITOR)
          },
          onOpenSettings = { navController.navigate(SETTINGS) },
        )
      }
    }
    composable(MEDIA_PICKER) {
      AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)),
        exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.98f, animationSpec = tween(140)),
      ) {
        MediaPickerScreen(
          state = pickerState,
          onBack = navController::popBackStack,
          onRequestPermissionRefresh = viewModel::refreshMediaPermissionAndContent,
          onSelectTab = viewModel::selectPickerTab,
          onSelectAlbum = viewModel::selectPickerAlbum,
          onToggleSelection = viewModel::togglePickerSelection,
          onReorderSelection = viewModel::reorderPickerSelection,
          onPreviewItem = viewModel::previewPickerItem,
          onClearValidationError = viewModel::clearContinueValidationError,
          onLoadMore = viewModel::loadMorePickerItems,
          onConfirmSelection = {
            if (viewModel.confirmPickerSelection() != null) {
              navController.navigate(EDITOR) {
                popUpTo(MEDIA_PICKER) { inclusive = true }
              }
            }
          },
        )
      }
    }
    composable(EDITOR) {
      AnimatedVisibility(
        visible = true,
        enter = slideInHorizontally(initialOffsetX = { it / 5 }, animationSpec = tween(240)) + fadeIn(tween(200)),
        exit = slideOutHorizontally(targetOffsetX = { it / 6 }, animationSpec = tween(160)) + fadeOut(tween(140)),
      ) {
        EditorScreen(
          state = state,
          onBack = navController::popBackStack,
          onOpenMediaPicker = {
            viewModel.openMediaPicker(MediaTab.Videos)
            navController.navigate(MEDIA_PICKER)
          },
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
          LanguageCard(language = option, selected = language == option, isRecommended = false, onClick = { language = option })
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
  onOpenMediaPicker: () -> Unit,
  onOpenProject: (Long) -> Unit,
  onOpenSettings: () -> Unit,
) {
  var confirmExit by rememberSaveable { mutableStateOf(false) }
  var selectedDestination by rememberSaveable { mutableStateOf(HomeDestination.Home) }
  var selectedTemplateCategory by rememberSaveable { mutableStateOf(TemplateCategory.Trending) }
  val recentExports = remember(state.history) { state.history.take(6) }

  Scaffold(
    containerColor = ClipyBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF151A24),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text("C", color = ClipyOnDark, style = MaterialTheme.typography.titleLarge)
            }
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Clipy", color = ClipyOnDark, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.home_creator_label), color = ClipyMuted, style = MaterialTheme.typography.bodySmall)
          }
        }
        IconButton(onClick = onOpenSettings) {
          Icon(Icons.Rounded.AccountCircle, contentDescription = stringResource(R.string.nav_settings), tint = ClipyOnDark)
        }
      }
    },
    bottomBar = {
      Surface(color = Color(0xF2141820)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          HomeDestination.entries.forEach { destination ->
            HomeDestinationItem(
              destination = destination,
              selected = selectedDestination == destination,
              onClick = { selectedDestination = destination },
            )
          }
        }
      }
    },
  ) { padding ->
    AnimatedContent(targetState = selectedDestination, label = "homeDestination") { destination ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(horizontal = 16.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Spacer(Modifier.height(8.dp))
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(28.dp),
          color = Color(0xFF141922),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.verticalGradient(
                  listOf(Color(0xFF1A2232), Color(0xFF12161E)),
                ),
              )
              .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              stringResource(R.string.home_title),
              color = ClipyOnDark,
              style = MaterialTheme.typography.headlineMedium,
              textAlign = TextAlign.Center,
            )
            Text(stringResource(R.string.tagline), color = ClipyMuted, textAlign = TextAlign.Center)
            Button(
              onClick = onOpenMediaPicker,
              modifier = Modifier.fillMaxWidth().height(58.dp),
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(stringResource(R.string.home_new_project))
            }
            Text(stringResource(R.string.home_new_project_hint), color = ClipyMuted, style = MaterialTheme.typography.bodySmall)
          }
        }

        if (destination == HomeDestination.Home) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(stringResource(R.string.home_recent_projects), color = ClipyOnDark, style = MaterialTheme.typography.titleLarge)
              Text(stringResource(R.string.home_recent_projects_hint), color = ClipyMuted, style = MaterialTheme.typography.bodySmall)
            }
            TimelineCompactBadge(primary = recentExports.size.toString(), secondary = stringResource(R.string.nav_history))
          }

          if (recentExports.isEmpty()) {
            PremiumCard {
              Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.titleLarge)
              Spacer(Modifier.height(8.dp))
              Text(stringResource(R.string.history_empty_body), color = ClipyMuted)
              Spacer(Modifier.height(14.dp))
              Button(
                onClick = onOpenMediaPicker,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary),
              ) {
                Text(stringResource(R.string.home_new_project))
              }
            }
          } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 4.dp)) {
              items(recentExports, key = { it.id }) { item ->
                RecentProjectCard(item = item, onClick = { onOpenProject(item.id) })
              }
            }
          }
        } else if (destination == HomeDestination.Templates) {
          TemplatesSection(selectedCategory = selectedTemplateCategory, onCategorySelected = { selectedTemplateCategory = it })
        } else {
          ProfileSection(onOpenSettings = onOpenSettings)
        }

        PremiumCard {
          Text(stringResource(R.string.home_tools), style = MaterialTheme.typography.titleLarge)
          Spacer(Modifier.height(14.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeToolShortcut(
              modifier = Modifier.weight(1f),
              icon = Icons.Rounded.FolderOpen,
              title = stringResource(R.string.home_pick_video),
              subtitle = stringResource(R.string.home_pick_hint),
              onClick = onOpenMediaPicker,
            )
            HomeToolShortcut(
              modifier = Modifier.weight(1f),
              icon = Icons.Rounded.Settings,
              title = stringResource(R.string.nav_settings),
              subtitle = stringResource(R.string.home_settings_hint),
              onClick = onOpenSettings,
            )
          }
          Spacer(Modifier.height(10.dp))
          OutlinedButton(onClick = { confirmExit = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.nav_exit))
          }
        }

        Spacer(Modifier.height(8.dp))
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

private enum class HomeDestination {
  Home,
  Templates,
  Profile,
}

private enum class TemplateCategory {
  Trending,
  Vlog,
  TikTok,
}

@Composable
private fun HomeDestinationItem(destination: HomeDestination, selected: Boolean, onClick: () -> Unit) {
  val icon = when (destination) {
    HomeDestination.Home -> Icons.Rounded.Home
    HomeDestination.Templates -> Icons.Rounded.AutoAwesomeMosaic
    HomeDestination.Profile -> Icons.Rounded.Person
  }
  val label = when (destination) {
    HomeDestination.Home -> stringResource(R.string.home_nav_home)
    HomeDestination.Templates -> stringResource(R.string.home_nav_templates)
    HomeDestination.Profile -> stringResource(R.string.home_nav_profile)
  }
  Column(
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Icon(icon, contentDescription = label, tint = if (selected) ClipyPrimary else ClipyMuted)
    Text(label, color = if (selected) ClipyOnDark else ClipyMuted, style = MaterialTheme.typography.labelMedium)
  }
}

@Composable
private fun RecentProjectCard(item: ExportRecordUi, onClick: () -> Unit) {
  Card(
    modifier = Modifier.width(220.dp).clickable(onClick = onClick),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF151A22)),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f)
          .background(Brush.linearGradient(listOf(Color(0xFF202B40), Color(0xFF141A24), Color(0xFF0E1118)))),
      ) {
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(10.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.34f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
          Text(item.detailLabel, color = ClipyOnDark, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        Column(
          modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(item.formatLabel, color = ClipyAccent, style = MaterialTheme.typography.labelMedium)
          Text(item.outputName, color = ClipyOnDark, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
      }
      Column(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(stringResource(R.string.home_recent_meta_duration, item.detailLabel), color = ClipyOnDark, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        Text(item.timestampLabel, color = ClipyMuted, style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

private data class TemplateCardModel(
  val title: String,
  val subtitle: String,
  val badge: String,
  val accent: Color,
)

private data class ProfileMenuModel(
  val title: String,
  val subtitle: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun TemplatesSection(selectedCategory: TemplateCategory, onCategorySelected: (TemplateCategory) -> Unit) {
  val templates = remember(selectedCategory) {
    when (selectedCategory) {
      TemplateCategory.Trending -> listOf(
        TemplateCardModel("Beat Cut", "Fast rhythm edits for short highlight drops", "Coming soon", ClipyPrimary),
        TemplateCardModel("Loop Flash", "Boomerang-friendly pacing for meme moments", "Coming soon", ClipySecondary),
        TemplateCardModel("Caption Pop", "Quick headline overlays for hooks and teasers", "Coming soon", ClipyAccent),
      )
      TemplateCategory.Vlog -> listOf(
        TemplateCardModel("Daily Recap", "Soft framing and quick trims for daily stories", "Coming soon", Color(0xFF38BDF8)),
        TemplateCardModel("Travel Stack", "Layered opener for scenic clips and transitions", "Coming soon", Color(0xFF22C55E)),
      )
      TemplateCategory.TikTok -> listOf(
        TemplateCardModel("Hook First", "Front-load the moment with title-safe spacing", "Coming soon", Color(0xFFF472B6)),
        TemplateCardModel("Reaction Loop", "Built for repeatable gags and boomerang energy", "Coming soon", Color(0xFFF59E0B)),
      )
    }
  }

  PremiumCard {
    Text(stringResource(R.string.templates_title), style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.templates_body), color = ClipyMuted)
    Spacer(Modifier.height(14.dp))
    ChipRow(items = TemplateCategory.entries.toList(), selected = selectedCategory, label = { Text(templateCategoryLabel(it)) }, onSelected = onCategorySelected)
  }

  templates.forEach { template ->
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF121827)),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(
              Brush.linearGradient(
                listOf(template.accent.copy(alpha = 0.42f), Color(0xFF151C2F), Color(0xFF0B1020)),
              ),
            ),
        ) {
          Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = 0.26f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = template.accent, modifier = Modifier.size(14.dp))
              Text(template.badge, color = ClipyOnDark, style = MaterialTheme.typography.labelMedium)
            }
          }
          Text(
            text = stringResource(R.string.templates_preview_label),
            modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
            color = Color.White.copy(alpha = 0.74f),
            style = MaterialTheme.typography.labelSmall,
          )
        }
        Column(
          modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(template.title, color = ClipyOnDark, style = MaterialTheme.typography.titleMedium)
          Text(template.subtitle, color = ClipyMuted, style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}

@Composable
private fun ProfileSection(onOpenSettings: () -> Unit) {
  val menuItems = remember {
    listOf(
      ProfileMenuModel("My Projects", "Jump back into local exports and reusable drafts", Icons.Rounded.FolderOpen),
      ProfileMenuModel("Settings", "Language, defaults, and storage behavior", Icons.Rounded.Settings),
      ProfileMenuModel("Language", "Adjust app copy for your creator workflow", Icons.Rounded.Language),
      ProfileMenuModel("About", "Privacy-first on-device editing for quick exports", Icons.Rounded.Info),
    )
  }

  PremiumCard {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
      Surface(shape = CircleShape, color = ClipyPrimary.copy(alpha = 0.18f), modifier = Modifier.size(68.dp)) {
        Box(contentAlignment = Alignment.Center) {
          Text("CL", color = ClipyOnDark, style = MaterialTheme.typography.titleMedium)
        }
      }
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.profile_name), color = ClipyOnDark, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.profile_email), color = ClipyMuted, style = MaterialTheme.typography.bodyMedium)
      }
    }
  }

  PremiumCard {
    menuItems.forEachIndexed { index, item ->
      ProfileMenuRow(item = item, onClick = onOpenSettings)
      if (index != menuItems.lastIndex) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
      }
    }
  }

  OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().height(54.dp)) {
    Icon(Icons.Rounded.Logout, contentDescription = null, tint = ClipyMuted)
    Spacer(Modifier.width(8.dp))
    Text(stringResource(R.string.profile_logout), color = ClipyMuted)
  }
}

@Composable
private fun ProfileMenuRow(item: ProfileMenuModel, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(shape = CircleShape, color = Color(0xFF172033)) {
      Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
        Icon(item.icon, contentDescription = null, tint = ClipyPrimary, modifier = Modifier.size(18.dp))
      }
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(item.title, color = ClipyOnDark, style = MaterialTheme.typography.titleSmall)
      Text(item.subtitle, color = ClipyMuted, style = MaterialTheme.typography.bodySmall)
    }
    Icon(Icons.Rounded.ArrowForwardIos, contentDescription = null, tint = ClipyMuted, modifier = Modifier.size(14.dp))
  }
}

@Composable
private fun HomeToolShortcut(
  modifier: Modifier = Modifier,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
) {
  Surface(
    modifier = modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
    color = Color(0xFF11161E),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(shape = CircleShape, color = Color(0x1F2563EB)) {
        Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
          Icon(icon, contentDescription = null, tint = ClipyPrimary)
        }
      }
      Text(title, color = ClipyOnDark, style = MaterialTheme.typography.titleSmall)
      Text(subtitle, color = ClipyMuted, style = MaterialTheme.typography.bodySmall)
    }
  }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun EditorScreen(
  state: AppSnapshot,
  onBack: () -> Unit,
  onOpenMediaPicker: () -> Unit,
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
  val isVideoSource = draft.sourceMediaType.equals("video", ignoreCase = true)
  val player = remember { ExoPlayer.Builder(context).build() }
  val timeline = remember(draft) { draft.timelineSnapshot() }
  var selectedControlTab by rememberSaveable { mutableStateOf(EditorControlTab.Frame) }
  var selectedPrimaryTool by rememberSaveable { mutableStateOf(EditorPrimaryTool.Edit) }
  var timelineInteracting by remember { mutableStateOf(false) }
  var visibleWindowStartMs by remember { mutableStateOf(0L) }
  var visibleWindowEndMs by remember { mutableStateOf(draft.sourceDurationMs) }
  var pendingSeekMs by remember { mutableStateOf<Long?>(null) }
  var syncedPlayerPositionMs by remember { mutableStateOf(-1L) }
  var isPlaying by remember { mutableStateOf(false) }
  var selectedTrack by rememberSaveable { mutableStateOf(EditorTrack.Video) }
  var selectedTimelineTool by rememberSaveable { mutableStateOf(TimelineTool.Trim) }
  var audioTrimStartMs by rememberSaveable(draft.sourceUri) { mutableStateOf(draft.trimStartMs) }
  var audioTrimEndMs by rememberSaveable(draft.sourceUri) { mutableStateOf(draft.trimEndMs) }
  var audioGain by rememberSaveable(draft.sourceUri) { mutableStateOf(1f) }
  var audioSegments by rememberSaveable(draft.sourceUri) {
    mutableStateOf(listOf(AudioSegmentUi(id = "seg-0", startMs = 0L, endMs = draft.sourceDurationMs.coerceAtLeast(MIN_TRIM_GAP_MS * 2))))
  }
  var textClips by rememberSaveable(draft.sourceUri) {
    mutableStateOf(
      listOf(
        TextClipUi(
          id = "text-0",
          label = context.getString(R.string.editor_text_overlay_default),
          startMs = draft.trimStartMs,
          endMs = (draft.trimStartMs + 2500L).coerceAtMost(draft.trimEndMs),
        ),
      ),
    )
  }
  var selectedTextClipId by rememberSaveable(draft.sourceUri) { mutableStateOf("text-0") }
  var selectedAudioSegmentId by rememberSaveable(draft.sourceUri) { mutableStateOf("seg-0") }
  var previewZoom by rememberSaveable { mutableStateOf(1f) }
  var previewFill by rememberSaveable { mutableStateOf(false) }
  var previewOverlayVisible by rememberSaveable { mutableStateOf(true) }
  var previewScrubberWidthPx by remember { mutableStateOf(0) }
  var toolPanelExpanded by rememberSaveable { mutableStateOf(true) }
  var volumeAmount by rememberSaveable(draft.sourceUri) { mutableStateOf(1f) }
  var fadeAmount by rememberSaveable(draft.sourceUri) { mutableStateOf(0.18f) }
  var effectIntensity by rememberSaveable(draft.sourceUri) { mutableStateOf(0.42f) }
  var filterStrength by rememberSaveable(draft.sourceUri) { mutableStateOf(0.36f) }
  var undoRedoState by rememberSaveable { mutableStateOf(UndoRedoState()) }
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
      visibleWindowLabel = formatTimelineWindow(visibleWindowStartMs, visibleWindowEndMs),
    )
  }

  LaunchedEffect(draft.sourceUri, draft.sourceDurationMs, draft.trimStartMs, draft.trimEndMs) {
    audioTrimStartMs = draft.trimStartMs.coerceIn(0L, draft.sourceDurationMs)
    audioTrimEndMs = draft.trimEndMs.coerceIn(
      audioTrimStartMs + MIN_TRIM_GAP_MS,
      draft.sourceDurationMs.coerceAtLeast(audioTrimStartMs + MIN_TRIM_GAP_MS),
    )
    audioSegments = listOf(AudioSegmentUi(id = "seg-0", startMs = 0L, endMs = draft.sourceDurationMs.coerceAtLeast(MIN_TRIM_GAP_MS * 2)))
    selectedAudioSegmentId = audioSegments.firstOrNull()?.id ?: "seg-0"
    selectedTrack = EditorTrack.Video
    selectedTimelineTool = TimelineTool.Trim
    audioGain = 1f
    textClips = listOf(
      TextClipUi(
        id = "text-0",
        label = context.getString(R.string.editor_text_overlay_default),
        startMs = draft.trimStartMs,
        endMs = (draft.trimStartMs + 2500L).coerceAtMost(draft.trimEndMs),
      ),
    )
    selectedTextClipId = textClips.firstOrNull()?.id ?: "text-0"
    selectedPrimaryTool = EditorPrimaryTool.Edit
    volumeAmount = 1f
    fadeAmount = 0.18f
    effectIntensity = 0.42f
    filterStrength = 0.36f
    previewZoom = 1f
    previewFill = false
    previewOverlayVisible = true
    toolPanelExpanded = true
    undoRedoState = UndoRedoState()
  }

  fun recordEditorAction(label: String) {
    undoRedoState = undoRedoState.copy(
      undoStack = (undoRedoState.undoStack + label).takeLast(12),
      redoStack = emptyList(),
    )
  }

  LaunchedEffect(draft.sourceUri, isVideoSource) {
    if (draft.sourceUri.isBlank() || !isVideoSource) {
      player.stop()
      player.clearMediaItems()
    } else {
      player.setMediaItem(MediaItem.fromUri(draft.sourceUri))
      player.prepare()
      player.seekTo(draft.playheadMs)
    }
  }

  LaunchedEffect(draft.playheadMs, draft.sourceUri, isVideoSource) {
    if (
      isVideoSource &&
      draft.sourceUri.isNotBlank() &&
      !timelineInteracting &&
      abs(player.currentPosition - draft.playheadMs) > timelineFrameStepMs(draft.sourceDurationMs)
    ) {
      player.seekTo(draft.playheadMs)
    }
  }

  LaunchedEffect(player, draft.trimStartMs, draft.trimEndMs, draft.sourceUri, isVideoSource) {
    while (isActive) {
      delay(66)
      if (draft.sourceUri.isBlank() || !isVideoSource) {
        isPlaying = false
        continue
      }
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

  val previewProgress = ((draft.playheadMs - draft.trimStartMs).toFloat() / (draft.trimEndMs - draft.trimStartMs).coerceAtLeast(1L).toFloat())
    .coerceIn(0f, 1f)
  val centerTransportAlpha by animateFloatAsState(
    targetValue = if (previewOverlayVisible || !isPlaying) 1f else 0f,
    animationSpec = tween(durationMillis = 180),
    label = "centerTransportAlpha",
  )
  val previewOverlayAlpha by animateFloatAsState(
    targetValue = if (previewOverlayVisible) 1f else 0f,
    animationSpec = tween(durationMillis = 180),
    label = "previewOverlayAlpha",
  )

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
          IconButton(onClick = onOpenMediaPicker) { Icon(Icons.Rounded.FolderOpen, contentDescription = stringResource(R.string.editor_pick_video)) }
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
          } else if (!isVideoSource) {
            Text(stringResource(R.string.editor_image_not_supported_hint), color = MaterialTheme.colorScheme.error)
          }
          Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary),
            enabled = draft.sourceUri.isNotBlank() && isVideoSource,
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
        .padding(horizontal = 12.dp, vertical = 8.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF090C11),
        tonalElevation = 0.dp,
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(draft.displayName, color = ClipyOnDark, style = MaterialTheme.typography.titleSmall, maxLines = 1)
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
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0C1016),
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .aspectRatio(9f / 16f)
                  .clip(RoundedCornerShape(22.dp))
                  .background(
                    Brush.verticalGradient(
                      listOf(
                        Color(0xFF010203),
                        Color(0xFF05080D),
                        Color(0xFF111723),
                      ),
                    ),
                  )
                  .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(22.dp))
                  .pointerInput(draft.trimStartMs, draft.trimEndMs, draft.playheadMs) {
                    detectTapGestures(
                      onTap = { previewOverlayVisible = !previewOverlayVisible },
                      onDoubleTap = {
                        previewZoom = 1f
                        previewFill = false
                        previewOverlayVisible = true
                      },
                    )
                  },
                contentAlignment = Alignment.Center,
              ) {
                if (draft.sourceUri.isBlank()) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(56.dp), tint = ClipyOnDark)
                    Spacer(Modifier.height(8.dp))
                    Text(draft.displayName)
                    Text(stringResource(R.string.editor_pick_video_hint), color = ClipyMuted)
                  }
                } else if (!isVideoSource) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 24.dp),
                  ) {
                    Icon(Icons.Rounded.FitScreen, contentDescription = null, modifier = Modifier.size(52.dp), tint = ClipyAccent)
                    Text(draft.displayName, color = ClipyOnDark, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text(
                      stringResource(R.string.editor_image_not_supported_hint),
                      color = ClipyMuted,
                      style = MaterialTheme.typography.bodyMedium,
                      textAlign = TextAlign.Center,
                    )
                  }
                } else {
                  AndroidView(
                    modifier = Modifier
                      .fillMaxSize()
                      .pointerInput(draft.trimStartMs, draft.trimEndMs) {
                        detectTransformGestures { _, pan, zoom, _ ->
                          previewOverlayVisible = true
                          previewZoom = (previewZoom * zoom).coerceIn(1f, 3f)
                          if (kotlin.math.abs(pan.x) > 6f) {
                            val deltaMs = (pan.x * -6f).roundToLong()
                            onPlayheadChange((draft.playheadMs + deltaMs).coerceIn(draft.trimStartMs, draft.trimEndMs))
                          }
                        }
                      },
                    factory = { viewContext ->
                      PlayerView(viewContext).apply {
                        useController = false
                        resizeMode = if (previewFill) {
                          AspectRatioFrameLayout.RESIZE_MODE_FILL
                        } else {
                          AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        this.player = player
                      }
                    },
                    update = {
                      it.player = player
                      it.resizeMode = if (previewFill) {
                        AspectRatioFrameLayout.RESIZE_MODE_FILL
                      } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                      }
                      it.scaleX = previewZoom
                      it.scaleY = previewZoom
                    },
                  )
                  if (previewOverlayAlpha > 0.01f) {
                    Box(modifier = Modifier.fillMaxSize()) {
                      EditorOverlayBadge(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                        title = dockState.playheadLabel,
                        subtitle = dockState.trimLabel,
                      )
                      Row(
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        AssistChip(
                          onClick = { previewFill = false },
                          label = { Text(stringResource(R.string.editor_preview_fit)) },
                        )
                        AssistChip(
                          onClick = { previewFill = true },
                          label = { Text(stringResource(R.string.editor_preview_fill)) },
                        )
                      }
                      EditorStatusPill(
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                        isLive = timelineChrome.isInteracting || isPlaying,
                      )
                      Text(
                        text = stringResource(R.string.editor_preview_glass_hint),
                        modifier = Modifier
                          .align(Alignment.BottomStart)
                          .padding(start = 10.dp, bottom = 34.dp)
                          .background(Color.Black.copy(alpha = 0.32f * previewOverlayAlpha))
                          .clip(RoundedCornerShape(14.dp))
                          .padding(horizontal = 10.dp, vertical = 6.dp),
                        color = ClipyOnDark.copy(alpha = 0.84f * previewOverlayAlpha),
                        style = MaterialTheme.typography.labelSmall,
                      )
                    }
                  }
                  Surface(
                    modifier = Modifier
                      .align(Alignment.Center)
                      .size(72.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.28f),
                    tonalElevation = 0.dp,
                  ) {
                    IconButton(
                      onClick = {
                        previewOverlayVisible = true
                        if (player.isPlaying) player.pause() else player.play()
                        isPlaying = player.isPlaying
                      },
                      modifier = Modifier.fillMaxSize(),
                    ) {
                      Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.editor_transport_play),
                        tint = Color.White.copy(alpha = centerTransportAlpha),
                        modifier = Modifier.size(34.dp),
                      )
                    }
                  }
                  Box(
                    modifier = Modifier
                      .align(Alignment.BottomCenter)
                      .padding(horizontal = 12.dp, vertical = 12.dp)
                      .fillMaxWidth()
                      .height(14.dp)
                      .onSizeChanged { previewScrubberWidthPx = it.width }
                      .pointerInput(draft.trimStartMs, draft.trimEndMs) {
                        detectDragGestures { change, dragAmount ->
                          change.consume()
                          val width = previewScrubberWidthPx.toFloat().coerceAtLeast(1f)
                          val relativeX = (change.position.x + dragAmount.x).coerceIn(0f, width)
                          val seekMs = draft.trimStartMs + ((draft.trimEndMs - draft.trimStartMs) * (relativeX / width)).roundToLong()
                          previewOverlayVisible = true
                          onPlayheadChange(seekMs.coerceIn(draft.trimStartMs, draft.trimEndMs))
                        }
                      },
                  ) {
                    Box(
                      modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                    )
                    Box(
                      modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(previewProgress)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(ClipyAccent.copy(alpha = 0.92f)),
                    )
                    Box(
                      modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset {
                          IntOffset(
                            (previewProgress * previewScrubberWidthPx - 7.dp.toPx()).roundToInt(),
                            0,
                          )
                        }
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, ClipyAccent, CircleShape),
                    )
                  }
                  Box(
                    modifier = Modifier
                      .align(Alignment.Center)
                      .fillMaxHeight()
                      .width(1.dp)
                      .background(ClipyOnDark.copy(alpha = 0.92f)),
                  )
                }
              }

              if (isVideoSource) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    Text(dockState.playheadLabel, color = ClipyOnDark, style = MaterialTheme.typography.titleMedium)
                    Text(dockState.trimLabel, color = ClipyMuted, style = MaterialTheme.typography.bodySmall)
                  }
                  TimelineCompactBadge(primary = dockState.zoomLabel, secondary = stringResource(R.string.editor_timeline_ruler))
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                      onClick = {
                        val newId = "seg-${audioSegments.size}"
                        val startMs = draft.playheadMs.coerceIn(0L, draft.sourceDurationMs.coerceAtLeast(MIN_TRIM_GAP_MS * 2) - MIN_TRIM_GAP_MS)
                        val endMs = (startMs + 1800L).coerceAtMost(draft.sourceDurationMs.coerceAtLeast(startMs + MIN_TRIM_GAP_MS))
                        audioSegments = audioSegments + AudioSegmentUi(newId, startMs, endMs)
                        selectedAudioSegmentId = newId
                        selectedTrack = EditorTrack.Audio
                        selectedPrimaryTool = EditorPrimaryTool.Audio
                        recordEditorAction(context.getString(R.string.editor_history_add_audio))
                      },
                      leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                      label = { Text(stringResource(R.string.editor_quick_add_audio)) },
                    )
                    AssistChip(
                      onClick = {
                        val newId = "text-${textClips.size}"
                        val startMs = draft.playheadMs.coerceIn(draft.trimStartMs, draft.trimEndMs)
                        val endMs = (startMs + 2200L).coerceAtMost(draft.trimEndMs)
                        textClips = textClips + TextClipUi(newId, context.getString(R.string.editor_text_overlay_default), startMs, endMs)
                        selectedTextClipId = newId
                        selectedTrack = EditorTrack.Text
                        selectedPrimaryTool = EditorPrimaryTool.Text
                        recordEditorAction(context.getString(R.string.editor_history_add_text))
                      },
                      leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null) },
                      label = { Text(stringResource(R.string.editor_quick_add_text)) },
                    )
                  }
                  Surface(shape = RoundedCornerShape(999.dp), color = Color(0x33161C28)) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Rounded.Layers, contentDescription = null, tint = ClipyAccent)
                      Text(stringResource(R.string.editor_quick_layers), color = ClipyOnDark, style = MaterialTheme.typography.labelMedium)
                    }
                  }
                }

                Surface(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(20.dp),
                  color = Color(0xFF10141C),
                ) {
                  Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      TimelineMetaBadge(label = stringResource(R.string.editor_trim_label), value = if (selectedTrack == EditorTrack.Video) dockState.trimLabel else formatTimelineWindow(audioTrimStartMs, audioTrimEndMs), modifier = Modifier.weight(1f))
                      Spacer(Modifier.width(6.dp))
                      TimelineMetaBadge(label = stringResource(R.string.editor_playhead_label), value = dockState.playheadLabel, modifier = Modifier.weight(1f))
                      Spacer(Modifier.width(6.dp))
                      TimelineMetaBadge(label = stringResource(R.string.editor_timeline_zoom, timeline.zoom), value = dockState.zoomLabel, modifier = Modifier.weight(1f))
                    }
                    Text(
                      text = stringResource(R.string.editor_timeline_precision),
                      color = ClipyMuted,
                      style = MaterialTheme.typography.labelMedium,
                      textAlign = TextAlign.Center,
                      modifier = Modifier.fillMaxWidth(),
                    )
                    TimelineEditor(
                      sourceUri = draft.sourceUri,
                      canLoadVideoFrames = shouldLoadVideoTimelineFrames(draft.sourceUri, isVideoSource),
                      timeline = timeline,
                      selectedTrack = selectedTrack,
                      selectedTool = selectedTimelineTool,
                      audioTrimStartMs = audioTrimStartMs,
                      audioTrimEndMs = audioTrimEndMs,
                      audioSegments = audioSegments,
                      selectedAudioSegmentId = selectedAudioSegmentId,
                      textClips = textClips,
                      selectedTextClipId = selectedTextClipId,
                      audioGain = audioGain,
                      isMuted = draft.isMuted,
                      onTrimStartChange = onTrimStartChange,
                      onTrimEndChange = onTrimEndChange,
                      onAudioTrimStartChange = { audioTrimStartMs = it.coerceIn(0L, audioTrimEndMs - MIN_TRIM_GAP_MS) },
                      onAudioTrimEndChange = { audioTrimEndMs = it.coerceIn(audioTrimStartMs + MIN_TRIM_GAP_MS, draft.sourceDurationMs) },
                      onPlayheadChange = onPlayheadChange,
                      onZoomChange = onTimelineZoomChange,
                      onTrackSelected = { selectedTrack = it },
                      onToolSelected = { selectedTimelineTool = it },
                      onAudioSegmentSelected = { selectedAudioSegmentId = it },
                      onTextClipSelected = { selectedTextClipId = it },
                      onAudioCut = {
                        val updated = splitAudioSegments(audioSegments, selectedAudioSegmentId, draft.playheadMs)
                        audioSegments = updated
                        selectedAudioSegmentId = updated.lastOrNull { draft.playheadMs in it.startMs..it.endMs }?.id ?: updated.lastOrNull()?.id ?: selectedAudioSegmentId
                      },
                      onAudioGainChange = { audioGain = it.coerceIn(0f, 1.5f) },
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
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      EditorStatChip(label = stringResource(R.string.editor_timeline_focus_video), value = when (selectedTrack) {
                        EditorTrack.Video -> stringResource(R.string.editor_timeline_video_track)
                        EditorTrack.Audio -> stringResource(R.string.editor_timeline_audio_track)
                        EditorTrack.Text -> stringResource(R.string.editor_timeline_text_track)
                      }, modifier = Modifier.weight(1f))
                      EditorStatChip(label = stringResource(R.string.editor_duration_label), value = dockState.durationLabel, modifier = Modifier.weight(1f))
                      EditorStatChip(label = stringResource(R.string.editor_timeline_ruler), value = dockState.visibleWindowLabel, modifier = Modifier.weight(1f))
                    }
                  }
                }
              }
            }
          }
        }
      }

      Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
          modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
          shape = RoundedCornerShape(22.dp),
          color = Color(0xFF121720),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.editor_creator_rail_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.editor_context_panel_hint), color = ClipyMuted, style = MaterialTheme.typography.bodySmall)
              }
              TimelineCompactBadge(primary = undoRedoState.lastActionLabel ?: stringResource(R.string.editor_history_idle), secondary = stringResource(R.string.editor_tool_rail))
            }
            PrimaryToolRail(
              selected = selectedPrimaryTool,
              onSelected = {
                selectedPrimaryTool = it
                toolPanelExpanded = true
                recordEditorAction(context.getString(R.string.editor_history_tool_switch))
              },
            )
            if (toolPanelExpanded) {
              ContextToolPanel(
                selectedPrimaryTool = selectedPrimaryTool,
                cropRatio = draft.cropRatio,
                speed = draft.speedMultiplier,
                volumeAmount = volumeAmount,
                fadeAmount = fadeAmount,
                effectIntensity = effectIntensity,
                filterStrength = filterStrength,
                exportFormat = draft.exportFormat,
                gifFps = draft.gifFps,
                gifResolution = draft.gifResolution,
                mp4Quality = draft.mp4Quality,
                audioSplitEnabled = selectedAudioSegmentId != null,
                onCropChange = onCropChange,
                onSpeedChange = onSpeedChange,
                onVolumeChange = {
                  volumeAmount = it
                  audioGain = it
                },
                onFadeChange = { fadeAmount = it },
                onEffectIntensityChange = { effectIntensity = it },
                onFilterStrengthChange = { filterStrength = it },
                onFormatChange = onFormatChange,
                onGifFpsChange = onGifFpsChange,
                onGifResolutionChange = onGifResolutionChange,
                onMp4QualityChange = onMp4QualityChange,
                onCollapse = { toolPanelExpanded = false },
              )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              CompactToggleCard(title = stringResource(R.string.toggle_mute), checked = draft.isMuted, onToggle = onToggleMute, modifier = Modifier.weight(1f))
              CompactToggleCard(title = stringResource(R.string.toggle_reverse), checked = draft.isReversed, onToggle = onToggleReverse, modifier = Modifier.weight(1f))
              CompactToggleCard(title = stringResource(R.string.toggle_boomerang), checked = draft.isBoomerang, onToggle = onToggleBoomerang, modifier = Modifier.weight(1f))
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.End,
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniActionButton(
              icon = Icons.Rounded.Undo,
              label = stringResource(R.string.editor_undo),
              enabled = undoRedoState.canUndo,
              onClick = {
                val label = undoRedoState.undoStack.lastOrNull() ?: return@MiniActionButton
                undoRedoState = undoRedoState.copy(
                  undoStack = undoRedoState.undoStack.dropLast(1),
                  redoStack = undoRedoState.redoStack + label,
                )
              },
            )
            MiniActionButton(
              icon = Icons.Rounded.Redo,
              label = stringResource(R.string.editor_redo),
              enabled = undoRedoState.canRedo,
              onClick = {
                val label = undoRedoState.redoStack.lastOrNull() ?: return@MiniActionButton
                undoRedoState = undoRedoState.copy(
                  undoStack = undoRedoState.undoStack + label,
                  redoStack = undoRedoState.redoStack.dropLast(1),
                )
              },
            )
            MiniActionButton(
              icon = Icons.Rounded.Add,
              label = stringResource(R.string.home_new_project),
              enabled = true,
              onClick = onOpenMediaPicker,
            )
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
  val systemLanguageCode = remember { java.util.Locale.getDefault().language }

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
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.language_screen_body), color = ClipyMuted)
      }
      AppLanguage.entries.forEach { option ->
        LanguageCard(
          language = option,
          selected = language == option,
          isRecommended = option.code == systemLanguageCode,
          onClick = { language = option },
        )
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
  canLoadVideoFrames: Boolean,
  timeline: TimelineSnapshot,
  selectedTrack: EditorTrack,
  selectedTool: TimelineTool,
  audioTrimStartMs: Long,
  audioTrimEndMs: Long,
  audioSegments: List<AudioSegmentUi>,
  selectedAudioSegmentId: String?,
  textClips: List<TextClipUi>,
  selectedTextClipId: String?,
  audioGain: Float,
  isMuted: Boolean,
  onTrimStartChange: (Long) -> Unit,
  onTrimEndChange: (Long) -> Unit,
  onAudioTrimStartChange: (Long) -> Unit,
  onAudioTrimEndChange: (Long) -> Unit,
  onPlayheadChange: (Long) -> Unit,
  onZoomChange: (Float) -> Unit,
  onTrackSelected: (EditorTrack) -> Unit,
  onToolSelected: (TimelineTool) -> Unit,
  onAudioSegmentSelected: (String) -> Unit,
  onTextClipSelected: (String) -> Unit,
  onAudioCut: () -> Unit,
  onAudioGainChange: (Float) -> Unit,
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

  LaunchedEffect(sourceUri, canLoadVideoFrames, prefetchRange, frameCount) {
    if (!canLoadVideoFrames || prefetchRange.isEmpty()) {
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
  val audioStartOffsetPx by remember(audioTrimStartMs, duration, trackWidthPx, currentScrollPx, viewportWidthPx) {
    derivedStateOf { (viewportWidthPx / 2f) + timelineMsToTrackPx(audioTrimStartMs, duration, trackWidthPx) - currentScrollPx }
  }
  val audioEndOffsetPx by remember(audioTrimEndMs, duration, trackWidthPx, currentScrollPx, viewportWidthPx) {
    derivedStateOf { (viewportWidthPx / 2f) + timelineMsToTrackPx(audioTrimEndMs, duration, trackWidthPx) - currentScrollPx }
  }
  val audioRangeWidthPx by remember(audioStartOffsetPx, audioEndOffsetPx) {
    derivedStateOf { (audioEndOffsetPx - audioStartOffsetPx).coerceAtLeast(0f) }
  }
  val waveformSamples = remember(sourceUri, timeline.zoom, audioTrimStartMs, audioTrimEndMs, selectedAudioSegmentId) {
    buildWaveformSamples(
      durationMs = duration,
      bucketCount = (frameCount * 2).coerceIn(24, 180),
      trimStartMs = audioTrimStartMs,
      trimEndMs = audioTrimEndMs,
      seed = sourceUri.hashCode(),
    )
  }
  val timelineTicks = remember(visibleWindowMs, duration) {
    buildTimelineTicks(
      visibleStartMs = visibleWindowMs.first,
      visibleEndMs = visibleWindowMs.last,
      durationMs = duration,
      targetTickCount = 7,
    )
  }
  val selectedSegment = remember(audioSegments, selectedAudioSegmentId) {
    audioSegments.firstOrNull { it.id == selectedAudioSegmentId }
  }

  val activeTrackTrimLabel = when (selectedTrack) {
    EditorTrack.Video -> formatTimelineWindow(timeline.trimStartMs, timeline.trimEndMs)
    EditorTrack.Audio -> formatTimelineWindow(audioTrimStartMs, audioTrimEndMs)
    EditorTrack.Text -> formatTimelineWindow(timeline.trimStartMs, timeline.trimEndMs)
  }
  val playheadOffsetPx by remember(timeline.playheadMs, duration, trackWidthPx, currentScrollPx, viewportWidthPx) {
    derivedStateOf {
      ((viewportWidthPx / 2f) + timelineMsToTrackPx(timeline.playheadMs, duration, trackWidthPx) - currentScrollPx)
        .coerceIn(0f, viewportWidthPx.toFloat())
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    TimelineTrackHeader(
      zoomLabel = String.format(java.util.Locale.US, "%.1fx", timeline.zoom),
      playheadLabel = formatDurationMs(timeline.playheadMs),
      trimLabel = activeTrackTrimLabel,
      selectedTrack = selectedTrack,
      selectedTool = selectedTool,
      audioGain = audioGain,
      isMuted = isMuted,
      onTrackSelected = onTrackSelected,
      onToolSelected = onToolSelected,
      onAudioCut = onAudioCut,
      onZoomOut = { onZoomChange((timeline.zoom - 0.35f).coerceIn(1f, 6f)) },
      onZoomIn = { onZoomChange((timeline.zoom + 0.35f).coerceIn(1f, 6f)) },
      onAudioGainChange = onAudioGainChange,
    )
    TimelineActionRow(
      modifier = Modifier.fillMaxWidth(),
      selectedTrack = selectedTrack,
      selectedTool = selectedTool,
      audioGain = audioGain,
      isMuted = isMuted,
      selectedSegment = selectedSegment,
      playheadMs = timeline.playheadMs,
      onToolSelected = onToolSelected,
      onAudioCut = onAudioCut,
      onAudioGainChange = onAudioGainChange,
    )
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(298.dp)
        .onSizeChanged { viewportWidthPx = it.width }
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xFF080B11))
        .border(1.dp, Color.White.copy(alpha = 0.045f), RoundedCornerShape(16.dp))
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
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        TimelineRuler(
          ticks = timelineTicks,
          currentScrollPx = currentScrollPx,
          viewportWidthPx = viewportWidthPx,
          durationMs = duration,
          trackWidthPx = trackWidthPx,
          modifier = Modifier.fillMaxWidth().height(24.dp),
        )
        TimelineTrackLane(
          modifier = Modifier.fillMaxWidth().weight(1f),
          title = stringResource(R.string.editor_timeline_video_track),
          selected = selectedTrack == EditorTrack.Video,
          iconLabel = stringResource(R.string.editor_timeline_video_icon),
          onSelect = { onTrackSelected(EditorTrack.Video) },
        ) {
          LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = activeDragTarget == null,
            contentPadding = PaddingValues(horizontal = with(density) { edgePadding.toDp() }),
          ) {
            items(
              items = timelineFrames,
              key = { it.cacheKey },
            ) { frame ->
              TimelineThumbnail(
                thumbnail = timelineBitmaps[frame.cacheKey],
                active = frame.captureTimeMs in timeline.trimStartMs..timeline.trimEndMs,
                emphasized = selectedTrack == EditorTrack.Video,
                modifier = Modifier.width(cellWidth).fillMaxHeight(),
              )
            }
          }
        }
        TimelineTrackLane(
          modifier = Modifier.fillMaxWidth().weight(1f),
          title = stringResource(R.string.editor_timeline_audio_track),
          selected = selectedTrack == EditorTrack.Audio,
          iconLabel = stringResource(R.string.editor_timeline_audio_icon),
          subtitle = if (isMuted) stringResource(R.string.editor_audio_muted) else stringResource(R.string.editor_audio_linked),
          onSelect = { onTrackSelected(EditorTrack.Audio) },
        ) {
          AudioWaveformTrack(
            modifier = Modifier.fillMaxSize().padding(horizontal = with(density) { edgePadding.toDp() }),
            samples = waveformSamples,
            segments = audioSegments,
            selectedSegmentId = selectedAudioSegmentId,
            selectedTrack = selectedTrack,
            currentScrollPx = currentScrollPx,
            viewportWidthPx = viewportWidthPx,
            durationMs = duration,
            trackWidthPx = trackWidthPx,
            onSegmentSelected = onAudioSegmentSelected,
          )
        }
        TimelineTrackLane(
          modifier = Modifier.fillMaxWidth().weight(0.9f),
          title = stringResource(R.string.editor_timeline_text_track),
          selected = selectedTrack == EditorTrack.Text,
          iconLabel = stringResource(R.string.editor_timeline_text_icon),
          subtitle = stringResource(R.string.editor_quick_add_text),
          onSelect = { onTrackSelected(EditorTrack.Text) },
        ) {
          TextOverlayTrack(
            modifier = Modifier.fillMaxSize().padding(horizontal = with(density) { edgePadding.toDp() }),
            clips = textClips,
            selectedClipId = selectedTextClipId,
            currentScrollPx = currentScrollPx,
            viewportWidthPx = viewportWidthPx,
            durationMs = duration,
            trackWidthPx = trackWidthPx,
            selectedTrack = selectedTrack,
            onClipSelected = onTextClipSelected,
          )
        }
      }
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .width(with(density) { startOffsetPx.coerceIn(0f, viewportWidthPx.toFloat()).toDp() })
          .background(Color.Black.copy(alpha = 0.62f)),
      )
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .width(with(density) { activeRangeWidthPx.toDp() })
          .offset { IntOffset(startOffsetPx.roundToInt(), 0) }
          .clip(RoundedCornerShape(12.dp))
          .background(
            Brush.verticalGradient(
              listOf(
                ClipyAccent.copy(alpha = if (isUserInteracting) 0.22f else 0.16f),
                ClipyAccent.copy(alpha = if (isUserInteracting) 0.1f else 0.06f),
              ),
            ),
          )
          .border(1.dp, ClipyAccent.copy(alpha = 0.82f), RoundedCornerShape(12.dp)),
      )
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .width(with(density) { (viewportWidthPx.toFloat() - endOffsetPx).coerceIn(0f, viewportWidthPx.toFloat()).toDp() })
          .offset { IntOffset(endOffsetPx.roundToInt(), 0) }
          .background(Color.Black.copy(alpha = 0.62f)),
      )
      Box(
        modifier = Modifier
          .fillMaxHeight(0.34f)
          .width(handleTouchWidth)
          .offset { IntOffset((startOffsetPx - with(density) { handleTouchWidth.toPx() / 2f }).roundToInt(), 34.dp.roundToPx()) }
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
            .fillMaxHeight(0.82f)
            .width(10.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.verticalGradient(listOf(Color.White, if (activeDragTarget == TimelineDragTarget.Start) ClipyPrimary else ClipyAccent)))
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
          .fillMaxHeight(0.34f)
          .width(handleTouchWidth)
          .offset { IntOffset((endOffsetPx - with(density) { handleTouchWidth.toPx() / 2f }).roundToInt(), 34.dp.roundToPx()) }
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
            .fillMaxHeight(0.82f)
            .width(10.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.verticalGradient(listOf(Color.White, if (activeDragTarget == TimelineDragTarget.End) ClipyPrimary else ClipyAccent)))
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
            .background(Color.White.copy(alpha = 0.92f)),
        )
        Box(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 6.dp)
            .width(14.dp)
            .height(16.dp)
            .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
            .background(if (isUserInteracting) ClipyPrimary else ClipyAccent),
        )
      }
      if (selectedTrack != EditorTrack.Video && selectedTool == TimelineTool.Split) {
        Box(
          modifier = Modifier
            .offset { IntOffset((playheadOffsetPx - 1.dp.toPx()).roundToInt(), 148.dp.roundToPx()) }
            .width(2.dp)
            .height(76.dp)
            .background(Color.White.copy(alpha = 0.92f)),
        )
        Box(
          modifier = Modifier
            .offset { IntOffset((playheadOffsetPx - 8.dp.toPx()).roundToInt(), 142.dp.roundToPx()) }
            .width(16.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(ClipyAccent.copy(alpha = 0.9f)),
        )
      }
      AudioTrimHandle(
        xOffsetPx = audioStartOffsetPx,
        viewportWidthPx = viewportWidthPx,
        topOffsetPx = with(density) { 148.dp.toPx().roundToInt() },
        density = density,
        active = selectedTrack == EditorTrack.Audio,
        activeColor = ClipyAccent,
        onDrag = { deltaPx ->
          val nextTrackOffset = (timelineMsToTrackPx(audioTrimStartMs, duration, trackWidthPx) + deltaPx).coerceIn(
            0f,
            timelineMsToTrackPx(audioTrimEndMs - MIN_TRIM_GAP_MS, duration, trackWidthPx),
          )
          onAudioTrimStartChange(boundedTrimStartMs(nextTrackOffset, duration, trackWidthPx, audioTrimEndMs, MIN_TRIM_GAP_MS))
        },
      )
      AudioTrimHandle(
        xOffsetPx = audioEndOffsetPx,
        viewportWidthPx = viewportWidthPx,
        topOffsetPx = with(density) { 148.dp.toPx().roundToInt() },
        density = density,
        active = selectedTrack == EditorTrack.Audio,
        activeColor = ClipyAccent,
        onDrag = { deltaPx ->
          val nextTrackOffset = (timelineMsToTrackPx(audioTrimEndMs, duration, trackWidthPx) + deltaPx).coerceIn(
            timelineMsToTrackPx(audioTrimStartMs + MIN_TRIM_GAP_MS, duration, trackWidthPx),
            trackWidthPx,
          )
          onAudioTrimEndChange(boundedTrimEndMs(nextTrackOffset, duration, trackWidthPx, audioTrimStartMs, MIN_TRIM_GAP_MS))
        },
      )
      Box(
        modifier = Modifier
          .height(76.dp)
          .width(with(density) { audioRangeWidthPx.toDp() })
          .offset { IntOffset(audioStartOffsetPx.roundToInt(), 148.dp.roundToPx()) }
          .clip(RoundedCornerShape(12.dp))
          .background(ClipyAccent.copy(alpha = if (selectedTrack == EditorTrack.Audio) 0.12f else 0.04f))
          .border(1.dp, ClipyAccent.copy(alpha = if (selectedTrack == EditorTrack.Audio) 0.62f else 0.18f), RoundedCornerShape(12.dp)),
      )
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.34f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
      ) {
        Text(
          text = formatDurationMs(timeline.playheadMs.coerceIn(timeline.trimStartMs, timeline.trimEndMs)),
          color = ClipyOnDark,
          style = MaterialTheme.typography.labelMedium,
        )
      }
      if (timelineBitmaps.isEmpty() && canLoadVideoFrames) {
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
      fontSize = 11.sp,
    )
  }
}

@Composable
private fun TimelineThumbnail(
  thumbnail: Bitmap?,
  active: Boolean,
  emphasized: Boolean,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .padding(horizontal = 1.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(Color(0xFF161D28)),
    contentAlignment = Alignment.Center,
  ) {
    if (thumbnail != null) {
      Image(
        bitmap = thumbnail.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alpha = when {
          active && emphasized -> 0.98f
          active -> 0.9f
          else -> 0.44f
        },
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
private fun TimelineMetaBadge(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = Color(0xFF121722)) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
      Text(label, color = ClipyMuted, style = MaterialTheme.typography.labelSmall)
      Text(value, color = ClipyOnDark, style = MaterialTheme.typography.labelLarge)
    }
  }
}

@Composable
private fun TimelineCompactBadge(primary: String, secondary: String) {
  Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF121722)) {
    Column(
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(1.dp),
      horizontalAlignment = Alignment.End,
    ) {
      Text(primary, color = ClipyOnDark, style = MaterialTheme.typography.labelLarge)
      Text(secondary, color = ClipyMuted, style = MaterialTheme.typography.labelSmall)
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineTrackHeader(
  zoomLabel: String,
  playheadLabel: String,
  trimLabel: String,
  selectedTrack: EditorTrack,
  selectedTool: TimelineTool,
  audioGain: Float,
  isMuted: Boolean,
  onTrackSelected: (EditorTrack) -> Unit,
  onToolSelected: (TimelineTool) -> Unit,
  onAudioCut: () -> Unit,
  onZoomOut: () -> Unit,
  onZoomIn: () -> Unit,
  onAudioGainChange: (Float) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(playheadLabel, color = ClipyOnDark, style = MaterialTheme.typography.titleMedium)
        Text(trimLabel, color = ClipyMuted, style = MaterialTheme.typography.bodySmall)
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TimelineMetaBadge(label = stringResource(R.string.editor_playhead_label), value = playheadLabel)
        TimelineMetaBadge(label = stringResource(R.string.editor_timeline_zoom, 1f), value = zoomLabel)
      }
    }
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      TimelineFilterChip(selected = selectedTrack == EditorTrack.Video, onClick = { onTrackSelected(EditorTrack.Video) }, label = stringResource(R.string.editor_timeline_video_track))
      TimelineFilterChip(selected = selectedTrack == EditorTrack.Audio, onClick = { onTrackSelected(EditorTrack.Audio) }, label = stringResource(R.string.editor_timeline_audio_track))
      TimelineFilterChip(selected = selectedTrack == EditorTrack.Text, onClick = { onTrackSelected(EditorTrack.Text) }, label = stringResource(R.string.editor_timeline_text_track))
      TimelineFilterChip(selected = selectedTool == TimelineTool.Trim, onClick = { onToolSelected(TimelineTool.Trim) }, label = stringResource(R.string.editor_timeline_trim_tool))
      TimelineFilterChip(selected = selectedTool == TimelineTool.Split, onClick = { onToolSelected(TimelineTool.Split) }, label = stringResource(R.string.editor_timeline_cut_tool))
      if (selectedTrack == EditorTrack.Audio) {
        TimelineFilterChip(selected = selectedTool == TimelineTool.Gain, onClick = { onToolSelected(TimelineTool.Gain) }, label = stringResource(R.string.editor_timeline_gain_tool))
        AssistChip(onClick = {}, label = { Text(stringResource(R.string.editor_audio_gain) + " ${String.format(java.util.Locale.US, "%.1fx", audioGain)}") })
        if (isMuted) {
          AssistChip(onClick = {}, label = { Text(stringResource(R.string.editor_audio_muted)) })
        }
      }
      AssistChip(onClick = onZoomOut, label = { Text(stringResource(R.string.editor_timeline_zoom_out)) })
      AssistChip(onClick = onZoomIn, label = { Text(stringResource(R.string.editor_timeline_zoom_in)) })
    }
  }
}

@Composable
private fun TimelineActionRow(
  modifier: Modifier = Modifier,
  selectedTrack: EditorTrack,
  selectedTool: TimelineTool,
  audioGain: Float,
  isMuted: Boolean,
  selectedSegment: AudioSegmentUi?,
  playheadMs: Long,
  onToolSelected: (TimelineTool) -> Unit,
  onAudioCut: () -> Unit,
  onAudioGainChange: (Float) -> Unit,
) {
  val canSplit = selectedSegment?.let { playheadMs in (it.startMs + MIN_TRIM_GAP_MS)..(it.endMs - MIN_TRIM_GAP_MS) } == true
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFF0F141E),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = when (selectedTrack) {
            EditorTrack.Video -> stringResource(R.string.editor_timeline_focus_video)
            EditorTrack.Audio -> stringResource(R.string.editor_timeline_focus_audio)
            EditorTrack.Text -> stringResource(R.string.editor_timeline_text_track)
          },
          color = ClipyOnDark,
          style = MaterialTheme.typography.labelLarge,
        )
        Text(
          text = when (selectedTool) {
            TimelineTool.Trim -> stringResource(R.string.editor_timeline_tool_trim_hint)
            TimelineTool.Split -> stringResource(R.string.editor_timeline_tool_split_hint)
            TimelineTool.Gain -> stringResource(R.string.editor_timeline_tool_gain_hint)
          },
          color = ClipyMuted,
          style = MaterialTheme.typography.bodySmall,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (selectedTrack == EditorTrack.Audio && selectedTool == TimelineTool.Split) {
          OutlinedButton(onClick = onAudioCut, enabled = canSplit) {
            Text(stringResource(R.string.editor_timeline_cut_tool))
          }
        }
        if (selectedTrack == EditorTrack.Audio && selectedTool == TimelineTool.Gain) {
          AssistChip(onClick = { onAudioGainChange((audioGain - 0.1f).coerceAtLeast(0f)) }, label = { Text("-") })
          TimelineCompactBadge(primary = String.format(java.util.Locale.US, "%.1fx", audioGain), secondary = stringResource(R.string.editor_audio_gain))
          AssistChip(onClick = { onAudioGainChange((audioGain + 0.1f).coerceAtMost(1.5f)) }, label = { Text("+") })
        }
        TimelineCompactBadge(primary = formatDurationMs(playheadMs), secondary = stringResource(R.string.editor_playhead_label))
        if (selectedTrack == EditorTrack.Audio && isMuted) {
          AssistChip(onClick = {}, label = { Text(stringResource(R.string.editor_audio_muted)) })
        }
      }
    }
  }
}

@Composable
private fun TextOverlayTrack(
  modifier: Modifier = Modifier,
  clips: List<TextClipUi>,
  selectedClipId: String?,
  currentScrollPx: Float,
  viewportWidthPx: Int,
  durationMs: Long,
  trackWidthPx: Float,
  selectedTrack: EditorTrack,
  onClipSelected: (String) -> Unit,
) {
  val density = LocalDensity.current
  Box(modifier = modifier) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(12.dp))
        .background(if (selectedTrack == EditorTrack.Text) ClipyAccent.copy(alpha = 0.05f) else Color.Transparent),
    )
    clips.forEachIndexed { index, clip ->
      val clipStartPx = (viewportWidthPx / 2f) + timelineMsToTrackPx(clip.startMs, durationMs, trackWidthPx) - currentScrollPx
      val clipEndPx = (viewportWidthPx / 2f) + timelineMsToTrackPx(clip.endMs, durationMs, trackWidthPx) - currentScrollPx
      Box(
        modifier = Modifier
          .offset { IntOffset(clipStartPx.roundToInt(), 10.dp.roundToPx()) }
          .width(with(density) { (clipEndPx - clipStartPx).coerceAtLeast(52f).toDp() })
          .height(32.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(if (clip.id == selectedClipId) ClipyAccent.copy(alpha = 0.28f) else Color(0xFF22304C))
          .border(1.dp, if (clip.id == selectedClipId) ClipyAccent else Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
          .clickable { onClipSelected(clip.id) }
          .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
      ) {
        Text(
          text = if (clip.label.isBlank()) stringResource(R.string.editor_text_overlay_label, index + 1) else clip.label,
          color = ClipyOnDark,
          style = MaterialTheme.typography.labelMedium,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun MiniActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    shape = CircleShape,
    color = if (enabled) Color(0xFF1A2230) else Color(0xFF121720),
  ) {
    IconButton(onClick = onClick, enabled = enabled) {
      Icon(icon, contentDescription = label, tint = if (enabled) ClipyOnDark else ClipyMuted.copy(alpha = 0.45f))
    }
  }
}

@Composable
private fun PrimaryToolRail(selected: EditorPrimaryTool, onSelected: (EditorPrimaryTool) -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    EditorPrimaryTool.entries.forEach { tool ->
      val icon = when (tool) {
        EditorPrimaryTool.Edit -> Icons.Rounded.FitScreen
        EditorPrimaryTool.Audio -> Icons.Rounded.GraphicEq
        EditorPrimaryTool.Text -> Icons.Rounded.TextFields
        EditorPrimaryTool.Effects -> Icons.Rounded.AutoFixHigh
        EditorPrimaryTool.Filters -> Icons.Rounded.FilterAlt
      }
      val label = when (tool) {
        EditorPrimaryTool.Edit -> stringResource(R.string.editor_primary_tool_edit)
        EditorPrimaryTool.Audio -> stringResource(R.string.editor_primary_tool_audio)
        EditorPrimaryTool.Text -> stringResource(R.string.editor_primary_tool_text)
        EditorPrimaryTool.Effects -> stringResource(R.string.editor_primary_tool_effects)
        EditorPrimaryTool.Filters -> stringResource(R.string.editor_primary_tool_filters)
      }
      FilterChip(
        selected = selected == tool,
        onClick = { onSelected(tool) },
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = Modifier.widthIn(min = 104.dp),
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = ClipyAccent,
          selectedLabelColor = Color.White,
          selectedLeadingIconColor = Color.White,
          containerColor = Color(0xFF131925),
          labelColor = ClipyMuted,
          iconColor = ClipyMuted,
        ),
      )
    }
  }
}

@Composable
private fun ContextToolPanel(
  selectedPrimaryTool: EditorPrimaryTool,
  cropRatio: CropRatio,
  speed: Float,
  volumeAmount: Float,
  fadeAmount: Float,
  effectIntensity: Float,
  filterStrength: Float,
  exportFormat: ExportFormat,
  gifFps: Int,
  gifResolution: String,
  mp4Quality: Mp4Quality,
  audioSplitEnabled: Boolean,
  onCropChange: (CropRatio) -> Unit,
  onSpeedChange: (Float) -> Unit,
  onVolumeChange: (Float) -> Unit,
  onFadeChange: (Float) -> Unit,
  onEffectIntensityChange: (Float) -> Unit,
  onFilterStrengthChange: (Float) -> Unit,
  onFormatChange: (ExportFormat) -> Unit,
  onGifFpsChange: (Int) -> Unit,
  onGifResolutionChange: (String) -> Unit,
  onMp4QualityChange: (Mp4Quality) -> Unit,
  onCollapse: () -> Unit,
) {
  Surface(shape = RoundedCornerShape(18.dp), color = Color(0xCC161C28)) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = when (selectedPrimaryTool) {
            EditorPrimaryTool.Edit -> stringResource(R.string.editor_primary_tool_edit)
            EditorPrimaryTool.Audio -> stringResource(R.string.editor_primary_tool_audio)
            EditorPrimaryTool.Text -> stringResource(R.string.editor_primary_tool_text)
            EditorPrimaryTool.Effects -> stringResource(R.string.editor_primary_tool_effects)
            EditorPrimaryTool.Filters -> stringResource(R.string.editor_primary_tool_filters)
          },
          style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = onCollapse) { Text(stringResource(R.string.dialog_cancel)) }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TimelineCompactBadge(primary = stringResource(R.string.editor_panel_live_preview), secondary = stringResource(R.string.editor_panel_snap))
        TimelineCompactBadge(primary = stringResource(R.string.editor_panel_smooth), secondary = stringResource(R.string.editor_timeline_hint))
      }
      when (selectedPrimaryTool) {
        EditorPrimaryTool.Edit -> {
          ChipRow(items = CropRatio.entries.toList(), selected = cropRatio, label = { Text(it.label) }, onSelected = onCropChange)
          SliderRow(label = stringResource(R.string.editor_context_trim), value = speed, valueLabel = String.format(java.util.Locale.US, "%.1fx", speed), onValueChange = onSpeedChange, valueRange = 0.5f..2f)
        }
        EditorPrimaryTool.Audio -> {
          AssistChip(onClick = {}, label = { Text(if (audioSplitEnabled) stringResource(R.string.editor_split_ready) else stringResource(R.string.editor_split_wait)) })
          SliderRow(label = stringResource(R.string.editor_context_volume), value = volumeAmount, valueLabel = String.format(java.util.Locale.US, "%.0f%%", volumeAmount * 100f), onValueChange = onVolumeChange, valueRange = 0f..1.5f)
          SliderRow(label = stringResource(R.string.editor_context_fade), value = fadeAmount, valueLabel = String.format(java.util.Locale.US, "%.0f%%", fadeAmount * 100f), onValueChange = onFadeChange, valueRange = 0f..1f)
        }
        EditorPrimaryTool.Text -> {
          ChipRow(items = listOf(stringResource(R.string.editor_context_text_style), stringResource(R.string.editor_context_text_position)), selected = stringResource(R.string.editor_context_text_style), label = { Text(it) }, onSelected = {})
        }
        EditorPrimaryTool.Effects -> {
          SliderRow(label = stringResource(R.string.editor_context_effects_intensity), value = effectIntensity, valueLabel = String.format(java.util.Locale.US, "%.0f%%", effectIntensity * 100f), onValueChange = onEffectIntensityChange, valueRange = 0f..1f)
        }
        EditorPrimaryTool.Filters -> {
          ChipRow(items = ExportFormat.entries.toList(), selected = exportFormat, label = { Text(it.name.uppercase()) }, onSelected = onFormatChange)
          if (exportFormat == ExportFormat.Gif) {
            ChipRow(items = listOf(12, 18, 24, 30), selected = gifFps, label = { Text("${it} FPS") }, onSelected = onGifFpsChange)
            ChipRow(items = listOf("480p", "720p", "1080p"), selected = gifResolution, label = { Text(it) }, onSelected = onGifResolutionChange)
          } else {
            ChipRow(items = Mp4Quality.entries.toList(), selected = mp4Quality, label = { Text(mp4QualityLabel(it)) }, onSelected = onMp4QualityChange)
          }
          SliderRow(label = stringResource(R.string.editor_context_filters_strength), value = filterStrength, valueLabel = String.format(java.util.Locale.US, "%.0f%%", filterStrength * 100f), onValueChange = onFilterStrengthChange, valueRange = 0f..1f)
        }
      }
    }
  }
}

@Composable
private fun SliderRow(
  label: String,
  value: Float,
  valueLabel: String,
  onValueChange: (Float) -> Unit,
  valueRange: ClosedFloatingPointRange<Float>,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(label, color = ClipyOnDark, style = MaterialTheme.typography.labelLarge)
      Text(valueLabel, color = ClipyMuted, style = MaterialTheme.typography.labelMedium)
    }
    Slider(value = value.coerceIn(valueRange.start, valueRange.endInclusive), onValueChange = onValueChange, valueRange = valueRange)
  }
}

@Composable
private fun TimelineRuler(
  ticks: List<TimelineTickUiModel>,
  currentScrollPx: Float,
  viewportWidthPx: Int,
  durationMs: Long,
  trackWidthPx: Float,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val baselineY = size.height - 6.dp.toPx()
      drawLine(
        color = Color.White.copy(alpha = 0.08f),
        start = androidx.compose.ui.geometry.Offset(0f, baselineY),
        end = androidx.compose.ui.geometry.Offset(size.width, baselineY),
        strokeWidth = 1.dp.toPx(),
      )
      ticks.forEach { tick ->
        val x = (viewportWidthPx / 2f) + timelineMsToTrackPx(tick.timeMs, durationMs, trackWidthPx) - currentScrollPx
        if (x !in -24f..(size.width + 24f)) return@forEach
        val tickHeight = if (tick.isMajor) 12.dp.toPx() else 7.dp.toPx()
        drawLine(
          color = if (tick.isMajor) Color.White.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.16f),
          start = androidx.compose.ui.geometry.Offset(x, baselineY - tickHeight),
          end = androidx.compose.ui.geometry.Offset(x, baselineY),
          strokeWidth = 1.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }
    }
    ticks.filter { it.isMajor }.forEach { tick ->
      val x = (viewportWidthPx / 2f) + timelineMsToTrackPx(tick.timeMs, durationMs, trackWidthPx) - currentScrollPx
      if (x in 0f..viewportWidthPx.toFloat()) {
        Text(
          text = formatDurationMs(tick.timeMs),
          color = ClipyMuted,
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.offset { IntOffset((x - 14.dp.toPx()).roundToInt(), 0) },
        )
      }
    }
  }
}

@Composable
private fun TimelineTrackLane(
  modifier: Modifier = Modifier,
  title: String,
  selected: Boolean,
  iconLabel: String,
  subtitle: String? = null,
  onSelect: () -> Unit,
  content: @Composable BoxScope.() -> Unit,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(if (selected) Color(0xFF171E2B) else Color(0xFF0F151F))
      .border(1.dp, if (selected) ClipyAccent.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.035f), RoundedCornerShape(14.dp))
      .clickable(onClick = onSelect)
      .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier.size(7.dp).clip(CircleShape).background(if (selected) ClipyAccent else Color.White.copy(alpha = 0.22f)),
        )
        Text(title, color = ClipyOnDark, style = MaterialTheme.typography.labelLarge)
      }
      if (subtitle != null) {
        Text(subtitle, color = ClipyMuted, style = MaterialTheme.typography.labelSmall)
      }
    }
    Box(modifier = Modifier.fillMaxSize(), content = content)
  }
}

@Composable
private fun AudioWaveformTrack(
  modifier: Modifier = Modifier,
  samples: List<com.nantcompany.clipy.model.WaveformSampleUiModel>,
  segments: List<AudioSegmentUi>,
  selectedSegmentId: String?,
  selectedTrack: EditorTrack,
  currentScrollPx: Float,
  viewportWidthPx: Int,
  durationMs: Long,
  trackWidthPx: Float,
  onSegmentSelected: (String) -> Unit,
) {
  val density = LocalDensity.current
  Box(modifier = modifier) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(12.dp))
        .background(if (selectedTrack == EditorTrack.Audio) ClipyAccent.copy(alpha = 0.05f) else Color.Transparent),
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
      val centerY = size.height / 2f
      val barWidth = (size.width / samples.size.coerceAtLeast(1)).coerceIn(2.5f, 7f)
      samples.forEach { sample ->
        val x = ((viewportWidthPx / 2f) + timelineMsToTrackPx(sample.timeMs, durationMs, trackWidthPx) - currentScrollPx)
        if (x < -barWidth || x > size.width + barWidth) return@forEach
        val barHeight = (size.height * 0.18f) + (sample.amplitude * size.height * 0.34f)
        drawLine(
          color = if (sample.isSelected) ClipyAccent.copy(alpha = if (selectedTrack == EditorTrack.Audio) 0.95f else 0.58f) else Color.White.copy(alpha = 0.14f),
          start = androidx.compose.ui.geometry.Offset(x, centerY - barHeight / 2f),
          end = androidx.compose.ui.geometry.Offset(x, centerY + barHeight / 2f),
          strokeWidth = barWidth,
          cap = StrokeCap.Round,
        )
      }
    }
    segments.forEachIndexed { index, segment ->
      val segmentStartPx = (viewportWidthPx / 2f) + timelineMsToTrackPx(segment.startMs, durationMs, trackWidthPx) - currentScrollPx
      val segmentEndPx = (viewportWidthPx / 2f) + timelineMsToTrackPx(segment.endMs, durationMs, trackWidthPx) - currentScrollPx
      Box(
        modifier = Modifier
          .offset { IntOffset(segmentStartPx.roundToInt(), 8.dp.roundToPx()) }
          .width(with(density) { (segmentEndPx - segmentStartPx).coerceAtLeast(24f).toDp() })
          .height(24.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(if (segment.id == selectedSegmentId) ClipyAccent.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.18f))
          .border(1.dp, if (segment.id == selectedSegmentId) ClipyAccent.copy(alpha = 0.82f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
          .clickable { onSegmentSelected(segment.id) },
        contentAlignment = Alignment.Center,
      ) {
        Text(stringResource(R.string.editor_audio_segment, index + 1), color = ClipyOnDark, style = MaterialTheme.typography.labelSmall)
      }
    }
  }
}

@Composable
private fun AudioTrimHandle(
  xOffsetPx: Float,
  viewportWidthPx: Int,
  topOffsetPx: Int,
  density: androidx.compose.ui.unit.Density,
  active: Boolean,
  activeColor: Color,
  onDrag: (Float) -> Unit,
) {
  val handleTouchWidth = 42.dp
  val handleVisualWidth = 10.dp
  Box(
    modifier = Modifier
      .height(88.dp)
      .width(handleTouchWidth)
      .offset { IntOffset((xOffsetPx - with(density) { handleTouchWidth.toPx() / 2f }).roundToInt().coerceIn(-viewportWidthPx, viewportWidthPx * 2), topOffsetPx) }
      .pointerInput(active) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          onDrag(dragAmount.x)
        }
      },
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .width(handleVisualWidth)
        .clip(RoundedCornerShape(999.dp))
        .background(if (active) activeColor else Color.White.copy(alpha = 0.34f)),
    )
  }
}

@Composable
private fun TimelineFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = { Text(label) },
    colors = FilterChipDefaults.filterChipColors(
      selectedContainerColor = ClipyAccent.copy(alpha = 0.18f),
      selectedLabelColor = ClipyOnDark,
      containerColor = Color(0xFF131925),
      labelColor = ClipyMuted,
    ),
    border = FilterChipDefaults.filterChipBorder(
      enabled = true,
      selected = selected,
      borderColor = Color.White.copy(alpha = 0.05f),
      selectedBorderColor = ClipyAccent.copy(alpha = 0.36f),
      disabledBorderColor = Color.Transparent,
      disabledSelectedBorderColor = Color.Transparent,
      borderWidth = 1.dp,
      selectedBorderWidth = 1.dp,
    ),
  )
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
  enabled: Boolean = true,
) {
  Surface(
    modifier = Modifier.size(if (highlighted) 44.dp else 38.dp),
    shape = CircleShape,
    color = if (!enabled) Color(0xFF161B25) else if (highlighted) ClipyAccent else Color(0xFF1C2230),
  ) {
    IconButton(onClick = onClick, enabled = enabled) {
      Icon(
        icon,
        contentDescription = contentDescription,
        tint = if (!enabled) ClipyMuted else if (highlighted) Color.White else ClipyOnDark,
      )
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
private fun LanguageCard(language: AppLanguage, selected: Boolean, isRecommended: Boolean, onClick: () -> Unit) {
  val borderColor by animateFloatAsState(targetValue = if (selected) 1f else 0f, label = "languageBorder")
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, ClipyPrimary.copy(alpha = borderColor), RoundedCornerShape(24.dp))
      .clip(RoundedCornerShape(24.dp))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
      ),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF182443) else ClipySurfaceVariant()),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(shape = CircleShape, color = if (selected) ClipyPrimary.copy(alpha = 0.18f) else Color(0xFF1A2234)) {
        Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
          Text(languageEmoji(language), style = MaterialTheme.typography.titleMedium)
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(languageLabel(language), color = ClipyOnDark, style = MaterialTheme.typography.titleMedium)
          if (isRecommended) {
            Surface(shape = RoundedCornerShape(999.dp), color = ClipyPrimary.copy(alpha = 0.14f)) {
              Text(
                text = stringResource(R.string.language_recommended),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = ClipyPrimary,
                style = MaterialTheme.typography.labelSmall,
              )
            }
          }
        }
        Text(languageHelper(language), color = ClipyMuted)
      }
      Surface(
        shape = CircleShape,
        color = if (selected) ClipyPrimary else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) ClipyPrimary else ClipyMuted),
      ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
          if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun ClipySurfaceVariant(): Color = MaterialTheme.colorScheme.surfaceVariant

@Composable
private fun UnsupportedMediaPreview(title: String, message: String) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.padding(horizontal = 24.dp),
  ) {
    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(44.dp), tint = ClipyOnDark)
    Text(title, color = ClipyOnDark, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    Text(message, color = ClipyMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
  }
}

private fun exportSummary(state: AppSnapshot): String {
  val draft = state.draft
  if (draft.sourceUri.isBlank()) {
    return ""
  }
  if (!draft.isVideoSource) {
    return "Unsupported source type"
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

private fun languageEmoji(language: AppLanguage): String =
  when (language) {
    AppLanguage.English -> "EN"
    AppLanguage.Vietnamese -> "VI"
  }

@Composable
private fun templateCategoryLabel(category: TemplateCategory): String =
  stringResource(
    when (category) {
      TemplateCategory.Trending -> R.string.templates_category_trending
      TemplateCategory.Vlog -> R.string.templates_category_vlog
      TemplateCategory.TikTok -> R.string.templates_category_tiktok
    },
  )

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
