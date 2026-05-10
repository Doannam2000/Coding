package com.nantcompany.clipy.settings

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.export.output.LocalOutputRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class SettingsUiState(
    val enableHardwareAcceleration: Boolean = true,
    val keepOriginalFiles: Boolean = true,
    val outputHistoryPath: String = "",
    val appVersionLabel: String = "Clipy",
    val message: String? = null,
    val confirmClearHistory: Boolean = false
)

class SettingsViewModel(
    private val outputRepository: LocalOutputRepository = LocalOutputRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            outputHistoryPath = resolveOutputHistoryPath(),
            appVersionLabel = resolveAppVersionLabel()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleHardwareAcceleration() {
        _uiState.value = _uiState.value.copy(
            enableHardwareAcceleration = !_uiState.value.enableHardwareAcceleration
        )
    }

    fun toggleKeepOriginal() {
        _uiState.value = _uiState.value.copy(
            keepOriginalFiles = !_uiState.value.keepOriginalFiles
        )
    }

    fun clearTempFiles() {
        val root = File(System.getProperty("java.io.tmpdir") ?: ".", "clipy")
        val removed = runCatching {
            root.listFiles()
                ?.filter { it.isFile && (it.name.startsWith("clipy-") || it.name.endsWith(".tmp")) }
                ?.count { it.delete() } ?: 0
        }.getOrDefault(0)
        _uiState.value = _uiState.value.copy(
            message = "Cleared $removed temporary files. Exported media files were not removed."
        )
    }

    fun askClearHistory() {
        _uiState.value = _uiState.value.copy(confirmClearHistory = true)
    }

    fun cancelClearHistory() {
        _uiState.value = _uiState.value.copy(confirmClearHistory = false)
    }

    fun clearHistory() {
        outputRepository.clear()
        _uiState.value = _uiState.value.copy(
            confirmClearHistory = false,
            message = "Export history cleared."
        )
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun resolveOutputHistoryPath(): String {
        val root = File(System.getProperty("java.io.tmpdir") ?: ".")
        return File(root, "clipy/clipy_output_history.json").absolutePath
    }

    private fun resolveAppVersionLabel(): String {
        return "Clipy v1.0"
    }
}
