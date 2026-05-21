package com.nantcompany.clipy.tools.speed

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.design.ClipySecondaryButton
import com.nantcompany.clipy.design.ClipySectionTitle
import com.nantcompany.clipy.design.ClipyVideoPlayer
import com.nantcompany.clipy.edit.tools.speed.SpeedRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File
import kotlinx.coroutines.delay

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun SpeedVideoScreen(
    inputPath: String?,
    onNavigate: (AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    viewModel: SpeedVideoViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(inputPath) {
        viewModel.setInputPath(inputPath)
    }

    val videoFile = remember(uiState.inputPath) { uiState.inputPath?.let(::File) }
    val videoUri = remember(uiState.inputPath) { videoFile?.let { Uri.fromFile(it) } }
    
    val player = remember(videoUri, context) {
        if (videoUri == null) null else ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            playWhenReady = false
            prepare()
        }
    }

    var durationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect
        while (true) {
            durationMs = player.duration.takeIf { it > 0L } ?: 0L
            if (durationMs > 0L) break
            delay(100)
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
        title = "Speed Control",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
                colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Video Preview", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    
                    if (player != null) {
                        ClipyVideoPlayer(
                            player = player,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(260.dp).background(Color.Black, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No video selected", color = ClipyDesignTokens.secondaryText)
                        }
                    }
                    
                    if (durationMs > 0L) {
                        val newDurationMs = (durationMs / uiState.speedFactor).toLong()
                        Text(
                            "New Duration: ${formatTime(newDurationMs)} (Original: ${formatTime(durationMs)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClipyDesignTokens.primaryAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ClipySectionTitle(text = "Speed Factor: ${"%.2f".format(uiState.speedFactor)}x")
                
                Slider(
                    value = uiState.speedFactor,
                    onValueChange = { viewModel.setSpeedFactor(it) },
                    valueRange = 0.25f..4.0f,
                    steps = 0,
                    colors = SliderDefaults.colors(
                        thumbColor = ClipyDesignTokens.primaryAccent,
                        activeTrackColor = ClipyDesignTokens.primaryAccent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(0.5f, 1.0f, 2.0f, 4.0f).forEach { preset ->
                        val isSelected = uiState.speedFactor == preset
                        ClipySecondaryButton(
                            modifier = Modifier.weight(1f),
                            label = "${preset}x", 
                            enabled = !isSelected,
                            onClick = { viewModel.setSpeedFactor(preset) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            ClipyPrimaryButton(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                label = "Export Video",
                enabled = !inputPath.isNullOrBlank(),
                onClick = {
                    val input = inputPath ?: return@ClipyPrimaryButton
                    val request = SpeedRequest(
                        inputPath = input,
                        outputPath = MediaFileUtils.createOutputPath(context, "speed", "mp4"),
                        speedFactor = uiState.speedFactor
                    )
                    onSubmitRequest(ProcessingRequest.Speed(request))
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
