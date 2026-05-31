package com.nantcompany.clipy.editor

import android.graphics.ColorMatrix
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMicros
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.app.ProEditorState
import com.nantcompany.clipy.app.ProEditorViewModel
import com.nantcompany.clipy.design.ClipyEmptyState
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.design.ClipyTextField
import com.nantcompany.clipy.design.ClipyVideoPlayer
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.export.job.StudioRequest
import com.nantcompany.clipy.export.model.TextLayer
import com.nantcompany.clipy.filters.gpu.ClipyFilterType
import com.nantcompany.clipy.filters.gpu.ClipyGpuFilterManager
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

private val EditorBg = Color(0xFF020611)
private val EditorPanel = Color(0xFF080D18)
private val EditorPanelAlt = Color(0xFF0D1422)
private val EditorTrack = Color(0xFF050913)
private val EditorBorder = Color.White.copy(alpha = 0.10f)
private val EditorCyan = Color(0xFF38BDF8)
private val EditorAmber = Color(0xFFFBBF24)

private enum class EditorTool(val label: String, val icon: ImageVector) {
    EDIT("Edit", Icons.Default.Build),
    AUDIO("Audio", Icons.Default.PlayArrow),
    TEXT("Text", Icons.Default.Edit),
    OVERLAY("Overlay", Icons.Default.KeyboardArrowDown),
    EFFECTS("Effects", Icons.Default.Star),
    STICKERS("Stickers", Icons.Default.Add),
    FILTERS("Filters", Icons.Default.Settings)
}

@OptIn(UnstableApi::class)
@Composable
fun ProEditorScreen(
    onNavigate: (AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    viewModel: ProEditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var activeTool by remember { mutableStateOf(EditorTool.EDIT) }
    var currentMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var previewWidth by remember { mutableFloatStateOf(1f) }
    var previewHeight by remember { mutableFloatStateOf(1f) }
    val splitMarks = remember(state.videoPath) { mutableStateListOf<Long>() }

    val videoUri = remember(state.videoPath) {
        state.videoPath?.let { Uri.fromFile(File(it)) }
    }
    val player = remember(videoUri) {
        if (videoUri == null) return@remember null
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        }
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
            prepare()
        }
    }

    val previewColorMatrix = remember(
        state.brightness,
        state.contrast,
        state.saturation,
        state.selectedFilter
    ) {
        ClipyGpuFilterManager.createPreviewColorMatrix(
            brightness = state.brightness,
            contrast = state.contrast,
            saturation = state.saturation,
            filterType = state.selectedFilter
        )
    }

    DisposableEffect(player) {
        if (player == null) {
            onDispose { }
        } else {
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
    }

    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect
        while (true) {
            currentMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(0L)
            delay(33)
        }
    }

    LaunchedEffect(durationMs) {
        if (durationMs > 0L && state.endMs == 0L) {
            viewModel.setDuration(durationMs)
        }
    }

    LaunchedEffect(state.speedFactor, player) {
        player?.setPlaybackSpeed(state.speedFactor)
    }

    LaunchedEffect(state.volume, player) {
        player?.volume = state.volume
    }

    fun togglePlayback() {
        val p = player ?: return
        if (p.isPlaying || p.playWhenReady) {
            p.pause()
        } else {
            if (p.playbackState == Player.STATE_ENDED) p.seekTo(0L)
            p.play()
        }
    }

    fun exportStudio() {
        val input = state.videoPath ?: return
        val safeDuration = durationMs.coerceAtLeast(state.durationMs)
        val end = when {
            state.endMs > state.startMs -> state.endMs
            safeDuration > 0L -> safeDuration
            else -> return
        }
        val textLayers = if (state.overlayText.isNotBlank()) {
            listOf(
                TextLayer(
                    id = "overlay_text",
                    text = state.overlayText,
                    x = state.textX,
                    y = state.textY,
                    startMs = 0L,
                    endMs = end,
                    color = android.graphics.Color.WHITE
                )
            )
        } else {
            emptyList()
        }

        onSubmitRequest(
            ProcessingRequest.Studio(
                StudioRequest(
                    inputPath = input,
                    outputPath = MediaFileUtils.createOutputPath(context, "studio", "mp4"),
                    startMs = state.startMs,
                    endMs = end,
                    rotation = state.rotation,
                    flipHorizontal = state.flipHorizontal,
                    brightness = state.brightness,
                    contrast = state.contrast,
                    saturation = state.saturation,
                    filterName = state.selectedFilter.name,
                    speedFactor = state.speedFactor,
                    textLayers = textLayers,
                    audioTracks = emptyList(),
                    mainVideoVolume = state.volume
                )
            )
        )
    }

    ClipyScaffold(showTopBar = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EditorBg)
        ) {
            EditorTopBar(
                currentMs = currentMs,
                canExport = state.videoPath != null && durationMs > 0L,
                onBack = { onNavigate(AppRoute.HOME) },
                onExport = ::exportStudio
            )

            if (state.videoPath == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ClipyEmptyState(
                        title = "No clip selected",
                        message = "Pick a video to open the studio editor.",
                        actionLabel = "Select video",
                        onAction = { onNavigate(AppRoute.PICK_VIDEO) }
                    )
                }
                return@ClipyScaffold
            }

            EditorPreviewStage(
                player = player,
                state = state,
                previewColorMatrix = previewColorMatrix,
                isPlaying = isPlaying,
                onPreviewSizeChanged = { width, height ->
                    previewWidth = width
                    previewHeight = height
                },
                previewWidth = previewWidth,
                previewHeight = previewHeight,
                onMoveText = viewModel::setTextPos,
                onPlayPause = ::togglePlayback,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )

            QuickActionStrip(
                onSplit = {
                    if (durationMs > 0L && currentMs in 250 until (durationMs - 250)) {
                        val exists = splitMarks.any { abs(it - currentMs) < 300L }
                        if (!exists) splitMarks.add(currentMs)
                    }
                },
                onSpeed = { activeTool = EditorTool.EDIT },
                onAnimation = { activeTool = EditorTool.EFFECTS },
                onDelete = {
                    if (state.overlayText.isNotBlank() && activeTool == EditorTool.TEXT) {
                        viewModel.setOverlayText("")
                    } else if (splitMarks.isNotEmpty()) {
                        splitMarks.removeLast()
                    }
                },
                onAudio = { activeTool = EditorTool.AUDIO }
            )

            EditorTimeline(
                videoPath = state.videoPath.orEmpty(),
                durationMs = durationMs,
                currentMs = currentMs,
                splitMarks = splitMarks,
                onSeek = { player?.seekTo(it) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            ToolPanel(
                activeTool = activeTool,
                state = state,
                durationMs = durationMs,
                onToolChange = { activeTool = it },
                onFilterChange = viewModel::setFilter,
                onAdjustmentsChange = viewModel::setAdjustments,
                onSpeedChange = viewModel::setSpeed,
                onVolumeChange = viewModel::setVolume,
                onTextChange = viewModel::setOverlayText,
                onRotate = { viewModel.setTransform((state.rotation + 90) % 360, state.flipHorizontal) },
                onFlip = { viewModel.setTransform(state.rotation, !state.flipHorizontal) }
            )
        }
    }
}

@Composable
private fun EditorTopBar(
    currentMs: Long,
    canExport: Boolean,
    onBack: () -> Unit,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close editor", tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Text(
            text = formatClock(currentMs),
            modifier = Modifier.weight(1f),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.06f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("1080P", color = EditorCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = ClipyDesignTokens.textMuted, modifier = Modifier.size(14.dp))
            }
        }
        Button(
            onClick = onExport,
            enabled = canExport,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContainerColor = Color.White.copy(alpha = 0.24f),
                disabledContentColor = Color.Black.copy(alpha = 0.45f)
            ),
            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 0.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text("Export", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(5.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp).graphicsLayer(rotationZ = 90f))
        }
    }
}

@Composable
private fun EditorPreviewStage(
    player: ExoPlayer?,
    state: ProEditorState,
    previewColorMatrix: ColorMatrix?,
    isPlaying: Boolean,
    previewWidth: Float,
    previewHeight: Float,
    onPreviewSizeChanged: (Float, Float) -> Unit,
    onMoveText: (Float, Float) -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF07111B), Color(0xFF02040A))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.80f)
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xFF010309))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(30.dp))
                .padding(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .onSizeChanged {
                        onPreviewSizeChanged(it.width.toFloat(), it.height.toFloat())
                    },
                contentAlignment = Alignment.Center
            ) {
                if (player != null) {
                    ClipyVideoPlayer(
                        player = player,
                        previewColorMatrix = previewColorMatrix,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = state.rotation.toFloat()
                                scaleX = if (state.flipHorizontal) -1f else 1f
                            }
                    )
                }
                if (state.overlayText.isNotBlank()) {
                    DraggableOverlayText(
                        text = state.overlayText,
                        x = state.textX,
                        y = state.textY,
                        previewWidth = previewWidth,
                        previewHeight = previewHeight,
                        onMove = onMoveText
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 7.dp)
                        .width(54.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .width(44.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.White.copy(alpha = 0.34f))
                )
            }
        }
        PreviewControlDock(
            isPlaying = isPlaying,
            onPlayPause = onPlayPause,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun DraggableOverlayText(
    text: String,
    x: Float,
    y: Float,
    previewWidth: Float,
    previewHeight: Float,
    onMove: (Float, Float) -> Unit
) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .offset {
                IntOffset(
                    (x / 100f * previewWidth).roundToInt(),
                    (y / 100f * previewHeight).roundToInt()
                )
            }
            .pointerInput(previewWidth, previewHeight) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onMove(
                        (x + drag.x / previewWidth * 100f).coerceIn(0f, 92f),
                        (y + drag.y / previewHeight * 100f).coerceIn(0f, 92f)
                    )
                }
            }
            .background(Color.Black.copy(alpha = 0.44f), RoundedCornerShape(9.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    )
}

@Composable
private fun PreviewControlDock(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(bottom = 8.dp),
        shape = RoundedCornerShape(99.dp),
        color = Color(0xD9040710),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallEditorIcon(Icons.Default.Refresh, "Undo", Color.White.copy(alpha = 0.82f)) {}
            IconButton(onClick = onPlayPause, modifier = Modifier.size(32.dp)) {
                if (isPlaying) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(width = 4.dp, height = 17.dp).background(Color.White, RoundedCornerShape(2.dp)))
                        Box(Modifier.size(width = 4.dp, height = 17.dp).background(Color.White, RoundedCornerShape(2.dp)))
                    }
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play preview", tint = Color.White, modifier = Modifier.size(23.dp))
                }
            }
            SmallEditorIcon(Icons.Default.Refresh, "Redo", Color.White.copy(alpha = 0.82f)) {}
            SmallEditorIcon(Icons.Default.Settings, "Preview options", Color.White.copy(alpha = 0.82f)) {}
        }
    }
}

@Composable
private fun QuickActionStrip(
    onSplit: () -> Unit,
    onSpeed: () -> Unit,
    onAnimation: () -> Unit,
    onDelete: () -> Unit,
    onAudio: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        EditorActionButton("Split", Icons.Default.Build, onSplit, modifier = Modifier.weight(1f))
        EditorActionButton("Speed", Icons.Default.Refresh, onSpeed, modifier = Modifier.weight(1f))
        EditorActionButton("Animation", Icons.Default.Star, onAnimation, modifier = Modifier.weight(1.25f))
        EditorActionButton("Delete", Icons.Default.Delete, onDelete, modifier = Modifier.weight(1f))
        Surface(
            modifier = Modifier
                .width(74.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .clickable(onClick = onAudio),
            shape = RoundedCornerShape(9.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add\naudio", color = Color.Black, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Black, lineHeight = 10.sp)
            }
        }
    }
}

@Composable
private fun EditorActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(17.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EditorTimeline(
    videoPath: String,
    durationMs: Long,
    currentMs: Long,
    splitMarks: List<Long>,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var trackWidth by remember { mutableFloatStateOf(1f) }
    val thumbnailCount = remember(trackWidth, durationMs) {
        ((trackWidth / 92f).roundToInt()).coerceIn(4, 10)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(116.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(formatClock(0L), color = ClipyDesignTokens.textMuted, style = MaterialTheme.typography.labelSmall)
            Text(formatClock(durationMs / 2), color = ClipyDesignTokens.textMuted, style = MaterialTheme.typography.labelSmall)
            Text(formatClock(durationMs), color = ClipyDesignTokens.textMuted, style = MaterialTheme.typography.labelSmall)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(EditorTrack)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                .onSizeChanged { trackWidth = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(durationMs, trackWidth) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            if (durationMs > 0) onSeek((pos.x / trackWidth * durationMs).toLong().coerceIn(0L, durationMs))
                        },
                        onDrag = { change, _ ->
                            if (durationMs > 0) onSeek((change.position.x / trackWidth * durationMs).toLong().coerceIn(0L, durationMs))
                        }
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 28.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
            ) {
                repeat(thumbnailCount) { index ->
                    val frameMs = if (thumbnailCount <= 1) 0L else durationMs * index / (thumbnailCount - 1)
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(videoPath)
                            .decoderFactory(VideoFrameDecoder.Factory())
                            .videoFrameMicros(frameMs * 1000L)
                            .size(140)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFF111827)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 28.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
            )
            splitMarks.forEach { mark ->
                if (durationMs > 0L) {
                    val x = (mark.toFloat() / durationMs.toFloat() * trackWidth).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x - 1, 20) }
                            .width(2.dp)
                            .height(56.dp)
                            .background(EditorAmber)
                    )
                }
            }
            if (durationMs > 0L) {
                val x = (currentMs.toFloat() / durationMs.toFloat() * trackWidth).roundToInt()
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x - 2, 0) }
                        .width(4.dp)
                        .height(92.dp)
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x - 9, 0) }
                        .size(width = 18.dp, height = 12.dp)
                        .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
private fun ToolPanel(
    activeTool: EditorTool,
    state: ProEditorState,
    durationMs: Long,
    onToolChange: (EditorTool) -> Unit,
    onFilterChange: (ClipyFilterType) -> Unit,
    onAdjustmentsChange: (Float, Float, Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTextChange: (String) -> Unit,
    onRotate: () -> Unit,
    onFlip: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(182.dp),
        color = EditorPanel,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                when (activeTool) {
                    EditorTool.EDIT -> EditPanel(
                        state = state,
                        durationMs = durationMs,
                        onSpeedChange = onSpeedChange,
                        onRotate = onRotate,
                        onFlip = onFlip
                    )
                    EditorTool.AUDIO -> AudioPanel(volume = state.volume, onVolumeChange = onVolumeChange)
                    EditorTool.TEXT -> TextPanel(text = state.overlayText, onTextChange = onTextChange)
                    EditorTool.OVERLAY -> OverlayPanel()
                    EditorTool.EFFECTS -> EffectPanel(selected = state.selectedFilter, onSelect = onFilterChange)
                    EditorTool.STICKERS -> StickerPanel()
                    EditorTool.FILTERS -> FilterPanel(
                        state = state,
                        onSelect = onFilterChange,
                        onAdjustmentsChange = onAdjustmentsChange
                    )
                }
            }
            BottomToolDock(activeTool = activeTool, onToolChange = onToolChange)
        }
    }
}

@Composable
private fun EditPanel(
    state: ProEditorState,
    durationMs: Long,
    onSpeedChange: (Float) -> Unit,
    onRotate: () -> Unit,
    onFlip: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            EditorChip("Speed ${formatSpeed(state.speedFactor)}", active = true, accent = EditorAmber)
            EditorChip("Rotate ${state.rotation}deg", active = state.rotation != 0, onClick = onRotate)
            EditorChip("Flip", active = state.flipHorizontal, onClick = onFlip)
            EditorChip(formatClock(durationMs), active = false)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)) { speed ->
                EditorChip(
                    label = formatSpeed(speed),
                    active = state.speedFactor == speed,
                    accent = EditorCyan,
                    onClick = { onSpeedChange(speed) }
                )
            }
        }
    }
}

@Composable
private fun AudioPanel(volume: Float, onVolumeChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Main audio", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        LabeledSlider(
            label = "Volume",
            value = volume,
            valueRange = 0f..1.5f,
            valueText = "${(volume * 100).roundToInt()}%",
            onValueChange = onVolumeChange
        )
    }
}

@Composable
private fun TextPanel(text: String, onTextChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ClipyTextField(value = text, onValueChange = onTextChange, placeholder = "Add title text")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EditorChip("Title", active = text.isNotBlank(), onClick = { onTextChange(if (text.isBlank()) "Clipy title" else text) })
            EditorChip("Clear", active = false, onClick = { onTextChange("") })
        }
    }
}

@Composable
private fun OverlayPanel() {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(listOf("Frame", "Glow", "Safe area", "Grid", "Caption bar")) { item ->
            PreviewTile(label = item, accent = overlayAccent(item), active = item == "Frame")
        }
    }
}

@Composable
private fun StickerPanel() {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(listOf("Badge", "Arrow", "Circle", "Label", "Spark")) { item ->
            PreviewTile(label = item, accent = overlayAccent(item), active = item == "Badge")
        }
    }
}

@Composable
private fun EffectPanel(
    selected: ClipyFilterType,
    onSelect: (ClipyFilterType) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(listOf(ClipyFilterType.NONE, ClipyFilterType.DRAMATIC, ClipyFilterType.CYBERPUNK, ClipyFilterType.VINTAGE, ClipyFilterType.LOMO, ClipyFilterType.TOON)) { filter ->
            PreviewTile(
                label = filter.displayName,
                accent = filterAccent(filter),
                active = selected == filter,
                onClick = { onSelect(filter) }
            )
        }
    }
}

@Composable
private fun FilterPanel(
    state: ProEditorState,
    onSelect: (ClipyFilterType) -> Unit,
    onAdjustmentsChange: (Float, Float, Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ClipyFilterType.entries) { filter ->
                PreviewTile(
                    label = filter.displayName,
                    accent = filterAccent(filter),
                    active = state.selectedFilter == filter,
                    onClick = { onSelect(filter) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniAdjust("Bright", state.brightness, -0.5f..0.5f, Modifier.weight(1f)) {
                onAdjustmentsChange(it, state.contrast, state.saturation)
            }
            MiniAdjust("Contrast", state.contrast, -0.5f..0.5f, Modifier.weight(1f)) {
                onAdjustmentsChange(state.brightness, it, state.saturation)
            }
        }
    }
}

@Composable
private fun BottomToolDock(activeTool: EditorTool, onToolChange: (EditorTool) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(Color(0xF2050810))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        EditorTool.entries.forEach { tool ->
            val active = tool == activeTool
            Column(
                modifier = Modifier
                    .width(46.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) ClipyDesignTokens.primaryAccent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onToolChange(tool) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(tool.icon, contentDescription = tool.label, tint = if (active) ClipyDesignTokens.primaryAccent else Color.White, modifier = Modifier.size(17.dp))
                Spacer(Modifier.height(3.dp))
                Text(
                    tool.label,
                    color = if (active) ClipyDesignTokens.primaryAccent else Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PreviewTile(
    label: String,
    accent: Color,
    active: Boolean,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .width(78.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (active) accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) accent else Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(25.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.86f), Color.White.copy(alpha = 0.22f), accent.copy(alpha = 0.42f))
                        )
                    )
            )
            Text(
                label,
                color = if (active) Color.White else ClipyDesignTokens.secondaryText,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EditorChip(
    label: String,
    active: Boolean,
    accent: Color = ClipyDesignTokens.primaryAccent,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .height(29.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (active) accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, if (active) accent.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.08f))
    ) {
        Box(modifier = Modifier.padding(horizontal = 9.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (active) accent else ClipyDesignTokens.secondaryText,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    CompactSlider(
        label = label,
        value = value,
        valueRange = valueRange,
        valueText = valueText,
        accent = EditorCyan,
        onValueChange = onValueChange
    )
}

@Composable
private fun MiniAdjust(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onChange: (Float) -> Unit
) {
    CompactSlider(
        label = label,
        value = value,
        valueRange = range,
        valueText = "%.2f".format(value),
        accent = ClipyDesignTokens.primaryAccent,
        modifier = modifier,
        onValueChange = onChange
    )
}

@Composable
private fun CompactSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {
    var trackWidth by remember { mutableFloatStateOf(1f) }
    val span = valueRange.endInclusive - valueRange.start
    val fraction = if (span == 0f) 0f else ((value - valueRange.start) / span).coerceIn(0f, 1f)

    fun updateValue(x: Float) {
        val next = valueRange.start + (x / trackWidth).coerceIn(0f, 1f) * span
        onValueChange(next.coerceIn(valueRange.start, valueRange.endInclusive))
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                label,
                color = ClipyDesignTokens.secondaryText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                valueText,
                color = accent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .onSizeChanged { trackWidth = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(valueRange, trackWidth) {
                    detectDragGestures(
                        onDragStart = { updateValue(it.x) },
                        onDrag = { change, _ ->
                            change.consume()
                            updateValue(change.position.x)
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(accent)
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset((fraction * trackWidth).roundToInt() - 7, 3) }
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, accent, CircleShape)
            )
        }
    }
}

@Composable
private fun SmallEditorIcon(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(19.dp))
    }
}

private fun filterAccent(filter: ClipyFilterType): Color {
    return when (filter) {
        ClipyFilterType.NONE -> Color(0xFF64748B)
        ClipyFilterType.SEPIA,
        ClipyFilterType.WARM,
        ClipyFilterType.VINTAGE,
        ClipyFilterType.LOMO -> EditorAmber
        ClipyFilterType.COOL,
        ClipyFilterType.CYBERPUNK -> EditorCyan
        ClipyFilterType.GRAYSCALE,
        ClipyFilterType.SKETCH -> Color(0xFFCBD5E1)
        ClipyFilterType.INVERT,
        ClipyFilterType.DRAMATIC -> ClipyDesignTokens.primaryAccent
        ClipyFilterType.TOON,
        ClipyFilterType.KUWAHARA,
        ClipyFilterType.PIXEL,
        ClipyFilterType.POSTER -> ClipyDesignTokens.tertiaryAccent
        ClipyFilterType.VIGNETTE -> Color(0xFF8B5CF6)
    }
}

private fun overlayAccent(label: String): Color {
    return when (label) {
        "Frame", "Badge" -> ClipyDesignTokens.primaryAccent
        "Glow", "Spark" -> EditorAmber
        "Grid", "Circle" -> EditorCyan
        else -> ClipyDesignTokens.tertiaryAccent
    }
}

private fun formatClock(ms: Long): String {
    val safeMs = ms.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}

private fun formatSpeed(speed: Float): String {
    return if (speed == 1.0f) "1x" else "%.2fx".format(speed).trimEnd('0').trimEnd('.')
}
