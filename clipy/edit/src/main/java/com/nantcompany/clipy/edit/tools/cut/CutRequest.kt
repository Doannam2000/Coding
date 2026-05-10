package com.nantcompany.clipy.edit.tools.cut

data class CutRequest(
    val inputPath: String,
    val outputPath: String,
    val startMs: Long,
    val endMs: Long
)
