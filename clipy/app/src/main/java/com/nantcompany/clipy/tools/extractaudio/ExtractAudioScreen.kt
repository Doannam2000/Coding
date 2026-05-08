package com.nantcompany.clipy.tools.extractaudio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.edit.tools.extractaudio.ExtractAudioRequest
import com.nantcompany.clipy.edit.tools.extractaudio.ExtractAudioValidator
import com.nantcompany.clipy.export.job.ProcessingRequest

@Composable
fun ExtractAudioScreen(
    inputPath: String?,
    onSubmitRequest: (ProcessingRequest) -> Unit
) {
    val context = LocalContext.current
    var format by remember { mutableStateOf("mp3") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Extract Audio", style = MaterialTheme.typography.headlineSmall)
        Text(inputPath ?: "No video selected", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = format,
            onValueChange = { format = it.lowercase() },
            label = { Text("Format (mp3/aac/wav)") }
        )

        validationError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            enabled = !inputPath.isNullOrBlank(),
            onClick = {
                val input = inputPath ?: return@Button
                val ext = when (format.lowercase()) {
                    "aac", "m4a" -> "m4a"
                    "wav" -> "wav"
                    else -> "mp3"
                }
                val request = ExtractAudioRequest(
                    inputPath = input,
                    outputPath = MediaFileUtils.createOutputPath(context, "extract_audio", ext),
                    format = format
                )
                val result = ExtractAudioValidator().validate(request)
                if (!result.isValid) {
                    validationError = result.errorMessage
                } else {
                    validationError = null
                    onSubmitRequest(ProcessingRequest.ExtractAudio(request))
                }
            }
        ) {
            Text("Start Processing")
        }
    }
}
