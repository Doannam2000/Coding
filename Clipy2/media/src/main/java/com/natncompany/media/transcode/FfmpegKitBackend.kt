package com.natncompany.media.transcode

import com.natncompany.media.RenderRequest
import com.natncompany.media.TranscodeRequest
import com.natncompany.media.render.RenderCommandBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ConcurrentHashMap

@Suppress("UNCHECKED_CAST")
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
            runCatching {
                val ffmpegKit = Class.forName("com.arthenica.ffmpegkit.FFmpegKit")
                ffmpegKit.getMethod("cancel", Long::class.javaPrimitiveType).invoke(null, sessionId)
            }
        }
    }

    private fun executeFlow(jobId: String, command: String, durationMs: Long): Flow<Int> = callbackFlow {
        try {
            val ffmpegKitClass = Class.forName("com.arthenica.ffmpegkit.FFmpegKit")
            val completeCallbackClass = Class.forName("com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback")
            val logCallbackClass = Class.forName("com.arthenica.ffmpegkit.LogCallback")
            val statisticsCallbackClass = Class.forName("com.arthenica.ffmpegkit.StatisticsCallback")
            val returnCodeClass = Class.forName("com.arthenica.ffmpegkit.ReturnCode")

            val complete = java.lang.reflect.Proxy.newProxyInstance(
                completeCallbackClass.classLoader,
                arrayOf(completeCallbackClass)
            ) { _, _, args ->
                val session = args?.firstOrNull()
                val returnCode = session?.javaClass?.getMethod("getReturnCode")?.invoke(session)
                sessions.remove(jobId)
                val isSuccess = returnCodeClass.getMethod("isSuccess", returnCodeClass).invoke(null, returnCode) as Boolean
                val isCancel = returnCodeClass.getMethod("isCancel", returnCodeClass).invoke(null, returnCode) as Boolean
                when {
                    isSuccess -> {
                        trySend(100)
                        channel.close()
                    }
                    isCancel -> close(CancellationException("FFmpeg operation cancelled"))
                    else -> {
                        val failStack = session?.javaClass?.getMethod("getFailStackTrace")?.invoke(session) as? String
                        val output = session?.javaClass?.getMethod("getOutput")?.invoke(session) as? String
                        close(FfmpegUnavailableException(failStack?.ifBlank { null } ?: output?.ifBlank { null } ?: "FFmpeg command failed"))
                    }
                }
                null
            }

            val log = java.lang.reflect.Proxy.newProxyInstance(
                logCallbackClass.classLoader,
                arrayOf(logCallbackClass)
            ) { _, _, _ -> null }

            val stats = java.lang.reflect.Proxy.newProxyInstance(
                statisticsCallbackClass.classLoader,
                arrayOf(statisticsCallbackClass)
            ) { _, _, args ->
                val statistics = args?.firstOrNull()
                val time = (statistics?.javaClass?.getMethod("getTime")?.invoke(statistics) as? Number)?.toLong() ?: 0L
                val progress = if (durationMs > 0L) ((time * 100L) / durationMs).toInt().coerceIn(0, 99) else 50
                trySend(progress)
                null
            }

            val executeAsync = ffmpegKitClass.methods.first { method ->
                method.name == "executeAsync" && method.parameterTypes.size == 4
            }
            val session = executeAsync.invoke(null, command, complete, log, stats)
            val sessionId = (session?.javaClass?.getMethod("getSessionId")?.invoke(session) as? Number)?.toLong()
            if (sessionId != null) {
                sessions[jobId] = sessionId
            }
        } catch (throwable: Throwable) {
            close(FfmpegUnavailableException(throwable.message ?: "Unable to invoke FFmpeg AAR backend"))
        }

        awaitClose {
            sessions.remove(jobId)?.let { sessionId ->
                runCatching {
                    val ffmpegKit = Class.forName("com.arthenica.ffmpegkit.FFmpegKit")
                    ffmpegKit.getMethod("cancel", Long::class.javaPrimitiveType).invoke(null, sessionId)
                }
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
