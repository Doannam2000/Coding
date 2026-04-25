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
import com.example.clipystudio.data.Project
import com.example.clipystudio.data.RenderPipelineStatus
import com.example.clipystudio.data.StickerAsset
import com.example.clipystudio.data.Timeline
import com.example.clipystudio.data.TransitionType
import com.example.clipystudio.data.TrimHandle
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun uiState_initiallyLoading() = runTest {
    val viewModel = MainScreenViewModel(FakeClipyRepository())
    assertEquals(MainScreenUiState.Loading, viewModel.uiState.first())
  }

  @Test
  fun prepareRenderPipeline_setsReadyForValidProject() = runTest {
    val project = Project("p1", "Render", 0, 0, timeline = Timeline.defaultTimeline())
    val appState = AppState(hasCompletedIntro = true, projects = listOf(project), activeProjectId = project.id)
    val viewModel = MainScreenViewModel(FakeClipyRepository(appState))

    viewModel.prepareRenderPipeline(appState)

    assertEquals(RenderPipelineStatus.READY, viewModel.renderPipelineState.value.status)
    assertEquals(300L, viewModel.renderPipelineState.value.totalFrames)
  }

  @Test
  fun prepareRenderPipeline_setsErrorForUnsupportedFormat() = runTest {
    val project = Project("p1", "Render", 0, 0, timeline = Timeline.defaultTimeline())
    val appState = AppState(hasCompletedIntro = true, projects = listOf(project), activeProjectId = project.id, defaultExportSettings = ExportSettings(format = "MOV"))
    val viewModel = MainScreenViewModel(FakeClipyRepository(appState))

    viewModel.prepareRenderPipeline(appState)

    assertEquals(RenderPipelineStatus.ERROR, viewModel.renderPipelineState.value.status)
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
  override fun togglePlayback() = Unit
  override fun seekTo(positionMs: Long) = Unit
  override fun seekBy(deltaMs: Long) = Unit
  override fun scrollTimelineTo(scrollOffsetPx: Float) = Unit
  override fun tickPlayback(deltaMs: Long) = Unit
  override fun dragSelectedClip(deltaMs: Long) = Unit
  override fun trimSelectedClipEdge(handle: TrimHandle, deltaMs: Long) = Unit
  override fun reorderSelectedVideoClip(targetIndex: Int) = Unit
  override fun updateTimelineZoom(delta: Float) = Unit
  override fun updateCanvasRatio(ratio: CanvasRatio) = Unit
  override fun splitSelectedClip() = Unit
  override fun deleteSelectedClip() = Unit
  override fun duplicateSelectedClip() = Unit
  override fun trimSelectedClip(deltaMs: Long) = Unit
  override fun moveSelectedClip(deltaMs: Long) = Unit
  override fun updateSelectedTool(tool: EditorTool) = Unit
  override fun adjustSelectedClip(action: ClipAction) = Unit
  override fun transformSelectedClip(deltaX: Float, deltaY: Float, scaleChange: Float, rotationChange: Float) = Unit
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
