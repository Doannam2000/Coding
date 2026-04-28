package com.natncompany.media.preview

import com.natncompany.media.AssetType
import com.natncompany.media.TimelineClip
import com.natncompany.media.VideoProject

internal class TimelinePositionMapper(project: VideoProject) {
    data class Segment(
        val clip: TimelineClip,
        val windowIndex: Int
    ) {
        val timelineStartMs: Long = clip.timelineStartMs
        val timelineEndMs: Long = clip.timelineEndMs
    }

    private val segments: List<Segment> = project.timeline.tracks
        .filter { it.isEnabled }
        .flatMap { it.clips }
        .sortedBy { it.timelineStartMs }
        .filter { it.assetType != AssetType.Image }
        .mapIndexed { index, clip -> Segment(clip = clip, windowIndex = index) }

    fun timelineToWindow(positionMs: Long): Pair<Int, Long>? {
        val segment = segments.firstOrNull { positionMs in it.timelineStartMs until it.timelineEndMs } ?: return null
        val sourcePositionMs = segment.clip.sourceStartMs + (positionMs - segment.timelineStartMs)
        return segment.windowIndex to sourcePositionMs
    }

    fun windowToTimeline(windowIndex: Int, positionMs: Long): Long {
        val segment = segments.firstOrNull { it.windowIndex == windowIndex } ?: return 0L
        return segment.timelineStartMs + (positionMs - segment.clip.sourceStartMs).coerceAtLeast(0L)
    }

    fun activeClipIdAt(positionMs: Long): String? {
        return segments.firstOrNull { positionMs in it.timelineStartMs until it.timelineEndMs }?.clip?.id
    }

    fun clipIdForWindow(windowIndex: Int): String? {
        return segments.firstOrNull { it.windowIndex == windowIndex }?.clip?.id
    }
}
