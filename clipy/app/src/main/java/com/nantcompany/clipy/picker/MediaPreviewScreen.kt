package com.nantcompany.clipy.picker

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nantcompany.clipy.design.ClipyLoadingState
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun MediaPreviewScreen(
    singleVideoPath: String?,
    multipleVideoPaths: List<String>,
    imagePaths: List<String>,
    targetRoute: AppRoute?,
    onNavigate: (AppRoute) -> Unit,
    viewModel: MediaPreviewViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(singleVideoPath) {
        if (!singleVideoPath.isNullOrBlank()) {
            viewModel.loadVideoPreview(singleVideoPath)
        }
    }

    LaunchedEffect(imagePaths) {
        if (imagePaths.isNotEmpty()) {
            viewModel.loadImagesPreview(imagePaths)
        }
    }

    ClipyScaffold(
        title = "Media Preview",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!singleVideoPath.isNullOrBlank()) {
                VideoPreviewSection(
                    path = singleVideoPath,
                    videoInfo = uiState.videoInfo,
                    targetRoute = targetRoute,
                    onNavigate = onNavigate
                )
            } else if (multipleVideoPaths.isNotEmpty()) {
                MultiVideoPreviewSection(
                    paths = multipleVideoPaths,
                    targetRoute = targetRoute,
                    onNavigate = onNavigate
                )
            } else if (imagePaths.isNotEmpty()) {
                ImagePreviewSection(
                    previews = uiState.imagePreviews,
                    isLoading = uiState.isLoading,
                    targetRoute = targetRoute,
                    onNavigate = onNavigate
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No media selected", style = MaterialTheme.typography.bodyLarge, color = ClipyDesignTokens.secondaryText)
                }
            }
        }
    }
}

@OptIn(markerClass = [UnstableApi::class])
@Composable
private fun VideoPreviewSection(
    path: String,
    videoInfo: MediaItemModel?,
    targetRoute: AppRoute?,
    onNavigate: (AppRoute) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val file = remember(path) { File(path) }
    val uri = remember(path) { Uri.fromFile(file) }
    val player = remember(path, context) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            playWhenReady = false
            prepare()
        }
    }

    var currentMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(player) {
        while (true) {
            currentMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it > 0L } ?: 0L
            delay(250)
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Selected video", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                val dur = if (durationMs > 0L) formatTime(durationMs) else videoInfo?.durationMs?.let { formatTime(it) } ?: "..."
                Text("Duration: $dur", style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.secondaryText)
                Text("Size: ${formatFileSize(file.length())}", style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.secondaryText)
            }
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = true
                    this.player = player
                }
            },
            update = { view -> view.player = player }
        )

        ClipyPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            label = "Continue",
            enabled = targetRoute != null,
            onClick = { targetRoute?.let(onNavigate) }
        )
    }
}

@Composable
private fun MultiVideoPreviewSection(
    paths: List<String>,
    targetRoute: AppRoute?,
    onNavigate: (AppRoute) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Selected clips: ${paths.size}", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(paths) { path ->
                val file = File(path)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.height(40.dp).background(ClipyDesignTokens.primaryAccent.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                            Text("MP4", style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.primaryAccent)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Text(formatFileSize(file.length()), style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.secondaryText)
                        }
                    }
                }
            }
        }
        ClipyPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            label = "Continue",
            enabled = targetRoute != null,
            onClick = { targetRoute?.let(onNavigate) }
        )
    }
}

@Composable
private fun ImagePreviewSection(
    previews: List<ImagePreviewData>,
    isLoading: Boolean,
    targetRoute: AppRoute?,
    onNavigate: (AppRoute) -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Selected images: ${previews.size}", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        
        if (isLoading && previews.isEmpty()) {
            ClipyLoadingState(message = "Loading images...")
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(previews) { info ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(info.path))
                            .crossfade(true)
                            .build(),
                        contentDescription = info.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ClipyDesignTokens.cardSurface),
                        contentScale = ContentScale.Crop
                    )
                    Text(info.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.secondaryText)
                }
            }
        }

        ClipyPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            label = "Continue",
            enabled = targetRoute != null && !isLoading,
            onClick = { targetRoute?.let(onNavigate) }
        )
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    val value = bytes.toDouble()
    return when {
        value >= gb -> "%.2f GB".format(value / gb)
        value >= mb -> "%.2f MB".format(value / mb)
        value >= kb -> "%.1f KB".format(value / kb)
        else -> "$bytes B"
    }
}
