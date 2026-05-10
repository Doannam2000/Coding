package com.nantcompany.clipy.export.output

data class OutputMedia(
    val id: String,
    val fileName: String,
    val path: String,
    val sizeInBytes: Long,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val operation: String = "unknown"
)
