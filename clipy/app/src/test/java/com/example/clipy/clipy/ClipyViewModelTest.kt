package com.example.clipy.clipy

import com.example.clipy.clipy.model.CropRatio
import com.example.clipy.clipy.model.ExportFormat
import com.example.clipy.clipy.model.Mp4Quality
import com.example.clipy.clipy.model.ProjectDraft
import com.example.clipy.clipy.model.buildExportPlan
import com.example.clipy.clipy.model.sanitizeTimeline
import com.example.clipy.clipy.model.sanitizeOutputName
import com.example.clipy.clipy.model.snapTimelineMs
import com.example.clipy.clipy.model.timelineScrollForPlayhead
import com.example.clipy.clipy.model.timelineThumbnailCount
import com.example.clipy.clipy.model.validateExport
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
}
