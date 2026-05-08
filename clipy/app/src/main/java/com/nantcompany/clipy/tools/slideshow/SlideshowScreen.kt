package com.nantcompany.clipy.tools.slideshow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.edit.tools.slideshow.SlideshowRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun SlideshowScreen(
    imagePaths: List<String>,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    viewModel: SlideshowViewModel = viewModel()
) {
    val context = LocalContext.current
    var secondsPerImage by remember { mutableStateOf("3") }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Slideshow", style = MaterialTheme.typography.headlineSmall)
        Text("Selected images: ${imagePaths.size}", style = MaterialTheme.typography.bodyMedium)
        if (imagePaths.isEmpty()) {
            Text("You need to pick images before creating slideshow.", style = MaterialTheme.typography.bodyMedium)
        } else {
            imagePaths.take(5).forEach { path ->
                Text(path, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Button(onClick = { onNavigate(AppRoute.PICK_IMAGES) }) {
            Text(if (imagePaths.isEmpty()) "Pick Images" else "Change Images")
        }

        OutlinedTextField(
            value = secondsPerImage,
            onValueChange = { secondsPerImage = it },
            label = { Text("Seconds per image") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        uiState.validationError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            enabled = imagePaths.isNotEmpty(),
            onClick = {
                val request = SlideshowRequest(
                    imagePaths = imagePaths,
                    outputPath = MediaFileUtils.createOutputPath(context, "slideshow", "mp4"),
                    secondsPerImage = secondsPerImage.toIntOrNull() ?: -1
                )
                if (viewModel.validate(request)) {
                    onSubmitRequest(ProcessingRequest.Slideshow(request))
                }
            }
        ) {
            Text("Start Processing")
        }

        if (imagePaths.isNotEmpty()) {
            Button(onClick = { onNavigate(AppRoute.PICK_IMAGES) }) {
                Text("Pick More Images")
            }
        }
    }
}
