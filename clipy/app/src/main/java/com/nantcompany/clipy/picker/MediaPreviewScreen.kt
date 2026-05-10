package com.nantcompany.clipy.picker

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nantcompany.clipy.navigation.AppRoute
import java.io.File
import kotlinx.coroutines.delay

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun MediaPreviewScreen(
    singleVideoPath: String?,
    multipleVideoPaths: List<String>,
    imagePaths: List<String>,
    targetRoute: AppRoute?,
    onNavigate: (AppRoute) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Media Preview", style = MaterialTheme.typography.headlineSmall)

        if (!singleVideoPath.isNullOrBlank()) {
            val file = remember(singleVideoPath) { File(singleVideoPath) }
            val exists = remember(singleVideoPath) { file.exists() }
            val uri = remember(singleVideoPath) { Uri.fromFile(file) }
            val player = remember(singleVideoPath, context, exists) {
                if (!exists) null else ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(uri))
                    playWhenReady = false
                    prepare()
                }
            }

            var currentMs by remember(singleVideoPath) { mutableLongStateOf(0L) }
            var durationMs by remember(singleVideoPath) { mutableLongStateOf(0L) }

            if (player != null) {
                LaunchedEffect(player) {
                    while (true) {
                        currentMs = player.currentPosition.coerceAtLeast(0L)
                        durationMs = player.duration.takeIf { it > 0L } ?: 0L
                        delay(250)
                    }
                }
            }

            DisposableEffect(player, lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_PAUSE) {
                        player?.pause()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    player?.release()
                }
            }

            val resolutionText = if (player != null && player.videoSize.width > 0 && player.videoSize.height > 0) {
                "${player.videoSize.width}x${player.videoSize.height}"
            } else {
                "Unknown resolution"
            }

            val isMetadataLoading = player != null && durationMs <= 0L && resolutionText == "Unknown resolution"

            Text("Selected video", style = MaterialTheme.typography.titleMedium)
            Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (isMetadataLoading) {
                Text("Loading metadata...", style = MaterialTheme.typography.bodySmall)
            }
            Text("Duration: ${if (durationMs > 0L) formatTime(durationMs) else "Unknown duration"}", style = MaterialTheme.typography.bodySmall)
            Text("Current: ${formatTime(currentMs)}", style = MaterialTheme.typography.bodySmall)
            Text("Resolution: $resolutionText", style = MaterialTheme.typography.bodySmall)
            Text("Size: ${if (exists) formatFileSize(file.length()) else "Unknown size"}", style = MaterialTheme.typography.bodySmall)

            if (player != null && player.playbackState == androidx.media3.common.Player.STATE_BUFFERING) {
                Text("Loading preview...", style = MaterialTheme.typography.bodySmall)
            }

            if (player != null) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            useController = true
                            this.player = player
                        }
                    },
                    update = { view -> view.player = player }
                )
            } else {
                Text("Could not load this video.", color = MaterialTheme.colorScheme.error)
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = targetRoute != null && exists,
                onClick = { targetRoute?.let(onNavigate) }
            ) {
                Text("Continue")
            }
            return@Column
        }

        if (multipleVideoPaths.isNotEmpty()) {
            Text("Selected clips: ${multipleVideoPaths.size}", style = MaterialTheme.typography.titleMedium)
            multipleVideoPaths.take(5).forEachIndexed { index, path ->
                val clipFile = remember(path) { File(path) }
                val clipExists = remember(path) { clipFile.exists() }
                Text("#${index + 1} ${clipFile.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Size: ${if (clipExists) formatFileSize(clipFile.length()) else "Unknown size"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = targetRoute != null,
                onClick = { targetRoute?.let(onNavigate) }
            ) { Text("Continue") }
            return@Column
        }

        if (imagePaths.isNotEmpty()) {
            Text("Selected images: ${imagePaths.size}", style = MaterialTheme.typography.titleMedium)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(imagePaths.take(12)) { path ->
                    val imageFile = remember(path) { File(path) }
                    val imageExists = remember(path) { imageFile.exists() }
                    val thumb = remember(path) {
                        runCatching {
                            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(path, options)
                            val sample = maxOf(1, minOf(options.outWidth / 512, options.outHeight / 512))
                            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
                            BitmapFactory.decodeFile(path, decodeOptions)
                        }.getOrNull()
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (thumb != null) {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = imageFile.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else if (imageExists) {
                            Text("Loading preview...", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(imageFile.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Size: ${if (imageExists) formatFileSize(imageFile.length()) else "Unknown size"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = targetRoute != null,
                onClick = { targetRoute?.let(onNavigate) }
            ) { Text("Continue") }
            return@Column
        }

        Text("No media selected", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "Unknown size"
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
