package com.nantcompany.clipy.editor

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.nantcompany.clipy.app.ProEditorViewModel
import com.nantcompany.clipy.design.*
import com.nantcompany.clipy.filters.gpu.ClipyFilterType
import com.nantcompany.clipy.filters.gpu.ClipyGpuFilterManager
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.export.job.StudioRequest
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun ProEditorScreen(
    onNavigate: (AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    viewModel: ProEditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var currentTab by remember { mutableStateOf("filter") }
    
    val videoUri = remember(state.videoPath) { state.videoPath?.let { Uri.fromFile(File(it)) } }
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

    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    var currentMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var previewWidth by remember { mutableFloatStateOf(1f) }
    var previewHeight by remember { mutableFloatStateOf(1f) }
    val previewColorMatrix = remember(state.brightness, state.contrast, state.saturation, state.selectedFilter) {
        ClipyGpuFilterManager.createPreviewColorMatrix(
            brightness = state.brightness,
            contrast = state.contrast,
            saturation = state.saturation,
            filterType = state.selectedFilter
        )
    }

    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect
        while (true) {
            currentMs = player.currentPosition
            durationMs = player.duration.coerceAtLeast(0L)
            delay(33)
        }
    }

    LaunchedEffect(state.speedFactor, player) { player?.setPlaybackSpeed(state.speedFactor) }

    ClipyScaffold(
        title = "Studio",
        onBackClick = { onNavigate(AppRoute.HOME) },
        actions = {
            TextButton(onClick = {
                val input = state.videoPath ?: return@TextButton
                val textLayers = if (state.overlayText.isNotEmpty()) {
                    listOf(com.nantcompany.clipy.export.model.TextLayer(
                        id = "overlay_text",
                        text = state.overlayText,
                        x = state.textX,
                        y = state.textY,
                        startMs = 0L,
                        endMs = durationMs,
                        color = android.graphics.Color.WHITE
                    ))
                } else emptyList()

                onSubmitRequest(ProcessingRequest.Studio(StudioRequest(
                    inputPath = input,
                    outputPath = com.nantcompany.clipy.app.MediaFileUtils.createOutputPath(context, "studio", "mp4"),
                    startMs = state.startMs, endMs = state.endMs,
                    rotation = state.rotation, flipHorizontal = state.flipHorizontal,
                    brightness = state.brightness, contrast = state.contrast, saturation = state.saturation,
                    filterName = state.selectedFilter.name, speedFactor = state.speedFactor,
                    textLayers = textLayers, audioTracks = emptyList(), mainVideoVolume = 1.0f
                )))
            }) {
                Text("Export", color = ClipyDesignTokens.primaryAccent, fontWeight = FontWeight.Black)
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().background(ClipyDesignTokens.bgMain)) {
            // 1. Preview
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .onSizeChanged { previewWidth = it.width.toFloat(); previewHeight = it.height.toFloat() },
                contentAlignment = Alignment.Center
            ) {
                if (player != null) {
                    ClipyVideoPlayer(
                        player = player,
                        previewColorMatrix = previewColorMatrix,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            rotationZ = state.rotation.toFloat()
                            scaleX = if (state.flipHorizontal) -1f else 1f
                        }
                    )

                    if (state.overlayText.isNotEmpty()) {
                        Text(
                            text = state.overlayText, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .offset { IntOffset((state.textX / 100f * previewWidth).roundToInt(), (state.textY / 100f * previewHeight).roundToInt()) }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, drag ->
                                        change.consume()
                                        viewModel.setTextPos(
                                            (state.textX + (drag.x / previewWidth * 100f)).coerceIn(0f, 100f),
                                            (state.textY + (drag.y / previewHeight * 100f)).coerceIn(0f, 100f)
                                        )
                                    }
                                }
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                    }
                }
            }

            // 2. Timeline
            ClipyTimeline(
                clips = listOf(com.nantcompany.clipy.export.model.VideoClip(path = state.videoPath ?: "", durationMs = durationMs, endMs = durationMs)),
                durationMs = durationMs, currentMs = currentMs, onSeek = { player?.seekTo(it) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // 3. New Refined Toolbar
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                color = ClipyDesignTokens.bgNav,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Tab Selector
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TabItem("Filters", Icons.Default.Star, currentTab == "filter") { currentTab = "filter" }
                        TabItem("Adjust", Icons.Default.Settings, currentTab == "adjust") { currentTab = "adjust" }
                        TabItem("Text", Icons.Default.Edit, currentTab == "text") { currentTab = "text" }
                        TabItem("Flip", Icons.Default.Refresh, state.flipHorizontal) { viewModel.setTransform(state.rotation, !state.flipHorizontal) }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Content Area
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                        when (currentTab) {
                            "filter" -> FilterGrid(state.selectedFilter) { viewModel.setFilter(it) }
                            "adjust" -> AdjustSliders(state) { b, c, s -> viewModel.setAdjustments(b, c, s) }
                            "text" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Overlay Text", style = MaterialTheme.typography.labelMedium, color = ClipyDesignTokens.secondaryText)
                                ClipyTextField(value = state.overlayText, onValueChange = viewModel::setOverlayText, placeholder = "Enter text to overlay...")
                                Text("Drag text on preview to reposition", style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.primaryAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) ClipyDesignTokens.primaryAccent.copy(alpha = 0.15f) else Color.Transparent
    val contentColor = if (active) ClipyDesignTokens.primaryAccent else ClipyDesignTokens.secondaryText
    
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = contentColor, modifier = Modifier.size(18.dp))
        if (active) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = contentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FilterGrid(selected: ClipyFilterType, onSelect: (ClipyFilterType) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(ClipyFilterType.entries) { f ->
            val isS = selected == f
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isS) ClipyDesignTokens.primaryAccent.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f))
                    .border(1.dp, if (isS) ClipyDesignTokens.primaryAccent else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable { onSelect(f) }
                    .padding(8.dp)
                    .width(72.dp)
            ) {
                Box(
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(Color.DarkGray)
                ) {
                    // Here we could show a small preview if needed
                }
                Spacer(Modifier.height(6.dp))
                Text(f.displayName, color = if (isS) Color.White else ClipyDesignTokens.secondaryText, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AdjustSliders(state: com.nantcompany.clipy.app.ProEditorState, onUpdate: (Float, Float, Float) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdjustSlider("Brightness", state.brightness, -0.5f..0.5f) { onUpdate(it, state.contrast, state.saturation) }
        AdjustSlider("Contrast", state.contrast, -0.5f..0.5f) { onUpdate(state.brightness, it, state.saturation) }
        AdjustSlider("Saturation", state.saturation, 0f..2f) { onUpdate(state.brightness, state.contrast, it) }
    }
}

@Composable
private fun AdjustSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
            Text("%.2f".format(value), style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.primaryAccent)
        }
        Slider(
            value = value, 
            onValueChange = onValueChange, 
            valueRange = range, 
            colors = SliderDefaults.colors(
                thumbColor = Color.White, 
                activeTrackColor = ClipyDesignTokens.primaryAccent,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}
