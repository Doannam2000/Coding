package com.natncompany.media

import android.content.Context
import android.net.Uri
import android.view.Surface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

interface MediaImporter {
    suspend fun import(request: ImportRequest): MediaResult<ImportResult>

    suspend fun importFromUri(uri: Uri, projectId: String): MediaResult<ImportResult> =
        import(ImportRequest(projectId = projectId, uri = uri))

    suspend fun importFromPath(path: String, projectId: String): MediaResult<ImportResult> =
        import(ImportRequest(projectId = projectId, filePath = path))

    fun importBatch(inputs: List<MediaImportInput>, projectId: String): Flow<ImportBatchProgress> = flow {
        val imported = mutableListOf<Asset>()
        val failures = mutableListOf<ImportFailure>()
        emit(ImportBatchProgress(total = inputs.size, completed = 0, succeeded = 0, failed = 0))
        inputs.forEachIndexed { index, input ->
            val result = import(ImportRequest(projectId = projectId, uri = input.uri, filePath = input.filePath))
            when (result) {
                is MediaResult.Success -> {
                    val asset = result.value.asset
                    if (asset != null) {
                        imported += asset
                        emit(
                            ImportBatchProgress(
                                total = inputs.size,
                                completed = index + 1,
                                succeeded = imported.size,
                                failed = failures.size,
                                currentInput = input,
                                latestAsset = asset
                            )
                        )
                    } else {
                        val failure = ImportFailure(input, MediaError.InvalidInput("Import returned no asset"))
                        failures += failure
                        emit(
                            ImportBatchProgress(
                                total = inputs.size,
                                completed = index + 1,
                                succeeded = imported.size,
                                failed = failures.size,
                                currentInput = input,
                                latestError = failure.error
                            )
                        )
                    }
                }
                is MediaResult.Failure -> {
                    failures += ImportFailure(input, result.error)
                    emit(
                        ImportBatchProgress(
                            total = inputs.size,
                            completed = index + 1,
                            succeeded = imported.size,
                            failed = failures.size,
                            currentInput = input,
                            latestError = result.error
                        )
                    )
                }
            }
        }
        emit(
            ImportBatchProgress(
                total = inputs.size,
                completed = inputs.size,
                succeeded = imported.size,
                failed = failures.size,
                result = ImportBatchResult(imported = imported, failures = failures)
            )
        )
    }
}

interface MetadataReader {
    suspend fun read(asset: Asset): MediaResult<MetadataResult>

    suspend fun readMetadata(asset: Asset): MediaResult<MediaMetadata> =
        read(asset).map { it.metadata }

    suspend fun checkCompatibility(asset: Asset, metadata: MediaMetadata): MediaResult<CompatibilityReport> =
        MediaResult.Success(
            CompatibilityReport(
                isSafe = !asset.needsTranscode && metadata.durationMs > 0L,
                needsTranscode = asset.needsTranscode || metadata.durationMs <= 0L,
                reasons = buildList {
                    if (asset.needsTranscode) add("Asset was marked for transcode")
                    if (metadata.durationMs <= 0L) add("Missing duration")
                }
            )
        )

    suspend fun isTimelineSafe(asset: Asset, metadata: MediaMetadata): MediaResult<Boolean> =
        checkCompatibility(asset, metadata).map { it.isSafe }
}

interface TimelineEditor {
    fun addTrack(
        timeline: Timeline,
        track: TimelineTrack,
        index: Int = timeline.tracks.size
    ): MediaResult<Timeline>

    fun removeTrack(timeline: Timeline, trackId: String): MediaResult<Timeline>
    fun reorderTrack(timeline: Timeline, trackId: String, newIndex: Int): MediaResult<Timeline>
    fun setTrackEnabled(timeline: Timeline, trackId: String, enabled: Boolean): MediaResult<Timeline>
    fun setTrackLocked(timeline: Timeline, trackId: String, locked: Boolean): MediaResult<Timeline>
    fun setTrackMuted(timeline: Timeline, trackId: String, muted: Boolean): MediaResult<Timeline>
    fun calculateTrackDuration(timeline: Timeline, trackId: String): MediaResult<Long>

    fun addClip(timeline: Timeline, trackId: String, clip: TimelineClip): MediaResult<Timeline>
    fun removeClip(timeline: Timeline, trackId: String, clipId: String): MediaResult<Timeline>
    fun selectClip(timeline: Timeline, clipId: String, selected: Boolean = true): MediaResult<Timeline>
    fun clearSelection(timeline: Timeline): MediaResult<Timeline>
    fun groupClips(timeline: Timeline, groupId: String, clipIds: Set<String>): MediaResult<Timeline>
    fun ungroupClips(timeline: Timeline, groupId: String): MediaResult<Timeline>
    fun validateClipPosition(timeline: Timeline, trackId: String, clip: TimelineClip): MediaResult<TimelineClip>
    fun splitClip(timeline: Timeline, trackId: String, clipId: String, playheadMs: Long): MediaResult<Timeline>
    fun splitAllAt(timeline: Timeline, playheadMs: Long): MediaResult<Timeline>
    fun trimClip(timeline: Timeline, trackId: String, clipId: String, startMs: Long, endMs: Long): MediaResult<Timeline>
    fun trimClipStart(
        timeline: Timeline,
        trackId: String,
        clipId: String,
        newSourceStartMs: Long,
        ripple: Boolean = false
    ): MediaResult<Timeline>

    fun trimClipEnd(
        timeline: Timeline,
        trackId: String,
        clipId: String,
        newSourceEndMs: Long,
        ripple: Boolean = false
    ): MediaResult<Timeline>

    fun moveClip(
        timeline: Timeline,
        fromTrackId: String,
        toTrackId: String,
        clipId: String,
        newStartMs: Long,
        newIndex: Int? = null
    ): MediaResult<Timeline>

    fun duplicateClip(timeline: Timeline, trackId: String, clipId: String, newClipId: String): MediaResult<Timeline>
    fun snapClip(
        timeline: Timeline,
        trackId: String,
        clipId: String,
        desiredStartMs: Long,
        playheadMs: Long? = null,
        thresholdMs: Long = timeline.settings.snapThresholdMs,
        enabled: Boolean = timeline.settings.snapEnabled
    ): MediaResult<SnapResult>

    fun calculateDuration(timeline: Timeline): MediaResult<Long>
    fun calculateClipVisibleRange(timeline: Timeline, clipId: String): MediaResult<ClipRange>
    fun calculateSourceRange(timeline: Timeline, clipId: String): MediaResult<SourceRange>
    fun validateTimeline(timeline: Timeline): MediaResult<Timeline>
    fun validate(timeline: Timeline): MediaResult<Timeline> = validateTimeline(timeline)
    fun undo(timeline: Timeline): MediaResult<Timeline>
    fun redo(timeline: Timeline): MediaResult<Timeline>
}

interface PreviewController {
    val state: StateFlow<PreviewState>

    suspend fun openProject(project: VideoProject): MediaResult<Unit> = prepare(project)
    suspend fun prepare(): MediaResult<Unit> = MediaResult.Success(Unit)
    suspend fun prepare(project: VideoProject): MediaResult<Unit>
    suspend fun setSurface(surface: Surface?): MediaResult<Unit>
    suspend fun play(): MediaResult<Unit>
    suspend fun pause(): MediaResult<Unit>
    suspend fun stop(): MediaResult<Unit> {
        pause()
        return seek(0L)
    }
    suspend fun seek(positionMs: Long): MediaResult<Unit>
    suspend fun seekTo(positionMs: Long): MediaResult<Unit> = seek(positionMs)
    suspend fun scrub(positionMs: Long): MediaResult<Unit>
    suspend fun scrubTo(positionMs: Long): MediaResult<Unit> = scrub(positionMs)
    suspend fun updateTimeline(project: VideoProject): MediaResult<Unit>
    suspend fun release(): MediaResult<Unit>
}

interface Transcoder {
    suspend fun shouldTranscode(asset: Asset, metadata: MediaMetadata): MediaResult<Boolean>
    fun transcode(request: TranscodeRequest): Flow<TranscodeUpdate>
    fun transcode(asset: Asset, projectId: String = "default"): Flow<TranscodeUpdate> =
        transcode(TranscodeRequest(asset = asset, projectId = projectId))

    suspend fun cancel(jobId: String): MediaResult<Unit>
}

interface Renderer {
    fun render(request: RenderRequest): Flow<RenderUpdate>
    fun render(project: VideoProject, timeline: Timeline, config: RenderConfig): Flow<RenderUpdate> =
        render(RenderRequest(project = project.copy(timeline = timeline), outputFileName = config.outputFileName, timeline = timeline))

    suspend fun cancel(jobId: String): MediaResult<Unit>
    suspend fun cancelRender(jobId: String): MediaResult<Unit> = cancel(jobId)
    suspend fun validate(request: RenderRequest): MediaResult<Unit>
    suspend fun validateBeforeRender(project: VideoProject, timeline: Timeline, config: RenderConfig): MediaResult<Unit> =
        validate(RenderRequest(project = project.copy(timeline = timeline), outputFileName = config.outputFileName, timeline = timeline))

    fun estimateRenderTime(project: VideoProject, timeline: Timeline, config: RenderConfig): MediaResult<Long> {
        val complexity = timeline.tracks.sumOf { it.clips.size }.coerceAtLeast(1)
        return MediaResult.Success(timeline.durationMs * (1L + complexity / 4L))
    }
}

interface AudioProcessor {
    suspend fun analyzeAudio(asset: Asset): MediaResult<AudioInfo>
    suspend fun readInfo(asset: Asset): MediaResult<AudioInfo> = analyzeAudio(asset)
    fun applyAudioSettings(clip: TimelineClip, settings: AudioSettings): MediaResult<TimelineClip>
    fun setVolume(clip: TimelineClip, volume: Float): MediaResult<TimelineClip> =
        applyAudioSettings(clip, AudioSettings(volume = volume))

    fun mute(clip: TimelineClip, muted: Boolean): MediaResult<TimelineClip> =
        applyAudioSettings(clip, AudioSettings(muted = muted))

    fun setFade(clip: TimelineClip, fadeInMs: Long, fadeOutMs: Long): MediaResult<TimelineClip> =
        applyAudioSettings(clip, AudioSettings(fadeInMs = fadeInMs, fadeOutMs = fadeOutMs))

    fun createMixPlan(timeline: Timeline): MediaResult<AudioMixPlan>
    fun buildMixPlan(timeline: Timeline): MediaResult<AudioMixPlan> = createMixPlan(timeline)
    fun mapTimelineToSourceTime(clip: TimelineClip, timelineTimeMs: Long): MediaResult<Long>
    fun extractWaveform(asset: Asset, config: WaveformConfig = WaveformConfig()): Flow<WaveformProgress>
    fun createWaveformPlaceholder(asset: Asset, samples: Int = 32): MediaResult<WaveformPlaceholder>
}

interface ProjectCacheManager {
    suspend fun createProjectCache(projectId: String): MediaResult<CacheLayout>
    suspend fun createProject(projectId: String): MediaResult<CacheLayout> = createProjectCache(projectId)
    suspend fun createSafeFileName(projectId: String, originalName: String, extension: String): MediaResult<String>
    suspend fun safeFileName(projectId: String, originalName: String, extension: String): MediaResult<String> =
        createSafeFileName(projectId, originalName, extension)

    suspend fun copyToAssets(projectId: String, source: String): MediaResult<CachedAssetFile>
    suspend fun createTranscodedFile(projectId: String, extension: String): MediaResult<String>
    suspend fun createRenderOutput(projectId: String, fileName: String): MediaResult<String>
    suspend fun clearTemp(projectId: String): MediaResult<Unit>
    suspend fun deleteTempFiles(projectId: String): MediaResult<Unit> = clearTemp(projectId)
    suspend fun deleteUnusedAssets(projectId: String, usedAssetIds: Set<String>): MediaResult<Int>
    suspend fun deleteProjectCache(projectId: String): MediaResult<Unit>
    suspend fun deleteProject(projectId: String): MediaResult<Unit> = deleteProjectCache(projectId)
    suspend fun getCacheSize(projectId: String): MediaResult<CacheStats>
    suspend fun stats(projectId: String): MediaResult<CacheStats> = getCacheSize(projectId)
}

interface MediaModuleFactory {
    fun createImporter(context: Context): MediaImporter
    fun createMetadataReader(context: Context): MetadataReader
    fun createTimelineEditor(): TimelineEditor
    fun createPreviewController(context: Context): PreviewController
    fun createTranscoder(context: Context): Transcoder
    fun createRenderer(context: Context): Renderer
    fun createAudioProcessor(context: Context): AudioProcessor
    fun createCacheManager(context: Context): ProjectCacheManager
    fun createDiagnostics(context: Context): MediaDiagnostics
    fun createSessionManager(context: Context): MediaSessionManager
}
