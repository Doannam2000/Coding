package com.nantcompany.clipy.edit.media.model

data class MediaItem(
    val uriString: String,
    val displayName: String = "",
    val mimeType: String = "",
    val durationMs: Long = 0L,
    val sizeInBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0
)
