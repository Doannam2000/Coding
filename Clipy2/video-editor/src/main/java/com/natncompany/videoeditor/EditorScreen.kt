package com.natncompany.videoeditor

import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.onAction(EditorAction.Import(uris))
    }

    LaunchedEffect(state.snackbarErrorMessage) {
        state.snackbarErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbarError()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            EditorTopBar(
                projectName = state.project?.name ?: "Untitled project",
                onBack = onBack,
                onExport = { showExportDialog = true }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF101217)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PreviewArea(
                state = state,
                onSurfaceChanged = viewModel::setSurface,
                onPlayPause = { viewModel.onAction(EditorAction.PlayPause) },
                onSeekStart = { viewModel.onAction(EditorAction.Seek(0L)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            TimelineAndTools(
                state = state,
                onClipSelected = { viewModel.onAction(EditorAction.SelectClip(it)) },
                onScrub = { viewModel.onAction(EditorAction.Seek(it)) },
                onClipMoved = { trackId, clipId, newStartMs -> viewModel.onAction(EditorAction.MoveClip(trackId, clipId, newStartMs)) },
                onClipTrimStart = { trackId, clipId, newSourceStartMs ->
                    viewModel.onAction(EditorAction.TrimClip(trackId, clipId, newSourceStartMs = newSourceStartMs))
                },
                onClipTrimEnd = { trackId, clipId, newSourceEndMs ->
                    viewModel.onAction(EditorAction.TrimClip(trackId, clipId, newSourceEndMs = newSourceEndMs))
                },
                onImport = { importLauncher.launch(arrayOf("video/*", "audio/*", "image/*")) },
                onSplit = { viewModel.onAction(EditorAction.Split) },
                onDelete = { viewModel.onAction(EditorAction.Delete) },
                onDuplicate = viewModel::duplicateSelectedClip,
                onTrim = { viewModel.onAction(EditorAction.SelectTool(EditorTool.Trim)) },
                onCrop = { viewModel.onAction(EditorAction.SelectTool(EditorTool.Crop)) },
                onRotate = { viewModel.onAction(EditorAction.SelectTool(EditorTool.Rotate)) },
                onFilter = { viewModel.onAction(EditorAction.SelectTool(EditorTool.Filter)) },
                onSpeed = { viewModel.onAction(EditorAction.SelectTool(EditorTool.Speed)) },
                onVolume = { viewModel.onAction(EditorAction.SelectTool(EditorTool.Volume)) },
                onAction = viewModel::onAction
            )
        }
    }

    state.criticalErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearCriticalError,
            confirmButton = { TextButton(onClick = viewModel::clearCriticalError) { Text("OK") } },
            title = { Text("Critical editor error") },
            text = { Text(message) }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            exportProgress = state.exportProgress,
            exportResultPath = state.exportResultPath,
            onDismiss = { showExportDialog = false },
            onExport = { quality -> viewModel.onAction(EditorAction.Export(quality)) },
            onCancelExport = viewModel::cancelExport
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
private fun EditorTopBar(projectName: String, onBack: () -> Unit, onExport: () -> Unit) {
    Surface(color = Color(0xFF181B22), tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text(
                text = projectName,
                modifier = Modifier.weight(1f),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Button(onClick = onExport) { Text("Export") }
        }
    }
}

@Composable
private fun PreviewArea(
    state: EditorUiState,
    onSurfaceChanged: (android.view.Surface?) -> Unit,
    onPlayPause: () -> Unit,
    onSeekStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = Color(0xFF080A0F)) {
        Column(modifier = Modifier.fillMaxSize()) {
            PreviewPanel(
                state = state,
                onSurfaceChanged = onSurfaceChanged,
                onPlayPause = onPlayPause,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onPlayPause) { Text(if (state.isPlaying) "Pause" else "Play") }
                Text(
                    text = "${state.position.formatTime()} / ${state.duration.formatTime()}",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onSeekStart) { Text("Start") }
            }
        }
    }
}

@Composable
private fun PreviewPanel(
    state: EditorUiState,
    onSurfaceChanged: (android.view.Surface?) -> Unit,
    onPlayPause: () -> Unit,
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

        if (state.project != null && state.previewError == null) {
            Button(onClick = onPlayPause) { Text(if (state.isPlaying) "Pause" else "Play") }
        }
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
    onAction: (EditorAction) -> Unit
) {
    var zoom by remember { mutableFloatStateOf(1f) }

    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF181B22)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Timeline", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(state.position.formatTime(), color = Color.White.copy(alpha = 0.7f))
            }
            TimelinePanel(
                timeline = state.timeline,
                selectedClipId = state.selectedClipId,
                duration = state.duration,
                position = state.position,
                zoom = zoom,
                onZoomChange = { zoom = it.coerceIn(0.5f, 4f) },
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
            SelectedToolPanel(
                activeTool = state.activeTool,
                selectedClip = state.selectedClip,
                onAction = onAction
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
    val timelineWidth = ((timelineDuration * pixelsPerMs).roundToInt().coerceAtLeast(640)).dp
    val tracks = timeline.tracks

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Zoom", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            OutlinedButton(onClick = { onZoomChange(zoom - 0.25f) }) { Text("-") }
            Text("${(zoom * 100).roundToInt()}%", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelMedium)
            OutlinedButton(onClick = { onZoomChange(zoom + 0.25f) }) { Text("+") }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(((tracks.size.coerceAtLeast(1) * 64) + 28).dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF101217))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.width(76.dp).fillMaxHeight().padding(vertical = 12.dp)) {
                tracks.ifEmpty { listOf(TimelineTrack(id = "empty", type = TrackType.Video)) }.forEach { track ->
                    TrackLabel(track = track, modifier = Modifier.height(64.dp))
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
                            detectDragGestures { change, _ ->
                                val targetMs = ((change.position.x + scrollState.value) / pixelsPerMs).toLong()
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
                                .padding(top = (12 + index * 64).dp)
                                .height(52.dp)
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
                            .offset { IntOffset((position * pixelsPerMs).roundToInt(), 0) }
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFFF4D6D))
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
            color = Color.White.copy(alpha = if (track.isEnabled) 0.76f else 0.36f),
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
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onImport, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF56C271))) {
            Text(EditorTool.Import.label)
        }
        ToolButton(tool = EditorTool.Split, selected = false, enabled = hasSelection, onClick = onSplit)
        ToolButton(tool = EditorTool.Delete, selected = false, enabled = hasSelection, onClick = onDelete)
        ToolButton(tool = EditorTool.Duplicate, selected = false, enabled = hasSelection, onClick = onDuplicate)
        ToolButton(tool = EditorTool.Trim, selected = activeTool == EditorTool.Trim, enabled = hasSelection, onClick = onTrim)
        ToolButton(tool = EditorTool.Crop, selected = activeTool == EditorTool.Crop, enabled = hasSelection, onClick = onCrop)
        ToolButton(tool = EditorTool.Rotate, selected = activeTool == EditorTool.Rotate, enabled = hasSelection, onClick = onRotate)
        ToolButton(tool = EditorTool.Filter, selected = activeTool == EditorTool.Filter, enabled = hasSelection, onClick = onFilter)
        ToolButton(tool = EditorTool.Speed, selected = activeTool == EditorTool.Speed, enabled = hasSelection, onClick = onSpeed)
        ToolButton(tool = EditorTool.Volume, selected = activeTool == EditorTool.Volume, enabled = hasSelection, onClick = onVolume)
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
    onAction: (EditorAction) -> Unit
) {
    if (selectedClip == null || activeTool == null) return

    when (activeTool) {
        EditorTool.Filter -> FilterToolPanel(onAction = onAction)
        EditorTool.Crop, EditorTool.Rotate -> CropRotateToolPanel(onAction = onAction)
        EditorTool.Volume -> VolumeToolPanel(
            volume = selectedClip.audio.volume,
            muted = selectedClip.audio.isMuted,
            onAction = onAction
        )
        else -> Unit
    }
}

@Composable
private fun FilterToolPanel(onAction: (EditorAction) -> Unit) {
    ToolDetailSurface(title = "Filter") {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ClipFilter.entries.forEach { filter ->
                OutlinedButton(onClick = { onAction(EditorAction.ApplyFilter(filter)) }) {
                    Text(filter.label)
                }
            }
        }
    }
}

@Composable
private fun CropRotateToolPanel(onAction: (EditorAction) -> Unit) {
    ToolDetailSurface(title = "Crop / Rotate") {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { onAction(EditorAction.RotateLeft) }) { Text("Rotate left") }
            OutlinedButton(onClick = { onAction(EditorAction.RotateRight) }) { Text("Rotate right") }
            OutlinedButton(onClick = { onAction(EditorAction.FlipHorizontal) }) { Text("Flip horizontal") }
            OutlinedButton(onClick = { onAction(EditorAction.FlipVertical) }) { Text("Flip vertical") }
        }
    }
}

@Composable
private fun VolumeToolPanel(
    volume: Float,
    muted: Boolean,
    onAction: (EditorAction) -> Unit
) {
    ToolDetailSurface(title = "Volume") {
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
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF101217),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
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
