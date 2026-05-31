package com.nantcompany.clipy.export.job

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import com.nantcompany.clipy.export.output.LocalOutputRepository
import com.nantcompany.clipy.export.output.OutputMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.Locale

@UnstableApi
class ProcessingJobManager(
    private val context: Context? = null,
    private val outputRepository: LocalOutputRepository = LocalOutputRepository()
) {
    interface ProgressCallback {
        fun onProgress(event: ProcessEvent.ProgressUpdate)
    }

    @Volatile
    private var cancelled: Boolean = false
    private var activeTransformer: Transformer? = null

    suspend fun process(request: ProcessingRequest, progressCallback: ProgressCallback? = null): ProcessEvent = withContext(Dispatchers.IO) {
        cancelled = false
        progressCallback?.onProgress(ProcessEvent.ProgressUpdate(2, 0L, "Initializing Studio Engine"))

        val outputFile = File(request.outputPath)
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) outputFile.delete()

        val totalDurationMs = resolveDurationMs(request)
        val plan = try { buildExecutionPlan(request) } catch (error: Throwable) { return@withContext ProcessEvent.Failed(asFriendlyError(error)) }
        executeFFmpegPlan(request, plan, outputFile, totalDurationMs, progressCallback)
    }

    private suspend fun executeWithTransformer(request: ProcessingRequest, outputFile: File, progressCallback: ProgressCallback?): ProcessEvent = callbackFlow {
        val appContext = context ?: error("Context is required for hardware export.")
        val transformer = Transformer.Builder(appContext).build()
        activeTransformer = transformer
        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                val output = OutputMedia(
                    id = UUID.randomUUID().toString(),
                    fileName = outputFile.name,
                    path = outputFile.absolutePath,
                    sizeInBytes = outputFile.length(),
                    operation = "hardware-export"
                )
                outputRepository.save(output)
                trySend(ProcessEvent.Completed(output))
                close()
            }
            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                trySend(ProcessEvent.Failed(exportException))
                close()
            }
        }
        transformer.addListener(listener)
        
        val mediaItem: MediaItem? = when (request) {
            is ProcessingRequest.Cut -> {
                val r = (request as ProcessingRequest.Cut).request
                MediaItem.Builder()
                    .setUri(Uri.fromFile(File(r.inputPath)))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(r.startMs)
                            .setEndPositionMs(r.endMs)
                            .build()
                    )
                    .build()
            }
            is ProcessingRequest.Compress -> {
                val r = (request as ProcessingRequest.Compress).request
                MediaItem.fromUri(Uri.fromFile(File(r.inputPath)))
            }
            is ProcessingRequest.Rotate -> {
                val r = (request as ProcessingRequest.Rotate).request
                MediaItem.fromUri(Uri.fromFile(File(r.inputPath)))
            }
            else -> null
        }
        
        if (mediaItem == null) { close(); return@callbackFlow }

        transformer.start(mediaItem, outputFile.absolutePath)
        while (isActive) {
            if (cancelled) { transformer.cancel(); trySend(ProcessEvent.Cancelled); close(); break }
            val ph = androidx.media3.transformer.ProgressHolder()
            if (transformer.getProgress(ph) == Transformer.PROGRESS_STATE_AVAILABLE) {
                progressCallback?.onProgress(ProcessEvent.ProgressUpdate(ph.progress, 0L, "Accelerating"))
            }
            kotlinx.coroutines.delay(200)
        }
        awaitClose { transformer.removeListener(listener); activeTransformer = null }
    }.first()

    private fun executeFFmpegPlan(request: ProcessingRequest, plan: ExecutionPlan, outputFile: File, totalDurationMs: Long, progressCallback: ProgressCallback?): ProcessEvent {
        return try {
            val done = CountDownLatch(1); val resultRef = AtomicReference<ProcessEvent>()
            FFmpegKit.executeWithArgumentsAsync(plan.arguments.toTypedArray(), { session ->
                val result = when {
                    cancelled || ReturnCode.isCancel(session.returnCode) -> ProcessEvent.Cancelled
                    ReturnCode.isSuccess(session.returnCode) -> {
                        if (!outputFile.exists() || outputFile.length() <= 0L) ProcessEvent.Failed(IllegalStateException("Empty output created."))
                        else {
                            val output = OutputMedia(
                                id = UUID.randomUUID().toString(),
                                fileName = outputFile.name,
                                path = outputFile.absolutePath,
                                sizeInBytes = outputFile.length(),
                                operation = plan.operation
                            )
                            outputRepository.save(output)
                            ProcessEvent.Completed(output)
                        }
                    }
                    else -> {
                        val logs = session.allLogsAsString
                        val lastLogs = logs.split("\n").takeLast(10).joinToString("\n")
                        ProcessEvent.Failed(asFriendlyError(IllegalStateException("FFmpeg error: $lastLogs")))
                    }
                }
                resultRef.set(result); done.countDown()
            }, null, { stats ->
                val tMs: Long = stats.time.toLong()
                val percent: Int = if (totalDurationMs > 0L) ((tMs.toDouble() / totalDurationMs.toDouble()) * 100.0).toInt().coerceIn(2, 99) else 25
                progressCallback?.onProgress(ProcessEvent.ProgressUpdate(percent, tMs, "Processing"))
            })
            while (!done.await(200, TimeUnit.MILLISECONDS)) { if (cancelled) FFmpegKit.cancel() }
            resultRef.get() ?: ProcessEvent.Failed(IllegalStateException("No response"))
        } catch (error: Throwable) { ProcessEvent.Failed(asFriendlyError(error)) }
        finally { plan.tempFiles.forEach { runCatching { it.delete() } } }
    }

    fun cancelProcessing() {
        cancelled = true; activeTransformer?.cancel(); FFmpegKit.cancel()
    }

    internal fun buildExecutionPlan(request: ProcessingRequest): ExecutionPlan {
        val commonVf = "scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p"

        return when (request) {
            is ProcessingRequest.Cut -> {
                val r = (request as ProcessingRequest.Cut).request
                val durationMs = (r.endMs - r.startMs).coerceAtLeast(1L)
                ExecutionPlan(listOf("-y", "-ss", formatSeconds(r.startMs), "-i", r.inputPath, "-t", formatSeconds(durationMs), "-vf", commonVf, "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac", request.outputPath), "cut")
            }
            is ProcessingRequest.Compress -> {
                val r = (request as ProcessingRequest.Compress).request
                val vf = if (r.targetHeight != null) "scale=-2:${r.targetHeight},format=yuv420p" else commonVf
                val args = mutableListOf("-y", "-i", r.inputPath, "-b:v", "${r.bitrateKbps}k", "-vf", vf, "-c:v", "libx264", "-preset", "fast")
                if (r.keepAudio) args.addAll(listOf("-c:a", "aac")) else args.add("-an")
                args.add(request.outputPath); ExecutionPlan(args, "compress")
            }
            is ProcessingRequest.Merge -> {
                val r = (request as ProcessingRequest.Merge).request
                val requestedTransition = normalizeTransitionName(r.transition)
                val inputDurationsMs = r.inputPaths.map { probeDurationMs(it).coerceAtLeast(1L) }
                val hasAudioTracks = r.inputPaths.map { probeHasAudioTrack(it) }
                if (requestedTransition == "none") {
                    val args = mutableListOf("-y")
                    r.inputPaths.forEach { args.addAll(listOf("-i", it)) }

                    val filter = StringBuilder()
                    r.inputPaths.forEachIndexed { i, _ ->
                        filter.append("[$i:v]${fitToPortraitCanvas()}[v$i];")
                        filter.append(mergeAudioSource(i, hasAudioTracks[i], inputDurationsMs[i]))
                    }
                    r.inputPaths.indices.forEach { i -> filter.append("[v$i][a$i]") }
                    filter.append("concat=n=${r.inputPaths.size}:v=1:a=1[outv][outa]")

                    args.addAll(listOf("-filter_complex", filter.toString(), "-map", "[outv]", "-map", "[outa]", "-c:v", "libx264", "-preset", "ultrafast", "-pix_fmt", "yuv420p", "-c:a", "aac", "-movflags", "+faststart", request.outputPath))
                    ExecutionPlan(args, "merge")
                } else {
                    val args = mutableListOf("-y")
                    r.inputPaths.forEach { args.addAll(listOf("-i", it)) }
                    
                    val filter = StringBuilder()
                    r.inputPaths.forEachIndexed { i, _ ->
                        filter.append("[$i:v]${fitToPortraitCanvas()}[v$i];")
                        filter.append(mergeAudioSource(i, hasAudioTracks[i], inputDurationsMs[i]))
                    }
                    
                    val transDur = r.transitionDurationMs / 1000.0
                    val durations = inputDurationsMs.map { it / 1000.0 }
                    var currentOffset = 0.0
                    var lastV = "v0"
                    var lastA = "a0"
                    
                    r.inputPaths.indices.drop(1).forEach { i ->
                        val prevDur = durations[i-1]
                        currentOffset += (prevDur - transDur)
                        
                        val nextV = "v$i"
                        val nextA = "a$i"
                        val outV = "m0v$i"
                        val outA = "m0a$i"
                        
                        filter.append("[$lastV][$nextV]xfade=transition=$requestedTransition:duration=$transDur:offset=$currentOffset")
                        if (i < r.inputPaths.size - 1) {
                            filter.append("[$outV];")
                            lastV = outV
                        } else {
                            filter.append("[outv];")
                        }
                        
                        filter.append("[$lastA][$nextA]acrossfade=d=$transDur")
                        if (i < r.inputPaths.size - 1) {
                            filter.append("[$outA];")
                            lastA = outA
                        } else {
                            filter.append("[outa]")
                        }
                    }
                    
                    args.addAll(listOf("-filter_complex", filter.toString(), "-map", "[outv]", "-map", "[outa]", "-c:v", "libx264", "-preset", "ultrafast", "-pix_fmt", "yuv420p", "-c:a", "aac", request.outputPath))
                    ExecutionPlan(args, "merge-transition")
                }
            }
            is ProcessingRequest.ExtractAudio -> {
                val r = (request as ProcessingRequest.ExtractAudio).request
                val format = r.format.lowercase(Locale.US)
                val codec = if (format == "m4a" || format == "aac") "aac" else "libmp3lame"
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-vn", "-c:a", codec, "-b:a", "${r.bitrateKbps}k", request.outputPath), "extract-audio")
            }
            is ProcessingRequest.Slideshow -> {
                val r = (request as ProcessingRequest.Slideshow).request
                val requestedTransition = normalizeTransitionName(r.transition)
                if (requestedTransition == "none") {
                    val args = mutableListOf("-y")
                    r.imagePaths.forEach { args.addAll(listOf("-loop", "1", "-t", r.secondsPerImage.toString(), "-i", it)) }
                    r.audioPath?.let { args.addAll(listOf("-i", it)) }

                    val filter = StringBuilder()
                    r.imagePaths.forEachIndexed { i, _ ->
                        filter.append("[$i:v]${fitToPortraitCanvas()},fps=30[v$i];")
                    }
                    r.imagePaths.indices.forEach { i -> filter.append("[v$i]") }
                    filter.append("concat=n=${r.imagePaths.size}:v=1:a=0[outv]")

                    args.addAll(listOf("-filter_complex", filter.toString(), "-map", "[outv]"))
                    if (r.audioPath != null) {
                        args.addAll(listOf("-map", "${r.imagePaths.size}:a", "-c:a", "aac", "-shortest"))
                    }
                    args.addAll(listOf("-pix_fmt", "yuv420p", "-c:v", "libx264", "-preset", "ultrafast", "-movflags", "+faststart", request.outputPath))
                    ExecutionPlan(args, "slideshow")
                } else {
                    val args = mutableListOf("-y")
                    r.imagePaths.forEach { args.addAll(listOf("-loop", "1", "-t", "${r.secondsPerImage}", "-i", it)) }
                    r.audioPath?.let { args.addAll(listOf("-i", it)) }
                    
                    val filter = StringBuilder()
                    r.imagePaths.forEachIndexed { i, _ ->
                        filter.append("[$i:v]${fitToPortraitCanvas()},fps=30[v$i];")
                    }
                    
                    var lastLabel = "v0"
                    val transDur = r.transitionDurationMs / 1000.0
                    r.imagePaths.drop(1).forEachIndexed { i, _ ->
                        val nextIdx = i + 1
                        val offset = (nextIdx * r.secondsPerImage) - (nextIdx * transDur)
                        val outLabel = "v0$nextIdx"
                        filter.append("[$lastLabel][v$nextIdx]xfade=transition=$requestedTransition:duration=$transDur:offset=$offset")
                        if (nextIdx < r.imagePaths.size - 1) {
                            filter.append("[$outLabel];")
                            lastLabel = outLabel
                        } else {
                            filter.append("[outv]")
                        }
                    }
                    
                    args.addAll(listOf("-filter_complex", filter.toString(), "-map", "[outv]"))
                    if (r.audioPath != null) {
                        args.addAll(listOf("-map", "${r.imagePaths.size}:a", "-c:a", "aac", "-shortest"))
                    }
                    args.addAll(listOf("-pix_fmt", "yuv420p", "-c:v", "libx264", "-preset", "ultrafast", "-movflags", "+faststart", request.outputPath))
                    ExecutionPlan(args, "slideshow-transition")
                }
            }
            is ProcessingRequest.Filters -> {
                val r = (request as ProcessingRequest.Filters).request
                val vf = buildFilterVideoChain(
                    filterName = r.filterName,
                    brightness = r.brightness,
                    contrast = r.contrast,
                    saturation = r.saturation,
                    finalScale = commonVf,
                    filterIntensity = r.filterIntensity
                )
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-vf", vf, "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac", "-movflags", "+faststart", request.outputPath), "filters")
            }
            is ProcessingRequest.Rotate -> {
                val r = (request as ProcessingRequest.Rotate).request
                val vf = mutableListOf(commonVf)
                when (r.rotation) { 90 -> vf.add("transpose=1"); 180 -> vf.add("transpose=2,transpose=2"); 270 -> vf.add("transpose=2") }
                if (r.flipHorizontal) vf.add("hflip")
                if (r.flipVertical) vf.add("vflip")
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-vf", vf.joinToString(","), "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac", request.outputPath), "rotate")
            }
            is ProcessingRequest.Speed -> {
                val r = (request as ProcessingRequest.Speed).request
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-vf", "setpts=${1f/r.speedFactor}*PTS,$commonVf", "-af", buildAtempoFilter(r.speedFactor), "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac", request.outputPath), "speed")
            }
            is ProcessingRequest.Crop -> {
                val r = (request as ProcessingRequest.Crop).request
                val vf = "crop=${r.width}:${r.height}:${r.x}:${r.y},$commonVf"
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-vf", vf, "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac", request.outputPath), "crop")
            }
            is ProcessingRequest.Reverse -> {
                val r = (request as ProcessingRequest.Reverse).request
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-vf", "reverse,$commonVf", "-af", "areverse", "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac", request.outputPath), "reverse")
            }
            is ProcessingRequest.Stickers -> {
                val r = (request as ProcessingRequest.Stickers).request
                val stickerWidth = r.width
                val stickerInput = if (stickerWidth != null && stickerWidth > 0) {
                    "[1:v]scale=$stickerWidth:-1[sticker];[0:v][sticker]"
                } else {
                    "[0:v][1:v]"
                }
                val timing = if (r.endTimeMs > r.startTimeMs) {
                    ":enable='between(t\\,${formatSeconds(r.startTimeMs)}\\,${formatSeconds(r.endTimeMs)})'"
                } else {
                    ""
                }
                val filter = "${stickerInput}overlay=(W*${r.x}/100):(H*${r.y}/100)$timing,$commonVf[v]"
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-i", r.stickerPath, "-filter_complex", filter, "-map", "[v]", "-map", "0:a?", "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac", "-movflags", "+faststart", request.outputPath), "stickers")
            }
            is ProcessingRequest.TextOverlay -> {
                val r = (request as ProcessingRequest.TextOverlay).request
                val color = r.fontColor.lowercase()
                val fontPath = "/system/fonts/Roboto-Regular.ttf"
                val vf = "drawtext=text='${escapeDrawText(r.text)}':x=(w*${r.x}/100):y=(h*${r.y}/100):fontsize=${r.fontSize}:fontcolor=$color:fontfile=$fontPath,$commonVf"
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-vf", vf, "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac", request.outputPath), "text")
            }
            is ProcessingRequest.Studio -> {
                val r = (request as ProcessingRequest.Studio).request
                val vf = mutableListOf<String>()
                when (r.rotation) { 90 -> vf.add("transpose=1"); 180 -> vf.add("transpose=2,transpose=2"); 270 -> vf.add("transpose=2") }
                if (r.flipHorizontal) vf.add("hflip")
                vf.add(buildFilterVideoChain(r.filterName, r.brightness, r.contrast, r.saturation, finalScale = null))
                if (r.speedFactor != 1.0f) vf.add("setpts=${1f/r.speedFactor}*PTS")
                r.textLayers.forEach { layer ->
                    val colorHex = String.format("0x%06X", 0xFFFFFF and layer.color)
                    val fontPath = "/system/fonts/Roboto-Regular.ttf"
                    vf.add("drawtext=text='${escapeDrawText(layer.text)}':x=(w*${layer.x}/100):y=(h*${layer.y}/100):fontsize=28:fontcolor=$colorHex:fontfile=$fontPath")
                }
                vf.add(commonVf)
                val args = mutableListOf("-y", "-ss", formatSeconds(r.startMs), "-i", r.inputPath)
                if (r.audioTracks.isNotEmpty()) {
                    r.audioTracks.forEach { args.addAll(listOf("-i", it.path)) }
                    val amix = StringBuilder("[0:a]volume=${r.mainVideoVolume}[main];")
                    r.audioTracks.forEachIndexed { i, t -> amix.append("[${i+1}:a]volume=${t.volume}[a$i];") }
                    amix.append("[main]")
                    r.audioTracks.forEachIndexed { i, _ -> amix.append("[a$i]") }
                    amix.append("amix=inputs=${r.audioTracks.size + 1}:duration=shortest[outa]")
                    args.addAll(listOf("-filter_complex", amix.toString(), "-map", "0:v", "-map", "[outa]"))
                }
                if (vf.isNotEmpty()) args.addAll(listOf("-vf", vf.joinToString(",")))
                if (r.speedFactor != 1.0f) args.addAll(listOf("-af", buildAtempoFilter(r.speedFactor)))
                args.addAll(listOf("-t", formatSeconds(r.endMs - r.startMs), "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac", request.outputPath))
                ExecutionPlan(args, "studio-composite")
            }
        }
    }

    private fun resolveDurationMs(request: ProcessingRequest): Long {
        return when (request) {
            is ProcessingRequest.Cut -> {
                val r = (request as ProcessingRequest.Cut).request
                (r.endMs - r.startMs).coerceAtLeast(0L)
            }
            is ProcessingRequest.Slideshow -> {
                val r = (request as ProcessingRequest.Slideshow).request
                val base = (r.imagePaths.size.toLong() * r.secondsPerImage.toLong() * 1000L)
                if (r.transition == "none") base 
                else base - ((r.imagePaths.size - 1) * r.transitionDurationMs)
            }
            is ProcessingRequest.Studio -> {
                val r = (request as ProcessingRequest.Studio).request
                (r.endMs - r.startMs).coerceAtLeast(0L)
            }
            is ProcessingRequest.Compress -> probeDurationMs((request as ProcessingRequest.Compress).request.inputPath)
            is ProcessingRequest.Rotate -> probeDurationMs((request as ProcessingRequest.Rotate).request.inputPath)
            is ProcessingRequest.Speed -> {
                val r = (request as ProcessingRequest.Speed).request
                (probeDurationMs(r.inputPath) / r.speedFactor).toLong()
            }
            is ProcessingRequest.Crop -> probeDurationMs((request as ProcessingRequest.Crop).request.inputPath)
            is ProcessingRequest.Filters -> probeDurationMs((request as ProcessingRequest.Filters).request.inputPath)
            is ProcessingRequest.Reverse -> probeDurationMs((request as ProcessingRequest.Reverse).request.inputPath)
            is ProcessingRequest.Merge -> {
                val r = (request as ProcessingRequest.Merge).request
                val total = r.inputPaths.sumOf { probeDurationMs(it) }
                if (r.transition == "none") total
                else total - ((r.inputPaths.size - 1) * r.transitionDurationMs)
            }
            is ProcessingRequest.ExtractAudio -> probeDurationMs((request as ProcessingRequest.ExtractAudio).request.inputPath)
            is ProcessingRequest.Stickers -> probeDurationMs((request as ProcessingRequest.Stickers).request.inputPath)
            is ProcessingRequest.TextOverlay -> probeDurationMs((request as ProcessingRequest.TextOverlay).request.inputPath)
        }
    }

    private fun probeDurationMs(path: String): Long {
        return runCatching {
            val session = FFprobeKit.getMediaInformation(path)
            (session.mediaInformation?.duration?.toDouble()?.times(1000))?.toLong()
        }.getOrNull() ?: 0L
    }

    private fun probeHasAudioTrack(path: String): Boolean {
        return runCatching {
            val mediaInformation = FFprobeKit.getMediaInformation(path).mediaInformation ?: return@runCatching true
            val streams = mediaInformation.streams ?: return@runCatching true
            streams.any { stream -> stream.type.equals("audio", ignoreCase = true) }
        }.getOrDefault(true)
    }

    private fun formatSeconds(ms: Long) = String.format(Locale.US, "%.3f", ms / 1000.0)

    private fun fitToPortraitCanvas(): String {
        return "scale=720:1280:force_original_aspect_ratio=decrease," +
            "pad=720:1280:(ow-iw)/2:(oh-ih)/2," +
            "setsar=1,format=yuv420p"
    }

    private fun standardizeAudio(): String {
        return "aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo"
    }

    private fun mergeAudioSource(index: Int, hasAudioTrack: Boolean, durationMs: Long): String {
        return if (hasAudioTrack) {
            "[$index:a]${standardizeAudio()}[a$index];"
        } else {
            "anullsrc=channel_layout=stereo:sample_rate=44100:d=${formatSeconds(durationMs)}[a$index];"
        }
    }

    private fun normalizeTransitionName(name: String): String {
        return when (name.lowercase(Locale.US)) {
            "xfade", "crossfade" -> "fade"
            else -> name.lowercase(Locale.US)
        }
    }

    private fun buildAtempoFilter(speedFactor: Float): String {
        var remaining = speedFactor.coerceIn(0.25f, 4.0f)
        val parts = mutableListOf<Float>()
        while (remaining < 0.5f) {
            parts.add(0.5f)
            remaining /= 0.5f
        }
        while (remaining > 2.0f) {
            parts.add(2.0f)
            remaining /= 2.0f
        }
        parts.add(remaining)
        return parts.joinToString(",") { part -> String.format(Locale.US, "atempo=%.3f", part) }
    }

    private fun buildFilterVideoChain(
        filterName: String,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        finalScale: String?,
        filterIntensity: Float = 1f
    ): String {
        val filters = mutableListOf<String>()
        val intensity = filterIntensity.coerceIn(0f, 1f)
        if (intensity > 0.001f) {
            if (intensity >= 0.999f) {
                when (filterName.uppercase(Locale.US)) {
                    "SEPIA" -> filters.addAll(listOf(
                        "hue=h=28:s=0.45",
                        "eq=contrast=1.2800:brightness=0.0600"
                    ))
                    "GRAYSCALE" -> filters.addAll(listOf("hue=s=0", "eq=contrast=1.2200"))
                    "INVERT" -> filters.add("negate")
                    "WARM" -> filters.addAll(listOf("hue=h=22:s=1.28", "eq=brightness=0.0600"))
                    "COOL" -> filters.addAll(listOf("hue=h=-28:s=1.30", "eq=contrast=1.1200"))
                    "VINTAGE" -> filters.addAll(listOf("hue=h=22:s=0.38", "eq=contrast=0.7200:brightness=0.0600"))
                    "DRAMATIC" -> filters.addAll(listOf("hue=s=0.65", "eq=contrast=1.7200:brightness=-0.0800"))
                    "TOON" -> filters.add("eq=contrast=1.7500:saturation=1.6500")
                    "SKETCH" -> filters.addAll(listOf("hue=s=0", "eq=contrast=1.8500:brightness=0.0800"))
                    "VIGNETTE" -> filters.add("eq=brightness=-0.1800:contrast=1.3000")
                    "KUWAHARA" -> filters.add("eq=contrast=0.7000:saturation=1.4500")
                    "PIXEL" -> filters.add("eq=contrast=1.5500:saturation=1.3500")
                    "POSTER" -> filters.add("eq=contrast=1.7500:saturation=1.5500")
                    "LOMO" -> filters.addAll(listOf("hue=h=12:s=1.42", "eq=contrast=1.5500"))
                    "CYBERPUNK" -> filters.addAll(listOf("hue=h=-150:s=1.85", "eq=contrast=1.5500:brightness=0.0400"))
                    "NONE", "NORMAL" -> Unit
                }
            } else {
                when (filterName.uppercase(Locale.US)) {
                    "SEPIA" -> filters.addAll(listOf(
                        hueFilter(hue = 28f * intensity, saturation = lerp(1f, 0.45f, intensity)),
                        eqFilter(contrast = lerp(1f, 1.28f, intensity), brightness = 0.06f * intensity)
                    ))
                    "GRAYSCALE" -> filters.addAll(listOf(
                        hueFilter(saturation = 1f - intensity),
                        eqFilter(contrast = lerp(1f, 1.22f, intensity))
                    ))
                    "INVERT" -> filters.add(invertFilter(intensity))
                    "WARM" -> filters.addAll(listOf(
                        hueFilter(hue = 22f * intensity, saturation = lerp(1f, 1.28f, intensity)),
                        eqFilter(brightness = 0.06f * intensity)
                    ))
                    "COOL" -> filters.addAll(listOf(
                        hueFilter(hue = -28f * intensity, saturation = lerp(1f, 1.30f, intensity)),
                        eqFilter(contrast = lerp(1f, 1.12f, intensity))
                    ))
                    "VINTAGE" -> filters.addAll(listOf(
                        hueFilter(hue = 22f * intensity, saturation = lerp(1f, 0.38f, intensity)),
                        eqFilter(contrast = lerp(1f, 0.72f, intensity), brightness = 0.06f * intensity)
                    ))
                    "DRAMATIC" -> filters.addAll(listOf(
                        hueFilter(saturation = lerp(1f, 0.65f, intensity)),
                        eqFilter(contrast = lerp(1f, 1.72f, intensity), brightness = -0.08f * intensity)
                    ))
                    "TOON" -> filters.add(eqFilter(contrast = lerp(1f, 1.75f, intensity), saturation = lerp(1f, 1.65f, intensity)))
                    "SKETCH" -> filters.addAll(listOf(
                        hueFilter(saturation = 1f - intensity),
                        eqFilter(contrast = lerp(1f, 1.85f, intensity), brightness = 0.08f * intensity)
                    ))
                    "VIGNETTE" -> filters.add(eqFilter(brightness = -0.18f * intensity, contrast = lerp(1f, 1.30f, intensity)))
                    "KUWAHARA" -> filters.add(eqFilter(contrast = lerp(1f, 0.70f, intensity), saturation = lerp(1f, 1.45f, intensity)))
                    "PIXEL" -> filters.add(eqFilter(contrast = lerp(1f, 1.55f, intensity), saturation = lerp(1f, 1.35f, intensity)))
                    "POSTER" -> filters.add(eqFilter(contrast = lerp(1f, 1.75f, intensity), saturation = lerp(1f, 1.55f, intensity)))
                    "LOMO" -> filters.addAll(listOf(
                        hueFilter(hue = 12f * intensity, saturation = lerp(1f, 1.42f, intensity)),
                        eqFilter(contrast = lerp(1f, 1.55f, intensity))
                    ))
                    "CYBERPUNK" -> filters.addAll(listOf(
                        hueFilter(hue = -150f * intensity, saturation = lerp(1f, 1.85f, intensity)),
                        eqFilter(contrast = lerp(1f, 1.55f, intensity), brightness = 0.04f * intensity)
                    ))
                    "NONE", "NORMAL" -> Unit
                }
            }
        }

        val adjustedContrast = (contrast + 1f).coerceIn(0.01f, 3.0f)
        val adjustedSaturation = saturation.coerceIn(0.0f, 3.0f)
        val adjustedBrightness = brightness.coerceIn(-1.0f, 1.0f)
        filters.add(
            "eq=" +
                "brightness=${formatFilterFloat(adjustedBrightness)}:" +
                "contrast=${formatFilterFloat(adjustedContrast)}:" +
                "saturation=${formatFilterFloat(adjustedSaturation)}"
        )
        if (finalScale != null) filters.add(finalScale)
        return filters.joinToString(",")
    }

    private fun formatFilterFloat(value: Float): String = String.format(Locale.US, "%.4f", value)

    private fun lerp(start: Float, end: Float, amount: Float): Float = start + (end - start) * amount.coerceIn(0f, 1f)

    private fun hueFilter(hue: Float? = null, saturation: Float? = null): String {
        val parts = mutableListOf<String>()
        if (hue != null) parts.add("h=${formatFilterFloat(hue)}")
        if (saturation != null) parts.add("s=${formatFilterFloat(saturation)}")
        return "hue=${parts.joinToString(":")}"
    }

    private fun eqFilter(
        brightness: Float? = null,
        contrast: Float? = null,
        saturation: Float? = null
    ): String {
        val parts = mutableListOf<String>()
        if (brightness != null) parts.add("brightness=${formatFilterFloat(brightness)}")
        if (contrast != null) parts.add("contrast=${formatFilterFloat(contrast)}")
        if (saturation != null) parts.add("saturation=${formatFilterFloat(saturation)}")
        return "eq=${parts.joinToString(":")}"
    }

    private fun invertFilter(intensity: Float): String {
        val keep = formatFilterFloat(1f - 2f * intensity.coerceIn(0f, 1f))
        val offset = formatFilterFloat(255f * intensity.coerceIn(0f, 1f))
        return "lutrgb=r=val*$keep+$offset:g=val*$keep+$offset:b=val*$keep+$offset"
    }

    private fun escapeDrawText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(":", "\\:")
            .replace("'", "\\'")
            .replace("%", "\\%")
            .replace("\n", "\\n")
    }

    internal fun asFriendlyError(e: Throwable): IllegalStateException {
        val raw = e.message.orEmpty()
        val lower = raw.lowercase(Locale.US)
        val message = when {
            "no such file" in lower || "not found" in lower -> "Could not read file."
            "invalid data" in lower || "unsupported" in lower -> "Format not supported."
            "no space" in lower || "enospc" in lower -> "Not enough storage."
            else -> "Export failed. Please try another file."
        }
        return IllegalStateException(message)
    }
    internal data class ExecutionPlan(val arguments: List<String>, val operation: String, val tempFiles: List<File> = emptyList())
}
