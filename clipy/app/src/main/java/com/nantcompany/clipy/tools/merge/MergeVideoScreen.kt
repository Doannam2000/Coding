package com.nantcompany.clipy.tools.merge

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.edit.tools.merge.MergeRequest
import com.nantcompany.clipy.edit.tools.merge.MergeValidator
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import java.io.File
import java.util.Locale

@Composable
fun MergeVideoScreen(
    inputPaths: List<String>,
    onRemoveAt: (Int) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit
) {
    val context = LocalContext.current
    val validationError = remember { mutableStateOf<String?>(null) }
    val clipSpecs = remember(inputPaths) { inputPaths.map(::readClipSpec) }
    val distinctResolutions = remember(clipSpecs) {
        clipSpecs.mapNotNull { spec ->
            val width = spec.width
            val height = spec.height
            if (width != null && height != null && width > 0 && height > 0) "${width}x${height}" else null
        }.toSet()
    }
    val distinctOrientations = remember(clipSpecs) {
        clipSpecs.mapNotNull { spec ->
            val width = spec.width
            val height = spec.height
            when {
                width == null || height == null || width <= 0 || height <= 0 -> null
                width > height -> "landscape"
                width < height -> "portrait"
                else -> "square"
            }
        }.toSet()
    }
    val showMixedWarning = distinctResolutions.size > 1 || distinctOrientations.size > 1
    var warningExpanded by remember { mutableStateOf(false) }

    if (!showMixedWarning) {
        warningExpanded = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Merge Videos", style = MaterialTheme.typography.headlineSmall)
        Text("Selected clips: ${inputPaths.size}", style = MaterialTheme.typography.bodyMedium)

        if (inputPaths.size < 2) {
            Text("Select at least 2 videos to merge.", color = MaterialTheme.colorScheme.error)
        }

        if (showMixedWarning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Mixed clip formats detected", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Some clips have different orientation or resolution. Clipy will fit clips automatically to keep export safe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    TextButton(onClick = { warningExpanded = !warningExpanded }) {
                        Text(if (warningExpanded) "Hide details" else "Show details")
                    }
                    if (warningExpanded) {
                        Text(
                            "Resolutions: ${distinctResolutions.ifEmpty { setOf("Unknown") }.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "Orientations: ${distinctOrientations.ifEmpty { setOf("Unknown") }.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        inputPaths.forEachIndexed { index, path ->
            val spec = clipSpecs.getOrNull(index)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("#${index + 1}", style = MaterialTheme.typography.labelLarge)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            path,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Duration: ${formatClipDuration(spec?.durationMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onRemoveAt(index) }) {
                        Text("Remove")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Transition", style = MaterialTheme.typography.titleSmall)
                Text("None", style = MaterialTheme.typography.bodySmall)
                Text("Fade · Coming soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Cross dissolve · Coming soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text(
            "Reorder is not available yet. Merge order follows your picker selection.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "If clip sizes differ, Clipy keeps safe automatic fitting.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        validationError.value?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onNavigate(AppRoute.PICK_MULTIPLE_VIDEOS) }) {
                Text("Add more clips")
            }
            TextButton(onClick = { onNavigate(AppRoute.PICK_MULTIPLE_VIDEOS) }) {
                Text("Back to picker")
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = inputPaths.size >= 2,
            onClick = {
                val request = MergeRequest(
                    inputPaths = inputPaths,
                    outputPath = MediaFileUtils.createOutputPath(context, "merge", "mp4")
                )
                val result = MergeValidator().validate(request)
                if (!result.isValid) {
                    validationError.value = result.errorMessage
                } else {
                    validationError.value = null
                    onSubmitRequest(ProcessingRequest.Merge(request))
                }
            }
        ) {
            Text("Merge Videos")
        }
    }
}

data class ClipSpec(
    val width: Int?,
    val height: Int?,
    val durationMs: Long?
)

private fun readClipSpec(path: String): ClipSpec {
    val file = File(path)
    if (!file.exists()) return ClipSpec(width = null, height = null, durationMs = null)
    var width: Int? = null
    var height: Int? = null
    var durationMs: Long? = null
    runCatching {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
        height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        retriever.release()
    }
    return ClipSpec(width = width, height = height, durationMs = durationMs)
}

private fun formatClipDuration(durationMs: Long?): String {
    if (durationMs == null || durationMs <= 0L) return "Unknown"
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
