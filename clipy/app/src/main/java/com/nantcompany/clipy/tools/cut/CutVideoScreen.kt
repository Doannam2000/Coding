package com.nantcompany.clipy.tools.cut

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.design.*
import com.nantcompany.clipy.edit.tools.cut.CutRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.export.model.VideoClip
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File
import kotlinx.coroutines.delay

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun CutVideoScreen(
    inputPath: String?,
    onNavigate: (AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    viewModel: CutVideoViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(inputPath) {
        viewModel.setInputPath(inputPath)
    }

    val videoFile = remember(uiState.inputPath) { uiState.inputPath?.let(::File) }
    val videoExists = remember(uiState.inputPath) { videoFile?.exists() == true }
    val videoUri = remember(uiState.inputPath, videoExists) { if (videoExists && videoFile != null) Uri.fromFile(videoFile) else null }
    
    val player = remember(videoUri, context) {
        if (videoUri == null) null else ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            playWhenReady = false
            prepare()
        }
    }

    var currentMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect
        while (true) {
            currentMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it > 0L } ?: 0L
            delay(50)
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player?.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player?.release()
        }
    }

    ClipyScaffold(
        title = "Cut Video",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (player != null) {
                ClipyVideoPlayer(
                    player = player,
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(260.dp).background(Color.Black, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Text("Loading video...", color = ClipyDesignTokens.secondaryText)
                }
            }

            Text("Adjust Timeline", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            
            if (inputPath != null) {
                ClipyTimeline(
                    clips = listOf(VideoClip(path = inputPath, durationMs = durationMs, endMs = durationMs)),
                    durationMs = durationMs,
                    currentMs = currentMs,
                    onSeek = { player?.seekTo(it) },
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Start Point", style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.secondaryText)
                        Text(formatTime(uiState.startMs), style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    ClipySecondaryButton(label = "Set Start", onClick = { viewModel.setStartMs(currentMs) })
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("End Point", style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.secondaryText)
                        Text(formatTime(uiState.endMs), style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    ClipySecondaryButton(label = "Set End", onClick = { viewModel.setEndMs(currentMs) })
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            ClipyPrimaryButton(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                label = "Export Selected Range",
                enabled = !inputPath.isNullOrBlank() && uiState.endMs > uiState.startMs,
                onClick = {
                    val input = inputPath ?: return@ClipyPrimaryButton
                    val request = CutRequest(
                        inputPath = input,
                        outputPath = MediaFileUtils.createOutputPath(context, "cut", "mp4"),
                        startMs = uiState.startMs,
                        endMs = uiState.endMs
                    )
                    onSubmitRequest(ProcessingRequest.Cut(request))
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000) % 60; val m = s / 60
    return "%02d:%02d".format(m, s)
}
