package com.natncompany.clipy.editor.ui

import android.annotation.SuppressLint
import android.graphics.Color.parseColor
import android.net.Uri
import android.widget.ImageView
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.natncompany.clipy.R
import com.natncompany.clipy.editor.AspectPreset
import com.natncompany.clipy.editor.ClipDraft
import com.natncompany.clipy.editor.ClipyAppState
import com.natncompany.clipy.editor.EditorTool
import com.natncompany.clipy.editor.ExportResolutionPreset
import com.natncompany.clipy.editor.HomeFeature
import com.natncompany.clipy.editor.MediaKind
import com.natncompany.clipy.editor.backgroundOptions
import com.natncompany.clipy.editor.buildMediaTimeline
import com.natncompany.clipy.editor.buildExportPlan
import com.natncompany.clipy.editor.buildPreviewPlan
import com.natncompany.clipy.editor.buildVideoEditorSession
import com.natncompany.clipy.editor.exportWithMediaPipeline
import com.natncompany.clipy.editor.filterOptions
import com.natncompany.clipy.editor.formatDuration
import com.natncompany.clipy.filter.GpuImagePreview
import com.natncompany.videoeditor.PipelineStage
import com.natncompany.videoeditor.ReusableEditorTool
import com.natncompany.videoeditor.VideoEditorToolBar
import com.natncompany.videoeditor.VideoEditorTimelineStrip

private val HomeBackground = Color(0xFF323232)
private val EditorPanelBackground = Color(0xFF242728)
private val EditorPanelRaised = Color(0xFF363738)
private val EditorToolbarBackground = Color(0xFF373B3D)
private val EditorPreviewBackground = Color(0xFF11161D)
private val EditorAccentBlue = Color(0xFF4A90E2)
private val EditorAccentGreen = Color(0xFFAFD800)
private val EditorTextPrimary = Color(0xFFFFFFFF)
private val EditorTextSecondary = Color(0xCCFFFFFF)
private val EditorTextMuted = Color(0xFF999CB0)
private val EditorUnselected = Color(0xFF4D4F51)
private val EditorBottomTrayHeight = 296.dp
private val EditorToolbarHeight = 50.dp
private val SelectedStripHeight = 96.dp
private val TimelineCardWidth = 84.dp
private val TimelineCardHeight = 58.dp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    features: List<HomeFeature>,
    projectName: String,
    clipCount: Int,
    durationLabel: String,
    hasProject: Boolean,
    onContinueProject: () -> Unit,
    onFeatureClick: (HomeFeature) -> Unit
) {
    val editFeature = features.firstOrNull()
    val captureFeature = features.getOrNull(1) ?: editFeature
    val footerText = "v1.0"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.main_setting),
            contentDescription = "Settings",
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(35.dp)
                .clickable(enabled = hasProject || editFeature != null) {
                    if (hasProject) {
                        onContinueProject()
                    } else {
                        editFeature?.let(onFeatureClick)
                    }
                }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(38.dp)
        ) {
            HomeActionCard(
                backgroundRes = R.drawable.activity_main_edit_background,
                label = "Video Edit",
                onClick = {
                    if (hasProject) {
                        onContinueProject()
                    } else {
                        editFeature?.let(onFeatureClick)
                    }
                }
            )
            HomeActionCard(
                backgroundRes = R.drawable.activity_main_capture_background,
                label = "Video Capture",
                onClick = {
                    captureFeature?.let(onFeatureClick)
                }
            )
        }

        Text(
            text = footerText,
            color = EditorTextPrimary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 15.dp)
        )
    }
}

@Composable
private fun HomeActionCard(
    backgroundRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = label,
            color = EditorTextPrimary,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 15.dp)
        )
    }
}

@Composable
fun EditorScreen(
    modifier: Modifier = Modifier,
    appState: ClipyAppState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onImportMore: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            EditorTitleBar(onBack = onBack)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                EditorPreviewPane(
                    clip = appState.selectedClip,
                    aspectPreset = appState.aspectPreset,
                    activeTool = appState.activeTool,
                    statusMessage = appState.statusMessage,
                    modifier = Modifier.fillMaxSize()
                )
                EditorTopActions(
                    resolutionPreset = appState.exportResolutionPreset,
                    onResolutionSelected = appState::updateExportResolution,
                    onExportClick = onNext,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 38.dp, end = 12.dp)
                )
            }
            EditorBottomTray(
                appState = appState,
                onImportMore = onImportMore
            )
        }
    }
}

@Composable
private fun EditorTitleBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(Color.Black)
            .padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(width = 56.dp, height = 28.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.CenterStart
        ) {
            Image(
                painter = painterResource(id = R.mipmap.icon_edit_back),
                contentDescription = "Back",
                modifier = Modifier
                    .padding(start = 11.dp)
                    .size(width = 11.dp, height = 20.dp)
            )
        }

        Text(
            text = "Video Edit",
            color = EditorTextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun EditorTopActions(
    resolutionPreset: ExportResolutionPreset,
    onResolutionSelected: (ExportResolutionPreset) -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ResolutionButton(
            selected = resolutionPreset,
            onSelected = onResolutionSelected
        )
        FloatingExportButton(
            onClick = onExportClick
        )
    }
}

@Composable
private fun ResolutionButton(
    selected: ExportResolutionPreset,
    onSelected: (ExportResolutionPreset) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier.size(width = 76.dp, height = 30.dp),
            shape = RoundedCornerShape(4.dp),
            color = EditorPanelRaised.copy(alpha = 0.94f),
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selected.label,
                    color = EditorTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ExportResolutionPreset.entries.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.label) },
                    onClick = {
                        onSelected(preset)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FloatingExportButton(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(width = 68.dp, height = 30.dp),
        shape = RoundedCornerShape(4.dp),
        color = EditorAccentGreen,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Next",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun EditorPreviewPane(
    clip: ClipDraft?,
    aspectPreset: AspectPreset,
    activeTool: EditorTool,
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
        contentAlignment = Alignment.Center
    ) {
        val previewModifier = if (aspectPreset.previewAspectRatio >= 1f) {
            Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(aspectPreset.previewAspectRatio)
        } else {
            Modifier
                .fillMaxHeight(0.74f)
                .aspectRatio(aspectPreset.previewAspectRatio)
        }

        Box(
            modifier = previewModifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(parseColor(clip?.adjustments?.backgroundHex ?: "#11161D"))),
            contentAlignment = Alignment.Center
        ) {
            when {
                clip == null -> EmptyPreview()
                activeTool == EditorTool.Filter -> GpuImagePreview(
                    clip = clip,
                    modifier = Modifier.fillMaxSize()
                )
                clip.mediaKind == MediaKind.Video -> VideoPreview(uri = Uri.parse(clip.uriString))
                else -> ImagePreview(uri = Uri.parse(clip.uriString))
            }

            if (clip != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Black.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = "${clip.displayName}  ${formatDuration(clip.visibleDurationMs())}",
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
            shape = RoundedCornerShape(999.dp),
            color = EditorPanelBackground.copy(alpha = 0.78f),
            tonalElevation = 2.dp
        ) {
            Text(
                text = statusMessage,
                color = EditorTextPrimary,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun EmptyPreview() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.1f)
        ) {
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }
        }
        Text(
            text = "No media",
            color = EditorTextPrimary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun VideoPreview(uri: Uri) {
    var isPlaying by rememberSaveable(uri.toString()) { mutableStateOf(true) }
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                VideoView(context).apply {
                    tag = uri.toString()
                    setVideoURI(uri)
                    setOnPreparedListener { player ->
                        player.isLooping = true
                        if (isPlaying) {
                            start()
                        }
                    }
                }
            },
            update = { view ->
                if (view.tag != uri.toString()) {
                    view.tag = uri.toString()
                    view.setVideoURI(uri)
                }
                if (isPlaying && !view.isPlaying) {
                    view.start()
                } else if (!isPlaying && view.isPlaying) {
                    view.pause()
                }
            }
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .clickable { isPlaying = !isPlaying },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlaying) "Pause" else "Play",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ImagePreview(uri: Uri) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(uri)
            }
        },
        update = { imageView ->
            imageView.setImageURI(uri)
        }
    )
}

@Composable
private fun EditorBottomTray(
    appState: ClipyAppState,
    onImportMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(EditorBottomTrayHeight)
            .background(EditorPanelBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SelectedMediaStrip(
                appState = appState,
                onImportMore = onImportMore
            )
            CompactToolPanel(
                appState = appState,
                selectedClip = appState.selectedClip
            )
        }
        EditorToolBar(
            activeTool = appState.activeTool,
            onToolSelected = appState::selectTool
        )
    }
}

@Composable
private fun SelectedMediaStrip(
    appState: ClipyAppState,
    onImportMore: () -> Unit
) {
    val timeline = appState.buildMediaTimeline()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SelectedStripHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoEditorTimelineStrip(
            timeline = timeline,
            selectedClipId = appState.selectedClipId,
            durationMs = appState.projectDurationMs,
            positionMs = 0L,
            onClipSelected = { clipId -> clipId?.let(appState::selectClip) },
            modifier = Modifier.weight(1f),
            zoom = 0.72f
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                    .clickable(onClick = onImportMore),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_add_media),
                    contentDescription = "Add media",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
    }
}

@Composable
private fun TimelineClipCard(
    clip: ClipDraft,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) EditorAccentBlue else Color.White.copy(alpha = 0.08f)
    val backgroundColor = if (isSelected) {
        EditorAccentBlue.copy(alpha = 0.18f)
    } else {
        EditorPanelRaised
    }

    OutlinedCard(
        modifier = Modifier
            .width(TimelineCardWidth)
            .height(TimelineCardHeight)
            .clickable(onClick = onClick),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = clip.displayName,
                color = Color.White,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (clip.mediaKind == MediaKind.Video) "VIDEO" else "IMAGE",
                color = EditorTextMuted,
                fontSize = 9.sp
            )
            Text(
                text = formatDuration(clip.visibleDurationMs()),
                color = EditorTextSecondary,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun EditorToolBar(
    activeTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit
) {
    VideoEditorToolBar(
        tools = EditorTool.entries.map { it.toReusableEditorTool() },
        selectedToolId = activeTool.name,
        onToolSelected = { selected ->
            EditorTool.entries.firstOrNull { it.name == selected.id }?.let(onToolSelected)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun EditorTool.toReusableEditorTool(): ReusableEditorTool = ReusableEditorTool(
    id = name,
    label = label,
    badge = toolBadge(this)
)

private fun toolBadge(tool: EditorTool): String {
    return when (tool) {
        EditorTool.Trim -> "TR"
        EditorTool.Canvas -> "9:16"
        EditorTool.Copy -> "CP"
        EditorTool.Background -> "BG"
        EditorTool.Speed -> "1X"
        EditorTool.Split -> "SP"
        EditorTool.Volume -> "VO"
        EditorTool.Filter -> "FX"
    }
}

@Composable
private fun CompactToolPanel(
    appState: ClipyAppState,
    selectedClip: ClipDraft?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp),
        shape = RoundedCornerShape(4.dp),
        color = EditorPanelRaised
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = appState.activeTool.label,
                color = EditorTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            when (appState.activeTool) {
                EditorTool.Trim -> TrimPanel(appState = appState, selectedClip = selectedClip)
                EditorTool.Canvas -> CanvasPanel(
                    selectedPreset = appState.aspectPreset,
                    onPresetSelected = appState::updateAspectPreset
                )
                EditorTool.Copy -> ActionPanel(
                    primaryLabel = "Duplicate selected clip",
                    secondaryLabel = "Make another copy",
                    onPrimary = appState::copySelectedClip
                )
                EditorTool.Background -> BackgroundPanel(
                    selectedHex = selectedClip?.adjustments?.backgroundHex,
                    onColorSelected = appState::updateSelectedBackground
                )
                EditorTool.Speed -> SpeedPanel(appState = appState, selectedClip = selectedClip)
                EditorTool.Split -> ActionPanel(
                    primaryLabel = "Split selected clip",
                    secondaryLabel = "Cut at the midpoint",
                    onPrimary = appState::splitSelectedClip
                )
                EditorTool.Volume -> VolumePanel(appState = appState, selectedClip = selectedClip)
                EditorTool.Filter -> FilterPanel(appState = appState, selectedClip = selectedClip)
            }
        }
    }
}

@Composable
private fun TrimPanel(
    appState: ClipyAppState,
    selectedClip: ClipDraft?
) {
    if (selectedClip == null) {
        PlaceholderPanel()
        return
    }

    var trimRange by remember(selectedClip.id, selectedClip.adjustments.trimStartMs, selectedClip.trimEndMs()) {
        mutableStateOf(
            selectedClip.adjustments.trimStartMs.toFloat()..selectedClip.trimEndMs().toFloat()
        )
    }
    val maxValue = selectedClip.sourceDurationMs.toFloat().coerceAtLeast(1000f)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricPill(label = "In", value = formatDuration(trimRange.start.toLong()))
            MetricPill(label = "Out", value = formatDuration(trimRange.endInclusive.toLong()))
        }
        RangeSlider(
            value = trimRange,
            valueRange = 0f..maxValue,
            onValueChange = { newRange ->
                val clampedEnd = newRange.endInclusive.coerceAtLeast(newRange.start + 250f)
                trimRange = newRange.start..clampedEnd
            },
            onValueChangeFinished = {
                appState.updateSelectedTrim(
                    startMs = trimRange.start.toLong(),
                    endMs = trimRange.endInclusive.toLong()
                )
            }
        )
    }
}

@Composable
private fun CanvasPanel(
    selectedPreset: AspectPreset,
    onPresetSelected: (AspectPreset) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AspectPreset.entries.forEach { preset ->
            FilterChip(
                selected = preset == selectedPreset,
                onClick = { onPresetSelected(preset) },
                label = { Text(preset.label) }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun ActionPanel(
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = secondaryLabel,
            color = EditorTextSecondary,
            fontSize = 11.sp
        )
        Button(
            onClick = onPrimary,
            colors = ButtonDefaults.buttonColors(
                containerColor = EditorAccentBlue,
                contentColor = Color.White
            )
        ) {
            Text(primaryLabel)
        }
    }
}

@Composable
private fun BackgroundPanel(
    selectedHex: String?,
    onColorSelected: (String) -> Unit
) {
    WrapFlowRow(
        horizontalSpacing = 10.dp,
        verticalSpacing = 10.dp
    ) {
        backgroundOptions.forEach { hex ->
            val isSelected = hex == selectedHex
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(parseColor(hex)))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) EditorAccentBlue else Color.White.copy(alpha = 0.14f),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(hex) }
            )
        }
    }
}

@Composable
private fun SpeedPanel(
    appState: ClipyAppState,
    selectedClip: ClipDraft?
) {
    val speed = selectedClip?.adjustments?.speed ?: 1f
    var sliderSpeed by remember(selectedClip?.id, speed) {
        mutableFloatStateOf(speed)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricPill(label = "Speed", value = String.format("%.2fx", sliderSpeed))
        Slider(
            value = sliderSpeed,
            onValueChange = { sliderSpeed = it },
            onValueChangeFinished = {
                appState.updateSelectedSpeed(sliderSpeed)
            },
            valueRange = 0.25f..3f
        )
    }
}

@Composable
private fun VolumePanel(
    appState: ClipyAppState,
    selectedClip: ClipDraft?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VolumeSliderRow(
            label = "Clip",
            value = selectedClip?.adjustments?.volume ?: 1f,
            onValueChange = appState::updateSelectedVolume
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        VolumeSliderRow(
            label = "Source",
            value = appState.sourceVolume,
            onValueChange = appState::updateSourceVolume
        )
        VolumeSliderRow(
            label = "Music",
            value = appState.musicVolume,
            onValueChange = appState::updateMusicVolume
        )
        VolumeSliderRow(
            label = "Voice",
            value = appState.voiceOverVolume,
            onValueChange = appState::updateVoiceOverVolume
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.finish),
                contentDescription = "Done",
                modifier = Modifier.size(width = 25.dp, height = 20.dp)
            )
        }
    }
}

@Composable
private fun FilterPanel(
    appState: ClipyAppState,
    selectedClip: ClipDraft?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { filter ->
                FilterChip(
                    selected = selectedClip?.adjustments?.filterName == filter,
                    onClick = { appState.updateSelectedFilter(filter) },
                    label = { Text(filter) }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        EffectSlider(
            label = "Brightness",
            value = selectedClip?.adjustments?.brightness ?: 0f,
            onValueChange = appState::updateSelectedBrightness
        )
        EffectSlider(
            label = "Contrast",
            value = selectedClip?.adjustments?.contrast ?: 0f,
            onValueChange = appState::updateSelectedContrast
        )
        EffectSlider(
            label = "Saturation",
            value = selectedClip?.adjustments?.saturation ?: 0f,
            onValueChange = appState::updateSelectedSaturation
        )
    }
}

@Composable
private fun EffectSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricPill(label = label, value = String.format("%.2f", value))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -1f..1f
        )
    }
}

@Composable
private fun VolumeSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MetricPill(label = label, value = "${(value * 100f).toInt()}%")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f
        )
    }
}

@Composable
private fun PlaceholderPanel() {
    Text(
        text = "Select a clip",
        color = EditorTextSecondary,
        fontSize = 11.sp
    )
}

@Composable
private fun WrapFlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 0.dp,
    verticalSpacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val hSpacingPx = horizontalSpacing.roundToPx()
        val vSpacingPx = verticalSpacing.roundToPx()
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(childConstraints) }
        val maxWidth = constraints.maxWidth

        data class PositionedPlaceable(
            val placeable: Placeable,
            val x: Int,
            val y: Int
        )

        val positioned = mutableListOf<PositionedPlaceable>()
        var rowHeight = 0
        var x = 0
        var y = 0
        var contentWidth = 0

        placeables.forEach { placeable ->
            val proposedWidth = if (x == 0) {
                placeable.width
            } else {
                x + hSpacingPx + placeable.width
            }
            val shouldWrap = maxWidth != Constraints.Infinity &&
                x > 0 &&
                proposedWidth > maxWidth

            if (shouldWrap) {
                y += rowHeight + vSpacingPx
                x = 0
                rowHeight = 0
            }

            val childX = if (x == 0) 0 else x + hSpacingPx
            positioned += PositionedPlaceable(
                placeable = placeable,
                x = childX,
                y = y
            )

            x = childX + placeable.width
            rowHeight = maxOf(rowHeight, placeable.height)
            contentWidth = maxOf(contentWidth, x)
        }

        val rawHeight = if (placeables.isEmpty()) 0 else y + rowHeight
        val layoutWidth = contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = rawHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(layoutWidth, layoutHeight) {
            positioned.forEach { item ->
                item.placeable.placeRelative(item.x, item.y)
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = EditorPanelBackground
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 24.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = EditorTextMuted,
                fontSize = 9.sp
            )
            Text(
                text = value,
                color = EditorTextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ExportScreen(
    appState: ClipyAppState,
    onBackHome: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportMessage by remember { mutableStateOf("Preparing export") }
    var exportOutputPath by remember { mutableStateOf<String?>(null) }
    var exportFinished by remember { mutableStateOf(false) }
    var exportFailed by remember { mutableStateOf(false) }
    val progressPercent = (exportProgress.coerceIn(0f, 1f) * 100).toInt()

    LaunchedEffect(Unit) {
        if (isExporting || exportFinished) return@LaunchedEffect
        isExporting = true
        exportFailed = false
        exportProgress = 0f
        exportOutputPath = null
        exportMessage = "Preparing export"
        val result = appState.exportWithMediaPipeline(context) { progress ->
            exportProgress = progress.progressPercent / 100f
            exportMessage = progress.message
            exportOutputPath = progress.outputPath ?: exportOutputPath
        }
        when (result) {
            is com.natncompany.media.MediaResult.Success -> {
                exportProgress = 1f
                exportOutputPath = result.value.ifBlank { exportOutputPath }
                exportMessage = "Export completed"
                appState.updateStatus("Saved to Movies/Clipy")
            }
            is com.natncompany.media.MediaResult.Failure -> {
                exportFailed = true
                exportMessage = result.error.message
                appState.updateStatus(result.error.message)
            }
        }
        isExporting = false
        exportFinished = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = if (exportFinished && !exportFailed) "Export completed" else "Exporting video",
                color = EditorTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            ExportProgressThumb(
                clip = appState.selectedClip ?: appState.clips.firstOrNull(),
                aspectPreset = appState.aspectPreset,
                progress = exportProgress,
                progressPercent = progressPercent,
                isFailed = exportFailed,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .heightIn(max = 430.dp)
            )

            Text(
                text = exportMessage,
                color = if (exportFailed) Color(0xFFFF8A8A) else EditorTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            exportOutputPath?.takeIf { exportFinished && !exportFailed }?.let { path ->
                Text(
                    text = path,
                    color = EditorAccentGreen,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (exportFinished) {
                Button(
                    onClick = onBackHome,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EditorAccentGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text("Back to Home")
                }
            }
        }
    }
}

@Composable
private fun ExportProgressThumb(
    clip: ClipDraft?,
    aspectPreset: AspectPreset,
    progress: Float,
    progressPercent: Int,
    isFailed: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.aspectRatio(aspectPreset.previewAspectRatio),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(parseColor(clip?.adjustments?.backgroundHex ?: "#11161D"))),
            contentAlignment = Alignment.Center
        ) {
            when {
                clip == null -> EmptyPreview()
                clip.mediaKind == MediaKind.Video -> ExportVideoPlaceholder(clip = clip)
                else -> ImagePreview(uri = Uri.parse(clip.uriString))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isFailed) "Failed" else "$progressPercent%",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = if (isFailed) Color(0xFFFF8A8A) else EditorAccentGreen,
                    trackColor = Color.White.copy(alpha = 0.22f)
                )
            }
        }
    }
}

@Composable
private fun ExportVideoPlaceholder(clip: ClipDraft) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorPreviewBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Video",
                color = EditorTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = clip.displayName,
                color = EditorTextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PipelineStageRow(stages: List<PipelineStage>) {
    WrapFlowRow(
        horizontalSpacing = 8.dp,
        verticalSpacing = 8.dp
    ) {
        stages.forEach { stage ->
            MetricPill(
                label = "Stage",
                value = stage.toDisplayLabel()
            )
        }
    }
}

private fun PipelineStage.toDisplayLabel(): String {
    return when (this) {
        PipelineStage.Timeline -> "Timeline"
        PipelineStage.MediaCodecPreview -> "MediaCodec Preview"
        PipelineStage.OpenGlEffect -> "OpenGL Effect"
        PipelineStage.MediaCodecExport -> "MediaCodec Export"
        PipelineStage.FfmpegFallback -> "FFmpeg Fallback"
    }
}
