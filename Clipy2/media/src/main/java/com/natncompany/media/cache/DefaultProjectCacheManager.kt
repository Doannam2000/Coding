package com.natncompany.media.cache

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.natncompany.media.CacheLayout
import com.natncompany.media.CacheStats
import com.natncompany.media.CachedAssetFile
import com.natncompany.media.MediaError
import com.natncompany.media.MediaResult
import com.natncompany.media.ProjectCacheManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.coroutineContext

class DefaultProjectCacheManager(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProjectCacheManager {
    private val appContext = context.applicationContext
    private val cacheRoot = File(appContext.cacheDir, "clipy-media")

    override suspend fun createProjectCache(projectId: String): MediaResult<CacheLayout> = withContext(ioDispatcher) {
        if (projectId.isBlank()) {
            return@withContext failure("Project id is required")
        }
        val root = projectRoot(projectId)
        val layout = CacheLayout(
            projectRoot = root.absolutePath,
            assetsDir = File(root, ASSETS_DIR).absolutePath,
            transcodedDir = File(root, TRANSCODED_DIR).absolutePath,
            previewDir = File(root, PREVIEW_DIR).absolutePath,
            renderDir = File(root, RENDER_DIR).absolutePath,
            tempDir = File(root, TEMP_DIR).absolutePath
        )
        runCatching {
            listOf(
                layout.projectRoot,
                layout.assetsDir,
                layout.transcodedDir,
                layout.previewDir,
                layout.renderDir,
                layout.tempDir
            ).forEach { path ->
                File(path).mkdirs()
            }
            MediaResult.Success(layout)
        }.getOrElse { MediaResult.Failure(MediaError.FileAccess(it.message ?: "Unable to create cache")) }
    }

    override suspend fun createSafeFileName(
        projectId: String,
        originalName: String,
        extension: String
    ): MediaResult<String> = withContext(ioDispatcher) {
        if (projectId.isBlank()) {
            return@withContext failure("Project id is required")
        }
        val safeBase = sanitizeName(originalName.substringBeforeLast('.').ifBlank { "media" })
        val safeExtension = sanitizeExtension(extension.ifBlank { originalName.substringAfterLast('.', "") })
        val suffix = UUID.randomUUID().toString().take(8)
        MediaResult.Success(
            if (safeExtension.isBlank()) "$safeBase-$suffix" else "$safeBase-$suffix.$safeExtension"
        )
    }

    override suspend fun copyToAssets(projectId: String, source: String): MediaResult<CachedAssetFile> = withContext(ioDispatcher) {
        val layout = when (val result = createProjectCache(projectId)) {
            is MediaResult.Success -> result.value
            is MediaResult.Failure -> return@withContext result
        }
        if (source.isBlank()) {
            return@withContext failure("Source path is required")
        }

        val sourceInfo = resolveSourceInfo(source)
        val extension = sourceInfo.extension.ifBlank {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(sourceInfo.mimeType).orEmpty()
        }
        val assetId = "asset_${UUID.randomUUID()}"
        val fileName = when (val safe = createSafeFileName(projectId, sourceInfo.displayName, extension)) {
            is MediaResult.Success -> safe.value
            is MediaResult.Failure -> return@withContext safe
        }
        val destination = File(layout.assetsDir, "${assetId}_$fileName")
        runCatching {
            destination.parentFile?.mkdirs()
            openInputStream(source)?.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                }
            } ?: return@withContext failure("Unable to open source media")
            MediaResult.Success(
                CachedAssetFile(
                    projectId = projectId,
                    assetId = assetId,
                    displayName = sourceInfo.displayName,
                    filePath = destination.absolutePath
                )
            )
        }.getOrElse { MediaResult.Failure(MediaError.FileAccess(it.message ?: "Unable to copy media")) }
    }

    override suspend fun createTranscodedFile(projectId: String, extension: String): MediaResult<String> =
        createGeneratedFile(projectId, TRANSCODED_DIR, "transcoded", extension)

    override suspend fun createRenderOutput(projectId: String, fileName: String): MediaResult<String> = withContext(ioDispatcher) {
        val layout = when (val result = createProjectCache(projectId)) {
            is MediaResult.Success -> result.value
            is MediaResult.Failure -> return@withContext result
        }
        val safeName = sanitizeName(fileName.substringBeforeLast('.').ifBlank { "render" })
        val extension = sanitizeExtension(fileName.substringAfterLast('.', "mp4").ifBlank { "mp4" })
        MediaResult.Success(File(layout.renderDir, "$safeName.$extension").absolutePath)
    }

    override suspend fun clearTemp(projectId: String): MediaResult<Unit> = withContext(ioDispatcher) {
        val temp = File(projectRoot(projectId), TEMP_DIR)
        runCatching {
            temp.deleteRecursively()
            temp.mkdirs()
            MediaResult.Success(Unit)
        }.getOrElse { MediaResult.Failure(MediaError.FileAccess(it.message ?: "Unable to clear temp files")) }
    }

    override suspend fun deleteUnusedAssets(projectId: String, usedAssetIds: Set<String>): MediaResult<Int> = withContext(ioDispatcher) {
        val assetsDir = File(projectRoot(projectId), ASSETS_DIR)
        if (!assetsDir.exists()) {
            return@withContext MediaResult.Success(0)
        }
        runCatching {
            var deleted = 0
            assetsDir.listFiles().orEmpty().forEach { file ->
                coroutineContext.ensureActive()
                val keep = usedAssetIds.any { file.name.contains(it, ignoreCase = true) }
                if (!keep && file.isFile && file.delete()) {
                    deleted++
                }
            }
            MediaResult.Success(deleted)
        }.getOrElse { MediaResult.Failure(MediaError.FileAccess(it.message ?: "Unable to delete unused assets")) }
    }

    override suspend fun deleteProjectCache(projectId: String): MediaResult<Unit> = withContext(ioDispatcher) {
        runCatching {
            projectRoot(projectId).deleteRecursively()
            MediaResult.Success(Unit)
        }.getOrElse { MediaResult.Failure(MediaError.FileAccess(it.message ?: "Unable to delete project cache")) }
    }

    override suspend fun getCacheSize(projectId: String): MediaResult<CacheStats> = withContext(ioDispatcher) {
        val root = projectRoot(projectId)
        runCatching {
            val assets = File(root, ASSETS_DIR).sizeBytes()
            val transcoded = File(root, TRANSCODED_DIR).sizeBytes()
            val preview = File(root, PREVIEW_DIR).sizeBytes()
            val render = File(root, RENDER_DIR).sizeBytes()
            val temp = File(root, TEMP_DIR).sizeBytes()
            MediaResult.Success(
                CacheStats(
                    projectId = projectId,
                    totalBytes = assets + transcoded + preview + render + temp,
                    assetsBytes = assets,
                    transcodedBytes = transcoded,
                    previewBytes = preview,
                    renderBytes = render,
                    tempBytes = temp
                )
            )
        }.getOrElse { MediaResult.Failure(MediaError.FileAccess(it.message ?: "Unable to read cache size")) }
    }

    private suspend fun createGeneratedFile(
        projectId: String,
        directory: String,
        prefix: String,
        extension: String
    ): MediaResult<String> = withContext(ioDispatcher) {
        val layout = when (val result = createProjectCache(projectId)) {
            is MediaResult.Success -> result.value
            is MediaResult.Failure -> return@withContext result
        }
        val targetDir = when (directory) {
            TRANSCODED_DIR -> layout.transcodedDir
            RENDER_DIR -> layout.renderDir
            PREVIEW_DIR -> layout.previewDir
            else -> layout.tempDir
        }
        val ext = sanitizeExtension(extension.ifBlank { "mp4" })
        MediaResult.Success(File(targetDir, "$prefix-${UUID.randomUUID()}.$ext").absolutePath)
    }

    private fun projectRoot(projectId: String): File = File(File(cacheRoot, "projects"), sanitizeName(projectId))

    private fun openInputStream(source: String) = when {
        source.startsWith("content://") || source.startsWith("file://") ->
            appContext.contentResolver.openInputStream(Uri.parse(source))
        else -> File(source).takeIf { it.exists() }?.inputStream()
    }

    private fun resolveSourceInfo(source: String): SourceInfo {
        if (source.startsWith("content://") || source.startsWith("file://")) {
            val uri = Uri.parse(source)
            val displayName = queryDisplayName(uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "media"
            val mimeType = appContext.contentResolver.getType(uri)
            return SourceInfo(
                displayName = displayName,
                mimeType = mimeType,
                extension = displayName.substringAfterLast('.', "")
            )
        }
        val file = File(source)
        return SourceInfo(
            displayName = file.name.ifBlank { "media" },
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase(Locale.US)),
            extension = file.extension
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        return appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }

    private fun sanitizeName(value: String): String {
        return value
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-', '.', '_')
            .ifBlank { "media" }
    }

    private fun sanitizeExtension(value: String): String {
        return value.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "").take(8)
    }

    private fun File.sizeBytes(): Long {
        if (!exists()) return 0L
        if (isFile) return length()
        return walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun failure(message: String): MediaResult.Failure = MediaResult.Failure(MediaError.FileAccess(message))

    private data class SourceInfo(
        val displayName: String,
        val mimeType: String?,
        val extension: String
    )

    private companion object {
        const val ASSETS_DIR = "assets"
        const val TRANSCODED_DIR = "transcoded"
        const val PREVIEW_DIR = "preview"
        const val RENDER_DIR = "render"
        const val TEMP_DIR = "temp"
        const val DEFAULT_BUFFER_SIZE = 64 * 1024
    }
}
