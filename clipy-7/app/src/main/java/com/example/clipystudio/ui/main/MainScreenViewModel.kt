package com.example.clipystudio.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clipystudio.data.AppState
import com.example.clipystudio.data.AudioSource
import com.example.clipystudio.data.CanvasRatio
import com.example.clipystudio.data.ClipAction
import com.example.clipystudio.data.DataRepository
import com.example.clipystudio.data.DefaultDataRepository
import com.example.clipystudio.data.EditorTool
import com.example.clipystudio.data.ExportSettings
import com.example.clipystudio.data.LanguageCode
import com.example.clipystudio.data.MediaType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainScreenViewModel(private val dataRepository: DataRepository = DefaultDataRepository()) : ViewModel() {
  val uiState: StateFlow<MainScreenUiState> =
    dataRepository.appState
      .map<AppState, MainScreenUiState>(MainScreenUiState::Success)
      .catch { emit(MainScreenUiState.Error(it)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState.Loading)

  fun completeIntro() = dataRepository.completeIntro()
  fun setLanguage(languageCode: LanguageCode) = dataRepository.setLanguage(languageCode)
  fun createProject(ratio: CanvasRatio) = dataRepository.createProject(ratio)
  fun renameProject(projectId: String, name: String) = dataRepository.renameProject(projectId, name)
  fun duplicateProject(projectId: String) = dataRepository.duplicateProject(projectId)
  fun deleteProject(projectId: String) = dataRepository.deleteProject(projectId)
  fun openProject(projectId: String) = dataRepository.openProject(projectId)
  fun addImportedAsset(type: MediaType, uri: String? = null, displayName: String? = null, sizeBytes: Long? = null) = dataRepository.addImportedAsset(type, uri, displayName, sizeBytes)
  fun removeImportedAsset(assetId: String) = dataRepository.removeImportedAsset(assetId)
  fun addImportsToProject() = dataRepository.addImportsToProject()
  fun selectClip(clipId: String) = dataRepository.selectClip(clipId)
  fun togglePlayback() = dataRepository.togglePlayback()
  fun seekTo(positionMs: Long) = dataRepository.seekTo(positionMs)
  fun seekBy(deltaMs: Long) = dataRepository.seekBy(deltaMs)
  fun updateTimelineZoom(delta: Float) = dataRepository.updateTimelineZoom(delta)
  fun updateCanvasRatio(ratio: CanvasRatio) = dataRepository.updateCanvasRatio(ratio)
  fun splitSelectedClip() = dataRepository.splitSelectedClip()
  fun deleteSelectedClip() = dataRepository.deleteSelectedClip()
  fun duplicateSelectedClip() = dataRepository.duplicateSelectedClip()
  fun trimSelectedClip(deltaMs: Long) = dataRepository.trimSelectedClip(deltaMs)
  fun moveSelectedClip(deltaMs: Long) = dataRepository.moveSelectedClip(deltaMs)
  fun updateSelectedTool(tool: EditorTool) = dataRepository.updateSelectedTool(tool)
  fun adjustSelectedClip(action: ClipAction) = dataRepository.adjustSelectedClip(action)
  fun transformSelectedClip(deltaX: Float, deltaY: Float, scaleChange: Float, rotationChange: Float) = dataRepository.transformSelectedClip(deltaX, deltaY, scaleChange, rotationChange)
  fun addAudioClipAtPlayhead(title: String, source: AudioSource) = dataRepository.addAudioClipAtPlayhead(title, source)
  fun addTextClipAtPlayhead(content: String, fontSizeSp: Float, color: String, backgroundColor: String?, strokeEnabled: Boolean, shadowEnabled: Boolean, alignment: String, animation: String) = dataRepository.addTextClipAtPlayhead(content, fontSizeSp, color, backgroundColor, strokeEnabled, shadowEnabled, alignment, animation)
  fun undo() = dataRepository.undo()
  fun redo() = dataRepository.redo()
  fun updateExportSettings(settings: ExportSettings) = dataRepository.updateExportSettings(settings)
  fun startExport() = dataRepository.startExport()
  fun completeExport() = dataRepository.completeExport()
  fun cancelExport() = dataRepository.cancelExport()
  fun clearExportResult() = dataRepository.clearExportResult()
  fun clearCache() = dataRepository.clearCache()
}

sealed interface MainScreenUiState {
  data object Loading : MainScreenUiState
  data class Error(val throwable: Throwable) : MainScreenUiState
  data class Success(val appState: AppState) : MainScreenUiState
}
