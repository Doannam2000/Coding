package com.nantcompany.clipy.tools.cut

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.edit.tools.cut.CutRequest
import com.nantcompany.clipy.edit.tools.cut.CutValidator
import com.nantcompany.clipy.export.job.ProcessingRequest
import java.io.File
import kotlinx.coroutines.delay

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun CutVideoScreen(
    inputPath: String?,
    onSubmitRequest: (ProcessingRequest) -> Unit
) {
    val context = LocalContext.current
    var startMsText by remember { mutableStateOf("0") }
    var endMsText by remember { mutableStateOf("5000") }
    var validationError by remember { mutableStateOf<String?>(null) }
    val minDurationMs = 300L
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoFile = remember(inputPath) { inputPath?.let(::File) }
    val videoExists = remember(inputPath) { videoFile?.exists() == true }
    val videoUri = remember(inputPath, videoExists) { if (videoExists && videoFile != null) Uri.fromFile(videoFile) else null }
    val player = remember(videoUri, context) {
        if (videoUri == null) null else ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            playWhenReady = false
            prepare()
        }
    }
    var currentMs by remember(inputPath) { mutableLongStateOf(0L) }
    var durationMs by remember(inputPath) { mutableLongStateOf(0L) }
    var playRangeStartMs by remember(inputPath) { mutableLongStateOf(-1L) }
    var playRangeEndMs by remember(inputPath) { mutableLongStateOf(-1L) }

    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect
        while (true) {
            currentMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it > 0L } ?: 0L
            if (player.isPlaying && playRangeEndMs > playRangeStartMs && currentMs >= playRangeEndMs) {
                player.pause()
                player.seekTo(playRangeEndMs)
                playRangeStartMs = -1L
                playRangeEndMs = -1L
            }
            delay(100)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Cut Video", style = MaterialTheme.typography.headlineSmall)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Preview", style = MaterialTheme.typography.titleSmall)
                Text(videoFile?.name ?: "No video selected", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Current / Total: ${formatTime(currentMs)} / ${if (durationMs > 0L) formatTime(durationMs) else "Unknown"}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (player != null) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        factory = { viewContext ->
                            PlayerView(viewContext).apply {
                                useController = true
                                this.player = player
                            }
                        },
                        update = { view -> view.player = player }
                    )
                } else {
                    Text(
                        if (inputPath.isNullOrBlank()) "No video selected" else "Could not load preview.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (inputPath.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        OutlinedTextField(
            value = startMsText,
            onValueChange = { startMsText = it },
            label = { Text("Start (ms)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = endMsText,
            onValueChange = { endMsText = it },
            label = { Text("End (ms)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        val startMs = startMsText.toLongOrNull() ?: 0L
        val endMs = endMsText.toLongOrNull() ?: 0L
        val selectedDurationMs = (endMs - startMs).coerceAtLeast(0L)
        val isRangeOrdered = endMs > startMs
        val isDurationValid = selectedDurationMs >= minDurationMs
        val rangeError = when {
            !isRangeOrdered -> "End must be greater than start."
            !isDurationValid -> "Minimum duration is ${minDurationMs} ms."
            else -> null
        }
        Text("Selected duration: ${selectedDurationMs} ms", style = MaterialTheme.typography.bodySmall)
        rangeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val current = startMsText.toLongOrNull() ?: 0L
                startMsText = (current - 100).coerceAtLeast(0).toString()
            }) { Text("Start -0.1s") }
            Button(onClick = {
                val current = startMsText.toLongOrNull() ?: 0L
                startMsText = (current + 100).toString()
            }) { Text("Start +0.1s") }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val current = endMsText.toLongOrNull() ?: 0L
                endMsText = (current - 100).coerceAtLeast(0).toString()
            }) { Text("End -0.1s") }
            Button(onClick = {
                val current = endMsText.toLongOrNull() ?: 0L
                endMsText = (current + 100).toString()
            }) { Text("End +0.1s") }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                startMsText = "0"
                endMsText = "5000"
                playRangeStartMs = -1L
                playRangeEndMs = -1L
            }) { Text("Reset") }
            Button(
                enabled = player != null && endMs > startMs && selectedDurationMs >= minDurationMs,
                onClick = {
                    val activePlayer = player ?: return@Button
                    val cappedEnd = if (durationMs > 0L) endMs.coerceAtMost(durationMs) else endMs
                    playRangeStartMs = startMs
                    playRangeEndMs = cappedEnd
                    activePlayer.seekTo(startMs)
                    if (activePlayer.playbackState == Player.STATE_ENDED) {
                        activePlayer.seekTo(startMs)
                    }
                    activePlayer.play()
                }
            ) {
                Text("Play selection")
            }
        }

        validationError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !inputPath.isNullOrBlank() && endMs > startMs && selectedDurationMs >= minDurationMs,
            onClick = {
                val input = inputPath ?: return@Button
                val request = CutRequest(
                    inputPath = input,
                    outputPath = MediaFileUtils.createOutputPath(context, "cut", "mp4"),
                    startMs = startMsText.toLongOrNull() ?: -1L,
                    endMs = endMsText.toLongOrNull() ?: -1L
                )
                val result = CutValidator().validate(request)
                if (!result.isValid) {
                    validationError = result.errorMessage
                } else {
                    validationError = null
                    onSubmitRequest(ProcessingRequest.Cut(request))
                }
            }
        ) {
            Text("Export Cut Video")
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
