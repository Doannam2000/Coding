package com.example.clipystudio.ui.main.editor.components

import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.PlaybackException
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView


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
import androidx.compose.ui.graphics.asImageBitmap
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
fun EditorPreviewSection(
  modifier: Modifier,
  ratio: CanvasRatio,
  timeline: Timeline,
  onSelect: (String) -> Unit,
  onClearSelection: () -> Unit,
  onDelete: () -> Unit,
  onTransform: (Float, Float, Float, Float) -> Unit,
  onEditText: (EditorTool) -> Unit,
  onRatio: (CanvasRatio) -> Unit,
  onSeek: (Long) -> Unit,
) {
  val selectedClip = timeline.findClip(timeline.selectedClipId)
  val composition = remember(timeline) { TimelineEngine.resolveActiveComposition(timeline) }
  val allClips = remember(timeline) { timeline.tracks.flatMap { it.clips } }
  val activeIds = remember(composition) {
    buildSet {
      composition.video?.let { add(it.clipId) }
      addAll(composition.audio.map { it.clipId })
      addAll(composition.text.map { it.clipId })
      addAll(composition.stickers.map { it.clipId })
      addAll(composition.overlays.map { it.clipId })
      addAll(composition.effects.map { it.clipId })
    }
  }
  val activeLayers = remember(allClips, activeIds) { allClips.filter { it.id in activeIds } }
  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(Color.Black)
      .padding(horizontal = 18.dp, vertical = 18.dp),
  ) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      PreviewStageHeader(
        ratio = ratio,
        timeline = timeline,
        selectedClip = selectedClip,
        activeLayers = activeLayers,
      )
      Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        PreviewCanvas(ratio, timeline, onSelect, onClearSelection, onDelete, onTransform, onEditText, onRatio, onSeek)
      }
      PreviewLayerStrip(
        selectedClipId = timeline.selectedClipId,
        activeLayers = activeLayers,
        onSelect = onSelect,
      )
    }
  }
}

@Composable
private fun PreviewStageHeader(
  ratio: CanvasRatio,
  timeline: Timeline,
  selectedClip: TimelineClip?,
  activeLayers: List<TimelineClip>,
) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text("Canvas ${ratio.label}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
      Text(
        selectedClip?.title ?: if (activeLayers.isEmpty()) "No active layers at the current playhead" else "${activeLayers.size} active layers at ${timeline.playheadMs.asTimecode()}",
        color = EditorChromeMuted,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Surface(
      shape = RoundedCornerShape(999.dp),
      color = EditorChromeSurfaceLow,
      border = androidx.compose.foundation.BorderStroke(1.dp, EditorChromeBorder),
    ) {
      Text(
        if (timeline.isPlaying) "LIVE" else "PAUSED",
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        color = if (timeline.isPlaying) StudioSecondary else EditorChromeMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
      )
    }
  }
}

@Composable
private fun PreviewLayerStrip(
  selectedClipId: String?,
  activeLayers: List<TimelineClip>,
  onSelect: (String) -> Unit,
) {
  if (activeLayers.isEmpty()) return
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    activeLayers.sortedBy { it.startMs }.forEach { clip ->
      val selected = clip.id == selectedClipId
      Surface(
        onClick = { onSelect(clip.id) },
        shape = RoundedCornerShape(999.dp),
        color = if (selected) EditorChromePrimary.copy(alpha = 0.22f) else EditorChromeSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) EditorChromePrimary else EditorChromeBorder),
      ) {
        Text(
          "${clipTypeBadge(clip.clipType)} ${clip.title.take(18)}",
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
          color = if (selected) Color.White else EditorChromeMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
        )
      }
    }
  }
}


@Composable
fun PreviewCanvas(ratio: CanvasRatio, timeline: Timeline, onSelect: (String) -> Unit, onClearSelection: () -> Unit, onDelete: () -> Unit, onTransform: (Float, Float, Float, Float) -> Unit, onEditText: (EditorTool) -> Unit, onRatio: (CanvasRatio) -> Unit, onSeek: (Long) -> Unit) {
  val ratioValue = when (ratio) { CanvasRatio.Portrait -> 9f / 16f; CanvasRatio.Square -> 1f; CanvasRatio.Landscape -> 16f / 9f; CanvasRatio.FourFive -> 4f / 5f; CanvasRatio.Original -> 3f / 4f }
  val glow by animateFloatAsState(if (timeline.isPlaying) 1f else 0.35f, label = "previewGlow")
  val composition = remember(timeline) { TimelineEngine.resolveActiveComposition(timeline) }
  val activeIds = buildSet { composition.video?.let { add(it.clipId) }; addAll(composition.audio.map { it.clipId }); addAll(composition.text.map { it.clipId }); addAll(composition.stickers.map { it.clipId }); addAll(composition.overlays.map { it.clipId }); addAll(composition.effects.map { it.clipId }) }
  val allClips = timeline.tracks.flatMap { it.clips }
  val activeClips = allClips.filter { it.id in activeIds }.sortedBy { it.zIndex }
  val selectedClip = timeline.findClip(timeline.selectedClipId)
  val selectedVisualClip = selectedClip?.takeIf { it.clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Overlay) }
  val primaryVisualClip = selectedVisualClip ?: timeline.activePreviewClip()?.takeIf { it.clipType in setOf(ClipType.Image, ClipType.Video, ClipType.Overlay) }
  val context = LocalContext.current
  val previewState = remember(primaryVisualClip?.id, primaryVisualClip?.mediaUri, primaryVisualClip?.clipType) { context.resolvePreviewSurfaceState(primaryVisualClip) }
  var feedback by remember { mutableStateOf(PreviewGestureFeedback()) }
  val haptic = LocalHapticFeedback.current
  val density = LocalDensity.current
  val touchSlopPx = with(density) { 8.dp.toPx() }
  LaunchedEffect(feedback.pendingHaptic) {
    val event = feedback.pendingHaptic ?: return@LaunchedEffect
    haptic.performHapticFeedback(if (event == HapticEvent.INVALID_ACTION) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
    feedback = feedback.copy(pendingHaptic = null)
  }
  BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    val previewHeight = maxHeight.coerceAtLeast(180.dp)
    Box(
      Modifier
        .fillMaxHeight(0.92f)
        .height(previewHeight)
        .aspectRatio(ratioValue)
        .clip(RoundedCornerShape(14.dp))
        .background(Color.Black)
        .border(1.dp, EditorChromeBorder, RoundedCornerShape(14.dp)),
      contentAlignment = Alignment.Center,
    ) {
      val backgroundColor = runCatching { Color(android.graphics.Color.parseColor(timeline.canvasBackground.color)) }.getOrDefault(StudioBackground)
      var previewWidthPx by remember { mutableStateOf(1f) }
      var previewHeightPx by remember { mutableStateOf(1f) }
      Box(Modifier.fillMaxSize().background(if (timeline.canvasBackground.blurEnabled) Brush.radialGradient(listOf(StudioPrimary.copy(alpha = 0.20f + timeline.canvasBackground.blurStrength * 0.20f), backgroundColor)) else Brush.radialGradient(listOf(StudioPrimary.copy(alpha = 0.16f * glow), backgroundColor))).onSizeChanged { previewWidthPx = it.width.toFloat().coerceAtLeast(1f); previewHeightPx = it.height.toFloat().coerceAtLeast(1f) }.pointerInput(activeClips.map { Triple(it.id, it.transform, it.zIndex) }, timeline.selectedClipId, previewWidthPx, previewHeightPx) {
        detectTapGestures(onTap = { tap ->
          val hit = TimelineEngine.overlayHitTest(TimelineEngine.overlayHitTargets(activeClips.filter { it.clipType == ClipType.Text || it.clipType == ClipType.Sticker || it.clipType == ClipType.Overlay }, tap.x, tap.y, previewWidthPx, previewHeightPx), tap.x, tap.y)
          when {
            hit.selectedOverlayId != null -> onSelect(hit.selectedOverlayId)
            else -> onClearSelection()
          }
          feedback = feedback.copy(owner = GestureOwner.PREVIEW_TAP)
        }, onDoubleTap = { tap ->
          val hitId = TimelineEngine.overlayHitTest(TimelineEngine.overlayHitTargets(activeClips.filter { it.clipType == ClipType.Text }, tap.x, tap.y, previewWidthPx, previewHeightPx, 144f, 88f), tap.x, tap.y).selectedOverlayId
          val hit = activeClips.firstOrNull { it.id == hitId }
          if (hit != null) {
            onSelect(hit.id)
            onEditText(EditorTool.Text)
            feedback = feedback.copy(owner = GestureOwner.TEXT_DOUBLE_TAP, pendingHaptic = HapticEvent.SNAP)
          }
        })
  }.pointerInput(selectedClip?.id, selectedClip?.transform, previewWidthPx, previewHeightPx) {
        val clip = selectedClip ?: return@pointerInput
        var gestureStarted = false
        var gesturePositionX = clip.transform.positionX
        var gesturePositionY = clip.transform.positionY
        var gestureScale = clip.transform.scale
        var gestureRotationDegrees = clip.transform.rotationDegrees
        var pointerStartX = 0f
        var pointerStartY = 0f
        detectTransformGestures { centroid, pan, zoom, rotation ->
          val owner = if (abs(zoom - 1f) > 0.01f || abs(rotation) > 0.25f) GestureOwner.OVERLAY_TRANSFORM else GestureOwner.OVERLAY_DRAG
          val slop = TimelineEngine.touchSlopGate(0f, 0f, pan.x, pan.y, touchSlopPx, if (owner == GestureOwner.OVERLAY_TRANSFORM) TimelineGestureMode.SCALING_OVERLAY else TimelineGestureMode.MOVING_OVERLAY)
          if (!gestureStarted && !slop.hasExceededTouchSlop && abs(zoom - 1f) <= 0.01f && abs(rotation) <= 0.25f) return@detectTransformGestures
          if (!gestureStarted) {
            gestureStarted = true
            pointerStartX = centroid.x
            pointerStartY = centroid.y
          }
          val lock = TimelineEngine.resolvePlaybackEditLock(timeline.isPlaying, slop.confirmedGestureMode.takeUnless { it == TimelineGestureMode.IDLE } ?: TimelineGestureMode.MOVING_OVERLAY)
          if (lock.shouldPauseBeforeEdit) {
            onSeek(timeline.playheadMs)
          } else if (lock.shouldBlockEditGesture) {
            feedback = feedback.copy(chipLabel = lock.lockReason, pendingHaptic = HapticEvent.INVALID_ACTION)
            return@detectTransformGestures
          }
          val startCenterX = gesturePositionX * previewWidthPx
          val startCenterY = gesturePositionY * previewHeightPx
          val drag = TimelineEngine.resolveOverlayDrag(clip.id, startCenterX, startCenterY, pointerStartX, pointerStartY, centroid.x, centroid.y, previewWidthPx, previewHeightPx)
          val transformed = TimelineEngine.resolveOverlayTransform(clip.id, drag.resolvedCenterX, drag.resolvedCenterY, 112f, 48f, centroid.x, centroid.y, gestureScale, zoom, gestureRotationDegrees, rotation)
          val boundary = TimelineEngine.resolveOverlayCanvasBoundary(clip.id, transformed.boundingBox.centerX, transformed.boundingBox.centerY, 112f, 48f, transformed.resolvedScale, transformed.resolvedRotationDegrees, previewWidthPx, previewHeightPx)
          gesturePositionX = boundary.resolvedCenterX / previewWidthPx
          gesturePositionY = boundary.resolvedCenterY / previewHeightPx
          gestureScale = transformed.resolvedScale
          gestureRotationDegrees = transformed.resolvedRotationDegrees
          onTransform(gesturePositionX, gesturePositionY, gestureScale, gestureRotationDegrees)
          val snap = transformed.snapResolution
          val guide = drag.snapResolution
          feedback = feedback.copy(
            owner = owner,
            showCenterXGuide = guide?.showVerticalCenterGuide == true,
            showCenterYGuide = guide?.showHorizontalCenterGuide == true,
            showBoundaryGuide = boundary.showBoundaryGuide,
            angleLabel = snap?.snappedRotationDegrees?.let { "${it.roundToInt()} deg" },
            chipLabel = if (boundary.showBoundaryGuide) "Edge limit" else null,
            pendingHaptic = if ((guide?.feedbackIntensity ?: 0f) > 0.85f || (snap?.feedbackIntensity ?: 0f) > 0.85f) HapticEvent.SNAP else feedback.pendingHaptic,
          )
        }
      }) {
        PreviewMediaSurface(primaryVisualClip, previewState, timeline.playheadMs, timeline.isPlaying, onSeek)
        Canvas(Modifier.fillMaxSize()) {
          drawRect(Color.White.copy(alpha = 0.05f), style = Stroke(width = 1.dp.toPx()))
          if (feedback.showCenterXGuide || selectedClip != null && feedback.owner == GestureOwner.OVERLAY_DRAG) {
            drawLine(EditorChromePrimary.copy(alpha = 0.35f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 1.dp.toPx())
          }
          if (feedback.showCenterYGuide || selectedClip != null && feedback.owner == GestureOwner.OVERLAY_DRAG) {
            drawLine(EditorChromePrimary.copy(alpha = 0.35f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1.dp.toPx())
          }
          if (feedback.showBoundaryGuide) {
            drawRect(EditorChromeAudioAccent.copy(alpha = 0.42f), style = Stroke(width = 2.dp.toPx()))
          }
        }
        activeClips.filter { it.clipType == ClipType.Text || it.clipType == ClipType.Sticker || it.clipType == ClipType.Overlay }.forEach { clip ->
          PreviewLayerChip(clip, selected = clip.id == timeline.selectedClipId, previewWidthPx = previewWidthPx, previewHeightPx = previewHeightPx, onSelect = { onSelect(clip.id) }, onDelete = onDelete)
        }
        feedback.angleLabel?.let { Text(it, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.42f)).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp), color = EditorChromeAudioAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        feedback.chipLabel?.let { Text(it, modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.42f)).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp), color = EditorChromeAudioAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        composition.transition?.let { transition ->
          Text("${transition.type.label} transition", modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(999.dp)).background(StudioBackground.copy(alpha = 0.72f)).padding(horizontal = 12.dp, vertical = 8.dp), color = StudioSecondary, fontWeight = FontWeight.Bold)
        }
        Text(
          timeline.playheadMs.asTimecode(),
          modifier = Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.40f)).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
        )
        Text(
          navGlyph(EditorTool.Canvas),
          modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.40f)).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
          color = Color.White,
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
        )
      }
    }
  }
}


@Composable
fun PreviewLayerChip(clip: TimelineClip, selected: Boolean, previewWidthPx: Float, previewHeightPx: Float, onSelect: () -> Unit, onDelete: () -> Unit) {
  val x = (clip.transform.positionX * previewWidthPx).roundToInt()
  val y = (clip.transform.positionY * previewHeightPx).roundToInt()
  Box(Modifier.fillMaxSize()) {
    Box(Modifier.align(Alignment.TopStart).offset { IntOffset(x - 56, y - 24) }.graphicsLayer { scaleX = clip.transform.scale; scaleY = clip.transform.scale; rotationZ = clip.transform.rotationDegrees }.size(width = 112.dp, height = 48.dp).clip(RoundedCornerShape(14.dp)).background(StudioBackground.copy(alpha = 0.72f)).border(if (selected) 2.dp else 1.dp, if (selected) StudioPrimary else Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp)).clickable(onClick = onSelect).pointerInput(clip.id) { detectTapGestures(onDoubleTap = { onSelect() }, onTap = { onSelect() }) }.padding(horizontal = 12.dp, vertical = 8.dp).semantics { contentDescription = if (selected) "Selected overlay ${clip.title}" else "Overlay ${clip.title}" }, contentAlignment = Alignment.Center) {
      Text(if (clip.clipType == ClipType.Text) clip.textProperties.content else clip.title, fontSize = clip.textProperties.fontSizeSp.coerceIn(14f, 34f).sp, maxLines = 2, textAlign = TextAlign.Center)
    }
    if (selected) {
      TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).semantics { contentDescription = "Delete selected overlay" }) { Text("Delete", color = StudioDanger) }
      Box(Modifier.align(Alignment.Center).size(10.dp).clip(CircleShape).background(StudioPrimary))
      Text("Rotate", modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp), color = StudioSecondary, fontSize = 11.sp)
    }
  }
}


@Composable
fun PreviewMediaSurface(clip: TimelineClip?, previewState: PreviewSurfaceState, playheadMs: Long, isPlaying: Boolean, onSeek: (Long) -> Unit) {
  when (previewState) {
    PreviewSurfaceState.NoMedia -> PreviewStatusCard("No media selected", "Import an image or video to start previewing and editing.", StudioTextMuted)
    PreviewSurfaceState.Loading -> PreviewStatusCard("Loading media", "Clipy Studio is preparing the selected preview.", StudioSecondary)
    PreviewSurfaceState.InvalidUri -> PreviewStatusCard("Invalid media", "This clip does not have a usable URI.", StudioDanger)
    PreviewSurfaceState.LoadFailed -> PreviewStatusCard("Media failed to load", "Clipy Studio could not open this file for preview.", StudioDanger)
    PreviewSurfaceState.ImageReady -> {
      val model = clip?.mediaUri ?: return PreviewStatusCard("Image unavailable", "The selected image is missing.", StudioDanger)
      val context = LocalContext.current
      val bitmap by produceState<Bitmap?>(initialValue = null, model, clip.id, clip.durationMs) {
        value = context.loadThumbnailBitmap(model, clip.sourceInMs.coerceAtLeast(0L), 1440, 1440)
      }
      if (bitmap == null) {
        PreviewStatusCard("Image failed to load", "Clipy Studio could not decode this image for preview.", StudioDanger)
        return
      }
      val previewBitmap = bitmap ?: return
      Box(Modifier.fillMaxSize()) {
        Image(
          bitmap = previewBitmap.asImageBitmap(),
          contentDescription = "Image preview for ${clip.title}",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Fit,
        )
        PreviewMediaBadges(clip, Modifier.align(Alignment.TopEnd).padding(10.dp))
      }
    }
    PreviewSurfaceState.VideoReady -> clip?.let { VideoPreviewPlayer(clip = it, isPlaying = isPlaying, playheadMs = playheadMs, onSeek = onSeek) }
      ?: PreviewStatusCard("Video unavailable", "The selected video is missing.", StudioDanger)
  }
}


@Composable
fun VideoPreviewPlayer(clip: TimelineClip, isPlaying: Boolean, playheadMs: Long, onSeek: (Long) -> Unit) {
  val context = LocalContext.current
  val mediaUri = clip.mediaUri
  if (mediaUri.isNullOrBlank()) {
    PreviewStatusCard("Video unavailable", "The selected video is missing.", StudioDanger)
    return
  }
  var loadState by remember(mediaUri) { mutableStateOf(VideoPreviewLoadState.Loading) }
  val player = remember(mediaUri) {
    ExoPlayer.Builder(context).build().apply {
      repeatMode = Player.REPEAT_MODE_OFF
      setMediaItem(MediaItem.fromUri(mediaUri))
      prepare()
    }
  }
  DisposableEffect(player) {
    onDispose { player.release() }
  }
  LaunchedEffect(isPlaying, mediaUri) {
    player.playWhenReady = isPlaying
    if (!isPlaying) player.pause()
  }
  LaunchedEffect(playheadMs, mediaUri, clip.id, clip.startMs, clip.durationMs, clip.sourceInMs, clip.sourceDurationMs, clip.videoProperties.speed, isPlaying) {
    val localPlayhead = (playheadMs - clip.startMs).coerceIn(0L, clip.durationMs)
    val maxSourcePosition = clip.sourceDurationMs?.coerceAtLeast(clip.sourceInMs + 1L)
    val unclampedTarget = (clip.sourceInMs + localPlayhead * clip.videoProperties.speed).toLong().coerceAtLeast(0L)
    val targetPosition = maxSourcePosition?.let { unclampedTarget.coerceAtMost(it - 1L) } ?: unclampedTarget
    if (!isPlaying || kotlin.math.abs(player.currentPosition - targetPosition) > 250L) {
      player.seekTo(targetPosition)
    }
  }
  DisposableEffect(player, onSeek) {
    val listener = object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        loadState = when (playbackState) {
          Player.STATE_READY -> VideoPreviewLoadState.Ready
          Player.STATE_IDLE -> VideoPreviewLoadState.Loading
          Player.STATE_BUFFERING -> if (loadState == VideoPreviewLoadState.Failed) VideoPreviewLoadState.Failed else VideoPreviewLoadState.Loading
          Player.STATE_ENDED -> VideoPreviewLoadState.Ready
          else -> loadState
        }
      }

      override fun onPlayerError(error: PlaybackException) {
        loadState = VideoPreviewLoadState.Failed
      }

      override fun onIsPlayingChanged(playing: Boolean) {
        if (!playing && loadState == VideoPreviewLoadState.Ready && player.playbackState == Player.STATE_ENDED) {
          onSeek((clip.startMs + clip.durationMs).coerceAtMost(playheadMs.coerceAtLeast(clip.startMs + clip.durationMs)))
        }
      }
    }
    player.addListener(listener)
    onDispose { player.removeListener(listener) }
  }
  Box(Modifier.fillMaxSize()) {
    AndroidView(
      factory = {
        PlayerView(it).apply {
          useController = false
          this.player = player
        }
      },
      modifier = Modifier.fillMaxSize(),
    )
    when (loadState) {
      VideoPreviewLoadState.Loading -> {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(color = StudioSecondary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            Text("Loading video preview", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
          }
        }
      }
      VideoPreviewLoadState.Failed -> PreviewStatusCard("Video failed to load", "Clipy Studio could not prepare this video for preview.", StudioDanger)
      VideoPreviewLoadState.Ready -> Unit
    }
    PreviewMediaBadges(clip, Modifier.align(Alignment.TopEnd).padding(10.dp))
  }
}

@Composable
private fun PreviewMediaBadges(clip: TimelineClip, modifier: Modifier = Modifier) {
  Column(
    modifier,
    horizontalAlignment = Alignment.End,
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    PreviewBadge(text = clipTypeBadge(clip.clipType))
    PreviewBadge(text = if (clip.clipType == ClipType.Image || clip.clipType == ClipType.Overlay && clip.mediaUri?.contains("image", ignoreCase = true) == true) "${(clip.durationMs / 1000f).let { "%.1fs".format(it) }} still" else clip.sourceDurationMs?.asTimecode() ?: clip.durationMs.asTimecode())
    PreviewBadge(text = clip.title)
  }
}

@Composable
private fun PreviewBadge(text: String) {
  Text(
    text,
    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(StudioBackground.copy(alpha = 0.78f)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
    color = Color.White,
    fontSize = 11.sp,
    fontWeight = FontWeight.SemiBold,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}


@Composable
fun PreviewStatusCard(title: String, body: String, tint: Color) {
  Box(
    Modifier.fillMaxSize().padding(18.dp).clip(RoundedCornerShape(16.dp)).background(EditorChromeSurfaceLow.copy(alpha = 0.72f)).border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
      Text(title, color = tint, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
      Text(body, color = StudioTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
  }
}


@Composable
fun PlaybackControls(timeline: Timeline, onPlay: () -> Unit, onSeek: (Long) -> Unit) {
  val hasContent = timeline.durationMs > 0L
  Row(
    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = { onSeek(-1_000) }, enabled = hasContent, modifier = Modifier.size(44.dp).semantics { contentDescription = "Seek backward" }) {
        Text("⏮", color = EditorChromeMuted, fontSize = 20.sp, fontWeight = FontWeight.Medium)
      }
      Surface(
        onClick = onPlay,
        enabled = hasContent,
        shape = CircleShape,
        color = Color.White,
        modifier = Modifier.size(40.dp).semantics { contentDescription = if (timeline.isPlaying) "Pause playback" else "Play playback" },
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(if (timeline.isPlaying) "❚❚" else "▶", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
      }
      IconButton(onClick = { onSeek(1_000) }, enabled = hasContent, modifier = Modifier.size(44.dp).semantics { contentDescription = "Seek forward" }) {
        Text("⏭", color = EditorChromeMuted, fontSize = 20.sp, fontWeight = FontWeight.Medium)
      }
    }
  }
}
