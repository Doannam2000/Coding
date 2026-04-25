package com.example.clipystudio.data

import java.util.UUID
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToLong

data class ProjectTimeline(
  val id: String = UUID.randomUUID().toString(),
  val durationMs: Long = 0,
  val tracks: TimelineTracks = TimelineTracks(),
  val zoomScale: Float = 1f,
  val pixelsPerSecond: Float = 72f,
  val scrollOffsetPx: Float = 0f,
  val currentTimeMs: Long = 0,
  val selectedClipId: String? = null,
  val markers: List<TimelineMarker> = emptyList(),
  val version: Long = 1,
)

data class TimelineTracks(
  val video: List<ProjectTimelineClip> = emptyList(),
  val audio: List<ProjectTimelineClip> = emptyList(),
  val text: List<ProjectTimelineClip> = emptyList(),
  val sticker: List<ProjectTimelineClip> = emptyList(),
  val overlay: List<ProjectTimelineClip> = emptyList(),
  val effect: List<ProjectTimelineClip> = emptyList(),
) {
  fun byType(trackType: TrackType): List<ProjectTimelineClip> = when (trackType) {
    TrackType.Video -> video
    TrackType.Audio -> audio
    TrackType.Text -> text
    TrackType.Sticker -> sticker
    TrackType.Overlay -> overlay
    TrackType.Effect -> effect
  }
}

data class ProjectTimelineClip(
  val id: String,
  val type: ClipType,
  val mediaUri: String? = null,
  val startTimeMs: Long,
  val durationMs: Long,
  val trimStartMs: Long = 0,
  val trimEndMs: Long = 0,
  val speed: Float = 1f,
  val volume: Float = 1f,
  val transform: TransformState = TransformState(),
  val filter: String? = null,
  val effect: String? = null,
  val trackType: TrackType,
  val trackIndex: Int = 0,
  val keyframes: List<Keyframe> = emptyList(),
  val text: String? = null,
  val textStyleRef: String? = null,
  val animationRef: String? = null,
)

data class TimelineViewportState(
  val scrollOffsetPx: Float,
  val zoomScale: Float,
  val pixelsPerSecond: Float,
  val currentTimeMs: Long,
  val isPlaying: Boolean,
  val fixedPlayheadOffsetPx: Float,
)

data class TimelineGestureState(
  val contentOffsetPx: Float = 0f,
  val viewportWidthPx: Float = TimelineEngine.DefaultViewportWidthPx,
  val contentWidthPx: Float = 0f,
  val isDragging: Boolean = false,
  val isFlinging: Boolean = false,
  val isOverscrolling: Boolean = false,
  val lastVelocityPxPerSec: Float = 0f,
  val currentZoom: Float = 1f,
)

data class TimelinePhysicsConfig(
  val decelerationRate: Float = 0.92f,
  val edgeResistanceFactor: Float = 0.22f,
  val overscrollLimitPx: Float = 116f,
  val minFlingVelocityPxPerSec: Float = 140f,
  val stopVelocityThresholdPxPerSec: Float = 24f,
  val settleSpringStiffness: Float = 0.18f,
  val settleSpringDampingRatio: Float = 0.82f,
)

data class TimelineScrollSnapshot(
  val contentOffsetPx: Float,
  val centerPlayheadTimeMs: Long,
  val visibleStartTimeMs: Long,
  val visibleEndTimeMs: Long,
  val pixelsPerMs: Float,
  val zoom: Float,
)

data class TimelineZoomState(
  val minZoom: Float,
  val maxZoom: Float,
  val currentZoom: Float,
  val anchorViewportPx: Float,
  val anchorTimeMs: Long,
)

data class SnapCandidateTarget(
  val type: SnapTargetType,
  val timeMs: Long,
  val distancePx: Float,
  val strength: Float,
  val label: String? = null,
)

data class SnapResolution(
  val snappedTimeMs: Long? = null,
  val target: SnapCandidateTarget? = null,
  val appliedOffsetPx: Float = 0f,
  val feedbackIntensity: Float = 0f,
)

data class TimelineSnapConfig(
  val baseThresholdPx: Float = 18f,
  val strongThresholdPx: Float = 8f,
  val maxInfluencePx: Float = 28f,
  val playheadBiasMultiplier: Float = 1.2f,
  val edgeBiasMultiplier: Float = 1.08f,
)

data class ClipLayoutBounds(
  val clipId: String,
  val trackType: TrackType,
  val trackIndex: Int,
  val leftPx: Float,
  val rightPx: Float,
  val widthPx: Float,
  val topPx: Float,
  val bottomPx: Float,
  val startTimeMs: Long,
  val endTimeMs: Long,
)

data class TimelineHitTestResult(
  val type: HitTestType,
  val clipId: String? = null,
  val trackType: TrackType? = null,
  val trackIndex: Int? = null,
  val timeMs: Long,
  val localXpx: Float = 0f,
)

enum class HitTestType { Empty, ClipBody, LeftTrimHandle, RightTrimHandle, TrackBackground }
data class TimelineDragState(val clipId: String, val trackType: TrackType, val dragStartTimeMs: Long, val originalStartTimeMs: Long, val proposedStartTimeMs: Long, val snapTarget: SnapTarget? = null, val isValid: Boolean)
data class TimelineTrimState(val clipId: String, val trackType: TrackType, val handle: TrimHandle, val originalTrimStartMs: Long, val originalTrimEndMs: Long, val proposedTrimStartMs: Long, val proposedTrimEndMs: Long, val minDurationMs: Long, val isValid: Boolean)
enum class TrimHandle { Left, Right }
enum class SnapTarget { None, Playhead, ClipStart, ClipEnd }
data class TimelineMutationResult(val timeline: Timeline, val selectedClipId: String?, val currentTimeMs: Long, val changedClipIds: List<String>, val rejectedReason: String? = null)

data class TimelineZoomResult(val timeline: Timeline, val previousZoomScale: Float, val newZoomScale: Float, val previousScrollOffsetPx: Float, val newScrollOffsetPx: Float, val focalTimeMs: Long, val currentTimeMs: Long)
data class VisibleTimelineRange(val startTimeMs: Long, val endTimeMs: Long, val viewportStartPx: Float, val viewportEndPx: Float, val zoomScale: Float, val pixelsPerSecond: Float)
data class TimelineThumbnailRequest(val clipId: String, val mediaUri: String, val startTimeMs: Long, val durationMs: Long, val thumbnailTimeMs: Long, val widthPx: Int, val heightPx: Int, val cacheKey: String)
data class TimelineThumbnailState(val clipId: String, val cacheKey: String, val status: ThumbnailStatus, val requestedAtMs: Long, val lastAccessedAtMs: Long)
enum class ThumbnailStatus { Missing, Loading, Ready, Failed }
data class TimelineSnapResult(val adjustedTimeMs: Long, val targetTimeMs: Long?, val targetType: SnapTargetType, val distanceMs: Long, val sourceClipId: String?, val targetClipId: String?, val markerId: String?, val isSnapped: Boolean)
enum class SnapTargetType { None, Playhead, ClipStart, ClipEnd, TimelineStart, TimelineEnd, Marker, TransitionStart, TransitionEnd }
data class TimelineMarker(val id: String = UUID.randomUUID().toString(), val timeMs: Long, val label: String, val color: Long? = null)
data class ActiveTimelineComposition(val currentTimeMs: Long, val video: ActiveClip?, val audio: List<ActiveClip>, val text: List<ActiveClip>, val stickers: List<ActiveClip>, val overlays: List<ActiveClip>, val effects: List<ActiveClip>, val transition: ActiveTransition?)
data class ActiveClip(val clipId: String, val trackType: TrackType, val trackIndex: Int, val localTimeMs: Long, val sourceTimeMs: Long, val transform: TransformState, val opacity: Float, val volume: Float)
data class ActiveTransition(val transitionId: String, val fromClipId: String, val toClipId: String, val type: TransitionType, val startTimeMs: Long, val endTimeMs: Long, val progress: Float)
data class TimelineFlingFrame(val nextOffsetPx: Float, val nextVelocityPxPerSec: Float, val resistanceFraction: Float, val isFinished: Boolean)
data class ClipGestureResolution(val proposedStartTimeMs: Long, val resolvedStartTimeMs: Long, val snapResolution: SnapResolution, val isValid: Boolean)
data class TrimGestureResolution(val proposedTimeMs: Long, val resolvedTimeMs: Long, val snapResolution: SnapResolution, val isValid: Boolean)
data class TimelineDragUpdate(
  val nextOffsetPx: Float,
  val currentTimeMs: Long,
  val resistanceFraction: Float,
)

data class TimelineSettleFrame(
  val offsetPx: Float,
  val resistanceFraction: Float,
)

class TimelineThumbnailCache(private val maxEntries: Int = 96) {
  private val entries = linkedMapOf<String, TimelineThumbnailState>()
  fun get(cacheKey: String): TimelineThumbnailState? = entries[cacheKey]?.also { entries[cacheKey] = it.copy(lastAccessedAtMs = System.currentTimeMillis()) }
  fun put(state: TimelineThumbnailState) {
    entries.remove(state.cacheKey)
    entries[state.cacheKey] = state
    while (entries.size > maxEntries) entries.remove(entries.keys.first())
  }
  fun snapshot(): Map<String, TimelineThumbnailState> = entries.toMap()
}

object TimelineEngine {
  const val MinClipDurationMs: Long = 600
  const val DefaultPixelsPerSecond: Float = 72f
  const val DefaultViewportWidthPx: Float = 440f
  const val MinZoomScale: Float = 0.65f
  const val MaxZoomScale: Float = 3f
  private const val SnapThresholdPx: Float = 14f
  val DefaultPhysics = TimelinePhysicsConfig()
  val DefaultSnapConfig = TimelineSnapConfig()

  fun toProjectTimeline(timeline: Timeline): ProjectTimeline = ProjectTimeline(
    id = timeline.id,
    durationMs = timeline.durationMs,
    tracks = TimelineTracks(
      video = timeline.projectClips(TrackType.Video),
      audio = timeline.projectClips(TrackType.Audio),
      text = timeline.projectClips(TrackType.Text),
      sticker = timeline.projectClips(TrackType.Sticker),
      overlay = timeline.projectClips(TrackType.Overlay),
      effect = timeline.projectClips(TrackType.Effect),
    ),
    zoomScale = timeline.zoomLevel,
    pixelsPerSecond = timeline.pixelsPerSecond,
    scrollOffsetPx = timeline.scrollOffsetPx,
    currentTimeMs = timeline.playheadMs,
    selectedClipId = timeline.selectedClipId,
    markers = timeline.markers,
    version = timeline.version,
  )

  fun pixelsPerMs(zoomScale: Float, pixelsPerSecond: Float): Float = (pixelsPerSecond * zoomScale).coerceAtLeast(1f) / 1_000f

  fun centerPlayheadOffsetPx(viewportWidthPx: Float): Float = viewportWidthPx.coerceAtLeast(1f) / 2f

  fun contentWidthPx(durationMs: Long, zoomScale: Float, pixelsPerSecond: Float): Float = durationMs.coerceAtLeast(0L) * pixelsPerMs(zoomScale, pixelsPerSecond)

  fun maxScrollOffsetPx(durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx): Float {
    val contentWidth = contentWidthPx(durationMs, zoomScale, pixelsPerSecond)
    return (contentWidth - centerPlayheadOffsetPx(viewportWidthPx)).coerceAtLeast(0f)
  }

  fun clampScrollOffset(scrollOffsetPx: Float, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx): Float {
    return scrollOffsetPx.coerceIn(0f, maxScrollOffsetPx(durationMs, zoomScale, pixelsPerSecond, viewportWidthPx))
  }

  fun timeFromScroll(scrollOffsetPx: Float, zoomScale: Float, pixelsPerSecond: Float, durationMs: Long, viewportWidthPx: Float = DefaultViewportWidthPx): Long {
    val pxPerMs = (pixelsPerSecond * zoomScale).coerceAtLeast(1f) / 1_000f
    val centeredOffset = scrollOffsetPx + centerPlayheadOffsetPx(viewportWidthPx)
    return (centeredOffset / pxPerMs).roundToLong().coerceIn(0L, durationMs)
  }

  fun scrollFromTime(timeMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx): Float {
    return (timeMs.coerceAtLeast(0L) * pixelsPerMs(zoomScale, pixelsPerSecond) - centerPlayheadOffsetPx(viewportWidthPx)).coerceAtLeast(0f)
  }

  fun scrollSnapshot(timeline: Timeline, viewportWidthPx: Float = DefaultViewportWidthPx): TimelineScrollSnapshot {
    val pxPerMs = pixelsPerMs(timeline.zoomLevel, timeline.pixelsPerSecond)
    val visibleStart = ((timeline.scrollOffsetPx.coerceAtLeast(0f)) / pxPerMs).roundToLong().coerceIn(0L, timeline.durationMs)
    val visibleEnd = ((timeline.scrollOffsetPx + viewportWidthPx.coerceAtLeast(1f)) / pxPerMs).roundToLong().coerceIn(0L, timeline.durationMs)
    return TimelineScrollSnapshot(
      contentOffsetPx = timeline.scrollOffsetPx,
      centerPlayheadTimeMs = timeFromScroll(timeline.scrollOffsetPx, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs, viewportWidthPx),
      visibleStartTimeMs = visibleStart,
      visibleEndTimeMs = visibleEnd,
      pixelsPerMs = pxPerMs,
      zoom = timeline.zoomLevel,
    )
  }

  fun applyEdgeResistance(rawOffsetPx: Float, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx, physics: TimelinePhysicsConfig = DefaultPhysics): Float {
    val min = 0f
    val max = maxScrollOffsetPx(durationMs, zoomScale, pixelsPerSecond, viewportWidthPx)
    return when {
      rawOffsetPx < min -> (rawOffsetPx * physics.edgeResistanceFactor).coerceAtLeast(-physics.overscrollLimitPx)
      rawOffsetPx > max -> max + ((rawOffsetPx - max) * physics.edgeResistanceFactor).coerceAtMost(physics.overscrollLimitPx)
      else -> rawOffsetPx
    }
  }

  fun settleScrollOffset(scrollOffsetPx: Float, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx): Float {
    return clampScrollOffset(scrollOffsetPx, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx)
  }

  fun overscrollDistancePx(scrollOffsetPx: Float, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx): Float {
    val clamped = clampScrollOffset(scrollOffsetPx, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx)
    return abs(scrollOffsetPx - clamped)
  }

  fun resistanceFraction(scrollOffsetPx: Float, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx, physics: TimelinePhysicsConfig = DefaultPhysics): Float {
    return (overscrollDistancePx(scrollOffsetPx, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx) / physics.overscrollLimitPx)
      .coerceIn(0f, 1f)
  }

  fun dragTimelineBy(scrollOffsetPx: Float, deltaX: Float, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx, physics: TimelinePhysicsConfig = DefaultPhysics): Float {
    return applyEdgeResistance(
      rawOffsetPx = scrollOffsetPx - deltaX,
      durationMs = durationMs,
      zoomScale = zoomScale,
      pixelsPerSecond = pixelsPerSecond,
      viewportWidthPx = viewportWidthPx,
      physics = physics,
    )
  }

  fun dragTimeline(
    scrollOffsetPx: Float,
    deltaX: Float,
    durationMs: Long,
    zoomScale: Float,
    pixelsPerSecond: Float,
    viewportWidthPx: Float = DefaultViewportWidthPx,
    physics: TimelinePhysicsConfig = DefaultPhysics,
  ): TimelineDragUpdate {
    val nextOffset = dragTimelineBy(scrollOffsetPx, deltaX, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx, physics)
    return TimelineDragUpdate(
      nextOffsetPx = nextOffset,
      currentTimeMs = timeFromScroll(nextOffset, zoomScale, pixelsPerSecond, durationMs, viewportWidthPx),
      resistanceFraction = resistanceFraction(nextOffset, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx, physics),
    )
  }

  fun decayVelocity(velocityPxPerSec: Float, frameDeltaMs: Long, physics: TimelinePhysicsConfig = DefaultPhysics): Float {
    val decayPower = (frameDeltaMs.coerceAtLeast(1L) / 16f).coerceAtLeast(0.25f)
    return velocityPxPerSec * physics.decelerationRate.toDouble().pow(decayPower.toDouble()).toFloat()
  }

  fun updateDragVelocity(previousVelocityPxPerSec: Float, deltaPx: Float, deltaMs: Long): Float {
    val instantVelocity = ((-deltaPx / deltaMs.coerceAtLeast(1L)) * 1000f)
    return if (previousVelocityPxPerSec == 0f) instantVelocity else (previousVelocityPxPerSec * 0.72f) + (instantVelocity * 0.28f)
  }

  fun advanceFling(scrollOffsetPx: Float, velocityPxPerSec: Float, frameDeltaMs: Long, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx, physics: TimelinePhysicsConfig = DefaultPhysics): TimelineFlingFrame {
    val frameSeconds = frameDeltaMs.coerceAtLeast(1L) / 1_000f
    val rawOffset = scrollOffsetPx + (velocityPxPerSec * frameSeconds)
    val nextOffset = applyEdgeResistance(rawOffset, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx, physics)
    val resistance = resistanceFraction(nextOffset, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx, physics)
    val nextVelocity = decayVelocity(velocityPxPerSec, frameDeltaMs, physics) * (1f - resistance * 0.35f)
    return TimelineFlingFrame(
      nextOffsetPx = nextOffset,
      nextVelocityPxPerSec = nextVelocity,
      resistanceFraction = resistance,
      isFinished = abs(nextVelocity) <= physics.stopVelocityThresholdPxPerSec,
    )
  }

  fun settleScrollFrames(
    scrollOffsetPx: Float,
    durationMs: Long,
    zoomScale: Float,
    pixelsPerSecond: Float,
    viewportWidthPx: Float = DefaultViewportWidthPx,
    physics: TimelinePhysicsConfig = DefaultPhysics,
    steps: Int = 6,
  ): List<TimelineSettleFrame> {
    val target = settleScrollOffset(scrollOffsetPx, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx)
    if (abs(target - scrollOffsetPx) < 0.5f) return emptyList()
    val frameCount = steps.coerceAtLeast(1)
    return (1..frameCount).map { index ->
      val progress = index / frameCount.toFloat()
      val eased = 1f - (1f - progress) * (1f - progress) * (1f - progress)
      val offset = scrollOffsetPx + (target - scrollOffsetPx) * eased
      TimelineSettleFrame(
        offsetPx = offset,
        resistanceFraction = resistanceFraction(offset, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx, physics),
      )
    }
  }

  fun zoomAroundFocal(timeline: Timeline, zoomDelta: Float, focalXpx: Float, viewportWidthPx: Float): TimelineZoomResult {
    val previousZoom = timeline.zoomLevel
    val nextZoom = (previousZoom * zoomDelta.coerceIn(0.25f, 4f)).coerceIn(MinZoomScale, MaxZoomScale)
    val previousScroll = timeline.scrollOffsetPx
    val pxPerMsBefore = pixelsPerMs(previousZoom, timeline.pixelsPerSecond)
    val focalTime = ((previousScroll + focalXpx.coerceIn(0f, viewportWidthPx.coerceAtLeast(1f))) / pxPerMsBefore).roundToLong().coerceIn(0L, timeline.durationMs)
    val playheadTime = timeFromScroll(previousScroll, previousZoom, timeline.pixelsPerSecond, timeline.durationMs, viewportWidthPx)
    val anchor = anchorZoom(previousScroll, previousZoom, nextZoom, focalXpx, timeline.durationMs, timeline.pixelsPerSecond, viewportWidthPx)
    val nextScroll = scrollFromTime(playheadTime, nextZoom, timeline.pixelsPerSecond, viewportWidthPx)
      .coerceIn(0f, maxScrollOffsetPx(timeline.durationMs, nextZoom, timeline.pixelsPerSecond, viewportWidthPx))
    val next = timeline.copy(zoomLevel = nextZoom, scrollOffsetPx = nextScroll, playheadMs = playheadTime).nextVersion()
    return TimelineZoomResult(next, previousZoom, nextZoom, previousScroll, nextScroll, focalTime, next.playheadMs)
  }

  fun anchorZoom(
    scrollOffsetPx: Float,
    previousZoom: Float,
    nextZoom: Float,
    focalXpx: Float,
    durationMs: Long,
    pixelsPerSecond: Float,
    viewportWidthPx: Float = DefaultViewportWidthPx,
  ): TimelineDragUpdate {
    val anchorViewportPx = focalXpx.coerceIn(0f, viewportWidthPx.coerceAtLeast(1f))
    val anchorTimeMs = ((scrollOffsetPx + anchorViewportPx) / pixelsPerMs(previousZoom, pixelsPerSecond)).roundToLong().coerceIn(0L, durationMs)
    val nextOffset = (anchorTimeMs * pixelsPerMs(nextZoom, pixelsPerSecond) - anchorViewportPx)
      .coerceIn(0f, maxScrollOffsetPx(durationMs, nextZoom, pixelsPerSecond, viewportWidthPx))
    return TimelineDragUpdate(
      nextOffsetPx = nextOffset,
      currentTimeMs = anchorTimeMs,
      resistanceFraction = resistanceFraction(nextOffset, durationMs, nextZoom, pixelsPerSecond, viewportWidthPx),
    )
  }

  fun withScroll(timeline: Timeline, scrollOffsetPx: Float, viewportWidthPx: Float = DefaultViewportWidthPx): Timeline {
    val current = timeFromScroll(scrollOffsetPx, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs, viewportWidthPx)
    return timeline.copy(scrollOffsetPx = scrollOffsetPx, playheadMs = current).nextVersion()
  }

  fun visibleRange(timeline: Timeline, viewportWidthPx: Float, prefetchPx: Float = 160f): VisibleTimelineRange {
    val startPx = (timeline.scrollOffsetPx - prefetchPx).coerceAtLeast(0f)
    val endPx = timeline.scrollOffsetPx + viewportWidthPx.coerceAtLeast(1f) + prefetchPx
    return VisibleTimelineRange(
      startTimeMs = ((startPx / pixelsPerMs(timeline.zoomLevel, timeline.pixelsPerSecond)).roundToLong()).coerceIn(0L, timeline.durationMs),
      endTimeMs = ((endPx / pixelsPerMs(timeline.zoomLevel, timeline.pixelsPerSecond)).roundToLong()).coerceIn(0L, timeline.durationMs),
      viewportStartPx = startPx,
      viewportEndPx = endPx,
      zoomScale = timeline.zoomLevel,
      pixelsPerSecond = timeline.pixelsPerSecond,
    )
  }

  fun planThumbnailRequests(timeline: Timeline, visibleRange: VisibleTimelineRange, cached: Map<String, TimelineThumbnailState> = emptyMap(), cellWidthPx: Int = 72, heightPx: Int = 32): List<TimelineThumbnailRequest> {
    val visibleTypes = setOf(ClipType.Video, ClipType.Image, ClipType.Overlay)
    return timeline.tracks.flatMap { track ->
      track.clips.filter { it.clipType in visibleTypes && it.startMs < visibleRange.endTimeMs && it.startMs + it.durationMs > visibleRange.startTimeMs }.mapNotNull { clip ->
        val sourceTime = (visibleRange.startTimeMs - clip.startMs).coerceIn(0L, clip.durationMs) + clip.sourceInMs
        val key = listOf(clip.id, clip.assetId.orEmpty(), sourceTime / 500L, cellWidthPx, heightPx, timeline.version).joinToString(":")
        val state = cached[key]
        if (state?.status == ThumbnailStatus.Ready || state?.status == ThumbnailStatus.Loading) null else TimelineThumbnailRequest(clip.id, clip.assetId ?: "local://${clip.clipType.name.lowercase()}/${clip.id}", clip.startMs, clip.durationMs, sourceTime, cellWidthPx, heightPx, key)
      }
    }
  }

  fun withPlaybackTick(timeline: Timeline, deltaMs: Long): Timeline {
    val nextTime = (timeline.playheadMs + deltaMs).coerceIn(0L, timeline.durationMs)
    return timeline.copy(playheadMs = nextTime, scrollOffsetPx = scrollFromTime(nextTime, timeline.zoomLevel, timeline.pixelsPerSecond), isPlaying = nextTime < timeline.durationMs).nextVersion()
  }

  fun calculateLayout(projectTimeline: ProjectTimeline, rowHeightPx: Float, rowGapPx: Float): List<ClipLayoutBounds> {
    val order = listOf(TrackType.Video, TrackType.Audio, TrackType.Text, TrackType.Sticker, TrackType.Overlay, TrackType.Effect)
    val pxPerMs = pixelsPerMs(projectTimeline.zoomScale, projectTimeline.pixelsPerSecond)
    return order.flatMapIndexed { row, type ->
      projectTimeline.tracks.byType(type).map { clip ->
        val top = row * (rowHeightPx + rowGapPx)
        val left = clip.startTimeMs * pxPerMs - projectTimeline.scrollOffsetPx
        val width = clip.durationMs.coerceAtLeast(MinClipDurationMs) * pxPerMs
        ClipLayoutBounds(clip.id, type, clip.trackIndex, left, left + width, width, top, top + rowHeightPx, clip.startTimeMs, clip.startTimeMs + clip.durationMs)
      }
    }
  }

  fun hitTest(bounds: List<ClipLayoutBounds>, xPx: Float, yPx: Float, handleWidthPx: Float, currentTimeMs: Long): TimelineHitTestResult {
    val row = bounds.firstOrNull { yPx in it.topPx..it.bottomPx }
    val clip = bounds.lastOrNull { xPx in it.leftPx..it.rightPx && yPx in it.topPx..it.bottomPx }
      ?: return TimelineHitTestResult(if (row == null) HitTestType.Empty else HitTestType.TrackBackground, trackType = row?.trackType, trackIndex = row?.trackIndex, timeMs = currentTimeMs)
    val localX = xPx - clip.leftPx
    val type = when {
      localX <= handleWidthPx -> HitTestType.LeftTrimHandle
      clip.rightPx - xPx <= handleWidthPx -> HitTestType.RightTrimHandle
      else -> HitTestType.ClipBody
    }
    return TimelineHitTestResult(type, clip.clipId, clip.trackType, clip.trackIndex, currentTimeMs, localX)
  }

  fun dragClip(timeline: Timeline, clipId: String, deltaMs: Long): TimelineMutationResult {
    val located = timeline.locateClip(clipId) ?: return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Clip not found")
    val original = located.clip
    val resolution = resolveDraggedClip(timeline, clipId, (original.startMs + deltaMs).coerceAtLeast(0L))
    if (!resolution.isValid) {
      return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Video clips cannot overlap")
    }
    val next = timeline.replaceClip(clipId) { it.copy(startMs = resolution.resolvedStartTimeMs) }.recalculateDuration().nextVersion()
    return TimelineMutationResult(next, clipId, next.playheadMs, listOf(clipId))
  }

  fun trimClip(timeline: Timeline, clipId: String, handle: TrimHandle, deltaMs: Long): TimelineMutationResult {
    val located = timeline.locateClip(clipId) ?: return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Clip not found")
    val clip = located.clip
    val proposedTimeMs = when (handle) {
      TrimHandle.Left -> clip.startMs + deltaMs
      TrimHandle.Right -> clip.startMs + clip.durationMs + deltaMs
    }
    val nextClip = resolveTrimmedClip(timeline, clipId, handle, proposedTimeMs)
    if (nextClip == null) {
      return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Trim would overlap video")
    }
    val next = timeline.replaceClip(clipId) { nextClip }.recalculateDuration().nextVersion()
    return TimelineMutationResult(next, clipId, next.playheadMs, listOf(clipId))
  }

  fun resolveDraggedClip(timeline: Timeline, clipId: String, proposedStartMs: Long): ClipGestureResolution {
    val located = timeline.locateClip(clipId) ?: return ClipGestureResolution(proposedStartMs, proposedStartMs, SnapResolution(), false)
    val proposed = proposedStartMs.coerceIn(0L, timeline.durationMs.coerceAtLeast(located.clip.durationMs))
    val snap = resolveSnapResolution(timeline, located.track.type, clipId, proposed)
    val resolved = (snap.snappedTimeMs ?: proposed).coerceAtLeast(0L)
    val isValid = located.track.type != TrackType.Video || !hasVideoOverlap(timeline, clipId, resolved, located.clip.durationMs)
    return ClipGestureResolution(proposed, resolved, snap, isValid)
  }

  fun resolveTrimGesture(timeline: Timeline, clipId: String, handle: TrimHandle, proposedTimeMs: Long): TrimGestureResolution {
    val located = timeline.locateClip(clipId) ?: return TrimGestureResolution(proposedTimeMs, proposedTimeMs, SnapResolution(), false)
    val clip = located.clip
    val rawTime = when (handle) {
      TrimHandle.Left -> proposedTimeMs.coerceIn((clip.startMs - clip.sourceInMs).coerceAtLeast(0L), clip.startMs + clip.durationMs - MinClipDurationMs)
      TrimHandle.Right -> proposedTimeMs.coerceIn(clip.startMs + MinClipDurationMs, timeline.durationMs.coerceAtLeast(clip.startMs + MinClipDurationMs))
    }
    val snap = resolveSnapResolution(timeline, located.track.type, clipId, rawTime)
    val resolved = when (handle) {
      TrimHandle.Left -> (snap.snappedTimeMs ?: rawTime).coerceIn((clip.startMs - clip.sourceInMs).coerceAtLeast(0L), clip.startMs + clip.durationMs - MinClipDurationMs)
      TrimHandle.Right -> (snap.snappedTimeMs ?: rawTime).coerceIn(clip.startMs + MinClipDurationMs, timeline.durationMs.coerceAtLeast(clip.startMs + MinClipDurationMs))
    }
    val nextClip = resolveTrimmedClip(timeline, clipId, handle, resolved)
    return TrimGestureResolution(rawTime, resolved, snap, nextClip != null)
  }

  fun resolveTrimmedClip(timeline: Timeline, clipId: String, handle: TrimHandle, proposedTimeMs: Long): TimelineClip? {
    val located = timeline.locateClip(clipId) ?: return null
    val clip = located.clip
    val nextClip = when (handle) {
      TrimHandle.Left -> {
        val start = proposedTimeMs.coerceIn((clip.startMs - clip.sourceInMs).coerceAtLeast(0L), clip.startMs + clip.durationMs - MinClipDurationMs)
        val end = clip.startMs + clip.durationMs
        clip.copy(
          startMs = start,
          sourceInMs = (clip.sourceInMs + (start - clip.startMs)).coerceAtLeast(0L),
          durationMs = (end - start).coerceAtLeast(MinClipDurationMs),
        )
      }
      TrimHandle.Right -> {
        val end = proposedTimeMs.coerceAtLeast(clip.startMs + MinClipDurationMs)
        clip.copy(durationMs = (end - clip.startMs).coerceAtLeast(MinClipDurationMs))
      }
    }
    return if (located.track.type == TrackType.Video && hasVideoOverlap(timeline, clip.id, nextClip.startMs, nextClip.durationMs)) null else nextClip
  }

  fun splitSelectedClip(timeline: Timeline): TimelineMutationResult {
    val selectedId = timeline.selectedClipId ?: return TimelineMutationResult(timeline, null, timeline.playheadMs, emptyList(), "No selected clip")
    val located = timeline.locateClip(selectedId) ?: return TimelineMutationResult(timeline, selectedId, timeline.playheadMs, emptyList(), "Clip not found")
    val clip = located.clip
    val split = timeline.playheadMs - clip.startMs
    if (split < MinClipDurationMs || clip.durationMs - split < MinClipDurationMs) return TimelineMutationResult(timeline, selectedId, timeline.playheadMs, emptyList(), "Split too close to clip edge")
    val first = clip.copy(durationMs = split)
    val second = clip.copy(id = UUID.randomUUID().toString(), startMs = timeline.playheadMs, durationMs = clip.durationMs - split, sourceInMs = clip.sourceInMs + split)
    val next = timeline.copy(tracks = timeline.tracks.map { track -> if (track.id == located.track.id) track.copy(clips = track.clips.flatMap { if (it.id == clip.id) listOf(first, second) else listOf(it) }) else track }).recalculateDuration().nextVersion()
    return TimelineMutationResult(next, second.id, timeline.playheadMs, listOf(first.id, second.id))
  }

  fun reorderVideoClip(timeline: Timeline, clipId: String, targetIndex: Int): TimelineMutationResult {
    val video = timeline.tracks.firstOrNull { it.type == TrackType.Video } ?: return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Video track missing")
    val clips = video.clips.sortedBy { it.startMs }.toMutableList()
    val currentIndex = clips.indexOfFirst { it.id == clipId }
    if (currentIndex == -1) return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Clip not found")
    val moved = clips.removeAt(currentIndex)
    clips.add(targetIndex.coerceIn(0, clips.size), moved)
    var cursor = 0L
    val ordered = clips.map { clip -> clip.copy(startMs = cursor).also { cursor += clip.durationMs } }
    val next = timeline.copy(tracks = timeline.tracks.map { if (it.type == TrackType.Video) it.copy(clips = ordered) else it }).recalculateDuration().nextVersion()
    return TimelineMutationResult(next, clipId, next.playheadMs, ordered.map { it.id })
  }

  fun activeClips(timeline: Timeline): List<TimelineClip> = timeline.tracks.flatMap { it.clips }.filter { timeline.playheadMs >= it.startMs && timeline.playheadMs < it.startMs + it.durationMs }.sortedBy { it.zIndex }

  fun resolveActiveComposition(timeline: Timeline, currentTimeMs: Long = timeline.playheadMs): ActiveTimelineComposition {
    val activeByTrack = timeline.tracks.sortedBy { it.orderIndex }.flatMap { track ->
      track.clips.filter { currentTimeMs >= it.startMs && currentTimeMs < it.startMs + it.durationMs }.sortedWith(compareBy<TimelineClip> { it.startMs }.thenBy { it.id }).map { clip ->
        val local = (currentTimeMs - clip.startMs).coerceAtLeast(0L)
        val transform = interpolateTransform(clip, currentTimeMs)
        ActiveClip(clip.id, track.type, track.orderIndex, local, clip.sourceInMs + (local * clip.videoProperties.speed).roundToLong(), transform, transform.opacity, clip.audioProperties.volume)
      }
    }
    return ActiveTimelineComposition(
      currentTimeMs = currentTimeMs,
      video = activeByTrack.firstOrNull { it.trackType == TrackType.Video },
      audio = activeByTrack.filter { it.trackType == TrackType.Audio },
      text = activeByTrack.filter { it.trackType == TrackType.Text },
      stickers = activeByTrack.filter { it.trackType == TrackType.Sticker },
      overlays = activeByTrack.filter { it.trackType == TrackType.Overlay },
      effects = activeByTrack.filter { it.trackType == TrackType.Effect },
      transition = activeTransition(timeline, currentTimeMs),
    )
  }

  fun transitionWindow(timeline: Timeline, transition: Transition): LongRange? {
    val from = timeline.tracks.firstOrNull { it.type == TrackType.Video }?.clips.orEmpty().firstOrNull { it.id == transition.fromClipId } ?: return null
    val to = timeline.tracks.firstOrNull { it.type == TrackType.Video }?.clips.orEmpty().firstOrNull { it.id == transition.toClipId } ?: return null
    val boundary = transition.boundaryMs.takeIf { it > 0 } ?: from.startMs + from.durationMs
    val duration = transition.durationMs.coerceIn(300L, min(from.durationMs, to.durationMs).coerceAtLeast(300L))
    val start = (boundary - duration / 2).coerceAtLeast(from.startMs)
    val end = (start + duration).coerceAtMost(to.startMs + to.durationMs)
    return start..end
  }

  fun activeTransition(timeline: Timeline, currentTimeMs: Long = timeline.playheadMs): ActiveTransition? = timeline.transitions.firstNotNullOfOrNull { transition ->
    val window = transitionWindow(timeline, transition) ?: return@firstNotNullOfOrNull null
    if (currentTimeMs !in window) null else {
      val span = (window.last - window.first).coerceAtLeast(1L)
      ActiveTransition(transition.id, transition.fromClipId, transition.toClipId, transition.type, window.first, window.last, ((currentTimeMs - window.first).toFloat() / span).coerceIn(0f, 1f))
    }
  }

  fun interpolateTransform(clip: TimelineClip, absoluteTimeMs: Long): TransformState {
    val localTime = (absoluteTimeMs - clip.startMs).coerceIn(0L, clip.durationMs)
    fun property(property: KeyframeProperty, fallback: Float): Float {
      val frames = clip.keyframes.filter { it.property == property }.sortedBy { it.timeMs }
      if (frames.isEmpty()) return fallback
      val before = frames.lastOrNull { it.timeMs <= localTime } ?: return fallback
      val after = frames.firstOrNull { it.timeMs >= localTime } ?: return fallback
      if (before.timeMs == after.timeMs) return before.value
      val t = ((localTime - before.timeMs).toFloat() / (after.timeMs - before.timeMs)).coerceIn(0f, 1f)
      return before.value + (after.value - before.value) * t
    }
    return clip.transform.copy(
      positionX = property(KeyframeProperty.PositionX, clip.transform.positionX),
      positionY = property(KeyframeProperty.PositionY, clip.transform.positionY),
      scale = property(KeyframeProperty.Scale, clip.transform.scale),
      rotationDegrees = property(KeyframeProperty.Rotation, clip.transform.rotationDegrees),
      opacity = property(KeyframeProperty.Opacity, clip.transform.opacity),
    )
  }

  private fun Timeline.projectClips(type: TrackType): List<ProjectTimelineClip> = tracks.firstOrNull { it.type == type }?.clips.orEmpty().map { clip ->
    ProjectTimelineClip(
      id = clip.id,
      type = clip.clipType,
      mediaUri = clip.assetId,
      startTimeMs = clip.startMs,
      durationMs = clip.durationMs,
      trimStartMs = clip.sourceInMs,
      trimEndMs = 0,
      speed = clip.videoProperties.speed,
      volume = clip.audioProperties.volume,
      transform = clip.transform,
      filter = clip.filterAdjustments.filterId,
      effect = if (clip.clipType == ClipType.Effect) clip.filterAdjustments.filterId ?: clip.title else null,
      trackType = type,
      trackIndex = tracks.firstOrNull { it.type == type }?.orderIndex ?: 0,
      keyframes = clip.keyframes,
      text = clip.textProperties.content.takeIf { clip.clipType == ClipType.Text || clip.clipType == ClipType.Sticker },
      textStyleRef = listOf(clip.textProperties.fontSizeSp, clip.textProperties.color, clip.textProperties.backgroundColor, clip.textProperties.alignment).joinToString("|"),
      animationRef = clip.textProperties.animation,
    )
  }

  fun collectSnapCandidates(timeline: Timeline, trackType: TrackType, clipId: String, proposed: Long, snapConfig: TimelineSnapConfig = DefaultSnapConfig): List<SnapCandidateTarget> {
    val pxPerMs = pixelsPerMs(timeline.zoomLevel, timeline.pixelsPerSecond)
    return buildList {
      add(candidateTarget(timeline.playheadMs, SnapTargetType.Playhead, proposed, pxPerMs, snapConfig, "Playhead"))
      add(candidateTarget(0L, SnapTargetType.TimelineStart, proposed, pxPerMs, snapConfig, "Start"))
      add(candidateTarget(timeline.durationMs, SnapTargetType.TimelineEnd, proposed, pxPerMs, snapConfig, "End"))
      timeline.tracks.firstOrNull { it.type == trackType }?.clips.orEmpty().filterNot { it.id == clipId }.forEach { clip ->
        add(candidateTarget(clip.startMs, SnapTargetType.ClipStart, proposed, pxPerMs, snapConfig, "Clip start"))
        add(candidateTarget(clip.startMs + clip.durationMs, SnapTargetType.ClipEnd, proposed, pxPerMs, snapConfig, "Clip end"))
      }
      timeline.markers.forEach { add(candidateTarget(it.timeMs, SnapTargetType.Marker, proposed, pxPerMs, snapConfig, it.label)) }
      timeline.transitions.forEach { transition ->
        transitionWindow(timeline, transition)?.let {
          add(candidateTarget(it.first, SnapTargetType.TransitionStart, proposed, pxPerMs, snapConfig, "Transition"))
          add(candidateTarget(it.last, SnapTargetType.TransitionEnd, proposed, pxPerMs, snapConfig, "Transition"))
        }
      }
    }
  }

  fun resolveSnapResolution(timeline: Timeline, trackType: TrackType, clipId: String, proposed: Long, snapConfig: TimelineSnapConfig = DefaultSnapConfig): SnapResolution {
    val best = collectSnapCandidates(timeline, trackType, clipId, proposed, snapConfig)
      .filter { it.distancePx <= snapConfig.baseThresholdPx.coerceAtLeast(snapConfig.maxInfluencePx) || it.distancePx <= snapConfig.maxInfluencePx }
      .maxWithOrNull(compareBy<SnapCandidateTarget> { it.strength }.thenBy { -it.distancePx })
      ?: return SnapResolution()
    val snappedTime = best.timeMs
    val pxPerMs = pixelsPerMs(timeline.zoomLevel, timeline.pixelsPerSecond)
    return SnapResolution(
      snappedTimeMs = snappedTime,
      target = best,
      appliedOffsetPx = (snappedTime - proposed) * pxPerMs,
      feedbackIntensity = best.strength,
    )
  }

  fun resolveSnap(timeline: Timeline, trackType: TrackType, clipId: String, proposed: Long): TimelineSnapResult {
    val resolution = resolveSnapResolution(timeline, trackType, clipId, proposed)
    val target = resolution.target
    return if (target != null && resolution.snappedTimeMs != null) {
      TimelineSnapResult(
        adjustedTimeMs = resolution.snappedTimeMs,
        targetTimeMs = resolution.snappedTimeMs,
        targetType = target.type,
        distanceMs = abs(resolution.snappedTimeMs - proposed),
        sourceClipId = clipId,
        targetClipId = null,
        markerId = null,
        isSnapped = true,
      )
    } else TimelineSnapResult(proposed, null, SnapTargetType.None, Long.MAX_VALUE, clipId, null, null, false)
  }

  private fun candidateTarget(timeMs: Long, type: SnapTargetType, proposed: Long, pxPerMs: Float, snapConfig: TimelineSnapConfig, label: String?): SnapCandidateTarget {
    val distancePx = abs(timeMs - proposed) * pxPerMs
    val bias = when (type) {
      SnapTargetType.Playhead -> snapConfig.playheadBiasMultiplier
      SnapTargetType.ClipStart, SnapTargetType.ClipEnd, SnapTargetType.TimelineStart, SnapTargetType.TimelineEnd -> snapConfig.edgeBiasMultiplier
      else -> 1f
    }
    val normalized = 1f - (distancePx / snapConfig.maxInfluencePx).coerceIn(0f, 1f)
    val strongBoost = if (distancePx <= snapConfig.strongThresholdPx) 1f else normalized
    return SnapCandidateTarget(type = type, timeMs = timeMs, distancePx = distancePx, strength = (strongBoost * bias).coerceIn(0f, 1.5f), label = label)
  }

  private fun hasVideoOverlap(timeline: Timeline, clipId: String, startMs: Long, durationMs: Long): Boolean {
    val endMs = startMs + durationMs
    return timeline.tracks.firstOrNull { it.type == TrackType.Video }?.clips.orEmpty().filterNot { it.id == clipId }.any { startMs < it.startMs + it.durationMs && endMs > it.startMs }
  }

  private data class LocatedClip(val track: TimelineTrack, val clip: TimelineClip)
  private fun Timeline.locateClip(clipId: String): LocatedClip? = tracks.firstNotNullOfOrNull { track -> track.clips.firstOrNull { it.id == clipId }?.let { LocatedClip(track, it) } }
  private fun Timeline.replaceClip(clipId: String, transform: (TimelineClip) -> TimelineClip): Timeline = copy(tracks = tracks.map { track -> track.copy(clips = track.clips.map { if (it.id == clipId) transform(it) else it }) })
}

fun Timeline.nextVersion(): Timeline = copy(version = version + 1)

private fun Double.pow(exponent: Double): Double = exp(kotlin.math.ln(this) * exponent)
