package com.nantcompany.clipy.processing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.nantcompany.clipy.app.EditorSessionViewModel
import com.nantcompany.clipy.design.ClipyErrorState
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.design.ClipySecondaryButton
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens

@OptIn(UnstableApi::class)
@Composable
fun ProcessingScreen(
    sessionViewModel: EditorSessionViewModel,
    onNavigate: (AppRoute) -> Unit,
    viewModel: ProcessingViewModel = viewModel(factory = ProcessingViewModel.Factory(LocalContext.current))
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessionState by sessionViewModel.state.collectAsState()
    val pendingRequest = sessionState.pendingRequest
    val retryRequest = uiState.activeRequest ?: pendingRequest

    LaunchedEffect(pendingRequest, uiState.activeRequest, uiState.isRunning) {
        if (pendingRequest != null && uiState.activeRequest == null && !uiState.isRunning) {
            val request = sessionViewModel.consumePendingRequest() ?: return@LaunchedEffect
            viewModel.start(request)
        }
    }

    LaunchedEffect(uiState.isCompleted, uiState.output) {
        if (uiState.isCompleted) {
            uiState.output?.let {
                sessionViewModel.setLastOutput(it)
            }
            sessionViewModel.clearPendingRequest()
            viewModel.consumeCompletion()
            onNavigate(AppRoute.RESULT)
        }
    }

    BackHandler(enabled = uiState.isRunning) { }

    val progress = (uiState.progressPercent.coerceIn(0, 100)) / 100f
    val outputName = retryRequest?.outputPath?.substringAfterLast('/')?.substringAfterLast('\\') ?: "Clipy_Output.mp4"

    val subtitle = when {
        uiState.errorMessage != null -> "Export failed"
        uiState.phase == ProcessingPhase.Preparing -> "Preparing your media..."
        uiState.phase == ProcessingPhase.Processing -> "Processing: ${uiState.progressPercent}%"
        else -> "Working on your request..."
    }

    ClipyScaffold(
        title = "Exporting",
        onBackClick = { if (!uiState.isRunning) onNavigate(AppRoute.HOME) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = Color.White)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Output File", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Text(outputName, style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.secondaryText)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = ClipyDesignTokens.primaryAccent,
                    trackColor = ClipyDesignTokens.cardSurface
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(uiState.statusText, style = MaterialTheme.typography.bodyMedium, color = ClipyDesignTokens.secondaryText)
                    Text("${uiState.progressPercent}%", style = MaterialTheme.typography.bodyMedium, color = ClipyDesignTokens.primaryAccent)
                }
            }

            if (uiState.errorMessage != null) {
                ClipyErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = {
                        viewModel.clearFailure()
                        retryRequest?.let { viewModel.start(it) }
                    }
                )
            }

            // NOTE: weight(1f) is removed because it's inside a scrollable Column
            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (uiState.isRunning) {
                    ClipyPrimaryButton(
                        label = "Cancel Export",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.cancel() }
                    )
                } else if (uiState.errorMessage != null) {
                    ClipyPrimaryButton(
                        label = "Retry Export",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.clearFailure()
                            retryRequest?.let { viewModel.start(it) }
                        }
                    )
                    ClipySecondaryButton(
                        label = "Back to Home",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigate(AppRoute.HOME) }
                    )
                } else {
                    ClipyPrimaryButton(
                        label = "Back Home",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigate(AppRoute.HOME) }
                    )
                }
            }
        }
    }
}
