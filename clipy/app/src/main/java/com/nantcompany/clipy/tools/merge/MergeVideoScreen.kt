package com.nantcompany.clipy.tools.merge

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.design.ClipyVideoPlayer
import com.nantcompany.clipy.edit.common.TransitionType
import com.nantcompany.clipy.edit.tools.merge.MergeRequest
import com.nantcompany.clipy.export.job.MergePreviewRenderer
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File

private val MergePanelColor = Color(0xFF0B1018)
private val MergePanelElevatedColor = Color(0xFF101722)
private val MergeTrackColor = Color(0xFF060A11)
private val MergeBorderColor = Color(0x1FFFFFFF)
private val MergeNodeColor = Color(0xFF1E293B)
private val MergeClipSurface = Color(0xFF111827)
private val MergeWarmAccent = Color(0xFFFBBF24)
private val MergeGreenAccent = Color(0xFF34D399)

@Composable
fun MergeVideoScreen(
    inputPaths: List<String>,
    onRemoveAt: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onAddMoreAt: (Int) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    viewModel: MergeVideoViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestInputPaths by rememberUpdatedState(inputPaths)
    var selectedClipIndex by remember { mutableStateOf<Int?>(null) }
    var selectedInsertIndex by remember { mutableStateOf(inputPaths.size) }
    var selectedEffectGapIndex by remember { mutableStateOf(0) }
    var gapTransitions by remember { mutableStateOf<List<TransitionType>>(emptyList()) }
    var previewClipIndex by remember { mutableStateOf(0) }
    var previewMode by remember { mutableStateOf(MergePreviewMode.GAP) }
    var effectPreviewPath by remember { mutableStateOf<String?>(null) }
    var effectPreviewRendering by remember { mutableStateOf(false) }
    val clipSpecs = remember(inputPaths) { inputPaths.map(::readClipSpec) }
    val totalDurationMs = remember(clipSpecs) { clipSpecs.sumOf { it.durationMs ?: 0L } }
    val gapCount = (inputPaths.size - 1).coerceAtLeast(0)
    val effectiveGapTransitions = remember(gapCount, gapTransitions) {
        List(gapCount) { index -> gapTransitions.getOrNull(index) ?: TransitionType.CROSSFADE }
    }
    val selectedEffectTransition = effectiveGapTransitions.getOrNull(selectedEffectGapIndex) ?: TransitionType.NONE
    val transitionSummary = remember(effectiveGapTransitions) { mergeTransitionSummary(effectiveGapTransitions) }
    val previewMediaPaths = remember(inputPaths, previewMode, effectPreviewPath, selectedEffectGapIndex, gapCount) {
        val renderedPath = effectPreviewPath
        when {
            previewMode == MergePreviewMode.GAP && !renderedPath.isNullOrBlank() -> listOf(renderedPath)
            previewMode == MergePreviewMode.GAP && gapCount > 0 -> {
                val leftIndex = selectedEffectGapIndex.coerceIn(0, inputPaths.lastIndex)
                inputPaths.drop(leftIndex).take(2)
            }
            else -> inputPaths
        }
    }
    val previewSubtitle = if (previewMode == MergePreviewMode.GAP && gapCount > 0) {
        if (effectPreviewRendering) {
            "Rendering ${selectedEffectTransition.label} preview"
        } else {
            "Gap ${selectedEffectGapIndex + 1}-${selectedEffectGapIndex + 2} | ${selectedEffectTransition.label}"
        }
    } else {
        "Clip ${(previewClipIndex + 1).coerceIn(1, inputPaths.size.coerceAtLeast(1))} | ${formatDurationShort(totalDurationMs)} total"
    }

    LaunchedEffect(inputPaths.size) {
        selectedInsertIndex = selectedInsertIndex.coerceIn(0, inputPaths.size)
        selectedClipIndex = selectedClipIndex?.takeIf { it in inputPaths.indices }
        previewClipIndex = previewClipIndex.coerceIn(0, inputPaths.lastIndex.coerceAtLeast(0))
        selectedEffectGapIndex = selectedEffectGapIndex.coerceIn(0, (inputPaths.size - 2).coerceAtLeast(0))
        gapTransitions = List(gapCount) { index -> gapTransitions.getOrNull(index) ?: TransitionType.CROSSFADE }
    }

    LaunchedEffect(inputPaths, selectedEffectGapIndex, selectedEffectTransition, previewMode) {
        if (previewMode != MergePreviewMode.GAP || gapCount <= 0) {
            effectPreviewRendering = false
            if (previewMode != MergePreviewMode.GAP) effectPreviewPath = null
            return@LaunchedEffect
        }

        val leftPath = inputPaths.getOrNull(selectedEffectGapIndex)
        val rightPath = inputPaths.getOrNull(selectedEffectGapIndex + 1)
        if (leftPath == null || rightPath == null) {
            effectPreviewRendering = false
            effectPreviewPath = null
            return@LaunchedEffect
        }

        effectPreviewRendering = true
        effectPreviewPath = MergePreviewRenderer.renderGapPreview(
            context = context.applicationContext,
            leftPath = leftPath,
            rightPath = rightPath,
            transition = selectedEffectTransition.ffmpegName,
            transitionDurationMs = 1000L
        )
        effectPreviewRendering = false
    }

    val previewPlayer = remember(previewMediaPaths, context) {
        if (previewMediaPaths.isEmpty()) {
            null
        } else {
            ExoPlayer.Builder(context).build().apply {
                setMediaItems(
                    previewMediaPaths.map { path ->
                        MediaItem.fromUri(Uri.fromFile(File(path)))
                    }
                )
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
                prepare()
            }
        }
    }

    DisposableEffect(previewPlayer, lifecycleOwner, inputPaths.size) {
        val player = previewPlayer
        if (player == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    previewClipIndex = player.currentMediaItemIndex.coerceIn(0, inputPaths.lastIndex.coerceAtLeast(0))
                }
            }
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE) player.pause()
            }
            player.addListener(listener)
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                player.removeListener(listener)
                player.release()
            }
        }
    }

    LaunchedEffect(previewClipIndex, previewPlayer, inputPaths.size, previewMode) {
        val player = previewPlayer ?: return@LaunchedEffect
        if (previewMode == MergePreviewMode.GAP) {
            player.seekTo(0L)
            player.play()
            return@LaunchedEffect
        }
        if (inputPaths.isEmpty()) return@LaunchedEffect
        val targetIndex = previewClipIndex.coerceIn(0, inputPaths.lastIndex)
        if (targetIndex < player.mediaItemCount) {
            if (player.currentMediaItemIndex != targetIndex) {
                player.seekTo(targetIndex, 0L)
            }
            player.play()
        }
    }

    val distinctResolutions = remember(clipSpecs) {
        clipSpecs.mapNotNull { spec ->
            val width = spec.width
            val height = spec.height
            if (width != null && height != null && width > 0 && height > 0) "${width}x${height}" else null
        }.toSet()
    }

    val distinctOrientations = remember(clipSpecs) {
        clipSpecs.mapNotNull { spec ->
            val width = spec.width
            val height = spec.height
            when {
                width == null || height == null || width <= 0 || height <= 0 -> null
                width > height -> "Landscape"
                width < height -> "Portrait"
                else -> "Square"
            }
        }.toSet()
    }

    val showMixedWarning = distinctResolutions.size > 1 || distinctOrientations.size > 1
    var warningExpanded by remember { mutableStateOf(false) }

    if (!showMixedWarning) {
        warningExpanded = false
    }

    ClipyScaffold(
        title = "Merge Videos",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .padding(bottom = 142.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (inputPaths.size < 2) {
                    MergeNotice(
                        title = "Need at least 2 clips",
                        message = "Select another video to enable merge export.",
                        accent = ClipyDesignTokens.error
                    )
                }

                previewPlayer?.let { player ->
                    MergePreviewPanel(
                        player = player,
                        subtitle = previewSubtitle
                    )
                }

                MergeTimelinePanel(
                    inputPaths = inputPaths,
                    clipSpecs = clipSpecs,
                    totalDurationMs = totalDurationMs,
                    selectedClipIndex = selectedClipIndex,
                    selectedInsertIndex = selectedInsertIndex,
                    gapTransitions = effectiveGapTransitions,
                    onClipSelected = { index ->
                        selectedClipIndex = index
                        previewClipIndex = index
                        previewMode = MergePreviewMode.CLIP
                        effectPreviewPath = null
                    },
                    onInsertSelected = { index ->
                        selectedInsertIndex = index.coerceIn(0, inputPaths.size)
                        if (index in 1 until inputPaths.size) {
                            selectedEffectGapIndex = index - 1
                            previewMode = MergePreviewMode.GAP
                        }
                    },
                    onMoveSelected = {
                        val fromIndex = selectedClipIndex ?: return@MergeTimelinePanel
                        val targetIndex = targetIndexForInsertSlot(fromIndex, selectedInsertIndex, inputPaths.size)
                        if (targetIndex != fromIndex) {
                            onMove(fromIndex, targetIndex)
                            selectedClipIndex = targetIndex
                            selectedInsertIndex = targetIndex
                            previewClipIndex = targetIndex
                            previewMode = MergePreviewMode.CLIP
                            effectPreviewPath = null
                        }
                    },
                    onAddMore = { onAddMoreAt(selectedInsertIndex.coerceIn(0, inputPaths.size)) },
                    onRemoveAt = { index ->
                        onRemoveAt(index)
                        selectedClipIndex = null
                        selectedInsertIndex = (selectedInsertIndex - if (index < selectedInsertIndex) 1 else 0)
                            .coerceIn(0, (inputPaths.size - 1).coerceAtLeast(0))
                    }
                )

                TransitionPanel(
                    selectedGapIndex = selectedEffectGapIndex,
                    clipCount = inputPaths.size,
                    selectedTransition = selectedEffectTransition,
                    gapTransitions = effectiveGapTransitions,
                    onGapSelected = {
                        selectedEffectGapIndex = it.coerceIn(0, (inputPaths.size - 2).coerceAtLeast(0))
                        previewMode = MergePreviewMode.GAP
                    },
                    onTransitionSelected = { transition ->
                        gapTransitions = List(gapCount) { index ->
                            if (index == selectedEffectGapIndex) transition else effectiveGapTransitions.getOrNull(index) ?: TransitionType.CROSSFADE
                        }
                        previewMode = MergePreviewMode.GAP
                    }
                )

                if (showMixedWarning) {
                    MergeFormatWarning(
                        warningExpanded = warningExpanded,
                        onToggle = { warningExpanded = !warningExpanded },
                        resolutions = distinctResolutions.ifEmpty { setOf("Unknown") }.joinToString(),
                        orientations = distinctOrientations.ifEmpty { setOf("Unknown") }.joinToString()
                    )
                }
            }

            MergeExportBar(
                clipCount = inputPaths.size,
                totalDurationMs = totalDurationMs,
                transitionLabel = transitionSummary,
                canExport = inputPaths.size >= 2,
                modifier = Modifier.align(Alignment.BottomCenter),
                onExport = {
                    val gapTransitionNames = effectiveGapTransitions.map { it.ffmpegName }
                    val request = MergeRequest(
                        inputPaths = latestInputPaths,
                        outputPath = MediaFileUtils.createOutputPath(context, "merge", "mp4"),
                        transition = gapTransitionNames.firstOrNull() ?: TransitionType.NONE.ffmpegName,
                        transitionDurationMs = 1000L,
                        gapTransitions = gapTransitionNames
                    )
                    onSubmitRequest(ProcessingRequest.Merge(request))
                }
            )
        }
    }
}

@Composable
private fun MergePreviewPanel(
    player: ExoPlayer,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MergePanelColor,
        border = BorderStroke(1.dp, MergeBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = ClipyDesignTokens.primaryAccent.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, ClipyDesignTokens.primaryAccent.copy(alpha = 0.24f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = ClipyDesignTokens.primaryAccent,
                        modifier = Modifier.padding(9.dp).size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Preview",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        subtitle,
                        color = ClipyDesignTokens.secondaryText,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MergeGreenAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MergeGreenAccent.copy(alpha = 0.22f))
                ) {
                    Text(
                        "Live",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = MergeGreenAccent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            ) {
                ClipyVideoPlayer(
                    player = player,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun MergeTimelinePanel(
    inputPaths: List<String>,
    clipSpecs: List<ClipSpec>,
    totalDurationMs: Long,
    selectedClipIndex: Int?,
    selectedInsertIndex: Int,
    gapTransitions: List<TransitionType>,
    onClipSelected: (Int) -> Unit,
    onInsertSelected: (Int) -> Unit,
    onMoveSelected: () -> Unit,
    onAddMore: () -> Unit,
    onRemoveAt: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MergePanelColor,
        border = BorderStroke(1.dp, MergeBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Timeline",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "${inputPaths.size} clips | ${formatDurationShort(totalDurationMs)} total",
                        color = ClipyDesignTokens.secondaryText,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TimelinePillButton(
                    label = "Add",
                    enabled = true,
                    filled = true,
                    onClick = onAddMore
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(MergePanelElevatedColor, MergeTrackColor)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 7.dp, vertical = 7.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TimelineRuler(tickCount = (inputPaths.size * 4).coerceAtLeast(8))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InsertSlot(
                            index = 0,
                            active = selectedInsertIndex == 0,
                            hasEffect = false,
                            onClick = { onInsertSelected(0) }
                        )
                        inputPaths.forEachIndexed { index, path ->
                            MergeClipTile(
                                path = path,
                                index = index,
                                clipSpec = clipSpecs.getOrNull(index),
                                selected = selectedClipIndex == index,
                                onClick = { onClipSelected(index) },
                                onRemove = { onRemoveAt(index) }
                            )
                            InsertSlot(
                                index = index + 1,
                                active = selectedInsertIndex == index + 1,
                                hasEffect = gapTransitions.getOrNull(index) != null && gapTransitions[index] != TransitionType.NONE,
                                onClick = { onInsertSelected(index + 1) }
                            )
                        }
                    }
                }
            }

            MergeTimelineActions(
                selectedClipIndex = selectedClipIndex,
                selectedInsertIndex = selectedInsertIndex,
                clipCount = inputPaths.size,
                onMoveSelected = onMoveSelected,
                onAddMore = onAddMore
            )
        }
    }
}

@Composable
private fun TimelineRuler(tickCount: Int) {
    Row(
        modifier = Modifier
            .height(10.dp)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(tickCount) { index ->
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(if (index % 4 == 0) 8.dp else 4.dp)
                    .background(Color.White.copy(alpha = if (index % 4 == 0) 0.22f else 0.10f))
            )
        }
    }
}

@Composable
private fun InsertSlot(
    index: Int,
    active: Boolean,
    hasEffect: Boolean,
    onClick: () -> Unit
) {
    val nodeSize = if (active) 26.dp else 21.dp
    val nodeColor = when {
        active -> ClipyDesignTokens.primaryAccent
        hasEffect -> MergeWarmAccent
        else -> MergeNodeColor
    }
    val lineColor = if (active) {
        ClipyDesignTokens.primaryAccent.copy(alpha = 0.68f)
    } else if (hasEffect) {
        MergeWarmAccent.copy(alpha = 0.46f)
    } else {
        Color.White.copy(alpha = 0.13f)
    }

    Box(
        modifier = Modifier
            .width(29.dp)
            .height(68.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (active || hasEffect) 3.dp else 2.dp)
                .background(lineColor, RoundedCornerShape(99.dp))
        )
        Surface(
            modifier = Modifier.size(nodeSize),
            shape = RoundedCornerShape(11.dp),
            color = nodeColor,
            border = BorderStroke(1.dp, if (active) Color.White.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.08f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (hasEffect) Icons.Default.PlayArrow else Icons.Default.Add,
                    contentDescription = "Insert slot $index",
                    tint = if (active || hasEffect) Color.Black else ClipyDesignTokens.secondaryText,
                    modifier = Modifier.size(if (active) 15.dp else 11.dp)
                )
            }
        }
        if (hasEffect) {
            Text(
                "FX",
                modifier = Modifier.align(Alignment.BottomCenter),
                color = MergeWarmAccent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun MergeClipTile(
    path: String,
    index: Int,
    clipSpec: ClipSpec?,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(path) { File(path) }
    val borderColor = if (selected) ClipyDesignTokens.primaryAccent else Color.White.copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .width(78.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(MergeClipSurface)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(file)
                .decoderFactory(VideoFrameDecoder.Factory())
                .crossfade(true)
                .size(260)
                .build(),
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(11.dp))
                .background(Color.Black)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.04f), Color.Transparent, Color.Black.copy(alpha = 0.72f))
                    )
                )
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(5.dp),
            shape = CircleShape,
            color = if (selected) ClipyDesignTokens.primaryAccent else Color.Black.copy(alpha = 0.62f)
        ) {
            Text(
                text = (index + 1).toString(),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                color = if (selected) Color.Black else Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            text = formatDurationShort(clipSpec?.durationMs ?: 0L),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .background(Color.Black.copy(alpha = 0.50f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove",
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(11.dp)
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .width(22.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(ClipyDesignTokens.primaryAccent)
            )
        }
    }
}

@Composable
private fun MergeTimelineActions(
    selectedClipIndex: Int?,
    selectedInsertIndex: Int,
    clipCount: Int,
    onMoveSelected: () -> Unit,
    onAddMore: () -> Unit
) {
    val slotLabel = when (selectedInsertIndex) {
        0 -> "Start"
        clipCount -> "End"
        else -> "Between $selectedInsertIndex and ${selectedInsertIndex + 1}"
    }
    val canMove = selectedClipIndex != null &&
        selectedInsertIndex != selectedClipIndex &&
        selectedInsertIndex != selectedClipIndex + 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Insert slot",
                color = ClipyDesignTokens.textMuted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                slotLabel,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        TimelinePillButton(label = "Move", enabled = canMove, filled = false, onClick = onMoveSelected)
        TimelinePillButton(label = "Add here", enabled = true, filled = false, onClick = onAddMore)
    }
}

@Composable
private fun TimelinePillButton(
    label: String,
    enabled: Boolean,
    filled: Boolean,
    onClick: () -> Unit
) {
    val activeColor = ClipyDesignTokens.primaryAccent
    val backgroundColor = when {
        !enabled -> Color.White.copy(alpha = 0.035f)
        filled -> activeColor
        else -> activeColor.copy(alpha = 0.13f)
    }
    val borderColor = when {
        !enabled -> Color.White.copy(alpha = 0.05f)
        filled -> activeColor
        else -> activeColor.copy(alpha = 0.32f)
    }
    val textColor = when {
        !enabled -> ClipyDesignTokens.textMuted
        filled -> Color.Black
        else -> activeColor
    }

    Surface(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(13.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (filled) 12.dp else 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (filled) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TransitionPanel(
    selectedGapIndex: Int,
    clipCount: Int,
    selectedTransition: TransitionType,
    gapTransitions: List<TransitionType>,
    onGapSelected: (Int) -> Unit,
    onTransitionSelected: (TransitionType) -> Unit
) {
    val gapCount = (clipCount - 1).coerceAtLeast(0)
    val canEditGap = gapCount > 0
    val gapLabel = if (canEditGap) {
        "Between clip ${selectedGapIndex + 1} and ${selectedGapIndex + 2}"
    } else {
        "Select at least 2 clips"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MergePanelColor,
        border = BorderStroke(1.dp, MergeBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Effects",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        gapLabel,
                        color = ClipyDesignTokens.secondaryText,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = transitionAccent(selectedTransition).copy(alpha = 0.13f),
                    border = BorderStroke(1.dp, transitionAccent(selectedTransition).copy(alpha = 0.25f))
                ) {
                    Text(
                        selectedTransition.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = transitionAccent(selectedTransition),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }

            if (canEditGap) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(gapCount) { gapIndex ->
                        GapChip(
                            label = "${gapIndex + 1}-${gapIndex + 2}",
                            transition = gapTransitions.getOrNull(gapIndex) ?: TransitionType.CROSSFADE,
                            selected = selectedGapIndex == gapIndex,
                            onClick = { onGapSelected(gapIndex) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                TransitionType.entries.forEach { transition ->
                    TransitionChip(
                        transition = transition,
                        selected = selectedTransition == transition,
                        enabled = canEditGap,
                        onClick = { onTransitionSelected(transition) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GapChip(
    label: String,
    transition: TransitionType,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) transitionAccent(transition).copy(alpha = 0.16f) else Color.White.copy(alpha = 0.045f),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) transitionAccent(transition) else Color.White.copy(alpha = 0.07f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(transitionAccent(transition), CircleShape)
            )
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = transition.label,
                color = if (selected) transitionAccent(transition) else ClipyDesignTokens.textMuted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TransitionChip(
    transition: TransitionType,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val accent = transitionAccent(transition)
    Surface(
        modifier = Modifier
            .width(124.dp)
            .height(76.dp)
            .clip(RoundedCornerShape(17.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        color = if (selected) accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.045f),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) accent else Color.White.copy(alpha = 0.06f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            TransitionPreviewStrip(transition = transition, selected = selected, enabled = enabled)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = transition.label,
                    modifier = Modifier.weight(1f),
                    color = when {
                        !enabled -> ClipyDesignTokens.textMuted
                        selected -> Color.White
                        else -> ClipyDesignTokens.secondaryText
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (selected) accent else Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (selected) Color.Black else Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransitionPreviewStrip(
    transition: TransitionType,
    selected: Boolean,
    enabled: Boolean
) {
    val accent = transitionAccent(transition)
    val leftColor = if (enabled) accent.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.12f)
    val rightColor = if (enabled) transitionSecondaryAccent(transition).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.07f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, Color.White.copy(alpha = if (selected) 0.18f else 0.07f), RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(leftColor)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(rightColor)
            )
        }
        val previewBrush = when (transition) {
            TransitionType.NONE -> Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.24f), Color.Transparent))
            TransitionType.FADE,
            TransitionType.CROSSFADE -> Brush.horizontalGradient(listOf(leftColor, Color.White.copy(alpha = 0.68f), rightColor))
            TransitionType.WIPE_LEFT,
            TransitionType.WIPE_RIGHT -> Brush.horizontalGradient(listOf(rightColor, leftColor))
            TransitionType.SLIDE_LEFT,
            TransitionType.SLIDE_RIGHT -> Brush.horizontalGradient(listOf(leftColor, accent, rightColor))
            TransitionType.CIRCLE_OPEN,
            TransitionType.ZOOM_IN -> Brush.radialGradient(listOf(Color.White.copy(alpha = 0.82f), accent.copy(alpha = 0.16f), Color.Transparent))
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(if (transition == TransitionType.NONE) 0.12f else 0.62f)
                .height(if (transition == TransitionType.NONE) 23.dp else 5.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(previewBrush)
        )
    }
}

@Composable
private fun MergeFormatWarning(
    warningExpanded: Boolean,
    onToggle: () -> Unit,
    resolutions: String,
    orientations: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF451A03).copy(alpha = 0.34f),
        border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF97316),
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Mixed formats",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Auto-fit on export",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFED7AA),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onToggle),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF97316).copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.24f))
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (warningExpanded) "Hide" else "Details",
                            color = Color(0xFFF97316),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            if (warningExpanded) {
                HorizontalDivider(color = Color(0x33F97316))
                Text(
                    "Clips have different orientation or resolution. Clipy will fit clips automatically for export.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFED7AA)
                )
                Text("Resolutions: $resolutions", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFED7AA))
                Text("Orientations: $orientations", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFED7AA))
            }
        }
    }
}

@Composable
private fun MergeNotice(
    title: String,
    message: String,
    accent: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodySmall, color = accent.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun MergeExportBar(
    clipCount: Int,
    totalDurationMs: Long,
    transitionLabel: String,
    canExport: Boolean,
    modifier: Modifier = Modifier,
    onExport: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color(0xF2070B13),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(horizontal = 14.dp, vertical = 8.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportStatPill(value = "$clipCount clips", accent = ClipyDesignTokens.primaryAccent)
                ExportStatPill(value = formatDurationShort(totalDurationMs), accent = ClipyDesignTokens.secondaryAccent)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transitionLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ready to export",
                        color = ClipyDesignTokens.textMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
            ClipyPrimaryButton(
                modifier = Modifier
                    .width(122.dp)
                    .height(48.dp),
                label = "Export",
                enabled = canExport,
                onClick = onExport
            )
        }
    }
}

@Composable
private fun ExportStatPill(value: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Text(
            text = value,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            color = accent,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

private data class ClipSpec(
    val width: Int?,
    val height: Int?,
    val durationMs: Long?
) {
    val resolutionLabel: String
        get() {
            val safeWidth = width
            val safeHeight = height
            return if (safeWidth != null && safeHeight != null && safeWidth > 0 && safeHeight > 0) {
                "${safeWidth}x${safeHeight}"
            } else {
                ""
            }
        }
}

private fun readClipSpec(path: String): ClipSpec {
    return runCatching {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        retriever.release()

        if (rotation == 90 || rotation == 270) {
            ClipSpec(height, width, duration)
        } else {
            ClipSpec(width, height, duration)
        }
    }.getOrDefault(ClipSpec(null, null, null))
}

private fun targetIndexForInsertSlot(fromIndex: Int, insertIndex: Int, size: Int): Int {
    if (size <= 1 || fromIndex !in 0 until size) return fromIndex
    val safeInsertIndex = insertIndex.coerceIn(0, size)
    val target = if (safeInsertIndex > fromIndex) safeInsertIndex - 1 else safeInsertIndex
    return target.coerceIn(0, size - 1)
}

private fun formatDurationShort(ms: Long): String {
    val totalSecs = ms.coerceAtLeast(0L) / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%d:%02d".format(mins, secs)
}

private fun mergeTransitionSummary(transitions: List<TransitionType>): String {
    if (transitions.isEmpty() || transitions.all { it == TransitionType.NONE }) return "No effect"
    val distinct = transitions.distinct()
    return if (distinct.size == 1) {
        "${distinct.first().label} effect"
    } else {
        "Mixed effects"
    }
}

private fun transitionAccent(transition: TransitionType): Color {
    return when (transition) {
        TransitionType.NONE -> ClipyDesignTokens.textMuted
        TransitionType.FADE,
        TransitionType.CROSSFADE -> ClipyDesignTokens.secondaryAccent
        TransitionType.WIPE_LEFT,
        TransitionType.WIPE_RIGHT -> MergeWarmAccent
        TransitionType.SLIDE_LEFT,
        TransitionType.SLIDE_RIGHT -> MergeGreenAccent
        TransitionType.CIRCLE_OPEN -> ClipyDesignTokens.tertiaryAccent
        TransitionType.ZOOM_IN -> ClipyDesignTokens.primaryAccent
    }
}

private fun transitionSecondaryAccent(transition: TransitionType): Color {
    return when (transition) {
        TransitionType.NONE -> Color(0xFF475569)
        TransitionType.FADE,
        TransitionType.CROSSFADE -> ClipyDesignTokens.primaryAccent
        TransitionType.WIPE_LEFT,
        TransitionType.WIPE_RIGHT -> Color(0xFFF97316)
        TransitionType.SLIDE_LEFT,
        TransitionType.SLIDE_RIGHT -> ClipyDesignTokens.secondaryAccent
        TransitionType.CIRCLE_OPEN -> MergeWarmAccent
        TransitionType.ZOOM_IN -> MergeGreenAccent
    }
}

private enum class MergePreviewMode {
    GAP,
    CLIP
}
