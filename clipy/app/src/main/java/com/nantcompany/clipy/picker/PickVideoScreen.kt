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
fun PickVideoScreen(
    selectedPath: String?,
    onVideoPicked: (String) -> Unit,
    onNavigate: (AppRoute) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                MediaFileUtils.importUriToLocalPath(
                    context = context,
                    uri = uri,
                    folderName = "imports/video",
                    defaultExtension = "mp4"
                )
            }.onSuccess(onVideoPicked)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pick Video", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = selectedPath ?: "No video selected",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = { launcher.launch("video/*") }) {
            Text("Select Video")
        }
        Button(
            enabled = !selectedPath.isNullOrBlank(),
            onClick = { onNavigate(AppRoute.CUT_VIDEO) }
        ) {
            Text("Continue To Cut")
        }
    }
}
