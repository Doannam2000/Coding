package com.nantcompany.clipy.edit.tools.extractaudio

data class ExtractAudioRequest(
    val inputPath: String,
    val outputPath: String,
    val format: String = "mp3",
    val bitrateKbps: Int = 128
)
