package com.nantcompany.clipy.tools.compress

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.edit.tools.compress.CompressRequest
import com.nantcompany.clipy.edit.tools.compress.CompressValidator
import com.nantcompany.clipy.export.job.ProcessingRequest

@Composable
fun CompressVideoScreen(
    inputPath: String?,
    onSubmitRequest: (ProcessingRequest) -> Unit
) {
    val context = LocalContext.current
    var bitrateText by remember { mutableStateOf("1200") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Compress Video", style = MaterialTheme.typography.headlineSmall)
        Text(inputPath ?: "No video selected", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = bitrateText,
            onValueChange = { bitrateText = it },
            label = { Text("Bitrate (kbps)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        validationError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            enabled = !inputPath.isNullOrBlank(),
            onClick = {
                val input = inputPath ?: return@Button
                val request = CompressRequest(
                    inputPath = input,
                    outputPath = MediaFileUtils.createOutputPath(context, "compress", "mp4"),
                    bitrateKbps = bitrateText.toIntOrNull() ?: -1
                )
                val result = CompressValidator().validate(request)
                if (!result.isValid) {
                    validationError = result.errorMessage
                } else {
                    validationError = null
                    onSubmitRequest(ProcessingRequest.Compress(request))
                }
            }
        ) {
            Text("Start Processing")
        }
    }
}
