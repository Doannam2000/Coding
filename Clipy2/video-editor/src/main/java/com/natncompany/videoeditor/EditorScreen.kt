package com.natncompany.videoeditor

import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.natncompany.media.Timeline
import com.natncompany.media.TimelineTrack
import com.natncompany.media.TrackType
import kotlin.math.roundToInt

private val ScreenBackground = Color(0xFF0B0D12)
private val TopBarBackground = Color(0xFF11141A)
private val CardBackground = Color(0xFF13161D)
private val CardInnerBackground = Color(0xFF0F1218)
private val StrokeColor = Color(0xFF232833)
private val PrimaryBlue = Color(0xFF5B8DEF)
private val PrimaryBlueSelected = Color(0xFF4D7FE0)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFAAB0BD)
private val TextDisabled = Color(0xFF6F7683)
private val PlayheadAccent = Color(0xFFFF5C7A)
private val PanelSurface = Color(0xFF171C25)
private val PanelSurfaceElevated = Color(0xFF1D2430)

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onExportClick: (() -> Unit)? = null,
    exportButtonLabel: String = "Export",
    qualityLabel: String? = null,
    qualityOptions: List<String> = emptyList(),
    onQualitySelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    VideoEditScreen(
        viewModel = viewModel,
        onBack = onBack,
        onNext = onExportClick ?: {},
        onResolutionClick = { if (qualityOptions.isNotEmpty()) onQualitySelected(qualityOptions.first()) },
        exportButtonLabel = exportButtonLabel,
        qualityLabel = qualityLabel ?: "1080p",
        modifier = modifier
    )

    state.criticalErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearCriticalError,
            confirmButton = { TextButton(onClick = viewModel::clearCriticalError) { Text("OK") } },
            title = { Text("Critical editor error") },
            text = { Text(message) }
        )
    }

}

@Composable
private fun ExportDialog(
    exportProgress: Int?,
    exportResultPath: String?,
    onDismiss: () -> Unit,
    onExport: (ExportQuality) -> Unit,
    onCancelExport: () -> Unit
) {
    var selectedQuality by remember { mutableStateOf(ExportQuality.High) }
    val isExporting = exportProgress != null

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = { Text("Export video") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Quality", style = MaterialTheme.typography.labelLarge)
                ExportQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isExporting) { selectedQuality = quality }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedQuality == quality,
                            onClick = { selectedQuality = quality },
                            enabled = !isExporting
                        )
                        Column {
                            Text(quality.label, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${quality.width}x${quality.height}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isExporting) {
                    val progress = exportProgress.coerceIn(0, 100)
                    Text("Rendering $progress%", fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                exportResultPath?.let { path ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Export completed", fontWeight = FontWeight.SemiBold)
                            Text(path, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(selectedQuality) },
                enabled = !isExporting
            ) {
                Text(if (exportResultPath == null) "Start export" else "Export again")
            }
        },
        dismissButton = {
            if (isExporting) {
                OutlinedButton(onClick = onCancelExport) { Text("Cancel") }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
private fun VideoEditorTopBar(
    onBack: () -> Unit,
    onExport: () -> Unit,
    exportButtonLabel: String,
    qualityLabel: String?,
    qualityOptions: List<String>,
    onQualitySelected: (String) -> Unit
) {
    var qualityExpanded by remember { mutableStateOf(false) }
    Surface(color = TopBarBackground) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .height(68.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp),
                contentPadding = PaddingValues(0.dp)
            ) { Text("‹", color = TextPrimary, style = MaterialTheme.typography.headlineSmall) }
            Text(
                text = "Edit",
                modifier = Modifier.weight(1f),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            if (qualityLabel != null && qualityOptions.isNotEmpty()) {
                Box {
                    OutlinedButton(
                        onClick = { qualityExpanded = true },
                        modifier = Modifier.size(width = 96.dp, height = 44.dp),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, StrokeColor),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) { Text(qualityLabel, maxLines = 1, color = TextPrimary) }
                    DropdownMenu(expanded = qualityExpanded, onDismissRequest = { qualityExpanded = false }) {
                        qualityOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onQualitySelected(option)
                                    qualityExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Button(
                onClick = onExport,
                modifier = Modifier.size(width = 104.dp, height = 48.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = TextPrimary),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) { Text(exportButtonLabel, maxLines = 1) }
        }
    }
}

@Composable
private fun PreviewArea(
    state: EditorUiState,
    onSurfaceChanged: (android.view.Surface?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 430.dp),
        shape = RoundedCornerShape(18.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, StrokeColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            PreviewPanel(
                state = state,
                onSurfaceChanged = onSurfaceChanged,
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardInnerBackground)
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSeekStart: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, StrokeColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPlayPause,
                modifier = Modifier.height(44.dp).widthIn(min = 76.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueSelected, contentColor = TextPrimary),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) { Text(if (isPlaying) "Pause" else "Play") }
            Text(
                text = "${position.formatTime()} / ${duration.formatTime()}",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = onSeekStart,
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, StrokeColor),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) { Text("Start", color = TextPrimary) }
        }
    }
}

@Composable
private fun PreviewPanel(
    state: EditorUiState,
    onSurfaceChanged: (android.view.Surface?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) = onSurfaceChanged(holder.surface)
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = onSurfaceChanged(holder.surface)
                        override fun surfaceDestroyed(holder: SurfaceHolder) = onSurfaceChanged(null)
                    })
                }
            }
        )

        when {
            state.project == null -> Text("Open a project to start editing", color = Color.White.copy(alpha = 0.7f))
            state.previewError != null -> PreviewErrorOverlay(state.previewError)
            !state.isPreviewPrepared -> PreviewLoadingOverlay()
        }

        if (state.importProgress != null || state.exportProgress != null) {
            LoadingOverlay(state.importProgress, state.exportProgress)
        }

        state.selectedClip?.let { clip ->
            FilterPreviewOverlay(clip)
        }
    }
}

@Composable
private fun FilterPreviewOverlay(clip: com.natncompany.media.TimelineClip) {
    val filterName = clip.effect.parameters[ClipEffectParameterFilterName].orEmpty().ifBlank { "Original" }
    val hasAdjustments = filterName != "Original" ||
        clip.transform.brightness != 0f ||
        clip.transform.contrast != 1f ||
        clip.transform.saturation != 1f ||
        clip.transform.blur != 0f
    if (!hasAdjustments) return

    val tint = filterName.previewTint()
    if (tint != Color.Transparent) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tint)
        )
    }
    if (clip.transform.brightness != 0f) {
        val brightnessColor = if (clip.transform.brightness > 0f) Color.White else Color.Black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brightnessColor.copy(alpha = kotlin.math.abs(clip.transform.brightness).coerceIn(0f, 1f) * 0.28f))
        )
    }
    if (clip.transform.saturation != 1f) {
        val alpha = kotlin.math.abs(clip.transform.saturation - 1f).coerceIn(0f, 1f) * 0.18f
        val saturationColor = if (clip.transform.saturation > 1f) Color(0xFFFF4D6D) else Color.Gray
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(saturationColor.copy(alpha = alpha))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (clip.transform.contrast > 1f) 0.04f else 0.12f))
    )
    Surface(
        modifier = Modifier
            .padding(12.dp),
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.62f)
    ) {
        Text(
            text = "Filter: $filterName",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

private fun String.previewTint(): Color {
    return when (this) {
        "Sepia" -> Color(0xB06B3F12)
        "Mono", "Monochrome", "Luminance" -> Color(0x8A808080)
        "Invert", "Solarize" -> Color.White.copy(alpha = 0.32f)
        "RGB Warm", "Exposure", "Gamma", "White Balance" -> Color(0x55FFB74D)
        "RGB Cool", "False Color", "CGA" -> Color(0x554A90E2)
        "Hue Shift", "Vignette" -> Color(0x554A148C)
        "Sketch", "Sobel Edge", "Sobel Threshold", "Threshold Edge", "Directional Edge", "Laplacian" -> Color.Black.copy(alpha = 0.42f)
        "Pixel", "Posterize", "Halftone", "Crosshatch" -> Color(0x4456C271)
        "Emboss", "Sharpen" -> Color.White.copy(alpha = 0.16f)
        "Gaussian Blur", "Box Blur", "Bilateral Blur", "Zoom Blur" -> Color.White.copy(alpha = 0.20f)
        "Haze" -> Color.White.copy(alpha = 0.26f)
        "Kuwahara", "Toon", "Smooth Toon" -> Color(0x44AFD800)
        "Swirl", "Bulge", "Glass Sphere", "Sphere" -> Color(0x44FF7AB6)
        else -> Color.Transparent
    }
}

@Composable
private fun PreviewLoadingOverlay() {
    Surface(color = Color.Black.copy(alpha = 0.48f), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
            Text("Preparing preview", color = Color.White)
        }
    }
}

@Composable
private fun PreviewErrorOverlay(message: String) {
    Surface(color = Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Preview unavailable", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(message, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LoadingOverlay(importProgress: Int?, exportProgress: Int?) {
    Surface(color = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
            val label = when {
                exportProgress != null -> "Exporting $exportProgress%"
                importProgress != null -> "Importing $importProgress%"
                else -> "Loading"
            }
            Text(label, color = Color.White)
            val progress = exportProgress ?: importProgress
            if (progress != null) {
                LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.width(180.dp))
            }
        }
    }
}

@Composable
private fun TimelineAndTools(
    state: EditorUiState,
    onClipSelected: (String?) -> Unit,
    onScrub: (Long) -> Unit,
    onClipMoved: (trackId: String, clipId: String, newStartMs: Long) -> Unit,
    onClipTrimStart: (trackId: String, clipId: String, newSourceStartMs: Long) -> Unit,
    onClipTrimEnd: (trackId: String, clipId: String, newSourceEndMs: Long) -> Unit,
    onImport: () -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onTrim: () -> Unit,
    onCrop: () -> Unit,
    onRotate: () -> Unit,
    onFilter: () -> Unit,
    onSpeed: () -> Unit,
    onVolume: () -> Unit,
    onAction: (EditorAction) -> Unit,
    onDismissToolPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var zoom by remember { mutableFloatStateOf(0.35f) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, StrokeColor)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Timeline", color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(state.position.formatTime(), color = TextSecondary)
                }
                TimelinePanel(
                    timeline = state.timeline,
                    selectedClipId = state.selectedClipId,
                    duration = state.duration,
                    position = state.position,
                    zoom = zoom,
                    onZoomChange = { zoom = it.coerceIn(0.2f, 4f) },
                    onClipSelected = onClipSelected,
                    onClipMoved = onClipMoved,
                    onClipTrimStart = onClipTrimStart,
                    onClipTrimEnd = onClipTrimEnd,
                    onScrub = onScrub
                )
                ToolPanel(
                    hasSelection = state.selectedClipId != null,
                    activeTool = state.activeTool,
                    onImport = onImport,
                    onSplit = onSplit,
                    onDelete = onDelete,
                    onDuplicate = onDuplicate,
                    onTrim = onTrim,
                    onCrop = onCrop,
                    onRotate = onRotate,
                    onFilter = onFilter,
                    onSpeed = onSpeed,
                    onVolume = onVolume
                )
            }
            SelectedToolPanel(
                activeTool = state.activeTool,
                selectedClip = state.selectedClip,
                onAction = onAction,
                onDismiss = onDismissToolPanel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .heightIn(max = 260.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TimelinePanel(
    timeline: Timeline,
    selectedClipId: String?,
    duration: Long,
    position: Long,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onClipSelected: (String?) -> Unit,
    onClipMoved: (trackId: String, clipId: String, newStartMs: Long) -> Unit,
    onClipTrimStart: (trackId: String, clipId: String, newSourceStartMs: Long) -> Unit,
    onClipTrimEnd: (trackId: String, clipId: String, newSourceEndMs: Long) -> Unit,
    onScrub: (Long) -> Unit
) {
    val timelineDuration = duration.coerceAtLeast(1L)
    val scrollState = rememberScrollState()
    val pixelsPerMs = 0.052f * zoom
    val timelineWidth = ((timelineDuration * pixelsPerMs).roundToInt().coerceAtLeast(360)).dp
    val tracks = timeline.tracks

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(((tracks.size.coerceAtLeast(1) * 64) + 28).dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardInnerBackground)
                .border(1.dp, StrokeColor, RoundedCornerShape(12.dp))
                .pointerInput(zoom) {
                    detectTransformGestures { _, _, gestureZoom, _ ->
                        onZoomChange(zoom * gestureZoom)
                    }
                }
        ) {
            Column(modifier = Modifier.width(64.dp).fillMaxHeight().padding(vertical = 10.dp)) {
                tracks.ifEmpty { listOf(TimelineTrack(id = "empty", type = TrackType.Video)) }.forEach { track ->
                    TrackLabel(track = track, modifier = Modifier.height(56.dp))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(timelineWidth)
                        .fillMaxHeight()
                        .clickable { onClipSelected(null) }
                        .pointerInput(pixelsPerMs, scrollState.value) {
                            detectTapGestures { offset ->
                                val targetMs = ((offset.x + scrollState.value) / pixelsPerMs).toLong()
                                onScrub(targetMs.coerceIn(0L, timelineDuration))
                            }
                        }
                ) {
                    if (tracks.all { it.clips.isEmpty() }) {
                        Text(
                            text = "Import media to build your timeline",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }

                    tracks.forEachIndexed { index, track ->
                        TrackRow(
                            track = track,
                            selectedClipId = selectedClipId,
                            pixelsPerMs = pixelsPerMs,
                            modifier = Modifier
                                .padding(top = (10 + index * 64).dp)
                                .height(48.dp)
                                .fillMaxWidth(),
                            onClipSelected = onClipSelected,
                            onClipDragged = { clip, deltaMs ->
                                onClipMoved(track.id, clip.id, (clip.timelineStartMs + deltaMs).coerceAtLeast(0L))
                            },
                            onClipTrimStartDragged = { clip, deltaMs ->
                                val newStart = (clip.sourceStartMs + deltaMs).coerceIn(0L, clip.sourceEndMs - 100L)
                                onClipTrimStart(track.id, clip.id, newStart)
                            },
                            onClipTrimEndDragged = { clip, deltaMs ->
                                val newEnd = (clip.sourceEndMs + deltaMs).coerceIn(clip.sourceStartMs + 100L, clip.sourceDurationMs)
                                onClipTrimEnd(track.id, clip.id, newEnd)
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { IntOffset((position * pixelsPerMs).roundToInt(), 0) }
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(PlayheadAccent)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackLabel(track: TimelineTrack, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
        Text(
            text = track.type.name,
            color = if (track.isEnabled) TextSecondary else TextDisabled,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun ToolPanel(
    hasSelection: Boolean,
    activeTool: EditorTool?,
    onImport: () -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onTrim: () -> Unit,
    onCrop: () -> Unit,
    onRotate: () -> Unit,
    onFilter: () -> Unit,
    onSpeed: () -> Unit,
    onVolume: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        item(key = "import") { EditorToolChip(label = "Import", selected = false, enabled = true, onClick = onImport) }
        item(key = "split") { EditorToolChip(label = "Split", selected = false, enabled = hasSelection, onClick = onSplit) }
        item(key = "delete") { EditorToolChip(label = "Delete", selected = false, enabled = hasSelection, onClick = onDelete) }
        item(key = "copy") { EditorToolChip(label = "Copy", selected = false, enabled = hasSelection, onClick = onDuplicate) }
        item(key = EditorTool.Trim.name) {
            EditorToolChip(label = EditorTool.Trim.label, selected = activeTool == EditorTool.Trim, enabled = hasSelection, onClick = onTrim)
        }
        item(key = EditorTool.Crop.name) {
            EditorToolChip(label = EditorTool.Crop.label, selected = activeTool == EditorTool.Crop, enabled = hasSelection, onClick = onCrop)
        }
        item(key = EditorTool.Rotate.name) {
            EditorToolChip(label = EditorTool.Rotate.label, selected = activeTool == EditorTool.Rotate, enabled = hasSelection, onClick = onRotate)
        }
        item(key = EditorTool.Filter.name) {
            EditorToolChip(label = EditorTool.Filter.label, selected = activeTool == EditorTool.Filter, enabled = hasSelection, onClick = onFilter)
        }
        item(key = EditorTool.Speed.name) {
            EditorToolChip(label = EditorTool.Speed.label, selected = activeTool == EditorTool.Speed, enabled = hasSelection, onClick = onSpeed)
        }
        item(key = EditorTool.Volume.name) {
            EditorToolChip(label = EditorTool.Volume.label, selected = activeTool == EditorTool.Volume, enabled = hasSelection, onClick = onVolume)
        }
    }
}

@Composable
private fun EditorToolChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) PrimaryBlueSelected.copy(alpha = 0.22f) else Color.Transparent
    val border = if (selected) PrimaryBlue else StrokeColor
    Surface(
        modifier = Modifier
            .height(46.dp)
            .widthIn(min = 62.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = BorderStroke(1.dp, border)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = if (enabled) TextSecondary else TextDisabled, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun ToolButton(tool: EditorTool, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent
        )
    ) {
        Text(tool.label)
    }
}

@Composable
private fun SelectedToolPanel(
    activeTool: EditorTool?,
    selectedClip: com.natncompany.media.TimelineClip?,
    onAction: (EditorAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedClip == null || activeTool == null) return

    Box(modifier = modifier) {
        when (activeTool) {
            EditorTool.Filter -> FilterToolPanel(selectedClip = selectedClip, onAction = onAction, onDismiss = onDismiss)
            EditorTool.Crop, EditorTool.Rotate -> CropRotateToolPanel(onAction = onAction, onDismiss = onDismiss)
            EditorTool.Volume -> VolumeToolPanel(
                volume = selectedClip.audio.volume,
                muted = selectedClip.audio.isMuted,
                onAction = onAction,
                onDismiss = onDismiss
            )
            else -> Unit
        }
    }
}

@Composable
private fun FilterToolPanel(
    selectedClip: com.natncompany.media.TimelineClip,
    onAction: (EditorAction) -> Unit,
    onDismiss: () -> Unit
) {
    ToolDetailSurface(title = "Filter", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                legacyFilterOptions.forEach { filter ->
                    OutlinedButton(
                        onClick = { onAction(EditorAction.ApplyNamedFilter(filter)) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StrokeColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(filter, color = TextPrimary)
                    }
                }
            }
            FilterSlider(
                label = "Brightness",
                value = selectedClip.transform.brightness,
                onValueChange = { onAction(EditorAction.SetBrightness(it)) }
            )
            FilterSlider(
                label = "Contrast",
                value = selectedClip.transform.contrast - 1f,
                onValueChange = { onAction(EditorAction.SetContrast(it)) }
            )
            FilterSlider(
                label = "Saturation",
                value = selectedClip.transform.saturation - 1f,
                onValueChange = { onAction(EditorAction.SetSaturation(it)) }
            )
        }
    }
}

@Composable
private fun FilterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$label ${String.format(java.util.Locale.US, "%.2f", value)}", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelMedium)
        Slider(
            value = value.coerceIn(-1f, 1f),
            onValueChange = onValueChange,
            valueRange = -1f..1f
        )
    }
}

@Composable
private fun CropRotateToolPanel(onAction: (EditorAction) -> Unit, onDismiss: () -> Unit) {
    ToolDetailSurface(title = "Crop / Rotate", onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onAction(EditorAction.RotateLeft) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StrokeColor),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) { Text("Rotate left", color = TextPrimary) }
            OutlinedButton(
                onClick = { onAction(EditorAction.RotateRight) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StrokeColor),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) { Text("Rotate right", color = TextPrimary) }
            OutlinedButton(
                onClick = { onAction(EditorAction.FlipHorizontal) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StrokeColor),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) { Text("Flip horizontal", color = TextPrimary) }
            OutlinedButton(
                onClick = { onAction(EditorAction.FlipVertical) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StrokeColor),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) { Text("Flip vertical", color = TextPrimary) }
        }
    }
}

@Composable
private fun VolumeToolPanel(
    volume: Float,
    muted: Boolean,
    onAction: (EditorAction) -> Unit,
    onDismiss: () -> Unit
) {
    ToolDetailSurface(title = "Volume", onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("0", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            Slider(
                value = volume.coerceIn(0f, 1f),
                onValueChange = { onAction(EditorAction.SetVolume(it)) },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
            Text("1", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mute", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelMedium)
                Switch(
                    checked = muted,
                    onCheckedChange = { onAction(EditorAction.SetMuted(it)) }
                )
            }
        }
    }
}

@Composable
private fun ToolDetailSurface(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PanelSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, StrokeColor),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelSurfaceElevated)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = TextPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) { Text("Apply") }
            }
            content()
        }
    }
}

private fun Long.formatTime(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
