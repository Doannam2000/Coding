package com.nantcompany.clipy.processing

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.app.EditorSessionViewModel
import com.nantcompany.clipy.design.ScreenLayout
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

    LaunchedEffect(pendingRequest, uiState.activeRequest) {
        val request = pendingRequest
        if (request != null && uiState.activeRequest != request && !uiState.isRunning) {
            viewModel.start(request)
        }
    }

    LaunchedEffect(uiState.isCompleted, uiState.output) {
        if (uiState.isCompleted) {
            uiState.output?.let { sessionViewModel.setLastOutput(it) }
            sessionViewModel.clearPendingRequest()
            viewModel.consumeCompletion()
            onNavigate(AppRoute.RESULT)
        }
    }

    val subtitle = remember(uiState.progressPercent, uiState.statusText, uiState.errorMessage) {
        buildString {
            append("Progress: ${uiState.progressPercent}%\n")
            append("Status: ${uiState.statusText}")
            uiState.errorMessage?.let { message ->
                append("\nError: ")
                append(message)
            }
        }
    }

    val actionLabel = when {
        uiState.isRunning -> "Cancel"
        uiState.errorMessage != null -> "Try Again"
        else -> "Back Home"
    }

    ScreenLayout(
        title = "Processing",
        subtitle = subtitle,
        primaryActionLabel = actionLabel,
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
            LinearProgressIndicator(progress = { uiState.progressPercent / 100f })
            if (uiState.isRunning) {
                Text("Processing in background...")
            }
        }
    )
}
