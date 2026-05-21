package com.nantcompany.clipy.result

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipySecondaryButton
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.export.output.OutputMedia
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.nantcompany.clipy.design.ClipyScaffold

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

    ClipyScaffold(
        title = "Export Complete",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF5ED6A8).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color(0xFF5ED6A8),
                    modifier = Modifier.size(40.dp)
                )
            }

            if (output == null) {
                Text("No recent output available.", style = MaterialTheme.typography.bodyMedium, color = ClipyDesignTokens.secondaryText)
            } else {
                val file = File(output.path)
                val exists = file.exists() && file.length() > 0L
                val mimeType = resolveMimeType(file.name)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
                    colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            output.fileName, 
                            style = MaterialTheme.typography.titleMedium, 
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis, 
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        
                        InfoRow(label = "Format", value = mimeType.substringAfterLast('/'))
                        InfoRow(label = "Operation", value = output.operation)
                        InfoRow(label = "Size", value = formatFileSize(output.sizeInBytes))
                        InfoRow(label = "Created", value = formatDate(output.createdAtEpochMs))
                        
                        androidx.compose.material3.HorizontalDivider(color = Color(0x11FFFFFF))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Saved location", style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.secondaryText)
                            Text(output.path, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = ClipyDesignTokens.secondaryText)
                        }
                        
                        if (!exists) {
                            Text("File not found on disk.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClipyPrimaryButton(
                            modifier = Modifier.weight(1f),
                            label = "Play File",
                            enabled = exists,
                            onClick = {
                                val ok = openFile(context, file)
                                if (!ok) message.value = "Could not open this file."
                            }
                        )
                        ClipySecondaryButton(
                            modifier = Modifier.weight(1f),
                            label = "Save to Gallery",
                            enabled = exists,
                            onClick = {
                                val uri = com.nantcompany.clipy.app.GallerySaver.saveToGallery(context, file)
                                if (uri != null) message.value = "Saved to Gallery!"
                                else message.value = "Failed to save to Gallery."
                            }
                        )
                    }

                    ClipySecondaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Share",
                        enabled = exists,
                        onClick = {
                            val ok = shareFile(context, file)
                            if (!ok) message.value = "Could not share this file."
                        }
                    )

                    ClipyPrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Create Another",
                        onClick = { onNavigate(createAnotherRoute) }
                    )
                    
                    ClipySecondaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Back to Home",
                        onClick = { onNavigate(AppRoute.HOME) }
                    )
                }
            }

            message.value?.let { 
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) 
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.secondaryText)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color.White)
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
    if (bytes <= 0L) return "0 B"
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
