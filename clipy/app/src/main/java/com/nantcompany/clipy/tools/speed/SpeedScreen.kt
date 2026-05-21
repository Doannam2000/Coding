package com.nantcompany.clipy.tools.speed

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
fun SpeedScreen(
    inputPath: String?,
    onNavigate: (AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    viewModel: SpeedViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val videoExists = remember(inputPath) { inputPath?.let { File(it).exists() } == true }

    ClipyScaffold(
        title = "Speed Control",
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
                "Speed: ${"%.2f".format(uiState.speedFactor)}x",
                style = MaterialTheme.typography.bodyLarge,
                color = if (!videoExists) ClipyDesignTokens.textMuted else ClipyDesignTokens.textPrimary
            )

            Slider(
                value = uiState.speedFactor,
                onValueChange = { viewModel.setSpeedFactor(it) },
                valueRange = 0.25f..4.0f,
                colors = SliderDefaults.colors(thumbColor = ClipyDesignTokens.primaryAccent, activeTrackColor = ClipyDesignTokens.primaryAccent)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0.25x", style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.textMuted)
                Text("4.0x", style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.textMuted)
            }

            Spacer(modifier = Modifier.weight(1f))

            ClipyPrimaryButton(
                label = "Apply Speed",
                enabled = videoExists,
                onClick = { /* Export */ },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
        }
    }
}
