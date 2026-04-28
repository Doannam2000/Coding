package com.natncompany.media.transcode

import com.natncompany.media.RenderRequest
import com.natncompany.media.TranscodeRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface FfmpegBackend {
    fun transcode(request: TranscodeRequest, outputPath: String): Flow<Int>
    fun render(request: RenderRequest, outputPath: String): Flow<Int>
    suspend fun cancel(projectId: String)
}

class UnavailableFfmpegBackend : FfmpegBackend {
    override fun transcode(request: TranscodeRequest, outputPath: String): Flow<Int> = flow {
        throw FfmpegUnavailableException("FFmpeg backend is not bound. Provide an FfmpegBackend implementation.")
    }

    override fun render(request: RenderRequest, outputPath: String): Flow<Int> = flow {
        throw FfmpegUnavailableException("FFmpeg backend is not bound. Provide an FfmpegBackend implementation.")
    }

    override suspend fun cancel(projectId: String) = Unit
}

class FfmpegUnavailableException(message: String) : RuntimeException(message)
