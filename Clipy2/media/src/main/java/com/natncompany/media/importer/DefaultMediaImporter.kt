package com.natncompany.media.importer

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.natncompany.media.Asset
import com.natncompany.media.AssetType
import com.natncompany.media.ImportRequest
import com.natncompany.media.ImportResult
import com.natncompany.media.ImportStatus
import com.natncompany.media.MediaError
import com.natncompany.media.MediaImporter
import com.natncompany.media.MediaResult
import com.natncompany.media.MetadataReader
import com.natncompany.media.ProjectCacheManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class DefaultMediaImporter(
    context: Context,
    private val cacheManager: ProjectCacheManager,
    private val metadataReader: MetadataReader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MediaImporter {
    private val appContext = context.applicationContext

    override suspend fun import(request: ImportRequest): MediaResult<ImportResult> = withContext(ioDispatcher) {
        if (request.projectId.isBlank()) {
            return@withContext failure("Project id is required")
        }
        val source = request.uri?.toString() ?: request.filePath
        if (source.isNullOrBlank()) {
            return@withContext failure("Uri or file path is required")
        }
        validateReadable(request.uri, request.filePath)?.let { return@withContext it }

        val sourceInfo = inspectSource(request.uri, request.filePath)
        val copy = when (val result = cacheManager.copyToAssets(request.projectId, source)) {
            is MediaResult.Success -> result.value
            is MediaResult.Failure -> return@withContext result
        }

        val type = sourceInfo.assetType
        val extensionSupported = sourceInfo.extension in SUPPORTED_EXTENSIONS
        val mimeSupported = sourceInfo.mimeType == null || isSupportedMime(sourceInfo.mimeType)
        val initiallyNeedsTranscode = type == AssetType.Unknown || !extensionSupported || !mimeSupported
        val provisionalAsset = Asset(
            id = copy.assetId,
            sourceUri = source,
            cachedPath = copy.filePath,
            displayName = copy.displayName,
            type = type,
            mimeType = sourceInfo.mimeType,
            fileSizeBytes = File(copy.filePath).length(),
            needsTranscode = initiallyNeedsTranscode
        )

        val metadataResult = metadataReader.read(provisionalAsset)
        val asset = when (metadataResult) {
            is MediaResult.Success -> provisionalAsset.copy(
                durationMs = metadataResult.value.metadata.durationMs,
                mimeType = metadataResult.value.metadata.mimeType ?: provisionalAsset.mimeType,
                needsTranscode = initiallyNeedsTranscode || metadataResult.value.compatibility.needsTranscode
            )
            is MediaResult.Failure -> provisionalAsset.copy(needsTranscode = true)
        }

        val warnings = buildList {
            if (!extensionSupported) add("Unsupported extension .${sourceInfo.extension.ifBlank { "unknown" }}")
            if (!mimeSupported) add("Unsupported MIME ${sourceInfo.mimeType}")
            if (metadataResult is MediaResult.Failure) add(metadataResult.error.message)
            if (asset.needsTranscode) add("Asset should be normalized before render")
        }.distinct()

        MediaResult.Success(
            ImportResult(
                asset = asset,
                status = if (warnings.isEmpty()) ImportStatus.Ok else ImportStatus.Warning,
                warnings = warnings
            )
        )
    }

    override fun importBatch(inputs: List<com.natncompany.media.MediaImportInput>, projectId: String): Flow<com.natncompany.media.ImportBatchProgress> = flow {
        val imported = mutableListOf<Asset>()
        val failures = mutableListOf<com.natncompany.media.ImportFailure>()
        emit(com.natncompany.media.ImportBatchProgress(total = inputs.size, completed = 0, succeeded = 0, failed = 0))
        inputs.forEachIndexed { index, input ->
            val result = import(ImportRequest(projectId = projectId, uri = input.uri, filePath = input.filePath))
            when (result) {
                is MediaResult.Success -> {
                    val asset = result.value.asset
                    if (asset != null) {
                        imported += asset
                        emit(
                            com.natncompany.media.ImportBatchProgress(
                                total = inputs.size,
                                completed = index + 1,
                                succeeded = imported.size,
                                failed = failures.size,
                                currentInput = input,
                                latestAsset = asset
                            )
                        )
                    } else {
                        val failure = com.natncompany.media.ImportFailure(input, MediaError.InvalidInput("Import returned no asset"))
                        failures += failure
                        emit(
                            com.natncompany.media.ImportBatchProgress(
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
                    val failure = com.natncompany.media.ImportFailure(input, result.error)
                    failures += failure
                    emit(
                        com.natncompany.media.ImportBatchProgress(
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
        }
        emit(
            com.natncompany.media.ImportBatchProgress(
                total = inputs.size,
                completed = inputs.size,
                succeeded = imported.size,
                failed = failures.size,
                result = com.natncompany.media.ImportBatchResult(imported = imported, failures = failures)
            )
        )
    }.flowOn(ioDispatcher)

    private fun validateReadable(uri: Uri?, path: String?): MediaResult.Failure? {
        if (path != null) {
            val file = File(path)
            if (!file.exists() || !file.isFile) {
                return MediaResult.Failure(MediaError.FileAccess("Source file does not exist: $path"))
            }
            if (!file.canRead()) {
                return MediaResult.Failure(MediaError.FileAccess("Source file is not readable: $path"))
            }
        }
        if (uri != null) {
            return try {
                appContext.contentResolver.openInputStream(uri)?.close()
                null
            } catch (throwable: Throwable) {
                MediaResult.Failure(MediaError.FileAccess(throwable.message ?: "Source Uri is not readable"))
            }
        }
        return null
    }

    private fun inspectSource(uri: Uri?, path: String?): SourceInfo {
        val mimeType = when {
            uri != null -> appContext.contentResolver.getType(uri)
            path != null -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(File(path).extension.lowercase(Locale.US))
            else -> null
        }
        val extension = when {
            path != null -> File(path).extension.lowercase(Locale.US)
            uri != null -> uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase(Locale.US).orEmpty()
            else -> ""
        }
        val assetType = assetTypeFor(mimeType, extension)
        return SourceInfo(extension, mimeType, assetType)
    }

    private fun assetTypeFor(mimeType: String?, extension: String): AssetType {
        return when {
            mimeType?.startsWith("video/") == true || extension in VIDEO_EXTENSIONS -> AssetType.Video
            mimeType?.startsWith("audio/") == true || extension in AUDIO_EXTENSIONS -> AssetType.Audio
            mimeType?.startsWith("image/") == true || extension in IMAGE_EXTENSIONS -> AssetType.Image
            else -> AssetType.Unknown
        }
    }

    private fun isSupportedMime(mimeType: String): Boolean {
        return mimeType.startsWith("video/") ||
            mimeType.startsWith("audio/") ||
            mimeType in IMAGE_MIMES
    }

    private fun failure(message: String): MediaResult.Failure {
        return MediaResult.Failure(MediaError.InvalidInput(message))
    }

    private data class SourceInfo(
        val extension: String,
        val mimeType: String?,
        val assetType: AssetType
    )

    private companion object {
        val VIDEO_EXTENSIONS = setOf("mp4", "mov", "m4v")
        val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav")
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
        val SUPPORTED_EXTENSIONS = VIDEO_EXTENSIONS + AUDIO_EXTENSIONS + IMAGE_EXTENSIONS
        val IMAGE_MIMES = setOf("image/jpeg", "image/png", "image/webp")
    }
}
