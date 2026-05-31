package com.nantcompany.clipy.tools.cut

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMicros
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.design.ClipyVideoPlayer
import com.nantcompany.clipy.edit.tools.cut.CutRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.delay

private val CutPanelColor = Color(0xFF101725)
private val CutTrackColor = Color(0xFF08111F)
private val CutBorderColor = Color(0x1FFFFFFF)

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
    val latestState by rememberUpdatedState(uiState)
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(inputPath) {
        viewModel.setInputPath(inputPath)
    }

    val videoFile = remember(uiState.inputPath) { uiState.inputPath?.let(::File) }
    val videoExists = remember(uiState.inputPath) { videoFile?.exists() == true }
    val videoUri = remember(uiState.inputPath, videoExists) {
        if (videoExists && videoFile != null) Uri.fromFile(videoFile) else null
    }

    val player = remember(videoUri, context) {
        if (videoUri == null) {
            null
        } else {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUri))
                playWhenReady = false
                prepare()
            }
        }
    }

    var currentMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect
        while (true) {
            val current = player.currentPosition.coerceAtLeast(0L)
            val duration = player.duration.takeIf { it > 0L } ?: 0L
            val state = latestState
            if (player.isPlaying && state.endMs > state.startMs && current >= state.endMs) {
                player.pause()
                player.seekTo(state.startMs)
                currentMs = state.startMs
            } else {
                currentMs = current
            }
            durationMs = duration
            delay(50)
        }
    }

    LaunchedEffect(durationMs) {
        viewModel.setDurationMs(durationMs)
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

    val selectedDurationMs = (uiState.endMs - uiState.startMs).coerceAtLeast(0L)
    val canExport = !uiState.inputPath.isNullOrBlank() &&
        uiState.validationError == null &&
        selectedDurationMs >= uiState.minDurationMs

    ClipyScaffold(
        title = "Cut Video",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .padding(bottom = 108.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (player != null) {
                    ClipyVideoPlayer(
                        player = player,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(268.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(268.dp)
                            .background(Color.Black, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading video...", color = ClipyDesignTokens.secondaryText)
                    }
                }

                TrimTimelineCard(
                    inputPath = uiState.inputPath,
                    durationMs = durationMs,
                    currentMs = currentMs,
                    startMs = uiState.startMs,
                    endMs = uiState.endMs,
                    minDurationMs = uiState.minDurationMs,
                    onSeek = { ms ->
                        player?.seekTo(ms)
                        currentMs = ms
                    },
                    onStartChange = { ms ->
                        viewModel.setStartMs(ms)
                        player?.seekTo(ms)
                    },
                    onEndChange = { ms ->
                        viewModel.setEndMs(ms)
                        player?.seekTo(ms)
                    },
                    onReset = {
                        viewModel.resetRange()
                        player?.seekTo(0L)
                    },
                    onPlaySelection = {
                        player?.seekTo(uiState.startMs)
                        player?.play()
                    }
                )

                if (uiState.validationError != null) {
                    Text(
                        text = uiState.validationError.orEmpty(),
                        color = ClipyDesignTokens.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            CutExportBar(
                startMs = uiState.startMs,
                endMs = uiState.endMs,
                selectedDurationMs = selectedDurationMs,
                canExport = canExport,
                modifier = Modifier.align(Alignment.BottomCenter),
                onExport = {
                    val state = latestState
                    val input = state.inputPath ?: return@CutExportBar
                    val request = CutRequest(
                        inputPath = input,
                        outputPath = MediaFileUtils.createOutputPath(context, "cut", "mp4"),
                        startMs = state.startMs,
                        endMs = state.endMs
                    )
                    if (viewModel.validate(request)) {
                        onSubmitRequest(ProcessingRequest.Cut(request))
                    }
                }
            )
        }
    }
}

@Composable
private fun TrimTimelineCard(
    inputPath: String?,
    durationMs: Long,
    currentMs: Long,
    startMs: Long,
    endMs: Long,
    minDurationMs: Long,
    onSeek: (Long) -> Unit,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit,
    onReset: () -> Unit,
    onPlaySelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CutPanelColor),
        border = BorderStroke(1.dp, CutBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Trim timeline",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "${formatTime(startMs)} - ${formatTime(endMs)}",
                        color = ClipyDesignTokens.textSecondary,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onPlaySelection,
                    enabled = endMs > startMs
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play selection",
                        tint = ClipyDesignTokens.primaryAccent
                    )
                }
                IconButton(onClick = onReset) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset trim",
                        tint = ClipyDesignTokens.secondaryText
                    )
                }
            }

            TrimTimeline(
                inputPath = inputPath,
                durationMs = durationMs,
                currentMs = currentMs,
                startMs = startMs,
                endMs = endMs,
                minDurationMs = minDurationMs,
                onSeek = onSeek,
                onStartChange = onStartChange,
                onEndChange = onEndChange
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RangeMetric("Start", formatTimePrecise(startMs), Modifier.weight(1f))
                RangeMetric("Length", formatTimePrecise((endMs - startMs).coerceAtLeast(0L)), Modifier.weight(1f))
                RangeMetric("End", formatTimePrecise(endMs), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TrimTimeline(
    inputPath: String?,
    durationMs: Long,
    currentMs: Long,
    startMs: Long,
    endMs: Long,
    minDurationMs: Long,
    onSeek: (Long) -> Unit,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var widthPx by remember { mutableFloatStateOf(0f) }
    val trackHeight = 92.dp
    val handleWidth = 34.dp
    val safeDuration = durationMs.coerceAtLeast(1L)
    val safeStart = startMs.coerceIn(0L, safeDuration)
    val safeEnd = endMs.coerceIn(safeStart, safeDuration)
    val startPx = if (widthPx > 0f) safeStart.toFloat() / safeDuration.toFloat() * widthPx else 0f
    val endPx = if (widthPx > 0f) safeEnd.toFloat() / safeDuration.toFloat() * widthPx else widthPx
    val handleWidthPx = with(density) { handleWidth.toPx() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(22.dp))
                .background(CutTrackColor)
                .border(1.dp, CutBorderColor, RoundedCornerShape(22.dp))
                .onSizeChanged { widthPx = it.width.toFloat() }
                .pointerInput(widthPx, safeDuration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (widthPx > 0f) {
                                onSeek(((offset.x / widthPx) * safeDuration).roundToLong().coerceIn(0L, safeDuration))
                            }
                        },
                        onDrag = { change, _ ->
                            if (widthPx > 0f) {
                                onSeek(((change.position.x / widthPx) * safeDuration).roundToLong().coerceIn(0L, safeDuration))
                            }
                        }
                    )
                }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                val thumbnailCount = 8
                repeat(thumbnailCount) { index ->
                    val frameMs = ((index + 0.5f) / thumbnailCount.toFloat() * safeDuration).roundToLong()
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(inputPath)
                            .decoderFactory(VideoFrameDecoder.Factory())
                            .videoFrameMicros(frameMs * 1000L)
                            .size(180)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        alpha = if (inputPath == null) 0f else 0.86f
                    )
                }
            }

            if (inputPath == null || durationMs <= 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Reading timeline...",
                        color = ClipyDesignTokens.secondaryText,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (widthPx > 0f && durationMs > 0L) {
                Box(
                    modifier = Modifier
                        .width(with(density) { startPx.coerceAtLeast(0f).toDp() })
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.58f))
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(endPx.roundToInt(), 0) }
                        .width(with(density) { (widthPx - endPx).coerceAtLeast(0f).toDp() })
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.58f))
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(startPx.roundToInt(), 0) }
                        .width(with(density) { (endPx - startPx).coerceAtLeast(1f).toDp() })
                        .fillMaxHeight()
                        .border(3.dp, ClipyDesignTokens.primaryAccent, RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    ClipyDesignTokens.primaryAccent.copy(alpha = 0.16f),
                                    ClipyDesignTokens.secondaryAccent.copy(alpha = 0.10f)
                                )
                            )
                        )
                )

                val playheadX = (currentMs.coerceIn(0L, safeDuration).toFloat() / safeDuration.toFloat()) * widthPx
                Box(
                    modifier = Modifier
                        .offset { IntOffset(playheadX.roundToInt() - 1, 0) }
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(playheadX.roundToInt() - 7, 5) }
                        .size(14.dp)
                        .background(Color.White, CircleShape)
                        .border(2.dp, ClipyDesignTokens.primaryAccent, CircleShape)
                )

                TrimHandle(
                    label = "Start",
                    xPx = startPx - handleWidthPx / 2f,
                    handleWidth = handleWidth,
                    widthPx = widthPx,
                    durationMs = safeDuration,
                    currentValueMs = safeStart,
                    oppositeValueMs = safeEnd,
                    minDurationMs = minDurationMs,
                    isStart = true,
                    onDragStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onValueChange = onStartChange
                )
                TrimHandle(
                    label = "End",
                    xPx = endPx - handleWidthPx / 2f,
                    handleWidth = handleWidth,
                    widthPx = widthPx,
                    durationMs = safeDuration,
                    currentValueMs = safeEnd,
                    oppositeValueMs = safeStart,
                    minDurationMs = minDurationMs,
                    isStart = false,
                    onDragStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onValueChange = onEndChange
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("00:00", color = ClipyDesignTokens.textMuted, style = MaterialTheme.typography.labelSmall)
            Text(formatTime(durationMs), color = ClipyDesignTokens.textMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TrimHandle(
    label: String,
    xPx: Float,
    handleWidth: androidx.compose.ui.unit.Dp,
    widthPx: Float,
    durationMs: Long,
    currentValueMs: Long,
    oppositeValueMs: Long,
    minDurationMs: Long,
    isStart: Boolean,
    onDragStart: () -> Unit,
    onValueChange: (Long) -> Unit
) {
    val latestCurrent by rememberUpdatedState(currentValueMs)
    val latestOpposite by rememberUpdatedState(oppositeValueMs)
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnValueChange by rememberUpdatedState(onValueChange)

    Box(
        modifier = Modifier
            .offset { IntOffset(xPx.roundToInt(), 0) }
            .width(handleWidth)
            .fillMaxHeight()
            .pointerInput(label, widthPx, durationMs, isStart) {
                var dragValueMs = 0L
                detectDragGestures(
                    onDragStart = {
                        dragValueMs = latestCurrent
                        latestOnDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val deltaMs = ((dragAmount.x / widthPx) * durationMs).roundToLong()
                        dragValueMs = if (isStart) {
                            (dragValueMs + deltaMs).coerceIn(0L, (latestOpposite - minDurationMs).coerceAtLeast(0L))
                        } else {
                            (dragValueMs + deltaMs).coerceIn((latestOpposite + minDurationMs).coerceAtMost(durationMs), durationMs)
                        }
                        latestOnValueChange(dragValueMs)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(999.dp),
            color = ClipyDesignTokens.primaryAccent
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun RangeMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                label,
                color = ClipyDesignTokens.textMuted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                value,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CutExportBar(
    startMs: Long,
    endMs: Long,
    selectedDurationMs: Long,
    canExport: Boolean,
    modifier: Modifier = Modifier,
    onExport: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xF2070B13),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(horizontal = 16.dp, vertical = 14.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Selected ${formatTimePrecise(selectedDurationMs)}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatTimePrecise(startMs)} - ${formatTimePrecise(endMs)}",
                    color = ClipyDesignTokens.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ClipyPrimaryButton(
                modifier = Modifier
                    .width(152.dp)
                    .height(58.dp),
                label = "Export",
                enabled = canExport,
                onClick = onExport
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatTimePrecise(ms: Long): String {
    val safeMs = ms.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (safeMs % 1000) / 100
    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}
