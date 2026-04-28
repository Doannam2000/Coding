package com.natncompany.media.render
import com.natncompany.media.AssetType
import com.natncompany.media.MediaError
import com.natncompany.media.MediaResult
import com.natncompany.media.ProjectCacheManager
import com.natncompany.media.RenderConfig
import com.natncompany.media.RenderRequest
import com.natncompany.media.RenderUpdate
import com.natncompany.media.Renderer
import com.natncompany.media.TimelineClip
import com.natncompany.media.transcode.FfmpegBackend
import com.natncompany.media.transcode.FfmpegUnavailableException
import com.natncompany.media.transcode.UnavailableFfmpegBackend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

class InternalRenderer(
    private val cacheManager: ProjectCacheManager,
    private val ffmpegBackend: FfmpegBackend = UnavailableFfmpegBackend(),
    private val gpuImageBridge: GpuImageRenderBridge = GpuImageRenderBridge(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : Renderer {
    private val cancelledProjects = ConcurrentHashMap.newKeySet<String>()

    override fun render(project: com.natncompany.media.VideoProject, timeline: com.natncompany.media.Timeline, config: RenderConfig): Flow<RenderUpdate> {
        return render(
            RenderRequest(
                project = project.copy(timeline = timeline),
                outputFileName = config.outputFileName,
                timeline = timeline
            )
        )
    }

    override fun render(request: RenderRequest): Flow<RenderUpdate> {
        var outputPath: String? = null
        return flow {
        when (val validation = validate(request)) {
            is MediaResult.Failure -> {
                emit(RenderUpdate(progressPercent = 100, completed = true, error = validation.error))
                return@flow
            }
            is MediaResult.Success -> Unit
        }

        cancelledProjects.remove(request.project.id)
        cancelledProjects.remove(request.jobId)
        emit(RenderUpdate(progressPercent = 0))

        outputPath = when (val result = cacheManager.createRenderOutput(request.project.id, request.outputFileName)) {
            is MediaResult.Success -> result.value
            is MediaResult.Failure -> {
                emit(RenderUpdate(progressPercent = 100, completed = true, error = result.error))
                return@flow
            }
        }
        val targetPath = outputPath ?: return@flow

        val passthroughAsset = singlePassthroughAsset(request)
        if (passthroughAsset != null) {
            copyWithProgress(File(passthroughAsset.cachedPath), File(targetPath)) { progress ->
                emit(RenderUpdate(progressPercent = progress.coerceIn(1, 99)))
            }
        } else {
            ffmpegBackend.render(request, targetPath).collect { progress ->
                coroutineContext.ensureActive()
                if (request.project.id in cancelledProjects || request.jobId in cancelledProjects) {
                    throw CancellationException("Render cancelled")
                }
                emit(RenderUpdate(progressPercent = progress.coerceIn(1, 99)))
            }
        }

        emit(RenderUpdate(progressPercent = 100, outputPath = targetPath, completed = true))
    }.catch { throwable ->
        outputPath?.let { File(it).delete() }
        if (throwable is CancellationException) {
            emit(RenderUpdate(progressPercent = 100, completed = true, error = MediaError.Cancelled()))
        } else {
            emit(RenderUpdate(progressPercent = 100, completed = true, error = throwable.toRenderError()))
        }
    }.flowOn(ioDispatcher)
    }

    override suspend fun cancel(jobId: String): MediaResult<Unit> = withContext(ioDispatcher) {
        cancelledProjects += jobId
        ffmpegBackend.cancel(jobId)
        MediaResult.Success(Unit)
    }

    override suspend fun validate(request: RenderRequest): MediaResult<Unit> = withContext(ioDispatcher) {
        if (request.project.id.isBlank()) {
            return@withContext failure("Project id is required")
        }
        if (request.outputFileName.isBlank()) {
            return@withContext failure("Output file name is required")
        }
        if (request.timeline.tracks.isEmpty()) {
            return@withContext failure("Timeline must contain at least one track")
        }
        val enabledVisualClips = request.timeline.tracks
            .filter { it.isEnabled && it.type != com.natncompany.media.TrackType.Audio }
            .flatMap { it.clips }
        if (enabledVisualClips.isEmpty()) {
            val enabledAudioOnly = request.timeline.tracks.any { it.isEnabled && it.type == com.natncompany.media.TrackType.Audio && it.clips.isNotEmpty() }
            if (enabledAudioOnly) {
                return@withContext failure("Audio-only timelines are not renderable yet")
            }
        }
        val clipIds = mutableSetOf<String>()
        for (track in request.timeline.tracks) {
            if (track.id.isBlank()) return@withContext failure("Track id is required")
            for (clip in track.clips) {
                validateClip(request, clip)?.let { return@withContext MediaResult.Failure(it) }
                if (!clipIds.add(clip.id)) return@withContext failure("Duplicate clip id ${clip.id}")
            }
            if (!track.allowOverlap) {
                val ordered = track.clips.sortedBy { it.timelineStartMs }
                ordered.zipWithNext().firstOrNull { (left, right) -> right.timelineStartMs < left.timelineEndMs }?.let {
                    return@withContext failure("Track ${track.id} contains overlapping clips")
                }
            }
        }
        MediaResult.Success(Unit)
    }

    override suspend fun validateBeforeRender(
        project: com.natncompany.media.VideoProject,
        timeline: com.natncompany.media.Timeline,
        config: RenderConfig
    ): MediaResult<Unit> {
        return validate(
            RenderRequest(
                project = project.copy(timeline = timeline),
                outputFileName = config.outputFileName,
                timeline = timeline
            )
        )
    }

    override fun estimateRenderTime(
        project: com.natncompany.media.VideoProject,
        timeline: com.natncompany.media.Timeline,
        config: RenderConfig
    ): MediaResult<Long> {
        val gpuMultiplier = if (gpuImageBridge.requiresGpu(timeline)) 2L else 1L
        val clipComplexity = timeline.tracks.sumOf { it.clips.size }.coerceAtLeast(1)
        return MediaResult.Success(timeline.durationMs * gpuMultiplier + clipComplexity * 250L)
    }

    private fun validateClip(request: RenderRequest, clip: TimelineClip): MediaError? {
        if (clip.id.isBlank()) return MediaError.Validation("Clip id is required")
        if (clip.visibleDurationMs <= 0L) return MediaError.Validation("Clip ${clip.id} has invalid duration")
        if (clip.sourceStartMs < 0L || clip.sourceEndMs <= clip.sourceStartMs) {
            return MediaError.Validation("Clip ${clip.id} has invalid source range")
        }
        val asset = request.project.assets.firstOrNull { it.id == clip.assetId }
            ?: return MediaError.Validation("Clip ${clip.id} references missing asset ${clip.assetId}")
        if (!File(asset.cachedPath).exists()) {
            return MediaError.FileAccess("Asset file does not exist for clip ${clip.id}")
        }
        return null
    }

    private fun singlePassthroughAsset(request: RenderRequest): com.natncompany.media.Asset? {
        val clips = request.timeline.tracks.flatMap { it.clips }
        if (clips.size != 1) return null
        val clip = clips.single()
        val asset = request.project.assets.firstOrNull { it.id == clip.assetId } ?: return null
        if (asset.type != AssetType.Video) return null
        if (asset.needsTranscode) return null
        if (!asset.cachedPath.endsWith(".mp4", ignoreCase = true)) return null
        if (clip.sourceStartMs != 0L || asset.durationMs != null && clip.sourceEndMs < asset.durationMs) return null
        if (gpuImageBridge.requiresGpu(request.timeline)) return null
        return asset
    }

    private suspend fun copyWithProgress(
        source: File,
        destination: File,
        onProgress: suspend (Int) -> Unit
    ) {
        destination.parentFile?.mkdirs()
        val total = source.length().coerceAtLeast(1L)
        var copied = 0L
        source.inputStream().use { input ->
            destination.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copied += read
                    onProgress(((copied * 100L) / total).toInt())
                }
            }
        }
    }

    private fun Throwable.toRenderError(): MediaError {
        return when (this) {
            is FfmpegUnavailableException -> MediaError.BackendUnavailable(message ?: "FFmpeg backend unavailable")
            else -> MediaError.ExceptionError(this)
        }
    }

    private fun failure(message: String): MediaResult.Failure = MediaResult.Failure(MediaError.Validation(message))

    private companion object {
        const val DEFAULT_BUFFER_SIZE = 64 * 1024
    }
}
