package com.example.clipystudio.data

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EditorScenarioRegressionTest {
  @Test
  fun imageOnlyProject_importsToTimelineAndRejectsDeniedImport() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Portrait)
    repository.addImportedAsset(MediaType.Image, uri = "content://media/image/one", displayName = "Cover", sizeBytes = 2_000_000)
    repository.addImportsToProject()

    val afterImage = repository.appState.first().activeProject!!.timeline
    val imageClips = afterImage.track(TrackType.Video).clips

    assertEquals(1, imageClips.size)
    assertEquals(ClipType.Image, imageClips.first().clipType)
    assertEquals(4_000L, afterImage.durationMs)
    assertTrue(afterImage.durationMs > 0L)

    repository.addImportedAsset(MediaType.Image, uri = "", displayName = "Denied", sizeBytes = 1_000)

    assertTrue(repository.appState.first().selectedImports.isEmpty())
    assertEquals(1, repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips.size)
  }

  @Test
  fun videoOnlyProject_trimSplitUndoRedoAndReopenDraftStateStayConsistent() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Landscape)
    repository.addImportedAsset(MediaType.Video, uri = "content://media/video/main", displayName = "Main", sizeBytes = 48_000_000)
    repository.addImportsToProject()
    val imported = repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips.single()

    repository.selectClip(imported.id)
    repository.trimSelectedClipEdge(TrimHandle.Right, -1_000)
    val trimmed = repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips.single()
    assertEquals(7_000L, trimmed.durationMs)
    assertTrue(repository.appState.first().undoStack.isNotEmpty())

    repository.seekTo(3_000)
    repository.splitSelectedClip()
    val split = repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips
    assertEquals(2, split.size)
    assertEquals(3_000L, split.first().durationMs)
    assertEquals(4_000L, split.last().durationMs)

    repository.undo()
    assertEquals(1, repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips.size)
    assertTrue(repository.appState.first().redoStack.isNotEmpty())

    repository.redo()
    val reopenedProjectId = repository.appState.first().activeProjectId!!
    repository.openProject(reopenedProjectId)
    assertEquals(2, repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips.size)
  }

  @Test
  fun mixedMediaReorderOverlaysEffectsTransitionsAndUndoRedoArePersistedInState() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Portrait)
    repository.addImportedAsset(MediaType.Image, uri = "content://media/image/cover", displayName = "Cover", sizeBytes = 2_000_000)
    repository.addImportedAsset(MediaType.Video, uri = "content://media/video/clip", displayName = "Clip", sizeBytes = 48_000_000)
    repository.addImportsToProject()

    var timeline = repository.appState.first().activeProject!!.timeline
    val visualClips = timeline.track(TrackType.Video).clips
    assertEquals(listOf(ClipType.Image, ClipType.Video), visualClips.map { it.clipType })
    assertEquals(12_000L, timeline.durationMs)

    repository.selectClip(visualClips.last().id)
    repository.reorderSelectedVideoClip(0)
    timeline = repository.appState.first().activeProject!!.timeline
    assertEquals(ClipType.Video, timeline.track(TrackType.Video).clips.first().clipType)

    repository.seekTo(1_000)
    repository.addAudioClipAtPlayhead("Music bed", AudioSource.DeviceMusic)
    repository.addAudioClipAtPlayhead("Extracted voice", AudioSource.ExtractedAudio)
    repository.addTextClipAtPlayhead("Launch sale", 32f, "#FFFFFF", "#111111", strokeEnabled = true, shadowEnabled = true, alignment = "Center", animation = "Fade")
    repository.addStickerAtPlayhead(StickerLibrary.first())
    repository.addEffectAtPlayhead(EffectLibrary.first())
    repository.selectClip(repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips.first().id)
    repository.updateSelectedFilter("warm")
    repository.seekTo(7_000)
    repository.applyTransition(TransitionType.Fade, 600)

    timeline = repository.appState.first().activeProject!!.timeline
    assertEquals(2, timeline.track(TrackType.Audio).clips.size)
    assertEquals(1, timeline.track(TrackType.Text).clips.size)
    assertEquals(1, timeline.track(TrackType.Sticker).clips.size)
    assertEquals(1, timeline.track(TrackType.Effect).clips.size)
    assertEquals("warm", timeline.track(TrackType.Video).clips.first().filterAdjustments.filterId)
    assertEquals(1, timeline.transitions.size)

    repository.undo()
    assertTrue(repository.appState.first().activeProject!!.timeline.transitions.isEmpty())
    repository.redo()
    assertEquals(1, repository.appState.first().activeProject!!.timeline.transitions.size)
  }

  @Test
  fun exportSettingsLifecycleAndShareReadinessUseRealOutputOnly() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Portrait)
    repository.addImportedAsset(MediaType.Video, uri = "content://media/video/export", displayName = "Export", sizeBytes = 30_000_000)
    repository.addImportsToProject()

    repository.updateExportSettings(ExportSettings(resolution = ExportResolution.P720, fps = 24, bitrateMbps = 8f))
    repository.startExport()
    var state = repository.appState.first()
    var job = state.exportJob ?: error("Expected running export job")
    assertEquals(ExportResolution.P720, state.defaultExportSettings.resolution)
    assertEquals(ExportStatus.Running, job.status)
    assertNull(job.outputUri)

    repository.cancelExport()
    state = repository.appState.first()
    job = state.exportJob ?: error("Expected cancelled export job")
    assertEquals(ExportStatus.Cancelled, job.status)
    assertNull(job.outputUri)

    repository.updateExportSettings(ExportSettings(resolution = ExportResolution.P1080, fps = 30, bitrateMbps = 12f))
    repository.startExport()
    repository.completeExport()
    state = repository.appState.first()
    job = state.exportJob ?: error("Expected completed export job")
    assertEquals(ExportResolution.P1080, job.settings.resolution)
    assertEquals(ExportStatus.Complete, job.status)
    assertNotNull(job.outputUri)
  }

  @Test
  fun lowStorageExportFailureMapsToRecoverableStorageErrorWithoutOutput() {
    val error = RenderExportErrorClassifier.classify(IllegalStateException("No space left on device"), RenderExportPhase.SAVING_OUTPUT)
    val failedState = RenderExportState(
      status = RenderExportStatus.FAILED,
      phase = RenderExportPhase.SAVING_OUTPUT,
      error = error,
      output = null,
      canRetry = error.recoverable,
    )

    assertEquals(RenderExportErrorType.STORAGE_FAILURE, failedState.error!!.type)
    assertTrue(failedState.canRetry)
    assertNull(failedState.output)
  }

  @Test
  fun missingOrInaccessibleFileFixtureIsKeptOutOfTimeline() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Portrait)
    repository.addImportedAsset(MediaType.Video, uri = "   ", displayName = "Missing file", sizeBytes = 0)
    repository.addImportsToProject()

    val project = repository.appState.first().activeProject!!
    assertTrue(project.importedAssets.isEmpty())
    assertTrue(project.timeline.track(TrackType.Video).clips.isEmpty())
    assertTrue(repository.appState.first().undoStack.isEmpty())
  }

  private fun Timeline.track(type: TrackType): TimelineTrack = tracks.first { it.type == type }
}
