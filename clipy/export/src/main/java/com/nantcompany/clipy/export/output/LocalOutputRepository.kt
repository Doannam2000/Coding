package com.nantcompany.clipy.export.output

import org.json.JSONArray
import org.json.JSONObject
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

    private fun readFromDisk(): List<OutputMedia> {
        val file = storageFile
        if (!file.exists()) return emptyList()
        return runCatching {
            val raw = file.readText()
            if (raw.isBlank()) return emptyList()
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    add(
                        OutputMedia(
                            id = item.optString("id"),
                            fileName = item.optString("fileName"),
                            path = item.optString("path"),
                            sizeInBytes = item.optLong("sizeInBytes"),
                            createdAtEpochMs = item.optLong("createdAtEpochMs", System.currentTimeMillis()),
                            operation = item.optString("operation", "unknown")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeToDisk(outputs: List<OutputMedia>) {
        val file = storageFile
        file.parentFile?.mkdirs()
        val payload = JSONArray()
        outputs.forEach { output ->
            payload.put(
                JSONObject()
                    .put("id", output.id)
                    .put("fileName", output.fileName)
                    .put("path", output.path)
                    .put("sizeInBytes", output.sizeInBytes)
                    .put("createdAtEpochMs", output.createdAtEpochMs)
                    .put("operation", output.operation)
            )
        }
        file.writeText(payload.toString())
    }

    private val storageFile: File
        get() {
            val writableRoot = runCatching {
                File(System.getProperty("java.io.tmpdir") ?: ".")
            }.getOrElse { File(".") }
            return File(writableRoot, "clipy/clipy_output_history.json")
        }

    private companion object {
        val lock = Any()
        val store: MutableList<OutputMedia> = mutableListOf()
    }
}
