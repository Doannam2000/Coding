package com.natncompany.media.session

import com.natncompany.media.Asset
import com.natncompany.media.AssetType
import com.natncompany.media.AudioInfo
import com.natncompany.media.AudioMixPlan
import com.natncompany.media.AudioProcessor
import com.natncompany.media.AudioSettings
import com.natncompany.media.CacheLayout
import com.natncompany.media.CacheStats
import com.natncompany.media.CachedAssetFile
import com.natncompany.media.ClipRange
import com.natncompany.media.CompatibilityReport
import com.natncompany.media.ImportRequest
import com.natncompany.media.ImportResult
import com.natncompany.media.ImportStatus
import com.natncompany.media.MediaDiagnostics
import com.natncompany.media.MediaError
import com.natncompany.media.MediaImportInput
import com.natncompany.media.MediaMetadata
import com.natncompany.media.MediaResult
import com.natncompany.media.MetadataReader
import com.natncompany.media.PreviewController
import com.natncompany.media.PreviewState
import com.natncompany.media.ProjectCacheManager
import com.natncompany.media.RenderConfig
import com.natncompany.media.RenderRequest
import com.natncompany.media.RenderUpdate
import com.natncompany.media.Renderer
import com.natncompany.media.SourceRange
import com.natncompany.media.SnapResult
import com.natncompany.media.Timeline
import com.natncompany.media.TimelineClip
import com.natncompany.media.TimelineEditor
import com.natncompany.media.TimelineTrack
import com.natncompany.media.TrackType
import com.natncompany.media.TranscodeRequest
import com.natncompany.media.TranscodeUpdate
import com.natncompany.media.Transcoder
import com.natncompany.media.VideoProject
import com.natncompany.media.WaveformConfig
import com.natncompany.media.WaveformPlaceholder
import com.natncompany.media.WaveformProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMediaSessionManagerTest {
    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `openProject updates state and primes preview`() = runTest(dispatcher) {
        val preview = FakePreviewController()
        val manager = createManager(previewController = preview)
        val project = project()

        val result = manager.openProject(project) as MediaResult.Success

        assertEquals(project.id, result.value.id)
        assertEquals(project.id, manager.state.value.currentProject?.id)
        assertEquals(project.id, preview.openedProject?.id)
    }

    @Test
    fun `updateTimeline refreshes project and preview`() = runTest(dispatcher) {
        val preview = FakePreviewController()
        val manager = createManager(previewController = preview)
        val opened = manager.openProject(project()) as MediaResult.Success
        val updatedTimeline = opened.value.timeline.copy(
            tracks = listOf(
                TimelineTrack(
                    id = "video-main",
                    type = TrackType.Video,
                    clips = listOf(sampleClip("clip-1", 0L, 1_000L))
                )
            )
        )

        manager.updateTimeline(updatedTimeline)

        assertEquals(updatedTimeline, manager.state.value.currentTimeline)
        assertEquals(updatedTimeline, preview.updatedProject?.timeline)
    }

    @Test
    fun `transcodeAsset updates normalized asset into current project`() = runTest(dispatcher) {
        val preview = FakePreviewController()
        val normalizedAsset = Asset(
            id = "asset-1",
            sourceUri = "file://asset-1.mp4",
            cachedPath = "normalized.mp4",
            displayName = "asset-1.mp4",
            type = AssetType.Video,
            durationMs = 1_000L,
            mimeType = "video/mp4",
            needsTranscode = false
        )
        val manager = createManager(
            previewController = preview,
            transcoder = FakeTranscoder(
                updates = flowOf(
                    TranscodeUpdate(progressPercent = 50),
                    TranscodeUpdate(progressPercent = 100, asset = normalizedAsset, completed = true)
                )
            )
        )
        val project = project().copy(
            assets = listOf(
                normalizedAsset.copy(cachedPath = "original.mp4", needsTranscode = true)
            )
        )
        manager.openProject(project)

        val result = manager.transcodeAsset("asset-1")

        assertEquals(MediaResult.Success(Unit), result)
        assertEquals("normalized.mp4", manager.state.value.currentProject?.assets?.single()?.cachedPath)
        assertEquals("normalized.mp4", preview.updatedProject?.assets?.single()?.cachedPath)
    }

    private fun createManager(
        previewController: FakePreviewController,
        transcoder: Transcoder = FakeTranscoder()
    ): DefaultMediaSessionManager {
        return DefaultMediaSessionManager(
            importer = FakeImporter(),
            metadataReader = FakeMetadataReader(),
            timelineEditor = FakeTimelineEditor(),
            previewController = previewController,
            transcoder = transcoder,
            renderer = FakeRenderer(),
            audioProcessor = FakeAudioProcessor(),
            cacheManager = FakeCacheManager(),
            diagnostics = FakeDiagnostics(),
            ioDispatcher = dispatcher
        )
    }

    private fun project(): VideoProject {
        return VideoProject(
            id = "project-1",
            name = "Project",
            rootCachePath = "",
            timeline = Timeline(
                tracks = listOf(TimelineTrack(id = "video-main", type = TrackType.Video))
            )
        )
    }

    private fun sampleClip(id: String, start: Long, end: Long): TimelineClip {
        return TimelineClip(
            id = id,
            assetId = "asset-$id",
            assetType = AssetType.Video,
            timelineStartMs = start,
            sourceStartMs = 0L,
            sourceEndMs = end,
            sourceDurationMs = end
        )
    }
}

private class FakeImporter : com.natncompany.media.MediaImporter {
    override suspend fun import(request: ImportRequest): MediaResult<ImportResult> {
        return MediaResult.Success(ImportResult(asset = null, status = ImportStatus.Ok))
    }
}

private class FakeMetadataReader : MetadataReader {
    override suspend fun read(asset: Asset): MediaResult<com.natncompany.media.MetadataResult> {
        return MediaResult.Failure(MediaError.InvalidInput("unused"))
    }

    override suspend fun checkCompatibility(asset: Asset, metadata: MediaMetadata): MediaResult<CompatibilityReport> {
        return MediaResult.Success(CompatibilityReport(isSafe = true, needsTranscode = false))
    }
}

private class FakeTimelineEditor : TimelineEditor {
    override fun addTrack(timeline: Timeline, track: TimelineTrack, index: Int): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun removeTrack(timeline: Timeline, trackId: String): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun reorderTrack(timeline: Timeline, trackId: String, newIndex: Int): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun setTrackEnabled(timeline: Timeline, trackId: String, enabled: Boolean): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun setTrackLocked(timeline: Timeline, trackId: String, locked: Boolean): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun setTrackMuted(timeline: Timeline, trackId: String, muted: Boolean): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun calculateTrackDuration(timeline: Timeline, trackId: String): MediaResult<Long> = MediaResult.Success(0L)
    override fun addClip(timeline: Timeline, trackId: String, clip: TimelineClip): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun removeClip(timeline: Timeline, trackId: String, clipId: String): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun selectClip(timeline: Timeline, clipId: String, selected: Boolean): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun clearSelection(timeline: Timeline): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun groupClips(timeline: Timeline, groupId: String, clipIds: Set<String>): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun ungroupClips(timeline: Timeline, groupId: String): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun validateClipPosition(timeline: Timeline, trackId: String, clip: TimelineClip): MediaResult<TimelineClip> = MediaResult.Success(clip)
    override fun splitClip(timeline: Timeline, trackId: String, clipId: String, playheadMs: Long): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun splitAllAt(timeline: Timeline, playheadMs: Long): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun trimClip(timeline: Timeline, trackId: String, clipId: String, startMs: Long, endMs: Long): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun trimClipStart(timeline: Timeline, trackId: String, clipId: String, newSourceStartMs: Long, ripple: Boolean): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun trimClipEnd(timeline: Timeline, trackId: String, clipId: String, newSourceEndMs: Long, ripple: Boolean): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun moveClip(timeline: Timeline, fromTrackId: String, toTrackId: String, clipId: String, newStartMs: Long, newIndex: Int?): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun duplicateClip(timeline: Timeline, trackId: String, clipId: String, newClipId: String): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun snapClip(timeline: Timeline, trackId: String, clipId: String, desiredStartMs: Long, playheadMs: Long?, thresholdMs: Long, enabled: Boolean): MediaResult<SnapResult> = error("unused")
    override fun calculateDuration(timeline: Timeline): MediaResult<Long> = MediaResult.Success(timeline.durationMs)
    override fun calculateClipVisibleRange(timeline: Timeline, clipId: String): MediaResult<ClipRange> = error("unused")
    override fun calculateSourceRange(timeline: Timeline, clipId: String): MediaResult<SourceRange> = error("unused")
    override fun validateTimeline(timeline: Timeline): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun undo(timeline: Timeline): MediaResult<Timeline> = MediaResult.Success(timeline)
    override fun redo(timeline: Timeline): MediaResult<Timeline> = MediaResult.Success(timeline)
}

private class FakePreviewController : PreviewController {
    private val mutableState = MutableStateFlow(PreviewState())
    var openedProject: VideoProject? = null
    var updatedProject: VideoProject? = null
    override val state: StateFlow<PreviewState> = mutableState
    override suspend fun prepare(project: VideoProject): MediaResult<Unit> = MediaResult.Success(Unit)
    override suspend fun openProject(project: VideoProject): MediaResult<Unit> {
        openedProject = project
        mutableState.value = mutableState.value.copy(isPrepared = true, durationMs = project.timeline.durationMs)
        return MediaResult.Success(Unit)
    }
    override suspend fun setSurface(surface: android.view.Surface?): MediaResult<Unit> = MediaResult.Success(Unit)
    override suspend fun play(): MediaResult<Unit> = MediaResult.Success(Unit)
    override suspend fun pause(): MediaResult<Unit> = MediaResult.Success(Unit)
    override suspend fun seek(positionMs: Long): MediaResult<Unit> = MediaResult.Success(Unit)
    override suspend fun scrub(positionMs: Long): MediaResult<Unit> = MediaResult.Success(Unit)
    override suspend fun updateTimeline(project: VideoProject): MediaResult<Unit> {
        updatedProject = project
        mutableState.value = mutableState.value.copy(durationMs = project.timeline.durationMs)
        return MediaResult.Success(Unit)
    }
    override suspend fun release(): MediaResult<Unit> = MediaResult.Success(Unit)
}

private class FakeTranscoder : Transcoder {
    constructor(updates: Flow<TranscodeUpdate> = flowOf(TranscodeUpdate(progressPercent = 100, completed = true))) {
        this.updates = updates
    }

    private val updates: Flow<TranscodeUpdate>

    override suspend fun shouldTranscode(asset: Asset, metadata: MediaMetadata): MediaResult<Boolean> = MediaResult.Success(false)
    override fun transcode(request: TranscodeRequest): Flow<TranscodeUpdate> = updates
    override suspend fun cancel(jobId: String): MediaResult<Unit> = MediaResult.Success(Unit)
}

private class FakeRenderer : Renderer {
    override fun render(request: RenderRequest): Flow<RenderUpdate> = flowOf(RenderUpdate(progressPercent = 100, completed = true))
    override suspend fun cancel(jobId: String): MediaResult<Unit> = MediaResult.Success(Unit)
    override suspend fun validate(request: RenderRequest): MediaResult<Unit> = MediaResult.Success(Unit)
}

private class FakeAudioProcessor : AudioProcessor {
    override suspend fun analyzeAudio(asset: Asset): MediaResult<AudioInfo> = error("unused")
    override fun applyAudioSettings(clip: TimelineClip, settings: AudioSettings): MediaResult<TimelineClip> = MediaResult.Success(clip)
    override fun createMixPlan(timeline: Timeline): MediaResult<AudioMixPlan> = error("unused")
    override fun mapTimelineToSourceTime(clip: TimelineClip, timelineTimeMs: Long): MediaResult<Long> = error("unused")
    override fun extractWaveform(asset: Asset, config: WaveformConfig): Flow<WaveformProgress> = emptyFlow()
    override fun createWaveformPlaceholder(asset: Asset, samples: Int): MediaResult<WaveformPlaceholder> = error("unused")
}

private class FakeCacheManager : ProjectCacheManager {
    override suspend fun createProjectCache(projectId: String): MediaResult<CacheLayout> = MediaResult.Success(
        CacheLayout(projectRoot = "cache/$projectId", assetsDir = "", transcodedDir = "", previewDir = "", renderDir = "", tempDir = "")
    )
    override suspend fun createSafeFileName(projectId: String, originalName: String, extension: String): MediaResult<String> = error("unused")
    override suspend fun copyToAssets(projectId: String, source: String): MediaResult<CachedAssetFile> = error("unused")
    override suspend fun createTranscodedFile(projectId: String, extension: String): MediaResult<String> = error("unused")
    override suspend fun createRenderOutput(projectId: String, fileName: String): MediaResult<String> = error("unused")
    override suspend fun clearTemp(projectId: String): MediaResult<Unit> = MediaResult.Success(Unit)
    override suspend fun deleteUnusedAssets(projectId: String, usedAssetIds: Set<String>): MediaResult<Int> = error("unused")
    override suspend fun deleteProjectCache(projectId: String): MediaResult<Unit> = MediaResult.Success(Unit)
    override suspend fun getCacheSize(projectId: String): MediaResult<CacheStats> = error("unused")
}

private class FakeDiagnostics : MediaDiagnostics {
    override fun getVideoEncoders() = MediaResult.Success(emptyList<com.natncompany.media.CodecDescriptor>())
    override fun getVideoDecoders() = MediaResult.Success(emptyList<com.natncompany.media.CodecDescriptor>())
    override fun getAudioEncoders() = MediaResult.Success(emptyList<com.natncompany.media.CodecDescriptor>())
    override fun getAudioDecoders() = MediaResult.Success(emptyList<com.natncompany.media.CodecDescriptor>())
    override fun canDecode(mimeType: String) = MediaResult.Success(true)
    override fun canEncode(mimeType: String) = MediaResult.Success(true)
    override fun buildDebugReport(asset: Asset, metadata: MediaMetadata?, compatibility: CompatibilityReport?) = error("unused")
}
