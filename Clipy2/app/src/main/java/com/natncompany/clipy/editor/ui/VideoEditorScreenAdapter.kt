package com.natncompany.clipy.editor.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.natncompany.clipy.editor.ClipyAppState
import com.natncompany.clipy.editor.ExportResolutionPreset
import com.natncompany.clipy.editor.MediaKind
import com.natncompany.media.Asset
import com.natncompany.media.AssetType
import com.natncompany.media.ClipAudio
import com.natncompany.media.ClipEffect
import com.natncompany.media.ClipTransform
import com.natncompany.media.DefaultMediaModuleFactory
import com.natncompany.media.MediaImportInput
import com.natncompany.media.MediaResult
import com.natncompany.media.Timeline
import com.natncompany.media.TimelineClip
import com.natncompany.media.ClipMetadata
import com.natncompany.media.TimelineTrack
import com.natncompany.media.TrackType
import com.natncompany.media.VideoProject
import com.natncompany.videoeditor.EditorViewModel
import kotlinx.coroutines.flow.collectLatest

private data class ImportedClipAsset(
    val clipId: String,
    val asset: Asset
)

@Composable
fun VideoEditorScreenAdapter(
    appState: ClipyAppState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestAppState by rememberUpdatedState(appState)
    val factory = remember { DefaultMediaModuleFactory() }
    val sessionManager = remember { factory.createSessionManager(context.applicationContext) }
    val viewModel = remember {
        EditorViewModel(
            mediaSessionManager = sessionManager,
            timelineEditor = factory.createTimelineEditor()
        )
    }
    val initialClipSignature = remember {
        appState.clips.joinToString("|") { "${it.id}:${it.uriString}:${it.adjustments.trimStartMs}:${it.trimEndMs()}" }
    }
    var isInitializing by remember { mutableStateOf(true) }

    LaunchedEffect(initialClipSignature) {
        isInitializing = true
        val importedAssets = openAndImportProject(appState, sessionManager)
        val timeline = appState.toVideoEditorTimeline(importedAssets)
        when (sessionManager.updateTimeline(timeline)) {
            is MediaResult.Success -> Unit
            is MediaResult.Failure -> appState.updateStatus("Unable to load editor timeline")
        }
        isInitializing = false
    }

    LaunchedEffect(sessionManager) {
        sessionManager.state.collectLatest { state ->
            if (!isInitializing) {
                state.currentTimeline?.let { timeline ->
                    latestAppState.applyEditedProject(state.currentProject, timeline)
                }
            }
        }
    }

    com.natncompany.videoeditor.EditorScreen(
        viewModel = viewModel,
        onBack = onBack,
        onExportClick = {
            sessionManager.state.value.currentTimeline?.let { timeline ->
                appState.applyEditedProject(sessionManager.state.value.currentProject, timeline)
            }
            onNext()
        },
        exportButtonLabel = "Next",
        qualityLabel = appState.exportResolutionPreset.label,
        qualityOptions = ExportResolutionPreset.entries.map { it.label },
        onQualitySelected = { label ->
            ExportResolutionPreset.entries.firstOrNull { it.label == label }?.let(appState::updateExportResolution)
        },
        modifier = modifier
    )
}

private suspend fun openAndImportProject(
    appState: ClipyAppState,
    sessionManager: com.natncompany.media.MediaSessionManager
): List<ImportedClipAsset> {
    val project = VideoProject(
        id = "clipy_editor_${appState.projectName}_${appState.clips.size}".sanitizeProjectId(),
        name = appState.projectName,
        rootCachePath = ""
    )
    when (val opened = sessionManager.openProject(project)) {
        is MediaResult.Failure -> {
            appState.updateStatus(opened.error.message)
            return emptyList()
        }
        is MediaResult.Success -> Unit
    }

    val assets = mutableListOf<ImportedClipAsset>()
    appState.clips.forEach { clip ->
        when (val imported = sessionManager.importMedia(MediaImportInput(uri = Uri.parse(clip.uriString)))) {
            is MediaResult.Success -> assets += ImportedClipAsset(clipId = clip.id, asset = imported.value)
            is MediaResult.Failure -> appState.updateStatus(imported.error.message)
        }
    }
    return assets
}

private fun ClipyAppState.toVideoEditorTimeline(assets: List<ImportedClipAsset>): Timeline {
    var cursorMs = 0L
    val assetsByClipId = assets.associateBy { it.clipId }
    val timelineClips = clips.map { clip ->
        val sourceStart = clip.adjustments.trimStartMs.coerceAtLeast(0L)
        val sourceEnd = clip.trimEndMs().coerceAtLeast(sourceStart + 250L)
        TimelineClip(
            id = clip.id,
            assetId = assetsByClipId[clip.id]?.asset?.id ?: clip.id,
            assetType = when (clip.mediaKind) {
                MediaKind.Video -> AssetType.Video
                MediaKind.Image -> AssetType.Image
            },
            timelineStartMs = cursorMs,
            sourceStartMs = sourceStart,
            sourceEndMs = sourceEnd,
            sourceDurationMs = clip.sourceDurationMs,
            transform = ClipTransform(
                brightness = clip.adjustments.brightness,
                contrast = 1f + clip.adjustments.contrast,
                saturation = 1f + clip.adjustments.saturation,
                blur = if (clip.adjustments.filterName == "Box Blur" || clip.adjustments.filterName == "Gaussian Blur") 0.25f else 0f
            ),
            audio = ClipAudio(volume = clip.adjustments.volume),
            effect = ClipEffect(parameters = mapOf("filterName" to clip.adjustments.filterName)),
            metadata = ClipMetadata(label = clip.displayName)
        ).also {
            cursorMs += clip.visibleDurationMs()
        }
    }
    return Timeline(
        tracks = listOf(
            TimelineTrack(
                id = "video-main",
                type = TrackType.Video,
                clips = timelineClips,
                allowOverlap = false
            )
        ),
        selectedClipIds = selectedClipId?.let(::setOf).orEmpty()
    )
}

private fun String.sanitizeProjectId(): String {
    return lowercase()
        .replace(Regex("[^a-z0-9_-]+"), "_")
        .ifBlank { "clipy_editor" }
}
