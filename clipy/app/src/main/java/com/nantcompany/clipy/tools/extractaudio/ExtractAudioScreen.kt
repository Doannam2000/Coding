package com.nantcompany.clipy.tools.extractaudio

import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.edit.tools.extractaudio.ExtractAudioRequest
import com.nantcompany.clipy.edit.tools.extractaudio.ExtractAudioValidator
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.picker.MediaItemModel
import com.nantcompany.clipy.picker.VideoMetadataLoader
import java.io.File
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

private data class AudioQualityOption(
    val label: String,
    val description: String,
    val bitrateKbps: Int
)

@Composable
fun ExtractAudioScreen(
    inputPath: String?,
    onSubmitRequest: (ProcessingRequest) -> Unit
) {
    val context = LocalContext.current
    val formats = listOf("mp3", "m4a", "aac")
    val unsupportedFormats = setOf("aac")
    val qualityOptions = listOf(
        AudioQualityOption("Low", "Smallest file, lower quality", 96),
        AudioQualityOption("Standard", "Balanced quality and size", 128),
        AudioQualityOption("High", "Best quality, larger file", 192)
    )
    var format by remember { mutableStateOf("mp3") }
    if (format in unsupportedFormats) format = "mp3"
    var quality by remember { mutableStateOf("Standard") }
    val selectedQuality = qualityOptions.firstOrNull { it.label == quality } ?: qualityOptions[1]
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Extract Audio", style = MaterialTheme.typography.headlineSmall)

        val sourceInfo = remember(inputPath) { inputPath?.let { buildSourceInfo(it) } }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Video info", style = MaterialTheme.typography.titleSmall)
                sourceInfo?.thumbnail?.let { thumb ->
                    Image(
                        bitmap = thumb.asImageBitmap(),
                        contentDescription = "Extract audio video preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    sourceInfo?.displayName ?: inputPath ?: "No video selected",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Duration: ${sourceInfo?.durationMs?.let(::formatDuration) ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                Text("Size: ${sourceInfo?.sizeBytes?.let(::formatBytes) ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                Text("Resolution: ${sourceInfo?.width?.let { w -> sourceInfo.height?.let { h -> "${w}x${h}" } } ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
            }
        }

        Text("Output format", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            formats.forEach { item ->
                val selected = item == format
                val isSupported = item !in unsupportedFormats
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(selected = selected, enabled = isSupported, onClick = { format = item }),
                    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSupported) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        if (isSupported) item.uppercase() else "${item.uppercase()} (Soon)",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSupported) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text("Quality", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            qualityOptions.forEach { option ->
                val selected = option.label == quality
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(selected = selected, onClick = { quality = option.label }),
                    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(option.label, style = MaterialTheme.typography.bodySmall)
                        Text(
                            option.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Text(
            "Selected: ${selectedQuality.label} (${selectedQuality.bitrateKbps} kbps)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        validationError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !inputPath.isNullOrBlank(),
            onClick = {
                val input = inputPath ?: return@Button
                if (!hasAudioTrack(input)) {
                    validationError = "This video has no audio track to extract."
                    return@Button
                }
                val ext = when (format.lowercase()) {
                    "aac", "m4a" -> "m4a"
                    else -> "mp3"
                }
                val request = ExtractAudioRequest(
                    inputPath = input,
                    outputPath = MediaFileUtils.createOutputPath(context, "extract_audio", ext),
                    format = format,
                    bitrateKbps = selectedQuality.bitrateKbps
                )
                val result = ExtractAudioValidator().validate(request)
                if (!result.isValid) {
                    validationError = result.errorMessage
                } else {
                    validationError = null
                    onSubmitRequest(ProcessingRequest.ExtractAudio(request))
                }
            }
        ) {
            Text("Extract Audio")
        }

        Text(
            "No-audio videos may fail during export and will show an error.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildSourceInfo(path: String): MediaItemModel = VideoMetadataLoader.load(path)

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

private fun hasAudioTrack(path: String): Boolean {
    return runCatching {
        val extractor = MediaExtractor()
        extractor.setDataSource(path)
        val found = (0 until extractor.trackCount).any { index ->
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
            mime?.startsWith("audio/") == true
        }
        extractor.release()
        found
    }.getOrDefault(true)
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "Unknown"
    val unit = 1024.0
    val exp = (ln(bytes.toDouble()) / ln(unit)).toInt().coerceIn(0, 4)
    val suffix = listOf("B", "KB", "MB", "GB", "TB")[exp]
    val value = bytes / unit.pow(exp.toDouble())
    val decimals = if (exp == 0) 0 else 1
    return String.format(Locale.getDefault(), "%.${decimals}f %s", value, suffix)
}
