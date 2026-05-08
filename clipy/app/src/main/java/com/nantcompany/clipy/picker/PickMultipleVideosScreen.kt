package com.nantcompany.clipy.picker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun PickMultipleVideosScreen(
    selectedPaths: List<String>,
    onVideosPicked: (List<String>) -> Unit,
    onNavigate: (AppRoute) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val localPaths = uris.mapNotNull { uri ->
                runCatching {
                    MediaFileUtils.importUriToLocalPath(
                        context = context,
                        uri = uri,
                        folderName = "imports/videos",
                        defaultExtension = "mp4"
                    )
                }.getOrNull()
            }
            if (localPaths.isNotEmpty()) {
                onVideosPicked(localPaths)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pick Multiple Videos", style = MaterialTheme.typography.headlineSmall)
        Text("Selected: ${selectedPaths.size}", style = MaterialTheme.typography.bodyMedium)
        selectedPaths.take(3).forEach { path ->
            Text(path, style = MaterialTheme.typography.bodyMedium)
        }
        Button(onClick = { launcher.launch("video/*") }) {
            Text("Select Videos")
        }
        Button(
            enabled = selectedPaths.size >= 2,
            onClick = { onNavigate(AppRoute.MERGE_VIDEO) }
        ) {
            Text("Continue To Merge")
        }
    }
}
