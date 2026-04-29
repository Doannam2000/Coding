package com.natncompany.clipy.editor

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.natncompany.media.Asset
import com.natncompany.media.AssetType
import com.natncompany.media.DefaultMediaModuleFactory
import com.natncompany.media.MediaError
import com.natncompany.media.MediaExportConfig
import com.natncompany.media.MediaImportInput
import com.natncompany.media.MediaResult
import com.natncompany.media.MediaSessionEvent
import com.natncompany.media.ClipTransform
import com.natncompany.media.ClipEffect
import com.natncompany.media.RenderConfig
import com.natncompany.media.Timeline
import com.natncompany.media.TimelineClip
import com.natncompany.media.TimelineTrack
import com.natncompany.media.TrackType
import com.natncompany.media.VideoProject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
    val cacheManager = factory.createCacheManager(context.applicationContext)
    var outputPath: String? = null
    var eventJob: Job? = null
    var projectId: String? = null

    try {
        Log.i(EXPORT_LOG_TAG, "export start projectName=$projectName clips=${clips.size}")
        eventJob = launch {
            sessionManager.events.collect { event ->
                when (event) {
                    is MediaSessionEvent.RenderStarted -> Log.i(
                        EXPORT_LOG_TAG,
                        "render event started job=${event.job.id} output=${event.request.outputFileName} durationMs=${event.request.timeline.durationMs}"
                    )
                    is MediaSessionEvent.RenderProgress -> {
                        Log.d(EXPORT_LOG_TAG, "render event progress job=${event.job.id} progress=${event.progressPercent}")
                        onProgress(ClipyExportProgress(event.progressPercent, "Rendering ${event.progressPercent}%"))
                    }
                    is MediaSessionEvent.RenderCompleted -> {
                        outputPath = event.outputPath
                        Log.i(EXPORT_LOG_TAG, "render event completed output=${event.outputPath}")
                        onProgress(ClipyExportProgress(100, "Export completed", event.outputPath))
                    }
                    is MediaSessionEvent.RenderFailed -> {
                        Log.e(EXPORT_LOG_TAG, "render event failed error=${event.error.message}")
                        onProgress(ClipyExportProgress(100, event.error.message))
                    }
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
        projectId = project.id
        Log.i(EXPORT_LOG_TAG, "open project id=${project.id}")
        when (val opened = sessionManager.openProject(project)) {
            is MediaResult.Failure -> {
                Log.e(EXPORT_LOG_TAG, "open project failed error=${opened.error.message}")
                return@coroutineScope MediaResult.Failure(opened.error)
            }
            is MediaResult.Success -> Log.i(EXPORT_LOG_TAG, "open project success root=${opened.value.rootCachePath}")
        }

        val importedAssets = mutableListOf<Asset>()
        clips.forEachIndexed { index, clip ->
            Log.i(EXPORT_LOG_TAG, "import start index=$index name=${clip.displayName} uri=${clip.uriString}")
            onProgress(ClipyExportProgress((index * 20 / clips.size.coerceAtLeast(1)).coerceIn(0, 20), "Importing ${clip.displayName}"))
            val input = MediaImportInput(uri = Uri.parse(clip.uriString))
            when (val imported = sessionManager.importMedia(input)) {
                is MediaResult.Success -> {
                    Log.i(
                        EXPORT_LOG_TAG,
                        "import success index=$index asset=${imported.value.id} cached=${imported.value.cachedPath} type=${imported.value.type} transcode=${imported.value.needsTranscode}"
                    )
                    importedAssets += imported.value
                }
                is MediaResult.Failure -> {
                    Log.e(EXPORT_LOG_TAG, "import failed index=$index error=${imported.error.message}")
                    return@coroutineScope MediaResult.Failure(imported.error)
                }
            }
        }

        val timeline = buildTimeline(importedAssets)
        Log.i(EXPORT_LOG_TAG, "timeline built tracks=${timeline.tracks.size} durationMs=${timeline.durationMs} clips=${timeline.tracks.sumOf { it.clips.size }}")
        when (val updated = sessionManager.updateTimeline(timeline)) {
            is MediaResult.Failure -> {
                Log.e(EXPORT_LOG_TAG, "timeline update failed error=${updated.error.message}")
                return@coroutineScope MediaResult.Failure(updated.error)
            }
            is MediaResult.Success -> Log.i(EXPORT_LOG_TAG, "timeline update success durationMs=${updated.value.durationMs}")
        }

        onProgress(ClipyExportProgress(25, "Starting render"))
        val outputName = projectName
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_-]+"), "_")
            .ifBlank { "clipy_export" } + ".mp4"

        val renderSize = renderSize()
        Log.i(EXPORT_LOG_TAG, "render call start outputName=$outputName size=${renderSize.width}x${renderSize.height} aspect=${aspectPreset.label} resolution=${exportResolutionPreset.label}")
        return@coroutineScope when (val exported = sessionManager.export(
            MediaExportConfig(
                outputFileName = outputName,
                renderConfig = RenderConfig(
                    outputFileName = outputName,
                    targetWidth = renderSize.width,
                    targetHeight = renderSize.height,
                    videoBitrate = exportVideoBitrate()
                )
            )
        )) {
            is MediaResult.Success -> {
                Log.i(EXPORT_LOG_TAG, "render call success eventOutput=$outputPath")
                val resolvedOutputPath = outputPath ?: when (val resolved = cacheManager.createRenderOutput(projectId.orEmpty(), outputName)) {
                    is MediaResult.Success -> resolved.value
                    is MediaResult.Failure -> null
                }
                if (resolvedOutputPath.isNullOrBlank() || !File(resolvedOutputPath).isFile) {
                    Log.e(EXPORT_LOG_TAG, "rendered file missing resolved=$resolvedOutputPath")
                    return@coroutineScope MediaResult.Failure(MediaError.FileAccess("Rendered file was not created"))
                }
                Log.i(EXPORT_LOG_TAG, "publish start rendered=$resolvedOutputPath size=${File(resolvedOutputPath).length()}")
                when (val published = publishExportToGallery(context.applicationContext, resolvedOutputPath, outputName)) {
                    is MediaResult.Success -> {
                        Log.i(EXPORT_LOG_TAG, "publish success uri=${published.value}")
                        onProgress(ClipyExportProgress(100, "Saved to Movies/Clipy", published.value))
                        MediaResult.Success(published.value)
                    }
                    is MediaResult.Failure -> {
                        Log.e(EXPORT_LOG_TAG, "publish failed error=${published.error.message}")
                        published
                    }
                }
            }
            is MediaResult.Failure -> {
                Log.e(EXPORT_LOG_TAG, "render call failed error=${exported.error.message}")
                MediaResult.Failure(exported.error)
            }
        }
    } catch (cancelled: CancellationException) {
        Log.w(EXPORT_LOG_TAG, "export cancelled")
        throw cancelled
    } catch (throwable: Throwable) {
        Log.e(EXPORT_LOG_TAG, "export crashed", throwable)
        MediaResult.Failure(MediaError.ExceptionError(throwable))
    } finally {
        Log.i(EXPORT_LOG_TAG, "export cleanup projectId=$projectId")
        eventJob?.cancel()
        sessionManager.closeProject()
    }
}

fun ClipyAppState.buildMediaTimeline(assets: List<Asset> = emptyList()): Timeline {
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
            transform = ClipTransform(
                brightness = clip.adjustments.brightness,
                contrast = 1f + clip.adjustments.contrast,
                saturation = 1f + clip.adjustments.saturation,
                blur = if (clip.adjustments.filterName == "Box Blur" || clip.adjustments.filterName == "Gaussian Blur") 0.25f else 0f
            ),
            effect = ClipEffect(
                parameters = mapOf("ffmpegFilter" to clip.adjustments.filterName.toFfmpegFilter())
            ),
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

private fun ClipyAppState.renderSize(): RenderSize {
    val longEdge = exportResolutionPreset.longEdge
    return when (aspectPreset) {
        AspectPreset.NineSixteen -> RenderSize((longEdge * 9 / 16).roundToEven(), longEdge)
        AspectPreset.OneOne -> RenderSize(longEdge, longEdge)
        AspectPreset.FourFive -> RenderSize((longEdge * 4 / 5).roundToEven(), longEdge)
        AspectPreset.SixteenNine -> RenderSize(longEdge, (longEdge * 9 / 16).roundToEven())
    }
}

private fun ClipyAppState.buildTimeline(assets: List<Asset>): Timeline = buildMediaTimeline(assets)

private fun ClipyAppState.exportVideoBitrate(): Int {
    return when (exportResolutionPreset) {
        ExportResolutionPreset.Hd -> 5_000_000
        ExportResolutionPreset.FullHd -> 10_000_000
        ExportResolutionPreset.QuadHd -> 18_000_000
    }
}

private fun String.toFfmpegFilter(): String {
    return when (this) {
        "Sepia" -> "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131"
        "Mono", "Luminance" -> "format=gray"
        "Invert" -> "negate"
        "Posterize" -> "elbg=codebook_length=32"
        "RGB Warm" -> "colorbalance=rs=.08:bs=-.08"
        "RGB Cool" -> "colorbalance=rs=-.08:bs=.08"
        "Hue Shift" -> "hue=h=90"
        "Gamma" -> "eq=gamma=1.35"
        "Exposure" -> "eq=brightness=.08"
        "Sharpen" -> "unsharp=5:5:0.8:3:3:0.4"
        "Sketch" -> "edgedetect=mode=colormix"
        "Sobel Edge", "Directional Edge" -> "sobel"
        "Emboss" -> "convolution=-2:-1:0:-1:1:1:0:1:2"
        "Pixel" -> "scale=iw/16:ih/16,scale=iw*16:ih*16:flags=neighbor"
        "Solarize" -> "curves=preset=negative"
        else -> ""
    }
}

private fun Int.roundToEven(): Int = if (this % 2 == 0) this else this + 1

private data class RenderSize(
    val width: Int,
    val height: Int
)

private suspend fun publishExportToGallery(
    context: Context,
    renderedPath: String,
    outputName: String
): MediaResult<String> = withContext(Dispatchers.IO) {
    val source = File(renderedPath)
    if (!source.isFile) {
        Log.e(EXPORT_LOG_TAG, "publish source missing path=$renderedPath")
        return@withContext MediaResult.Failure(MediaError.FileAccess("Rendered file does not exist: $renderedPath"))
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, outputName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Clipy")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext MediaResult.Failure(MediaError.FileAccess("Unable to create MediaStore export"))

        Log.i(EXPORT_LOG_TAG, "mediastore insert uri=$uri name=$outputName")
        runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Unable to open MediaStore output")
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            MediaResult.Success(uri.toString())
        }.getOrElse { throwable ->
            Log.e(EXPORT_LOG_TAG, "mediastore write failed uri=$uri", throwable)
            resolver.delete(uri, null, null)
            MediaResult.Failure(MediaError.FileAccess(throwable.message ?: "Unable to save export"))
        }
    } else {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: return@withContext MediaResult.Failure(MediaError.FileAccess("External movies directory is unavailable"))
        val clipyDir = File(moviesDir, "Clipy").apply { mkdirs() }
        val target = File(clipyDir, outputName)
        runCatching {
            source.copyTo(target, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("video/mp4"), null)
            Log.i(EXPORT_LOG_TAG, "legacy publish success target=${target.absolutePath}")
            MediaResult.Success(target.absolutePath)
        }.getOrElse { throwable ->
            Log.e(EXPORT_LOG_TAG, "legacy publish failed target=${target.absolutePath}", throwable)
            MediaResult.Failure(MediaError.FileAccess(throwable.message ?: "Unable to save export"))
        }
    }
}

private const val EXPORT_LOG_TAG = "ClipyExport"
