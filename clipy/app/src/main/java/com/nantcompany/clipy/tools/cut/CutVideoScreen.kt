package com.nantcompany.clipy.tools.cut

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
import com.nantcompany.clipy.edit.tools.cut.CutRequest
import com.nantcompany.clipy.edit.tools.cut.CutValidator
import com.nantcompany.clipy.export.job.ProcessingRequest

@Composable
fun CutVideoScreen(
    inputPath: String?,
    onSubmitRequest: (ProcessingRequest) -> Unit
) {
    val context = LocalContext.current
    var startMsText by remember { mutableStateOf("0") }
    var endMsText by remember { mutableStateOf("5000") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Cut Video", style = MaterialTheme.typography.headlineSmall)
        Text(inputPath ?: "No video selected", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = startMsText,
            onValueChange = { startMsText = it },
            label = { Text("Start (ms)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = endMsText,
            onValueChange = { endMsText = it },
            label = { Text("End (ms)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        validationError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            enabled = !inputPath.isNullOrBlank(),
            onClick = {
                val input = inputPath ?: return@Button
                val request = CutRequest(
                    inputPath = input,
                    outputPath = MediaFileUtils.createOutputPath(context, "cut", "mp4"),
                    startMs = startMsText.toLongOrNull() ?: -1L,
                    endMs = endMsText.toLongOrNull() ?: -1L
                )
                val result = CutValidator().validate(request)
                if (!result.isValid) {
                    validationError = result.errorMessage
                } else {
                    validationError = null
                    onSubmitRequest(ProcessingRequest.Cut(request))
                }
            }
        ) {
            Text("Start Processing")
        }
    }
}
