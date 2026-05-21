package com.nantcompany.clipy.edit.tools.merge

data class MergeRequest(
    val inputPaths: List<String>,
    val outputPath: String,
    val transition: String = "none",
    val transitionDurationMs: Long = 1000L
)
