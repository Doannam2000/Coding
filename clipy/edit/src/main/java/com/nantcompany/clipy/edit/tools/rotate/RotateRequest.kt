package com.nantcompany.clipy.edit.tools.rotate

data class RotateRequest(
    val inputPath: String,
    val outputPath: String,
    val rotation: Int, // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
)
