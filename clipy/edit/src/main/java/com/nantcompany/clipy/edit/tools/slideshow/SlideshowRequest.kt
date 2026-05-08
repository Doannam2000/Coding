package com.nantcompany.clipy.edit.tools.slideshow

data class SlideshowRequest(
    val imagePaths: List<String>,
    val outputPath: String,
    val secondsPerImage: Int
)
