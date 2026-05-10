package com.nantcompany.clipy.edit.media.thumbnail

interface ThumbnailProvider {
    fun buildThumbnailCacheKey(request: ThumbnailRequest): String
}

class DefaultThumbnailProvider : ThumbnailProvider {
    override fun buildThumbnailCacheKey(request: ThumbnailRequest): String {
        return "${request.sourceUri}:${request.atMs}:${request.width}x${request.height}"
    }
}
