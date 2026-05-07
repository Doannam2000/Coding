package com.nantcompany.clipy.processing

import androidx.lifecycle.ViewModel
import com.nantcompany.clipy.export.job.ProcessEvent
import com.nantcompany.clipy.export.job.ProcessingJobManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProcessingUiState(
    val progressPercent: Int = 0,
    val statusText: String = "Idle"
)

class ProcessingViewModel(
    private val jobManager: ProcessingJobManager = ProcessingJobManager()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProcessingUiState())
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    fun start() {
        when (val event = jobManager.startMockJob()) {
            is ProcessEvent.Progress -> {
                _uiState.value = _uiState.value.copy(
                    progressPercent = event.percent,
                    statusText = "Processing"
                )
            }

            is ProcessEvent.Completed -> {
                _uiState.value = _uiState.value.copy(
                    progressPercent = 100,
                    statusText = "Completed"
                )
            }

            is ProcessEvent.Failed -> {
                _uiState.value = _uiState.value.copy(
                    statusText = event.error.message
                )
            }
        }
    }

    fun cancel() {
        jobManager.cancelProcessing()
        _uiState.value = _uiState.value.copy(statusText = "Cancelled")
    }
}
