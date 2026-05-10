package com.nantcompany.clipy.picker

import android.graphics.Bitmap
import android.net.Uri

enum class MediaItemType {
    VIDEO,
    IMAGE,
    AUDIO,
    UNKNOWN
}

data class MediaItemModel(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val thumbnail: Bitmap?,
    val type: MediaItemType
)