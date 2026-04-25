package com.nantcompany.clipy.model

fun ExportFormat.mimeType(): String =
  when (this) {
    ExportFormat.Gif -> "image/gif"
    ExportFormat.Mp4 -> "video/mp4"
  }

fun String.exportMimeType(): String =
  when {
    equals("GIF", ignoreCase = true) -> ExportFormat.Gif.mimeType()
    equals("MOV", ignoreCase = true) -> "video/quicktime"
    else -> ExportFormat.Mp4.mimeType()
  }
