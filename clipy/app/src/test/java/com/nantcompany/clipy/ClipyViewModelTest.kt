package com.nantcompany.clipy

import com.nantcompany.clipy.model.CropRatio
import com.nantcompany.clipy.model.ExportFormat
import com.nantcompany.clipy.model.Mp4Quality
import com.nantcompany.clipy.model.ProjectDraft
import com.nantcompany.clipy.model.AudioSegmentUi
import com.nantcompany.clipy.model.buildWaveformSamples
import com.nantcompany.clipy.model.buildExportPlan
import com.nantcompany.clipy.model.boundedTrimEndMs
import com.nantcompany.clipy.model.boundedTrimStartMs
import com.nantcompany.clipy.model.editorTimelineUiState
import com.nantcompany.clipy.model.sanitizeTimeline
import com.nantcompany.clipy.model.sanitizeOutputName
import com.nantcompany.clipy.model.shouldDispatchTimelinePreviewSeek
import com.nantcompany.clipy.model.splitAudioSegments
import com.nantcompany.clipy.model.snapTimelineMs
import com.nantcompany.clipy.model.timelineScrollForPlayhead
import com.nantcompany.clipy.model.timelineStripFrameCount
import com.nantcompany.clipy.model.timelinePrefetchRange
import com.nantcompany.clipy.model.timelineMsToTrackPx
import com.nantcompany.clipy.model.timelineTrackPxToMs
import com.nantcompany.clipy.model.timelineThumbnailCount
import com.nantcompany.clipy.model.validateExport
import com.nantcompany.clipy.shouldLoadVideoTimelineFrames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipyViewModelTest {
  @Test
  fun projectDraft_defaultsMatchEditorFlow() {
    val draft = ProjectDraft()
    assertEquals(CropRatio.Story, draft.cropRatio)
    assertEquals(ExportFormat.Gif, draft.exportFormat)
    assertEquals(Mp4Quality.Balanced, draft.mp4Quality)
  }

  @Test
  fun sanitizeTimeline_clampsOutOfBoundsValues() {
    val timeline = sanitizeTimeline(
      durationMs = 5_000L,
      trimStartMs = -200L,
      trimEndMs = 12_000L,
      playheadMs = 9_000L,
      zoom = 9f,
    )

    assertEquals(0L, timeline.trimStartMs)
    assertEquals(5_000L, timeline.trimEndMs)
    assertEquals(5_000L, timeline.playheadMs)
    assertEquals(6f, timeline.zoom)
  }

  @Test
  fun snapTimelineMs_roundsToNearestFrameStep() {
    assertEquals(66L, snapTimelineMs(70L))
    assertEquals(99L, snapTimelineMs(100L))
  }

  @Test
  fun validateExport_rejectsMissingSourceAndBadGifSettings() {
    val missingSource = ProjectDraft()
    assertFalse(missingSource.validateExport().isValid)

    val badGif = ProjectDraft(sourceUri = "content://clip", gifFps = 17)
    assertFalse(badGif.validateExport().isValid)
  }

  @Test
  fun validateExport_rejectsImageSourcesInVideoEditorFlow() {
    val imageDraft = ProjectDraft(
      sourceUri = "content://photo",
      sourceMediaType = "image",
      trimStartMs = 0L,
      trimEndMs = 2_000L,
    )

    assertFalse(imageDraft.validateExport().isValid)
    assertEquals("Only video clips can be exported in the current editor flow.", imageDraft.validateExport().message)
  }

  @Test
  fun shouldLoadVideoTimelineFrames_onlyAllowsNonBlankVideoSources() {
    assertTrue(shouldLoadVideoTimelineFrames(sourceUri = "content://clip", isVideoSource = true))
    assertFalse(shouldLoadVideoTimelineFrames(sourceUri = "content://photo", isVideoSource = false))
    assertFalse(shouldLoadVideoTimelineFrames(sourceUri = "", isVideoSource = true))
  }

  @Test
  fun validateExport_acceptsSupportedMp4Draft() {
    val draft = ProjectDraft(
      sourceUri = "content://clip",
      exportFormat = ExportFormat.Mp4,
      trimStartMs = 0L,
      trimEndMs = 2_000L,
    )

    assertTrue(draft.validateExport().isValid)
  }

  @Test
  fun validateExport_rejectsLongGifDraft() {
    val draft = ProjectDraft(
      sourceUri = "content://clip",
      exportFormat = ExportFormat.Gif,
      trimStartMs = 0L,
      trimEndMs = 16_000L,
    )

    assertFalse(draft.validateExport().isValid)
  }

  @Test
  fun sanitizeOutputName_removesUnsafeCharacters() {
    assertEquals("My_clip_v2", sanitizeOutputName("  My clip #v2  "))
  }

  @Test
  fun buildExportPlan_includesPalettePipelineForGif() {
    val plan = buildExportPlan(
      ProjectDraft(
        sourceUri = "content://clip",
        exportFormat = ExportFormat.Gif,
        trimStartMs = 0L,
        trimEndMs = 5_000L,
        watermarkText = "Clipy",
      ),
    )

    assertTrue(plan.ffmpegCommand.contains("palettegen"))
    assertTrue(plan.ffmpegCommand.contains("drawtext"))
  }

  @Test
  fun timelineHelpers_scaleWithZoomAndCenterPlayhead() {
    assertTrue(timelineThumbnailCount(4f, 1080) > timelineThumbnailCount(1f, 1080))
    assertEquals(440, timelineScrollForPlayhead(0.5f, 1200, 320))
  }

  @Test
  fun timelineTrackMapping_roundTripsAcrossDuration() {
    val offsetPx = timelineMsToTrackPx(timeMs = 5_000L, durationMs = 10_000L, trackWidthPx = 800f)

    assertEquals(400f, offsetPx)
    assertEquals(5_000L, timelineTrackPxToMs(offsetPx = offsetPx, durationMs = 10_000L, trackWidthPx = 800f))
  }

  @Test
  fun timelineStripFrameCount_scalesWithZoomAndDuration() {
    assertTrue(timelineStripFrameCount(durationMs = 18_000L, zoom = 4f) > timelineStripFrameCount(durationMs = 18_000L, zoom = 1f))
    assertTrue(timelineStripFrameCount(durationMs = 18_000L, zoom = 1f) > timelineStripFrameCount(durationMs = 3_000L, zoom = 1f))
  }

  @Test
  fun timelinePrefetchRange_expandsAroundVisibleWindowAndClamps() {
    assertEquals(0..7, timelinePrefetchRange(visibleStartIndex = 2, visibleEndIndex = 4, frameCount = 16, preloadCount = 3))
    assertEquals(0..2, timelinePrefetchRange(visibleStartIndex = 0, visibleEndIndex = 1, frameCount = 3, preloadCount = 4))
  }

  @Test
  fun boundedTrimHelpers_preserveMinimumGap() {
    assertEquals(
      3_750L,
      boundedTrimStartMs(
        offsetPx = 390f,
        durationMs = 4_000L,
        trackWidthPx = 400f,
        currentTrimEndMs = 4_000L,
      ),
    )

    assertEquals(
      250L,
      boundedTrimEndMs(
        offsetPx = 20f,
        durationMs = 4_000L,
        trackWidthPx = 400f,
        currentTrimStartMs = 0L,
      ),
    )
  }

  @Test
  fun editorTimelineUiState_clampsVisibleWindowAndPendingSeek() {
    val timeline = sanitizeTimeline(
      durationMs = 10_000L,
      trimStartMs = 1_000L,
      trimEndMs = 8_000L,
      playheadMs = 4_000L,
      zoom = 2f,
    )

    val uiState = editorTimelineUiState(
      timeline = timeline,
      visibleWindowStartMs = -500L,
      visibleWindowEndMs = 12_000L,
      isDraggingStartHandle = true,
      pendingSeekMs = 99_000L,
    )

    assertEquals(0L, uiState.visibleWindowStartMs)
    assertEquals(10_000L, uiState.visibleWindowEndMs)
    assertEquals(8_000L, uiState.pendingSeekMs)
    assertTrue(uiState.isDraggingStartHandle)
    assertFalse(uiState.isDraggingEndHandle)
  }

  @Test
  fun timelinePreviewSeekDispatch_throttlesOnlyWhileInteracting() {
    assertFalse(
      shouldDispatchTimelinePreviewSeek(
        targetMs = 1_020L,
        lastDispatchedMs = 1_000L,
        isInteracting = true,
        elapsedSinceLastDispatchMs = 40L,
        frameStepMs = 33L,
        throttleMs = 90L,
      ),
    )

    assertTrue(
      shouldDispatchTimelinePreviewSeek(
        targetMs = 1_100L,
        lastDispatchedMs = 1_000L,
        isInteracting = true,
        elapsedSinceLastDispatchMs = 100L,
        frameStepMs = 33L,
        throttleMs = 90L,
      ),
    )

    assertTrue(
      shouldDispatchTimelinePreviewSeek(
        targetMs = 1_050L,
        lastDispatchedMs = 1_000L,
        isInteracting = false,
        elapsedSinceLastDispatchMs = 0L,
        frameStepMs = 33L,
        throttleMs = 90L,
      ),
    )
  }

  @Test
  fun buildWaveformSamples_marksSelectedTrimWindow() {
    val samples = buildWaveformSamples(
      durationMs = 10_000L,
      bucketCount = 20,
      trimStartMs = 2_000L,
      trimEndMs = 6_000L,
      seed = 17,
    )

    assertEquals(20, samples.size)
    assertTrue(samples.any { it.isSelected })
    assertTrue(samples.first { it.isSelected }.timeMs >= 2_000L)
    assertTrue(samples.last { it.isSelected }.timeMs <= 6_000L)
  }

  @Test
  fun splitAudioSegments_splitsOnlySelectedSegment() {
    val segments = splitAudioSegments(
      segments = listOf(AudioSegmentUi("seg-0", 0L, 8_000L)),
      selectedSegmentId = "seg-0",
      playheadMs = 3_000L,
    )

    assertEquals(2, segments.size)
    assertEquals(0L, segments[0].startMs)
    assertEquals(3_000L, segments[0].endMs)
    assertEquals(3_000L, segments[1].startMs)
    assertEquals(8_000L, segments[1].endMs)
  }
}
