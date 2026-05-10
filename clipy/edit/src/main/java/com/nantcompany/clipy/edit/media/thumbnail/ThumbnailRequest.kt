package com.nantcompany.clipy.edit.media.thumbnail

data class ThumbnailRequest(
    val sourceUri: String,
    val atMs: Long = 0L,
    val width: Int = 320,
    val height: Int = 180
)
