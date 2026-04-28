package com.natncompany.media.transcode
import com.natncompany.media.Asset
import com.natncompany.media.AssetType
import com.natncompany.media.MediaMetadata
import com.natncompany.media.MediaError
import com.natncompany.media.MediaResult
import com.natncompany.media.MetadataReader
import com.natncompany.media.ProjectCacheManager
import com.natncompany.media.TranscodeRequest
import com.natncompany.media.TranscodeUpdate
import com.natncompany.media.Transcoder
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

class InternalTranscoder(
    private val cacheManager: ProjectCacheManager,
    private val metadataReader: MetadataReader,
    private val ffmpegBackend: FfmpegBackend = UnavailableFfmpegBackend(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : Transcoder {
    private val cancelledProjects = ConcurrentHashMap.newKeySet<String>()

    override suspend fun shouldTranscode(asset: Asset, metadata: MediaMetadata): MediaResult<Boolean> {
        return when (val compatibility = metadataReader.checkCompatibility(asset, metadata)) {
            is MediaResult.Success -> MediaResult.Success(shouldTranscode(asset.needsTranscode, compatibility.value.needsTranscode))
            is MediaResult.Failure -> compatibility
        }
    }

    override fun transcode(request: TranscodeRequest): Flow<TranscodeUpdate> {
        var outputPath: String? = null
        return flow {
            validate(request)?.let {
                emit(TranscodeUpdate(progressPercent = 100, completed = true, error = it))
                return@flow
            }
            cancelledProjects.remove(request.projectId)
            cancelledProjects.remove(request.jobId)
            emit(TranscodeUpdate(progressPercent = 0))

            outputPath = when (val path = cacheManager.createTranscodedFile(request.projectId, "mp4")) {
                is MediaResult.Success -> path.value
                is MediaResult.Failure -> {
                    emit(TranscodeUpdate(progressPercent = 100, completed = true, error = path.error))
                    return@flow
                }
            }
            val targetPath = outputPath ?: return@flow

            val compatibility = metadataReader.read(request.asset)
            val canPassthrough = request.asset.type == AssetType.Video &&
                request.asset.cachedPath.endsWith(".mp4", ignoreCase = true) &&
                request.asset.needsTranscode.not() &&
                compatibility is MediaResult.Success &&
                compatibility.value.compatibility.isSafe

            if (canPassthrough) {
                copyWithProgress(File(request.asset.cachedPath), File(targetPath)) { progress ->
                    emit(TranscodeUpdate(progressPercent = progress.coerceIn(1, 99)))
                }
            } else {
                ffmpegBackend.transcode(request, targetPath).collect { progress ->
                    coroutineContext.ensureActive()
                    if (request.projectId in cancelledProjects || request.jobId in cancelledProjects) {
                        throw CancellationException("Transcode cancelled")
                    }
                    emit(TranscodeUpdate(progressPercent = progress.coerceIn(1, 99)))
                }
            }

            val outputAsset = request.asset.copy(
                cachedPath = targetPath,
                mimeType = "video/mp4",
                needsTranscode = false
            )
            emit(TranscodeUpdate(progressPercent = 100, asset = outputAsset, completed = true))
        }.catch { throwable ->
            outputPath?.let { File(it).delete() }
            if (throwable is CancellationException) {
                emit(TranscodeUpdate(progressPercent = 100, completed = true, error = MediaError.Cancelled()))
            } else {
                emit(TranscodeUpdate(progressPercent = 100, completed = true, error = throwable.toTranscodeError()))
            }
        }.flowOn(ioDispatcher)
    }

    override suspend fun cancel(jobId: String): MediaResult<Unit> = withContext(ioDispatcher) {
        cancelledProjects += jobId
        ffmpegBackend.cancel(jobId)
        MediaResult.Success(Unit)
    }

    private fun validate(request: TranscodeRequest): MediaError? {
        if (request.projectId.isBlank()) return MediaError.InvalidInput("Project id is required")
        if (!File(request.asset.cachedPath).exists()) return MediaError.FileAccess("Asset file does not exist")
        if (request.maxWidth <= 0 || request.maxHeight <= 0 || request.maxFps <= 0) {
            return MediaError.InvalidInput("Transcode bounds must be greater than zero")
        }
        return null
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

    private fun Throwable.toTranscodeError(): MediaError {
        return when (this) {
            is FfmpegUnavailableException -> MediaError.BackendUnavailable(message ?: "FFmpeg backend unavailable")
            else -> MediaError.ExceptionError(this)
        }
    }

    private companion object {
        const val DEFAULT_BUFFER_SIZE = 64 * 1024

        fun shouldTranscode(assetNeedsTranscode: Boolean, compatibilityNeedsTranscode: Boolean): Boolean {
            return assetNeedsTranscode || compatibilityNeedsTranscode
        }
    }
}
