package com.example.clipystudio.data

import java.util.UUID
import kotlin.math.abs
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
enum class SnapTargetType { None, Playhead, ClipStart, ClipEnd, Marker, TransitionStart, TransitionEnd }
data class TimelineMarker(val id: String = UUID.randomUUID().toString(), val timeMs: Long, val label: String, val color: Long? = null)
data class ActiveTimelineComposition(val currentTimeMs: Long, val video: ActiveClip?, val audio: List<ActiveClip>, val text: List<ActiveClip>, val stickers: List<ActiveClip>, val overlays: List<ActiveClip>, val effects: List<ActiveClip>, val transition: ActiveTransition?)
data class ActiveClip(val clipId: String, val trackType: TrackType, val trackIndex: Int, val localTimeMs: Long, val sourceTimeMs: Long, val transform: TransformState, val opacity: Float, val volume: Float)
data class ActiveTransition(val transitionId: String, val fromClipId: String, val toClipId: String, val type: TransitionType, val startTimeMs: Long, val endTimeMs: Long, val progress: Float)

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
  const val MinZoomScale: Float = 0.65f
  const val MaxZoomScale: Float = 3f
  private const val SnapThresholdPx: Float = 14f

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

  fun timeFromScroll(scrollOffsetPx: Float, zoomScale: Float, pixelsPerSecond: Float, durationMs: Long): Long {
    val pxPerMs = (pixelsPerSecond * zoomScale).coerceAtLeast(1f) / 1_000f
    return (scrollOffsetPx / pxPerMs).roundToLong().coerceIn(0L, durationMs)
  }

  fun scrollFromTime(timeMs: Long, zoomScale: Float, pixelsPerSecond: Float): Float = timeMs.coerceAtLeast(0L) * ((pixelsPerSecond * zoomScale).coerceAtLeast(1f) / 1_000f)

  fun zoomAroundFocal(timeline: Timeline, zoomDelta: Float, focalXpx: Float, viewportWidthPx: Float): TimelineZoomResult {
    val previousZoom = timeline.zoomLevel
    val nextZoom = (previousZoom * zoomDelta.coerceIn(0.25f, 4f)).coerceIn(MinZoomScale, MaxZoomScale)
    val previousScroll = timeline.scrollOffsetPx
    val pxPerMsBefore = (timeline.pixelsPerSecond * previousZoom).coerceAtLeast(1f) / 1_000f
    val pxPerMsAfter = (timeline.pixelsPerSecond * nextZoom).coerceAtLeast(1f) / 1_000f
    val focalTime = ((previousScroll + focalXpx.coerceIn(0f, viewportWidthPx.coerceAtLeast(1f))) / pxPerMsBefore).roundToLong().coerceIn(0L, timeline.durationMs)
    val maxScroll = (timeline.durationMs * pxPerMsAfter - viewportWidthPx).coerceAtLeast(0f)
    val nextScroll = (focalTime * pxPerMsAfter - focalXpx).coerceIn(0f, maxScroll)
    val next = timeline.copy(zoomLevel = nextZoom, scrollOffsetPx = nextScroll).nextVersion()
    return TimelineZoomResult(next, previousZoom, nextZoom, previousScroll, nextScroll, focalTime, next.playheadMs)
  }

  fun withScroll(timeline: Timeline, scrollOffsetPx: Float): Timeline {
    val current = timeFromScroll(scrollOffsetPx, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs)
    return timeline.copy(scrollOffsetPx = scrollOffsetPx.coerceAtLeast(0f), playheadMs = current).nextVersion()
  }

  fun visibleRange(timeline: Timeline, viewportWidthPx: Float, prefetchPx: Float = 160f): VisibleTimelineRange {
    val startPx = (timeline.scrollOffsetPx - prefetchPx).coerceAtLeast(0f)
    val endPx = timeline.scrollOffsetPx + viewportWidthPx.coerceAtLeast(1f) + prefetchPx
    return VisibleTimelineRange(
      startTimeMs = timeFromScroll(startPx, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs),
      endTimeMs = timeFromScroll(endPx, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs),
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
    val pxPerMs = (projectTimeline.pixelsPerSecond * projectTimeline.zoomScale).coerceAtLeast(1f) / 1_000f
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
    val proposed = (original.startMs + deltaMs).coerceAtLeast(0L)
    val snapped = resolveSnap(timeline, located.track.type, original.id, proposed).adjustedTimeMs
    if (located.track.type == TrackType.Video && hasVideoOverlap(timeline, original.id, snapped, original.durationMs)) {
      return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Video clips cannot overlap")
    }
    val next = timeline.replaceClip(clipId) { it.copy(startMs = snapped) }.recalculateDuration().nextVersion()
    return TimelineMutationResult(next, clipId, next.playheadMs, listOf(clipId))
  }

  fun trimClip(timeline: Timeline, clipId: String, handle: TrimHandle, deltaMs: Long): TimelineMutationResult {
    val located = timeline.locateClip(clipId) ?: return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Clip not found")
    val clip = located.clip
    val rawClip = when (handle) {
      TrimHandle.Left -> {
        val shift = deltaMs.coerceIn(-clip.sourceInMs, clip.durationMs - MinClipDurationMs)
        clip.copy(startMs = (clip.startMs + shift).coerceAtLeast(0L), sourceInMs = (clip.sourceInMs + shift).coerceAtLeast(0L), durationMs = clip.durationMs - shift)
      }
      TrimHandle.Right -> clip.copy(durationMs = (clip.durationMs + deltaMs).coerceAtLeast(MinClipDurationMs))
    }
    val nextClip = when (handle) {
      TrimHandle.Left -> {
        val snappedStart = resolveSnap(timeline, located.track.type, clip.id, rawClip.startMs).adjustedTimeMs
        val end = clip.startMs + clip.durationMs
        rawClip.copy(startMs = snappedStart, sourceInMs = (clip.sourceInMs + (snappedStart - clip.startMs)).coerceAtLeast(0L), durationMs = (end - snappedStart).coerceAtLeast(MinClipDurationMs))
      }
      TrimHandle.Right -> {
        val snappedEnd = resolveSnap(timeline, located.track.type, clip.id, rawClip.startMs + rawClip.durationMs).adjustedTimeMs
        rawClip.copy(durationMs = (snappedEnd - rawClip.startMs).coerceAtLeast(MinClipDurationMs))
      }
    }
    if (located.track.type == TrackType.Video && hasVideoOverlap(timeline, clip.id, nextClip.startMs, nextClip.durationMs)) {
      return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Trim would overlap video")
    }
    val next = timeline.replaceClip(clipId) { nextClip }.recalculateDuration().nextVersion()
    return TimelineMutationResult(next, clipId, next.playheadMs, listOf(clipId))
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

  fun resolveSnap(timeline: Timeline, trackType: TrackType, clipId: String, proposed: Long): TimelineSnapResult {
    val thresholdMs = (SnapThresholdPx / ((timeline.pixelsPerSecond * timeline.zoomLevel).coerceAtLeast(1f) / 1_000f)).roundToLong().coerceAtLeast(80L)
    val targets = buildList {
      add(SnapCandidate(timeline.playheadMs, SnapTargetType.Playhead, null, null))
      timeline.tracks.firstOrNull { it.type == trackType }?.clips.orEmpty().filterNot { it.id == clipId }.forEach { clip ->
        add(SnapCandidate(clip.startMs, SnapTargetType.ClipStart, clip.id, null))
        add(SnapCandidate(clip.startMs + clip.durationMs, SnapTargetType.ClipEnd, clip.id, null))
      }
      timeline.markers.forEach { add(SnapCandidate(it.timeMs, SnapTargetType.Marker, null, it.id)) }
      timeline.transitions.forEach { transition -> transitionWindow(timeline, transition)?.let { add(SnapCandidate(it.first, SnapTargetType.TransitionStart, transition.id, null)); add(SnapCandidate(it.last, SnapTargetType.TransitionEnd, transition.id, null)) } }
    }
    val best = targets.minWithOrNull(compareBy<SnapCandidate> { abs(it.timeMs - proposed) }.thenBy { it.type.ordinal })
    return if (best != null && abs(best.timeMs - proposed) <= thresholdMs) {
      TimelineSnapResult(best.timeMs, best.timeMs, best.type, abs(best.timeMs - proposed), clipId, best.clipOrTransitionId, best.markerId, true)
    } else TimelineSnapResult(proposed, null, SnapTargetType.None, Long.MAX_VALUE, clipId, null, null, false)
  }

  private data class SnapCandidate(val timeMs: Long, val type: SnapTargetType, val clipOrTransitionId: String?, val markerId: String?)

  private fun hasVideoOverlap(timeline: Timeline, clipId: String, startMs: Long, durationMs: Long): Boolean {
    val endMs = startMs + durationMs
    return timeline.tracks.firstOrNull { it.type == TrackType.Video }?.clips.orEmpty().filterNot { it.id == clipId }.any { startMs < it.startMs + it.durationMs && endMs > it.startMs }
  }

  private data class LocatedClip(val track: TimelineTrack, val clip: TimelineClip)
  private fun Timeline.locateClip(clipId: String): LocatedClip? = tracks.firstNotNullOfOrNull { track -> track.clips.firstOrNull { it.id == clipId }?.let { LocatedClip(track, it) } }
  private fun Timeline.replaceClip(clipId: String, transform: (TimelineClip) -> TimelineClip): Timeline = copy(tracks = tracks.map { track -> track.copy(clips = track.clips.map { if (it.id == clipId) transform(it) else it }) })
}

fun Timeline.nextVersion(): Timeline = copy(version = version + 1)
