package com.natncompany.media.transcode

import com.natncompany.media.Asset
import com.natncompany.media.AssetType
import com.natncompany.media.CacheLayout
import com.natncompany.media.CachedAssetFile
import com.natncompany.media.CacheStats
import com.natncompany.media.CompatibilityReport
import com.natncompany.media.ImportResult
import com.natncompany.media.MediaMetadata
import com.natncompany.media.MediaResult
import com.natncompany.media.MetadataReader
import com.natncompany.media.ProjectCacheManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InternalTranscoderDecisionTest {
    @Test
    fun `shouldTranscode returns true when asset already flagged`() = runTest {
        val transcoder = createTranscoder(
            compatibility = MediaResult.Success(
                CompatibilityReport(
                    isSafe = true,
                    needsTranscode = false
                )
            )
        )

        val result = transcoder.shouldTranscode(
            asset = asset(needsTranscode = true),
            metadata = metadata()
        ) as MediaResult.Success

        assertEquals(true, result.value)
    }

    @Test
    fun `shouldTranscode returns true when compatibility requires it`() = runTest {
        val transcoder = createTranscoder(
            compatibility = MediaResult.Success(
                CompatibilityReport(
                    isSafe = false,
                    needsTranscode = true,
                    reasons = listOf("codec unsupported")
                )
            )
        )

        val result = transcoder.shouldTranscode(
            asset = asset(needsTranscode = false),
            metadata = metadata()
        ) as MediaResult.Success

        assertEquals(true, result.value)
    }

    @Test
    fun `shouldTranscode returns false when asset and compatibility are safe`() = runTest {
        val transcoder = createTranscoder(
            compatibility = MediaResult.Success(
                CompatibilityReport(
                    isSafe = true,
                    needsTranscode = false
                )
            )
        )

        val result = transcoder.shouldTranscode(
            asset = asset(needsTranscode = false),
            metadata = metadata()
        ) as MediaResult.Success

        assertEquals(false, result.value)
    }

    private fun createTranscoder(compatibility: MediaResult<CompatibilityReport>): InternalTranscoder {
        return InternalTranscoder(
            cacheManager = FakeCacheManager,
            metadataReader = FakeMetadataReader(compatibility = compatibility)
        )
    }

    private fun asset(needsTranscode: Boolean): Asset {
        return Asset(
            id = "asset-1",
            sourceUri = "file://video.mp4",
            cachedPath = "video.mp4",
            displayName = "video.mp4",
            type = AssetType.Video,
            durationMs = 1_000L,
            mimeType = "video/mp4",
            needsTranscode = needsTranscode
        )
    }

    private fun metadata(): MediaMetadata {
        return MediaMetadata(
            durationMs = 1_000L,
            width = 1280,
            height = 720,
            rotationDegrees = 0,
            mimeType = "video/mp4",
            bitrate = 1_000_000,
            fps = 30f,
            hasVideoTrack = true,
            hasAudioTrack = true,
            isVariableFrameRate = false,
            audioSampleRate = 48_000,
            audioChannels = 2
        )
    }

    private class FakeMetadataReader(
        private val compatibility: MediaResult<CompatibilityReport>
    ) : MetadataReader {
        override suspend fun read(asset: Asset): MediaResult<com.natncompany.media.MetadataResult> {
            error("Not used in shouldTranscode tests")
        }

        override suspend fun checkCompatibility(asset: Asset, metadata: MediaMetadata): MediaResult<CompatibilityReport> {
            return compatibility
        }
    }

    private object FakeCacheManager : ProjectCacheManager {
        override suspend fun createProjectCache(projectId: String): MediaResult<CacheLayout> = error("Not used")
        override suspend fun createSafeFileName(projectId: String, originalName: String, extension: String): MediaResult<String> = error("Not used")
        override suspend fun copyToAssets(projectId: String, source: String): MediaResult<CachedAssetFile> = error("Not used")
        override suspend fun createTranscodedFile(projectId: String, extension: String): MediaResult<String> = error("Not used")
        override suspend fun createRenderOutput(projectId: String, fileName: String): MediaResult<String> = error("Not used")
        override suspend fun clearTemp(projectId: String): MediaResult<Unit> = error("Not used")
        override suspend fun deleteUnusedAssets(projectId: String, usedAssetIds: Set<String>): MediaResult<Int> = error("Not used")
        override suspend fun deleteProjectCache(projectId: String): MediaResult<Unit> = error("Not used")
        override suspend fun getCacheSize(projectId: String): MediaResult<CacheStats> = error("Not used")
    }
}
