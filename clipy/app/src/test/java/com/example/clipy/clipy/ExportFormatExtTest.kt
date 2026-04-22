package com.example.clipy.clipy

import com.example.clipy.clipy.model.ExportFormat
import com.example.clipy.clipy.model.exportMimeType
import com.example.clipy.clipy.model.mimeType
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportFormatExtTest {
  @Test
  fun gifUsesImageMimeType() {
    assertEquals("image/gif", ExportFormat.Gif.mimeType())
  }

  @Test
  fun historyFormatStringFallsBackToMp4WhenNotGif() {
    assertEquals("video/mp4", "MP4".exportMimeType())
  }
}
