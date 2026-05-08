package com.nantcompany.clipy.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.export.output.OutputMedia
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun ResultScreen(
    output: OutputMedia?,
    onNavigate: (AppRoute) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Result", style = MaterialTheme.typography.headlineSmall)

        if (output == null) {
            Text("No recent output available.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text("File: ${output.fileName}", style = MaterialTheme.typography.bodyMedium)
            Text("Operation: ${output.operation}", style = MaterialTheme.typography.bodyMedium)
            Text("Size: ${output.sizeInBytes} bytes", style = MaterialTheme.typography.bodyMedium)
            Text("Path: ${output.path}", style = MaterialTheme.typography.bodySmall)
        }

        Button(onClick = { onNavigate(AppRoute.OUTPUT_HISTORY) }) {
            Text("View History")
        }

        Button(onClick = { onNavigate(AppRoute.HOME) }) {
            Text("Back Home")
        }
    }
}
