package com.nantcompany.clipy.edit.media.metadata

data class MediaMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val bitrateKbps: Int? = null
)
