package com.nantcompany.clipy.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

object MediaFileUtils {
    fun importUriToLocalPath(
        context: Context,
        uri: Uri,
        folderName: String,
        defaultExtension: String
    ): String {
        val directory = File(context.filesDir, folderName).apply { mkdirs() }
        val displayName = queryDisplayName(context, uri)
        val extension = displayName
            ?.substringAfterLast('.', missingDelimiterValue = defaultExtension)
            ?.takeIf { it.isNotBlank() }
            ?: defaultExtension
        val baseName = displayName
            ?.substringBeforeLast('.', missingDelimiterValue = displayName)
            ?.takeIf { it.isNotBlank() }
            ?: "media"
        val safeBase = baseName.lowercase(Locale.US).replace("[^a-z0-9-_]".toRegex(), "_")
        val output = File(directory, "${safeBase}_${System.currentTimeMillis()}.$extension")

        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open input stream for uri: $uri" }
            copyToFile(input, output)
        }
        return output.absolutePath
    }

    fun createOutputPath(
        context: Context,
        operation: String,
        extension: String
    ): String {
        val outputDir = File(context.filesDir, "outputs").apply { mkdirs() }
        val safeOp = operation.lowercase(Locale.US).replace("[^a-z0-9-_]".toRegex(), "_")
        return File(
            outputDir,
            "clipy_${safeOp}_${System.currentTimeMillis()}.$extension"
        ).absolutePath
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columnIndex >= 0 && cursor.moveToFirst()) cursor.getString(columnIndex) else null
        }
    }

    private fun copyToFile(input: InputStream, destination: File) {
        FileOutputStream(destination).use { output ->
            input.copyTo(output)
        }
    }
}
