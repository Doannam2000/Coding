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
    private val context: Context,
    private val outputRepository: LocalOutputRepository
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

        val useHardware = request is ProcessingRequest.Cut || 
                          request is ProcessingRequest.Compress || 
                          request is ProcessingRequest.Rotate ||
                          request is ProcessingRequest.Speed ||
                          request is ProcessingRequest.Crop

        if (useHardware) {
            progressCallback?.onProgress(ProcessEvent.ProgressUpdate(5, 0L, "Accelerating via GPU"))
            val hwResult = runCatching { executeWithTransformer(request, outputFile, progressCallback) }.getOrNull()
            if (hwResult is ProcessEvent.Completed || hwResult is ProcessEvent.Cancelled) return@withContext hwResult
            progressCallback?.onProgress(ProcessEvent.ProgressUpdate(10, 0L, "Switching to Compatibility Engine"))
        }

        val totalDurationMs = resolveDurationMs(request)
        val plan = try { buildExecutionPlan(request) } catch (error: Throwable) { return@withContext ProcessEvent.Failed(asFriendlyError(error)) }
        executeFFmpegPlan(request, plan, outputFile, totalDurationMs, progressCallback)
    }

    private suspend fun executeWithTransformer(request: ProcessingRequest, outputFile: File, progressCallback: ProgressCallback?): ProcessEvent = callbackFlow {
        val transformer = Transformer.Builder(context).build()
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
                    else -> ProcessEvent.Failed(asFriendlyError(IllegalStateException("FFmpeg error")))
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

    private fun buildExecutionPlan(request: ProcessingRequest): ExecutionPlan {
        return when (request) {
            is ProcessingRequest.Cut -> {
                val r = (request as ProcessingRequest.Cut).request
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-ss", formatSeconds(r.startMs), "-to", formatSeconds(r.endMs), "-c:v", "libx264", "-c:a", "aac", request.outputPath), "cut")
            }
            is ProcessingRequest.Compress -> {
                val r = (request as ProcessingRequest.Compress).request
                val args = mutableListOf("-y", "-i", r.inputPath, "-b:v", "${r.bitrateKbps}k", "-c:v", "libx264", "-preset", "fast")
                r.targetHeight?.let { args.addAll(listOf("-vf", "scale=-2:$it")) }
                if (r.keepAudio) args.addAll(listOf("-c:a", "aac")) else args.add("-an")
                args.add(request.outputPath); ExecutionPlan(args, "compress")
            }
            is ProcessingRequest.Merge -> {
                val r = (request as ProcessingRequest.Merge).request
                if (r.transition == "none") {
                    val listFile = File.createTempFile("merge-", ".txt")
                    listFile.writeText(r.inputPaths.joinToString("\n") { "file '${it.replace("'", "'\\''")}'" })
                    ExecutionPlan(listOf("-y", "-f", "concat", "-safe", "0", "-i", listFile.absolutePath, "-c:v", "libx264", "-c:a", "aac", request.outputPath), "merge", listOf(listFile))
                } else {
                    val args = mutableListOf("-y")
                    r.inputPaths.forEach { args.addAll(listOf("-i", it)) }
                    
                    val filter = StringBuilder()
                    r.inputPaths.forEachIndexed { i, _ ->
                        filter.append("[$i:v]scale=720:1280:force_original_aspect_ratio=decrease,pad=720:1280:(ow-iw)/2:(oh-ih)/2,setsar=1[v$i];")
                        filter.append("[$i:a]aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo[a$i];")
                    }
                    
                    val transDur = r.transitionDurationMs / 1000.0
                    val durations = r.inputPaths.map { probeDurationMs(it) / 1000.0 }
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
                        
                        filter.append("[$lastV][$nextV]xfade=transition=${r.transition}:duration=$transDur:offset=$currentOffset")
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
                    
                    args.addAll(listOf("-filter_complex", filter.toString(), "-map", "[outv]", "-map", "[outa]", "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac", request.outputPath))
                    ExecutionPlan(args, "merge-transition")
                }
            }
            is ProcessingRequest.ExtractAudio -> {
                val r = (request as ProcessingRequest.ExtractAudio).request
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-vn", "-c:a", "libmp3lame", "-b:a", "${r.bitrateKbps}k", request.outputPath), "extract-audio")
            }
            is ProcessingRequest.Slideshow -> {
                val r = (request as ProcessingRequest.Slideshow).request
                if (r.transition == "none") {
                    val listFile = File.createTempFile("slide-", ".txt")
                    val lines = r.imagePaths.flatMap { listOf("file '$it'", "duration ${r.secondsPerImage}") } + "file '${r.imagePaths.last()}'"
                    listFile.writeText(lines.joinToString("\n"))
                    ExecutionPlan(listOf("-y", "-f", "concat", "-safe", "0", "-i", listFile.absolutePath, "-pix_fmt", "yuv420p", "-c:v", "libx264", request.outputPath), "slideshow", listOf(listFile))
                } else {
                    val args = mutableListOf("-y")
                    r.imagePaths.forEach { args.addAll(listOf("-loop", "1", "-t", "${r.secondsPerImage}", "-i", it)) }
                    
                    val filter = StringBuilder()
                    // Scale all to 720x1280 (portrait) and set sar
                    r.imagePaths.forEachIndexed { i, _ ->
                        filter.append("[$i:v]scale=720:1280:force_original_aspect_ratio=decrease,pad=720:1280:(ow-iw)/2:(oh-ih)/2,setsar=1[v$i];")
                    }
                    
                    var lastLabel = "v0"
                    val transDur = r.transitionDurationMs / 1000.0
                    r.imagePaths.drop(1).forEachIndexed { i, _ ->
                        val nextIdx = i + 1
                        val offset = (nextIdx * r.secondsPerImage) - (nextIdx * transDur)
                        val outLabel = "v0$nextIdx"
                        filter.append("[$lastLabel][v$nextIdx]xfade=transition=${r.transition}:duration=$transDur:offset=$offset")
                        if (nextIdx < r.imagePaths.size - 1) {
                            filter.append("[$outLabel];")
                            lastLabel = outLabel
                        } else {
                            filter.append("[outv]")
                        }
                    }
                    
                    args.addAll(listOf("-filter_complex", filter.toString(), "-map", "[outv]"))
                    if (r.audioPath != null) {
                        args.addAll(listOf("-i", r.audioPath!!, "-map", "${r.imagePaths.size}:a", "-c:a", "aac", "-shortest"))
                    }
                    args.addAll(listOf("-pix_fmt", "yuv420p", "-c:v", "libx264", "-preset", "ultrafast", request.outputPath))
                    ExecutionPlan(args, "slideshow-transition")
                }
            }
            is ProcessingRequest.Filters -> {
                val r = (request as ProcessingRequest.Filters).request
                val filters = mutableListOf("eq=brightness=${r.brightness}:contrast=${r.contrast + 1f}:saturation=${r.saturation}")
                when (r.filterName.uppercase()) {
                    "SEPIA" -> filters.add("colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131")
                    "GRAYSCALE" -> filters.add("hue=s=0")
                    "CYBERPUNK" -> filters.add("hue=h=-150:s=1.4,eq=contrast=1.15")
                }
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-vf", filters.joinToString(","), "-c:v", "libx264", "-c:a", "copy", request.outputPath), "filters")
            }
            is ProcessingRequest.Rotate -> {
                val r = (request as ProcessingRequest.Rotate).request
                val vf = mutableListOf<String>()
                when (r.rotation) { 90 -> vf.add("transpose=1"); 180 -> vf.add("transpose=2,transpose=2"); 270 -> vf.add("transpose=2") }
                if (r.flipHorizontal) vf.add("hflip")
                if (r.flipVertical) vf.add("vflip")
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-vf", vf.joinToString(","), "-c:v", "libx264", "-c:a", "copy", request.outputPath), "rotate")
            }
            is ProcessingRequest.Speed -> {
                val r = (request as ProcessingRequest.Speed).request
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-vf", "setpts=${1f/r.speedFactor}*PTS", "-af", "atempo=${r.speedFactor}", "-c:v", "libx264", "-c:a", "aac", request.outputPath), "speed")
            }
            is ProcessingRequest.Crop -> {
                val r = (request as ProcessingRequest.Crop).request
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-vf", "crop=${r.width}:${r.height}:${r.x}:${r.y}", "-c:v", "libx264", "-c:a", "copy", request.outputPath), "crop")
            }
            is ProcessingRequest.Reverse -> {
                val r = (request as ProcessingRequest.Reverse).request
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-vf", "reverse", "-af", "areverse", "-c:v", "libx264", "-c:a", "aac", request.outputPath), "reverse")
            }
            is ProcessingRequest.Stickers -> {
                val r = (request as ProcessingRequest.Stickers).request
                // Use main_w and main_h (or W/H) for percentage-based positioning
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-i", r.stickerPath, "-filter_complex", "[0:v][1:v]overlay=(W*${r.x}/100):(H*${r.y}/100)", "-c:v", "libx264", request.outputPath), "stickers")
            }
            is ProcessingRequest.TextOverlay -> {
                val r = (request as ProcessingRequest.TextOverlay).request
                // Use w and h for percentage-based positioning. Also handle font.
                val color = r.fontColor.lowercase()
                val fontPath = "/system/fonts/Roboto-Regular.ttf"
                val vf = "drawtext=text='${r.text}':x=(w*${r.x}/100):y=(h*${r.y}/100):fontsize=${r.fontSize}:fontcolor=$color:fontfile=$fontPath"
                ExecutionPlan(listOf("-y", "-i", r.inputPath, "-vf", vf, "-c:v", "libx264", request.outputPath), "text")
            }
            is ProcessingRequest.Studio -> {
                val r = (request as ProcessingRequest.Studio).request
                val vf = mutableListOf<String>()
                when (r.rotation) { 90 -> vf.add("transpose=1"); 180 -> vf.add("transpose=2,transpose=2"); 270 -> vf.add("transpose=2") }
                if (r.flipHorizontal) vf.add("hflip")
                vf.add("eq=brightness=${r.brightness}:contrast=${r.contrast + 1f}:saturation=${r.saturation}")
                when (r.filterName.uppercase()) {
                    "SEPIA" -> vf.add("colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131")
                    "GRAYSCALE" -> vf.add("hue=s=0")
                    "CYBERPUNK" -> vf.add("hue=h=-150:s=1.4,eq=contrast=1.15")
                }
                if (r.speedFactor != 1.0f) vf.add("setpts=${1f/r.speedFactor}*PTS")
                r.textLayers.forEach { layer ->
                    val colorHex = String.format("#%06X", 0xFFFFFF and layer.color)
                    val fontPath = "/system/fonts/Roboto-Regular.ttf"
                    vf.add("drawtext=text='${layer.text}':x=(w*${layer.x}/100):y=(h*${layer.y}/100):fontsize=28:fontcolor=$colorHex:fontfile=$fontPath")
                }
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
                if (r.speedFactor != 1.0f) args.addAll(listOf("-af", "atempo=${r.speedFactor}"))
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
            else -> 0L
        }
    }

    private fun probeDurationMs(path: String): Long {
        return runCatching {
            val session = FFprobeKit.getMediaInformation(path)
            (session.mediaInformation?.duration?.toDouble()?.times(1000))?.toLong()
        }.getOrNull() ?: 0L
    }

    private fun formatSeconds(ms: Long) = String.format(Locale.US, "%.3f", ms / 1000.0)
    private fun asFriendlyError(e: Throwable) = IllegalStateException(e.message ?: "Processing Failed")
    private data class ExecutionPlan(val arguments: List<String>, val operation: String, val tempFiles: List<File> = emptyList())
}
