package com.natncompany.media.importer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.natncompany.media.Asset
import com.natncompany.media.AssetType
import com.natncompany.media.CacheLayout
import com.natncompany.media.CachedAssetFile
import com.natncompany.media.CacheStats
import com.natncompany.media.Compatibility
import com.natncompany.media.ImportRequest
import com.natncompany.media.ImportStatus
import com.natncompany.media.MediaError
import com.natncompany.media.MediaMetadata
import com.natncompany.media.MediaResult
import com.natncompany.media.MetadataReader
import com.natncompany.media.MetadataResult
import com.natncompany.media.ProjectCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DefaultMediaImporterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `import marks unsupported extension for transcode`() = runTest {
        val tempFile = File.createTempFile("clip", ".mkv").apply { writeText("fake") }
        val importer = DefaultMediaImporter(
            context = context,
            cacheManager = FakeImporterCacheManager(tempFile),
            metadataReader = FakeImporterMetadataReader(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = importer.import(ImportRequest(projectId = "project-1", filePath = tempFile.absolutePath)) as MediaResult.Success
        val asset = result.value.asset!!

        assertEquals(ImportStatus.Warning, result.value.status)
        assertTrue(asset.needsTranscode)
        assertTrue(result.value.warnings.any { it.contains("Unsupported extension") })
    }

    @Test
    fun `import returns file access error for missing file`() = runTest {
        val importer = DefaultMediaImporter(
            context = context,
            cacheManager = FakeImporterCacheManager(File("missing.mp4")),
            metadataReader = FakeImporterMetadataReader(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = importer.import(ImportRequest(projectId = "project-1", filePath = "D:/missing/file.mp4")) as MediaResult.Failure

        assertTrue(result.error is MediaError.FileAccess)
    }

    @Test
    fun `import propagates metadata failure as warning and transcode requirement`() = runTest {
        val tempFile = File.createTempFile("clip", ".mp4").apply { writeText("fake") }
        val importer = DefaultMediaImporter(
            context = context,
            cacheManager = FakeImporterCacheManager(tempFile),
            metadataReader = object : MetadataReader {
                override suspend fun read(asset: Asset): MediaResult<MetadataResult> {
                    return MediaResult.Failure(MediaError.CorruptMedia("cannot inspect metadata"))
                }
            },
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = importer.import(ImportRequest(projectId = "project-1", filePath = tempFile.absolutePath)) as MediaResult.Success
        val asset = result.value.asset!!

        assertEquals(ImportStatus.Warning, result.value.status)
        assertTrue(asset.needsTranscode)
        assertTrue(result.value.warnings.any { it.contains("cannot inspect metadata") })
    }
}

private class FakeImporterMetadataReader : MetadataReader {
    override suspend fun read(asset: Asset): MediaResult<MetadataResult> {
        return MediaResult.Success(
            MetadataResult(
                metadata = MediaMetadata(
                    durationMs = 1_000L,
                    width = 1280,
                    height = 720,
                    rotationDegrees = 0,
                    mimeType = asset.mimeType,
                    bitrate = 1_000_000,
                    fps = 30f,
                    hasVideoTrack = asset.type == AssetType.Video,
                    hasAudioTrack = asset.type != AssetType.Image,
                    isVariableFrameRate = false,
                    audioSampleRate = 48_000,
                    audioChannels = 2
                ),
                compatibility = Compatibility(
                    isSafe = !asset.needsTranscode,
                    needsTranscode = asset.needsTranscode
                )
            )
        )
    }
}

private class FakeImporterCacheManager(
    private val sourceFile: File
) : ProjectCacheManager {
    override suspend fun createProjectCache(projectId: String): MediaResult<CacheLayout> = error("unused")
    override suspend fun createSafeFileName(projectId: String, originalName: String, extension: String): MediaResult<String> = error("unused")
    override suspend fun copyToAssets(projectId: String, source: String): MediaResult<CachedAssetFile> {
        return MediaResult.Success(
            CachedAssetFile(
                projectId = projectId,
                assetId = "asset-1",
                displayName = sourceFile.name,
                filePath = sourceFile.absolutePath
            )
        )
    }
    override suspend fun createTranscodedFile(projectId: String, extension: String): MediaResult<String> = error("unused")
    override suspend fun createRenderOutput(projectId: String, fileName: String): MediaResult<String> = error("unused")
    override suspend fun clearTemp(projectId: String): MediaResult<Unit> = error("unused")
    override suspend fun deleteUnusedAssets(projectId: String, usedAssetIds: Set<String>): MediaResult<Int> = error("unused")
    override suspend fun deleteProjectCache(projectId: String): MediaResult<Unit> = error("unused")
    override suspend fun getCacheSize(projectId: String): MediaResult<CacheStats> = error("unused")
}
