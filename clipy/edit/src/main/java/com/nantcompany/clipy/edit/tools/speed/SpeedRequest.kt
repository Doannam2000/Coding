package com.nantcompany.clipy.edit.tools.speed

data class SpeedRequest(
    val inputPath: String,
    val outputPath: String,
    val speedFactor: Float = 1f
)