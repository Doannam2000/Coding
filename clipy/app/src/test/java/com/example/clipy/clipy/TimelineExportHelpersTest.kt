package com.example.clipy.clipy

import com.example.clipy.clipy.model.CropRatio
import com.example.clipy.clipy.model.ExportFormat
import com.example.clipy.clipy.model.ProjectDraft
import com.example.clipy.clipy.model.TimelineSnapshot
import com.example.clipy.clipy.model.buildExportPlan
import com.example.clipy.clipy.model.resolutionPreset
import com.example.clipy.clipy.model.shouldPersistUri
import com.example.clipy.clipy.model.thumbnailCaptureTimesMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineExportHelpersTest {
  @Test
  fun thumbnailCaptureTimes_spanSelectedTrimRange() {
    val times = thumbnailCaptureTimesMs(
      TimelineSnapshot(durationMs = 10_000L, trimStartMs = 2_000L, trimEndMs = 6_000L, playheadMs = 3_000L, zoom = 2f),
      frameCount = 5,
    )

    assertEquals(listOf(2_000L, 3_000L, 4_000L, 5_000L, 6_000L), times)
  }

  @Test
  fun resolutionPreset_matchesStoryAspectRatio() {
    val preset = resolutionPreset("720p", CropRatio.Story)

    assertEquals(720, preset.width)
    assertEquals(1280, preset.height)
  }

  @Test
  fun buildExportPlan_setsMp4FaststartAndAudioWhenNotMuted() {
    val plan = buildExportPlan(
      ProjectDraft(
        sourceUri = "content://clip",
        exportFormat = ExportFormat.Mp4,
        trimEndMs = 4_000L,
      ),
    )

    assertTrue(plan.ffmpegCommand.contains("-movflags +faststart"))
    assertTrue(plan.ffmpegCommand.contains("-c:a aac"))
  }

  @Test
  fun uriPersistence_onlyAppliesToContentUris() {
    assertTrue(shouldPersistUri("content://media/external/video/media/1"))
    assertFalse(shouldPersistUri("file:///storage/emulated/0/DCIM/clip.mp4"))
  }
}
