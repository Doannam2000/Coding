package com.example.clipystudio.data

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class RenderPipelineEngineTest {
  @Test
  fun collectInput_includesAllTrackTypesAndTransitions() {
    val input = RenderPipelineEngine.collectInput(sampleRenderTimeline(), sampleProject())

    assertEquals(listOf("v1"), input.videoClips.map { it.id })
    assertEquals(listOf("img1"), input.imageClips.map { it.id })
    assertEquals(listOf("a1"), input.audioClips.map { it.id })
    assertEquals(listOf("txt1"), input.textClips.map { it.id })
    assertEquals(listOf("stk1"), input.stickerClips.map { it.id })
    assertEquals(listOf("ov1"), input.overlayClips.map { it.id })
    assertEquals(listOf("fx1"), input.effectClips.map { it.id })
    assertEquals(1, input.transitions.size)
  }

  @Test
  fun exportSettingsMapper_mapsResolutionFpsAndQuality() {
    val settings = ProjectRenderSettings("p1", 1080, 1920, 30, 0)
    val config = ExportSettingsMapper.map(ExportOptions(RenderExportResolution.HD_720P, 24, ExportFormat.MP4, ExportQuality.HIGH), settings, 2_000).getOrThrow()

    assertEquals(404, config.width)
    assertEquals(720, config.height)
    assertEquals(24, config.fps)
    assertEquals("mp4", config.outputExtension)
    assertTrue(config.videoBitrate >= 2_000_000)
  }

  @Test
  fun exportSettingsMapper_rejectsInvalidFpsAndDuration() {
    val settings = ProjectRenderSettings("p1", 1080, 1920, 30, 0)

    assertTrue(ExportSettingsMapper.map(ExportOptions(fps = 25), settings, 2_000).isFailure)
    assertTrue(ExportSettingsMapper.map(ExportOptions(), settings, 0).isFailure)
  }

  @Test
  fun buildRenderGraph_ordersVisualLayersAndSeparatesAudio() {
    val input = RenderPipelineEngine.collectInput(sampleRenderTimeline(), sampleProject())
    val encoder = ExportSettingsMapper.map(ExportOptions(), input.settings, input.durationMs).getOrThrow()
    val graph = RenderPipelineEngine.buildRenderGraph(input, encoder).getOrThrow()

    assertEquals(listOf(RenderLayerType.MAIN_VIDEO, RenderLayerType.IMAGE, RenderLayerType.EFFECT, RenderLayerType.OVERLAY, RenderLayerType.STICKER, RenderLayerType.TEXT), graph.layers.map { it.type })
    assertEquals(listOf("a1"), graph.audio.map { it.clipId })
    assertEquals(180L, graph.totalFrames)
  }

  @Test
  fun frameScheduler_returnsStableTimestampsAndLastFrame() {
    val frames = FrameScheduler.schedule(1_000, 30)

    assertEquals(30, frames.size)
    assertEquals(0L, frames.first().timeMs)
    assertEquals(966L, frames.last().timeMs)
    assertTrue(frames.last().isLastFrame)
    assertEquals(60L, FrameScheduler.totalFrames(1_000, 60))
  }

  @Test
  fun planFrame_resolvesActiveLayersTransitionAndText() {
    val input = RenderPipelineEngine.collectInput(sampleRenderTimeline(), sampleProject())
    val encoder = ExportSettingsMapper.map(ExportOptions(), input.settings, input.durationMs).getOrThrow()
    val graph = RenderPipelineEngine.buildRenderGraph(input, encoder).getOrThrow()
    val plan = RenderPipelineEngine.planFrame(graph, ScheduledFrame(45, 1_500, 1_500_000, 33, false))

    assertEquals("v1", plan.mainLayer?.clipId)
    assertNotNull(plan.transition)
    assertEquals(2, plan.overlays.size)
    assertEquals("Caption", plan.texts.first().text)
    assertEquals(1, plan.effects.size)
  }

  @Test
  fun sourceTimeFor_accountsForTrimAndSpeed() {
    val layer = ProjectTimelineClip("v", ClipType.Video, "asset", 1_000, 2_000, trimStartMs = 500, speed = 2f, trackType = TrackType.Video).let {
      RenderPipelineEngine.buildRenderGraph(
        RenderInput(ProjectTimeline(durationMs = 3_000, tracks = TimelineTracks(video = listOf(it))), 3_000, TimelineTracks(video = listOf(it)), listOf(it), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), ProjectRenderSettings("p", 1080, 1920, 30, 0), 1),
        ExportSettingsMapper.map(ExportOptions(), ProjectRenderSettings("p", 1080, 1920, 30, 0), 3_000).getOrThrow(),
      ).getOrThrow().layers.first()
    }

    assertEquals(1_500L, RenderPipelineEngine.sourceTimeFor(layer, 1_500))
  }

  private fun sampleProject() = Project("p1", "Render", 0, 0, canvasRatio = CanvasRatio.Portrait, timeline = sampleRenderTimeline())

  private fun sampleRenderTimeline() = Timeline(
    durationMs = 6_000,
    transitions = listOf(Transition("tr1", TransitionType.Fade, "v1", "img1", 1_000, 2_000)),
    tracks = listOf(
      TimelineTrack("video", TrackType.Video, "Video", 0, listOf(
        TimelineClip("v1", assetId = "asset-v", clipType = ClipType.Video, title = "Video", startMs = 0, durationMs = 2_000, sourceInMs = 200, videoProperties = VideoProperties(speed = 1.25f), filterAdjustments = FilterAdjustmentSet(filterId = "warm")),
        TimelineClip("img1", assetId = "asset-i", clipType = ClipType.Image, title = "Image", startMs = 2_000, durationMs = 2_000),
      )),
      TimelineTrack("audio", TrackType.Audio, "Audio", 1, listOf(TimelineClip("a1", assetId = "asset-a", clipType = ClipType.Audio, title = "Audio", startMs = 0, durationMs = 6_000, audioProperties = AudioProperties(volume = 0.7f)))),
      TimelineTrack("text", TrackType.Text, "Text", 2, listOf(TimelineClip("txt1", clipType = ClipType.Text, title = "Text", startMs = 1_000, durationMs = 2_500, textProperties = TextProperties(content = "Caption", animation = "Fade"), keyframes = listOf(Keyframe(timeMs = 1_000, property = KeyframeProperty.Opacity, value = 0.5f))))),
      TimelineTrack("sticker", TrackType.Sticker, "Sticker", 3, listOf(TimelineClip("stk1", clipType = ClipType.Sticker, title = "Star", startMs = 1_000, durationMs = 2_000))),
      TimelineTrack("overlay", TrackType.Overlay, "Overlay", 4, listOf(TimelineClip("ov1", assetId = "asset-o", clipType = ClipType.Overlay, title = "Overlay", startMs = 1_000, durationMs = 2_000, transform = TransformState(opacity = 0.8f)))),
      TimelineTrack("effect", TrackType.Effect, "Effect", 5, listOf(TimelineClip("fx1", clipType = ClipType.Effect, title = "Glow", startMs = 1_000, durationMs = 2_000, filterAdjustments = FilterAdjustmentSet(filterId = "glow")))),
    ),
  )
}
