package com.nantcompany.clipy.tools.textoverlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.design.*
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File

@Composable
fun TextOverlayScreen(
    inputPath: String?,
    onNavigate: (AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    viewModel: TextOverlayViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val videoExists = remember(inputPath) { inputPath?.let { File(it).exists() } == true }

    ClipyScaffold(
        title = "Text Overlay",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Preview", style = MaterialTheme.typography.titleSmall, color = ClipyDesignTokens.textPrimary)
                }
            }

            Text(
                text = "Add Text Layer",
                style = MaterialTheme.typography.bodyLarge,
                color = if (!videoExists) ClipyDesignTokens.textMuted else ClipyDesignTokens.textPrimary
            )

            Spacer(modifier = Modifier.weight(1f))

            ClipyPrimaryButton(
                label = "Apply Text",
                enabled = videoExists,
                onClick = { /* Export */ },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
        }
    }
}
