package com.nantcompany.clipy.tools.crop

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.nantcompany.clipy.edit.tools.crop.CropRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File
import kotlinx.coroutines.delay

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun CropVideoScreen(
    inputPath: String?,
    onNavigate: (AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    viewModel: CropVideoViewModel = viewModel()
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
            playWhenReady = false
            prepare()
        }
    }

    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }

    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect
        while (true) {
            val format = player.videoFormat
            if (format != null) {
                videoWidth = format.width
                videoHeight = format.height
                if (videoWidth > 0 && videoHeight > 0) break
            }
            delay(100)
        }
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

    ClipyScaffold(
        title = "Crop Video",
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
                    Text("Crop Preview", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (player != null) {
                            ClipyVideoPlayer(
                                player = player,
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Simple visual overlay for crop area (simulated)
                            Box(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxSize()
                                    .border(2.dp, ClipyDesignTokens.primaryAccent, RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                            )
                        } else {
                            Text("No video selected", color = ClipyDesignTokens.secondaryText)
                        }
                    }
                    
                    if (videoWidth > 0) {
                        Text(
                            "Resolution: ${videoWidth}x${videoHeight} • Format: ${uiState.aspectRatio}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClipyDesignTokens.primaryAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ClipySectionTitle(text = "Aspect Ratio")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1:1", "16:9", "9:16", "4:3").forEach { ratio ->
                        val isSelected = uiState.aspectRatio == ratio
                        if (isSelected) {
                            ClipyPrimaryButton(
                                modifier = Modifier.weight(1f),
                                label = ratio, 
                                onClick = { viewModel.setAspectRatio(ratio) }
                            )
                        } else {
                            ClipySecondaryButton(
                                modifier = Modifier.weight(1f),
                                label = ratio, 
                                onClick = { viewModel.setAspectRatio(ratio) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            ClipyPrimaryButton(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                label = "Export Cropped Video",
                enabled = !inputPath.isNullOrBlank() && videoWidth > 0,
                onClick = {
                    val input = inputPath ?: return@ClipyPrimaryButton
                    val targetRatio = when (uiState.aspectRatio) {
                        "1:1" -> 1.0f
                        "16:9" -> 16f/9f
                        "9:16" -> 9f/16f
                        "4:3" -> 4f/3f
                        else -> videoWidth.toFloat() / videoHeight.toFloat()
                    }
                    
                    var outW = videoWidth
                    var outH = (videoWidth / targetRatio).toInt()
                    if (outH > videoHeight) {
                        outH = videoHeight
                        outW = (videoHeight * targetRatio).toInt()
                    }
                    
                    outW = (outW / 2) * 2
                    outH = (outH / 2) * 2
                    val x = (videoWidth - outW) / 2
                    val y = (videoHeight - outH) / 2
                    
                    val request = CropRequest(
                        inputPath = input,
                        outputPath = MediaFileUtils.createOutputPath(context, "crop", "mp4"),
                        x = x,
                        y = y,
                        width = outW,
                        height = outH
                    )
                    onSubmitRequest(ProcessingRequest.Crop(request))
                }
            )
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
