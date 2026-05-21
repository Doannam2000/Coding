package com.nantcompany.clipy.edit.tools.textoverlay

data class TextOverlayRequest(
    val inputPath: String,
    val outputPath: String,
    val text: String,
    val fontSize: Int = 48,
    val fontColor: String = "white",
    val x: Float = 10f,
    val y: Float = 10f
)