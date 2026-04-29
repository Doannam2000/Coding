package com.natncompany.clipy

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.natncompany.clipy.editor.HomeFeature
import com.natncompany.clipy.editor.editorFeatures
import com.natncompany.clipy.editor.rememberClipyAppState
import com.natncompany.clipy.editor.EditorScreen as AppScreen
import com.natncompany.clipy.editor.ui.EditorScreen as EditorScreenView
import com.natncompany.clipy.editor.ui.ExportScreen
import com.natncompany.clipy.editor.ui.HomeScreen
import com.natncompany.clipy.ui.theme.ClipyTheme

@Composable
fun ClipyApp() {
    val context = LocalContext.current
    val appState = rememberClipyAppState()
    var pendingFeature by remember { mutableStateOf(editorFeatures.first()) }
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris ->
        appState.importMedia(
            context = context,
            uris = uris,
            replaceTimeline = pendingFeature.replaceTimeline,
            initialTool = pendingFeature.defaultTool,
            aspectPreset = pendingFeature.defaultAspect
        )
    }

    ClipyTheme(darkTheme = true, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (appState.currentScreen) {
                AppScreen.Home -> HomeScreen(
                    features = editorFeatures,
                    projectName = appState.projectName,
                    clipCount = appState.clips.size,
                    durationLabel = appState.projectDurationLabel,
                    hasProject = appState.clips.isNotEmpty(),
                    onContinueProject = { appState.openEditor() },
                    onFeatureClick = { feature ->
                        pendingFeature = feature
                        launchPickerForFeature(
                            appState = appState,
                            feature = feature,
                            launchPicker = {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        mediaType = ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                    )
                                )
                            }
                        )
                    }
                )

                AppScreen.Editor -> EditorScreenView(
                    appState = appState,
                    onBack = { appState.goHome() },
                    onNext = {
                        appState.openExport()
                    },
                    onImportMore = {
                        pendingFeature = HomeFeature(
                            title = "Add",
                            shortLabel = "+",
                            defaultTool = appState.activeTool,
                            defaultAspect = appState.aspectPreset,
                            replaceTimeline = false
                        )
                        pickerLauncher.launch(
                            PickVisualMediaRequest(
                                mediaType = ActivityResultContracts.PickVisualMedia.ImageAndVideo
                            )
                        )
                    }
                )

                AppScreen.Export -> ExportScreen(
                    appState = appState,
                    onBackHome = { appState.goHome() }
                )
            }
        }
    }
}

private fun launchPickerForFeature(
    appState: com.natncompany.clipy.editor.ClipyAppState,
    feature: HomeFeature,
    launchPicker: () -> Unit
) {
    if (appState.clips.isNotEmpty() && !feature.replaceTimeline) {
        appState.openEditor(
            tool = feature.defaultTool,
            aspectPreset = feature.defaultAspect
        )
        return
    }
    launchPicker()
}
