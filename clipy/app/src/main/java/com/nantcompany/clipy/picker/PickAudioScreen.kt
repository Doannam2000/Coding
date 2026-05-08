package com.nantcompany.clipy.picker

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
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun PickAudioScreen(onNavigate: (AppRoute) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pick Audio", style = MaterialTheme.typography.headlineSmall)
        Text("Audio import is available in the next audio editor iteration.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { onNavigate(AppRoute.COMING_SOON_AUDIO_EDITOR) }) {
            Text("Open Audio Editor Placeholder")
        }
        Button(onClick = { onNavigate(AppRoute.HOME) }) {
            Text("Back Home")
        }
    }
}
