package com.nantcompany.clipy.tools.merge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.edit.tools.merge.MergeRequest
import com.nantcompany.clipy.edit.tools.merge.MergeValidator
import com.nantcompany.clipy.export.job.ProcessingRequest

@Composable
fun MergeVideoScreen(
    inputPaths: List<String>,
    onSubmitRequest: (ProcessingRequest) -> Unit
) {
    val context = LocalContext.current
    val validationError = remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Merge Video", style = MaterialTheme.typography.headlineSmall)
        Text("Selected clips: ${inputPaths.size}", style = MaterialTheme.typography.bodyMedium)
        inputPaths.take(3).forEach { path ->
            Text(path, style = MaterialTheme.typography.bodyMedium)
        }

        validationError.value?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            enabled = inputPaths.size >= 2,
            onClick = {
                val request = MergeRequest(
                    inputPaths = inputPaths,
                    outputPath = MediaFileUtils.createOutputPath(context, "merge", "mp4")
                )
                val result = MergeValidator().validate(request)
                if (!result.isValid) {
                    validationError.value = result.errorMessage
                } else {
                    validationError.value = null
                    onSubmitRequest(ProcessingRequest.Merge(request))
                }
            }
        ) {
            Text("Start Processing")
        }
    }
}
