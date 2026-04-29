package com.natncompany.videoeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.natncompany.media.Timeline
import com.natncompany.media.TimelineTrack
import com.natncompany.media.TrackType
import kotlin.math.roundToInt

data class ReusableEditorTool(
    val id: String,
    val label: String,
    val badge: String
)

@Composable
fun VideoEditorToolBar(
    tools: List<ReusableEditorTool>,
    selectedToolId: String?,
    onToolSelected: (ReusableEditorTool) -> Unit,
    modifier: Modifier = Modifier,
    colors: VideoEditorToolBarColors = VideoEditorToolBarDefaults.colors()
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(VideoEditorToolBarDefaults.Height)
            .background(colors.containerColor),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tools, key = { it.id }) { tool ->
            VideoEditorToolBarItem(
                tool = tool,
                isSelected = tool.id == selectedToolId,
                colors = colors,
                onClick = { onToolSelected(tool) }
            )
        }
    }
}

@Composable
private fun VideoEditorToolBarItem(
    tool: ReusableEditorTool,
    isSelected: Boolean,
    colors: VideoEditorToolBarColors,
    onClick: () -> Unit
) {
    val badgeColor = if (isSelected) colors.selectedBadgeColor else colors.unselectedBadgeColor
    val textColor = if (isSelected) colors.selectedTextColor else colors.unselectedTextColor

    Column(
        modifier = Modifier
            .widthIn(min = 56.dp)
            .height(VideoEditorToolBarDefaults.Height)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tool.badge,
                color = colors.badgeTextColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = tool.label,
            color = textColor,
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

data class VideoEditorToolBarColors(
    val containerColor: Color,
    val selectedBadgeColor: Color,
    val unselectedBadgeColor: Color,
    val selectedTextColor: Color,
    val unselectedTextColor: Color,
    val badgeTextColor: Color
)

object VideoEditorToolBarDefaults {
    val Height = 50.dp

    fun colors(
        containerColor: Color = Color(0xFF373B3D),
        selectedBadgeColor: Color = Color(0xFF4A90E2),
        unselectedBadgeColor: Color = Color(0xFF4D4F51),
        selectedTextColor: Color = Color.White,
        unselectedTextColor: Color = Color(0xFF999CB0),
        badgeTextColor: Color = Color.White
    ): VideoEditorToolBarColors {
        return VideoEditorToolBarColors(
            containerColor = containerColor,
            selectedBadgeColor = selectedBadgeColor,
            unselectedBadgeColor = unselectedBadgeColor,
            selectedTextColor = selectedTextColor,
            unselectedTextColor = unselectedTextColor,
            badgeTextColor = badgeTextColor
        )
    }
}

@Composable
fun VideoEditorTimelineStrip(
    timeline: Timeline,
    selectedClipId: String?,
    durationMs: Long,
    positionMs: Long,
    onClipSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    zoom: Float = 1f,
    onClipMoved: (trackId: String, clipId: String, newStartMs: Long) -> Unit = { _, _, _ -> },
    onClipTrimStart: (trackId: String, clipId: String, newSourceStartMs: Long) -> Unit = { _, _, _ -> },
    onClipTrimEnd: (trackId: String, clipId: String, newSourceEndMs: Long) -> Unit = { _, _, _ -> }
) {
    val timelineDuration = durationMs.coerceAtLeast(1L)
    val scrollState = rememberScrollState()
    val pixelsPerMs = 0.052f * zoom
    val timelineWidth = ((timelineDuration * pixelsPerMs).roundToInt().coerceAtLeast(640)).dp
    val tracks = remember(timeline.tracks) {
        timeline.tracks.ifEmpty { listOf(TimelineTrack(id = "empty", type = TrackType.Video)) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(((tracks.size.coerceAtLeast(1) * 64) + 28).dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF101217)),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .width(76.dp)
                .fillMaxHeight()
                .padding(vertical = 12.dp)
        ) {
            tracks.forEach { track ->
                VideoEditorTrackLabel(track = track, modifier = Modifier.height(64.dp))
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .width(timelineWidth)
                    .fillMaxHeight()
                    .clickable { onClipSelected(null) }
            ) {
                if (timeline.tracks.all { it.clips.isEmpty() }) {
                    Text(
                        text = "Import media to build your timeline",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                tracks.forEachIndexed { index, track ->
                    TrackRow(
                        track = track,
                        selectedClipId = selectedClipId,
                        pixelsPerMs = pixelsPerMs,
                        modifier = Modifier
                            .padding(top = (12 + index * 64).dp)
                            .height(52.dp)
                            .fillMaxWidth(),
                        onClipSelected = onClipSelected,
                        onClipDragged = { clip, deltaMs ->
                            onClipMoved(track.id, clip.id, (clip.timelineStartMs + deltaMs).coerceAtLeast(0L))
                        },
                        onClipTrimStartDragged = { clip, deltaMs ->
                            val newStart = (clip.sourceStartMs + deltaMs).coerceIn(0L, clip.sourceEndMs - 100L)
                            onClipTrimStart(track.id, clip.id, newStart)
                        },
                        onClipTrimEndDragged = { clip, deltaMs ->
                            val newEnd = (clip.sourceEndMs + deltaMs).coerceIn(clip.sourceStartMs + 100L, clip.sourceDurationMs)
                            onClipTrimEnd(track.id, clip.id, newEnd)
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset((positionMs * pixelsPerMs).roundToInt(), 0) }
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFFF4D6D))
                )
            }
        }
    }
}

@Composable
private fun VideoEditorTrackLabel(track: TimelineTrack, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = track.type.name,
            color = Color.White.copy(alpha = if (track.isEnabled) 0.76f else 0.36f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
