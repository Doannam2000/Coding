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
    assertEquals(4_056L, TimelineEngine.timeFromScroll(72f, 1f, 72f, 10_000L))
    assertEquals(2_028L, TimelineEngine.timeFromScroll(72f, 2f, 72f, 10_000L))
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
    assertEquals(468f, result.newScrollOffsetPx, 0.001f)
    assertEquals(4_500L, result.currentTimeMs)
    assertEquals(TimelineEngine.MaxZoomScale, TimelineEngine.zoomAroundFocal(result.timeline, 99f, 72f, 360f).newZoomScale)
  }

  @Test
  fun anchorZoom_preservesFocalTimeAcrossZoomLevels() {
    val anchored = TimelineEngine.anchorZoom(scrollOffsetPx = 144f, previousZoom = 1f, nextZoom = 2f, focalXpx = 72f, durationMs = 20_000, pixelsPerSecond = 72f, viewportWidthPx = 360f)

    assertEquals(3_000L, anchored.currentTimeMs)
    assertEquals(360f, anchored.nextOffsetPx, 0.001f)
    assertEquals(0f, anchored.resistanceFraction, 0.001f)
  }

  @Test
  fun repeatedPinchZoom_usesPreviousGestureZoomWithoutJumpingClipDensity() {
    val first = TimelineEngine.zoomAroundFocal(sampleTimeline().copy(durationMs = 20_000, scrollOffsetPx = 144f, zoomLevel = 1f), 1.2f, focalXpx = 160f, viewportWidthPx = 360f)
    val second = TimelineEngine.zoomAroundFocal(first.timeline, 1.2f, focalXpx = 160f, viewportWidthPx = 360f)

    assertEquals(1.2f, first.newZoomScale, 0.001f)
    assertEquals(1.44f, second.newZoomScale, 0.001f)
    assertEquals(first.currentTimeMs, second.currentTimeMs)
    assertTrue(second.newScrollOffsetPx > first.newScrollOffsetPx)
  }

  @Test
  fun dragTimelineAndResistance_stayBoundedNearEdges() {
    val offset = TimelineEngine.dragTimelineBy(0f, 80f, durationMs = 3_000, zoomScale = 1f, pixelsPerSecond = 72f, viewportWidthPx = 360f)

    assertEquals(-17.6f, offset, 0.001f)
    assertEquals(17.6f / TimelineEngine.DefaultPhysics.overscrollLimitPx, TimelineEngine.resistanceFraction(offset, 3_000, 1f, 72f, 360f), 0.001f)
    assertEquals(0f, TimelineEngine.settleScrollOffset(offset, 3_000, 1f, 72f, 360f), 0.001f)
  }

  @Test
  fun dragTimeline_returnsCenterMappedTimeAndResistance() {
    val update = TimelineEngine.dragTimeline(120f, -40f, durationMs = 20_000, zoomScale = 1f, pixelsPerSecond = 72f, viewportWidthPx = 360f)

    assertEquals(160f, update.nextOffsetPx, 0.001f)
    assertEquals(TimelineEngine.timeFromScroll(160f, 1f, 72f, 20_000, 360f), update.currentTimeMs)
    assertEquals(0f, update.resistanceFraction, 0.001f)
  }

  @Test
  fun advanceFling_deceleratesAndStopsSmoothly() {
    val frame = TimelineEngine.advanceFling(120f, 600f, 16, durationMs = 20_000, zoomScale = 1f, pixelsPerSecond = 72f, viewportWidthPx = 360f)

    assertEquals(129.6f, frame.nextOffsetPx, 0.001f)
    assertTrue(frame.nextVelocityPxPerSec < 600f)
    assertFalse(frame.isFinished)
    assertEquals(0f, frame.resistanceFraction, 0.001f)
  }

  @Test
  fun updateDragVelocity_smoothsRapidPointerNoise() {
    val first = TimelineEngine.updateDragVelocity(0f, deltaPx = -24f, deltaMs = 16)
    val smoothed = TimelineEngine.updateDragVelocity(first, deltaPx = -4f, deltaMs = 16)

    assertTrue(first > smoothed)
    assertTrue(smoothed > 0f)
  }

  @Test
  fun dragDuringFling_takesOverFromCurrentFlingOffsetWithoutJump() {
    val fling = TimelineEngine.advanceFling(120f, 600f, 16, durationMs = 20_000, zoomScale = 1f, pixelsPerSecond = 72f, viewportWidthPx = 360f)
    val takeover = TimelineEngine.dragTimeline(fling.nextOffsetPx, -10f, durationMs = 20_000, zoomScale = 1f, pixelsPerSecond = 72f, viewportWidthPx = 360f)

    assertEquals(fling.nextOffsetPx + 10f, takeover.nextOffsetPx, 0.001f)
    assertEquals(TimelineEngine.timeFromScroll(takeover.nextOffsetPx, 1f, 72f, 20_000, 360f), takeover.currentTimeMs)
  }

  @Test
  fun settleScrollFrames_returnToNearestLegalBound() {
    val frames = TimelineEngine.settleScrollFrames(scrollOffsetPx = -18f, durationMs = 3_000, zoomScale = 1f, pixelsPerSecond = 72f, viewportWidthPx = 360f)

    assertTrue(frames.isNotEmpty())
    assertEquals(0f, frames.last().offsetPx, 0.001f)
    assertEquals(0f, frames.last().resistanceFraction, 0.001f)
  }

  @Test
  fun resolveSnapResolution_prefersStrongestNearbyTarget() {
    val timeline = sampleTimeline().copy(playheadMs = 1_450, markers = listOf(TimelineMarker(id = "m1", timeMs = 1_454, label = "Beat")))
    val resolution = TimelineEngine.resolveSnapResolution(timeline, TrackType.Audio, "a1", 1_448)

    assertNotNull(resolution.target)
    assertTrue(resolution.feedbackIntensity > 0f)
    assertTrue(resolution.snappedTimeMs == 1_450L || resolution.snappedTimeMs == 1_454L)
  }

  @Test
  fun snapStrength_increasesAsTargetGetsCloserWithinPixelThreshold() {
    val timeline = sampleTimeline().copy(playheadMs = 1_500, zoomLevel = 1f)
    val far = TimelineEngine.resolveSnapResolution(timeline, TrackType.Audio, "a1", 1_300)
    val close = TimelineEngine.resolveSnapResolution(timeline, TrackType.Audio, "a1", 1_430)

    assertNotNull(far.target)
    assertNotNull(close.target)
    assertTrue(close.feedbackIntensity > far.feedbackIntensity)
    assertEquals(1_500L, close.snappedTimeMs)
  }

  @Test
  fun scrollSnapshot_keepsCenterPlayheadMappingConsistent() {
    val timeline = sampleTimeline().copy(durationMs = 20_000, scrollOffsetPx = 216f, zoomLevel = 1.5f)
    val snapshot = TimelineEngine.scrollSnapshot(timeline, viewportWidthPx = 360f)

    assertEquals(TimelineEngine.timeFromScroll(timeline.scrollOffsetPx, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs, 360f), snapshot.centerPlayheadTimeMs)
    assertEquals(2_000L, snapshot.visibleStartTimeMs)
    assertEquals(5_333L, snapshot.visibleEndTimeMs)
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
  fun magneticSnapPriority_usesFixedPriorityBeforeDistance() {
    val timeline = sampleTimeline().copy(playheadMs = 1_450, markers = listOf(TimelineMarker(id = "m1", timeMs = 1_420, label = "Beat")))

    val playheadBeatsCloserMarker = TimelineEngine.resolveMagneticSnap(timeline, TrackType.Audio, "a1", 1_421)
    val transitionTimeline = timeline.copy(transitions = listOf(Transition("t1", TransitionType.Fade, "v1", "v2", 1_000, 400)))
    val edgeBeatsTransition = TimelineEngine.resolveMagneticSnap(transitionTimeline, TrackType.Video, "v2", 1_010)

    assertEquals(MagneticSnapTargetType.PLAYHEAD, playheadBeatsCloserMarker.target?.type)
    assertEquals(MagneticSnapTargetType.NEIGHBOR_CLIP_EDGE, edgeBeatsTransition.target?.type)
  }

  @Test
  fun decayStateFrame_clampsAndUpdatesCurrentTime() {
    val state = TimelineDecayState(true, 4_000f, 4_000f, TimelineEngine.maxScrollOffsetPx(3_000, 1f, 72f, 360f) - 2f, 0f, 36f, 0L, 0L)

    val frame = TimelineEngine.decayStateFrame(state, 16, 3_000, 1f, 72f, 360f)

    assertEquals(TimelineEngine.maxScrollOffsetPx(3_000, 1f, 72f, 360f), frame.scrollOffsetPx, 0.001f)
    assertEquals(3_000L, frame.currentTimeMs)
    assertFalse(frame.isFlinging)
  }

  @Test
  fun touchInterruption_cancelsRunningModesWithoutJump() {
    val interruption = TimelineEngine.interruptTimelineGesture(TimelineGestureMode.FLINGING, 120f, 80f, 1_250L, nowMs = 2L)
    val state = TimelineEngine.scrollJobAfterInterruption(TimelineScrollJobState(activeMode = TimelineGestureMode.FLINGING, hasRunningDecayJob = true), interruption)

    assertTrue(interruption.shouldCancelScrollJob)
    assertEquals(80f, state.lastStableScrollOffsetPx)
    assertEquals(1_250L, state.lastStableCurrentTimeMs)
    assertFalse(state.hasRunningDecayJob)
  }

  @Test
  fun autoScrollDirectionAndVelocity_areCappedByEdgeZone() {
    val left = TimelineEngine.resolveAutoScroll(4f, 320f, "v1", 0L)
    val none = TimelineEngine.resolveAutoScroll(160f, 320f, "v1", 0L)

    assertEquals(AutoScrollDirection.LEFT, left.direction)
    assertTrue(kotlin.math.abs(left.velocityPxPerSecond) <= TimelineEngine.DefaultPhysics.maxAutoScrollVelocityPxPerSec)
    assertFalse(none.isAutoScrolling)
  }

  @Test
  fun clipBoundaryResistance_rejectsOutsideProject() {
    val timeline = sampleTimeline()
    val beforeStart = TimelineEngine.resolveClipBoundaryState(timeline, "a1", -250, 0)
    val beyondEnd = TimelineEngine.resolveClipBoundaryState(timeline, "a1", timeline.durationMs + 1_000, 0)

    assertTrue(beforeStart.isBeyondStart)
    assertTrue(beyondEnd.isBeyondEnd)
    assertFalse(TimelineEngine.resolveDraggedClip(timeline, "a1", -250).isValid)
  }

  @Test
  fun trimPreviewScrub_usesActiveBoundaryTime() {
    val timeline = sampleTimeline()
    val left = TimelineEngine.resolveTrimPreviewScrub(timeline, "v1", TrimHandle.Left, 300)
    val right = TimelineEngine.resolveTrimPreviewScrub(timeline, "v1", TrimHandle.Right, 900)

    assertEquals(left.proposedBoundaryMs, left.previewTimeMs)
    assertEquals(right.proposedBoundaryMs, right.previewTimeMs)
  }

  @Test
  fun snapSettleAndInvalidRecovery_areNonOvershootingAndNonCommitting() {
    val timeline = sampleTimeline().copy(playheadMs = 1_450)
    val snap = TimelineEngine.resolveMagneticSnap(timeline, TrackType.Audio, "a1", 1_430)
    val settle = TimelineEngine.resolveSnapReleaseSettle("a1", null, 1_430, snap, nowMs = 0L)
    val midway = TimelineEngine.settleTimeAt(settle.fromTimeMs, settle.toTimeMs, elapsedMs = 55L, durationMs = settle.durationMs)
    val recovery = TimelineEngine.invalidDropRecovery("a1", -200, 800, 0, 1_000)

    assertTrue(settle.durationMs in 80..140)
    assertTrue(midway in settle.fromTimeMs..settle.toTimeMs)
    assertFalse(recovery.shouldCommitTimelineState)
  }

  @Test
  fun selectionStability_clearsOnlyWhenIdle() {
    val selected = SelectionStabilityState(selectedClipId = "v1")
    val draggingTap = TimelineEngine.selectionAfterGesture(selected, TimelineGestureMode.DRAGGING_CLIP, emptyTap = true)
    val idleTap = TimelineEngine.selectionAfterGesture(selected, TimelineGestureMode.IDLE, emptyTap = true)

    assertEquals("v1", draggingTap.selectedClipId)
    assertNull(idleTap.selectedClipId)
  }

  @Test
  fun resolveDraggedClip_returnsSnappedPreviewAndValidity() {
    val timeline = sampleTimeline().copy(playheadMs = 1_450)

    val snapped = TimelineEngine.resolveDraggedClip(timeline, "a1", 1_420)
    val blocked = TimelineEngine.resolveDraggedClip(timeline, "v2", 500)

    assertEquals(1_450L, snapped.resolvedStartTimeMs)
    assertTrue(snapped.snapResolution.feedbackIntensity > 0f)
    assertFalse(blocked.isValid)
  }

  @Test
  fun resolveTrimGesture_enforcesMinimumDurationAndSnap() {
    val timeline = sampleTimeline().copy(playheadMs = 1_450)

    val snappedEnd = TimelineEngine.resolveTrimGesture(timeline, "a1", TrimHandle.Right, 1_420)
    val minimumLeft = TimelineEngine.resolveTrimGesture(timeline, "v1", TrimHandle.Left, 900)

    assertEquals(1_450L, snappedEnd.resolvedTimeMs)
    assertEquals(400L, minimumLeft.resolvedTimeMs)
    assertTrue(minimumLeft.isValid)
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

  @Test
  fun previewSeekThrottle_limitsIntermediateSeeksButForcesFinalFrame() {
    val initial = PreviewSeekThrottleState(minIntervalMs = 48L)
    val first = TimelineEngine.previewSeekDecision(initial, 1_000L, nowMs = 0L, PreviewSeekSource.TIMELINE_SCROLL)
    val throttled = TimelineEngine.previewSeekDecision(first.state, 1_080L, nowMs = 16L, PreviewSeekSource.TIMELINE_SCROLL)
    val final = TimelineEngine.previewSeekDecision(throttled.state, 1_120L, nowMs = 24L, PreviewSeekSource.TIMELINE_SCROLL, forceFinalSeek = true)

    assertEquals(1_000L, first.seekTimeMs)
    assertNull(throttled.seekTimeMs)
    assertEquals(1_080L, throttled.state.pendingSeekMs)
    assertEquals(1_120L, final.seekTimeMs)
    assertTrue(final.shouldSeekImmediately)
  }

  @Test
  fun exactFrameSeek_mapsClampedScrollStartMiddleAndEnd() {
    val middle = TimelineEngine.exactFrameSeekFromScroll(72f, 1f, 72f, 3_000L, 72f, TimelineGestureMode.SCROLLING, true)
    val start = TimelineEngine.exactFrameSeekFromScroll(-200f, 1f, 72f, 3_000L, 72f, TimelineGestureMode.SCROLLING, true)
    val end = TimelineEngine.exactFrameSeekFromScroll(999f, 1f, 72f, 3_000L, 72f, TimelineGestureMode.FLINGING, true)

    assertEquals(1_500L, middle.currentTimeMs)
    assertEquals(500L, start.currentTimeMs)
    assertEquals(3_000L, end.currentTimeMs)
    assertTrue(end.shouldSeekImmediately)
  }

  @Test
  fun playbackEditLock_pausesBeforeDirectManipulation() {
    val lock = TimelineEngine.resolvePlaybackEditLock(true, TimelineGestureMode.TRIMMING_CLIP)

    assertTrue(lock.shouldPauseBeforeEdit)
    assertFalse(lock.shouldBlockEditGesture)
    assertEquals("Playback paused", lock.lockReason)
  }

  @Test
  fun timelinePointerBounds_rejectsTouchesOutsideTimeline() {
    val inside = TimelineEngine.timelinePointerBounds(10f, 20f, 210f, 120f, 80f, 60f)
    val outside = TimelineEngine.timelinePointerBounds(10f, 20f, 210f, 120f, 8f, 60f)

    assertTrue(inside.shouldAcceptTimelineGesture)
    assertFalse(outside.shouldAcceptTimelineGesture)
  }

  @Test
  fun handleTouchTarget_expandsInvisibleBoundsAndResolvesOverlap() {
    val left = TimelineEngine.handleTouchTarget("v1", TrimHandle.Left, 0f, 4f, pointerXpx = -12f, minimumTouchTargetPx = 44f)
    val right = TimelineEngine.handleTouchTarget("v1", TrimHandle.Right, 20f, 24f, pointerXpx = 18f, minimumTouchTargetPx = 44f)

    assertTrue(left.isPointerInsideTouchTarget)
    assertEquals(TrimHandle.Right, TimelineEngine.resolveOverlappingHandle(left, right, 18f))
  }

  @Test
  fun touchSlop_keepsSmallMovementAsTapUntilThreshold() {
    val tap = TimelineEngine.touchSlopGate(0f, 0f, 3f, 4f, 8f, TimelineGestureMode.DRAGGING_CLIP)
    val drag = TimelineEngine.touchSlopGate(0f, 0f, 8f, 1f, 8f, TimelineGestureMode.DRAGGING_CLIP)

    assertFalse(tap.hasExceededTouchSlop)
    assertEquals(TimelineGestureMode.IDLE, tap.confirmedGestureMode)
    assertTrue(drag.hasExceededTouchSlop)
    assertEquals(TimelineGestureMode.DRAGGING_CLIP, drag.confirmedGestureMode)
  }

  @Test
  fun overlayHitTesting_selectsHighestVisibleZIndexWithStableTieBreak() {
    val clips = listOf(
      TimelineClip("low", clipType = ClipType.Text, title = "Low", startMs = 0, durationMs = 1_000, zIndex = 1),
      TimelineClip("top", clipType = ClipType.Text, title = "Top", startMs = 0, durationMs = 1_000, zIndex = 4),
      TimelineClip("hidden", clipType = ClipType.Text, title = "Hidden", startMs = 0, durationMs = 1_000, zIndex = 9, transform = TransformState(opacity = 0f)),
    )
    val result = TimelineEngine.overlayHitTest(TimelineEngine.overlayHitTargets(clips, 50f, 50f, 100f, 100f), 50f, 50f)

    assertEquals("top", result.selectedOverlayId)
  }

  @Test
  fun overlayBoundary_allowsPartialOutsideButPreservesSelectableArea() {
    val partial = TimelineEngine.resolveOverlayCanvasBoundary("txt", -20f, 50f, 112f, 48f, 1f, 0f, 200f, 120f)
    val clamped = TimelineEngine.resolveOverlayCanvasBoundary("txt", -300f, -300f, 112f, 48f, 1f, 0f, 200f, 120f)

    assertTrue(partial.showBoundaryGuide)
    assertTrue(partial.isSelectableAreaPreserved)
    assertTrue(clamped.isSelectableAreaPreserved)
    assertTrue(clamped.resolvedCenterX > -300f)
    assertTrue(clamped.resolvedCenterY > -300f)
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
