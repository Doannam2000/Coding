package com.natncompany.videoeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.natncompany.media.AssetType
import com.natncompany.media.TimelineClip
import com.natncompany.media.TimelineTrack
import com.natncompany.media.TrackType
import kotlin.math.roundToInt

@Composable
fun TrackRow(
    track: TimelineTrack,
    selectedClipId: String?,
    pixelsPerMs: Float,
    modifier: Modifier = Modifier,
    onClipSelected: (String) -> Unit,
    onClipDragged: (clip: TimelineClip, deltaMs: Long) -> Unit,
    onClipTrimStartDragged: (clip: TimelineClip, deltaMs: Long) -> Unit,
    onClipTrimEndDragged: (clip: TimelineClip, deltaMs: Long) -> Unit
) {
    val enabledAlpha = if (track.isEnabled) 1f else 0.42f
    val rowTint = track.type.trackTint()

    Box(
        modifier = modifier
            .background(rowTint.copy(alpha = 0.08f * enabledAlpha))
            .border(1.dp, rowTint.copy(alpha = if (track.isEnabled) 0.18f else 0.08f))
    ) {
        track.clips.forEach { clip ->
            ClipBlock(
                clip = clip,
                selected = clip.id == selectedClipId,
                enabled = track.isEnabled,
                muted = track.isMuted,
                pixelsPerMs = pixelsPerMs,
                modifier = Modifier.align(Alignment.TopStart),
                onSelected = { onClipSelected(clip.id) },
                onDragged = { onClipDragged(clip, it) },
                onTrimStartDragged = { onClipTrimStartDragged(clip, it) },
                onTrimEndDragged = { onClipTrimEndDragged(clip, it) }
            )
        }

        if (!track.isEnabled || track.isMuted) {
            Text(
                text = if (!track.isEnabled) "Disabled" else "Muted",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp),
                color = Color.White.copy(alpha = 0.38f),
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun ClipBlock(
    clip: TimelineClip,
    selected: Boolean,
    enabled: Boolean,
    muted: Boolean,
    pixelsPerMs: Float,
    modifier: Modifier = Modifier,
    onSelected: () -> Unit,
    onDragged: (deltaMs: Long) -> Unit,
    onTrimStartDragged: (deltaMs: Long) -> Unit,
    onTrimEndDragged: (deltaMs: Long) -> Unit
) {
    val startPx = remember(clip.timelineStartMs, pixelsPerMs) {
        (clip.timelineStartMs * pixelsPerMs).roundToInt()
    }
    val widthPx = remember(clip.visibleDurationMs, pixelsPerMs) {
        (clip.visibleDurationMs * pixelsPerMs).roundToInt().coerceAtLeast(72)
    }
    val blockAlpha = when {
        !enabled -> 0.45f
        muted -> 0.68f
        else -> 1f
    }
    val thumbCount = (widthPx / 24).coerceIn(3, 18)

    Box(
        modifier = modifier
            .height(48.dp)
            .width(widthPx.dp)
            .offset { IntOffset(startPx, 0) }
            .clip(RoundedCornerShape(12.dp))
            .background(clip.blockColor(selected).copy(alpha = blockAlpha))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color(0xFF5B8DEF) else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(clip.id, enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onTap = { onSelected() })
            }
            .pointerInput(clip.id, enabled, pixelsPerMs) {
                if (!enabled) return@pointerInput
                var draggedPx = 0f
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    draggedPx += dragAmount.x
                    onDragged((draggedPx / pixelsPerMs).toLong())
                }
            },
        contentAlignment = Alignment.Center
    ) {
        TrimHandle(
            modifier = Modifier.align(Alignment.CenterStart),
            enabled = enabled,
            pixelsPerMs = pixelsPerMs,
            onDrag = onTrimStartDragged
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(thumbCount) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = if (index % 2 == 0) 0.16f else 0.08f))
                    )
                }
            }
            Text(
                text = clip.metadata.label ?: clip.assetType.name,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall
            )
        }
        TrimHandle(
            modifier = Modifier.align(Alignment.CenterEnd),
            enabled = enabled,
            pixelsPerMs = pixelsPerMs,
            onDrag = onTrimEndDragged
        )
    }
}

@Composable
private fun TrimHandle(
    modifier: Modifier,
    enabled: Boolean,
    pixelsPerMs: Float,
    onDrag: (Long) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(24.dp)
            .background(Color.Black.copy(alpha = if (enabled) 0.38f else 0.14f))
            .pointerInput(enabled, pixelsPerMs) {
                if (!enabled) return@pointerInput

                var draggedPx = 0f
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    draggedPx += dragAmount.x
                    onDrag((draggedPx / pixelsPerMs).toLong())
                }
            }
    )
}

private fun TimelineClip.blockColor(selected: Boolean): Color = when {
    selected -> Color(0xFF5B8CFF)
    assetType == AssetType.Audio -> Color(0xFF5E4CB5)
    assetType == AssetType.Image -> Color(0xFF2C7A62)
    else -> Color(0xFF2B3445)
}

private fun TrackType.trackTint(): Color = when (this) {
    TrackType.Video -> Color(0xFF5B8CFF)
    TrackType.Audio -> Color(0xFF8E72FF)
    TrackType.Text -> Color(0xFFFFC857)
    TrackType.Sticker -> Color(0xFFFF7AB6)
    TrackType.Effect -> Color(0xFF56C271)
}
