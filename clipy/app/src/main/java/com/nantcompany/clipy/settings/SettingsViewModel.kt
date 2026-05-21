package com.nantcompany.clipy.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
    private val context: Context,
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
        val importsDir = File(context.filesDir, "imports")
        val outputsDir = File(context.filesDir, "outputs")
        
        var removed = 0
        removed += deleteDirectoryContents(importsDir)
        removed += deleteDirectoryContents(outputsDir, keepFiles = setOf("clipy_output_history.json"))
        
        _uiState.value = _uiState.value.copy(
            message = "Cleared $removed files from cache."
        )
    }

    private fun deleteDirectoryContents(dir: File, keepFiles: Set<String> = emptySet()): Int {
        if (!dir.exists()) return 0
        val files = dir.listFiles() ?: return 0
        var count = 0
        files.forEach { file ->
            if (file.name !in keepFiles) {
                if (file.isDirectory) {
                    count += deleteDirectoryContents(file)
                    file.delete()
                } else {
                    if (file.delete()) count++
                }
            }
        }
        return count
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
        return File(context.filesDir, "outputs/clipy_output_history.json").absolutePath
    }

    private fun resolveAppVersionLabel(): String {
        return "Clipy v1.0"
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(context) as T
        }
    }
}
