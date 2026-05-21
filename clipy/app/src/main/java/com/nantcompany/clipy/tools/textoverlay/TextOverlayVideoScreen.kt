package com.nantcompany.clipy.tools.textoverlay

import android.net.Uri
import androidx.annotation.OptIn
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.nantcompany.clipy.design.ClipyTextField
import com.nantcompany.clipy.design.ClipyVideoPlayer
import com.nantcompany.clipy.edit.tools.textoverlay.TextOverlayRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File
import kotlin.math.roundToInt

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun TextOverlayVideoScreen(
    inputPath: String?,
    onNavigate: (AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    viewModel: TextOverlayVideoViewModel = viewModel()
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
        title = "Text Overlay",
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
                            
                            // Interactive Draggable Text Overlay
                            Box(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = uiState.text.ifBlank { "Sample Text" },
                                    color = when (uiState.fontColor.lowercase()) {
                                        "red" -> Color(0xFFFF5252)
                                        "green" -> Color(0xFF69F0AE)
                                        "blue" -> Color(0xFF448AFF)
                                        "yellow" -> Color(0xFFFFFF00)
                                        "purple" -> Color(0xFFE040FB)
                                        else -> Color.White
                                    },
                                    fontSize = uiState.fontSize.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .offset { 
                                            IntOffset(
                                                (uiState.x.toFloat() / 100f * containerSize.width).roundToInt(),
                                                (uiState.y.toFloat() / 100f * containerSize.height).roundToInt()
                                            )
                                        }
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val newX = (uiState.x + (dragAmount.x / containerSize.width * 100f)).coerceIn(0f, 90f)
                                                val newY = (uiState.y + (dragAmount.y / containerSize.height * 100f)).coerceIn(0f, 90f)
                                                viewModel.setPosition(newX, newY)
                                            }
                                        }
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(4.dp)
                                )
                            }
                        } else {
                            Text("No video selected", color = ClipyDesignTokens.secondaryText)
                        }
                    }
                    Text("Drag text to reposition it on the video.", style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.primaryAccent)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ClipySectionTitle(text = "Text Content")
                ClipyTextField(
                    value = uiState.text,
                    onValueChange = { viewModel.setText(it) },
                    placeholder = "Type something here..."
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClipySectionTitle(text = "Size")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClipySecondaryButton(
                            modifier = Modifier.size(44.dp),
                            label = "-", 
                            onClick = { viewModel.setFontSize(uiState.fontSize - 4) }
                        )
                        Text("${uiState.fontSize}", color = Color.White, fontWeight = FontWeight.Bold)
                        ClipySecondaryButton(
                            modifier = Modifier.size(44.dp),
                            label = "+", 
                            onClick = { viewModel.setFontSize(uiState.fontSize + 4) }
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClipySectionTitle(text = "Color")
                    val colors = listOf("White", "Red", "Green", "Blue", "Yellow", "Purple")
                    var expanded by remember { mutableStateOf(false) }
                    
                    Box {
                        ClipySecondaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            label = uiState.fontColor.replaceFirstChar { it.uppercase() }, 
                            onClick = { expanded = true }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            colors.forEach { color ->
                                DropdownMenuItem(
                                    text = { Text(color, color = Color.White) },
                                    onClick = {
                                        viewModel.setFontColor(color.lowercase())
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            ClipyPrimaryButton(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                label = "Export Video",
                enabled = !inputPath.isNullOrBlank() && uiState.text.isNotBlank(),
                onClick = {
                    val input = inputPath ?: return@ClipyPrimaryButton
                    val request = TextOverlayRequest(
                        inputPath = input,
                        outputPath = MediaFileUtils.createOutputPath(context, "text", "mp4"),
                        text = uiState.text,
                        x = uiState.x,
                        y = uiState.y,
                        fontSize = uiState.fontSize,
                        fontColor = uiState.fontColor
                    )
                    onSubmitRequest(ProcessingRequest.TextOverlay(request))
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
