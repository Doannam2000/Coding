package com.nantcompany.clipy.tools.crop

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
import androidx.compose.material3.OutlinedTextField
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
import com.nantcompany.clipy.edit.tools.crop.CropRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File

@Composable
fun CropScreen(
    inputPath: String?,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    onNavigate: (AppRoute) -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { CropViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    val videoFile = inputPath?.let(::File)
    val videoExists = videoFile?.exists() == true

    ScreenLayout(
        title = "Crop Video",
        subtitle = "Define the crop area",
        primaryActionLabel = "Apply Crop",
        onPrimaryAction = {
            val input = inputPath ?: return@ScreenLayout
            val req = CropRequest(
                inputPath = input,
                outputPath = MediaFileUtils.createOutputPath(context, "crop", "mp4"),
                x = uiState.x,
                y = uiState.y,
                width = uiState.width,
                height = uiState.height
            )
            if (viewModel.validate(req)) {
                onSubmitRequest(ProcessingRequest.Crop(req))
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

        if (uiState.videoWidth > 0 && uiState.videoHeight > 0) {
            Text("Video: ${uiState.videoWidth}×${uiState.videoHeight}", style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.textPrimary)
            Spacer(modifier = Modifier.height(4.dp))
        }

        Button(onClick = { viewModel.autoCrop() }, modifier = Modifier.fillMaxWidth()) {
            Text("Auto Crop (Full Video)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.x.toString(),
            onValueChange = { viewModel.updateX(it.toIntOrNull() ?: 0) },
            label = { Text("X (pixels)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.y.toString(),
            onValueChange = { viewModel.updateY(it.toIntOrNull() ?: 0) },
            label = { Text("Y (pixels)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.width.toString(),
            onValueChange = { viewModel.updateWidth(it.toIntOrNull() ?: 0) },
            label = { Text("Width (pixels)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.height.toString(),
            onValueChange = { viewModel.updateHeight(it.toIntOrNull() ?: 0) },
            label = { Text("Height (pixels)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        uiState.validationError?.let {
            Text(it, color = ClipyDesignTokens.ErrorRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}