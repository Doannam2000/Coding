package com.example.clipystudio.data

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class TimelineEngineTest {
  @Test
  fun timeFromScroll_isStableAndClamped() {
    assertEquals(1_000L, TimelineEngine.timeFromScroll(72f, 1f, 72f, 10_000L))
    assertEquals(500L, TimelineEngine.timeFromScroll(72f, 2f, 72f, 10_000L))
    assertEquals(10_000L, TimelineEngine.timeFromScroll(999_999f, 1f, 72f, 10_000L))
  }

  @Test
  fun calculateLayout_usesStartDurationZoomAndRows() {
    val timeline = TimelineEngine.toProjectTimeline(sampleTimeline())
    val bounds = TimelineEngine.calculateLayout(timeline, rowHeightPx = 40f, rowGapPx = 4f)
    val video = bounds.first { it.clipId == "v1" }
    val audio = bounds.first { it.clipId == "a1" }

    assertEquals(72f, video.widthPx)
    assertEquals(0f, video.leftPx)
    assertEquals(44f, audio.topPx)
  }

  @Test
  fun hitTest_prioritizesTrimHandlesBeforeBody() {
    val bounds = listOf(ClipLayoutBounds("clip", TrackType.Video, 0, 10f, 110f, 100f, 0f, 40f, 0, 1_000))

    assertEquals(HitTestType.LeftTrimHandle, TimelineEngine.hitTest(bounds, 14f, 12f, 12f, 0).type)
    assertEquals(HitTestType.RightTrimHandle, TimelineEngine.hitTest(bounds, 104f, 12f, 12f, 0).type)
    assertEquals(HitTestType.ClipBody, TimelineEngine.hitTest(bounds, 50f, 12f, 12f, 0).type)
    assertEquals(HitTestType.Empty, TimelineEngine.hitTest(bounds, 50f, 90f, 12f, 0).type)
  }

  @Test
  fun dragVideo_rejectsOverlapButAudioAllowsOverlap() {
    val timeline = sampleTimeline()
    val rejected = TimelineEngine.dragClip(timeline, "v2", -500)
    val allowed = TimelineEngine.dragClip(timeline, "a1", 500)

    assertNotNull(rejected.rejectedReason)
    assertNull(allowed.rejectedReason)
    assertEquals(500L, allowed.timeline.tracks.first { it.type == TrackType.Audio }.clips.first().startMs)
  }

  @Test
  fun trimAndSplit_preserveValidClipDurations() {
    val splitTimeline = sampleTimeline().copy(
      selectedClipId = "v1",
      playheadMs = 700,
      tracks = sampleTimeline().tracks.map { track ->
        if (track.type == TrackType.Video) track.copy(clips = listOf(
          TimelineClip("v1", clipType = ClipType.Video, title = "One", startMs = 0, durationMs = 1_500),
          TimelineClip("v2", clipType = ClipType.Video, title = "Two", startMs = 1_500, durationMs = 1_000),
        )) else track
      },
    )
    val trimmed = TimelineEngine.trimClip(sampleTimeline(), "v1", TrimHandle.Right, -500)
    val split = TimelineEngine.splitSelectedClip(splitTimeline)

    assertEquals(TimelineEngine.MinClipDurationMs, trimmed.timeline.tracks.first { it.type == TrackType.Video }.clips.first { it.id == "v1" }.durationMs)
    assertNull(split.rejectedReason)
    assertEquals(3, split.timeline.tracks.first { it.type == TrackType.Video }.clips.size)
  }

  @Test
  fun reorderVideo_producesContinuousMainTrack() {
    val result = TimelineEngine.reorderVideoClip(sampleTimeline(), "v2", 0)
    val clips = result.timeline.tracks.first { it.type == TrackType.Video }.clips

    assertFalse(result.rejectedReason != null)
    assertEquals("v2", clips[0].id)
    assertEquals(0L, clips[0].startMs)
    assertEquals(clips[0].durationMs, clips[1].startMs)
    assertTrue(result.timeline.durationMs >= clips.sumOf { it.durationMs })
  }

  @Test
  fun zoomAroundFocal_preservesFocalTimeAndClamps() {
    val timeline = sampleTimeline().copy(durationMs = 20_000, scrollOffsetPx = 144f, zoomLevel = 1f)
    val result = TimelineEngine.zoomAroundFocal(timeline, 2f, focalXpx = 72f, viewportWidthPx = 360f)

    assertEquals(3_000L, result.focalTimeMs)
    assertEquals(2f, result.newZoomScale, 0.001f)
    assertEquals(360f, result.newScrollOffsetPx, 0.001f)
    assertEquals(TimelineEngine.MaxZoomScale, TimelineEngine.zoomAroundFocal(result.timeline, 99f, 72f, 360f).newZoomScale)
  }

  @Test
  fun visibleRangeAndThumbnailPlanning_areBoundedToVisibleMedia() {
    val timeline = sampleTimeline().copy(scrollOffsetPx = 72f, zoomLevel = 1f)
    val range = TimelineEngine.visibleRange(timeline, viewportWidthPx = 72f, prefetchPx = 0f)
    val requests = TimelineEngine.planThumbnailRequests(timeline, range)

    assertEquals(1_000L, range.startTimeMs)
    assertEquals(2_000L, range.endTimeMs)
    assertEquals(listOf("v2"), requests.map { it.clipId })
    assertTrue(TimelineEngine.planThumbnailRequests(timeline, range, mapOf(requests.first().cacheKey to TimelineThumbnailState("v2", requests.first().cacheKey, ThumbnailStatus.Ready, 0, 0))).isEmpty())
  }

  @Test
  fun snapEngine_usesPlayheadMarkersAndNeighborEdges() {
    val timeline = sampleTimeline().copy(playheadMs = 1_450, markers = listOf(TimelineMarker(id = "m1", timeMs = 2_000, label = "Beat")))

    assertEquals(SnapTargetType.Playhead, TimelineEngine.resolveSnap(timeline, TrackType.Audio, "a1", 1_420).targetType)
    assertEquals(SnapTargetType.Marker, TimelineEngine.resolveSnap(timeline, TrackType.Audio, "a1", 1_960).targetType)
    assertEquals(SnapTargetType.ClipEnd, TimelineEngine.resolveSnap(timeline, TrackType.Video, "v2", 1_020).targetType)
  }

  @Test
  fun activeComposition_transitionAndKeyframes_areResolvedFromPlayhead() {
    val keyed = TimelineClip("txt", clipType = ClipType.Text, title = "Title", startMs = 0, durationMs = 2_000, transform = TransformState(opacity = 0.2f), keyframes = listOf(
      Keyframe(timeMs = 0, property = KeyframeProperty.Opacity, value = 0.2f),
      Keyframe(timeMs = 1_000, property = KeyframeProperty.Opacity, value = 1f),
    ))
    val timeline = sampleTimeline().copy(
      playheadMs = 900,
      transitions = listOf(Transition("t1", TransitionType.Fade, "v1", "v2", 800, 1_000)),
      tracks = sampleTimeline().tracks.map { if (it.type == TrackType.Text) it.copy(clips = listOf(keyed)) else it },
    )
    val composition = TimelineEngine.resolveActiveComposition(timeline)

    assertEquals("v1", composition.video?.clipId)
    assertNotNull(composition.transition)
    assertEquals(0.92f, composition.text.first().opacity, 0.001f)
  }

  private fun sampleTimeline() = Timeline(
    durationMs = 3_000,
    tracks = listOf(
      TimelineTrack("video", TrackType.Video, "Video", 0, listOf(
        TimelineClip("v1", assetId = "asset-v1", clipType = ClipType.Video, title = "One", startMs = 0, durationMs = 1_000),
        TimelineClip("v2", assetId = "asset-v2", clipType = ClipType.Video, title = "Two", startMs = 1_000, durationMs = 1_000),
      )),
      TimelineTrack("audio", TrackType.Audio, "Audio", 1, listOf(TimelineClip("a1", clipType = ClipType.Audio, title = "Audio", startMs = 0, durationMs = 1_500))),
      TimelineTrack("text", TrackType.Text, "Text", 2, emptyList()),
      TimelineTrack("sticker", TrackType.Sticker, "Sticker", 3, emptyList()),
      TimelineTrack("overlay", TrackType.Overlay, "Overlay", 4, emptyList()),
      TimelineTrack("effect", TrackType.Effect, "Effect", 5, emptyList()),
    ),
  )
}
