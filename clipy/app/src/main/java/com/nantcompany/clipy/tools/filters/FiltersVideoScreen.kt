package com.nantcompany.clipy.tools.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.design.ClipyVideoPlayer
import com.nantcompany.clipy.edit.tools.filters.FiltersRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import com.nantcompany.clipy.filters.gpu.ClipyFilterType
import com.nantcompany.clipy.filters.gpu.ClipyGpuFilterManager
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val FilterPanelColor = Color(0xFF111724)
private val FilterPanelElevatedColor = Color(0xFF171E2C)
private val FilterPanelBorderColor = Color(0x1FFFFFFF)
private val FilterTrackColor = Color(0xFF293241)

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun FiltersVideoScreen(
    inputPath: String?,
    onNavigate: (AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    viewModel: FiltersVideoViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(inputPath) {
        viewModel.setInputPath(inputPath)
    }

    val videoFile = remember(uiState.inputPath) { uiState.inputPath?.let(::File) }
    val videoUri = remember(uiState.inputPath) { videoFile?.let { Uri.fromFile(it) } }
    
    val player = remember(videoUri, context) {
        if (videoUri == null) null else ExoPlayer.Builder(
            context,
            DefaultRenderersFactory(context).apply {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            }
        ).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            prepare()
        }
    }

    val effectKey = "${uiState.selectedFilter.name}:${uiState.filterIntensity}:${uiState.brightness}:${uiState.contrast}:${uiState.saturation}"
    val previewColorMatrix = remember(effectKey) {
        ClipyGpuFilterManager.createPreviewColorMatrix(
            brightness = uiState.brightness,
            contrast = uiState.contrast,
            saturation = uiState.saturation,
            filterType = uiState.selectedFilter,
            filterIntensity = uiState.filterIntensity
        )
    }
    val thumbnail by produceState<Bitmap?>(initialValue = null, key1 = uiState.inputPath) {
        value = withContext(Dispatchers.IO) {
            uiState.inputPath?.let(::loadVideoThumbnail)
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player?.pause()
            if (event == Lifecycle.Event.ON_RESUME) player?.play()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player?.release()
        }
    }

    ClipyScaffold(
        title = "Video Filters",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .padding(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterPreview(
                    player = player,
                    previewColorMatrix = previewColorMatrix
                )

                StyleStrip(
                    thumbnail = thumbnail,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::setFilter
                )

                AdjustmentPanel(
                    filterIntensity = uiState.filterIntensity,
                    brightness = uiState.brightness,
                    contrast = uiState.contrast,
                    saturation = uiState.saturation,
                    onFilterIntensityChange = viewModel::setFilterIntensity,
                    onBrightnessChange = viewModel::setBrightness,
                    onContrastChange = viewModel::setContrast,
                    onSaturationChange = viewModel::setSaturation,
                    onReset = viewModel::reset
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            StickyExportBar(
                selectedFilter = uiState.selectedFilter,
                filterIntensity = uiState.filterIntensity,
                enabled = !uiState.inputPath.isNullOrBlank() || !inputPath.isNullOrBlank(),
                modifier = Modifier.align(Alignment.BottomCenter),
                onExport = {
                    val latestState = viewModel.uiState.value
                    val input = latestState.inputPath ?: inputPath ?: return@StickyExportBar
                    val request = FiltersRequest(
                        inputPath = input,
                        outputPath = MediaFileUtils.createOutputPath(context, "filters", "mp4"),
                        brightness = latestState.brightness,
                        contrast = latestState.contrast,
                        saturation = latestState.saturation,
                        filterIntensity = latestState.filterIntensity,
                        filterName = latestState.selectedFilter.name
                    )
                    onSubmitRequest(ProcessingRequest.Filters(request))
                }
            )
        }
    }
}

@Composable
private fun FilterPreview(
    player: ExoPlayer?,
    previewColorMatrix: ColorMatrix?
) {
    val interactionSource = remember { MutableInteractionSource() }
    val showOriginal by interactionSource.collectIsPressedAsState()
    val displayedMatrix = if (showOriginal) null else previewColorMatrix

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        if (player != null) {
            ClipyVideoPlayer(
                player = player,
                previewColorMatrix = displayedMatrix,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("No video selected", color = ClipyDesignTokens.secondaryText)
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(999.dp),
            color = if (showOriginal) ClipyDesignTokens.primaryAccent else Color.Black.copy(alpha = 0.58f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
        ) {
            Text(
                text = if (showOriginal) "Original" else "Hold Original",
                color = if (showOriginal) Color.Black else Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun StickyExportBar(
    selectedFilter: ClipyFilterType,
    filterIntensity: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onExport: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xF2030712),
        border = BorderStroke(1.dp, FilterPanelBorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = selectedFilter.displayName,
                    color = ClipyDesignTokens.textPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Intensity ${(filterIntensity * 100f).roundToInt()}%",
                    color = ClipyDesignTokens.secondaryText,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            ClipyPrimaryButton(
                modifier = Modifier
                    .width(132.dp)
                    .height(52.dp),
                label = "Export",
                enabled = enabled,
                onClick = onExport
            )
        }
    }
}

@Composable
private fun StyleStrip(
    thumbnail: Bitmap?,
    selectedFilter: ClipyFilterType,
    onFilterSelected: (ClipyFilterType) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = FilterPanelColor,
        border = BorderStroke(1.dp, FilterPanelBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Styles",
                    color = ClipyDesignTokens.textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = selectedFilter.displayName,
                    color = ClipyDesignTokens.secondaryText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ClipyFilterType.entries, key = { it.name }) { filter ->
                    FilterStyleThumb(
                        thumbnail = thumbnail,
                        filter = filter,
                        selected = selectedFilter == filter,
                        onClick = { onFilterSelected(filter) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterStyleThumb(
    thumbnail: Bitmap?,
    filter: ClipyFilterType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val previewBitmap = remember(thumbnail, filter) {
        thumbnail?.let {
            applyPreviewMatrix(
                source = it,
                matrix = ClipyGpuFilterManager.createPreviewColorMatrix(
                    brightness = 0f,
                    contrast = 0f,
                    saturation = 1f,
                    filterType = filter
                )
            )
        }
    }
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) ClipyDesignTokens.primaryAccent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 68.dp, height = 58.dp)
                .clip(shape)
                .background(FilterPanelElevatedColor)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) ClipyDesignTokens.primaryAccent else FilterPanelBorderColor,
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = filter.displayName.take(2),
                    color = if (selected) ClipyDesignTokens.primaryAccent else ClipyDesignTokens.secondaryText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = filter.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) ClipyDesignTokens.primaryAccent else ClipyDesignTokens.secondaryText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AdjustmentPanel(
    filterIntensity: Float,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    onFilterIntensityChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = FilterPanelColor,
        border = BorderStroke(1.dp, FilterPanelBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Adjust",
                    color = ClipyDesignTokens.textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Reset",
                    color = ClipyDesignTokens.primaryAccent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(ClipyDesignTokens.primaryAccent.copy(alpha = 0.12f))
                        .clickable(onClick = onReset)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
            FilterSlider(
                title = "Intensity",
                value = filterIntensity,
                onValueChange = onFilterIntensityChange,
                valueRange = 0.0f..1.0f
            )
            FilterSlider(
                title = "Brightness",
                value = brightness,
                onValueChange = onBrightnessChange,
                valueRange = -0.5f..0.5f
            )
            FilterSlider(
                title = "Contrast",
                value = contrast,
                onValueChange = onContrastChange,
                valueRange = -0.5f..0.5f
            )
            FilterSlider(
                title = "Saturation",
                value = saturation,
                onValueChange = onSaturationChange,
                valueRange = 0.0f..2.0f
            )
        }
    }
}

@Composable
private fun FilterSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = ClipyDesignTokens.secondaryText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(82.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        CompactSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = FilterPanelElevatedColor,
            border = BorderStroke(1.dp, FilterPanelBorderColor)
        ) {
            Text(
                "%.2f".format(value),
                style = MaterialTheme.typography.labelSmall,
                color = ClipyDesignTokens.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .width(46.dp)
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompactSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val range = valueRange.endInclusive - valueRange.start
    val fraction = if (range == 0f) {
        0f
    } else {
        ((value - valueRange.start) / range).coerceIn(0f, 1f)
    }
    val thumbSize = 14.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }

    fun updateValueFromPosition(x: Float) {
        if (trackWidthPx <= 0 || range == 0f) return
        val nextFraction = (x / trackWidthPx).coerceIn(0f, 1f)
        onValueChange(valueRange.start + range * nextFraction)
    }

    Box(
        modifier = modifier
            .height(28.dp)
            .onSizeChanged { trackWidthPx = it.width }
            .pointerInput(valueRange, trackWidthPx) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    updateValueFromPosition(down.position.x)
                    drag(down.id) { change ->
                        updateValueFromPosition(change.position.x)
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(FilterTrackColor)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(ClipyDesignTokens.secondaryAccent, ClipyDesignTokens.primaryAccent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .padding(start = 0.dp)
                .offset {
                    val maxOffset = (trackWidthPx - thumbSizePx).coerceAtLeast(0f)
                    IntOffset((maxOffset * fraction).roundToInt(), 0)
                }
                .size(thumbSize)
                .clip(CircleShape)
                .background(ClipyDesignTokens.primaryAccent)
                .border(2.dp, Color.White.copy(alpha = 0.75f), CircleShape)
        )
    }
}

private fun loadVideoThumbnail(path: String): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return runCatching {
        retriever.setDataSource(path)
        val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.frameAtTime
            ?: return@runCatching null
        Bitmap.createScaledBitmap(frame, 160, 160, true)
    }.getOrNull().also {
        runCatching { retriever.release() }
    }
}

private fun applyPreviewMatrix(source: Bitmap, matrix: ColorMatrix?): Bitmap {
    if (matrix == null) return source
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(matrix)
    }
    Canvas(output).drawBitmap(source, 0f, 0f, paint)
    return output
}
