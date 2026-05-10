package com.nantcompany.clipy.result

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.nantcompany.clipy.design.ScreenLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.nantcompany.clipy.export.output.OutputMedia
import java.net.URLConnection
import com.nantcompany.clipy.navigation.AppRoute
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultScreen(
    output: OutputMedia?,
    onNavigate: (AppRoute) -> Unit
) {
    val context = LocalContext.current
    val message = remember { mutableStateOf<String?>(null) }

    val createAnotherRoute = when (output?.operation?.lowercase(Locale.US)) {
        "cut", "compress", "extractaudio", "extract_audio", "extract-audio" -> AppRoute.PICK_VIDEO
        "merge" -> AppRoute.PICK_MULTIPLE_VIDEOS
        "slideshow" -> AppRoute.PICK_IMAGES
        else -> AppRoute.HOME
    }

    ScreenLayout(
        title = "Export complete",
        subtitle = "Your file is ready",
        primaryActionLabel = "Create another",
        onPrimaryAction = { onNavigate(createAnotherRoute) },
        content = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "✓",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF4CAF50),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )

                if (output == null) {
                    Text("No recent output available.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val file = File(output.path)
                    val exists = file.exists() && file.length() > 0L
                    val mimeType = resolveMimeType(file.name)
                    val isVideoOutput = mimeType.startsWith("video/")
                    val isAudioOutput = mimeType.startsWith("audio/")
                    val openLabel = when {
                        isVideoOutput -> "Play"
                        isAudioOutput -> "Play Audio"
                        else -> "Open"
                    }
                    val previewLabel = when {
                        isVideoOutput -> "Video Preview"
                        isAudioOutput -> "Audio Preview"
                        else -> "Preview"
                    }
                    val outputKindLabel = when {
                        isVideoOutput -> "Video output"
                        isAudioOutput -> "Audio output"
                        else -> "Output"
                    }
                    val missingMessage = when {
                        isVideoOutput -> "Video file is missing."
                        isAudioOutput -> "Audio file is missing."
                        else -> "Output file is missing."
                    }
                    val openFailedMessage = when {
                        isVideoOutput -> "Could not play this video."
                        isAudioOutput -> "Could not play this audio file."
                        else -> "Could not open this file."
                    }
                    val previewFailedMessage = when {
                        isVideoOutput -> "Could not preview this video."
                        isAudioOutput -> "Could not preview this audio file."
                        else -> "Could not preview this file."
                    }

                    if (isVideoOutput) {
                        Text("Video output ready", style = MaterialTheme.typography.titleSmall)
                    }
                    if (isAudioOutput) {
                        Text("Audio output ready", style = MaterialTheme.typography.titleSmall)
                    }

                    Text(outputKindLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(output.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Output info", style = MaterialTheme.typography.titleSmall)
                            Text(output.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Type: $mimeType", style = MaterialTheme.typography.bodySmall)
                            Text("Operation: ${output.operation}", style = MaterialTheme.typography.bodySmall)
                            Text("Size: ${formatFileSize(output.sizeInBytes)}", style = MaterialTheme.typography.bodySmall)
                            Text("Created: ${formatDate(output.createdAtEpochMs)}", style = MaterialTheme.typography.bodySmall)
                            Text("Saved location", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(output.path, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("Status: ${if (exists) "Verified" else "Missing"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (!exists) {
                        Text(missingMessage, color = MaterialTheme.colorScheme.error)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = exists,
                            onClick = {
                                val ok = previewFile(context, file, mimeType)
                                if (!ok) message.value = previewFailedMessage
                            }
                        ) {
                            Text(previewLabel)
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = exists,
                            onClick = {
                                val ok = openFile(context, file)
                                if (!ok) message.value = openFailedMessage
                            }
                        ) {
                            Text(openLabel)
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = exists,
                            onClick = {
                                val ok = shareFile(context, file)
                                if (!ok) message.value = "Could not share this file."
                            }
                        ) {
                            Text("Share")
                        }
                    }
                }

                message.value?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

                OutlinedButton(onClick = { onNavigate(AppRoute.HOME) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Back Home")
                }
            }
        }
    )
}

private fun previewFile(context: Context, file: File, mimeType: String): Boolean {
    if (!file.exists()) return false
    val uri = fileUri(context, file)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private fun openFile(context: Context, file: File): Boolean {
    if (!file.exists()) return false
    val mimeType = resolveMimeType(file.name)
    val uri = fileUri(context, file)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private fun shareFile(context: Context, file: File): Boolean {
    if (!file.exists()) return false
    val uri = fileUri(context, file)
    val mimeType = resolveMimeType(file.name)
    val intent = Intent(Intent.ACTION_SEND)
        .setType(mimeType)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.clipData = ClipData.newRawUri(file.name, uri)
    return try {
        context.startActivity(Intent.createChooser(intent, "Share export"))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private fun fileUri(context: Context, file: File): Uri {
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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

private fun formatDate(epochMs: Long): String {
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))
    }.getOrDefault("Unknown date")
}

private fun resolveMimeType(fileName: String): String {
    return URLConnection.guessContentTypeFromName(fileName)?.takeIf { it.isNotBlank() } ?: "*/*"
}
