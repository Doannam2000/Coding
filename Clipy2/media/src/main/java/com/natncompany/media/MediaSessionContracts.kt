package com.natncompany.media

import android.view.Surface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MediaSessionManager {
    val state: StateFlow<MediaSessionState>
    val events: Flow<MediaSessionEvent>

    suspend fun openProject(project: VideoProject): MediaResult<VideoProject>
    suspend fun closeProject(): MediaResult<Unit>
    suspend fun importMedia(input: MediaImportInput): MediaResult<Asset>
    suspend fun importBatch(inputs: List<MediaImportInput>): MediaResult<List<Asset>>
    suspend fun preparePreview(surface: Surface?): MediaResult<Unit>
    suspend fun play(): MediaResult<Unit>
    suspend fun pause(): MediaResult<Unit>
    suspend fun stop(): MediaResult<Unit>
    suspend fun seekTo(positionMs: Long): MediaResult<Unit>
    suspend fun scrubTo(positionMs: Long): MediaResult<Unit>
    suspend fun updateTimeline(timeline: Timeline): MediaResult<Timeline>
    suspend fun transcodeAsset(assetId: String): MediaResult<Unit>
    suspend fun export(config: MediaExportConfig): MediaResult<Unit>
    suspend fun cancelAllJobs(): MediaResult<Unit>
}

interface MediaDiagnostics {
    fun getVideoEncoders(): MediaResult<List<CodecDescriptor>>
    fun getVideoDecoders(): MediaResult<List<CodecDescriptor>>
    fun getAudioEncoders(): MediaResult<List<CodecDescriptor>>
    fun getAudioDecoders(): MediaResult<List<CodecDescriptor>>
    fun canDecode(mimeType: String): MediaResult<Boolean>
    fun canEncode(mimeType: String): MediaResult<Boolean>
    fun buildDebugReport(
        asset: Asset,
        metadata: MediaMetadata?,
        compatibility: Compatibility?
    ): MediaResult<AssetDebugReport>
}
