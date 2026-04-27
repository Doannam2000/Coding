package com.example.clipystudio.ui.main.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clipystudio.data.*
import com.example.clipystudio.theme.StudioSecondary
import com.example.clipystudio.ui.main.models.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause

@Composable
fun EditorPlaybackBar(timeline: Timeline, onPlay: () -> Unit, onSeekBy: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .height(36.dp)
            .background(Color.Transparent)
            .padding(horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PlaybackJumpChip(label = "-1s", enabled = timeline.durationMs > 0L) { onSeekBy(-1_000L) }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = EditorChromeSurfaceLow,
            border = androidx.compose.foundation.BorderStroke(1.dp, EditorChromeBorder),
        ) {
            Text(
                timeline.playheadMs.asTimecode(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Surface(onClick = onPlay, shape = RoundedCornerShape(999.dp), color = EditorChromePrimary, modifier = Modifier.size(34.dp)) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
                if (timeline.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (timeline.isPlaying) "Pause" else "Play",
                tint = Color(0xFF07111F),
            )
          }
        }

        PlaybackJumpChip(label = "+1s", enabled = timeline.durationMs > 0L) { onSeekBy(1_000L) }
    }
}

@Composable
private fun PlaybackJumpChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        color = EditorChromeSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, EditorChromeBorder),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            color = if (enabled) EditorChromeMuted else EditorChromeMuted.copy(alpha = 0.42f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
