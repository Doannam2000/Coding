package com.example.clipy.clipy.model

fun ExportFormat.mimeType(): String =
  when (this) {
    ExportFormat.Gif -> "image/gif"
    ExportFormat.Mp4 -> "video/mp4"
  }

fun String.exportMimeType(): String =
  if (equals("GIF", ignoreCase = true)) {
    ExportFormat.Gif.mimeType()
  } else {
    ExportFormat.Mp4.mimeType()
  }
