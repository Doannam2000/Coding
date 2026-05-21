package com.nantcompany.clipy.edit.tools.slideshow

data class SlideshowRequest(
    val imagePaths: List<String>,
    val outputPath: String,
    val secondsPerImage: Int,
    val backgroundMode: String = "fit",
    val transition: String = "none",
    val transitionDurationMs: Long = 1000L,
    val audioPath: String? = null
)
