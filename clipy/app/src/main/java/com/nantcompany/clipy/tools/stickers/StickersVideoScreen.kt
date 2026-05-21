package com.nantcompany.clipy.tools.stickers

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import com.nantcompany.clipy.edit.tools.stickers.StickersRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File
import kotlin.math.roundToInt

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun StickersVideoScreen(
    inputPath: String?,
    onNavigate: (AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    viewModel: StickersVideoViewModel = viewModel()
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

    var containerSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    val stickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val path = MediaFileUtils.importUriToLocalPath(context, uri, "imports/stickers", "png")
            viewModel.setStickerPath(path)
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
        title = "Stickers",
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
                    Text("Live Editor", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .onSizeChanged { containerSize = it },
                        contentAlignment = Alignment.Center
                    ) {
                        if (player != null) {
                            ClipyVideoPlayer(player = player, modifier = Modifier.fillMaxSize())
                            
                            // Interactive Draggable Sticker Overlay
                            uiState.stickerPath?.let { path ->
                                val bitmap = remember(path) { BitmapFactory.decodeFile(path) }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Sticker",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .offset { 
                                                IntOffset(
                                                    (uiState.x.toFloat() / 100f * containerSize.width).roundToInt(),
                                                    (uiState.y.toFloat() / 100f * containerSize.height).roundToInt()
                                                )
                                            }
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val newX = (uiState.x + (dragAmount.x / containerSize.width * 100f)).coerceIn(0f, 85f)
                                                    val newY = (uiState.y + (dragAmount.y / containerSize.height * 100f)).coerceIn(0f, 85f)
                                                    viewModel.setPosition(newX, newY)
                                                }
                                            },
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        } else {
                            Text("No video selected", color = ClipyDesignTokens.secondaryText)
                        }
                    }
                    Text("Drag the sticker to reposition it.", style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.primaryAccent)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ClipySectionTitle(text = "Sticker Asset")
                
                if (uiState.stickerPath == null) {
                    ClipySecondaryButton(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        label = "⊕ Select Image Sticker", 
                        onClick = {
                            stickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(), 
                            horizontalArrangement = Arrangement.SpaceBetween, 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bitmap = remember(uiState.stickerPath) { BitmapFactory.decodeFile(uiState.stickerPath) }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Text("Sticker Loaded", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                            ClipySecondaryButton(
                                label = "Replace", 
                                onClick = {
                                    stickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            ClipyPrimaryButton(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                label = "Export Video with Sticker",
                enabled = !inputPath.isNullOrBlank() && uiState.stickerPath != null,
                onClick = {
                    val input = inputPath ?: return@ClipyPrimaryButton
                    val sticker = uiState.stickerPath ?: return@ClipyPrimaryButton
                    val request = StickersRequest(
                        inputPath = input,
                        outputPath = MediaFileUtils.createOutputPath(context, "sticker", "mp4"),
                        stickerPath = sticker,
                        x = uiState.x,
                        y = uiState.y,
                        startTimeMs = 0,
                        endTimeMs = 999999
                    )
                    onSubmitRequest(ProcessingRequest.Stickers(request))
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
