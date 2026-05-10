package com.nantcompany.clipy.tools.compress

import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.material3.OutlinedTextField
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
import com.nantcompany.clipy.edit.tools.compress.CompressRequest
import com.nantcompany.clipy.edit.tools.compress.CompressValidator
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.picker.MediaItemModel
import com.nantcompany.clipy.picker.VideoMetadataLoader
import java.io.File
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

private data class CompressPreset(
    val title: String,
    val subtitle: String,
    val bitrateKbps: Int,
    val targetHeight: Int?
)

private data class ResolutionOption(
    val label: String,
    val height: Int?
)

@Composable
fun CompressVideoScreen(
    inputPath: String?,
    onSubmitRequest: (ProcessingRequest) -> Unit
) {
    val context = LocalContext.current
    val presets = listOf(
        CompressPreset("Small file", "Smaller output for sharing", 700, 480),
        CompressPreset("Balanced", "Good quality and size", 1200, 720),
        CompressPreset("High quality", "Best quality, larger file", 2200, 1080)
    )
    val resolutionOptions = listOf(
        ResolutionOption("Original", null),
        ResolutionOption("1080p", 1080),
        ResolutionOption("720p", 720),
        ResolutionOption("480p", 480)
    )
    val bitrateOptions = listOf(700, 1200, 1800, 2200, 3000)
    var selectedPreset by remember { mutableStateOf(presets[1]) }
    var selectedResolution by remember { mutableStateOf(resolutionOptions[2]) }
    var selectedBitrateKbps by remember { mutableStateOf(presets[1].bitrateKbps) }
    var keepAudio by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Compress Video", style = MaterialTheme.typography.headlineSmall)

        val sourceInfo = remember(inputPath) { inputPath?.let { buildSourceInfo(it) } }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Original file", style = MaterialTheme.typography.titleSmall)
                sourceInfo?.thumbnail?.let { thumb ->
                    Image(
                        bitmap = thumb.asImageBitmap(),
                        contentDescription = "Video preview",
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
                Text("Resolution: ${sourceInfo?.width?.let { w -> sourceInfo.height?.let { h -> "${w}x${h}" } } ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                Text("Size: ${sourceInfo?.sizeBytes?.let(::formatBytes) ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
            }
        }

        Text("Preset", style = MaterialTheme.typography.titleMedium)
        presets.forEach { preset ->
            val isSelected = selectedPreset == preset
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = isSelected, onClick = {
                        selectedPreset = preset
                        selectedBitrateKbps = preset.bitrateKbps
                        selectedResolution = resolutionOptions.firstOrNull { it.height == preset.targetHeight } ?: resolutionOptions.first()
                    }),
                shape = RoundedCornerShape(16.dp),
                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(preset.title, style = MaterialTheme.typography.titleSmall)
                    Text(preset.subtitle, style = MaterialTheme.typography.bodySmall)
                    Text("${preset.bitrateKbps} kbps • ${preset.targetHeight ?: 0}p", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Text("Advanced", style = MaterialTheme.typography.titleMedium)

        Text("Resolution", style = MaterialTheme.typography.bodyMedium)
        resolutionOptions.forEach { option ->
            val isSelected = selectedResolution == option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = isSelected, onClick = { selectedResolution = option }),
                shape = RoundedCornerShape(14.dp),
                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(option.label, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text("Bitrate", style = MaterialTheme.typography.bodyMedium)
        bitrateOptions.forEach { bitrate ->
            val isSelected = selectedBitrateKbps == bitrate
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = isSelected, onClick = { selectedBitrateKbps = bitrate }),
                shape = RoundedCornerShape(14.dp),
                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text("$bitrate kbps", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { keepAudio = !keepAudio }) {
                Text(if (keepAudio) "Audio: Keep" else "Audio: Remove")
            }
            OutlinedTextField(
                value = if (keepAudio) "On" else "Off",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.weight(1f),
                label = { Text("Keep audio") }
            )
        }

        val estimatedSizeText = remember(sourceInfo?.durationMs, selectedBitrateKbps, keepAudio) {
            estimateOutputSizeText(
                durationMs = sourceInfo?.durationMs,
                videoBitrateKbps = selectedBitrateKbps,
                keepAudio = keepAudio
            )
        }
        Text("Estimated output: $estimatedSizeText", style = MaterialTheme.typography.bodySmall)

        validationError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !inputPath.isNullOrBlank(),
            onClick = {
                val input = inputPath ?: return@Button
                val request = CompressRequest(
                    inputPath = input,
                    outputPath = MediaFileUtils.createOutputPath(context, "compress", "mp4"),
                    bitrateKbps = selectedBitrateKbps,
                    targetHeight = selectedResolution.height,
                    keepAudio = keepAudio
                )
                val result = CompressValidator().validate(request)
                if (!result.isValid) {
                    validationError = result.errorMessage
                } else {
                    validationError = null
                    onSubmitRequest(ProcessingRequest.Compress(request))
                }
            }
        ) {
            Text("Compress Video")
        }
    }
}

private fun buildSourceInfo(path: String): MediaItemModel = VideoMetadataLoader.load(path)

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
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

private fun estimateOutputSizeText(
    durationMs: Long?,
    videoBitrateKbps: Int,
    keepAudio: Boolean
): String {
    if (durationMs == null || durationMs <= 0L) {
        return "Estimate unavailable until source duration is known."
    }
    val audioBitrateKbps = if (keepAudio) 128 else 0
    val totalBitrateKbps = (videoBitrateKbps + audioBitrateKbps).coerceAtLeast(1)
    val estimatedBytes = (durationMs / 1000.0) * (totalBitrateKbps * 1000.0 / 8.0)
    return formatBytes(estimatedBytes.toLong())
}
