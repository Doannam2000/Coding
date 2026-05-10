package com.nantcompany.clipy.edit.tools.compress

data class CompressRequest(
    val inputPath: String,
    val outputPath: String,
    val bitrateKbps: Int,
    val targetHeight: Int? = null,
    val keepAudio: Boolean = true
)
