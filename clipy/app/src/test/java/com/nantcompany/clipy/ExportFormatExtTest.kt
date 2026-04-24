package com.nantcompany.clipy

import com.nantcompany.clipy.model.ExportFormat
import com.nantcompany.clipy.model.exportMimeType
import com.nantcompany.clipy.model.mimeType
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
