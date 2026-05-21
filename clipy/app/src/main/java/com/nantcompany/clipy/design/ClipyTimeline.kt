package com.nantcompany.clipy.design

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMicros
import com.nantcompany.clipy.export.model.TextLayer
import com.nantcompany.clipy.export.model.VideoClip
import com.nantcompany.clipy.theme.ClipyDesignTokens
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ClipyTimeline(
    clips: List<VideoClip>,
    durationMs: Long,
    currentMs: Long,
    textLayers: List<TextLayer> = emptyList(),
    selectedLayerId: String? = null,
    selectedClipId: String? = null,
    onSeek: (Long) -> Unit,
    onLayerClick: (String) -> Unit = {},
    onClipClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var widthPx by remember { mutableFloatStateOf(0f) }

    fun performSeek(newMs: Long) {
        var finalMs = newMs.coerceIn(0, durationMs)
        if (abs(finalMs - 0) < 300) finalMs = 0
        if (abs(finalMs - durationMs) < 300) finalMs = durationMs
        
        // Magnetic Snap to Clip boundaries
        var currentOffset = 0L
        clips.forEach { clip ->
            val clipLen = clip.endMs - clip.startMs
            if (abs(finalMs - currentOffset) < 300) finalMs = currentOffset
            currentOffset += clipLen
            if (abs(finalMs - currentOffset) < 300) finalMs = currentOffset
        }

        if (finalMs != currentMs) {
            if (finalMs != newMs) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onSeek(finalMs)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).onSizeChanged { widthPx = it.width.toFloat() }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                
                // 1. TEXT TRACKS
                textLayers.forEach { layer ->
                    TextTrackItem(layer, durationMs, widthPx, layer.id == selectedLayerId) { onLayerClick(layer.id) }
                }

                // 2. VIDEO TRACK (MULTI-CLIP)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A))
                        .onSizeChanged { widthPx = it.width.toFloat() }
                        .pointerInput(durationMs, widthPx) {
                            detectDragGestures(
                                onDragStart = { performSeek(((it.x / widthPx) * durationMs).toLong()) },
                                onDrag = { change, _ -> performSeek(((change.position.x / widthPx) * durationMs).toLong()) }
                            )
                        }
                ) {
                    clips.forEach { clip ->
                        val clipLen = clip.endMs - clip.startMs
                        val weight = if (durationMs > 0) clipLen.toFloat() / durationMs.toFloat() else 1f
                        
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .fillMaxHeight()
                                .border(if (clip.id == selectedClipId) 2.dp else 0.5.dp, if (clip.id == selectedClipId) ClipyDesignTokens.primaryAccent else Color.White.copy(alpha = 0.1f))
                                .clickable { onClipClick(clip.id) }
                        ) {
                            val thumbnailCount = remember(clipLen, widthPx, durationMs) {
                                if (widthPx > 0 && durationMs > 0) ((clipLen.toFloat() / durationMs.toFloat() * widthPx) / 60f).toInt().coerceAtLeast(1) else 1
                            }
                            
                            Row(modifier = Modifier.fillMaxSize()) {
                                repeat(thumbnailCount) { i ->
                                    val timeMs = clip.startMs + (i * clipLen / thumbnailCount)
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(clip.path)
                                            .decoderFactory(VideoFrameDecoder.Factory())
                                            .videoFrameMicros(timeMs * 1000L)
                                            .size(100)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        contentScale = ContentScale.Crop,
                                        alpha = 0.6f
                                    )
                                }
                            }

                            if (clip.transition != "none") {
                                Box(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(4.dp).background(ClipyDesignTokens.secondaryAccent))
                            }
                        }
                    }
                }
            }

            // 3. PLAYHEAD
            if (durationMs > 0 && widthPx > 0) {
                val offsetPx = (currentMs.toFloat() / durationMs.toFloat()) * widthPx
                Box(modifier = Modifier.offset { IntOffset(offsetPx.roundToInt() - 1.dp.toPx().toInt(), 0) }.fillMaxHeight().width(2.dp).background(ClipyDesignTokens.primaryAccent))
                Box(modifier = Modifier.offset { IntOffset(offsetPx.roundToInt() - 6.dp.toPx().toInt(), 0) }.size(12.dp).background(ClipyDesignTokens.primaryAccent, CircleShape).border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTimeCompact(currentMs), color = ClipyDesignTokens.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(formatTimeCompact(durationMs), color = ClipyDesignTokens.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TextTrackItem(layer: TextLayer, total: Long, width: Float, selected: Boolean, onClick: () -> Unit) {
    if (total <= 0 || width <= 0) return
    val startR = layer.startMs.toFloat() / total.toFloat()
    val endR = (if (layer.endMs == Long.MAX_VALUE) total else layer.endMs).toFloat() / total.toFloat()
    
    Box(modifier = Modifier.fillMaxWidth().height(22.dp)) {
        Box(
            modifier = Modifier.offset { IntOffset((startR * width).roundToInt(), 0) }.width(((endR - startR) * width / 2).dp).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(if (selected) ClipyDesignTokens.primaryAccent else Color(0xFF4C1D95).copy(alpha = 0.8f)).border(if (selected) 2.dp else 0.dp, Color.White, RoundedCornerShape(6.dp)).clickable { onClick() }.padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(layer.text, color = if (selected) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

private fun formatTimeCompact(ms: Long): String {
    val s = (ms / 1000) % 60; val m = (ms / 60000)
    return "%02d:%02d".format(m, s)
}
