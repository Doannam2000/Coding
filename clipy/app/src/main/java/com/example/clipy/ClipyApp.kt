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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.example.clipy.clipy.data.ClipyRepository.AppSnapshot
import com.example.clipy.clipy.model.AppLanguage
import com.example.clipy.clipy.model.CropRatio
import com.example.clipy.clipy.model.ExportFormat
import com.example.clipy.clipy.model.ExportRecordUi
import com.example.clipy.clipy.model.Mp4Quality
import com.example.clipy.clipy.model.SaveBehavior
import com.example.clipy.clipy.model.UserPreferences
import com.example.clipy.clipy.model.WatermarkPosition
import com.example.clipy.clipy.model.exportMimeType
import com.example.clipy.clipy.model.mimeType
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
private const val LANGUAGE = "language"

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
          if (viewModel.startExport()) {
            navController.navigate(EXPORT)
          }
        },
        onExit = finishApp,
      )
    }
    composable(SETTINGS) {
      SettingsScreen(
        preferences = state.preferences,
        onBack = navController::popBackStack,
        onSave = viewModel::saveSettings,
        onOpenLanguage = { navController.navigate(LANGUAGE) },
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
private fun IntroScreen(selectedLanguage: String, onContinue: (AppLanguage) -> Unit) {
  var page by rememberSaveable { mutableStateOf(0) }
  var language by rememberSaveable { mutableStateOf(AppLanguage.entries.first { it.code == selectedLanguage }) }
  val cards = listOf(
    stringResource(R.string.onboarding_trim_title) to stringResource(R.string.onboarding_trim_body),
    stringResource(R.string.onboarding_frame_title) to stringResource(R.string.onboarding_frame_body),
    stringResource(R.string.onboarding_export_title) to stringResource(R.string.onboarding_export_body),
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
        Text(stringResource(R.string.intro_language_title), style = MaterialTheme.typography.titleLarge)
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
      runCatching {
        context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
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
          IconButton(onClick = onOpenHistory) { Icon(Icons.Rounded.History, contentDescription = stringResource(R.string.nav_history)) }
          IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.nav_settings)) }
          IconButton(onClick = { showExitDialog = true }) { Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = stringResource(R.string.nav_exit)) }
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
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { picker.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth()) {
          Icon(Icons.Rounded.FolderOpen, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.editor_pick_video))
        }
      }

      SectionCard(title = stringResource(R.string.section_trim)) {
        Text(stringResource(R.string.section_trim_hint), color = ClipyMuted)
        Spacer(Modifier.height(12.dp))
        RangeRow(label = stringResource(R.string.trim_start), value = draft.trimStartMs, suffix = stringResource(R.string.time_ms), presets = listOf(0L, 1000L, 2000L, 3000L), onSelected = onTrimStartChange)
        Spacer(Modifier.height(12.dp))
        RangeRow(label = stringResource(R.string.trim_end), value = draft.trimEndMs, suffix = stringResource(R.string.time_ms), presets = listOf(6000L, 9000L, 12000L, 15000L), onSelected = onTrimEndChange)
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

  if (showExitDialog) {
    AlertDialog(
      onDismissRequest = { showExitDialog = false },
      confirmButton = { TextButton(onClick = onExit) { Text(stringResource(R.string.nav_exit)) } },
      dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text(stringResource(R.string.dialog_stay)) } },
      title = { Text(stringResource(R.string.dialog_exit_title)) },
      text = { Text(stringResource(R.string.dialog_exit_body)) },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
  preferences: UserPreferences,
  onBack: () -> Unit,
  onSave: (UserPreferences) -> Unit,
  onOpenLanguage: () -> Unit,
  onExit: () -> Unit,
) {
  val context = LocalContext.current
  var edited by remember(preferences) { mutableStateOf(preferences) }
  var confirmExit by rememberSaveable { mutableStateOf(false) }

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
      SectionCard(title = stringResource(R.string.settings_about)) {
        Text(stringResource(R.string.settings_about_body), color = ClipyMuted)
        Spacer(Modifier.height(8.dp))
        Text(settingsVersionLabel(), color = ClipyMuted, style = MaterialTheme.typography.bodyMedium)
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
private fun HistoryScreen(state: AppSnapshot, onBack: () -> Unit, onReuse: (Long) -> Unit) {
  Scaffold(
    containerColor = ClipyBackground,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.history_title)) },
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
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (state.history.isEmpty()) {
        PremiumCard {
          Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.titleLarge)
          Spacer(Modifier.height(8.dp))
          Text(stringResource(R.string.history_empty_body), color = ClipyMuted)
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
  val latestExport = latestExportRecord(state)
  val saveBehavior = saveBehaviorLabel(state.preferences.saveBehavior)

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
                 onClick = { shareUri(context, Uri.parse(latestExport?.outputUri.orEmpty()), state.draft.exportFormat.mimeType()) },
                 label = { Text(stringResource(R.string.share)) },
               )
               AssistChip(
                 onClick = { openUri(context, Uri.parse(latestExport?.outputUri.orEmpty()), state.draft.exportFormat.mimeType()) },
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
    Spacer(Modifier.height(10.dp))
    Button(onClick = onReuse, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary)) {
      Text(stringResource(R.string.history_reuse))
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
    ChipRow(items = presets, selected = value, label = { Text("$it") }, onSelected = onSelected)
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
