package com.nantcompany.clipy.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Text("Dark theme is enabled by default.", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.enableHardwareAcceleration,
                        onCheckedChange = { viewModel.toggleHardwareAcceleration() }
                    )
                    Text("Enable hardware acceleration")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Export", style = MaterialTheme.typography.titleMedium)
                Text(
                    "History file: ${uiState.outputHistoryPath}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.keepOriginalFiles,
                        onCheckedChange = { viewModel.toggleKeepOriginal() }
                    )
                    Text("Keep original media files")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Storage", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { viewModel.clearTempFiles() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear temporary files")
                }
                OutlinedButton(onClick = { viewModel.askClearHistory() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear export history")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Text(uiState.appVersionLabel, style = MaterialTheme.typography.bodyMedium)
                Text("Privacy policy: Coming soon", style = MaterialTheme.typography.bodySmall)
                Text("Terms: Coming soon", style = MaterialTheme.typography.bodySmall)
            }
        }

        uiState.message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
            LaunchedEffect(it) { viewModel.consumeMessage() }
        }
    }

    if (uiState.confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelClearHistory() },
            title = { Text("Clear export history?") },
            text = { Text("This removes history records only and does not delete actual files.") },
            confirmButton = {
                Button(onClick = { viewModel.clearHistory() }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.cancelClearHistory() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
