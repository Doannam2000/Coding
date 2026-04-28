package com.natncompany.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultTimelineEditorTest {
    private val editor = DefaultTimelineEditor()

    @Test
    fun `addClip rejects overlapping clips on non overlap track`() {
        val timeline = Timeline(
            tracks = listOf(
                TimelineTrack(
                    id = "video-main",
                    type = TrackType.Video,
                    allowOverlap = false,
                    clips = listOf(
                        clip(id = "clip-1", start = 0L, sourceStart = 0L, sourceEnd = 1_000L)
                    )
                )
            )
        )

        val result = editor.addClip(
            timeline = timeline,
            trackId = "video-main",
            clip = clip(id = "clip-2", start = 500L, sourceStart = 0L, sourceEnd = 1_000L)
        )

        val failure = result as MediaResult.Failure
        assertTrue(failure.error.message.contains("overlaps"))
    }

    @Test
    fun `splitClip creates second clip and keeps ordering`() {
        val timeline = Timeline(
            tracks = listOf(
                TimelineTrack(
                    id = "video-main",
                    type = TrackType.Video,
                    clips = listOf(
                        clip(id = "clip-1", start = 0L, sourceStart = 0L, sourceEnd = 2_000L)
                    )
                )
            )
        )

        val result = editor.splitClip(timeline, "video-main", "clip-1", 1_000L) as MediaResult.Success
        val clips = result.value.tracks.single().clips

        assertEquals(2, clips.size)
        assertEquals(0L, clips[0].timelineStartMs)
        assertEquals(1_000L, clips[0].sourceEndMs)
        assertEquals(1_000L, clips[1].timelineStartMs)
        assertEquals(1_000L, clips[1].sourceStartMs)
    }

    @Test
    fun `validateTimeline rejects duplicate clip ids across tracks`() {
        val shared = clip(id = "dup", start = 0L, sourceStart = 0L, sourceEnd = 500L)
        val timeline = Timeline(
            tracks = listOf(
                TimelineTrack(id = "video-main", type = TrackType.Video, clips = listOf(shared)),
                TimelineTrack(id = "audio-main", type = TrackType.Audio, clips = listOf(shared.copy(assetType = AssetType.Audio)))
            )
        )

        val result = editor.validateTimeline(timeline) as MediaResult.Failure
        assertTrue(result.error.message.contains("Duplicate clip id"))
    }

    private fun clip(
        id: String,
        start: Long,
        sourceStart: Long,
        sourceEnd: Long,
        assetType: AssetType = AssetType.Video
    ): TimelineClip {
        return TimelineClip(
            id = id,
            assetId = "asset-$id",
            assetType = assetType,
            timelineStartMs = start,
            sourceStartMs = sourceStart,
            sourceEndMs = sourceEnd,
            sourceDurationMs = sourceEnd
        )
    }
}
