package com.nantcompany.clipy.history

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.design.ClipyEmptyState
import com.nantcompany.clipy.export.output.OutputMedia
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryFilter { ALL, VIDEOS, AUDIO }

@Composable
fun OutputHistoryScreen(
    onOutputSelected: (OutputMedia) -> Unit,
    viewModel: OutputHistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val filter = remember { mutableStateOf(HistoryFilter.ALL) }
    val pendingDelete = remember { mutableStateOf<OutputMedia?>(null) }
    LaunchedEffect(Unit) { viewModel.loadHistory() }

    val outputs = uiState.outputs.filter { output ->
        when (filter.value) {
            HistoryFilter.ALL -> true
            HistoryFilter.VIDEOS -> output.path.endsWith(".mp4", true) || output.path.endsWith(".mov", true)
            HistoryFilter.AUDIO -> output.path.endsWith(".mp3", true) || output.path.endsWith(".m4a", true) || output.path.endsWith(".aac", true) || output.path.endsWith(".wav", true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Recent Exports", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (filter.value == HistoryFilter.ALL) {
                Button(onClick = { filter.value = HistoryFilter.ALL }) { Text("All") }
            } else {
                OutlinedButton(onClick = { filter.value = HistoryFilter.ALL }) { Text("All") }
            }
            if (filter.value == HistoryFilter.VIDEOS) {
                Button(onClick = { filter.value = HistoryFilter.VIDEOS }) { Text("Videos") }
            } else {
                OutlinedButton(onClick = { filter.value = HistoryFilter.VIDEOS }) { Text("Videos") }
            }
            if (filter.value == HistoryFilter.AUDIO) {
                Button(onClick = { filter.value = HistoryFilter.AUDIO }) { Text("Audio") }
            } else {
                OutlinedButton(onClick = { filter.value = HistoryFilter.AUDIO }) { Text("Audio") }
            }
        }

        uiState.message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            LaunchedEffect(it) { viewModel.consumeMessage() }
        }

        if (outputs.isEmpty()) {
            ClipyEmptyState(
                title = "No exports yet",
                message = "Your finished files will appear here."
            )
        } else {
            outputs.forEach { output ->
                val fileMissing = !File(output.path).exists()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !fileMissing) { onOutputSelected(output) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val mediaType = resolveMediaType(output.path)
                        Text(output.fileName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Type: $mediaType", style = MaterialTheme.typography.bodySmall)
                        Text("${output.operation} • ${formatFileSize(output.sizeInBytes)}", style = MaterialTheme.typography.bodySmall)
                        Text(formatDate(output.createdAtEpochMs), style = MaterialTheme.typography.bodySmall)
                        Text(output.path, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (fileMissing) {
                            Text("File missing (open/share disabled)", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Ready to open", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                enabled = !fileMissing,
                                onClick = {
                                    val opened = openFile(context, File(output.path))
                                    if (!opened) viewModel.showMessage("Could not open this file.")
                                }
                            ) {
                                Text("Open")
                            }
                            OutlinedButton(
                                enabled = !fileMissing,
                                onClick = {
                                    val shared = shareFile(context, File(output.path))
                                    if (!shared) viewModel.showMessage("Could not share this file.")
                                }
                            ) {
                                Text("Share")
                            }
                            var menuExpanded by remember(output.id) { mutableStateOf(false) }
                            TextButton(onClick = { menuExpanded = true }) {
                                Text("More")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Open") },
                                    onClick = {
                                        menuExpanded = false
                                        if (!fileMissing) onOutputSelected(output)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    onClick = {
                                        menuExpanded = false
                                        if (!fileMissing) {
                                            val shared = shareFile(context, File(output.path))
                                            if (!shared) viewModel.showMessage("Could not share this file.")
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete history") },
                                    onClick = {
                                        menuExpanded = false
                                        pendingDelete.value = output
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete.value?.let { output ->
        AlertDialog(
            onDismissRequest = { pendingDelete.value = null },
            title = { Text("Remove history item?") },
            text = { Text("This removes the entry from Recent Exports. The media file will not be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeHistoryItem(output)
                    pendingDelete.value = null
                }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete.value = null }) {
                    Text("Cancel")
                }
            }
        )
    }
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

private fun resolveMediaType(path: String): String {
    val lower = path.lowercase(Locale.getDefault())
    return when {
        lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".mkv") -> "Video"
        lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".wav") -> "Audio"
        else -> "Unknown"
    }
}

private fun openFile(context: Context, file: File): Boolean {
    if (!file.exists()) return false
    val mimeType = resolveMimeType(file.name)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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
    val mimeType = resolveMimeType(file.name)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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

private fun resolveMimeType(path: String): String {
    val lower = path.lowercase(Locale.getDefault())
    return when {
        lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".mkv") -> "video/*"
        lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".wav") -> "audio/*"
        else -> "*/*"
    }
}
