package com.nantcompany.clipy.picker

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

object MediaScanner {

    /**
     * Query all images from external storage.
     * @param context The application context.
     * @return A list of MediaItemModel representing images, sorted by date added (newest first).
     */
    fun getAllImages(context: Context): List<MediaItemModel> {
        return queryMediaStore(
            context = context,
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE
            ),
            sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC",
            typeMapper = { cursor ->
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), id)
                MediaItemModel(
                    uri = uri,
                    displayName = displayName,
                    mimeType = mimeType,
                    sizeBytes = size,
                    durationMs = null, // Images don't have duration
                    width = null,
                    height = null,
                    thumbnail = null, // Thumbnail can be loaded later by the caller if needed
                    type = MediaItemType.IMAGE
                )
            }
        )
    }

    /**
     * Query all videos from external storage.
     * @param context The application context.
     * @return A list of MediaItemModel representing videos, sorted by date added (newest first).
     */
    fun getAllVideos(context: Context): List<MediaItemModel> {
        return queryMediaStore(
            context = context,
            collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DURATION
            ),
            sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC",
            typeMapper = { cursor ->
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE))
                val durationMs = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), id)
                MediaItemModel(
                    uri = uri,
                    displayName = displayName,
                    mimeType = mimeType,
                    sizeBytes = size,
                    durationMs = durationMs,
                    width = null,
                    height = null,
                    thumbnail = null, // Thumbnail can be loaded later by the caller if needed
                    type = MediaItemType.VIDEO
                )
            }
        )
    }

    /**
     * Query all audio files from external storage.
     * @param context The application context.
     * @return A list of MediaItemModel representing audio files, sorted by date added (newest first).
     */
    fun getAllAudio(context: Context): List<MediaItemModel> {
        return queryMediaStore(
            context = context,
            collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DURATION
            ),
            sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC",
            typeMapper = { cursor ->
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
                val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE))
                val durationMs = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), id)
                MediaItemModel(
                    uri = uri,
                    displayName = displayName,
                    mimeType = mimeType,
                    sizeBytes = size,
                    durationMs = durationMs,
                    width = null,
                    height = null,
                    thumbnail = null,
                    type = MediaItemType.AUDIO
                )
            }
        )
    }

    /**
     * Generic method to query a MediaStore collection and map to MediaItemModel.
     * @param context The application context.
     * @param collection The Uri of the MediaStore collection (e.g., MediaStore.Images.Media.getContentUri(...)).
     * @param projection The columns to return.
     * @param sortOrder The sort order for the query.
     * @param typeMapper A function to map a Cursor to a MediaItemModel.
     * @return A list of MediaItemModel.
     */
    private fun queryMediaStore(
        context: Context,
        collection: Uri,
        projection: Array<String>,
        sortOrder: String,
        typeMapper: (Cursor) -> MediaItemModel
    ): List<MediaItemModel> {
        val cursor = context.contentResolver.query(
            collection,
            projection,
            null, // selection (all items)
            null, // selectionArgs
            sortOrder
        ) ?: return emptyList()

        return cursor.use { c ->
            val items = mutableListOf<MediaItemModel>()
            while (c.moveToNext()) {
                items.add(typeMapper(c))
            }
            items
        }
    }
}