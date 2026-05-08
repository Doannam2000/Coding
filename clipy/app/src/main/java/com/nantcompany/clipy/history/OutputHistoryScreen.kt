package com.nantcompany.clipy.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OutputHistoryScreen(
    onOutputSelected: (com.nantcompany.clipy.export.output.OutputMedia) -> Unit,
    viewModel: OutputHistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadHistory() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Output History", style = MaterialTheme.typography.headlineSmall)
        if (uiState.outputs.isEmpty()) {
            Text("No outputs yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            uiState.outputs.forEach { output ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOutputSelected(output) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(output.fileName, style = MaterialTheme.typography.titleSmall)
                        Text("${output.operation} • ${output.sizeInBytes} bytes", style = MaterialTheme.typography.bodySmall)
                        Text(output.path, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
