package com.nantcompany.clipy.tools.rotate

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.nantcompany.clipy.edit.tools.rotate.RotateRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun RotateVideoScreen(
    inputPath: String?,
    onNavigate: (AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    viewModel: RotateVideoViewModel = viewModel()
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
        title = "Rotate Video",
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
                    Text("Live Preview", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    
                    if (player != null) {
                        ClipyVideoPlayer(
                            player = player,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .graphicsLayer {
                                    rotationZ = uiState.rotation.toFloat()
                                    scaleX = if (uiState.flipHorizontal) -1f else 1f
                                    scaleY = if (uiState.flipVertical) -1f else 1f
                                }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(260.dp).background(Color.Black, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No video selected", color = ClipyDesignTokens.secondaryText)
                        }
                    }
                    
                    Text(
                        "Transform: Rotation ${uiState.rotation}°" + 
                        (if (uiState.flipHorizontal) " • H-Flip" else "") +
                        (if (uiState.flipVertical) " • V-Flip" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = ClipyDesignTokens.primaryAccent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ClipySectionTitle(text = "Orientation")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClipySecondaryButton(
                        modifier = Modifier.weight(1f),
                        label = "Rotate 90°", 
                        onClick = { viewModel.rotateClockwise() }
                    )
                    ClipySecondaryButton(
                        modifier = Modifier.weight(1f),
                        label = "Rotate -90°", 
                        onClick = { viewModel.rotateCounterClockwise() }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ClipySectionTitle(text = "Mirroring")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClipySecondaryButton(
                        modifier = Modifier.weight(1f),
                        label = "Flip Horizontal", 
                        onClick = { viewModel.toggleFlipHorizontal() }
                    )
                    ClipySecondaryButton(
                        modifier = Modifier.weight(1f),
                        label = "Flip Vertical", 
                        onClick = { viewModel.toggleFlipVertical() }
                    )
                }
            }

            ClipySecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                label = "Reset Transform",
                onClick = { viewModel.reset() }
            )

            Spacer(modifier = Modifier.weight(1f))

            ClipyPrimaryButton(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                label = "Export Video",
                enabled = !inputPath.isNullOrBlank(),
                onClick = {
                    val input = inputPath ?: return@ClipyPrimaryButton
                    val request = RotateRequest(
                        inputPath = input,
                        outputPath = MediaFileUtils.createOutputPath(context, "rotate", "mp4"),
                        rotation = uiState.rotation,
                        flipHorizontal = uiState.flipHorizontal,
                        flipVertical = uiState.flipVertical
                    )
                    onSubmitRequest(ProcessingRequest.Rotate(request))
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
