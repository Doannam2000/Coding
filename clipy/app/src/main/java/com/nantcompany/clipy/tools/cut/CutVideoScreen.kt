package com.nantcompany.clipy.tools.cut

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.edit.tools.cut.CutRequest
import com.nantcompany.clipy.edit.tools.cut.CutType
import com.nantcompany.clipy.export.job.ProcessingRequest
import kotlinx.coroutines.delay
import java.io.File

// Design Tokens (Refined)
val SurfaceBackground = Color(0xFF0A0A0A)
val SurfaceContainer = Color(0xFF1A1A1A)
val SurfaceContainerLow = Color(0xFF121212)
val AccentCyan = Color(0xFF00E5FF)
val TextMuted = Color(0xFF94A3B8)

@OptIn(UnstableApi::class)
@Composable
fun CutVideoScreen(
    inputPath: String?,
    onNavigate: (com.nantcompany.clipy.navigation.AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var cutType by remember { mutableStateOf(CutType.TRIM) }
    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(0L) }
    var currentMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    
    val videoFile = remember(inputPath) { inputPath?.let(::File) }
    val videoUri = remember(inputPath) { videoFile?.let { Uri.fromFile(it) } }
    
    val player = remember(videoUri) {
        if (videoUri == null) null else ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            playWhenReady = false
            prepare()
        }
    }

    LaunchedEffect(player, cutType, startMs, endMs) {
        if (player == null) return@LaunchedEffect
        while (true) {
            currentMs = player.currentPosition.coerceAtLeast(0L)
            
            if (player.isPlaying) {
                if (cutType == CutType.TRIM) {
                    if (currentMs >= endMs && endMs > 0) {
                        player.seekTo(startMs)
                    }
                    if (currentMs < startMs) {
                        player.seekTo(startMs)
                    }
                } else {
                    // CUT mode: Skip selection
                    if (currentMs >= startMs && currentMs < endMs) {
                        player.seekTo(endMs)
                    }
                }
            }
            delay(30)
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    player?.duration?.let {
                        if (it > 0) {
                            durationMs = it
                            if (endMs == 0L) endMs = it
                        }
                    }
                }
            }
        }
        player?.addListener(listener)
        
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player?.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            player?.removeListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
            player?.release()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SurfaceBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onNavigate(com.nantcompany.clipy.navigation.AppRoute.HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(0.1f)))
                    IconButton(onClick = {
                        startMs = 0L
                        endMs = durationMs
                        player?.seekTo(0)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                
                IconButton(
                    onClick = {
                        val input = inputPath ?: return@IconButton
                        val request = CutRequest(
                            inputPath = input,
                            outputPath = MediaFileUtils.createOutputPath(context, "cut", "mp4"),
                            startMs = startMs,
                            endMs = endMs,
                            type = cutType
                        )
                        onSubmitRequest(ProcessingRequest.Cut(request))
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            // Video Preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .shadow(20.dp, RoundedCornerShape(24.dp))
                ) {
                    if (player != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = false
                                    this.player = player
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    // Play/Pause Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .background(Color.White.copy(0.15f), CircleShape)
                            .border(1.dp, Color.White.copy(0.2f), CircleShape)
                            .clickable {
                                if (player?.isPlaying == true) player.pause() else player?.play()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (player?.isPlaying == true) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.width(8.dp).height(32.dp).background(Color.White, RoundedCornerShape(2.dp)))
                                Box(modifier = Modifier.width(8.dp).height(32.dp).background(Color.White, RoundedCornerShape(2.dp)))
                            }
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            // Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(SurfaceContainerLow)
                    .padding(top = 24.dp, bottom = 48.dp)
            ) {
                // Mode Toggle
                ModeToggle(
                    selectedType = cutType,
                    onTypeSelected = { cutType = it },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Time Display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = formatMs(currentMs),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            letterSpacing = (-1).sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "TOTAL ${formatMs(durationMs)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp
                        ),
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Timeline View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    if (videoUri != null) {
                        AndroidView(
                            factory = { ctx ->
                                TimelineView(ctx).apply {
                                    callback = object : TimelineView.Callback {
                                        override fun onSeek(position: Float, seekMillis: Long) {
                                            player?.seekTo(seekMillis)
                                        }
                                        override fun onSeekStart(position: Float, seekMillis: Long) {
                                            player?.pause()
                                        }
                                        override fun onStopSeek(position: Float, seekMillis: Long) {
                                            // No-op
                                        }
                                        override fun onLeftProgress(leftPos: Float, seekMillis: Long) {
                                            startMs = seekMillis
                                            player?.seekTo(seekMillis)
                                        }
                                        override fun onRightProgress(rightPos: Float, seekMillis: Long) {
                                            endMs = seekMillis
                                            player?.seekTo(seekMillis)
                                        }
                                    }
                                    setVideoUri(videoUri)
                                }
                            },
                            update = { view ->
                                // Sync initial state once duration is known
                                if (durationMs > 0 && endMs == 0L) {
                                    endMs = durationMs
                                }
                                
                                if (durationMs > 0) {
                                    view.totalDuration = durationMs
                                }
                                view.setCutMode(if (cutType == CutType.TRIM) TimelineView.CutMode.TRIM else TimelineView.CutMode.CUT)
                                view.setCurrentProgressValue(if (durationMs > 0) currentMs.toFloat() / durationMs else 0f)
                                if (durationMs > 0) {
                                    view.updateProgress(startMs.toFloat() / durationMs, endMs.toFloat() / durationMs)
                                }
                                
                                // Update duration if view found it first
                                if (durationMs == 0L && view.totalDuration > 0) {
                                    durationMs = view.totalDuration
                                    if (endMs == 0L) endMs = view.totalDuration
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Filename
                Text(
                    text = videoFile?.name?.uppercase() ?: "VID001.MP4",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp
                    ),
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
    
}

@Composable
fun ModeToggle(
    selectedType: CutType,
    onTypeSelected: (CutType) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(180.dp)
            .height(36.dp)
            .background(SurfaceContainer, RoundedCornerShape(18.dp))
            .padding(4.dp)
    ) {
        val trimSelected = selectedType == CutType.TRIM
        val animatedOffset by animateDpAsState(targetValue = if (trimSelected) 0.dp else 86.dp)
        
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .fillMaxHeight()
                .width(86.dp)
                .background(AccentCyan, RoundedCornerShape(16.dp))
        )
        
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable { onTypeSelected(CutType.TRIM) },
                contentAlignment = Alignment.Center
            ) {
                Text("Trim", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (trimSelected) Color.Black else TextMuted)
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable { onTypeSelected(CutType.CUT) },
                contentAlignment = Alignment.Center
            ) {
                Text("Cut", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (!trimSelected) Color.Black else TextMuted)
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (ms % 1000) / 100
    return "%d:%02d.%d".format(minutes, seconds, tenths)
}
