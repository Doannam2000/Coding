package com.nantcompany.clipy.edit.tools.crop

data class CropRequest(
    val inputPath: String,
    val outputPath: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)