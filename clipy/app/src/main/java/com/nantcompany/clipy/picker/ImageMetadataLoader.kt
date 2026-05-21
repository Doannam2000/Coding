package com.nantcompany.clipy.picker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

object ImageMetadataLoader {
    fun load(path: String, targetSizePx: Int = 720): MediaItemModel {
        val file = File(path)
        val displayName = file.name.ifBlank { "Unnamed image" }
        val sizeBytes = file.takeIf { it.exists() }?.length() ?: 0L

        var width: Int? = null
        var height: Int? = null
        var thumbnail: Bitmap? = null

        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                width = bounds.outWidth
                height = bounds.outHeight
            }

            val sample = maxOf(1, minOf(bounds.outWidth / targetSizePx, bounds.outHeight / targetSizePx))
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
            thumbnail = BitmapFactory.decodeFile(path, decodeOptions)
        }

        return MediaItemModel(
            uri = Uri.fromFile(file),
            displayName = displayName,
            mimeType = "image/*",
            sizeBytes = sizeBytes,
            durationMs = null,
            width = width,
            height = height,
            thumbnail = thumbnail,
            type = MediaItemType.IMAGE
        )
    }

    fun loadThumbnail(context: android.content.Context, uri: Uri): Bitmap? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                // We can't reuse the stream easily, but thumbnails are small
                // In production, we'd use a better way
                BitmapFactory.decodeStream(input, null, bounds)
                
                context.contentResolver.openInputStream(uri)?.use { freshInput ->
                    val sample = maxOf(1, minOf(bounds.outWidth / 256, bounds.outHeight / 256))
                    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
                    BitmapFactory.decodeStream(freshInput, null, decodeOptions)
                }
            }
        }.getOrNull()
    }
}
