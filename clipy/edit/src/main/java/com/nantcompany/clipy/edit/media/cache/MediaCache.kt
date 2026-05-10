package com.nantcompany.clipy.edit.media.cache

import com.nantcompany.clipy.edit.media.model.MediaItem

interface MediaCache {
    fun get(key: String): MediaItem?
    fun put(key: String, value: MediaItem)
    fun clear()
}

class InMemoryMediaCache : MediaCache {
    private val store = mutableMapOf<String, MediaItem>()

    override fun get(key: String): MediaItem? = store[key]

    override fun put(key: String, value: MediaItem) {
        store[key] = value
    }

    override fun clear() {
        store.clear()
    }
}
