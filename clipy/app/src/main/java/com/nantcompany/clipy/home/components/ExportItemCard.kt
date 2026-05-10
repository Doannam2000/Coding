package com.nantcompany.clipy.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.export.output.OutputMedia

@Composable
fun ExportItemCard(output: OutputMedia, onClick: () -> Unit) {
    val isAudio = output.fileName.endsWith(".mp3", true) || output.fileName.endsWith(".wav", true)
    val formatTag = if (isAudio) "WAV" else "MP4"
    val qualityTag = if (isAudio) "" else if (output.sizeInBytes > 800_000_000L) "4K" else "1080P"
    val thumbTime = if (isAudio) "" else if (output.sizeInBytes > 800_000_000L) "03:45" else "12:10"
    val subtitle = buildString {
        append(
            when {
                output.sizeInBytes >= 1_000_000_000L -> "1.2 GB"
                output.sizeInBytes >= 100_000_000L -> "450 MB"
                else -> "45 MB"
            }
        )
        append(" • ")
        append(output.operation)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17253F)),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(Color(0xFF2D3F63), Color(0xFF212F4A))))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(modifier = Modifier.size(70.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFF0D1629)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isAudio) Icons.Default.Star else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isAudio) Color(0xFF22D3EE) else Color(0xFFA78BFA),
                        modifier = Modifier.size(28.dp)
                    )
                    if (thumbTime.isNotBlank()) {
                        Text(
                            thumbTime,
                            color = Color(0xFFD6E2FF),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniTag(formatTag)
                    if (qualityTag.isNotBlank()) MiniTag(qualityTag)
                }
                Text(
                    output.fileName,
                    color = Color(0xFFD5E0F5),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    color = Color(0xFF7F95BC),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color(0xFF8FA4C6), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MiniTag(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF675E8A).copy(alpha = 0.55f)) {
        Text(
            text = text,
            color = Color(0xFFDCD1FF),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
