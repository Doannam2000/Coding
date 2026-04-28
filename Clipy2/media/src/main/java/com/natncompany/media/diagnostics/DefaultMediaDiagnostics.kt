package com.natncompany.media.diagnostics

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import com.natncompany.media.Asset
import com.natncompany.media.AssetDebugReport
import com.natncompany.media.CodecDescriptor
import com.natncompany.media.Compatibility
import com.natncompany.media.MediaDiagnostics
import com.natncompany.media.MediaError
import com.natncompany.media.MediaMetadata
import com.natncompany.media.MediaResult
import java.io.File

class DefaultMediaDiagnostics : MediaDiagnostics {
    override fun getVideoEncoders(): MediaResult<List<CodecDescriptor>> {
        return codecs(isEncoder = true, prefix = "video/")
    }

    override fun getVideoDecoders(): MediaResult<List<CodecDescriptor>> {
        return codecs(isEncoder = false, prefix = "video/")
    }

    override fun getAudioEncoders(): MediaResult<List<CodecDescriptor>> {
        return codecs(isEncoder = true, prefix = "audio/")
    }

    override fun getAudioDecoders(): MediaResult<List<CodecDescriptor>> {
        return codecs(isEncoder = false, prefix = "audio/")
    }

    override fun canDecode(mimeType: String): MediaResult<Boolean> {
        return MediaResult.Success(findCodec(mimeType, isEncoder = false) != null)
    }

    override fun canEncode(mimeType: String): MediaResult<Boolean> {
        return MediaResult.Success(findCodec(mimeType, isEncoder = true) != null)
    }

    override fun buildDebugReport(
        asset: Asset,
        metadata: MediaMetadata?,
        compatibility: Compatibility?
    ): MediaResult<AssetDebugReport> {
        val mimeType = metadata?.mimeType ?: asset.mimeType
        val decoders = if (mimeType?.startsWith("audio/") == true) {
            (getAudioDecoders() as? MediaResult.Success)?.value.orEmpty()
        } else {
            (getVideoDecoders() as? MediaResult.Success)?.value.orEmpty()
        }
        val encoders = if (mimeType?.startsWith("audio/") == true) {
            (getAudioEncoders() as? MediaResult.Success)?.value.orEmpty()
        } else {
            (getVideoEncoders() as? MediaResult.Success)?.value.orEmpty()
        }
        val file = File(asset.cachedPath)
        val codecInfo = buildList {
            add("mime=${mimeType ?: "unknown"}")
            add("type=${asset.type}")
            add("duration=${metadata?.durationMs ?: asset.durationMs ?: 0L}ms")
            add("size=${metadata?.width ?: 0}x${metadata?.height ?: 0}")
            add("rotation=${metadata?.rotationDegrees ?: 0}")
            add("fileExists=${file.exists()}")
            add("needsTranscode=${compatibility?.needsTranscode ?: asset.needsTranscode}")
        }
        return MediaResult.Success(
            AssetDebugReport(
                assetId = asset.id,
                displayName = asset.displayName,
                filePath = asset.cachedPath,
                fileSizeBytes = file.length().takeIf { file.exists() } ?: asset.fileSizeBytes,
                mimeType = mimeType,
                metadata = metadata,
                compatibility = compatibility,
                availableDecoders = decoders.filter { mimeType == null || mimeType in it.mimeTypes },
                availableEncoders = encoders.filter { mimeType == null || mimeType in it.mimeTypes },
                compatibilityReasons = compatibility?.reasons.orEmpty(),
                codecInfo = codecInfo,
                summary = if (compatibility?.isSafe == true) {
                    "Asset is preview/export safe"
                } else {
                    "Asset should be normalized before final render"
                }
            )
        )
    }

    private fun codecs(isEncoder: Boolean, prefix: String): MediaResult<List<CodecDescriptor>> {
        return runCatching {
            val list = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
                .filter { it.isEncoder == isEncoder }
                .mapNotNull { info ->
                    val supportedTypes = info.supportedTypes.filter { it.startsWith(prefix) }
                    if (supportedTypes.isEmpty()) {
                        null
                    } else {
                        info.toDescriptor(supportedTypes)
                    }
                }
            MediaResult.Success(list)
        }.getOrElse { MediaResult.Failure(MediaError.BackendUnavailable(it.message ?: "Unable to list codecs")) }
    }

    private fun findCodec(mimeType: String, isEncoder: Boolean): MediaCodecInfo? {
        return runCatching {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.firstOrNull { info ->
                info.isEncoder == isEncoder && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
            }
        }.getOrNull()
    }

    private fun MediaCodecInfo.toDescriptor(mimeTypes: List<String>): CodecDescriptor {
        return CodecDescriptor(
            name = name,
            mimeTypes = mimeTypes,
            isEncoder = isEncoder,
            isHardwareAccelerated = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                isHardwareAccelerated
            } else {
                !name.startsWith("OMX.google", ignoreCase = true)
            },
            isSoftwareOnly = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                isSoftwareOnly
            } else {
                name.startsWith("OMX.google", ignoreCase = true)
            },
            isVendor = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                isVendor
            } else {
                false
            },
            canonicalName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                canonicalName
            } else {
                null
            }
        )
    }
}
