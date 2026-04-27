package com.example.clipystudio.ui.main.editor.timeline

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.BoxScope

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clipystudio.data.*
import com.example.clipystudio.filter.*
import com.example.clipystudio.theme.*
import com.example.clipystudio.*
import com.example.clipystudio.ui.main.*
import com.example.clipystudio.ui.main.models.*
import com.example.clipystudio.ui.main.screens.*
import com.example.clipystudio.ui.main.editor.*
import com.example.clipystudio.ui.main.editor.components.*
import com.example.clipystudio.ui.main.editor.panels.*
import com.example.clipystudio.ui.main.editor.timeline.*
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.*

@Composable
fun EngineClipBlock(trackType: TrackType, clip: TimelineClip, index: Int, selected: Boolean, active: Boolean, zoom: Float, pixelsPerSecond: Float, scrollOffsetPx: Float, transition: com.example.clipystudio.data.Transition?, timeline: Timeline, viewportWidthPx: Float, touchSlopPx: Float, thumbnailBitmap: Bitmap?, preview: TimelineClipPreviewState?, onSelect: (String) -> Unit, onTrim: (TrimHandle, Long) -> Unit, onMove: (Long) -> Unit, onSplit: () -> Unit, onReorder: (Int) -> Unit, onPreview: (TimelineClipPreviewState?) -> Unit, onPreviewEnd: () -> Unit, onAutoScroll: (Float, com.example.clipystudio.data.AutoScrollDirection) -> Unit, onPreviewSeek: (Long) -> Unit, physics: TimelinePhysicsConfig = TimelineEngine.DefaultPhysics, snapConfig: TimelineSnapConfig = TimelineEngine.DefaultSnapConfig) {
  val color = when (trackType) { TrackType.Video -> EditorChromeSurface; TrackType.Audio -> EditorChromeAudio; TrackType.Text -> EditorChromePrimary.copy(alpha = 0.30f); TrackType.Sticker -> EditorChromeAudioAccent.copy(alpha = 0.24f); TrackType.Effect -> EditorChromePrimary.copy(alpha = 0.18f); TrackType.Overlay -> EditorChromePrimary.copy(alpha = 0.24f) }
  val selectedOutline by animateFloatAsState(if (selected) 1f else 0f, tween(140), label = "selectedOutline")
  val liftFraction by animateFloatAsState(if (preview != null) 1f else 0f, tween(120), label = "clipLift")
  var longPressReordering by remember { mutableStateOf(false) }
  val density = LocalDensity.current
  val reorderLift by animateFloatAsState(if (longPressReordering) 1f else 0f, tween(110), label = "reorderLift")
  val visualLift = max(liftFraction, reorderLift)
  val displayStartMs = preview?.startTimeMs ?: clip.startMs
  val displayDurationMs = preview?.durationMs ?: clip.durationMs
  val left = ((displayStartMs / 1_000f) * pixelsPerSecond * zoom - scrollOffsetPx).roundToInt()
  val width = ((displayDurationMs / 1_000f) * pixelsPerSecond * zoom).roundToInt().coerceAtLeast(44)
  val widthDp = with(density) { width.toDp() }
  val pxPerMs = TimelineEngine.pixelsPerMs(zoom, pixelsPerSecond)
  val isPreviewing = preview != null
  val visualState = when {
    clip.isVisualMediaClip() && !clip.hasUsableMediaUri() -> ClipVisualState.Invalid
    selected || isPreviewing || longPressReordering -> ClipVisualState.Selected
    active -> ClipVisualState.Active
    else -> ClipVisualState.Inactive
  }
  val backgroundColor = when (trackType) {
    TrackType.Video -> Color(0xFF2A2A2A)
    TrackType.Audio -> Color(0xFF1E3A5F)
    TrackType.Text -> Color(0xFF8E44AD)
    else -> Color(0xFF1A1A1A)
  }
  val outlineColor = when {
    visualState == ClipVisualState.Invalid -> StudioDanger
    selected || isPreviewing || longPressReordering -> Color.White
    active -> Color.White.copy(alpha = 0.34f)
    else -> Color.White.copy(alpha = 0.1f)
  }
  Box(
    Modifier.offset { IntOffset(left, (-2 * visualLift).roundToInt()) }.graphicsLayer { scaleX = 1f + visualLift * 0.02f; scaleY = 1f + visualLift * 0.04f; shadowElevation = visualLift * 10f }.width(widthDp).fillMaxHeight().padding(vertical = 2.dp).clip(RoundedCornerShape(if (trackType == TrackType.Video) 4.dp else 3.dp)).background(backgroundColor).border(if (selected || isPreviewing || longPressReordering || visualState == ClipVisualState.Invalid) 2.dp else 1.dp, outlineColor, RoundedCornerShape(if (trackType == TrackType.Video) 4.dp else 3.dp)).clickable { onSelect(clip.id) }.pointerInput(clip.id, selected) {
      detectTapGestures(onDoubleTap = { onSelect(clip.id); onSplit() }, onLongPress = { onSelect(clip.id); longPressReordering = true })
    }.pointerInput(clip.id, trackType, timeline.version) {
      var dragPx = 0f
      detectDragGesturesAfterLongPress(
        onDragStart = { dragPx = 0f; longPressReordering = true; onSelect(clip.id); onPreview(TimelineClipPreviewState(clip.id, clip.startMs, clip.durationMs)) },
        onDrag = { change, dragAmount ->
          dragPx += dragAmount.x
          val targetIndex = (index + (dragPx / width.coerceAtLeast(1)).roundToInt()).coerceAtLeast(0)
          if (trackType == TrackType.Video) onPreview(TimelineClipPreviewState(clip.id, clip.startMs, clip.durationMs, snapLabel = "Reorder ${targetIndex + 1}"))
          change.consume()
        },
        onDragEnd = {
          val targetIndex = (index + (dragPx / width.coerceAtLeast(1)).roundToInt()).coerceAtLeast(0)
          if (trackType == TrackType.Video) onReorder(targetIndex)
          longPressReordering = false
          onPreviewEnd()
        },
        onDragCancel = { longPressReordering = false; onPreviewEnd() },
      )
    }.pointerInput(clip.id, timeline.version, zoom, scrollOffsetPx, viewportWidthPx) {
      var accumulatedDragPx = 0f
      var previewState: TimelineClipPreviewState? = null
      var lastValidStartMs = clip.startMs
      detectHorizontalDragGestures(
        onDragStart = {
          accumulatedDragPx = 0f
          lastValidStartMs = clip.startMs
          onSelect(clip.id)
          previewState = null
        },
        onHorizontalDrag = { change, dragAmount ->
          accumulatedDragPx += dragAmount
          val slop = TimelineEngine.touchSlopGate(0f, 0f, accumulatedDragPx, 0f, touchSlopPx, TimelineGestureMode.DRAGGING_CLIP)
          if (!slop.hasExceededTouchSlop) return@detectHorizontalDragGestures
          val deltaMs = (accumulatedDragPx / pxPerMs).roundToLong()
          val proposedStartMs = clip.startMs + deltaMs
          val boundary = TimelineEngine.resolveClipBoundaryState(timeline, clip.id, proposedStartMs, lastValidStartMs, timeline.pixelsPerSecond, timeline.zoomLevel, physics)
          val autoScroll = TimelineEngine.resolveAutoScroll(change.position.x, viewportWidthPx, clip.id, proposedStartMs, physics)
          if (autoScroll.isAutoScrolling) onAutoScroll(0f, autoScroll.direction)
          val resolution = TimelineEngine.resolveDraggedClip(timeline, clip.id, proposedStartMs, snapConfig)
          if (resolution.isValid) lastValidStartMs = resolution.resolvedStartTimeMs
          previewState = TimelineClipPreviewState(
            clipId = clip.id,
            startTimeMs = if (boundary.isBeyondStart || boundary.isBeyondEnd) (resolution.resolvedStartTimeMs + (boundary.resistanceOffsetPx / pxPerMs).roundToLong()).coerceAtLeast(0L) else resolution.resolvedStartTimeMs,
            durationMs = clip.durationMs,
            snapLabel = resolution.snapResolution.target?.label,
            isValid = resolution.isValid,
            snapTimeMs = resolution.snapResolution.snappedTimeMs,
          )
          onPreview(previewState)
          change.consume()
        },
        onDragEnd = {
          val finalPreview = previewState
          if (finalPreview != null && finalPreview.isValid) onMove(finalPreview.startTimeMs - clip.startMs) else if (finalPreview != null) onPreview(TimelineClipPreviewState(clip.id, lastValidStartMs, clip.durationMs, snapLabel = "Invalid", isValid = false))
          previewState = null
          onPreviewEnd()
        },
        onDragCancel = {
          previewState = null
          onPreviewEnd()
        },
      )
    }.semantics { contentDescription = "${clip.clipType} clip, ${trackType.label} track, starts at ${clip.startMs.asTimecode()}, duration ${clip.durationMs.asTimecode()}" },
    contentAlignment = Alignment.Center,
  ) {
    if (selected && trackType == TrackType.Video) {
      Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().width(3.dp).background(EditorChromePrimary))
    }
    val shouldRenderBitmapThumbnail = clip.clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Overlay)
    if (shouldRenderBitmapThumbnail && thumbnailBitmap != null) {
      Image(bitmap = thumbnailBitmap.asImageBitmap(), contentDescription = "${clip.title} thumbnail", modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
    } else if (shouldRenderBitmapThumbnail || trackType == TrackType.Audio) {
      Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.16f))))) {
        Row(Modifier.matchParentSize().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
          repeat((width / 44).coerceIn(1, 8)) { Box(Modifier.weight(1f).height(if (trackType == TrackType.Audio) 22.dp else 18.dp).clip(RoundedCornerShape(6.dp)).background(if (trackType == TrackType.Audio) Brush.verticalGradient(listOf(EditorChromeAudioAccent.copy(alpha = 0.50f), EditorChromeAudioAccent.copy(alpha = 0.18f))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.10f))))) }
        }
        Text(clipTypeBadge(clip.clipType), modifier = Modifier.align(Alignment.TopStart).padding(4.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
      if (shouldRenderBitmapThumbnail) {
        Text(
          if (!clip.hasUsableMediaUri()) "Invalid media" else "Thumbnail unavailable",
          modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 4.dp),
          color = Color.White.copy(alpha = 0.92f),
          fontSize = 9.sp,
          fontWeight = FontWeight.Medium,
        )
      }
    }
    }
    if (active && trackType != TrackType.Audio) Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.30f)))
    transition?.let { Box(Modifier.align(if (it.fromClipId == clip.id) Alignment.CenterEnd else Alignment.CenterStart).width(18.dp).fillMaxHeight().background(EditorChromeAudioAccent.copy(alpha = 0.22f))) }
    clip.keyframes.distinctBy { it.timeMs }.forEach { keyframe ->
      val kx = ((keyframe.timeMs.toFloat() / clip.durationMs.coerceAtLeast(1L)) * width).roundToInt().coerceIn(8, width - 8)
      Box(Modifier.offset { IntOffset(kx - 4, 6) }.size(8.dp).background(StudioAccent, RoundedCornerShape(2.dp)))
    }
    Text(clip.title.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = if (trackType == TrackType.Video) 9.sp else 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = if (trackType == TrackType.Video) 10.dp else 6.dp), color = if (trackType == TrackType.Audio) EditorChromeAudioAccent else if (selected) Color.White else Color.White.copy(alpha = 0.96f))
    if (trackType == TrackType.Video && clip.clipType == ClipType.Video && clip.sourceDurationMs != null) {
      Text(
        clip.sourceDurationMs.asTimecode(),
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 4.dp).clip(RoundedCornerShape(999.dp)).background(StudioBackground.copy(alpha = 0.72f)).padding(horizontal = 6.dp, vertical = 2.dp),
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
      )
    }
    if (trackType == TrackType.Video && clip.clipType == ClipType.Image) {
      Text(
        "${(displayDurationMs / 1000f).let { "%.1fs".format(it) }}",
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 4.dp).clip(RoundedCornerShape(999.dp)).background(StudioBackground.copy(alpha = 0.72f)).padding(horizontal = 6.dp, vertical = 2.dp),
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
      )
    }
    if (selected) {
      TrimHandleGrip(Modifier.align(Alignment.CenterStart), color, TrimHandle.Left, clip, timeline, zoom, pixelsPerSecond, touchSlopPx, preview, onTrim, onPreview, onPreviewEnd, onPreviewSeek, snapConfig)
      TrimHandleGrip(Modifier.align(Alignment.CenterEnd), color, TrimHandle.Right, clip, timeline, zoom, pixelsPerSecond, touchSlopPx, preview, onTrim, onPreview, onPreviewEnd, onPreviewSeek, snapConfig)
    }
    if (preview?.snapLabel != null) {
      Text(preview.snapLabel, modifier = Modifier.align(Alignment.TopCenter).padding(top = 3.dp).clip(RoundedCornerShape(999.dp)).background(StudioBackground.copy(alpha = 0.82f)).padding(horizontal = 7.dp, vertical = 2.dp), color = StudioAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
  }
}


@Composable
fun TrimHandleGrip(modifier: Modifier, color: Color, handle: TrimHandle, clip: TimelineClip, timeline: Timeline, zoom: Float, pixelsPerSecond: Float, touchSlopPx: Float, preview: TimelineClipPreviewState?, onTrim: (TrimHandle, Long) -> Unit, onPreview: (TimelineClipPreviewState?) -> Unit, onPreviewEnd: () -> Unit, onPreviewSeek: (Long) -> Unit, snapConfig: TimelineSnapConfig = TimelineEngine.DefaultSnapConfig) {
  val pxPerMs = TimelineEngine.pixelsPerMs(zoom, pixelsPerSecond)
  Box(
    modifier.requiredWidth(12.dp).fillMaxHeight().background(Color.White).pointerInput(clip.id, handle, timeline.version, zoom) {
      var accumulatedDragPx = 0f
      var previewState: TimelineClipPreviewState? = null
      var slopExceeded = false
      detectHorizontalDragGestures(
        onDragStart = {
          accumulatedDragPx = 0f
          slopExceeded = false
        },
        onHorizontalDrag = { change, dragAmount ->
          accumulatedDragPx += dragAmount
          val slop = TimelineEngine.touchSlopGate(0f, 0f, accumulatedDragPx, 0f, touchSlopPx, TimelineGestureMode.TRIMMING_CLIP)
          if (!slop.hasExceededTouchSlop) return@detectHorizontalDragGestures
          if (!slopExceeded) {
            slopExceeded = true
            previewState = TimelineClipPreviewState(clip.id, clip.startMs, clip.durationMs, trimHandle = handle)
            onPreview(previewState)
          }
          val deltaMs = (accumulatedDragPx / pxPerMs).roundToLong()
          val proposedTime = if (handle == TrimHandle.Left) clip.startMs + deltaMs else clip.startMs + clip.durationMs + deltaMs
          val resolution = TimelineEngine.resolveTrimGesture(timeline, clip.id, handle, proposedTime, snapConfig)
          val scrub = TimelineEngine.resolveTrimPreviewScrub(timeline, clip.id, handle, proposedTime, snapConfig)
          val nextStart = if (handle == TrimHandle.Left) resolution.resolvedTimeMs else clip.startMs
          val nextEnd = if (handle == TrimHandle.Right) resolution.resolvedTimeMs else clip.startMs + clip.durationMs
          previewState = TimelineClipPreviewState(
            clipId = clip.id,
            startTimeMs = nextStart,
            durationMs = (nextEnd - nextStart).coerceAtLeast(TimelineEngine.MinClipDurationMs),
            snapLabel = resolution.snapResolution.target?.label,
            isValid = resolution.isValid,
            trimHandle = handle,
            snapTimeMs = resolution.snapResolution.snappedTimeMs,
          )
          onPreview(previewState)
          onPreviewSeek(scrub.previewTimeMs)
          change.consume()
        },
        onDragEnd = {
          val state = previewState?.takeIf { it.trimHandle == handle }
          if (state != null) {
            val deltaMs = if (handle == TrimHandle.Left) state.startTimeMs - clip.startMs else (state.startTimeMs + state.durationMs) - (clip.startMs + clip.durationMs)
            onTrim(handle, deltaMs)
            onPreviewSeek(if (handle == TrimHandle.Left) state.startTimeMs else state.startTimeMs + state.durationMs)
          }
          previewState = null
          onPreviewEnd()
        },
        onDragCancel = {
          previewState = null
          onPreviewEnd()
        },
      )
    }.semantics { contentDescription = if (handle == TrimHandle.Left) "Trim left edge" else "Trim right edge" },
  ) { }
}
