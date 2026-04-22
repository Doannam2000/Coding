package com.example.clipy.clipy

import com.example.clipy.clipy.model.CropRatio
import com.example.clipy.clipy.model.ExportFormat
import com.example.clipy.clipy.model.Mp4Quality
import com.example.clipy.clipy.model.ProjectDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipyViewModelTest {
  @Test
  fun projectDraft_defaultsMatchEditorFlow() {
    val draft = ProjectDraft()
    assertEquals(CropRatio.Story, draft.cropRatio)
    assertEquals(ExportFormat.Gif, draft.exportFormat)
    assertEquals(Mp4Quality.Balanced, draft.mp4Quality)
  }
}
