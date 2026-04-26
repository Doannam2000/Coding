package com.example.clipystudio.data

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.assertNotSame
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
    assertEquals(imageClips.first().id, afterImage.selectedClipId)
    assertEquals("content://media/image/one", imageClips.first().mediaUri)
    assertEquals(3_000L, afterImage.durationMs)
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
    assertEquals("content://media/video/main", imported.mediaUri)

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
  fun duplicateAndDeleteKeepSelectionOnRealMediaBackedClips() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Portrait)
    repository.addImportedAsset(MediaType.Image, uri = "content://media/image/hero", displayName = "Hero", sizeBytes = 2_000_000)
    repository.addImportedAsset(MediaType.Video, uri = "content://media/video/broll", displayName = "B-roll", sizeBytes = 48_000_000, durationMs = 5_000)
    repository.addImportsToProject()

    var timeline = repository.appState.first().activeProject!!.timeline
    val firstClip = timeline.track(TrackType.Video).clips.first()
    assertEquals(firstClip.id, timeline.selectedClipId)

    repository.duplicateSelectedClip()
    timeline = repository.appState.first().activeProject!!.timeline
    val clipsAfterDuplicate = timeline.track(TrackType.Video).clips
    val duplicatedClip = clipsAfterDuplicate.last()
    assertEquals(3, clipsAfterDuplicate.size)
    assertEquals(duplicatedClip.id, timeline.selectedClipId)
    assertEquals(firstClip.mediaUri, duplicatedClip.mediaUri)
    assertEquals(duplicatedClip.startMs, timeline.playheadMs)

    repository.deleteSelectedClip()
    timeline = repository.appState.first().activeProject!!.timeline
    assertEquals(2, timeline.track(TrackType.Video).clips.size)
    assertNotNull(timeline.selectedClipId)
    assertFalse(timeline.track(TrackType.Video).clips.any { it.id == duplicatedClip.id })
    assertTrue(timeline.track(TrackType.Video).clips.any { it.id == timeline.selectedClipId })
  }

  @Test
  fun importedVideoDurationMetadataIsPreservedIntoTimeline() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Landscape)
    repository.addImportedAsset(MediaType.Video, uri = "content://media/video/precise", displayName = "Precise", sizeBytes = 32_000_000, durationMs = 12_345)
    repository.addImportsToProject()

    val clip = repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips.single()
    assertEquals(12_345L, clip.durationMs)
    assertEquals(12_345L, clip.sourceDurationMs)
    assertEquals("content://media/video/precise", clip.mediaUri)
  }

  @Test
  fun videoTrimCannotExtendBeyondImportedSourceDuration() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Landscape)
    repository.addImportedAsset(MediaType.Video, uri = "content://media/video/fixed", displayName = "Fixed", sizeBytes = 32_000_000, durationMs = 5_000)
    repository.addImportsToProject()

    repository.trimSelectedClip(2_000)

    val clip = repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips.single()
    assertEquals(5_000L, clip.durationMs)
    assertEquals(5_000L, clip.sourceDurationMs)
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
    assertEquals(11_000L, timeline.durationMs)

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

  @Test
  fun invalidImportedDurationFallsBackToSafeDefaultsAndSelectionTargetsRealMediaClip() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Portrait)
    repository.addImportedAsset(MediaType.Image, uri = "content://media/image/bad-duration", displayName = "Poster", sizeBytes = 1_024, durationMs = 0)
    repository.addImportsToProject()

    val timeline = repository.appState.first().activeProject!!.timeline
    val clip = timeline.track(TrackType.Video).clips.single()

    assertEquals(3_000L, clip.durationMs)
    assertEquals(clip.id, timeline.selectedClipId)
    assertEquals("content://media/image/bad-duration", clip.mediaUri)
  }

  @Test
  fun deleteSelectedClipPrefersRemainingClipFromSameTrack() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Portrait)
    repository.addImportedAsset(MediaType.Image, uri = "content://media/image/a", displayName = "A", sizeBytes = 2_000)
    repository.addImportedAsset(MediaType.Video, uri = "content://media/video/b", displayName = "B", sizeBytes = 32_000_000, durationMs = 5_000)
    repository.addImportsToProject()

    var timeline = repository.appState.first().activeProject!!.timeline
    val secondClip = timeline.track(TrackType.Video).clips.last()
    repository.selectClip(secondClip.id)
    repository.deleteSelectedClip()

    timeline = repository.appState.first().activeProject!!.timeline
    assertEquals(1, timeline.track(TrackType.Video).clips.size)
    assertEquals(timeline.track(TrackType.Video).clips.single().id, timeline.selectedClipId)
  }

  @Test
  fun deleteSelectedVideoClipReflowsRemainingTimelineAndSyncsPlayheadToSelection() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Portrait)
    repository.addImportedAsset(MediaType.Image, uri = "content://media/image/one", displayName = "One", sizeBytes = 2_000)
    repository.addImportedAsset(MediaType.Video, uri = "content://media/video/two", displayName = "Two", sizeBytes = 32_000_000, durationMs = 5_000)
    repository.addImportedAsset(MediaType.Image, uri = "content://media/image/three", displayName = "Three", sizeBytes = 2_000)
    repository.addImportsToProject()

    val imported = repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips.sortedBy { it.startMs }
    repository.selectClip(imported[1].id)
    repository.deleteSelectedClip()

    val timeline = repository.appState.first().activeProject!!.timeline
    val clips = timeline.track(TrackType.Video).clips.sortedBy { it.startMs }
    assertEquals(listOf(0L, clips.first().durationMs), clips.map { it.startMs })
    assertEquals(clips.last().id, timeline.selectedClipId)
    assertEquals(clips.last().startMs, timeline.playheadMs)
    assertEquals(TimelineEngine.scrollFromTime(timeline.playheadMs, timeline.zoomLevel, timeline.pixelsPerSecond), timeline.scrollOffsetPx)
  }

  @Test
  fun imageDurationCanExtendBeyondInitialThreeSecondsAndSplitKeepsStillSourceOffsetStable() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Portrait)
    repository.addImportedAsset(MediaType.Image, uri = "content://media/image/story", displayName = "Story", sizeBytes = 2_048)
    repository.addImportsToProject()

    repository.trimSelectedClip(1_000)
    repository.seekTo(2_000)
    repository.splitSelectedClip()

    val clips = repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips
    assertEquals(2, clips.size)
    assertEquals(2_000L, clips.first().durationMs)
    assertEquals(2_000L, clips.last().durationMs)
    assertEquals(0L, clips.first().sourceInMs)
    assertEquals(0L, clips.last().sourceInMs)
    assertNull(clips.first().sourceDurationMs)
    assertNull(clips.last().sourceDurationMs)
  }

  @Test
  fun duplicateKeepsImportedVideoUriAndSourceMetadataForFollowUpTrimAndSplit() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Landscape)
    repository.addImportedAsset(MediaType.Video, uri = "content://media/video/source-meta", displayName = "Source", sizeBytes = 32_000_000, durationMs = 9_000)
    repository.addImportsToProject()

    val original = repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips.single()
    repository.selectClip(original.id)
    repository.duplicateSelectedClip()

    var clips = repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips
    val duplicate = clips.last()
    assertEquals(original.mediaUri, duplicate.mediaUri)
    assertEquals(9_000L, duplicate.sourceDurationMs)
    assertEquals(0L, duplicate.sourceInMs)

    repository.trimSelectedClipEdge(TrimHandle.Left, 1_000)
    repository.seekTo(duplicate.startMs + 4_000L)
    repository.splitSelectedClip()

    clips = repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips.sortedBy { it.startMs }
    val duplicatedSegments = clips.filter { it.mediaUri == "content://media/video/source-meta" && it.startMs >= duplicate.startMs }
    assertEquals(2, duplicatedSegments.size)
    assertEquals(listOf(1_000L, 4_000L), duplicatedSegments.map { it.sourceInMs })
    assertTrue(duplicatedSegments.all { it.sourceDurationMs == 9_000L })
  }

  @Test
  fun reorderSelectedVideoClipReflowsTimelineStartsWithoutLosingSelection() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Portrait)
    repository.addImportedAsset(MediaType.Image, uri = "content://media/image/first", displayName = "First", sizeBytes = 2_000)
    repository.addImportedAsset(MediaType.Video, uri = "content://media/video/second", displayName = "Second", sizeBytes = 24_000_000, durationMs = 5_000)
    repository.addImportedAsset(MediaType.Image, uri = "content://media/image/third", displayName = "Third", sizeBytes = 2_000)
    repository.addImportsToProject()

    val before = repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips.sortedBy { it.startMs }
    repository.selectClip(before.last().id)
    repository.reorderSelectedVideoClip(0)

    val reordered = repository.appState.first().activeProject!!.timeline.track(TrackType.Video).clips.sortedBy { it.startMs }
    assertEquals(before.last().id, repository.appState.first().activeProject!!.timeline.selectedClipId)
    assertEquals(listOf(before.last().id, before.first().id, before[1].id), reordered.map { it.id })
    assertEquals(listOf(0L, reordered[0].durationMs, reordered[0].durationMs + reordered[1].durationMs), reordered.map { it.startMs })
  }

  @Test
  fun playbackAndButtonSeekKeepScrollOffsetAlignedWithPlayhead() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Portrait)
    repository.addImportedAsset(MediaType.Video, uri = "content://media/video/sync", displayName = "Sync", sizeBytes = 32_000_000, durationMs = 6_000)
    repository.addImportsToProject()

    repository.seekBy(1_000)
    var timeline = repository.appState.first().activeProject!!.timeline
    assertEquals(1_000L, timeline.playheadMs)
    assertEquals(TimelineEngine.scrollFromTime(1_000L, timeline.zoomLevel, timeline.pixelsPerSecond), timeline.scrollOffsetPx)

    repository.togglePlayback()
    repository.tickPlayback(500)
    timeline = repository.appState.first().activeProject!!.timeline
    assertEquals(1_500L, timeline.playheadMs)
    assertEquals(TimelineEngine.scrollFromTime(1_500L, timeline.zoomLevel, timeline.pixelsPerSecond), timeline.scrollOffsetPx)
  }

  @Test
  fun overlayTransformFromEditorGestureIsAutosavedAndUndoable() = runTest {
    val repository = DefaultDataRepository()
    repository.createProject(CanvasRatio.Portrait)
    repository.addImportedAsset(MediaType.Image, uri = "content://media/image/base", displayName = "Base", sizeBytes = 2_000_000)
    repository.addImportedAsset(MediaType.Image, uri = "content://media/image/overlay", displayName = "Overlay", sizeBytes = 1_500_000)
    repository.addImportsToProject()

    val asset = repository.appState.first().activeProject!!.importedAssets.last()
    repository.addOverlayAtPlayhead(asset)

    var project = repository.appState.first().activeProject!!
    val overlay = project.timeline.track(TrackType.Overlay).clips.single()
    val originalTransform = overlay.transform
    val originalAutosaveVersion = project.autosaveVersion

    repository.selectClip(overlay.id)
    repository.transformSelectedClipAbsolute(positionX = 0.74f, positionY = 0.31f, scale = 1.45f, rotationDegrees = 27f)

    project = repository.appState.first().activeProject!!
    val transformed = project.timeline.track(TrackType.Overlay).clips.single()
    assertNotSame(originalTransform, transformed.transform)
    assertEquals(0.74f, transformed.transform.positionX)
    assertEquals(0.31f, transformed.transform.positionY)
    assertEquals(1.45f, transformed.transform.scale)
    assertEquals(27f, transformed.transform.rotationDegrees)
    assertTrue(project.autosaveVersion > originalAutosaveVersion)
    assertTrue(repository.appState.first().undoStack.isNotEmpty())

    repository.undo()
    project = repository.appState.first().activeProject!!
    val undone = project.timeline.track(TrackType.Overlay).clips.single()
    assertEquals(originalTransform, undone.transform)

    repository.redo()
    project = repository.appState.first().activeProject!!
    val redone = project.timeline.track(TrackType.Overlay).clips.single()
    assertEquals(transformed.transform, redone.transform)
  }

  private fun Timeline.track(type: TrackType): TimelineTrack = tracks.first { it.type == type }
}
