package com.nantcompany.clipy.processing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun ProcessingScreen(
    onNavigate: (AppRoute) -> Unit,
    viewModel: ProcessingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val subtitle = remember(uiState.progressPercent, uiState.statusText) {
        "Progress: ${uiState.progressPercent}%\nStatus: ${uiState.statusText}"
    }
    ScreenLayout(
        title = "Processing",
        subtitle = subtitle,
        primaryActionLabel = "Finish",
        onPrimaryAction = { onNavigate(AppRoute.RESULT) }
    )
}
