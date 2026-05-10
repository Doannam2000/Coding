package com.nantcompany.clipy.picker

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import java.io.File

object VideoMetadataLoader {
    fun load(path: String): MediaItemModel {
        val file = File(path)
        val displayName = file.name.ifBlank { "Unnamed video" }
        val sizeBytes = file.takeIf { it.exists() }?.length() ?: 0L
        var durationMs: Long? = null
        var width: Int? = null
        var height: Int? = null
        var thumbnail: Bitmap? = runCatching {
            ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND)
        }.getOrNull()

        runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            if (thumbnail == null) thumbnail = retriever.frameAtTime
            retriever.release()
        }

        return MediaItemModel(
            uri = Uri.fromFile(file),
            displayName = displayName,
            mimeType = "video/*",
            sizeBytes = sizeBytes,
            durationMs = durationMs,
            width = width,
            height = height,
            thumbnail = thumbnail,
            type = MediaItemType.VIDEO
        )
    }
}
