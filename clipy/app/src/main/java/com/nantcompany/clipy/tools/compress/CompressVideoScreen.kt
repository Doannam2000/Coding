package com.nantcompany.clipy.tools.compress

import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.design.ClipySecondaryButton
import com.nantcompany.clipy.edit.tools.compress.CompressRequest
import com.nantcompany.clipy.edit.tools.compress.CompressValidator
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun CompressVideoScreen(
    inputPath: String?,
    onNavigate: (AppRoute) -> Unit,
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
    var selectedPreset by remember { mutableStateOf(presets[1]) }
    var selectedResolution by remember { mutableStateOf(resolutionOptions[2]) }
    var selectedBitrateKbps by remember { mutableStateOf(presets[1].bitrateKbps) }
    var keepAudio by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    ClipyScaffold(
        title = "Compress Video",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val sourceInfo = remember(inputPath) { inputPath?.let { buildSourceInfo(it) } }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
                colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Original File", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    sourceInfo?.thumbnail?.let { thumb ->
                        Image(
                            bitmap = thumb.asImageBitmap(),
                            contentDescription = "Video preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        sourceInfo?.displayName ?: inputPath ?: "No video selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Duration: ${sourceInfo?.durationMs?.let(::formatDuration) ?: "..."}", style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.secondaryText)
                        Text("Size: ${sourceInfo?.sizeBytes?.let(::formatBytes) ?: "..."}", style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.secondaryText)
                    }
                }
            }

            Text("Select Preset", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
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
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, ClipyDesignTokens.primaryAccent) else androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) ClipyDesignTokens.primaryAccent.copy(alpha = 0.1f) else ClipyDesignTokens.cardSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(preset.title, style = MaterialTheme.typography.titleSmall, color = if (isSelected) ClipyDesignTokens.primaryAccent else Color.White, fontWeight = FontWeight.Bold)
                        Text(preset.subtitle, style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.secondaryText)
                        Text("${preset.bitrateKbps} kbps • ${preset.targetHeight ?: 0}p", style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.primaryAccent)
                    }
                }
            }

            Text("Audio Settings", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            ClipySecondaryButton(
                label = if (keepAudio) "Keep Audio Track" else "Remove Audio Track",
                modifier = Modifier.fillMaxWidth(),
                onClick = { keepAudio = !keepAudio }
            )

            val estimatedSizeText = remember(sourceInfo?.durationMs, selectedBitrateKbps, keepAudio) {
                estimateOutputSizeText(
                    durationMs = sourceInfo?.durationMs,
                    videoBitrateKbps = selectedBitrateKbps,
                    keepAudio = keepAudio
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "Estimated Output Size: $estimatedSizeText", 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = ClipyDesignTokens.primaryAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            validationError?.let { message ->
                Text(message, color = Color(0xFFFF4B4B), style = MaterialTheme.typography.labelSmall)
            }

            ClipyPrimaryButton(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !inputPath.isNullOrBlank(),
                label = "Compress Now",
                onClick = {
                    val input = inputPath ?: return@ClipyPrimaryButton
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
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private data class CompressPreset(val title: String, val subtitle: String, val bitrateKbps: Int, val targetHeight: Int?)
private data class ResolutionOption(val label: String, val height: Int?)

private data class SourceInfo(
    val displayName: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val thumbnail: Bitmap?
)

private fun buildSourceInfo(path: String): SourceInfo {
    val file = File(path)
    return SourceInfo(file.name, file.length(), 0L, 0, 0, null)
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", bytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

private fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%d:%02d".format(mins, secs)
}

private fun estimateOutputSizeText(durationMs: Long?, videoBitrateKbps: Int, keepAudio: Boolean): String {
    if (durationMs == null || durationMs <= 0) return "Unknown"
    val audioBitrateKbps = if (keepAudio) 128 else 0
    val totalBitrateKbps = (videoBitrateKbps + audioBitrateKbps).coerceAtLeast(1)
    val estimatedBytes = (durationMs / 1000.0) * (totalBitrateKbps * 1000.0 / 8.0)
    return formatBytes(estimatedBytes.toLong())
}
