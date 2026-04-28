package com.natncompany.media.transcode

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.natncompany.media.RenderRequest
import com.natncompany.media.TranscodeRequest
import com.natncompany.media.render.RenderCommandBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ConcurrentHashMap

class FfmpegKitBackend : FfmpegBackend {
    private val sessions = ConcurrentHashMap<String, Long>()

    override fun transcode(request: TranscodeRequest, outputPath: String): Flow<Int> {
        return executeFlow(request.jobId, buildTranscodeCommand(request, outputPath), request.asset.durationMs ?: 0L)
    }

    override fun render(request: RenderRequest, outputPath: String): Flow<Int> {
        return executeFlow(request.jobId, buildRenderCommand(request, outputPath), request.timeline.durationMs)
    }

    override suspend fun cancel(projectId: String) {
        sessions.remove(projectId)?.let { sessionId ->
            runCatching { FFmpegKit.cancel(sessionId) }
        }
    }

    private fun executeFlow(jobId: String, command: String, durationMs: Long): Flow<Int> = callbackFlow {
        try {
            val session = FFmpegKit.executeAsync(
                command,
                { completedSession ->
                    val returnCode = completedSession.returnCode
                    sessions.remove(jobId)
                    when {
                        ReturnCode.isSuccess(returnCode) -> {
                            trySend(100)
                            channel.close()
                        }
                        ReturnCode.isCancel(returnCode) -> close(CancellationException("FFmpeg operation cancelled"))
                        else -> {
                            val message = completedSession.failStackTrace
                                ?.ifBlank { null }
                                ?: completedSession.output?.ifBlank { null }
                                ?: "FFmpeg command failed"
                            close(FfmpegUnavailableException(message))
                        }
                    }
                },
                { /* Keep callback registered so FFmpegKit captures session output. */ },
                { statistics ->
                    val progress = if (durationMs > 0L) {
                        ((statistics.time * 100L) / durationMs).toInt().coerceIn(0, 99)
                    } else {
                        50
                    }
                    trySend(progress)
                }
            )

            sessions[jobId] = session.sessionId
        } catch (throwable: Throwable) {
            close(FfmpegUnavailableException(throwable.message ?: "Unable to invoke FFmpeg AAR backend"))
        }

        awaitClose {
            sessions.remove(jobId)?.let { sessionId ->
                runCatching { FFmpegKit.cancel(sessionId) }
            }
        }
    }

    private fun buildTranscodeCommand(request: TranscodeRequest, outputPath: String): String {
        val videoFilter = buildString {
            append("scale=")
            append(request.maxWidth)
            append(":")
            append(request.maxHeight)
            append(":force_original_aspect_ratio=decrease")
            append(",pad=")
            append(request.maxWidth)
            append(":")
            append(request.maxHeight)
            append(":(ow-iw)/2:(oh-ih)/2:color=black")
            append(",setsar=1")
        }
        return buildString {
            append("-y -i \"")
            append(request.asset.cachedPath)
            append("\"")
            append(" -vf \"")
            append(videoFilter)
            append("\"")
            append(" -r ")
            append(request.maxFps)
            append(" -c:v libx264 -preset fast -crf 23 -pix_fmt yuv420p")
            append(" -c:a aac -b:a 192k -movflags +faststart \"")
            append(outputPath)
            append("\"")
        }
    }

    private fun buildRenderCommand(request: RenderRequest, outputPath: String): String {
        return RenderCommandBuilder.build(request, outputPath)
    }
}
