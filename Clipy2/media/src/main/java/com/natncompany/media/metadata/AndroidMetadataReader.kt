package com.natncompany.media.metadata

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import android.webkit.MimeTypeMap
import com.natncompany.media.Asset
import com.natncompany.media.AssetType
import com.natncompany.media.Compatibility
import com.natncompany.media.CompatibilityReport
import com.natncompany.media.MediaError
import com.natncompany.media.MediaMetadata
import com.natncompany.media.MediaResult
import com.natncompany.media.MetadataReader
import com.natncompany.media.MetadataResult
import com.natncompany.media.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class AndroidMetadataReader(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MetadataReader {
    private val appContext = context.applicationContext

    override suspend fun read(asset: Asset): MediaResult<MetadataResult> = withContext(ioDispatcher) {
        if (asset.cachedPath.isBlank()) {
            return@withContext MediaResult.Failure(MediaError.InvalidInput("Cached path is required"))
        }
        val file = File(asset.cachedPath)
        if (!file.exists() || !file.isFile) {
            return@withContext MediaResult.Failure(MediaError.FileAccess("Media file not found: ${asset.cachedPath}"))
        }

        return@withContext when (asset.type) {
            AssetType.Image -> readImage(asset, file)
            AssetType.Video, AssetType.Audio, AssetType.Unknown -> readMedia(asset, file)
        }
    }

    private fun readImage(asset: Asset, file: File): MediaResult<MetadataResult> {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return MediaResult.Failure(MediaError.CorruptMedia("Unable to decode image bounds"))
            }
            val mimeType = asset.mimeType ?: bounds.outMimeType ?: mimeFromPath(file)
            val metadata = MediaMetadata(
                durationMs = asset.durationMs ?: DEFAULT_IMAGE_DURATION_MS,
                width = bounds.outWidth,
                height = bounds.outHeight,
                rotationDegrees = 0,
                mimeType = mimeType,
                bitrate = 0,
                fps = null,
                hasVideoTrack = false,
                hasAudioTrack = false,
                isVariableFrameRate = false,
                audioSampleRate = null,
                audioChannels = null
            )
            MediaResult.Success(MetadataResult(metadata, buildCompatibility(asset, metadata, emptyList())))
        }.getOrElse { MediaResult.Failure(MediaError.CorruptMedia(it.message ?: "Invalid image file")) }
    }

    private fun readMedia(asset: Asset, file: File): MediaResult<MetadataResult> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.longMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?: asset.durationMs
                ?: 0L
            val width = retriever.intMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: 0
            val height = retriever.intMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: 0
            val rotation = retriever.intMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) ?: 0
            val bitrate = retriever.intMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) ?: 0
            val hasAudio = retriever.stringMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
            val captureFps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                retriever.stringMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()
            } else {
                null
            }
            val trackInfo = inspectTracks(file)
            val mimeType = asset.mimeType ?: trackInfo.primaryMime ?: mimeFromPath(file)
            val hasVideo = width > 0 && height > 0 || trackInfo.hasVideo
            val metadata = MediaMetadata(
                durationMs = durationMs,
                width = width,
                height = height,
                rotationDegrees = rotation,
                mimeType = mimeType,
                bitrate = bitrate,
                fps = captureFps ?: trackInfo.videoFps,
                hasVideoTrack = hasVideo,
                hasAudioTrack = hasAudio || trackInfo.hasAudio,
                isVariableFrameRate = trackInfo.isVariableFrameRate,
                audioSampleRate = trackInfo.audioSampleRate,
                audioChannels = trackInfo.audioChannels
            )
            if (durationMs <= 0L && asset.type != AssetType.Image) {
                MediaResult.Failure(MediaError.CorruptMedia("Media duration is missing or invalid"))
            } else {
                MediaResult.Success(MetadataResult(metadata, buildCompatibility(asset, metadata, trackInfo.reasons)))
            }
        } catch (throwable: Throwable) {
            MediaResult.Failure(MediaError.CorruptMedia(throwable.message ?: "Unable to read media metadata"))
        } finally {
            runCatching { retriever.release() }
        }
    }

    override suspend fun readMetadata(asset: Asset): MediaResult<MediaMetadata> = withContext(ioDispatcher) {
        when (val result = read(asset)) {
            is MediaResult.Success -> MediaResult.Success(result.value.metadata)
            is MediaResult.Failure -> result
        }
    }

    override suspend fun checkCompatibility(
        asset: Asset,
        metadata: MediaMetadata
    ): MediaResult<CompatibilityReport> = withContext(ioDispatcher) {
        MediaResult.Success(buildCompatibility(asset, metadata, emptyList()))
    }

    override suspend fun isTimelineSafe(asset: Asset, metadata: MediaMetadata): MediaResult<Boolean> =
        checkCompatibility(asset, metadata).map { it.isSafe }

    private fun inspectTracks(file: File): TrackInfo {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            var hasVideo = false
            var hasAudio = false
            var videoFps: Float? = null
            var audioSampleRate: Int? = null
            var audioChannels: Int? = null
            var primaryMime: String? = null
            val reasons = mutableListOf<String>()

            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (primaryMime == null) primaryMime = mime
                when {
                    mime?.startsWith("video/") == true -> {
                        hasVideo = true
                        if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            videoFps = format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                        }
                        if (!SAFE_VIDEO_MIMES.contains(mime)) {
                            reasons += "Video codec $mime should be transcoded"
                        }
                    }
                    mime?.startsWith("audio/") == true -> {
                        hasAudio = true
                        if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            audioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        if (!SAFE_AUDIO_MIMES.contains(mime)) {
                            reasons += "Audio codec $mime should be transcoded"
                        }
                    }
                }
            }
            TrackInfo(
                hasVideo = hasVideo,
                hasAudio = hasAudio,
                primaryMime = primaryMime,
                videoFps = videoFps,
                audioSampleRate = audioSampleRate,
                audioChannels = audioChannels,
                isVariableFrameRate = false,
                reasons = reasons
            )
        } catch (_: Throwable) {
            TrackInfo(reasons = listOf("Unable to inspect codec tracks"))
        } finally {
            extractor.release()
        }
    }

    private fun buildCompatibility(
        asset: Asset,
        metadata: MediaMetadata,
        codecReasons: List<String>
    ): Compatibility {
        val reasons = mutableListOf<String>()
        reasons += codecReasons

        val extension = asset.cachedPath.substringAfterLast('.', "").lowercase(Locale.US)
        if (extension !in SUPPORTED_EXTENSIONS) {
            reasons += "File extension .$extension is not in the supported import set"
        }
        val mimeType = metadata.mimeType.orEmpty()
        if (mimeType.isNotBlank() && !isSupportedMime(mimeType)) {
            reasons += "MIME type $mimeType is not directly supported"
        }
        if (metadata.hasVideoTrack && (metadata.width <= 0 || metadata.height <= 0)) {
            reasons += "Video dimensions are missing"
        }
        if (metadata.width > 1920 || metadata.height > 1920) {
            reasons += "Resolution exceeds realtime-safe 1080p bounds"
        }
        if (metadata.rotationDegrees != 0) {
            reasons += "Rotation ${metadata.rotationDegrees} should be normalized"
        }
        metadata.fps?.let { fps ->
            if (fps > 30.5f) {
                reasons += "Frame rate ${fps.formatFps()} should be normalized to 30fps"
            }
        }
        if (metadata.isVariableFrameRate) {
            reasons += "Variable frame rate should be normalized"
        }
        if (metadata.hasAudioTrack && metadata.audioSampleRate != null && metadata.audioSampleRate != 48_000) {
            reasons += "Audio sample rate ${metadata.audioSampleRate}Hz should be normalized to 48000Hz"
        }
        if (asset.needsTranscode) {
            reasons += "Asset was flagged for transcode during import"
        }
        val needsTranscode = reasons.isNotEmpty()
        return Compatibility(
            isSafe = !needsTranscode,
            needsTranscode = needsTranscode,
            reasons = reasons.distinct()
        )
    }

    private fun MediaMetadataRetriever.stringMetadata(key: Int): String? = extractMetadata(key)

    private fun MediaMetadataRetriever.intMetadata(key: Int): Int? = extractMetadata(key)?.toIntOrNull()

    private fun MediaMetadataRetriever.longMetadata(key: Int): Long? = extractMetadata(key)?.toLongOrNull()

    private fun mimeFromPath(file: File): String? {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase(Locale.US))
    }

    private fun isSupportedMime(mimeType: String): Boolean {
        return SUPPORTED_IMAGE_MIMES.contains(mimeType) ||
            SAFE_VIDEO_MIMES.contains(mimeType) ||
            SAFE_AUDIO_MIMES.contains(mimeType) ||
            SUPPORTED_CONTAINER_MIMES.contains(mimeType)
    }

    private fun Float.formatFps(): String = "%.2f".format(Locale.US, this)

    private data class TrackInfo(
        val hasVideo: Boolean = false,
        val hasAudio: Boolean = false,
        val primaryMime: String? = null,
        val videoFps: Float? = null,
        val audioSampleRate: Int? = null,
        val audioChannels: Int? = null,
        val isVariableFrameRate: Boolean = false,
        val reasons: List<String> = emptyList()
    )

    private companion object {
        const val DEFAULT_IMAGE_DURATION_MS = 3_000L
        val SUPPORTED_EXTENSIONS = setOf("mp4", "mov", "m4v", "mp3", "m4a", "aac", "wav", "jpg", "jpeg", "png", "webp")
        val SAFE_VIDEO_MIMES = setOf("video/avc", "video/mp4", "video/h264")
        val SAFE_AUDIO_MIMES = setOf("audio/mp4a-latm", "audio/mp4", "audio/aac", "audio/mpeg", "audio/wav", "audio/x-wav")
        val SUPPORTED_IMAGE_MIMES = setOf("image/jpeg", "image/png", "image/webp")
        val SUPPORTED_CONTAINER_MIMES = setOf("video/mp4", "video/quicktime", "audio/mp4", "audio/aac", "audio/mpeg", "audio/wav")
    }
}
