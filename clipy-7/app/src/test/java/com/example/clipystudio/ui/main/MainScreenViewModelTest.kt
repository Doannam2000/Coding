package com.example.clipystudio.ui.main

import com.example.clipystudio.data.AppState
import com.example.clipystudio.data.CanvasRatio
import com.example.clipystudio.data.ClipAction
import com.example.clipystudio.data.DataRepository
import com.example.clipystudio.data.EditorTool
import com.example.clipystudio.data.ExportSettings
import com.example.clipystudio.data.LanguageCode
import com.example.clipystudio.data.MediaType
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
}

private class FakeClipyRepository : DataRepository {
  override val appState: Flow<AppState> = flowOf(AppState(hasCompletedIntro = true))
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
  override fun seekBy(deltaMs: Long) = Unit
  override fun splitSelectedClip() = Unit
  override fun duplicateSelectedClip() = Unit
  override fun trimSelectedClip(deltaMs: Long) = Unit
  override fun updateSelectedTool(tool: EditorTool) = Unit
  override fun adjustSelectedClip(action: ClipAction) = Unit
  override fun undo() = Unit
  override fun redo() = Unit
  override fun updateExportSettings(settings: ExportSettings) = Unit
  override fun startExport() = Unit
  override fun cancelExport() = Unit
  override fun clearExportResult() = Unit
  override fun clearCache() = Unit
}
