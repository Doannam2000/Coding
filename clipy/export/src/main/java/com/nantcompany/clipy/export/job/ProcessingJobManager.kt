package com.nantcompany.clipy.export.job

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import com.nantcompany.clipy.export.output.LocalOutputRepository
import com.nantcompany.clipy.export.output.OutputMedia
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ProcessingJobManager(
    private val outputRepository: LocalOutputRepository = LocalOutputRepository()
) {
    interface ProgressCallback {
        fun onProgress(event: ProcessEvent.Progress)
    }

    @Volatile
    private var cancelled: Boolean = false

    fun process(request: ProcessingRequest, progressCallback: ProgressCallback? = null): ProcessEvent {
        cancelled = false
        progressCallback?.onProgress(ProcessEvent.Progress(percent = 5, timeMs = 0L, statusText = "Preparing..."))

        val outputFile = File(request.outputPath)
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val totalDurationMs = resolveDurationMs(request)
        val firstPlan = try {
            buildExecutionPlan(request, preciseCut = false, mergeReencode = false)
        } catch (error: Throwable) {
            return ProcessEvent.Failed(error)
        }

        val firstResult = executePlan(firstPlan, outputFile, totalDurationMs, progressCallback)
        if (firstResult is ProcessEvent.Failed && !cancelled) {
            val retryPlan = when (request) {
                is ProcessingRequest.Cut -> runCatching {
                    buildExecutionPlan(request, preciseCut = true, mergeReencode = false)
                }.getOrNull()

                is ProcessingRequest.Merge -> runCatching {
                    buildExecutionPlan(request, preciseCut = false, mergeReencode = true)
                }.getOrNull()

                else -> null
            }
            if (retryPlan != null) {
                return executePlan(retryPlan, outputFile, totalDurationMs, progressCallback)
            }
        }

        return firstResult
    }

    private fun executePlan(
        plan: ExecutionPlan,
        outputFile: File,
        totalDurationMs: Long,
        progressCallback: ProgressCallback?
    ): ProcessEvent {
        return try {
            val done = CountDownLatch(1)
            val resultRef = AtomicReference<ProcessEvent>()

            FFmpegKit.executeWithArgumentsAsync(
                plan.arguments.toTypedArray(),
                { session ->
                    val result = when {
                        cancelled -> ProcessEvent.Cancelled
                        ReturnCode.isCancel(session.returnCode) -> ProcessEvent.Cancelled
                        ReturnCode.isSuccess(session.returnCode) -> {
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

                        else -> {
                            val details = session.failStackTrace
                                ?: session.allLogsAsString
                                ?: "Unknown FFmpeg error"
                            ProcessEvent.Failed(IllegalStateException(details))
                        }
                    }
                    resultRef.set(result)
                    done.countDown()
                },
                null,
                { statistics ->
                    val timeMs = statistics.time.toLong().coerceAtLeast(0L)
                    val percent = if (totalDurationMs > 0L) {
                        ((timeMs.toDouble() / totalDurationMs.toDouble()) * 100.0).toInt().coerceIn(5, 99)
                    } else {
                        20
                    }
                    val status = if (plan.operation.startsWith("merge")) "Merging clips..." else "Running FFmpeg..."
                    progressCallback?.onProgress(
                        ProcessEvent.Progress(
                            percent = percent,
                            timeMs = timeMs,
                            statusText = status
                        )
                    )
                }
            )

            while (!done.await(250, TimeUnit.MILLISECONDS)) {
                if (cancelled) {
                    FFmpegKit.cancel()
                }
            }

            resultRef.get() ?: ProcessEvent.Failed(IllegalStateException("FFmpeg session ended without result"))
        } catch (error: Throwable) {
            ProcessEvent.Failed(error)
        } finally {
            plan.tempFiles.forEach { temp ->
                runCatching { temp.delete() }
            }
        }
    }

    fun cancelProcessing() {
        cancelled = true
        FFmpegKit.cancel()
    }

    private fun buildExecutionPlan(
        request: ProcessingRequest,
        preciseCut: Boolean = false,
        mergeReencode: Boolean = false
    ): ExecutionPlan {
        return when (request) {
            is ProcessingRequest.Cut -> {
                val start = formatSeconds(request.request.startMs)
                val end = formatSeconds(request.request.endMs)
                if (!preciseCut) {
                    ExecutionPlan(
                        arguments = listOf(
                            "-y", "-ss", start, "-to", end,
                            "-i", request.request.inputPath,
                            "-c", "copy",
                            request.outputPath
                        ),
                        operation = "cut-fast"
                    )
                } else {
                    ExecutionPlan(
                        arguments = listOf(
                            "-y", "-i", request.request.inputPath,
                            "-ss", start, "-to", end,
                            "-c:v", "libx264",
                            "-c:a", "aac",
                            request.outputPath
                        ),
                        operation = "cut-precise"
                    )
                }
            }

            is ProcessingRequest.Compress -> {
                ExecutionPlan(
                    arguments = listOf(
                        "-y", "-i", request.request.inputPath,
                        "-b:v", "${request.request.bitrateKbps}k",
                        "-c:v", "libx264",
                        "-preset", "medium",
                        "-c:a", "aac",
                        "-b:a", "128k",
                        request.outputPath
                    ),
                    operation = "compress"
                )
            }

            is ProcessingRequest.Merge -> {
                val listFile = File.createTempFile("clipy-merge-", ".txt")
                val text = request.request.inputPaths.joinToString(separator = "\n") { path ->
                    "file '${escapeConcatPath(path)}'"
                }
                listFile.writeText(text)

                val args = if (!mergeReencode) {
                    listOf(
                        "-y", "-f", "concat", "-safe", "0",
                        "-i", listFile.absolutePath,
                        "-c", "copy",
                        request.outputPath
                    )
                } else {
                    listOf(
                        "-y", "-f", "concat", "-safe", "0",
                        "-i", listFile.absolutePath,
                        "-c:v", "libx264",
                        "-c:a", "aac",
                        request.outputPath
                    )
                }

                ExecutionPlan(
                    arguments = args,
                    operation = if (!mergeReencode) "merge-fast" else "merge-precise",
                    tempFiles = listOf(listFile)
                )
            }

            is ProcessingRequest.ExtractAudio -> {
                val codec = when (request.request.format.lowercase()) {
                    "aac", "m4a" -> "aac"
                    "wav" -> "pcm_s16le"
                    else -> "libmp3lame"
                }
                ExecutionPlan(
                    arguments = listOf(
                        "-y", "-i", request.request.inputPath,
                        "-vn", "-c:a", codec,
                        request.outputPath
                    ),
                    operation = "extract-audio"
                )
            }

            is ProcessingRequest.Slideshow -> {
                val listFile = File.createTempFile("clipy-slideshow-", ".txt")
                val lines = buildList {
                    request.request.imagePaths.forEach { path ->
                        add("file '${escapeConcatPath(path)}'")
                        add("duration ${request.request.secondsPerImage}")
                    }
                    request.request.imagePaths.lastOrNull()?.let { path ->
                        add("file '${escapeConcatPath(path)}'")
                    }
                }
                listFile.writeText(lines.joinToString(separator = "\n"))
                ExecutionPlan(
                    arguments = listOf(
                        "-y", "-f", "concat", "-safe", "0",
                        "-i", listFile.absolutePath,
                        "-vsync", "vfr",
                        "-pix_fmt", "yuv420p",
                        "-c:v", "libx264",
                        request.outputPath
                    ),
                    operation = "slideshow",
                    tempFiles = listOf(listFile)
                )
            }
        }
    }

    private fun resolveDurationMs(request: ProcessingRequest): Long {
        return when (request) {
            is ProcessingRequest.Cut -> (request.request.endMs - request.request.startMs).coerceAtLeast(0L)
            is ProcessingRequest.Slideshow -> {
                (request.request.imagePaths.size.toLong() * request.request.secondsPerImage * 1000.0).toLong()
            }

            else -> {
                val inputPath = when (request) {
                    is ProcessingRequest.Compress -> request.request.inputPath
                    is ProcessingRequest.Merge -> request.request.inputPaths.firstOrNull()
                    is ProcessingRequest.ExtractAudio -> request.request.inputPath
                    else -> null
                }
                inputPath?.let { probeDurationMs(it) } ?: 0L
            }
        }
    }

    private fun probeDurationMs(inputPath: String): Long {
        return runCatching {
            val session = FFprobeKit.getMediaInformation(inputPath)
            val info = session.mediaInformation ?: return 0L
            val duration = info.duration ?: return 0L
            (duration.toDoubleOrNull()?.times(1000.0))?.toLong()?.coerceAtLeast(0L) ?: 0L
        }.getOrDefault(0L)
    }

    private fun formatSeconds(milliseconds: Long): String {
        val total = milliseconds.coerceAtLeast(0L) / 1000.0
        return String.format(java.util.Locale.US, "%.3f", total)
    }

    private fun escapeConcatPath(path: String): String = path.replace("'", "'\\''")

    private data class ExecutionPlan(
        val arguments: List<String>,
        val operation: String,
        val tempFiles: List<File> = emptyList()
    )
}
