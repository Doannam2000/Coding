package com.nantcompany.clipy.history

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.design.ClipyEmptyState
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.design.ClipySecondaryButton
import com.nantcompany.clipy.design.ClipySectionTitle
import com.nantcompany.clipy.export.output.OutputMedia
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryFilter { ALL, VIDEOS, AUDIO }

@Composable
fun OutputHistoryScreen(
    onNavigate: (AppRoute) -> Unit,
    onOutputSelected: (OutputMedia) -> Unit,
    viewModel: OutputHistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    var pendingDelete by remember { mutableStateOf<OutputMedia?>(null) }
    
    LaunchedEffect(Unit) { viewModel.loadHistory() }

    val outputs = uiState.outputs.filter { output ->
        when (filter) {
            HistoryFilter.ALL -> true
            HistoryFilter.VIDEOS -> output.path.endsWith(".mp4", true) || output.path.endsWith(".mov", true)
            HistoryFilter.AUDIO -> output.path.endsWith(".mp3", true) || output.path.endsWith(".m4a", true) || output.path.endsWith(".aac", true) || output.path.endsWith(".wav", true)
        }
    }

    ClipyScaffold(
        title = "Output History",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HistoryFilter.entries.forEach { f ->
                    val label = f.name.lowercase().replaceFirstChar { it.uppercase() }
                    val isSelected = filter == f
                    if (isSelected) {
                        ClipyPrimaryButton(
                            label = label,
                            modifier = Modifier.height(40.dp).weight(1f),
                            onClick = { filter = f }
                        )
                    } else {
                        ClipySecondaryButton(
                            label = label,
                            modifier = Modifier.height(40.dp).weight(1f),
                            onClick = { filter = f }
                        )
                    }
                }
            }

            if (outputs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    ClipyEmptyState(
                        title = "No exports yet",
                        message = if (filter == HistoryFilter.ALL) "Your edited files will appear here." else "No files found for this filter.",
                        icon = Icons.Default.Info
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { ClipySectionTitle(text = "Recent Exports (${outputs.size})") }
                    items(outputs, key = { it.id }) { output ->
                        HistoryItem(
                            output = output,
                            onOutputSelected = onOutputSelected,
                            onDeleteRequest = { pendingDelete = it }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }

    pendingDelete?.let { output ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = ClipyDesignTokens.secondaryText,
            title = { Text("Remove history item?", fontWeight = FontWeight.Bold) },
            text = { Text("This removes the entry from Recent Exports. The media file will not be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeHistoryItem(output)
                    pendingDelete = null
                }) {
                    Text("Remove", color = Color(0xFFFF4B4B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun HistoryItem(
    output: OutputMedia,
    onOutputSelected: (OutputMedia) -> Unit,
    onDeleteRequest: (OutputMedia) -> Unit
) {
    val context = LocalContext.current
    val file = remember(output.path) { File(output.path) }
    val exists = file.exists()
    var showMenu by remember { mutableStateOf(false) }

    val mime = resolveMimeType(output.fileName)
    val isVideo = mime.startsWith("video")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
        colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.PlayArrow else Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isVideo) ClipyDesignTokens.primaryAccent else ClipyDesignTokens.secondaryAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = output.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${output.operation} • ${formatDate(output.createdAtEpochMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClipyDesignTokens.secondaryText
                )
                if (!exists) {
                    Text("File missing", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF4B4B))
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    DropdownMenuItem(
                        text = { Text("Open / Play", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White) },
                        onClick = {
                            showMenu = false
                            if (exists) openFile(context, file)
                        },
                        enabled = exists
                    )
                    DropdownMenuItem(
                        text = { Text("Share", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.White) },
                        onClick = {
                            showMenu = false
                            if (exists) shareFile(context, file)
                        },
                        enabled = exists
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Entry", color = Color(0xFFFF4B4B)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF4B4B)) },
                        onClick = {
                            showMenu = false
                            onDeleteRequest(output)
                        }
                    )
                }
            }
        }
    }
}

private fun openFile(context: Context, file: File) {
    val mimeType = resolveMimeType(file.name)
    val uri = fileUri(context, file)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) { }
}

private fun shareFile(context: Context, file: File) {
    val uri = fileUri(context, file)
    val mimeType = resolveMimeType(file.name)
    val intent = Intent(Intent.ACTION_SEND)
        .setType(mimeType)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.clipData = ClipData.newRawUri(file.name, uri)
    context.startActivity(Intent.createChooser(intent, "Share export"))
}

private fun fileUri(context: Context, file: File) = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

private fun formatDate(epochMs: Long): String {
    return SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(epochMs))
}

private fun resolveMimeType(fileName: String): String {
    val lower = fileName.lowercase(Locale.US)
    return when {
        lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".mkv") -> "video/*"
        lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".wav") -> "audio/*"
        else -> "*/*"
    }
}
