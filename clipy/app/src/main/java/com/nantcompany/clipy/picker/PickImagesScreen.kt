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
fun PickImagesScreen(
    selectedPaths: List<String>,
    onImagesPicked: (List<String>) -> Unit,
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
                        folderName = "imports/images",
                        defaultExtension = "jpg"
                    )
                }.getOrNull()
            }
            if (localPaths.isNotEmpty()) {
                onImagesPicked(localPaths)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pick Images", style = MaterialTheme.typography.headlineSmall)
        Text("Selected: ${selectedPaths.size}", style = MaterialTheme.typography.bodyMedium)
        selectedPaths.take(3).forEach { path ->
            Text(path, style = MaterialTheme.typography.bodyMedium)
        }
        Button(onClick = { launcher.launch("image/*") }) {
            Text("Select Images")
        }
        Button(
            enabled = selectedPaths.isNotEmpty(),
            onClick = { onNavigate(AppRoute.SLIDESHOW) }
        ) {
            Text("Continue To Slideshow")
        }
    }
}
