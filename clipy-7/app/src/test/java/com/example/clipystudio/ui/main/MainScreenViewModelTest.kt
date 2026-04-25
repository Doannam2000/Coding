package com.example.clipystudio.ui.main

import com.example.clipystudio.data.AppState
import com.example.clipystudio.data.AudioSource
import com.example.clipystudio.data.CanvasRatio
import com.example.clipystudio.data.ClipAction
import com.example.clipystudio.data.DataRepository
import com.example.clipystudio.data.EditorTool
import com.example.clipystudio.data.EffectPreset
import com.example.clipystudio.data.ExportSettings
import com.example.clipystudio.data.FilterAdjustmentSet
import com.example.clipystudio.data.LanguageCode
import com.example.clipystudio.data.MediaAsset
import com.example.clipystudio.data.MediaType
import com.example.clipystudio.data.CanvasBackground
import com.example.clipystudio.data.ExportOutput
import com.example.clipystudio.data.Project
import com.example.clipystudio.data.RenderExportState
import com.example.clipystudio.data.RenderExportStatus
import com.example.clipystudio.data.RenderPipelineStatus
import com.example.clipystudio.data.StickerAsset
import com.example.clipystudio.data.DefaultTempFileManager
import com.example.clipystudio.data.TempFileManager
import com.example.clipystudio.data.TempRenderWorkspace
import com.example.clipystudio.data.Timeline
import com.example.clipystudio.data.TransitionType
import com.example.clipystudio.data.TrimHandle
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import java.nio.file.Files

class MainScreenViewModelTest {
  @Test
  fun uiState_initiallyLoading() = runTest {
    val viewModel = MainScreenViewModel(FakeClipyRepository(), RecordingTempFileManager())
    assertEquals(MainScreenUiState.Loading, viewModel.uiState.first())
  }

  @Test
  fun prepareRenderPipeline_setsReadyForValidProject() = runTest {
    val project = Project("p1", "Render", 0, 0, timeline = Timeline.defaultTimeline())
    val appState = AppState(hasCompletedIntro = true, projects = listOf(project), activeProjectId = project.id)
    val viewModel = MainScreenViewModel(FakeClipyRepository(appState), RecordingTempFileManager())

    viewModel.prepareRenderPipeline(appState)

    assertEquals(RenderPipelineStatus.READY, viewModel.renderPipelineState.value.status)
    assertEquals(300L, viewModel.renderPipelineState.value.totalFrames)
  }

  @Test
  fun prepareRenderPipeline_setsErrorForUnsupportedFormat() = runTest {
    val project = Project("p1", "Render", 0, 0, timeline = Timeline.defaultTimeline())
    val appState = AppState(hasCompletedIntro = true, projects = listOf(project), activeProjectId = project.id, defaultExportSettings = ExportSettings(format = "MOV"))
    val viewModel = MainScreenViewModel(FakeClipyRepository(appState), RecordingTempFileManager())

    viewModel.prepareRenderPipeline(appState)

    assertEquals(RenderPipelineStatus.ERROR, viewModel.renderPipelineState.value.status)
  }

  @Test
  fun requestShare_isDisabledBeforeExportOutputExists() = runTest {
    val project = Project("p1", "Share", 0, 0, timeline = Timeline.defaultTimeline())
    val appState = AppState(hasCompletedIntro = true, projects = listOf(project), activeProjectId = project.id)
    val viewModel = MainScreenViewModel(FakeClipyRepository(appState), RecordingTempFileManager())

    viewModel.requestShare()
    viewModel.prepareRenderPipeline(appState)
    viewModel.requestShare()

    assertNull(viewModel.renderExportState.value.output)
    assertNull(viewModel.shareEvent.value)
  }

  @Test
  fun cancelExport_withoutRunningExportKeepsShareDisabled() = runTest {
    val project = Project("p1", "Cancel", 0, 0, timeline = Timeline.defaultTimeline())
    val appState = AppState(hasCompletedIntro = true, projects = listOf(project), activeProjectId = project.id)
    val viewModel = MainScreenViewModel(FakeClipyRepository(appState), RecordingTempFileManager())

    viewModel.prepareRenderPipeline(appState)
    viewModel.cancelExport()
    viewModel.requestShare()

    assertNull(viewModel.renderExportState.value.output)
    assertNull(viewModel.shareEvent.value)
  }

  @Test
  fun requestShare_rejectsNonContentExportUri() = runTest {
    val viewModel = MainScreenViewModel(FakeClipyRepository(), RecordingTempFileManager())
    viewModel.setExportStateForTest(
      RenderExportState(
        output = ExportOutput(
          uri = "file:///tmp/unsafe.mp4",
          displayName = "unsafe.mp4",
          mimeType = "video/mp4",
          durationMs = 1_000,
          sizeBytes = 1_024,
          width = 720,
          height = 1280,
          fps = 30,
          createdAtMs = 0,
        ),
      ),
    )

    viewModel.requestShare()

    assertNull(viewModel.shareEvent.value)
  }

  @Test
  fun requestShare_acceptsExistingAppOwnedFileUri() = runTest {
    val root = Files.createTempDirectory("clipy-share-root").toFile()
    try {
      val tempFileManager = DefaultTempFileManager(root)
      val workspace = tempFileManager.createWorkspace("share")
      val outputFile = tempFileManager.createShareableOutput(workspace, "safe.mp4")
      val viewModel = MainScreenViewModel(FakeClipyRepository(), tempFileManager)
      viewModel.setExportStateForTest(
        RenderExportState(
          output = ExportOutput(
            uri = outputFile.toURI().toString(),
            displayName = outputFile.name,
            mimeType = "video/mp4",
            durationMs = 1_000,
            sizeBytes = outputFile.length(),
            width = 720,
            height = 1280,
            fps = 30,
            createdAtMs = 0,
          ),
          status = RenderExportStatus.COMPLETED,
        ),
      )

      viewModel.requestShare()

      assertEquals(outputFile.toURI().toString(), viewModel.shareEvent.value?.uri)
    } finally {
      root.deleteRecursively()
    }
  }

  @Test
  fun clearCache_runsTempCleanupWithoutMutatingRepositoryState() = runTest {
    val tempFileManager = RecordingTempFileManager()
    val viewModel = MainScreenViewModel(FakeClipyRepository(), tempFileManager)

    viewModel.clearCache()

    waitUntil { tempFileManager.cleanupAgesMs.contains(0L) }
    assertTrue(tempFileManager.cleanupAgesMs.contains(0L))
  }

  @Test
  fun clearCache_updatesFriendlyProgressMessage() = runTest {
    val tempFileManager = RecordingTempFileManager()
    val viewModel = MainScreenViewModel(FakeClipyRepository(), tempFileManager)

    viewModel.clearCache()

    advanceUntilIdle()
    assertEquals("Temporary files cleared. Original media and completed exports were kept.", viewModel.renderExportState.value.progress.message)
  }

  @Test
  fun startExport_failsWhenAvailableStorageIsTooLow() = runTest {
    val project = Project("p1", "Storage", 0, 0, timeline = Timeline.defaultTimeline())
    val appState = AppState(hasCompletedIntro = true, projects = listOf(project), activeProjectId = project.id)
    val tempFileManager = RecordingTempFileManager(availableStorageBytes = 1L)
    val viewModel = MainScreenViewModel(FakeClipyRepository(appState), tempFileManager)

    viewModel.prepareRenderPipeline(appState)
    viewModel.startExport()

    assertEquals(RenderExportStatus.FAILED, viewModel.renderExportState.value.status)
    assertEquals("Not enough free space for export. Clear temporary files or choose a lower preset.", viewModel.renderExportState.value.error?.message)
  }

  @Test
  fun cleanupStale_preservesExpiredCompletedOutputs() {
    val root = Files.createTempDirectory("clipy-clean-root").toFile()
    try {
      val tempFileManager = DefaultTempFileManager(root)
      val completedDir = File(root, "completed").apply { mkdirs() }
      val oldOutput = File(completedDir, "old.mp4").apply {
        writeText("old")
        setLastModified(System.currentTimeMillis() - 172_800_000L)
      }

      val removedCount = tempFileManager.cleanupStale(86_400_000L)

      assertEquals(0, removedCount)
      assertTrue(completedDir.exists())
      assertTrue(oldOutput.exists())
    } finally {
      root.deleteRecursively()
    }
  }

  @Test
  fun cleanupStale_removesExpiredWorkspaceDirectory() {
    val root = Files.createTempDirectory("clipy-workspace-root").toFile()
    try {
      val tempFileManager = DefaultTempFileManager(root)
      val workspace = tempFileManager.createWorkspace("project")
      val workspaceDir = File(workspace.directoryPath).apply {
        setLastModified(System.currentTimeMillis() - 172_800_000L)
      }

      val removedCount = tempFileManager.cleanupStale(86_400_000L)

      assertEquals(1, removedCount)
      assertFalse(workspaceDir.exists())
    } finally {
      root.deleteRecursively()
    }
  }
}

private fun MainScreenViewModel.setExportStateForTest(state: RenderExportState) {
  val field = MainScreenViewModel::class.java.getDeclaredField("_renderExportState")
  field.isAccessible = true
  @Suppress("UNCHECKED_CAST")
  val flow = field.get(this) as kotlinx.coroutines.flow.MutableStateFlow<RenderExportState>
  flow.value = state
}

private class RecordingTempFileManager(private val availableStorageBytes: Long = Long.MAX_VALUE) : TempFileManager {
  var lastCleanupAgeMs: Long? = null
  val cleanupAgesMs = mutableListOf<Long>()
  override fun createWorkspace(projectId: String): TempRenderWorkspace = TempRenderWorkspace(projectId, projectId, null, null, null, null, 0L, false)
  override fun createShareableOutput(workspace: TempRenderWorkspace, displayName: String): File = File.createTempFile(displayName, ".mp4")
  override fun cleanup(workspace: TempRenderWorkspace): TempRenderWorkspace = workspace.copy(isCleaned = true)
  override fun cleanupStale(maxAgeMs: Long): Int {
    lastCleanupAgeMs = maxAgeMs
    cleanupAgesMs += maxAgeMs
    return 0
  }
  override fun availableStorageBytes(): Long = availableStorageBytes
  override fun owns(file: File): Boolean = true
}

private fun waitUntil(condition: () -> Boolean) {
  repeat(50) {
    if (condition()) return
    Thread.sleep(20)
  }
}

private class FakeClipyRepository(appState: AppState = AppState(hasCompletedIntro = true)) : DataRepository {
  override val appState: Flow<AppState> = flowOf(appState)
  override fun completeIntro() = Unit
  override fun setLanguage(languageCode: LanguageCode) = Unit
  override fun createProject(ratio: CanvasRatio) = Unit
  override fun renameProject(projectId: String, name: String) = Unit
  override fun duplicateProject(projectId: String) = Unit
  override fun deleteProject(projectId: String) = Unit
  override fun openProject(projectId: String) = Unit
  override fun addImportedAsset(type: MediaType, uri: String?, displayName: String?, sizeBytes: Long?) = Unit
  override fun removeImportedAsset(assetId: String) = Unit
  override fun addImportsToProject() = Unit
  override fun selectClip(clipId: String) = Unit
  override fun clearSelection() = Unit
  override fun togglePlayback() = Unit
  override fun seekTo(positionMs: Long) = Unit
  override fun seekBy(deltaMs: Long) = Unit
  override fun scrollTimelineTo(scrollOffsetPx: Float, viewportWidthPx: Float) = Unit
  override fun tickPlayback(deltaMs: Long) = Unit
  override fun dragSelectedClip(deltaMs: Long) = Unit
  override fun trimSelectedClipEdge(handle: TrimHandle, deltaMs: Long) = Unit
  override fun reorderSelectedVideoClip(targetIndex: Int) = Unit
  override fun updateTimelineZoom(delta: Float, focalXpx: Float, viewportWidthPx: Float) = Unit
  override fun updateCanvasRatio(ratio: CanvasRatio) = Unit
  override fun splitSelectedClip() = Unit
  override fun deleteSelectedClip() = Unit
  override fun duplicateSelectedClip() = Unit
  override fun trimSelectedClip(deltaMs: Long) = Unit
  override fun moveSelectedClip(deltaMs: Long) = Unit
  override fun updateSelectedTool(tool: EditorTool) = Unit
  override fun adjustSelectedClip(action: ClipAction) = Unit
  override fun transformSelectedClip(deltaX: Float, deltaY: Float, scaleChange: Float, rotationChange: Float) = Unit
  override fun transformSelectedClipAbsolute(positionX: Float, positionY: Float, scale: Float, rotationDegrees: Float) = Unit
  override fun addAudioClipAtPlayhead(title: String, source: AudioSource) = Unit
  override fun addTextClipAtPlayhead(content: String, fontSizeSp: Float, color: String, backgroundColor: String?, strokeEnabled: Boolean, shadowEnabled: Boolean, alignment: String, animation: String) = Unit
  override fun addStickerAtPlayhead(asset: StickerAsset) = Unit
  override fun updateSelectedFilter(filterId: String?) = Unit
  override fun updateSelectedAdjustments(adjustments: FilterAdjustmentSet) = Unit
  override fun addEffectAtPlayhead(effect: EffectPreset) = Unit
  override fun applyTransition(type: TransitionType, durationMs: Long) = Unit
  override fun removeTransition() = Unit
  override fun updateCanvasBackground(background: CanvasBackground) = Unit
  override fun updateSelectedSpeed(speed: Float) = Unit
  override fun updateSelectedAudio(volume: Float, fadeInMs: Long, fadeOutMs: Long, loopEnabled: Boolean) = Unit
  override fun updateSelectedText(content: String, fontSizeSp: Float, color: String, backgroundColor: String?, strokeEnabled: Boolean, shadowEnabled: Boolean, alignment: String, animation: String) = Unit
  override fun addOverlayAtPlayhead(asset: MediaAsset) = Unit
  override fun updateSelectedOpacity(opacity: Float) = Unit
  override fun toggleKeyframeAtPlayhead() = Unit
  override fun undo() = Unit
  override fun redo() = Unit
  override fun updateExportSettings(settings: ExportSettings) = Unit
  override fun startExport() = Unit
  override fun completeExport() = Unit
  override fun cancelExport() = Unit
  override fun clearExportResult() = Unit
  override fun clearCache() = Unit
}
