package com.nantcompany.clipy.tools.filters

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.design.ClipySecondaryButton
import com.nantcompany.clipy.design.ClipySectionTitle
import com.nantcompany.clipy.design.ClipyVideoPlayer
import com.nantcompany.clipy.edit.tools.filters.FiltersRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import com.nantcompany.clipy.filters.gpu.ClipyFilterType
import com.nantcompany.clipy.filters.gpu.ClipyGpuFilterManager
import java.io.File

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
        if (videoUri == null) null else ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            prepare()
        }
    }

    // Apply GPU Effects to Player in Real-time
    LaunchedEffect(uiState.brightness, uiState.contrast, uiState.saturation, uiState.selectedFilter) {
        player?.setVideoEffects(
            ClipyGpuFilterManager.createEffects(
                brightness = uiState.brightness,
                contrast = uiState.contrast,
                saturation = uiState.saturation,
                filterType = uiState.selectedFilter
            )
        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
                colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("GPU Live Preview", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    
                    if (player != null) {
                        ClipyVideoPlayer(
                            player = player,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(260.dp).background(Color.Black, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No video selected", color = ClipyDesignTokens.secondaryText)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ClipySectionTitle(text = "Style Filters")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ClipyFilterType.entries) { filter ->
                        val isSelected = uiState.selectedFilter == filter
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(70.dp).selectable(
                                selected = isSelected,
                                onClick = { viewModel.setFilter(filter) }
                            )
                        ) {
                            Surface(
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp),
                                color = if (isSelected) ClipyDesignTokens.primaryAccent else ClipyDesignTokens.cardSurface,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        filter.displayName.take(1), 
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                filter.displayName, 
                                style = MaterialTheme.typography.labelSmall, 
                                color = if (isSelected) ClipyDesignTokens.primaryAccent else Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ClipySectionTitle(text = "Fine Adjustments")
                
                FilterSlider(
                    title = "Brightness",
                    value = uiState.brightness,
                    onValueChange = { viewModel.setBrightness(it) },
                    valueRange = -0.5f..0.5f
                )

                FilterSlider(
                    title = "Contrast",
                    value = uiState.contrast,
                    onValueChange = { viewModel.setContrast(it) },
                    valueRange = -0.5f..0.5f
                )

                FilterSlider(
                    title = "Saturation",
                    value = uiState.saturation,
                    onValueChange = { viewModel.setSaturation(it) },
                    valueRange = 0.0f..2.0f
                )
            }

            ClipySecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                label = "Reset All",
                onClick = { viewModel.reset() }
            )

            ClipyPrimaryButton(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                label = "Export Enhanced Video",
                enabled = !inputPath.isNullOrBlank(),
                onClick = {
                    val input = inputPath ?: return@ClipyPrimaryButton
                    val request = FiltersRequest(
                        inputPath = input,
                        outputPath = MediaFileUtils.createOutputPath(context, "filters", "mp4"),
                        brightness = uiState.brightness,
                        contrast = uiState.contrast,
                        saturation = uiState.saturation,
                        filterName = uiState.selectedFilter.name
                    )
                    onSubmitRequest(ProcessingRequest.Filters(request))
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text("%.2f".format(value), style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.primaryAccent, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ClipyDesignTokens.primaryAccent,
                activeTrackColor = ClipyDesignTokens.primaryAccent,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
