package com.nantcompany.clipy.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = uiState.enableHardwareAcceleration,
                onCheckedChange = { viewModel.toggleHardwareAcceleration() }
            )
            Text("Enable hardware acceleration")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = uiState.keepOriginalFiles,
                onCheckedChange = { viewModel.toggleKeepOriginal() }
            )
            Text("Keep original media files")
        }
    }
}
