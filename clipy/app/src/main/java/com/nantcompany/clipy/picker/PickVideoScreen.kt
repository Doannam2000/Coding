package com.nantcompany.clipy.picker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.navigation.AppRoute
import java.io.File
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

@Composable
fun PickVideoScreen(
    selectedPath: String?,
    screenTitle: String,
    instructionText: String,
    onVideoPicked: (String?) -> Unit,
    onContinue: () -> Unit,
    onNavigate: (AppRoute) -> Unit
) {
    val context = LocalContext.current
    val message = remember { mutableStateOf<String?>(null) }
    val deniedAccess = remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                MediaFileUtils.importUriToLocalPath(
                    context = context,
                    uri = uri,
                    folderName = "imports/video",
                    defaultExtension = "mp4"
                )
            }.onSuccess {
                onVideoPicked(it)
                deniedAccess.value = false
                message.value = null
            }.onFailure {
                message.value = if (deniedAccess.value) {
                    "Access is blocked. Open app settings and allow media access."
                } else {
                    "Media access was denied. Allow access and retry."
                }
                deniedAccess.value = true
            }
        } else {
            deniedAccess.value = false
            message.value = null
        }
    }

    val canContinue = !selectedPath.isNullOrBlank()
    val openSettings = {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        context.startActivity(intent)
    }
    val launchPicker = {
        deniedAccess.value = false
        message.value = null
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(screenTitle, style = MaterialTheme.typography.headlineSmall)
        Text(instructionText, style = MaterialTheme.typography.bodySmall)
        val selectedFile = selectedPath?.let(::File)
        val fileExists = selectedFile?.exists() == true
        val preview = remember(selectedPath) {
            selectedPath?.let { buildVideoPreview(it) }
        }

        if (selectedPath == null) {
            Text(
                "No media selected",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            val info = preview ?: buildVideoPreview(selectedPath)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (info.thumbnail != null) {
                        Image(
                            bitmap = info.thumbnail.asImageBitmap(),
                            contentDescription = "Video thumbnail",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(172.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(172.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Placeholder thumbnail", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(info.name, style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Duration: ${info.durationText}", style = MaterialTheme.typography.bodySmall)
                        Text("Size: ${info.sizeText}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Resolution: ${info.resolutionText}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        selectedPath,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (selectedPath != null && !fileExists) {
            Text("Selected file is missing.", color = MaterialTheme.colorScheme.error)
        }
        if (!canContinue) {
            Text("Please select one video.", color = MaterialTheme.colorScheme.error)
        }
        message.value?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        if (deniedAccess.value) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = launchPicker) {
                    Text("Allow access and retry")
                }
                OutlinedButton(onClick = openSettings) {
                    Text("Open app settings")
                }
            }
        }

        Button(onClick = launchPicker) {
            Text("Select Video")
        }

        OutlinedButton(
            enabled = canContinue,
            onClick = {
                onVideoPicked(null)
                deniedAccess.value = false
                message.value = null
            }
        ) {
            Text("Remove selected")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = canContinue,
            onClick = onContinue
        ) {
            Text("Continue")
        }
    }
}

data class VideoPreviewData(
    val thumbnail: Bitmap?,
    val name: String,
    val durationText: String,
    val sizeText: String,
    val resolutionText: String
)

private fun buildVideoPreview(path: String): VideoPreviewData {
    val file = File(path)
    val name = file.name.ifBlank { "Unnamed video" }
    val sizeText = formatBytes(file.takeIf { it.exists() }?.length() ?: 0L)

    var durationText = "Unknown duration"
    var resolutionText = "Unknown resolution"
    var thumbnail: Bitmap? = runCatching {
        ThumbnailUtils.createVideoThumbnail(
            path,
            MediaStore.Images.Thumbnails.MINI_KIND
        )
    }.getOrNull()

    runCatching {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        if (durationMs != null && durationMs > 0L) {
            durationText = formatDuration(durationMs)
        }
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        if (width != null && height != null && width > 0 && height > 0) {
            resolutionText = "${width}x${height}"
        }
        if (thumbnail == null) {
            thumbnail = retriever.frameAtTime
        }
        retriever.release()
    }

    return VideoPreviewData(
        thumbnail = thumbnail,
        name = name,
        durationText = durationText,
        sizeText = sizeText,
        resolutionText = resolutionText
    )
}

private fun formatDuration(durationMs: Long): String {
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

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "Unknown size"
    val unit = 1024.0
    val exp = (ln(bytes.toDouble()) / ln(unit)).toInt().coerceIn(0, 4)
    val suffix = listOf("B", "KB", "MB", "GB", "TB")[exp]
    val value = bytes / unit.pow(exp.toDouble())
    val decimals = if (exp == 0) 0 else 1
    return String.format(Locale.getDefault(), "%.${decimals}f %s", value, suffix)
}
