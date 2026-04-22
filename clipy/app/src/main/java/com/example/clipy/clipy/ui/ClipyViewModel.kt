package com.example.clipy.clipy.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.clipy.clipy.data.ClipyRepository
import com.example.clipy.clipy.data.ClipyRepository.AppSnapshot
import com.example.clipy.clipy.model.AppLanguage
import com.example.clipy.clipy.model.CropRatio
import com.example.clipy.clipy.model.ExportFormat
import com.example.clipy.clipy.model.Mp4Quality
import com.example.clipy.clipy.model.SaveBehavior
import com.example.clipy.clipy.model.UserPreferences
import com.example.clipy.clipy.model.WatermarkPosition
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClipyViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = ClipyRepository.getInstance(application)

  val appState: StateFlow<AppSnapshot> = repository.appSnapshot.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5_000),
    AppSnapshot(UserPreferences(), repository.draft.value, repository.exportJob.value, emptyList()),
  )

  init {
    viewModelScope.launch { repository.applyDefaultSettingsToDraft() }
  }

  fun completeOnboarding(language: AppLanguage) {
    viewModelScope.launch { repository.completeOnboarding(language.code) }
  }

  fun importVideo(uri: Uri) {
    repository.loadVideo(uri)
  }

  fun updateTrimStart(value: Long) {
    repository.updateTrimWindow(startMs = value)
  }

  fun updateTrimEnd(value: Long) {
    repository.updateTrimWindow(endMs = value)
  }

  fun updatePlayhead(value: Long) {
    repository.setPlayhead(value)
  }

  fun stepPlayheadForward() {
    repository.stepPlayhead(1)
  }

  fun stepPlayheadBackward() {
    repository.stepPlayhead(-1)
  }

  fun updateTimelineZoom(zoom: Float) {
    repository.updateTimelineZoom(zoom)
  }

  fun updateCropRatio(ratio: CropRatio) {
    repository.updateDraft { it.copy(cropRatio = ratio) }
  }

  fun updateSpeed(speed: Float) {
    repository.updateDraft { it.copy(speedMultiplier = speed) }
  }

  fun toggleMuted() {
    repository.updateDraft { it.copy(isMuted = !it.isMuted) }
  }

  fun toggleReverse() {
    repository.updateDraft { it.copy(isReversed = !it.isReversed) }
  }

  fun toggleBoomerang() {
    repository.updateDraft { it.copy(isBoomerang = !it.isBoomerang) }
  }

  fun updateWatermark(text: String) {
    repository.updateDraft { it.copy(watermarkText = text) }
  }

  fun updateWatermarkPosition(position: WatermarkPosition) {
    repository.updateDraft { it.copy(watermarkPosition = position) }
  }

  fun updateFormat(format: ExportFormat) {
    repository.updateDraft { it.copy(exportFormat = format) }
  }

  fun updateGifFps(fps: Int) {
    repository.updateDraft { it.copy(gifFps = fps) }
  }

  fun updateGifResolution(resolution: String) {
    repository.updateDraft { it.copy(gifResolution = resolution) }
  }

  fun updateMp4Quality(quality: Mp4Quality) {
    repository.updateDraft { it.copy(mp4Quality = quality) }
  }

  fun updateOutputName(name: String) {
    repository.updateDraft { it.copy(outputName = name.ifBlank { "clipy_export" }) }
  }

  fun saveSettings(updated: UserPreferences) {
    viewModelScope.launch { repository.updateSettings(updated) }
  }

  fun reuseHistoryRecord(recordId: Long) {
    viewModelScope.launch { repository.reuseHistoryRecord(recordId) }
  }

  fun startExport(): Boolean {
    if (!repository.canExport()) {
      repository.blockExport()
      return false
    }
    viewModelScope.launch { repository.startExport() }
    return true
  }

  fun cancelExport() {
    repository.cancelExport()
  }

  companion object {
    fun factory(application: Application): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = ClipyViewModel(application) as T
      }
  }
}
