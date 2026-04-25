package com.example.clipystudio.data

import java.util.UUID
import kotlin.math.abs
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

object TimelineEngine {
  const val MinClipDurationMs: Long = 600
  const val DefaultPixelsPerSecond: Float = 72f
  private const val SnapThresholdMs: Long = 180

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
    version = timeline.version,
  )

  fun timeFromScroll(scrollOffsetPx: Float, zoomScale: Float, pixelsPerSecond: Float, durationMs: Long): Long {
    val pxPerMs = (pixelsPerSecond * zoomScale).coerceAtLeast(1f) / 1_000f
    return (scrollOffsetPx / pxPerMs).roundToLong().coerceIn(0L, durationMs)
  }

  fun scrollFromTime(timeMs: Long, zoomScale: Float, pixelsPerSecond: Float): Float = timeMs.coerceAtLeast(0L) * ((pixelsPerSecond * zoomScale).coerceAtLeast(1f) / 1_000f)

  fun withScroll(timeline: Timeline, scrollOffsetPx: Float): Timeline {
    val current = timeFromScroll(scrollOffsetPx, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs)
    return timeline.copy(scrollOffsetPx = scrollOffsetPx.coerceAtLeast(0f), playheadMs = current).nextVersion()
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
    val snapped = snapStart(timeline, located.track.type, original.id, proposed)
    if (located.track.type == TrackType.Video && hasVideoOverlap(timeline, original.id, snapped, original.durationMs)) {
      return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Video clips cannot overlap")
    }
    val next = timeline.replaceClip(clipId) { it.copy(startMs = snapped) }.recalculateDuration().nextVersion()
    return TimelineMutationResult(next, clipId, next.playheadMs, listOf(clipId))
  }

  fun trimClip(timeline: Timeline, clipId: String, handle: TrimHandle, deltaMs: Long): TimelineMutationResult {
    val located = timeline.locateClip(clipId) ?: return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Clip not found")
    val clip = located.clip
    val nextClip = when (handle) {
      TrimHandle.Left -> {
        val shift = deltaMs.coerceIn(-clip.sourceInMs, clip.durationMs - MinClipDurationMs)
        clip.copy(startMs = (clip.startMs + shift).coerceAtLeast(0L), sourceInMs = (clip.sourceInMs + shift).coerceAtLeast(0L), durationMs = clip.durationMs - shift)
      }
      TrimHandle.Right -> clip.copy(durationMs = (clip.durationMs + deltaMs).coerceAtLeast(MinClipDurationMs))
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

  private fun Timeline.projectClips(type: TrackType): List<ProjectTimelineClip> = tracks.firstOrNull { it.type == type }?.clips.orEmpty().map { clip ->
    ProjectTimelineClip(clip.id, clip.clipType, clip.assetId, clip.startMs, clip.durationMs, clip.sourceInMs, 0, clip.videoProperties.speed, clip.audioProperties.volume, clip.transform, clip.filterAdjustments.filterId, clip.filterAdjustments.filterId, type, 0)
  }

  private fun snapStart(timeline: Timeline, trackType: TrackType, clipId: String, proposed: Long): Long {
    val targets = buildList {
      add(timeline.playheadMs to SnapTarget.Playhead)
      timeline.tracks.firstOrNull { it.type == trackType }?.clips.orEmpty().filterNot { it.id == clipId }.forEach { clip ->
        add(clip.startMs to SnapTarget.ClipStart)
        add((clip.startMs + clip.durationMs) to SnapTarget.ClipEnd)
      }
    }
    return targets.minByOrNull { abs(it.first - proposed) }?.takeIf { abs(it.first - proposed) <= SnapThresholdMs }?.first ?: proposed
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
