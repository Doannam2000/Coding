package com.natncompany.clipy.editor

import android.content.Context
import android.net.Uri
import com.natncompany.media.Asset
import com.natncompany.media.AssetType
import com.natncompany.media.DefaultMediaModuleFactory
import com.natncompany.media.MediaExportConfig
import com.natncompany.media.MediaImportInput
import com.natncompany.media.MediaResult
import com.natncompany.media.MediaSessionEvent
import com.natncompany.media.MediaSessionManager
import com.natncompany.media.Timeline
import com.natncompany.media.TimelineClip
import com.natncompany.media.TimelineTrack
import com.natncompany.media.TrackType
import com.natncompany.media.VideoProject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

data class ClipyExportProgress(
    val progressPercent: Int,
    val message: String,
    val outputPath: String? = null
)

suspend fun ClipyAppState.exportWithMediaPipeline(
    context: Context,
    onProgress: (ClipyExportProgress) -> Unit = {}
): MediaResult<String> = coroutineScope {
    val factory = DefaultMediaModuleFactory()
    val sessionManager = factory.createSessionManager(context.applicationContext)
    var outputPath: String? = null
    var eventJob: Job? = null

    try {
        eventJob = launch {
            sessionManager.events.collect { event ->
                when (event) {
                    is MediaSessionEvent.RenderProgress -> onProgress(
                        ClipyExportProgress(event.progressPercent, "Rendering ${event.progressPercent}%")
                    )
                    is MediaSessionEvent.RenderCompleted -> {
                        outputPath = event.outputPath
                        onProgress(ClipyExportProgress(100, "Export completed", event.outputPath))
                    }
                    is MediaSessionEvent.RenderFailed -> onProgress(
                        ClipyExportProgress(100, event.error.message)
                    )
                    else -> Unit
                }
            }
        }

        onProgress(ClipyExportProgress(0, "Preparing project"))
        val project = VideoProject(
            id = buildProjectId(),
            name = projectName,
            rootCachePath = ""
        )
        when (val opened = sessionManager.openProject(project)) {
            is MediaResult.Failure -> return@coroutineScope MediaResult.Failure(opened.error)
            is MediaResult.Success -> Unit
        }

        val importedAssets = mutableListOf<Asset>()
        clips.forEachIndexed { index, clip ->
            onProgress(ClipyExportProgress((index * 20 / clips.size.coerceAtLeast(1)).coerceIn(0, 20), "Importing ${clip.displayName}"))
            val input = MediaImportInput(uri = Uri.parse(clip.uriString))
            when (val imported = sessionManager.importMedia(input)) {
                is MediaResult.Success -> importedAssets += imported.value
                is MediaResult.Failure -> return@coroutineScope MediaResult.Failure(imported.error)
            }
        }

        val timeline = buildTimeline(importedAssets)
        when (val updated = sessionManager.updateTimeline(timeline)) {
            is MediaResult.Failure -> return@coroutineScope MediaResult.Failure(updated.error)
            is MediaResult.Success -> Unit
        }

        onProgress(ClipyExportProgress(25, "Starting render"))
        val outputName = projectName
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_-]+"), "_")
            .ifBlank { "clipy_export" } + ".mp4"

        return@coroutineScope when (val exported = sessionManager.export(MediaExportConfig(outputFileName = outputName))) {
            is MediaResult.Success -> MediaResult.Success(outputPath.orEmpty())
            is MediaResult.Failure -> MediaResult.Failure(exported.error)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } finally {
        eventJob?.cancel()
        sessionManager.closeProject()
    }
}

private fun ClipyAppState.buildTimeline(assets: List<Asset>): Timeline {
    var cursorMs = 0L
    val timelineClips = clips.mapIndexed { index, clip ->
        val asset = assets.getOrNull(index)
        val sourceStart = clip.adjustments.trimStartMs.coerceAtLeast(0L)
        val sourceEnd = clip.trimEndMs().coerceAtLeast(sourceStart + 250L)
        TimelineClip(
            id = clip.id,
            assetId = asset?.id ?: clip.id,
            assetType = when (clip.mediaKind) {
                MediaKind.Video -> AssetType.Video
                MediaKind.Image -> AssetType.Image
            },
            timelineStartMs = cursorMs,
            sourceStartMs = sourceStart,
            sourceEndMs = sourceEnd,
            sourceDurationMs = clip.sourceDurationMs,
            audio = com.natncompany.media.ClipAudio(volume = clip.adjustments.volume)
        ).also {
            cursorMs += it.visibleDurationMs
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
        )
    )
}

private fun ClipyAppState.buildProjectId(): String {
    return "clipy_${projectName}_${clips.size}"
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9_-]+"), "_")
        .ifBlank { "clipy_project" }
}
