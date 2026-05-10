package com.nantcompany.clipy.picker

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import com.nantcompany.clipy.picker.ImageMetadataLoader
import com.nantcompany.clipy.picker.MediaItemModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
fun PickImagesScreen(
    selectedPaths: List<String>,
    screenTitle: String,
    instructionText: String,
    onImagesPicked: (List<String>) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onContinue: () -> Unit,
    onNavigate: (AppRoute) -> Unit
) {
    val context = LocalContext.current
    val message = remember { mutableStateOf<String?>(null) }
    val deniedAccess = remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val localPaths = uris.mapNotNull { uri ->
                runCatching {
                    MediaFileUtils.importUriToLocalPath(
                        context = context,
                        uri = uri,
                        folderName = "imports/images",
                        defaultExtension = "jpg"
                    )
                }.getOrNull()
            }
            if (localPaths.isNotEmpty()) {
                onImagesPicked(localPaths)
                deniedAccess.value = false
                message.value = null
            } else {
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

    val canContinue = selectedPaths.size >= 2
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
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(screenTitle, style = MaterialTheme.typography.headlineSmall)
        Text(instructionText, style = MaterialTheme.typography.bodySmall)
        Text("Selected: ${selectedPaths.size}", style = MaterialTheme.typography.bodyMedium)
        if (!canContinue) {
            Text("Select at least 2 images to create a slideshow.", color = MaterialTheme.colorScheme.error)
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

        selectedPaths.forEachIndexed { index, path ->
            val info = remember(path) { buildImagePreview(path) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (info.thumbnail != null) {
                        Image(
                            bitmap = info.thumbnail.asImageBitmap(),
                            contentDescription = "Image thumbnail",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Placeholder thumbnail", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("${index + 1}. ${info.displayName}", style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Size: ${formatBytes(info.sizeBytes)}", style = MaterialTheme.typography.bodySmall)
                        val resolutionText = info.width?.let { w -> info.height?.let { h -> "${w}x${h}" } } ?: "Unknown resolution"
                        Text("Resolution: $resolutionText", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(path, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { onRemoveAt(index) }) { Text("Remove") }
                    }
                }
            }
        }

        Button(onClick = launchPicker) {
            Text(if (selectedPaths.isEmpty()) "Select Images" else "Add more")
        }

        Button(
            enabled = canContinue,
            onClick = onContinue
        ) {
            Text("Continue")
        }
    }
}

private fun buildImagePreview(path: String): MediaItemModel = ImageMetadataLoader.load(path)

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "Unknown size"
    val unit = 1024.0
    val exp = (ln(bytes.toDouble()) / ln(unit)).toInt().coerceIn(0, 4)
    val suffix = listOf("B", "KB", "MB", "GB", "TB")[exp]
    val value = bytes / unit.pow(exp.toDouble())
    val decimals = if (exp == 0) 0 else 1
    return String.format(Locale.getDefault(), "%.${decimals}f %s", value, suffix)
}
