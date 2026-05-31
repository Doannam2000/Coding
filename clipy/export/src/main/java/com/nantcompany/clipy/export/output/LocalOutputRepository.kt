package com.nantcompany.clipy.export.output

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class LocalOutputRepository {
    fun getAll(): List<OutputMedia> = synchronized(lock) {
        val fromDisk = readFromDisk()
        store.clear()
        store.addAll(fromDisk)
        store.toList()
    }

    fun save(output: OutputMedia) {
        synchronized(lock) {
            val current = readFromDisk().toMutableList()
            current.removeAll { it.path == output.path }
            current.add(0, output)
            store.clear()
            store.addAll(current)
            runCatching { writeToDisk(current) }
        }
    }

    fun clear() {
        synchronized(lock) {
            store.clear()
            if (storageFile.exists()) {
                storageFile.delete()
            }
        }
    }

    fun removeById(id: String) {
        synchronized(lock) {
            val current = readFromDisk().toMutableList()
            current.removeAll { it.id == id }
            store.clear()
            store.addAll(current)
            runCatching { writeToDisk(current) }
        }
    }

    fun removeByPath(path: String) {
        synchronized(lock) {
            val current = readFromDisk().toMutableList()
            current.removeAll { it.path == path }
            store.clear()
            store.addAll(current)
            runCatching { writeToDisk(current) }
        }
    }

    private fun readFromDisk(): List<OutputMedia> {
        val file = storageFile
        if (!file.exists()) return emptyList()
        return runCatching {
            val raw = file.readText()
            if (raw.isBlank()) return emptyList()
            val type = object : TypeToken<List<OutputMedia>>() {}.type
            gson.fromJson<List<OutputMedia>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun writeToDisk(outputs: List<OutputMedia>) {
        val file = storageFile
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(outputs))
    }

    private val storageFile: File
        get() {
            return File(storageDir ?: File(System.getProperty("java.io.tmpdir") ?: "."), "clipy/clipy_output_history.json")
        }

    companion object {
        var storageDir: File? = null
        private val lock = Any()
        private val store: MutableList<OutputMedia> = mutableListOf()
        private val gson = Gson()
    }
}
