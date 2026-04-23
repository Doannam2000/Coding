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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.nantcompany.clipy.model.CropRatio
import com.nantcompany.clipy.model.ExportFormat
import com.nantcompany.clipy.model.ExportRecordUi
import com.nantcompany.clipy.model.Mp4Quality
import com.nantcompany.clipy.model.SaveBehavior
import com.nantcompany.clipy.model.thumbnailCaptureTimesMs
import com.nantcompany.clipy.model.timelineFrameStepMs
import com.nantcompany.clipy.model.timelineScrollForPlayhead
import com.nantcompany.clipy.model.timelineThumbnailCount
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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

private val timelineThumbnailCache = object : LruCache<String, Bitmap>(48) {}

@Stable
private data class TimelineGestureAnchor(
  val trimStartMs: Long,
  val trimEndMs: Long,
  val playheadMs: Long,
)

private enum class TimelineDragTarget {
  Start,
  End,
  Playhead,
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
    if (draft.sourceUri.isNotBlank() && abs(player.currentPosition - draft.playheadMs) > timelineFrameStepMs(draft.sourceDurationMs)) {
      player.seekTo(draft.playheadMs)
    }
  }

  LaunchedEffect(player, draft.trimStartMs, draft.trimEndMs, draft.sourceUri) {
    while (isActive) {
      delay(66)
      if (draft.sourceUri.isBlank()) continue
      val currentPosition = player.currentPosition.coerceAtLeast(0L)
      if (player.isPlaying && currentPosition >= draft.trimEndMs) {
        player.seekTo(draft.trimStartMs)
      }
      onPlayheadChange(currentPosition.coerceIn(draft.trimStartMs, draft.trimEndMs))
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
      Surface(color = ClipySurfaceVariant(), tonalElevation = 4.dp) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(stringResource(R.string.editor_ready), style = MaterialTheme.typography.titleMedium)
          Text(
            stringResource(if (draft.exportFormat == ExportFormat.Gif) R.string.editor_gif_hint else R.string.editor_mp4_hint),
            color = ClipyMuted,
          )
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
        .padding(horizontal = 16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      PremiumCard {
        Text(stringResource(R.string.editor_preview), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(ClipySurfaceVariant(), ClipyPrimary.copy(alpha = 0.18f)))),
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
                  useController = true
                  this.player = player
                }
              },
              update = { it.player = player },
            )
          }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.editor_source_label), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(draft.displayName, color = ClipyMuted)
        Spacer(Modifier.height(8.dp))
        Text("${draft.trimStartMs} - ${draft.trimEndMs} ms • ${draft.sourceDurationMs} ms source", color = ClipyMuted, fontSize = 12.sp)
      }

      SectionCard(title = stringResource(R.string.section_trim)) {
        Text(stringResource(R.string.section_trim_hint), color = ClipyMuted)
        Spacer(Modifier.height(12.dp))
        TimelineEditor(
          sourceUri = draft.sourceUri,
          timeline = timeline,
          onTrimStartChange = onTrimStartChange,
          onTrimEndChange = onTrimEndChange,
          onPlayheadChange = onPlayheadChange,
          onStepBackward = onStepBackward,
          onStepForward = onStepForward,
          onZoomChange = onTimelineZoomChange,
        )
      }

      SectionCard(title = stringResource(R.string.section_frame)) {
        ChipRow(items = CropRatio.entries.toList(), selected = draft.cropRatio, label = { it.label }, onSelected = onCropChange)
      }

      SectionCard(title = stringResource(R.string.section_motion)) {
        ChipRow(items = listOf(0.5f, 1f, 1.5f, 2f), selected = draft.speedMultiplier, label = { "${it}x" }, onSelected = onSpeedChange)
        Spacer(Modifier.height(12.dp))
        ToggleRow(stringResource(R.string.toggle_reverse), draft.isReversed, onToggleReverse)
        ToggleRow(stringResource(R.string.toggle_boomerang), draft.isBoomerang, onToggleBoomerang)
      }

      SectionCard(title = stringResource(R.string.section_audio)) {
        ToggleRow(stringResource(R.string.toggle_mute), draft.isMuted, onToggleMute)
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
        ChipRow(items = ExportFormat.entries.toList(), selected = draft.exportFormat, label = { Text(it.name.uppercase()) }, onSelected = onFormatChange)
        Spacer(Modifier.height(12.dp))
        if (draft.exportFormat == ExportFormat.Gif) {
          ChipRow(items = listOf(12, 18, 24, 30), selected = draft.gifFps, label = { Text("${it} FPS") }, onSelected = onGifFpsChange)
          Spacer(Modifier.height(12.dp))
          ChipRow(items = listOf("480p", "720p", "1080p"), selected = draft.gifResolution, label = { Text(it) }, onSelected = onGifResolutionChange)
        } else {
           ChipRow(items = Mp4Quality.entries.toList(), selected = draft.mp4Quality, label = { Text(mp4QualityLabel(it)) }, onSelected = onMp4QualityChange)
        }
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
  onStepBackward: () -> Unit,
  onStepForward: () -> Unit,
  onZoomChange: (Float) -> Unit,
) {
  val density = LocalDensity.current
  val scrollState = rememberScrollState()
  var viewportWidthPx by remember { mutableStateOf(with(density) { 320.dp.roundToPx() }) }
  var activeDragTarget by remember { mutableStateOf<TimelineDragTarget?>(null) }
  var gestureAnchor by remember(timeline) {
    mutableStateOf(
      TimelineGestureAnchor(
        trimStartMs = timeline.trimStartMs,
        trimEndMs = timeline.trimEndMs,
        playheadMs = timeline.playheadMs,
      ),
    )
  }
  val contentWidth = (520.dp * timeline.zoom)
  val contentWidthPx = with(density) { contentWidth.roundToPx() }
  val handleWidth = 18.dp
  val duration = timeline.durationMs.coerceAtLeast(1L)
  val startFraction = timeline.trimStartMs / duration.toFloat()
  val endFraction = timeline.trimEndMs / duration.toFloat()
  val playheadFraction = timeline.playheadMs / duration.toFloat()
  val thumbnailCount = remember(duration, timeline.zoom, viewportWidthPx) { timelineThumbnailCount(timeline.zoom, viewportWidthPx) }
  val thumbnailTimes = remember(timeline, thumbnailCount) { thumbnailCaptureTimesMs(timeline, thumbnailCount) }
  val contentWidthPxFloat = with(density) { contentWidth.toPx() }.coerceAtLeast(1f)

  LaunchedEffect(scrollState, activeDragTarget, timeline.durationMs) {
    snapshotFlow { Triple(timeline.playheadMs, contentWidthPx, viewportWidthPx) }
      .distinctUntilChanged()
      .collect { (playheadMs, currentContentWidthPx, currentViewportWidthPx) ->
        if (activeDragTarget != null) return@collect
        val centeredFraction = playheadMs / timeline.durationMs.coerceAtLeast(1L).toFloat()
        val target = timelineScrollForPlayhead(centeredFraction, currentContentWidthPx, currentViewportWidthPx)
        val maxValue = (currentContentWidthPx - currentViewportWidthPx).coerceAtLeast(0)
        scrollState.animateScrollTo(target.coerceIn(0, maxValue))
      }
  }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedButton(onClick = onStepBackward) { Text("-1f") }
      OutlinedButton(onClick = onStepForward) { Text("+1f") }
      Text("${timeline.zoom}x zoom", color = ClipyMuted)
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(118.dp)
        .onSizeChanged { viewportWidthPx = it.width }
        .clip(RoundedCornerShape(20.dp))
        .background(Color(0xFF171B26))
        .horizontalScroll(scrollState)
        .pointerInput(timeline.zoom) {
          detectTransformGestures { _, _, zoom, _ ->
            onZoomChange((timeline.zoom * zoom).coerceIn(1f, 6f))
          }
        },
    ) {
      Box(modifier = Modifier.width(contentWidth).fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          thumbnailTimes.forEach { captureTimeMs ->
            TimelineThumbnail(
              sourceUri = sourceUri,
              captureTimeMs = captureTimeMs,
              modifier = Modifier.weight(1f).fillMaxSize(),
            )
          }
        }
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .width(handleWidth)
            .offset { IntOffset(with(density) { (contentWidth.toPx() * startFraction).roundToInt() }, 0) }
            .background(ClipyAccent.copy(alpha = 0.9f))
            .pointerInput(timeline) {
              detectDragGestures(
                onDragStart = {
                  activeDragTarget = TimelineDragTarget.Start
                  gestureAnchor = TimelineGestureAnchor(timeline.trimStartMs, timeline.trimEndMs, timeline.playheadMs)
                },
                onDragEnd = {
                  activeDragTarget = null
                },
                onDragCancel = {
                  activeDragTarget = null
                },
              ) { change, dragAmount ->
                change.consume()
                val deltaMs = (timeline.durationMs * (dragAmount.x / contentWidthPxFloat)).roundToLong()
                val maxStart = max(0L, gestureAnchor.trimEndMs - MIN_TRIM_GAP_MS)
                val target = snapTimelineMs((gestureAnchor.trimStartMs + deltaMs).coerceIn(0L, maxStart))
                gestureAnchor = gestureAnchor.copy(trimStartMs = target)
                onTrimStartChange(target)
              }
            },
        )
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .width(handleWidth)
            .offset { IntOffset((with(density) { contentWidth.toPx() } * endFraction).roundToInt() - with(density) { handleWidth.toPx().roundToInt() }, 0) }
            .background(ClipyAccent.copy(alpha = 0.9f))
            .pointerInput(timeline) {
              detectDragGestures(
                onDragStart = {
                  activeDragTarget = TimelineDragTarget.End
                  gestureAnchor = TimelineGestureAnchor(timeline.trimStartMs, timeline.trimEndMs, timeline.playheadMs)
                },
                onDragEnd = {
                  activeDragTarget = null
                },
                onDragCancel = {
                  activeDragTarget = null
                },
              ) { change, dragAmount ->
                change.consume()
                val deltaMs = (timeline.durationMs * (dragAmount.x / contentWidthPxFloat)).roundToLong()
                val minEnd = min(timeline.durationMs, gestureAnchor.trimStartMs + MIN_TRIM_GAP_MS)
                val target = snapTimelineMs((gestureAnchor.trimEndMs + deltaMs).coerceIn(minEnd, timeline.durationMs))
                gestureAnchor = gestureAnchor.copy(trimEndMs = target)
                onTrimEndChange(target)
              }
            },
        )
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .width(3.dp)
            .offset { IntOffset((with(density) { contentWidth.toPx() } * playheadFraction).roundToInt(), 0) }
            .background(ClipyPrimary)
            .pointerInput(timeline) {
              detectDragGestures(
                onDragStart = {
                  activeDragTarget = TimelineDragTarget.Playhead
                  gestureAnchor = TimelineGestureAnchor(timeline.trimStartMs, timeline.trimEndMs, timeline.playheadMs)
                },
                onDragEnd = {
                  activeDragTarget = null
                },
                onDragCancel = {
                  activeDragTarget = null
                },
              ) { change, dragAmount ->
                change.consume()
                val deltaMs = (timeline.durationMs * (dragAmount.x / contentWidthPxFloat)).roundToLong()
                val target = snapTimelineMs((gestureAnchor.playheadMs + deltaMs).coerceIn(timeline.trimStartMs, timeline.trimEndMs))
                gestureAnchor = gestureAnchor.copy(playheadMs = target)
                onPlayheadChange(target)
              }
            },
        )
      }
    }
    Text("Loop ${timeline.trimStartMs} ms to ${timeline.trimEndMs} ms • Playhead ${timeline.playheadMs} ms", color = ClipyMuted, fontSize = 12.sp)
  }
}

@Composable
private fun TimelineThumbnail(sourceUri: String, captureTimeMs: Long, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val cacheKey = remember(sourceUri, captureTimeMs) { "$sourceUri@$captureTimeMs" }
  var thumbnail by remember(cacheKey) { mutableStateOf(timelineThumbnailCache.get(cacheKey)) }

  LaunchedEffect(sourceUri, captureTimeMs) {
    if (sourceUri.isBlank()) {
      thumbnail = null
      return@LaunchedEffect
    }
    thumbnail = timelineThumbnailCache.get(cacheKey) ?: runCatching {
      val retriever = MediaMetadataRetriever()
      try {
        retriever.setDataSource(context, Uri.parse(sourceUri))
        retriever.getFrameAtTime(captureTimeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
      } finally {
        runCatching { retriever.release() }
      }
    }.getOrNull()?.also { timelineThumbnailCache.put(cacheKey, it) }
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(ClipySurfaceVariant()),
    contentAlignment = Alignment.Center,
  ) {
    if (thumbnail != null) {
      Image(
        bitmap = thumbnail!!.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
    } else {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${captureTimeMs} ms", color = ClipyMuted, fontSize = 11.sp)
      }
    }
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
