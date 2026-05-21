package com.nantcompany.clipy.processing

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nantcompany.clipy.export.job.ClipyExportProvider
import com.nantcompany.clipy.export.job.ProcessEvent
import com.nantcompany.clipy.export.job.ProcessingJobManager
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.export.job.ProcessingWorker
import com.nantcompany.clipy.export.output.OutputMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProcessingPhase { Idle, Preparing, Processing, Success, Failed, Cancelled }

data class ProcessingUiState(
    val progressPercent: Int = 0,
    val statusText: String = "Idle",
    val phase: ProcessingPhase = ProcessingPhase.Idle,
    val isRunning: Boolean = false,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false,
    val output: OutputMedia? = null,
    val activeRequest: ProcessingRequest? = null
)

@UnstableApi
class ProcessingViewModel(
    private val context: Context,
    private val jobManager: ProcessingJobManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProcessingUiState())
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()
    
    private val workManager = WorkManager.getInstance(context)

    fun start(request: ProcessingRequest) {
        if (_uiState.value.isRunning) return
        
        _uiState.update { it.copy(
            progressPercent = 5, 
            statusText = "Preparing...", 
            phase = ProcessingPhase.Preparing, 
            isRunning = true, 
            activeRequest = request 
        ) }

        val workRequest = OneTimeWorkRequestBuilder<ProcessingWorker>()
            .setInputData(Data.Builder().putString("request_json", request.toJson()).build())
            .addTag("clipy_job")
            .build()

        workManager.enqueueUniqueWork(
            "clipy_export",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo == null) return@collect
                
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt("progress", 0)
                        val status = workInfo.progress.getString("status") ?: "Processing..."
                        _uiState.update { it.copy(
                            progressPercent = progress, 
                            statusText = status, 
                            phase = ProcessingPhase.Processing 
                        ) }
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val path = workInfo.outputData.getString("output_path") ?: ""
                        val id = workInfo.outputData.getString("output_id") ?: ""
                        val output = OutputMedia(
                            id = id,
                            path = path,
                            fileName = path.substringAfterLast('/'),
                            operation = request::class.simpleName ?: "Export",
                            sizeInBytes = java.io.File(path).length(),
                            createdAtEpochMs = System.currentTimeMillis()
                        )
                        _uiState.update { it.copy(
                            progressPercent = 100, 
                            statusText = "Complete", 
                            phase = ProcessingPhase.Success, 
                            isRunning = false, 
                            isCompleted = true, 
                            output = output, 
                            activeRequest = null 
                        ) }
                    }
                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString("error") ?: "Processing failed"
                        _uiState.update { it.copy(
                            isRunning = false, 
                            phase = ProcessingPhase.Failed, 
                            errorMessage = error, 
                            activeRequest = null 
                        ) }
                    }
                    WorkInfo.State.CANCELLED -> {
                        _uiState.update { it.copy(
                            isRunning = false, 
                            phase = ProcessingPhase.Cancelled, 
                            activeRequest = null 
                        ) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun consumeCompletion() { _uiState.update { it.copy(isCompleted = false) } }
    fun clearFailure() { _uiState.update { it.copy(errorMessage = null, phase = ProcessingPhase.Idle) } }
    fun cancel() { 
        workManager.cancelUniqueWork("clipy_export")
        jobManager.cancelProcessing() 
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProcessingViewModel(context, ClipyExportProvider.getJobManager(context)) as T
        }
    }
}
