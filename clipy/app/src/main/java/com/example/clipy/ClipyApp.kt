package com.example.clipy

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.clipy.clipy.data.ClipyRepository.AppSnapshot
import com.example.clipy.clipy.model.AppLanguage
import com.example.clipy.clipy.model.CropRatio
import com.example.clipy.clipy.model.ExportFormat
import com.example.clipy.clipy.model.ExportRecordUi
import com.example.clipy.clipy.model.Mp4Quality
import com.example.clipy.clipy.model.SaveBehavior
import com.example.clipy.clipy.model.UserPreferences
import com.example.clipy.clipy.model.WatermarkPosition
import com.example.clipy.clipy.ui.ClipyViewModel
import com.example.clipy.theme.ClipyAccent
import com.example.clipy.theme.ClipyBackground
import com.example.clipy.theme.ClipyMuted
import com.example.clipy.theme.ClipyOnDark
import com.example.clipy.theme.ClipyPrimary
import com.example.clipy.theme.ClipySecondary
import com.example.clipy.theme.ClipySuccess
import kotlinx.coroutines.delay

private const val SPLASH = "splash"
private const val INTRO = "intro"
private const val EDITOR = "editor"
private const val SETTINGS = "settings"
private const val HISTORY = "history"
private const val EXPORT = "export"

@Composable
fun ClipyApp(finishApp: () -> Unit) {
  val context = LocalContext.current
  val app = context.applicationContext as Application
  val viewModel: ClipyViewModel = viewModel(factory = ClipyViewModel.factory(app))
  val state by viewModel.appState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  var splashResolved by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(state.preferences.onboardingCompleted, splashResolved) {
    if (splashResolved) {
      navController.navigate(if (state.preferences.onboardingCompleted) EDITOR else INTRO) {
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
        onContinue = {
          viewModel.completeOnboarding(it)
          navController.navigate(EDITOR) { popUpTo(INTRO) { inclusive = true } }
        },
      )
    }
    composable(EDITOR) {
      EditorScreen(
        state = state,
        onImportVideo = viewModel::importVideo,
        onTrimStartChange = viewModel::updateTrimStart,
        onTrimEndChange = viewModel::updateTrimEnd,
        onCropChange = viewModel::updateCropRatio,
        onSpeedChange = viewModel::updateSpeed,
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
          viewModel.startExport()
          navController.navigate(EXPORT)
        },
        onExit = finishApp,
      )
    }
    composable(SETTINGS) {
      SettingsScreen(
        preferences = state.preferences,
        onBack = navController::popBackStack,
        onSave = viewModel::saveSettings,
        onExit = finishApp,
      )
    }
    composable(HISTORY) {
      HistoryScreen(
        state = state,
        onBack = navController::popBackStack,
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
        "Fast social-ready video edits and GIF exports in seconds.",
        modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
        textAlign = TextAlign.Center,
        color = ClipyMuted,
      )
      Spacer(Modifier.height(40.dp))
      CircularProgressIndicator(color = ClipyPrimary)
    }
  }
}

@Composable
private fun IntroScreen(selectedLanguage: String, onContinue: (AppLanguage) -> Unit) {
  var page by rememberSaveable { mutableStateOf(0) }
  var language by rememberSaveable { mutableStateOf(AppLanguage.entries.first { it.code == selectedLanguage }) }
  val cards = listOf(
    "Trim fast" to "Cut social-ready clips in seconds with one streamlined workspace.",
    "Frame every format" to "Switch between 1:1, 4:5, 9:16, and 16:9 without leaving the editor.",
    "Export smart" to "Balance GIF size, MP4 quality, speed, watermark, mute, reverse, and boomerang in one flow.",
  )

  Scaffold(containerColor = ClipyBackground, contentWindowInsets = WindowInsets.safeDrawing) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(20.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Creator-ready in one pass", style = MaterialTheme.typography.headlineLarge)
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
        Text("Choose language", style = MaterialTheme.typography.titleLarge)
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
          Text(if (page < cards.lastIndex) "Continue" else "Enter Clipy")
        }
        TextButton(onClick = { onContinue(language) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
          Text("Skip")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(
  state: AppSnapshot,
  onImportVideo: (Uri) -> Unit,
  onTrimStartChange: (Long) -> Unit,
  onTrimEndChange: (Long) -> Unit,
  onCropChange: (CropRatio) -> Unit,
  onSpeedChange: (Float) -> Unit,
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
  onExit: () -> Unit,
) {
  val context = LocalContext.current
  val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let {
      context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
      onImportVideo(it)
    }
  }
  var showExitDialog by rememberSaveable { mutableStateOf(false) }
  val draft = state.draft
  val player = remember { ExoPlayer.Builder(context).build() }

  LaunchedEffect(draft.sourceUri) {
    if (draft.sourceUri.isBlank()) {
      player.stop()
      player.clearMediaItems()
    } else {
      player.setMediaItem(MediaItem.fromUri(draft.sourceUri))
      player.prepare()
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
        actions = {
          IconButton(onClick = onOpenHistory) { Icon(Icons.Rounded.History, contentDescription = "History") }
          IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
          IconButton(onClick = { showExitDialog = true }) { Icon(Icons.Rounded.ExitToApp, contentDescription = "Exit") }
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
          Text("Ready for export", style = MaterialTheme.typography.titleMedium)
          Text(
            if (draft.exportFormat == ExportFormat.Gif) "Duration capped for compact sharing." else "MP4 quality stays tuned for social posting.",
            color = ClipyMuted,
          )
          Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary),
          ) {
            Text(if (draft.exportFormat == ExportFormat.Gif) "Export GIF" else "Export MP4")
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
        Text("Preview", style = MaterialTheme.typography.titleLarge)
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
              Text("Pick a clip to enable Media3 playback.", color = ClipyMuted)
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
        OutlinedButton(onClick = { picker.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth()) {
          Icon(Icons.Rounded.FolderOpen, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Pick video")
        }
      }

      SectionCard(title = "Trim") {
        Text("Quick presets keep the MVP lightweight while still feeling tactile.", color = ClipyMuted)
        Spacer(Modifier.height(12.dp))
        RangeRow(label = "Start", value = draft.trimStartMs, suffix = "ms", presets = listOf(0L, 1000L, 2000L, 3000L), onSelected = onTrimStartChange)
        Spacer(Modifier.height(12.dp))
        RangeRow(label = "End", value = draft.trimEndMs, suffix = "ms", presets = listOf(6000L, 9000L, 12000L, 15000L), onSelected = onTrimEndChange)
      }

      SectionCard(title = "Frame") {
        ChipRow(items = CropRatio.entries.toList(), selected = draft.cropRatio, label = { it.label }, onSelected = onCropChange)
      }

      SectionCard(title = "Motion") {
        ChipRow(items = listOf(0.5f, 1f, 1.5f, 2f), selected = draft.speedMultiplier, label = { "${it}x" }, onSelected = onSpeedChange)
        Spacer(Modifier.height(12.dp))
        ToggleRow("Reverse", draft.isReversed, onToggleReverse)
        ToggleRow("Boomerang", draft.isBoomerang, onToggleBoomerang)
      }

      SectionCard(title = "Audio") {
        ToggleRow("Mute export", draft.isMuted, onToggleMute)
      }

      SectionCard(title = "Watermark") {
        OutlinedTextField(
          value = draft.watermarkText,
          onValueChange = onWatermarkChange,
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Watermark text") },
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(12.dp))
        ChipRow(items = WatermarkPosition.entries.toList(), selected = draft.watermarkPosition, label = { it.label }, onSelected = onWatermarkPositionChange)
      }

      SectionCard(title = "Output") {
        OutlinedTextField(
          value = draft.outputName,
          onValueChange = onOutputNameChange,
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Output name") },
        )
        Spacer(Modifier.height(12.dp))
        ChipRow(items = ExportFormat.entries.toList(), selected = draft.exportFormat, label = { it.name.uppercase() }, onSelected = onFormatChange)
        Spacer(Modifier.height(12.dp))
        if (draft.exportFormat == ExportFormat.Gif) {
          ChipRow(items = listOf(12, 18, 24, 30), selected = draft.gifFps, label = { "${it} FPS" }, onSelected = onGifFpsChange)
          Spacer(Modifier.height(12.dp))
          ChipRow(items = listOf("480p", "720p", "1080p"), selected = draft.gifResolution, label = { it }, onSelected = onGifResolutionChange)
        } else {
          ChipRow(items = Mp4Quality.entries.toList(), selected = draft.mp4Quality, label = { it.label }, onSelected = onMp4QualityChange)
        }
      }
      Spacer(Modifier.height(12.dp))
    }
  }

  if (showExitDialog) {
    AlertDialog(
      onDismissRequest = { showExitDialog = false },
      confirmButton = { TextButton(onClick = onExit) { Text("Exit") } },
      dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Stay") } },
      title = { Text("Exit Clipy?") },
      text = { Text("Your draft stays local on device. Leave the app when you are ready.") },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(preferences: UserPreferences, onBack: () -> Unit, onSave: (UserPreferences) -> Unit, onExit: () -> Unit) {
  var edited by remember(preferences) { mutableStateOf(preferences) }
  var confirmExit by rememberSaveable { mutableStateOf(false) }

  Scaffold(
    containerColor = ClipyBackground,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("Settings") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
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
      SectionCard(title = "Language") {
        ChipRow(
          items = AppLanguage.entries.toList(),
          selected = AppLanguage.entries.first { it.code == edited.languageCode },
          label = { if (it == AppLanguage.English) "English" else "Tiếng Việt" },
          onSelected = { edited = edited.copy(languageCode = it.code) },
        )
      }
      SectionCard(title = "Default export preferences") {
        ChipRow(items = listOf(12, 18, 24, 30), selected = edited.defaultGifFps, label = { "${it} FPS" }, onSelected = { edited = edited.copy(defaultGifFps = it) })
        Spacer(Modifier.height(12.dp))
        ChipRow(items = listOf("480p", "720p", "1080p"), selected = edited.defaultGifResolution, label = { it }, onSelected = { edited = edited.copy(defaultGifResolution = it) })
        Spacer(Modifier.height(12.dp))
        ChipRow(items = Mp4Quality.entries.toList(), selected = edited.defaultMp4Quality, label = { it.label }, onSelected = { edited = edited.copy(defaultMp4Quality = it) })
        Spacer(Modifier.height(12.dp))
        ChipRow(items = CropRatio.entries.toList(), selected = edited.defaultCropRatio, label = { it.label }, onSelected = { edited = edited.copy(defaultCropRatio = it) })
        Spacer(Modifier.height(12.dp))
        ToggleRow(title = "Mute by default", checked = edited.defaultMuteEnabled) {
          edited = edited.copy(defaultMuteEnabled = !edited.defaultMuteEnabled)
        }
      }
      SectionCard(title = "Storage behavior") {
        Text("Choose whether Clipy saves directly, asks each time, or opens sharing first.", color = ClipyMuted)
        Spacer(Modifier.height(12.dp))
        ChipRow(items = SaveBehavior.entries.toList(), selected = edited.saveBehavior, label = { it.label }, onSelected = { edited = edited.copy(saveBehavior = it) })
      }
      SectionCard(title = "About") {
        Text("Privacy-first creator workflow with local history and reusable export presets.", color = ClipyMuted)
      }
      Button(
        onClick = { onSave(edited); onBack() },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary),
      ) {
        Text("Save settings")
      }
      OutlinedButton(onClick = { confirmExit = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Exit app")
      }
    }
  }

  if (confirmExit) {
    AlertDialog(
      onDismissRequest = { confirmExit = false },
      confirmButton = { TextButton(onClick = onExit) { Text("Exit") } },
      dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Cancel") } },
      title = { Text("Close Clipy") },
      text = { Text("Use the explicit exit path required for the app shell.") },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(state: AppSnapshot, onBack: () -> Unit, onReuse: (Long) -> Unit) {
  Scaffold(
    containerColor = ClipyBackground,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("Export history") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
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
      if (state.history.isEmpty()) {
        PremiumCard {
          Text("No exports yet", style = MaterialTheme.typography.titleLarge)
          Spacer(Modifier.height(8.dp))
          Text("Finish your first GIF or MP4 to build a reusable local history.", color = ClipyMuted)
        }
      } else {
        state.history.forEach { item ->
          HistoryItemCard(item = item, onReuse = { onReuse(item.id) })
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportScreen(state: AppSnapshot, onBack: () -> Unit, onCancel: () -> Unit) {
  val context = LocalContext.current
  val job = state.exportJobState

  Scaffold(
    containerColor = ClipyBackground,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("Export progress") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
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
          job.status == "Success" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              AssistChip(
                onClick = {},
                label = { Text("Saved locally") },
                leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(ClipySuccess)) },
              )
              AssistChip(onClick = { shareUri(context, Uri.parse(state.history.firstOrNull()?.outputUri ?: "")) }, label = { Text("Share") })
              AssistChip(onClick = { openUri(context, Uri.parse(state.history.firstOrNull()?.outputUri ?: "")) }, label = { Text("Open") })
            }
          }
          job.isCancellable -> {
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
              Text("Cancel export")
            }
          }
          job.status == "Cancelled" -> {
            Text("Export was cancelled before saving the final output.", color = ClipyMuted)
          }
        }
      }
    }
  }
}

@Composable
private fun HistoryItemCard(item: ExportRecordUi, onReuse: () -> Unit) {
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
      OutlinedButton(onClick = { shareUri(context, Uri.parse(item.outputUri)) }, modifier = Modifier.weight(1f)) {
        Icon(Icons.Rounded.Share, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Share")
      }
      OutlinedButton(onClick = { openUri(context, Uri.parse(item.outputUri)) }, modifier = Modifier.weight(1f)) {
        Icon(Icons.Rounded.OpenInNew, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Open")
      }
    }
    Spacer(Modifier.height(10.dp))
    Button(onClick = onReuse, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary)) {
      Text("Reuse settings")
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
private fun RangeRow(label: String, value: Long, suffix: String, presets: List<Long>, onSelected: (Long) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("$label: $value $suffix", style = MaterialTheme.typography.titleMedium)
    ChipRow(items = presets, selected = value, label = { "$it" }, onSelected = onSelected)
  }
}

@Composable
private fun <T> ChipRow(items: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit) {
  Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items.forEach { item ->
      FilterChip(selected = item == selected, onClick = { onSelected(item) }, label = { Text(label(item)) })
    }
  }
}

@Composable
private fun LanguageCard(language: AppLanguage, selected: Boolean, onClick: () -> Unit) {
  val borderColor = if (selected) ClipyPrimary else Color.Transparent
  val label = if (language == AppLanguage.English) "English" else "Tiếng Việt"
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
    Text(label, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(6.dp))
    Text(if (language == AppLanguage.English) "Fast workflow copy in English" else "Giao diện thao tác nhanh bằng tiếng Việt", color = ClipyMuted)
  }
}

@Composable
private fun ClipySurfaceVariant(): Color = MaterialTheme.colorScheme.surfaceVariant

private fun exportSummary(state: AppSnapshot): String {
  val draft = state.draft
  return if (draft.exportFormat == ExportFormat.Gif) {
    "GIF • ${draft.cropRatio.label} • ${draft.gifFps} FPS • ${draft.gifResolution}"
  } else {
    "MP4 • ${draft.cropRatio.label} • ${draft.mp4Quality.label}"
  }
}

private fun shareUri(context: android.content.Context, uri: Uri) {
  if (uri.toString().isBlank()) return
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = "video/*"
    putExtra(Intent.EXTRA_STREAM, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  try {
    context.startActivity(Intent.createChooser(intent, "Share export"))
  } catch (_: ActivityNotFoundException) {
    Toast.makeText(context, "No app available to share this export.", Toast.LENGTH_SHORT).show()
  }
}

private fun openUri(context: android.content.Context, uri: Uri) {
  if (uri.toString().isBlank()) return
  val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, "video/*")
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  try {
    context.startActivity(intent)
  } catch (_: ActivityNotFoundException) {
    Toast.makeText(context, "No app available to open this export.", Toast.LENGTH_SHORT).show()
  }
}
