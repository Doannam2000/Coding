package com.nantcompany.clipy.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun HomeScreen(
    onNavigate: (AppRoute) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Clipy Studio", style = MaterialTheme.typography.headlineSmall)
        Text("Pick media first, then run editing tools.", style = MaterialTheme.typography.bodyMedium)

        Text("Import", style = MaterialTheme.typography.titleSmall)
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(AppRoute.PICK_VIDEO) }) { Text("Pick Video") }
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(AppRoute.PICK_MULTIPLE_VIDEOS) }) { Text("Pick Multiple Videos") }
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(AppRoute.PICK_IMAGES) }) { Text("Pick Images") }
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(AppRoute.PICK_AUDIO) }) { Text("Pick Audio") }

        Text("Tools", style = MaterialTheme.typography.titleSmall)
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(AppRoute.CUT_VIDEO) }) { Text("Cut") }
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(AppRoute.COMPRESS_VIDEO) }) { Text("Compress") }
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(AppRoute.MERGE_VIDEO) }) { Text("Merge") }
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(AppRoute.EXTRACT_AUDIO) }) { Text("Extract Audio") }
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(AppRoute.SLIDESHOW) }) { Text("Slideshow") }
    }
}
