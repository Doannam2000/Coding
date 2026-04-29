package com.natncompany.media.render

import com.natncompany.media.Asset
import com.natncompany.media.AssetType
import com.natncompany.media.ClipTransform
import com.natncompany.media.RenderRequest
import com.natncompany.media.Timeline
import com.natncompany.media.TimelineClip
import com.natncompany.media.TimelineTrack
import com.natncompany.media.TrackType
import com.natncompany.media.VideoProject
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderCommandBuilderTest {
    @Test
    fun `build applies clip filter transforms to export command`() {
        val asset = Asset(
            id = "asset-1",
            sourceUri = "file://input.mp4",
            cachedPath = "input.mp4",
            displayName = "input.mp4",
            type = AssetType.Video,
            durationMs = 1_000L
        )
        val clip = TimelineClip(
            id = "clip-1",
            assetId = asset.id,
            assetType = AssetType.Video,
            timelineStartMs = 0L,
            sourceStartMs = 0L,
            sourceEndMs = 1_000L,
            transform = ClipTransform(
                brightness = 0.25f,
                contrast = 1.25f,
                blur = 0.25f
            )
        )
        val timeline = Timeline(
            tracks = listOf(
                TimelineTrack(
                    id = "video-main",
                    type = TrackType.Video,
                    clips = listOf(clip)
                )
            )
        )
        val request = RenderRequest(
            project = VideoProject(
                id = "project-1",
                name = "Project",
                rootCachePath = "",
                timeline = timeline,
                assets = listOf(asset)
            ),
            outputFileName = "output.mp4",
            timeline = timeline
        )

        val command = RenderCommandBuilder.build(request, "output.mp4")

        assertTrue(command.contains("eq=brightness=0.250:contrast=1.250"))
        assertTrue(command.contains("boxblur=3:1"))
    }
}
