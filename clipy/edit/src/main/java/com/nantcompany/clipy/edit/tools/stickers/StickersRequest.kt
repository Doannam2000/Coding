package com.nantcompany.clipy.edit.tools.stickers

data class StickersRequest(
    val inputPath: String,
    val outputPath: String,
    val stickerPath: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Int? = null,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L
)