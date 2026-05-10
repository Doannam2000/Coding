package com.nantcompany.clipy.processing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.app.EditorSessionViewModel
import com.nantcompany.clipy.design.ClipyErrorState
import com.nantcompany.clipy.design.ClipyLoadingState
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.export.output.LocalOutputRepository
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun ProcessingScreen(
    sessionViewModel: EditorSessionViewModel,
    onNavigate: (AppRoute) -> Unit,
    viewModel: ProcessingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessionState by sessionViewModel.state.collectAsState()
    val pendingRequest = sessionState.pendingRequest
    val outputRepository = remember { LocalOutputRepository() }

    LaunchedEffect(pendingRequest, uiState.activeRequest, uiState.isRunning) {
        if (pendingRequest != null && uiState.activeRequest == null && !uiState.isRunning) {
            val request = sessionViewModel.consumePendingRequest() ?: return@LaunchedEffect
            viewModel.start(request)
        }
    }

    LaunchedEffect(uiState.isCompleted, uiState.output) {
        if (uiState.isCompleted) {
            uiState.output?.let {
                outputRepository.save(it)
                sessionViewModel.setLastOutput(it)
            }
            sessionViewModel.clearPendingRequest()
            viewModel.consumeCompletion()
            onNavigate(AppRoute.RESULT)
        }
    }

    BackHandler(enabled = uiState.isRunning) { }

    val progress = (uiState.progressPercent.coerceIn(0, 100)) / 100f
    val stepText = uiState.statusText
    val progressValue = uiState.progressPercent.coerceIn(0, 100)
    val outputName = pendingRequest?.outputPath?.substringAfterLast('/')?.substringAfterLast('\\') ?: "Unknown"
    val phaseText = when (uiState.phase) {
        ProcessingPhase.Idle -> "Idle"
        ProcessingPhase.Preparing -> "Preparing"
        ProcessingPhase.Processing -> "Processing"
        ProcessingPhase.Success -> "Success"
        ProcessingPhase.Failed -> "Failed"
        ProcessingPhase.Cancelled -> "Cancelled"
    }
    val cancelAvailabilityText = if (uiState.isRunning) {
        "Cancel: Available"
    } else {
        "Cancel: Unavailable (not exporting)"
    }
    val subtitle = buildString {
        append("State: ")
        append(phaseText)
        append("\nStep: ")
        append(stepText)
        append("\n")
        append("Progress: ")
        append(progressValue)
        append("%")
        if (uiState.isRunning && progressValue <= 0) {
            append("\nPreparing output verification...")
        }
        uiState.errorMessage?.let {
            append("\nError: ")
            append(it)
        }
    }

    ScreenLayout(
        title = "Exporting",
        subtitle = subtitle,
        primaryActionLabel = if (uiState.isRunning) "Cancel" else if (uiState.errorMessage != null) "Retry" else "Back Home",
        onPrimaryAction = {
            when {
                uiState.isRunning -> viewModel.cancel()
                uiState.errorMessage != null -> {
                    viewModel.clearFailure()
                    pendingRequest?.let { viewModel.start(it) }
                }
                else -> onNavigate(AppRoute.HOME)
            }
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Output", style = MaterialTheme.typography.titleSmall)
                        Text(outputName, style = MaterialTheme.typography.bodySmall)
                        Text("Estimated size: Calculating...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(cancelAvailabilityText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (uiState.isRunning || uiState.progressPercent in 1..99) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    ClipyLoadingState(label = stepText)
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(stepText, style = MaterialTheme.typography.bodyMedium)
                }
                if (uiState.errorMessage != null) {
                    ClipyErrorState(
                        message = uiState.errorMessage!!,
                        onRetry = {
                            viewModel.clearFailure()
                            pendingRequest?.let { viewModel.start(it) }
                        }
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.clearFailure()
                            pendingRequest?.let { viewModel.start(it) }
                        }
                    ) {
                        Text("Retry Export")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigate(AppRoute.HOME) }
                    ) {
                        Text("Back Home")
                    }
                }
                if (!uiState.isRunning && uiState.errorMessage == null && !uiState.isCompleted) {
                    OutlinedButton(onClick = { onNavigate(AppRoute.HOME) }) { Text("Back Home") }
                }
            }
        }
    )
}
