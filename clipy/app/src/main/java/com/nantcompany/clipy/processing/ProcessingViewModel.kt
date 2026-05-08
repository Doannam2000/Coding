package com.nantcompany.clipy.processing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nantcompany.clipy.export.job.ProcessEvent
import com.nantcompany.clipy.export.job.ProcessingJobManager
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.export.output.OutputMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProcessingUiState(
    val progressPercent: Int = 0,
    val statusText: String = "Idle",
    val isRunning: Boolean = false,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false,
    val output: OutputMedia? = null,
    val activeRequest: ProcessingRequest? = null
)

class ProcessingViewModel(
    private val jobManager: ProcessingJobManager = ProcessingJobManager()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProcessingUiState())
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    private var lastStartedRequest: ProcessingRequest? = null

    private fun setTerminalState(state: ProcessingUiState) {
        _uiState.value = state
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun start(request: ProcessingRequest) {
        if (_uiState.value.isRunning && _uiState.value.activeRequest == request) return

        lastStartedRequest = request
        _uiState.value = ProcessingUiState(
            progressPercent = 5,
            statusText = "Preparing job...",
            isRunning = true,
            activeRequest = request
        )

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                progressPercent = 10,
                statusText = "Preparing..."
            )

            when (
                val event = jobManager.process(
                    request,
                    object : ProcessingJobManager.ProgressCallback {
                        override fun onProgress(event: ProcessEvent.Progress) {
                            val current = _uiState.value
                            if (!current.isRunning || current.activeRequest != request) return
                            _uiState.value = current.copy(
                                progressPercent = event.percent.coerceIn(0, 99),
                                statusText = event.statusText
                            )
                        }
                    }
                )
            ) {
                is ProcessEvent.Completed -> {
                    setTerminalState(
                        ProcessingUiState(
                            progressPercent = 100,
                            statusText = "Completed",
                            isRunning = false,
                            isCompleted = true,
                            output = event.output,
                            activeRequest = null
                        )
                    )
                }

                is ProcessEvent.Cancelled -> {
                    setTerminalState(
                        ProcessingUiState(
                            progressPercent = 0,
                            statusText = "Cancelled",
                            isRunning = false,
                            activeRequest = null
                        )
                    )
                }

                is ProcessEvent.Failed -> {
                    setTerminalState(
                        ProcessingUiState(
                            progressPercent = 0,
                            statusText = "Failed",
                            isRunning = false,
                            errorMessage = event.error.message ?: "Unknown processing error",
                            activeRequest = null
                        )
                    )
                }

                else -> {
                    setTerminalState(
                        _uiState.value.copy(
                            progressPercent = 100,
                            statusText = "Done",
                            isRunning = false,
                            isCompleted = true,
                            activeRequest = null
                        )
                    )
                }
            }
        }
    }

    fun consumeCompletion() {
        _uiState.value = _uiState.value.copy(isCompleted = false)
    }

    fun clearFailure() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun cancel() {
        jobManager.cancelProcessing()
        setTerminalState(
            _uiState.value.copy(
                isRunning = false,
                statusText = "Cancelled",
                activeRequest = null
            )
        )
    }
}
