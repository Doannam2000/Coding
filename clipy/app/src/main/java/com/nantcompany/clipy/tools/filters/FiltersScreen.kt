package com.nantcompany.clipy.tools.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.edit.tools.filters.FiltersRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File

@Composable
fun FiltersScreen(
    inputPath: String?,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    onNavigate: (AppRoute) -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { FiltersViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    val videoFile = inputPath?.let(::File)
    val videoExists = videoFile?.exists() == true

    ScreenLayout(
        title = "Filters",
        subtitle = "Adjust brightness, contrast, and saturation",
        primaryActionLabel = "Apply Filters",
        onPrimaryAction = {
            val input = inputPath ?: return@ScreenLayout
            val req = FiltersRequest(
                inputPath = input,
                outputPath = MediaFileUtils.createOutputPath(context, "filters", "mp4"),
                brightness = uiState.brightness,
                contrast = uiState.contrast,
                saturation = uiState.saturation
            )
            if (viewModel.validate(req)) {
                onSubmitRequest(ProcessingRequest.Filters(req))
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Preview", style = MaterialTheme.typography.titleSmall, color = ClipyDesignTokens.textPrimary)
                Text(
                    if (videoExists) videoFile.name else "No video selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!videoExists) ClipyDesignTokens.textMuted else ClipyDesignTokens.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Brightness: ${"%.1f".format(uiState.brightness)}", style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.textPrimary)
        Slider(
            value = uiState.brightness,
            onValueChange = { viewModel.updateBrightness(it) },
            valueRange = -1f..1f,
            steps = 40,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = ClipyDesignTokens.NeonPurple,
                activeTrackColor = ClipyDesignTokens.NeonPurple,
                inactiveTrackColor = Color(0xFF2D3748)
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text("Contrast: ${"%.1f".format(uiState.contrast)}", style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.textPrimary)
        Slider(
            value = uiState.contrast,
            onValueChange = { viewModel.updateContrast(it) },
            valueRange = -1f..1f,
            steps = 40,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = ClipyDesignTokens.NeonPurple,
                activeTrackColor = ClipyDesignTokens.NeonPurple,
                inactiveTrackColor = Color(0xFF2D3748)
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text("Saturation: ${"%.1f".format(uiState.saturation)}", style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.textPrimary)
        Slider(
            value = uiState.saturation,
            onValueChange = { viewModel.updateSaturation(it) },
            valueRange = 0f..3f,
            steps = 60,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = ClipyDesignTokens.NeonPurple,
                activeTrackColor = ClipyDesignTokens.NeonPurple,
                inactiveTrackColor = Color(0xFF2D3748)
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.reset() }, modifier = Modifier.weight(1f)) {
                Text("Reset")
            }
        }

        uiState.validationError?.let {
            Text(it, color = ClipyDesignTokens.ErrorRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}