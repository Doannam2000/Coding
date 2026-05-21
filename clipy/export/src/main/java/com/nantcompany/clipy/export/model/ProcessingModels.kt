package com.nantcompany.clipy.export.model

/**
 * Represents a single video segment in a multi-clip sequence.
 */
data class VideoClip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val path: String,
    val durationMs: Long,
    val startMs: Long = 0,
    val endMs: Long,
    val transition: String = "none" // crossfade, zoom, slide, etc.
)

data class TextLayer(
    val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val startMs: Long,
    val endMs: Long,
    val color: Int
)

data class AudioTrack(
    val id: String,
    val path: String,
    val volume: Float,
    val startMs: Long,
    val offsetMs: Long
)
