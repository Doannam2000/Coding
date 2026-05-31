package com.nantcompany.clipy.result

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.design.ClipySecondaryButton
import com.nantcompany.clipy.export.output.OutputMedia
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultScreen(
    output: OutputMedia?,
    onNavigate: (AppRoute) -> Unit
) {
    val context = LocalContext.current
    val file = remember(output?.path) { output?.path?.let { File(it) } }
    val exists = file?.exists() == true
    val message = remember { mutableStateOf("") }

    ClipyScaffold(
        title = "Export Success",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Surface(
                color = ClipyDesignTokens.success.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ClipyDesignTokens.success,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Your video is ready!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "The file has been saved to your internal storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClipyDesignTokens.secondaryText,
                    textAlign = TextAlign.Center
                )
            }

            if (output != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
                    colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ResultInfoRow("File Name", output.fileName)
                        ResultInfoRow("Operation", output.operation)
                        ResultInfoRow("Size", formatBytes(output.sizeInBytes))
                        ResultInfoRow("Date", formatDate(output.createdAtEpochMs))
                    }
                }
            }

            if (message.value.isNotEmpty()) {
                Text(message.value, color = ClipyDesignTokens.primaryAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }

            if (file != null) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClipyPrimaryButton(
                            modifier = Modifier.weight(1f),
                            label = "Play File",
                            enabled = exists,
                            onClick = { onNavigate(AppRoute.VIDEO_PLAYER) }
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
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        label = "Back to Home",
                        onClick = { onNavigate(AppRoute.HOME) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResultInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = ClipyDesignTokens.secondaryText)
        Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

private fun openFile(context: Context, file: File): Boolean {
    val mimeType = resolveMimeType(file.name)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching { context.startActivity(intent); true }.getOrDefault(false)
}

private fun shareFile(context: Context, file: File): Boolean {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val mimeType = resolveMimeType(file.name)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newRawUri(file.name, uri)
    }
    return runCatching { context.startActivity(Intent.createChooser(intent, "Share video")); true }.getOrDefault(false)
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}

private fun formatDate(epochMs: Long): String {
    return runCatching {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(epochMs))
    }.getOrDefault("Unknown date")
}

private fun resolveMimeType(fileName: String): String {
    return URLConnection.guessContentTypeFromName(fileName)?.takeIf { it.isNotBlank() } ?: "*/*"
}
