package com.nantcompany.clipy.edit.tools.cut

enum class CutType {
    TRIM,
    CUT // Remove selection
}

data class CutRequest(
    val inputPath: String,
    val outputPath: String,
    val startMs: Long,
    val endMs: Long,
    val type: CutType = CutType.TRIM
)
