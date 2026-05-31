package com.nantcompany.clipy.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import com.nantcompany.clipy.theme.ClipyDesignTokens

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun VideoPlayerScreen(
    videoPath: String?,
    onBack: () -> Unit
) {
    if (videoPath.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("No video selected", color = Color.White)
        }
        return
    }

    val context = LocalContext.current
    val file = remember(videoPath) { File(videoPath) }
    val uri = remember(videoPath) { Uri.fromFile(file) }
    val player = remember(videoPath, context) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
        }
    }

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var controlsVisible by remember { mutableStateOf(true) }
    val isAudio = remember(file.name) {
        val lower = file.name.lowercase()
        lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".wav")
    }

    LaunchedEffect(player) {
        while (true) {
            currentPositionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it > 0L } ?: 0L
            delay(250)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(isPlaying, controlsVisible) {
        if (isPlaying && controlsVisible) {
            delay(2800)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF020617), Color.Black, Color(0xFF090D1D))
                )
            )
            .pointerInput(player) {
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible },
                    onDoubleTap = { offset ->
                        val target = if (offset.x < size.width / 2f) {
                            player.currentPosition - 10_000L
                        } else {
                            player.currentPosition + 10_000L
                        }
                        val max = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                        player.seekTo(target.coerceIn(0L, max))
                        controlsVisible = true
                    }
                )
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowFastForwardButton(true)
                    setShowRewindButton(true)
                    setShowSubtitleButton(false)
                    this.player = player
                }
            },
            update = { view ->
                view.player = player
            }
        )

        if (isAudio) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ClipyDesignTokens.primaryAccent.copy(alpha = 0.16f),
                    modifier = Modifier.size(108.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = ClipyDesignTokens.primaryAccent,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }
                Text(
                    text = file.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (controlsVisible) {
            PlayerTopBar(
                fileName = file.name,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            PlayerControls(
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                onTogglePlay = {
                    if (player.isPlaying) player.pause() else player.play()
                    controlsVisible = true
                },
                onSeekBy = { deltaMs ->
                    val max = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                    player.seekTo((player.currentPosition + deltaMs).coerceIn(0L, max))
                    controlsVisible = true
                },
                onSeekTo = { positionMs ->
                    player.seekTo(positionMs)
                    controlsVisible = true
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PlayerTopBar(
    fileName: String,
    currentPositionMs: Long,
    durationMs: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xDD000000), Color(0x66000000), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = Color(0x66000000)) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = fileName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                    color = Color(0xFFD9E1EA),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onTogglePlay: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xAA000000), Color(0xEE000000))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Slider(
            value = currentPositionMs.toFloat(),
            onValueChange = { onSeekTo(it.toLong()) },
            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = ClipyDesignTokens.primaryAccent,
                activeTrackColor = ClipyDesignTokens.primaryAccent,
                inactiveTrackColor = Color.White.copy(alpha = 0.22f)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatTime(currentPositionMs), color = Color.White, style = MaterialTheme.typography.labelMedium)
            Text(formatTime(durationMs), color = ClipyDesignTokens.secondaryText, style = MaterialTheme.typography.labelMedium)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0x660F172A)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ControlPill(label = "-10s", onClick = { onSeekBy(-10_000L) })
                PlayPauseButton(isPlaying = isPlaying, onClick = onTogglePlay)
                ControlPill(label = "+10s", onClick = { onSeekBy(10_000L) })
            }
        }
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(68.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = ClipyDesignTokens.primaryAccent
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isPlaying) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(
                        modifier = Modifier
                            .size(width = 7.dp, height = 28.dp)
                            .background(Color.Black, RoundedCornerShape(2.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 7.dp, height = 28.dp)
                            .background(Color.Black, RoundedCornerShape(2.dp))
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun ControlPill(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(82.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs.coerceAtLeast(0L) / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}
