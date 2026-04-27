package com.example.clipystudio.ui.main.editor.timeline

import kotlinx.coroutines.Job


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
fun TimelineView(timeline: Timeline, onSelect: (String) -> Unit, onSeek: (Long) -> Unit, onScroll: (Float, Float) -> Unit, onZoom: (Float, Float, Float) -> Unit, onTrim: (TrimHandle, Long) -> Unit, onMove: (Long) -> Unit, onSplit: () -> Unit, onReorder: (Int) -> Unit) {
  val projectTimeline = remember(timeline) { TimelineEngine.toProjectTimeline(timeline) }
  val pxPerSecond = timeline.pixelsPerSecond * timeline.zoomLevel
  val contentWidth = ((timeline.durationMs / 1_000f) * pxPerSecond).roundToInt().coerceAtLeast(640)
  val activeComposition = remember(timeline) { TimelineEngine.resolveActiveComposition(timeline) }
  val activeIds = remember(activeComposition) {
    buildSet {
      activeComposition.video?.let { add(it.clipId) }
      addAll(activeComposition.audio.map { it.clipId })
      addAll(activeComposition.text.map { it.clipId })
      addAll(activeComposition.stickers.map { it.clipId })
      addAll(activeComposition.overlays.map { it.clipId })
      addAll(activeComposition.effects.map { it.clipId })
    }
  }
  val selectedClip = timeline.findClip(timeline.selectedClipId)
  var viewportWidthPx by remember { mutableStateOf(TimelineEngine.DefaultViewportWidthPx) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val visibleRange = remember(timeline.scrollOffsetPx, timeline.zoomLevel, timeline.version) { TimelineEngine.visibleRange(timeline, viewportWidthPx) }
  val thumbnailRequests = remember(visibleRange, timeline.version) { TimelineEngine.planThumbnailRequests(timeline, visibleRange) }
  val thumbnailFrames by produceState(initialValue = emptyMap<String, Bitmap?>(), thumbnailRequests, context) {
    value = thumbnailRequests.associate { request -> request.clipId to context.loadThumbnailBitmap(request.mediaUri, request.thumbnailTimeMs, request.widthPx, request.heightPx) }
  }
  var gestureOverlay by remember { mutableStateOf(TimelineGestureOverlayState()) }
  var activePreview by remember { mutableStateOf<TimelineClipPreviewState?>(null) }
  var isEditGestureActive by remember { mutableStateOf(false) }
  var gestureTimecode by remember { mutableStateOf<String?>(null) }
  var flingNonce by remember { mutableLongStateOf(0L) }
  var flingJob by remember { mutableStateOf<Job?>(null) }
  var seekThrottle by remember { mutableStateOf(com.example.clipystudio.data.PreviewSeekThrottleState(minIntervalMs = 48L)) }
  val density = LocalDensity.current
  val densityValue = density.density
  val physics = remember(densityValue) {
    TimelineEngine.DefaultPhysics.copy(
      overscrollLimitPx = TimelineEngine.DefaultPhysics.overscrollLimitPx * densityValue,
      minFlingVelocityPxPerSec = TimelineEngine.DefaultPhysics.minFlingVelocityPxPerSec * densityValue,
      stopVelocityThresholdPxPerSec = TimelineEngine.DefaultPhysics.stopVelocityThresholdPxPerSec * densityValue,
      autoScrollEdgeZonePx = TimelineEngine.DefaultPhysics.autoScrollEdgeZonePx * densityValue,
      maxAutoScrollVelocityPxPerSec = TimelineEngine.DefaultPhysics.maxAutoScrollVelocityPxPerSec * densityValue,
    )
  }
  val snapConfig = remember(densityValue) {
    TimelineEngine.DefaultSnapConfig.copy(
      baseThresholdPx = TimelineEngine.DefaultSnapConfig.baseThresholdPx * densityValue,
      strongThresholdPx = TimelineEngine.DefaultSnapConfig.strongThresholdPx * densityValue,
      maxInfluencePx = TimelineEngine.DefaultSnapConfig.maxInfluencePx * densityValue
    )
  }
  val previewSeek: (Long, PreviewSeekSource, Boolean) -> Unit = { timeMs, source, forceFinal ->
    val decision = TimelineEngine.previewSeekDecision(seekThrottle, timeMs.coerceIn(0L, timeline.durationMs), System.currentTimeMillis(), source, forceFinal)
    seekThrottle = decision.state
    decision.seekTimeMs?.let(onSeek)
  }
  val cancelFling = {
    flingNonce += 1L
    flingJob?.cancel()
    flingJob = null
  }
  val touchSlopPx = with(density) { 8.dp.toPx() }
  val paddingLeftPx = with(density) { 66.dp.toPx() }
  val totalPaddingPx = with(density) { 74.dp.toPx() } // horizontal padding 8dp*2 + 58dp
  Box(
    Modifier
      .fillMaxWidth()
      .fillMaxHeight()
      .onSizeChanged { viewportWidthPx = (it.width.toFloat() - totalPaddingPx).coerceAtLeast(180f) }
      .background(EditorChromeSurfaceAlt)
      .then(
        if (isEditGestureActive) {
          Modifier
        } else {
          Modifier.pointerInput(timeline.id, timeline.version, viewportWidthPx, timeline.scrollOffsetPx, timeline.zoomLevel, flingNonce) {
            var offset = timeline.scrollOffsetPx
            var velocityPxPerSec = 0f
            var lastDragAtMs = 0L
            var acceptedDrag = false
            detectHorizontalDragGestures(
              onDragStart = { start ->
                val bounds = TimelineEngine.timelinePointerBounds(paddingLeftPx, 0f, viewportWidthPx + paddingLeftPx, size.height.toFloat(), start.x, start.y)
                acceptedDrag = bounds.shouldAcceptTimelineGesture
                if (!acceptedDrag) return@detectHorizontalDragGestures
                val interruption = TimelineEngine.interruptTimelineGesture(gestureOverlay.mode, start.x, timeline.scrollOffsetPx, timeline.playheadMs)
                cancelFling()
                offset = interruption.scrollOffsetAtTouchDownPx
                velocityPxPerSec = 0f
                lastDragAtMs = System.currentTimeMillis()
                gestureTimecode = timeline.playheadMs.asTimecode()
                if (timeline.isPlaying) onSeek(timeline.playheadMs)
                gestureOverlay = gestureOverlay.copy(mode = TimelineGestureMode.SCROLLING, snapLabel = null, snapTimeMs = null, zoomLabel = null)
              },
              onHorizontalDrag = { change, dragAmount ->
                if (!acceptedDrag) return@detectHorizontalDragGestures
                val update = TimelineEngine.dragTimeline(offset, dragAmount, timeline.durationMs, timeline.zoomLevel, timeline.pixelsPerSecond, viewportWidthPx, physics)
                offset = update.nextOffsetPx
                gestureTimecode = update.currentTimeMs.asTimecode()
                gestureOverlay = gestureOverlay.copy(resistanceFraction = update.resistanceFraction, snapLabel = null, snapTimeMs = null)
                onScroll(offset, viewportWidthPx)
                previewSeek(update.currentTimeMs, PreviewSeekSource.TIMELINE_SCROLL, false)
                val now = System.currentTimeMillis()
                val elapsed = (now - lastDragAtMs).coerceAtLeast(1L)
                velocityPxPerSec = TimelineEngine.updateDragVelocity(velocityPxPerSec, dragAmount, elapsed)
                lastDragAtMs = now
                change.consumePositionChange()
              },
              onDragEnd = {
                if (!acceptedDrag) return@detectHorizontalDragGestures
                val finalSeek = TimelineEngine.exactFrameSeekFromScroll(offset, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs, viewportWidthPx, TimelineGestureMode.SCROLLING, finalFrame = true)
                previewSeek(finalSeek.currentTimeMs, PreviewSeekSource.TIMELINE_SCROLL, true)
                val token = flingNonce + 1L
                flingNonce = token
                if (abs(velocityPxPerSec) > physics.minFlingVelocityPxPerSec) {
                  flingJob = scope.launch {
                    gestureOverlay = gestureOverlay.copy(mode = TimelineGestureMode.FLINGING)
                    var velocity = velocityPxPerSec
                    var flingOffset = offset
                    while (abs(velocity) > physics.stopVelocityThresholdPxPerSec && flingNonce == token) {
                      val frame = TimelineEngine.advanceFling(flingOffset, velocity, 16, timeline.durationMs, timeline.zoomLevel, timeline.pixelsPerSecond, viewportWidthPx, physics)
                      flingOffset = frame.nextOffsetPx
                      velocity = frame.nextVelocityPxPerSec
                      gestureTimecode = TimelineEngine.timeFromScroll(flingOffset, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs, viewportWidthPx).asTimecode()
                      gestureOverlay = gestureOverlay.copy(resistanceFraction = frame.resistanceFraction)
                      onScroll(flingOffset, viewportWidthPx)
                      previewSeek(TimelineEngine.timeFromScroll(flingOffset, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs, viewportWidthPx), PreviewSeekSource.TIMELINE_FLING, false)
                      if (frame.isFinished) break
                        delay(16)
                     }
                    if (flingNonce != token) return@launch
                    animateTimelineSettle(flingOffset, timeline, viewportWidthPx, onScroll, previewSeek) { resistance ->
                      gestureOverlay = gestureOverlay.copy(resistanceFraction = resistance)
                    }
                    gestureTimecode = null
                    gestureOverlay = gestureOverlay.copy(mode = TimelineGestureMode.IDLE, resistanceFraction = 0f)
                    flingJob = null
                  }
                } else {
                  flingJob = scope.launch {
                    gestureOverlay = gestureOverlay.copy(mode = TimelineGestureMode.IDLE)
                    animateTimelineSettle(offset, timeline, viewportWidthPx, onScroll, previewSeek) { resistance ->
                      gestureOverlay = gestureOverlay.copy(resistanceFraction = resistance)
                    }
                    gestureTimecode = null
                    flingJob = null
                  }
                }
              },
              onDragCancel = {
                if (!acceptedDrag) return@detectHorizontalDragGestures
                flingJob = scope.launch {
                  animateTimelineSettle(offset, timeline, viewportWidthPx, onScroll, previewSeek) { resistance ->
                    gestureOverlay = gestureOverlay.copy(resistanceFraction = resistance)
                  }
                  gestureTimecode = null
                  gestureOverlay = gestureOverlay.copy(mode = TimelineGestureMode.IDLE, resistanceFraction = 0f)
                  flingJob = null
                }
              },
            )
          }
        },
      ),
  ) {
    Canvas(Modifier.matchParentSize()) {
      val stepPx = 40.dp.toPx()
      var x = paddingLeftPx - (timeline.scrollOffsetPx % stepPx)
      while (x < size.width) {
        drawLine(Color(0xFF333333), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
        x += stepPx
      }
    }
    Column(
      Modifier
        .fillMaxSize()
        .padding(top = 4.dp, bottom = 4.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      TimelineHeader(
        timeline = timeline,
        contentWidth = contentWidth,
        viewportWidthPx = viewportWidthPx,
        onSeek = onSeek,
        onScroll = onScroll,
        onZoom = onZoom,
        onGestureZoomLabel = { gestureOverlay = gestureOverlay.copy(zoomLabel = it) },
        onGestureTimecode = { gestureTimecode = it },
        onTransformStart = {
          cancelFling()
          isEditGestureActive = false
          gestureTimecode = timeline.playheadMs.asTimecode()
          gestureOverlay = gestureOverlay.copy(mode = TimelineGestureMode.SCROLLING, snapLabel = null, snapTimeMs = null)
        },
        onTransformFrame = { resistanceFraction ->
          gestureOverlay = gestureOverlay.copy(resistanceFraction = resistanceFraction)
        },
        physics = physics
      )
      TimelineLaneStack(
        projectTimeline = projectTimeline,
        timeline = timeline,
        contentWidth = contentWidth,
        viewportWidthPx = viewportWidthPx,
        activeIds = activeIds,
        touchSlopPx = touchSlopPx,
        thumbnailFrames = thumbnailFrames,
        activePreview = activePreview,
        onSelect = onSelect,
        onTrim = onTrim,
        onMove = onMove,
        onSplit = onSplit,
        onReorder = onReorder,
        onPreview = { preview ->
          cancelFling()
          activePreview = preview
          isEditGestureActive = preview != null
          gestureOverlay = gestureOverlay.copy(
            snapLabel = preview?.snapLabel,
            snapTimeMs = preview?.snapTimeMs,
            mode = when {
              preview?.trimHandle != null -> TimelineGestureMode.TRIMMING_CLIP
              preview != null -> TimelineGestureMode.DRAGGING_CLIP
              else -> TimelineGestureMode.IDLE
            },
            invalidFeedback = preview?.isValid == false,
          )
        },
        onAutoScroll = { scrollDeltaPx, direction ->
          val pointerX = if (direction == com.example.clipystudio.data.AutoScrollDirection.LEFT) 0f else viewportWidthPx
          val update = TimelineEngine.advanceAutoScroll(
            timeline.scrollOffsetPx,
            TimelineEngine.resolveAutoScroll(pointerX, viewportWidthPx, timeline.selectedClipId, timeline.playheadMs, physics),
            16,
            timeline.durationMs,
            timeline.zoomLevel,
            timeline.pixelsPerSecond,
            viewportWidthPx,
          )
          gestureOverlay = gestureOverlay.copy(autoScrollDirection = direction, mode = TimelineGestureMode.DRAGGING_CLIP)
          onScroll(update.nextOffsetPx + scrollDeltaPx, viewportWidthPx)
        },
        onPreviewSeek = { previewTimeMs ->
          previewSeek(previewTimeMs, PreviewSeekSource.CLIP_TRIM_LEFT, false)
        },
        onPreviewEnd = {
          activePreview?.trimHandle?.let { handle ->
            val seekTime = if (handle == TrimHandle.Left) activePreview?.startTimeMs else activePreview?.let { it.startTimeMs + it.durationMs }
            seekTime?.let {
              previewSeek(
                it,
                if (handle == TrimHandle.Left) PreviewSeekSource.CLIP_TRIM_LEFT else PreviewSeekSource.CLIP_TRIM_RIGHT,
                true,
              )
            }
          }
          activePreview = null
          isEditGestureActive = false
          gestureTimecode = null
          gestureOverlay = gestureOverlay.copy(
            mode = TimelineGestureMode.IDLE,
            snapLabel = null,
            snapTimeMs = null,
            autoScrollDirection = com.example.clipystudio.data.AutoScrollDirection.NONE,
            invalidFeedback = false,
          )
        },
        physics = physics,
        snapConfig = snapConfig,
      )
    }
    TimelineGuides(timeline, contentWidth, gestureOverlay.snapTimeMs)
    EdgeResistanceMask(gestureOverlay.resistanceFraction)
    AutoScrollEdgeMask(gestureOverlay.autoScrollDirection)
    gestureOverlay.snapLabel?.let { Text(it, modifier = Modifier.align(Alignment.TopCenter).padding(top = 30.dp).clip(RoundedCornerShape(999.dp)).background(EditorChromeSurface.copy(alpha = 0.92f)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(999.dp)).padding(horizontal = 9.dp, vertical = 3.dp), color = EditorChromeAudioAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    TimelineGestureReadout(gestureTimecode ?: timeline.playheadMs.asTimecode(), gestureOverlay.zoomLabel, gestureOverlay.snapLabel, gestureOverlay.resistanceFraction)
    val playheadX = paddingLeftPx + viewportWidthPx / 2f
    Box(
      Modifier
        .offset { IntOffset(playheadX.roundToInt(), 0) }
        .width(1.dp)
        .fillMaxHeight()
        .background(Color.White)
        .zIndex(50f)
    )
    Box(
      Modifier
        .offset { IntOffset(playheadX.roundToInt() - 5, 0) }
        .size(width = 10.dp, height = 12.dp)
        .background(Color.White, shape = object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.6f)
                    lineTo(size.width / 2f, size.height)
                    lineTo(0f, size.height * 0.6f)
                    close()
                }
                return Outline.Generic(path)
            }
        })
        .zIndex(51f)
    )
  }
}

@Composable
private fun TimelineLaneStack(
  projectTimeline: com.example.clipystudio.data.ProjectTimeline,
  timeline: Timeline,
  contentWidth: Int,
  viewportWidthPx: Float,
  activeIds: Set<String>,
  touchSlopPx: Float,
  thumbnailFrames: Map<String, Bitmap?>,
  activePreview: TimelineClipPreviewState?,
  onSelect: (String) -> Unit,
  onTrim: (TrimHandle, Long) -> Unit,
  onMove: (Long) -> Unit,
  onSplit: () -> Unit,
  onReorder: (Int) -> Unit,
  onPreview: (TimelineClipPreviewState?) -> Unit,
  onAutoScroll: (Float, com.example.clipystudio.data.AutoScrollDirection) -> Unit,
  onPreviewSeek: (Long) -> Unit,
  onPreviewEnd: () -> Unit,
  physics: TimelinePhysicsConfig,
  snapConfig: TimelineSnapConfig,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 0.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    timeline.tracks.sortedBy { it.orderIndex }.forEach { track ->
      EngineTrackLane(
        projectTimeline = projectTimeline,
        timeline = timeline,
        track = track,
        contentWidth = contentWidth,
        viewportWidthPx = viewportWidthPx,
        activeIds = activeIds,
        touchSlopPx = touchSlopPx,
        thumbnailFrames = thumbnailFrames,
        onSelect = onSelect,
        onTrim = onTrim,
        onMove = onMove,
        onSplit = onSplit,
        onReorder = onReorder,
        activePreview = activePreview,
        onPreview = onPreview,
        onAutoScroll = onAutoScroll,
        onPreviewSeek = onPreviewSeek,
        onPreviewEnd = onPreviewEnd,
        physics = physics,
        snapConfig = snapConfig,
      )
    }
  }
}
