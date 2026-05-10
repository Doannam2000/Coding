package com.natncompany.videoeditor

import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.natncompany.media.Timeline
import com.natncompany.media.TimelineClip
import com.natncompany.media.Crop
import com.natncompany.media.AssetType
import com.natncompany.media.VideoProject
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGaussianBlurFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val cropRatioOptions = listOf("Original", "1:1", "4:5", "9:16", "16:9")
private val speedOptions = listOf(0.5f, 1f, 1.5f, 2f)
private val bottomEditorTools = listOf(
    EditorTool.Trim,
    EditorTool.Crop,
    EditorTool.Rotate,
    EditorTool.Filter,
    EditorTool.Speed,
    EditorTool.Music,
    EditorTool.Volume
)

private val CScreenBackground = Color(0xFF0B0D12)
private val CTopBarBackground = Color(0xFF11141A)
private val CCardBackground = Color(0xFF151820)
private val CCardInnerBackground = Color(0xFF0F1218)
private val CStrokeColor = Color(0xFF2A2F3A)
private val CPrimaryBlue = Color(0xFF5B8DEF)
private val CTextPrimary = Color(0xFFFFFFFF)
private val CTextSecondary = Color(0xFFAAB0BD)
private val CTextDisabled = Color(0xFF6F7683)
private val CPlayheadAccent = Color(0xFFFF5C7A)

@Composable
fun VideoEditScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onResolutionClick: () -> Unit,
    exportButtonLabel: String = "Next",
    qualityLabel: String = "1080p",
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var importMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedClip = state.selectedClip
    val importVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.onAction(EditorAction.Import(listOf(uri)))
    }
    val importImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.onAction(EditorAction.Import(listOf(uri)))
    }
    val importAudioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.onAction(EditorAction.Import(listOf(uri)))
    }

    LaunchedEffect(state.snackbarErrorMessage) {
        state.snackbarErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbarError()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = CScreenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomFunctionBar(
                selectedTool = state.activeTool,
                onToolSelected = { tool ->
                    when (tool) {
                        EditorTool.Trim,
                        EditorTool.Crop,
                        EditorTool.Filter,
                        EditorTool.Volume,
                        EditorTool.Speed,
                        EditorTool.Rotate -> viewModel.onAction(EditorAction.SelectTool(tool))
                        EditorTool.Music -> importAudioLauncher.launch(arrayOf("audio/*"))
                        else -> Unit
                    }
                },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            VideoEditorTopBar(
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                qualityLabel = qualityLabel,
                nextLabel = exportButtonLabel,
                onBack = onBack,
                onUndo = { viewModel.onAction(EditorAction.Undo) },
                onRedo = { viewModel.onAction(EditorAction.Redo) },
                onResolutionClick = onResolutionClick,
                onNext = onNext
            )
            PreviewAreaNew(
                state = state,
                cropRatioLabel = selectedClip?.cropLabel() ?: "Original",
                showCropOverlay = state.activeTool == EditorTool.Crop && state.selectedClip != null,
                onSurfaceChanged = viewModel::setSurface,
                modifier = Modifier.weight(1f)
            )
            PlaybackControlBar(
                isPlaying = state.isPlaying,
                position = state.position,
                duration = state.duration,
                onPlayPause = { viewModel.onAction(EditorAction.PlayPause) },
                onSeekTo = { viewModel.onAction(EditorAction.Seek(it)) }
            )
            TimelinePanel(
                timeline = state.timeline,
                selectedClipId = state.selectedClipId,
                selectedClip = selectedClip,
                activeTool = state.activeTool,
                positionMs = state.position,
                durationMs = state.duration,
                zoomScale = zoomScale,
                onZoomInTimeline = { zoomScale = (zoomScale + 0.1f).coerceAtMost(4f) },
                onZoomOutTimeline = { zoomScale = (zoomScale - 0.1f).coerceAtLeast(0.5f) },
                onTimelineZoomChanged = { zoomScale = it.coerceIn(0.5f, 4f) },
                onSeekTo = { viewModel.onAction(EditorAction.Seek(it)) },
                onSelectClip = { viewModel.onAction(EditorAction.SelectClip(it)) },
                onTrimStartChanged = { id, time ->
                    state.timeline.findClipTrack(id)?.let { viewModel.onAction(EditorAction.TrimClip(it.id, id, newSourceStartMs = time)) }
                },
                onTrimEndChanged = { id, time ->
                    state.timeline.findClipTrack(id)?.let { viewModel.onAction(EditorAction.TrimClip(it.id, id, newSourceEndMs = time)) }
                },
                onClipMoved = { trackId, clipId, newStartMs ->
                    viewModel.onAction(EditorAction.MoveClip(trackId, clipId, newStartMs))
                },
                currentCropRatio = selectedClip?.cropLabel() ?: "Original",
                currentSpeed = selectedClip?.speedMultiplier() ?: 1f,
                isComparingOriginal = state.isComparingOriginal,
                onAction = viewModel::onAction,
                onImportVideo = { importVideoLauncher.launch(arrayOf("video/*")) },
                onImportImage = { importImageLauncher.launch(arrayOf("image/*")) },
                onAddMoreMedia = { importMenuExpanded = true }
            )
        }
    }

    DropdownMenu(expanded = importMenuExpanded, onDismissRequest = { importMenuExpanded = false }) {
        DropdownMenuItem(text = { Text("Import Video", fontSize = 12.sp) }, onClick = {
            importMenuExpanded = false
            importVideoLauncher.launch(arrayOf("video/*"))
        })
        DropdownMenuItem(text = { Text("Import Image", fontSize = 12.sp) }, onClick = {
            importMenuExpanded = false
            importImageLauncher.launch(arrayOf("image/*"))
        })
        DropdownMenuItem(text = { Text("Import Media", fontSize = 12.sp) }, onClick = {
            importMenuExpanded = false
            importVideoLauncher.launch(arrayOf("video/*", "image/*", "audio/*"))
        })
    }
}

@Composable
fun VideoEditorTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    qualityLabel: String,
    nextLabel: String,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResolutionClick: () -> Unit,
    onNext: () -> Unit
) {
    Surface(color = CTopBarBackground) {
        Row(modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CTextPrimary) }
            Text("Edit", color = CTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = CTextPrimary.copy(alpha = if (canUndo) 1f else 0.35f)) }
            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = CTextPrimary.copy(alpha = if (canRedo) 1f else 0.35f)) }
            OutlinedButton(onClick = onResolutionClick, modifier = Modifier.size(width = 82.dp, height = 36.dp), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CStrokeColor), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(qualityLabel, color = CTextPrimary, fontSize = 12.sp, maxLines = 1)
            }
            Spacer(Modifier.width(6.dp))
            Button(onClick = onNext, modifier = Modifier.size(width = 82.dp, height = 38.dp), shape = RoundedCornerShape(19.dp), colors = ButtonDefaults.buttonColors(containerColor = CPrimaryBlue)) {
                Text(nextLabel, color = CTextPrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun PreviewAreaNew(
    state: EditorUiState,
    cropRatioLabel: String,
    showCropOverlay: Boolean,
    onSurfaceChanged: (android.view.Surface?) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedClip = state.selectedClip
    val previewAspectRatio = aspectRatioForLabel(cropRatioLabel)
    Surface(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), color = CCardBackground, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CStrokeColor)) {
        Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .aspectRatio(previewAspectRatio)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        RealtimeGpuVideoView(context).apply {
                            setOnInputSurfaceChanged(onSurfaceChanged)
                            updateClip(selectedClip, state.isComparingOriginal)
                        }
                    },
                    update = { view ->
                        view.updateClip(selectedClip, state.isComparingOriginal)
                    }
                )
                if (!state.isPlaying) {
                    Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(999.dp)).background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                        Text("▶", color = CTextPrimary, fontSize = 14.sp)
                    }
                }
                if (showCropOverlay) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.9f)
                            .aspectRatio(previewAspectRatio)
                            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    )
                    Text(
                        text = cropRatioLabel,
                        color = CTextPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.EditorPreviewEffects(clip: TimelineClip) {
    val filterName = clip.effect.parameters[ClipEffectParameterFilterName].orEmpty()
    if (filterName.isNotBlank() && filterName != "Original") {
        val tint = when (filterName) {
            "Sepia" -> Color(0x666B3F12)
            "Mono", "Monochrome", "Luminance" -> Color.Gray.copy(alpha = 0.28f)
            "RGB Warm", "Exposure", "Gamma", "White Balance" -> Color(0x33FFB74D)
            "RGB Cool", "False Color", "CGA" -> Color(0x334A90E2)
            "Gaussian Blur", "Box Blur", "Bilateral Blur" -> Color.White.copy(alpha = 0.18f)
            else -> CPrimaryBlue.copy(alpha = 0.16f)
        }
        Box(Modifier.fillMaxSize().background(tint))
    }
    if (clip.transform.brightness != 0f) {
        Box(Modifier.fillMaxSize().background((if (clip.transform.brightness > 0f) Color.White else Color.Black).copy(alpha = kotlin.math.abs(clip.transform.brightness) * 0.25f)))
    }
    if (clip.transform.rotationDegrees != 0f || clip.transform.flipHorizontal || clip.transform.flipVertical) {
        Text(
            text = "Rotate ${clip.transform.rotationDegrees.toInt()}°" + if (clip.transform.flipHorizontal || clip.transform.flipVertical) " / Flip" else "",
            color = CTextPrimary,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
    clip.effect.parameters[ClipEffectParameterSpeed]?.let { speed ->
        Text(
            text = "Speed ${speed}x",
            color = CTextPrimary,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PlaybackControlBar(isPlaying: Boolean, position: Long, duration: Long, onPlayPause: () -> Unit, onSeekTo: (Long) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = CCardBackground, shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CStrokeColor)) {
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPlayPause, modifier = Modifier.size(44.dp)) { Text(if (isPlaying) "||" else "▶", color = CTextPrimary, fontSize = 14.sp) }
            Text("${formatClock(position)} / ${formatClock(duration)}", color = CTextSecondary, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onSeekTo(0L) }, modifier = Modifier.size(44.dp)) { Text("0", color = CTextSecondary, fontSize = 14.sp) }
        }
    }
}

@Composable
fun TimelinePanel(
    timeline: Timeline,
    selectedClipId: String?,
    selectedClip: TimelineClip?,
    activeTool: EditorTool?,
    positionMs: Long,
    durationMs: Long,
    zoomScale: Float,
    onZoomInTimeline: () -> Unit,
    onZoomOutTimeline: () -> Unit,
    onTimelineZoomChanged: (Float) -> Unit,
    onSeekTo: (Long) -> Unit,
    onSelectClip: (String?) -> Unit,
    onTrimStartChanged: (String, Long) -> Unit,
    onTrimEndChanged: (String, Long) -> Unit,
    onClipMoved: (String, String, Long) -> Unit,
    currentCropRatio: String,
    currentSpeed: Float,
    isComparingOriginal: Boolean,
    onAction: (EditorAction) -> Unit,
    onImportVideo: () -> Unit,
    onImportImage: () -> Unit,
    onAddMoreMedia: () -> Unit
) {
    val timelineDuration = durationMs.coerceAtLeast(timeline.durationMs).coerceAtLeast(1L)
    val pixelsPerMs = 0.04f * zoomScale
    val timelineWidth = ((timelineDuration * pixelsPerMs).roundToInt().coerceAtLeast(440)).dp
    val scrollState = rememberScrollState()
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).heightIn(min = 260.dp, max = 320.dp), color = CCardBackground, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CStrokeColor)) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Timeline", color = CTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    TimelineZoomControls(zoomScale = zoomScale, onZoomOutTimeline = onZoomOutTimeline, onZoomInTimeline = onZoomInTimeline)
                    Spacer(Modifier.width(8.dp))
                    Text(formatClock(positionMs), color = CTextSecondary, fontSize = 16.sp)
                }
                if (timeline.tracks.all { it.clips.isEmpty() }) {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("Import video or image to start editing", color = CTextSecondary, fontSize = 12.sp)
                        ImportMediaButton(onImportVideo = onImportVideo, onImportImage = onImportImage, onAddMoreMedia = onAddMoreMedia)
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Text("Video", color = CTextSecondary, fontSize = 12.sp, modifier = Modifier.width(44.dp).padding(top = 26.dp))
                        Box(modifier = Modifier.weight(1f).pointerInput(zoomScale) { detectTransformGestures { _, _, gestureZoom, _ -> onTimelineZoomChanged(zoomScale * gestureZoom) } }) {
                            Column(modifier = Modifier.horizontalScroll(scrollState)) {
                                TimelineRuler(durationMs = timelineDuration, width = timelineWidth)
                                TimelineTrack(
                                    timeline = timeline,
                                    selectedClipId = selectedClipId,
                                    activeTool = activeTool,
                                    pixelsPerMs = pixelsPerMs,
                                    width = timelineWidth,
                                    scrollOffsetPx = scrollState.value,
                                    onSelectClip = onSelectClip,
                                    onTrimStartChanged = onTrimStartChanged,
                                    onTrimEndChanged = onTrimEndChanged,
                                    onClipMoved = onClipMoved,
                                    onSeekTo = onSeekTo,
                                    onAddMoreMedia = onAddMoreMedia
                                )
                            }
                            Box(modifier = Modifier.align(Alignment.TopCenter).width(2.dp).fillMaxHeight().background(CPlayheadAccent))
                        }
                    }
                }
                ActiveToolActionPanel(
                    activeTool = activeTool,
                    selectedClip = selectedClip,
                    currentCropRatio = currentCropRatio,
                    currentSpeed = currentSpeed,
                    isComparingOriginal = isComparingOriginal,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
private fun ActiveToolActionPanel(
    activeTool: EditorTool?,
    selectedClip: TimelineClip?,
    currentCropRatio: String,
    currentSpeed: Float,
    isComparingOriginal: Boolean,
    onAction: (EditorAction) -> Unit
) {
    if (activeTool == null || selectedClip == null) return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .padding(top = 6.dp),
        color = CCardInnerBackground,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CStrokeColor)
    ) {
        when (activeTool) {
            EditorTool.Trim -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Drag left/right handles on selected clip",
                        color = CTextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        "Start ${formatClock(selectedClip.sourceStartMs)}  •  End ${formatClock(selectedClip.sourceEndMs)}",
                        color = CTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            EditorTool.Crop -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Crop applies to selected clip",
                        color = CTextSecondary,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cropRatioOptions.forEach { ratio ->
                            OutlinedButton(
                                onClick = { onAction(EditorAction.SetCrop(cropForRatio(ratio), ratio)) },
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text(if (ratio == currentCropRatio) "[$ratio]" else ratio, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            EditorTool.Rotate -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAction(EditorAction.RotateLeft) },
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) { Text("Rotate L", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = { onAction(EditorAction.RotateRight) },
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) { Text("Rotate R", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = { onAction(EditorAction.FlipHorizontal) },
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) { Text("Flip H", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = { onAction(EditorAction.FlipVertical) },
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) { Text("Flip V", fontSize = 12.sp) }
                }
            }
            EditorTool.Filter -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onAction(EditorAction.StartComparingOriginal)
                                        tryAwaitRelease()
                                        onAction(EditorAction.StopComparingOriginal)
                                    }
                                )
                            }
                    ) {
                        Text(if (isComparingOriginal) "Showing Original" else "Hold to Compare", fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        legacyFilterOptions.forEach { name ->
                            OutlinedButton(onClick = { onAction(EditorAction.ApplyNamedFilter(name)) }) { Text(name, fontSize = 12.sp) }
                        }
                    }
                }
            }
            EditorTool.Volume -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Vol", color = CTextSecondary, fontSize = 12.sp)
                    Slider(
                        value = selectedClip.audio.volume.coerceIn(0f, 1f),
                        onValueChange = { onAction(EditorAction.SetVolume(it)) },
                        modifier = Modifier.weight(1f)
                    )
                    Text("Mute", color = CTextSecondary, fontSize = 12.sp)
                    Switch(checked = selectedClip.audio.isMuted, onCheckedChange = { onAction(EditorAction.SetMuted(it)) })
                }
            }
            EditorTool.Speed -> {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    speedOptions.forEach { speed ->
                        OutlinedButton(onClick = {
                            onAction(EditorAction.SetSpeed(speed))
                        }) {
                            Text(if (speed == currentSpeed) "[${speed}x]" else "${speed}x", fontSize = 12.sp)
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}

private fun cropForRatio(ratio: String): Crop {
    return when (ratio) {
        "1:1" -> verticalCropForTargetRatio(1f)
        "4:5" -> verticalCropForTargetRatio(4f / 5f)
        "9:16" -> Crop()
        "16:9" -> verticalCropForTargetRatio(16f / 9f)
        else -> Crop()
    }
}

private fun aspectRatioForLabel(ratio: String): Float {
    return when (ratio) {
        "1:1" -> 1f
        "4:5" -> 4f / 5f
        "9:16" -> 9f / 16f
        "16:9" -> 16f / 9f
        else -> 9f / 16f
    }
}

private fun verticalCropForTargetRatio(targetRatio: Float): Crop {
    val sourceRatio = 9f / 16f
    if (targetRatio <= sourceRatio) return Crop()
    val visibleHeight = (sourceRatio / targetRatio).coerceIn(0f, 1f)
    val top = (1f - visibleHeight) / 2f
    return Crop(top = top, bottom = 1f - top)
}

@Composable
fun TimelineRuler(durationMs: Long, width: androidx.compose.ui.unit.Dp) {
    val labels = (durationMs / 10_000L).toInt().coerceAtLeast(1)
    Row(modifier = Modifier.width(width).height(24.dp), verticalAlignment = Alignment.Bottom) {
        repeat(labels + 1) { i ->
            val tickMs = i * 10_000L
            Column(modifier = Modifier.weight(1f)) {
                Text(formatClock(tickMs), color = CTextSecondary, fontSize = 10.sp)
                Box(modifier = Modifier.width(1.dp).height(if (i % 2 == 0) 8.dp else 5.dp).background(CTextSecondary.copy(alpha = 0.8f)))
            }
        }
    }
}

@Composable
fun TimelineTrack(
    timeline: Timeline,
    selectedClipId: String?,
    activeTool: EditorTool?,
    pixelsPerMs: Float,
    width: androidx.compose.ui.unit.Dp,
    scrollOffsetPx: Int,
    onSelectClip: (String?) -> Unit,
    onTrimStartChanged: (String, Long) -> Unit,
    onTrimEndChanged: (String, Long) -> Unit,
    onClipMoved: (String, String, Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onAddMoreMedia: () -> Unit
) {
    Box(modifier = Modifier.width(width).height(84.dp).clip(RoundedCornerShape(10.dp)).background(CCardInnerBackground).clickable { onSelectClip(null) }) {
        timeline.tracks.firstOrNull()?.let { track ->
            TrackRow(
                track = track,
                selectedClipId = selectedClipId,
                pixelsPerMs = pixelsPerMs,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, end = 72.dp).height(64.dp),
                onClipSelected = { onSelectClip(it) },
                onClipDragged = { clip, deltaMs ->
                    onClipMoved(track.id, clip.id, (clip.timelineStartMs + deltaMs).coerceAtLeast(0L))
                },
                onClipTrimStartDragged = { clip, deltaMs ->
                    val maxStart = (clip.sourceEndMs - 100L).coerceAtLeast(0L)
                    onTrimStartChanged(clip.id, (clip.sourceStartMs + deltaMs).coerceIn(0L, maxStart))
                },
                onClipTrimEndDragged = { clip, deltaMs ->
                    val minEnd = (clip.sourceStartMs + 100L).coerceAtMost(clip.sourceDurationMs)
                    onTrimEndChanged(clip.id, (clip.sourceEndMs + deltaMs).coerceIn(minEnd, clip.sourceDurationMs))
                }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(pixelsPerMs, scrollOffsetPx) {
                    detectTapGestures { offset ->
                        val targetX = (offset.x + scrollOffsetPx).coerceAtLeast(0f)
                        onSeekTo((targetX / pixelsPerMs).toLong())
                    }
                }
        )
        ImportMediaButton(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp), onImportVideo = onAddMoreMedia, onImportImage = onAddMoreMedia, onAddMoreMedia = onAddMoreMedia)
    }
}

@Composable
fun TimelineClipItem(
    clip: TimelineClip,
    selected: Boolean,
    showTrim: Boolean,
    pixelsPerMs: Float,
    onClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onTrimStartChanged: (String, Long) -> Unit,
    onTrimEndChanged: (String, Long) -> Unit
) {
    val start = (clip.timelineStartMs * pixelsPerMs).roundToInt().dp
    val width = (clip.visibleDurationMs * pixelsPerMs).roundToInt().coerceAtLeast(120).dp
    Box(
        modifier = Modifier
            .padding(start = start, top = 10.dp)
            .width(width)
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF253047))
            .border(if (selected) 2.dp else 1.dp, if (selected) CPrimaryBlue else CStrokeColor, RoundedCornerShape(10.dp))
            .clickable {
                onClick()
                onSeekTo(clip.timelineStartMs)
            }
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(6) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Brush.verticalGradient(listOf(Color(0xFF49566D), Color(0xFF344054)))))
            }
        }
        Text(clip.metadata.label ?: clip.assetId, color = CTextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 4.dp, end = 56.dp))
        Text(formatClock(clip.visibleDurationMs), color = CTextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 4.dp))
        if (selected && showTrim) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(CPrimaryBlue.copy(alpha = 0.72f))
                    .pointerInput(clip.id, pixelsPerMs) {
                        var draggedMs = 0f
                        detectDragGestures(
                            onDragStart = { draggedMs = 0f },
                            onDrag = { _, dragAmount ->
                                draggedMs += (dragAmount.x / pixelsPerMs) * 0.8f
                                val maxStart = (clip.sourceEndMs - 100L).coerceAtLeast(0L)
                                onTrimStartChanged(clip.id, (clip.sourceStartMs + draggedMs.toLong()).coerceIn(0L, maxStart))
                            }
                        )
                    }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(CPrimaryBlue.copy(alpha = 0.72f))
                    .pointerInput(clip.id, pixelsPerMs) {
                        var draggedMs = 0f
                        detectDragGestures(
                            onDragStart = { draggedMs = 0f },
                            onDrag = { _, dragAmount ->
                                draggedMs += (dragAmount.x / pixelsPerMs) * 0.8f
                                val minEnd = (clip.sourceStartMs + 100L).coerceAtMost(clip.sourceDurationMs)
                                onTrimEndChanged(clip.id, (clip.sourceEndMs + draggedMs.toLong()).coerceIn(minEnd, clip.sourceDurationMs))
                            }
                        )
                    }
            )
        }
    }
}

@Composable
fun TimelineZoomControls(zoomScale: Float, onZoomOutTimeline: () -> Unit, onZoomInTimeline: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onZoomOutTimeline, modifier = Modifier.size(32.dp)) { Text("-", color = CTextPrimary, fontSize = 14.sp) }
        Text(String.format(java.util.Locale.US, "%.1fx", zoomScale), color = CTextSecondary, fontSize = 10.sp)
        IconButton(onClick = onZoomInTimeline, modifier = Modifier.size(32.dp)) { Text("+", color = CTextPrimary, fontSize = 14.sp) }
    }
}

@Composable
fun BottomFunctionBar(selectedTool: EditorTool?, onToolSelected: (EditorTool) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth().height(74.dp), color = CTopBarBackground) {
        LazyRow(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(bottomEditorTools, key = { it.name }) { tool ->
                BottomFunctionItem(
                    tool = tool,
                    selected = selectedTool == tool,
                    enabled = true,
                    onClick = { onToolSelected(tool) }
                )
            }
        }
    }
}

@Composable
fun BottomFunctionItem(tool: EditorTool, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val tint = when {
        !enabled -> CTextDisabled
        selected -> CPrimaryBlue
        else -> CTextSecondary
    }
    val icon: ImageVector = when (tool) {
        EditorTool.Trim -> Icons.Filled.ContentCut
        EditorTool.Crop -> Icons.Filled.Crop
        EditorTool.Rotate -> Icons.Filled.Rotate90DegreesCcw
        EditorTool.Filter -> Icons.Filled.FilterAlt
        EditorTool.Speed -> Icons.Filled.Speed
        EditorTool.Text -> Icons.Filled.TextFields
        EditorTool.Sticker -> Icons.Filled.EmojiEmotions
        EditorTool.Music -> Icons.Filled.MusicNote
        EditorTool.Volume -> Icons.AutoMirrored.Filled.VolumeUp
        EditorTool.Background -> Icons.Filled.Wallpaper
        EditorTool.Canvas -> Icons.Filled.CropSquare
        EditorTool.Effects -> Icons.Filled.AutoAwesome
        else -> Icons.Filled.AutoAwesome
    }
    Column(modifier = Modifier.widthIn(min = 52.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(if (selected) CPrimaryBlue.copy(alpha = 0.2f) else Color.Transparent), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = tool.label, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(tool.label, color = tint, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
fun ImportMediaButton(modifier: Modifier = Modifier, onImportVideo: () -> Unit, onImportImage: () -> Unit, onAddMoreMedia: () -> Unit) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = onAddMoreMedia, modifier = Modifier.height(36.dp), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CStrokeColor)) {
            Icon(Icons.Filled.Add, contentDescription = "Add", tint = CTextPrimary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add", color = CTextPrimary, fontSize = 12.sp)
        }
        Icon(Icons.Filled.VideoLibrary, contentDescription = "Import video", tint = CTextSecondary, modifier = Modifier.size(18.dp).clickable(onClick = onImportVideo))
        Icon(Icons.Filled.Wallpaper, contentDescription = "Import image", tint = CTextSecondary, modifier = Modifier.size(18.dp).clickable(onClick = onImportImage))
    }
}

private fun buildGpuPreviewBitmap(project: VideoProject?, clip: TimelineClip?, timelinePositionMs: Long): Bitmap? {
    if (project == null || clip == null) return null
    val asset = project.assets.firstOrNull { it.id == clip.assetId } ?: return null
    val raw = when (asset.type) {
        AssetType.Image -> BitmapFactory.decodeFile(asset.cachedPath)
        AssetType.Video -> extractVideoFrame(asset.cachedPath, clip, timelinePositionMs)
        else -> null
    } ?: return null
    return raw.applyCropRotateFlip(clip)
}

private fun extractVideoFrame(path: String, clip: TimelineClip, timelinePositionMs: Long): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return runCatching {
        val sourceOffset = (timelinePositionMs - clip.timelineStartMs).coerceIn(0L, clip.visibleDurationMs)
        val frameTimeUs = (clip.sourceStartMs + sourceOffset).coerceAtLeast(0L) * 1000L
        retriever.setDataSource(path)
        retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    }.also {
        runCatching { retriever.release() }
    }.getOrNull()
}

private fun Bitmap.applyCropRotateFlip(clip: TimelineClip): Bitmap {
    val crop = clip.transform.crop
    val left = (width * crop.left).roundToInt().coerceIn(0, width - 1)
    val top = (height * crop.top).roundToInt().coerceIn(0, height - 1)
    val right = (width * crop.right).roundToInt().coerceIn(left + 1, width)
    val bottom = (height * crop.bottom).roundToInt().coerceIn(top + 1, height)
    val cropped = Bitmap.createBitmap(this, left, top, right - left, bottom - top)
    val matrix = Matrix().apply {
        if (clip.transform.flipHorizontal || clip.transform.flipVertical) {
            preScale(if (clip.transform.flipHorizontal) -1f else 1f, if (clip.transform.flipVertical) -1f else 1f)
        }
        if (clip.transform.rotationDegrees != 0f) {
            postRotate(clip.transform.rotationDegrees)
        }
    }
    return if (!matrix.isIdentity) Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true) else cropped
}

private fun gpuFilterForClip(clip: TimelineClip): GPUImageFilter {
    val filters = mutableListOf<GPUImageFilter>()
    when (clip.effect.parameters[ClipEffectParameterFilterName]) {
        "Sepia" -> filters += GPUImageSepiaToneFilter()
        "Mono", "Monochrome", "Luminance" -> filters += GPUImageGrayscaleFilter()
        "Gaussian Blur", "Box Blur", "Bilateral Blur" -> filters += GPUImageGaussianBlurFilter(2f)
    }
    if (clip.transform.brightness != 0f) filters += GPUImageBrightnessFilter(clip.transform.brightness.coerceIn(-1f, 1f))
    if (clip.transform.contrast != 1f) filters += GPUImageContrastFilter(clip.transform.contrast.coerceIn(0f, 2f))
    if (clip.transform.saturation != 1f) filters += GPUImageSaturationFilter(clip.transform.saturation.coerceIn(0f, 2f))
    return when (filters.size) {
        0 -> GPUImageFilter()
        1 -> filters.first()
        else -> GPUImageFilterGroup(filters)
    }
}

private fun formatClock(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) / 1000L)
    val minutes = totalSec / 60L
    val seconds = totalSec % 60L
    return "%02d:%02d".format(minutes, seconds)
}
